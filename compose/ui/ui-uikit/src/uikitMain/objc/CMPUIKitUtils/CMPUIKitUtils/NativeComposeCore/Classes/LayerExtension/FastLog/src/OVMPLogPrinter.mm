/*
 * Tencent is pleased to support the open source community by making ovCompose available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#import "OVMPLogPrinter.h"
#import <UIKit/UIKit.h>
#import <CoreFoundation/CoreFoundation.h>
#import <mach/mach_time.h>
#import "OVMPFastLogCAPI.h"
#import "TMMComposeInjectCommonServiceImpl.h"

static NSString* const kOVMPFastLogComposeTag = @"[OVComposeTrace]";

// 消费侧：把 mach ticks 换算为微秒（写入侧不做此换算以保证高性能）
NS_INLINE int64_t OVMPFastLogTicksToUs(int64_t ticks) {
    static mach_timebase_info_data_t timebase = {0, 0};
    if (timebase.denom == 0) {
        mach_timebase_info(&timebase);
    }
    if (ticks <= 0) return 0;
    // ticks * numer / denom => ns，再 / 1000 => us
    return (ticks * timebase.numer) / (timebase.denom * 1000);
}

@interface OVMPLogPrinter ()
@property (nonatomic, strong) dispatch_queue_t queue;
@property (nonatomic, strong, nullable) dispatch_source_t timer;
@property (nonatomic, assign) uint32_t intervalMs;
@property (nonatomic, assign) uint32_t threshold;
@property (nonatomic, strong, nullable) id bgToken;
@property (nonatomic, strong, nullable) id termToken;
@end

@implementation OVMPLogPrinter

+ (instancetype)shared {
    static OVMPLogPrinter *instance = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        instance = [[OVMPLogPrinter alloc] init];
    });
    return instance;
}

- (instancetype)init {
    self = [super init];
    if (self) {
        _queue = dispatch_queue_create("com.tencent.ovmp.fastlog.logprinter", DISPATCH_QUEUE_SERIAL);
        _intervalMs = 500;
        _threshold = 512;
    }
    return self;
}

- (void)start {
    const uint32_t resolvedInterval = 2000;
    const uint32_t resolvedThreshold = 512;

    dispatch_async(self.queue, ^{
        self.intervalMs = resolvedInterval;
        self.threshold = resolvedThreshold;

        [self stopTimerLocked];
        [self installLifecycleObserversLocked];
        [self startTimerLocked];
    });
}

- (void)stop {
    dispatch_async(self.queue, ^{
        [self stopTimerLocked];
        [self uninstallLifecycleObserversLocked];
    });
}

- (void)flush {
    [self requestAsyncDrainWithReason:@"manual" afterDrain:nil];
}

- (void)requestAsyncDrainWithReason:(NSString*)reason afterDrain:(dispatch_block_t _Nullable)afterDrain {
    dispatch_async(self.queue, ^{
        [self drainAndPrintLockedMaxEntries:0 reason:reason];
        if (afterDrain) {
            afterDrain();
        }
    });
}

#pragma mark - Internal (queue-locked)

- (void)startTimerLocked {
    dispatch_source_t timer = dispatch_source_create(DISPATCH_SOURCE_TYPE_TIMER, 0, 0, self.queue);
    if (!timer) return;

    self.timer = timer;
    dispatch_source_set_timer(
        timer,
        dispatch_time(DISPATCH_TIME_NOW, (int64_t)self.intervalMs * NSEC_PER_MSEC),
        (uint64_t)self.intervalMs * NSEC_PER_MSEC,
        1 * NSEC_PER_MSEC);

    __weak typeof(self) weakSelf = self;
    dispatch_source_set_event_handler(timer, ^{
        __strong typeof(weakSelf) strongSelf = weakSelf;
        if (!strongSelf) return;

        if (!ovmp_fastlog_is_initialized()) return;
        const OVMPFastLogStats stats = ovmp_fastlog_stats();
        if (stats.readable_count == 0) return;

        // 兜底策略：只要有可读日志，就按周期输出一次（不受 threshold 限制）。
        [strongSelf drainAndPrintLockedMaxEntries:0 reason:@"timer"];
    });

    dispatch_resume(timer);
}

- (void)stopTimerLocked {
    if (!self.timer) return;
    dispatch_source_cancel(self.timer);
    self.timer = nil;
}

- (void)installLifecycleObserversLocked {
    if (self.bgToken || self.termToken) {
        return;
    }

    __weak typeof(self) weakSelf = self;
    NSNotificationCenter* nc = [NSNotificationCenter defaultCenter];

    self.bgToken = [nc addObserverForName:UIApplicationDidEnterBackgroundNotification
                                  object:nil
                                   queue:nil
                              usingBlock:^(__unused NSNotification* note) {
        __strong typeof(weakSelf) strongSelf = weakSelf;
        if (!strongSelf) return;
        dispatch_async(strongSelf.queue, ^{
            [strongSelf drainAndPrintLockedMaxEntries:0 reason:@"background"];
        });
    }];

    self.termToken = [nc addObserverForName:UIApplicationWillTerminateNotification
                                    object:nil
                                     queue:nil
                                usingBlock:^(__unused NSNotification* note) {
        __strong typeof(weakSelf) strongSelf = weakSelf;
        if (!strongSelf) return;
        dispatch_async(strongSelf.queue, ^{
            [strongSelf drainAndPrintLockedMaxEntries:0 reason:@"terminate"];
        });
    }];
}

- (void)uninstallLifecycleObserversLocked {
    NSNotificationCenter* nc = [NSNotificationCenter defaultCenter];
    if (self.bgToken) {
        [nc removeObserver:self.bgToken];
        self.bgToken = nil;
    }
    if (self.termToken) {
        [nc removeObserver:self.termToken];
        self.termToken = nil;
    }
}

- (void)drainAndPrintLockedMaxEntries:(uint32_t)maxEntries reason:(NSString *)reason {
    if (!ovmp_fastlog_is_initialized()) {
        return;
    }

    // 获取全局 anchor 用于换算绝对时间（只获取一次）
    double anchorWallSeconds = 0;
    int64_t anchorMachTicks = 0;
    const BOOL hasAnchor = ovmp_fastlog_get_wall_clock_anchor(&anchorWallSeconds, &anchorMachTicks);
    const int64_t anchorWallUs = hasAnchor ? (int64_t)(anchorWallSeconds * 1000000.0) : 0;

    // 压缩格式: [Z<count>]<base64(zlib(binary))>
    // 二进制格式: 每条 74 字节 = time(8) + tag(2) + arg0-7(8*8=64)
    static const int32_t kBatchSize = 50;
    static const size_t kEntryBinarySize = 74;
    
    // 栈上分配：entries + binary buffer
    // 50 * 76 = 3800 字节 (OVMPFastLogEntry)
    // 50 * 74 = 3700 字节 (binary)
    // 总计约 7.5KB，完全没问题
    OVMPFastLogEntry entryBuffer[kBatchSize];
    uint8_t binaryBuffer[kBatchSize * kEntryBinarySize];

    // 循环 drain，每次最多 kBatchSize 条
    for (;;) {
        int32_t written = 0;
        const int32_t toDrain = (maxEntries == 0) ? kBatchSize : (int32_t)MIN(maxEntries, (uint32_t)kBatchSize);
        ovmp_fastlog_drain(entryBuffer, toDrain, &written);
        
        if (written == 0) {
            break;
        }
        
        // 序列化到 binaryBuffer
        uint8_t *p = binaryBuffer;
        for (int32_t i = 0; i < written; ++i) {
            const OVMPFastLogEntry e = entryBuffer[i];
            
            // 时间换算
            int64_t absWallUs = 0;
            if (hasAnchor && e.mach_ticks > 0) {
                absWallUs = anchorWallUs + OVMPFastLogTicksToUs(e.mach_ticks - anchorMachTicks);
            }
            
            // 写入二进制 (小端序，ARM64 原生小端，直接拷贝)
            memcpy(p, &absWallUs, 8); p += 8;
            int16_t tag16 = (int16_t)e.tag;
            memcpy(p, &tag16, 2); p += 2;
            // args 连续存储，一次 memcpy 64 字节
            memcpy(p, &e.arg0, 64); p += 64;
        }
        
        const size_t binaryLen = (size_t)written * kEntryBinarySize;
        NSData *rawData = [NSData dataWithBytesNoCopy:binaryBuffer length:binaryLen freeWhenDone:NO];
        
        // zlib 压缩
        NSData *compressedData = nil;
        if (@available(iOS 13.0, *)) {
            compressedData = [rawData compressedDataUsingAlgorithm:NSDataCompressionAlgorithmZlib error:nil];
        }
        
        if (compressedData && compressedData.length < binaryLen) {
            NSString *base64 = [compressedData base64EncodedStringWithOptions:0];
            OVComposeLog(kOVMPFastLogComposeTag, ([NSString stringWithFormat:@"[Z%d]%@\n", written, base64]));
        } else {
            NSString *base64 = [rawData base64EncodedStringWithOptions:0];
            OVComposeLog(kOVMPFastLogComposeTag, ([NSString stringWithFormat:@"[F%d]%@\n", written, base64]));
        }
        
        // 如果指定了 maxEntries，递减并检查
        if (maxEntries > 0) {
            maxEntries -= (uint32_t)written;
            if (maxEntries == 0) {
                break;
            }
        }
    }
}

@end
