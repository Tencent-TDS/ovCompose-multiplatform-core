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

#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

typedef void (^TMMImageProvideBlock)(UIImage *_Nullable image);

/// 从 SkBitmap 获取 Image 的 size
/// - Parameter skBitmapPtrAddress: 地址值
FOUNDATION_EXTERN CGSize TMMNativeComposeUIImageSizeFromSkBitmap(intptr_t skBitmapPtrAddress);

/// 根据 skBitmap 的地址同步创建一个 UIImage，cacheKey，如果存在缓存则直接返回
/// - Parameter skBitmapPtrAddress: 地址值
/// - Parameter cacheKey: 缓存 key
FOUNDATION_EXTERN UIImage *_Nullable TMMNativeComposeUIImageFromSkBitmap(intptr_t skBitmapPtrAddress, int32_t cacheKey);

/// 获取缓存的文本，文本不存在则返回 0，否则返回文本的 ptr 地址
/// - Parameter cacheKey: 缓存 key
FOUNDATION_EXTERN intptr_t TMMNativeComposeHasTextImageCache(int32_t cacheKey);

NS_ASSUME_NONNULL_END
