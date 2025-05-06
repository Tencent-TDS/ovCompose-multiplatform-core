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

#import <UIKit/UIKit.h>
#import "ITMMCanvasViewProxy.h"

NS_ASSUME_NONNULL_BEGIN

@class TMMCanvasViewProxy;

/// 从复用池中获取一个 proxy
/// - Parameter reusePoolPtr: 复用池指针
FOUNDATION_EXTERN id<ITMMCanvasViewProxy> _Nonnull TMMCanvasViewProxyDequeFromReusePool(intptr_t reusePoolPtr);

/// 将 proxy 放入复用池
/// - Parameter proxy: TMMCanvasViewProxy
/// - Parameter reusePoolPtr: 复用池指针
FOUNDATION_EXTERN void TMMCanvasViewProxyEnqueToReusePool(id<ITMMCanvasViewProxy> _Nonnull proxy, intptr_t reusePoolPtr);

/// 创建一个当前场景下的 ComposeScene 的 Native 对象复用池
FOUNDATION_EXTERN intptr_t TMMNativeCreateComposeSceneReusePool(void);

/// 释放当前场景下的 ComposeScene 的 Native 对象复用池
/// - Parameter reusePoolPtr: 复用池指针
FOUNDATION_EXTERN void TMMNativeReleaseComposeSceneReusePool(intptr_t reusePoolPtr);

NS_ASSUME_NONNULL_END
