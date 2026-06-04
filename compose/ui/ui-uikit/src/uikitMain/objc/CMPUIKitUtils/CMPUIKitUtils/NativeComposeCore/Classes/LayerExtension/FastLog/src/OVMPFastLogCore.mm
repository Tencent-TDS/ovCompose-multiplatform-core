/*
 * Tencent is pleased to support the open source community by making ovCompose
 * available. Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights
 * reserved.
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

#import "OVMPFastLogCore.h"
#import <cstring>
#import <mach/mach_time.h>

#import <CoreFoundation/CoreFoundation.h>
#import "OVMPLogPrinter.h"

#include <atomic>

namespace ovmp_fastlog {

// 静态缓冲区 - 无堆分配
static OVMPStaticBuffer g_staticBuffer;

// 全局环形缓冲区实例
static OVMPFastLogRingBuffer g_ringBuffer;

static bool gFastLogEnable = false;

// anchor 只在 App 初始化阶段设置一次：先写 wallUs，再写 machTicks。
static uint64_t gOVMPFastLogAnchorMach = 0;
static int64_t gOVMPFastLogAnchorWallUs = 0;

// 写入侧 drain 请求合并：避免高频写入下反复投递 drain 任务。
static std::atomic<bool> gDrainScheduled{false};

void OVMPFastLogRequestAsyncDrain() {
    bool expected = false;
    if (!gDrainScheduled.compare_exchange_strong(expected, true, std::memory_order_relaxed)) {
        return;
    }

    // 在打印队列上 drain，完成后再释放 scheduled 标记。
    [[OVMPLogPrinter shared] requestAsyncDrainWithReason:@"highwater" afterDrain:^{
        gDrainScheduled.store(false, std::memory_order_relaxed);
    }];
}


#pragma mark - OVMPFastLogRingBuffer 实现
OVMPFastLogStatus OVMPFastLogRingBuffer::initStatic(int32_t capacity) {
    if (buffer_ != nullptr) {
        return OVMP_FASTLOG_ERR_ALREADY_INIT;
    }

    if (capacity <= 0) {
        capacity = static_cast<int32_t>(kDefaultCapacity);
    }

    // 容量必须为 2 的幂，且不能超过静态缓冲区大小
    if ((capacity & (capacity - 1)) != 0) {
        return OVMP_FASTLOG_ERR_INVALID_ARG;
    }
    if (capacity > static_cast<int32_t>(kDefaultCapacity)) {
        return OVMP_FASTLOG_ERR_INVALID_ARG;
    }

    OVMPFastLogEntry* const buffer = g_staticBuffer.entries;
    std::atomic<uint32_t>* const seq = g_staticBuffer.seq;
    const uint32_t cap = static_cast<uint32_t>(capacity);
    const uint32_t mask = static_cast<uint32_t>(capacity - 1);

    std::memset(buffer, 0, sizeof(OVMPFastLogEntry) * cap);
    for (uint32_t i = 0; i < cap; ++i) {
        seq[i].store(i, std::memory_order_relaxed);
    }

    // 先重置队列/帧状态
    resetAllState();

    // 设置缓冲区（init 只调一次，shutdown 不会调用）
    capacity_ = cap;
    capacityMask_ = mask;
    seq_ = seq;
    buffer_ = buffer;

    return OVMP_FASTLOG_OK;
}

void OVMPFastLogRingBuffer::shutdown() {
    buffer_ = nullptr;
    seq_ = nullptr;
    capacity_ = 0;
    capacityMask_ = 0;
    resetAllState();
}

int32_t OVMPFastLogRingBuffer::drain(OVMPFastLogEntry* outBuffer, int32_t maxCount) {
    if (outBuffer == nullptr || maxCount <= 0 || buffer_ == nullptr) {
        return 0;
    }

    OVMPFastLogEntry* const buffer = buffer_;
    std::atomic<uint32_t>* const seq = seq_;
    const uint32_t capacity = capacity_;
    const uint32_t mask = capacityMask_;

    int32_t copied = 0;
    while (copied < maxCount) {
        uint32_t pos = dequeuePos_.load(std::memory_order_relaxed);
        std::atomic<uint32_t>& seqCell = seq[pos & mask];
        const uint32_t s = seqCell.load(std::memory_order_acquire);
        const int32_t dif = static_cast<int32_t>(s) - static_cast<int32_t>(pos + 1);
        if (dif == 0) {
            if (dequeuePos_.compare_exchange_weak(pos, pos + 1,
                                                 std::memory_order_relaxed,
                                                 std::memory_order_relaxed)) {
                outBuffer[copied++] = buffer[pos & mask];
                seqCell.store(pos + capacity, std::memory_order_release);
            }
        } else if (dif < 0) {
            break; // empty 或该位置尚未提交
        }
    }
    return copied;
}

int32_t OVMPFastLogRingBuffer::peek(OVMPFastLogEntry* outBuffer, int32_t maxCount, int32_t offset) const {
    if (outBuffer == nullptr || maxCount <= 0 || offset < 0 || buffer_ == nullptr) {
        return 0;
    }

    OVMPFastLogEntry* const buffer = buffer_;
    std::atomic<uint32_t>* const seq = seq_;
    const uint32_t mask = capacityMask_;

    const uint32_t available = readableCount();
    if (static_cast<uint32_t>(offset) >= available) {
        return 0;
    }

    const uint32_t start = dequeuePos_.load(std::memory_order_relaxed) + static_cast<uint32_t>(offset);
    int32_t copied = 0;
    while (copied < maxCount) {
        const uint32_t pos = start + static_cast<uint32_t>(copied);
        std::atomic<uint32_t>& seqCell = seq[pos & mask];
        const uint32_t s = seqCell.load(std::memory_order_acquire);
        const int32_t dif = static_cast<int32_t>(s) - static_cast<int32_t>(pos + 1);
        if (dif == 0) {
            outBuffer[copied++] = buffer[pos & mask];
        } else {
            break; // 遇到未提交条目就停止，保证顺序
        }
    }
    return copied;
}

void OVMPFastLogRingBuffer::clear() {
    if (buffer_ == nullptr) {
        return;
    }

    std::memset(buffer_, 0, sizeof(OVMPFastLogEntry) * capacity_);
    for (uint32_t i = 0; i < capacity_; ++i) {
        seq_[i].store(i, std::memory_order_relaxed);
    }
    resetAllState();
}

OVMPFastLogStats OVMPFastLogRingBuffer::stats() const {
    OVMPFastLogStats s;
    s.capacity = static_cast<int32_t>(capacity_);
    s.readable_count = readableCount();
    s.written_count = static_cast<int32_t>(enqueuePos_.load(std::memory_order_relaxed));
    s.dropped_count = static_cast<int32_t>(droppedCount_.load(std::memory_order_relaxed));
    s.frame_time_base = 0; // 已废弃，改用全局 anchor
    return s;
}

} // namespace ovmp_fastlog

// 使用全局实例
using namespace ovmp_fastlog;

#pragma mark - C API
extern "C" {

void ovmp_fastlog_shutdown(void) {
    gFastLogEnable = false;
    g_ringBuffer.shutdown();
    gDrainScheduled.store(false, std::memory_order_relaxed);
}

bool ovmp_fastlog_is_initialized(void) {
    return g_ringBuffer.isInitialized();
}

static std::atomic<int64_t> gFrameId{0};

static inline bool ovmp_fastlog_write_enabled() {
    return gFastLogEnable;
}

static inline bool ovmp_fastlog_ensure_initialized_default() {
    if (g_ringBuffer.isInitialized()) {
        return true;
    }
    const OVMPFastLogStatus st = g_ringBuffer.initStatic(static_cast<int32_t>(kDefaultCapacity));
    return (st == OVMP_FASTLOG_OK || st == OVMP_FASTLOG_ERR_ALREADY_INIT);
}

static inline OVMPFastLogStatus ovmp_fastlog_validate_read_args(OVMPFastLogEntry* out_buffer, int32_t* out_written) {
    if (!g_ringBuffer.isInitialized()) {
        return OVMP_FASTLOG_ERR_NOT_INIT;
    }
    if (out_buffer == nullptr || out_written == nullptr) {
        return OVMP_FASTLOG_ERR_INVALID_ARG;
    }
    return OVMP_FASTLOG_OK;
}

void ovmp_fastlog_log6(int32_t tag,
                  int64_t arg0, int64_t arg1, int64_t arg2,
                  int64_t arg3, int64_t arg4, int64_t arg5) {
    if (!ovmp_fastlog_write_enabled()) {
        return;
    }
    g_ringBuffer.log7(tag, arg0, arg1, arg2, arg3, arg4, arg5, 0);
}

void ovmp_fastlog_log7(int32_t tag,
                  int64_t arg0, int64_t arg1, int64_t arg2,
                  int64_t arg3, int64_t arg4, int64_t arg5,int64_t arg6) {
    if (!ovmp_fastlog_write_enabled()) {
        return;
    }
    g_ringBuffer.log7(tag, arg0, arg1, arg2, arg3, arg4, arg5, arg6);
}

void ovmp_fastlog_log8(int32_t tag,
                  int64_t arg0, int64_t arg1, int64_t arg2,
                  int64_t arg3, int64_t arg4, int64_t arg5,int64_t arg6, int64_t arg7) {
    if (!ovmp_fastlog_write_enabled()) {
        return;
    }
    g_ringBuffer.log8(tag, arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
}

void ovmp_fastlog_log4(int32_t tag,
                  int64_t arg0, int64_t arg1, int64_t arg2, int64_t arg3) {
    if (!ovmp_fastlog_write_enabled()) {
        return;
    }
    g_ringBuffer.log4(tag, arg0, arg1, arg2, arg3);
}

OVMPFastLogStatus ovmp_fastlog_drain(OVMPFastLogEntry* out_buffer, int32_t max_count, int32_t* out_written) {
    const OVMPFastLogStatus st = ovmp_fastlog_validate_read_args(out_buffer, out_written);
    if (st != OVMP_FASTLOG_OK) {
        return st;
    }

    *out_written = g_ringBuffer.drain(out_buffer, max_count);
    return OVMP_FASTLOG_OK;
}

OVMPFastLogStatus ovmp_fastlog_peek(OVMPFastLogEntry* out_buffer, int32_t max_count,
                           int32_t* out_written, int32_t offset) {
    const OVMPFastLogStatus st = ovmp_fastlog_validate_read_args(out_buffer, out_written);
    if (st != OVMP_FASTLOG_OK) {
        return st;
    }

    *out_written = g_ringBuffer.peek(out_buffer, max_count, offset);
    return OVMP_FASTLOG_OK;
}

void ovmp_fastlog_clear(void) {
    g_ringBuffer.clear();
}

OVMPFastLogStats ovmp_fastlog_stats(void) {
    return g_ringBuffer.stats();
}

void ovmp_fastlog_set_enabled(bool enabled) {
    if (!enabled) {
        gFastLogEnable = false;
        return;
    }

    if (!ovmp_fastlog_ensure_initialized_default()) {
        gFastLogEnable = false;
        return;
    }

    // 仅在启用时采样一次 anchor：避免每帧调用带来的额外开销。
    // 说明：anchor 固定后，日志绝对时间由"单调时钟差值 + anchorWall"映射得到；
    // 若系统墙钟发生校时/NTP 调整，输出的绝对时间不会跟随跳变（但单调性更好）。
    ovmp_fastlog_set_wall_clock_anchor();
    [[OVMPLogPrinter shared] start];
    
    gFastLogEnable = true;
}

bool ovmp_fastlog_is_enabled(void) {
    return gFastLogEnable;
}

void ovmp_fastlog_set_wall_clock_anchor(void) {
    const uint64_t mach = mach_absolute_time();
    // CFAbsoluteTime is seconds since 2001-01-01, add constant to get Unix epoch seconds
    const double wallSeconds = CFAbsoluteTimeGetCurrent() + kCFAbsoluteTimeIntervalSince1970;
    const int64_t wallUs = static_cast<int64_t>(wallSeconds * 1000000.0);

    // 先写 wallUs，再写 machTicks；以 machTicks!=0 作为"anchor 已初始化"标记。
    gOVMPFastLogAnchorWallUs = wallUs;
    gOVMPFastLogAnchorMach = mach;
}

bool ovmp_fastlog_get_wall_clock_anchor(double* out_wall_seconds, int64_t* out_mach_ticks) {
    const uint64_t mach = gOVMPFastLogAnchorMach;
    if (mach == 0) {
        return false;
    }

    const int64_t wallUs = gOVMPFastLogAnchorWallUs;
    if (out_wall_seconds) {
        *out_wall_seconds = (double)wallUs / 1000000.0;
    }
    if (out_mach_ticks) {
        *out_mach_ticks = (int64_t)mach;
    }
    return true;
}

static int64_t gComposeTraceIdCounter = 10086;   // 全局 traceId 计数器（Text/Image 共用）
static int64_t gComposeCurrentTraceId = 0;   // 当前正在处理的 traceId

int64_t ovmp_alloc_trace_id(void) {
    gComposeTraceIdCounter += 1;
    return gComposeTraceIdCounter;
}

void ovmp_set_current_trace_id(int64_t traceId) {
    gComposeCurrentTraceId = traceId;
}

int64_t ovmp_get_current_trace_id(void) {
    return gComposeCurrentTraceId;
}

} // extern "C"
