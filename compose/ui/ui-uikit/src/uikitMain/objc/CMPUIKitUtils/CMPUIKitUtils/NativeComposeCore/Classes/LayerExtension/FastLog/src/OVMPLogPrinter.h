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

/*
 * LogPrinter - FastLog 输出调度器
 *
 * 将 OVMPFastLog 缓冲区中的条目导出并对接 OVComposeLog 输出。
 * 支持多种打印策略：定时+水位触发、退后台/终止时 flush、手动 flush。
 */

#import <Foundation/Foundation.h>
#import <dispatch/dispatch.h>

NS_ASSUME_NONNULL_BEGIN

@interface OVMPLogPrinter : NSObject

+ (instancetype)shared;

/// 启动自动打印。
- (void)start;

/// 立即 drain 并输出所有可读条目。
- (void)flush;

/// 写入侧唤醒：在打印队列上异步 drain（可合并投递）。
/// - Note: 仅供 FastLog Core 内部使用。
- (void)requestAsyncDrainWithReason:(NSString*)reason afterDrain:(dispatch_block_t _Nullable)afterDrain;

@end

NS_ASSUME_NONNULL_END
