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

#import "TMMComposeRenderReuseUtils.h"
#import "TMMCanvasViewProxyV2.h"
#import "TMMRenderReuseCache.h"

/// 所有 Compose 场景都可以从全局的缓存池中复用 TMMCanvasViewProxy
static TMMRenderReuseCache<id<ITMMCanvasViewProxy>> *globalReusePool(void) {
    static TMMRenderReuseCache *cache;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        cache = [TMMRenderReuseCache cacheWithObjectLimitCount:50];
    });
    return cache;
}

///  将 TMMRenderReuseCache 的 ptr 指针转换成 TMMRenderReuseCache 对象
/// - Parameter reusePoolPtr: TMMRenderReuseCache 的 ptr 指针
NS_INLINE TMMRenderReuseCache *TMMRenderReuseCacheFromPtr(intptr_t reusePoolPtr) {
    return (__bridge TMMRenderReuseCache *)(CFTypeRef)reusePoolPtr;
}

#pragma mark - public
intptr_t TMMNativeCreateComposeSceneReusePool(void) {
    TMMRenderReuseCache *cache = [TMMRenderReuseCache cacheWithObjectLimitCount:100];
    CFTypeRef cacheRef = (__bridge_retained CFTypeRef)cache;
    return (intptr_t)cacheRef;
}

void TMMNativeReleaseComposeSceneReusePool(intptr_t reusePoolPtr) {
    TMMRenderReuseCache *sceneCache = (__bridge TMMRenderReuseCache *)(CFTypeRef)reusePoolPtr;
    [globalReusePool() addObjectFromReuseCache:sceneCache];
    CFTypeRef cacheRef = (CFTypeRef)reusePoolPtr;
    CFRelease(cacheRef);
}

id<ITMMCanvasViewProxy> _Nonnull TMMCanvasViewProxyDequeFromReusePool(intptr_t reusePoolPtr) {
    TMMRenderReuseCache<id<ITMMCanvasViewProxy>> *cache = TMMRenderReuseCacheFromPtr(reusePoolPtr);
    id<ITMMCanvasViewProxy> proxy = [cache dequeueObject];
    if (proxy) {
        [proxy prepareForReuse];
    } else {
        proxy = [globalReusePool() dequeueObject];
        [proxy prepareForReuse];
        if (!proxy) {
            proxy = [[TMMCanvasViewProxyV2 alloc] init];
        }
    }
    return proxy;
}

FOUNDATION_EXTERN void TMMCanvasViewProxyEnqueToReusePool(id<ITMMCanvasViewProxy> _Nonnull proxy, intptr_t reusePoolPtr) {
    TMMRenderReuseCache<id<ITMMCanvasViewProxy>> *cache = TMMRenderReuseCacheFromPtr(reusePoolPtr);
    BOOL enqueResult = [cache enqueObject:proxy];
    if (!enqueResult) {
        enqueResult = [globalReusePool() enqueObject:proxy];
    }
}
