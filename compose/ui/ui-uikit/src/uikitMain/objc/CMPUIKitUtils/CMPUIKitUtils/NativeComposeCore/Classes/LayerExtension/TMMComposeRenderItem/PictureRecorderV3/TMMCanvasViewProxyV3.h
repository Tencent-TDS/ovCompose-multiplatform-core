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

#import "ITMMCanvasViewProxy.h"
#import "TMMNativeComposeAdaptivedCanvas.h"

NS_ASSUME_NONNULL_BEGIN

/// 该类的作用是避免 Kt 侧主动直接持有 UIView 或 CALayer，隔离方便后续优化
/// 额外说明：本类的所有给 kt 方法调用的方法，都尽可能使用基本数据类型，因此会存在参数较多的情况。主要是为了性能考虑
/// 主要是避免创建 kt 对象，比如 kt 侧传递 GCSize 等都会通过对象进行传递，而 kt 侧创建对象占据 GC 内存，并且效率远不如
/// 传递基本数据类型快。
@interface TMMCanvasViewProxyV3 : NSObject <ITMMCanvasViewProxy>

/// 根据 OVComposeExperimentalConfig 的指针初始化一个 TMMCanvasViewProxyV3，该参数主要方便内部做 AB 实验
/// - Parameter configPtr: 入参可为空
+ (instancetype)canvasViewProxyV3WithExperimentalConfigPtr:(intptr_t)configPtr;

- (instancetype)init NS_UNAVAILABLE;
+ (instancetype)new NS_UNAVAILABLE;

@end

NS_ASSUME_NONNULL_END
