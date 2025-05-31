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

#import "TMMNativeComposeAdaptivedCanvas.h"
#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

@class TMMComposeTextLayer;
@class TMMComposeNativePath;
@class TMMComposeNativePaint;
@class TMMNativeRoundRect;

/// 获取设备的 density
FOUNDATION_EXTERN inline float TMMComposeCoreDeviceDensity(void);

/// 从 Long 值中还原出 UIColor，该 Long 值是 Kotlin Compose 中的 Color.value
/// - Parameter colorValue: Long
FOUNDATION_EXTERN inline UIColor *TMMComposeCoreMakeUIColorFromULong(uint64_t colorValue);

/// 从 Paint 中直接获取 UIColor
/// - Parameter paint: TMMComposeNativePaint *
FOUNDATION_EXTERN inline UIColor *UIColorFromNativePaint(TMMComposeNativePaint *paint);

/// 从 Paint 上根据参数，计算出一个 hash
/// - Parameter paint: TMMComposeNativePaint *
FOUNDATION_EXTERN inline uint64_t TMMNativeDataHashFromPaint(TMMComposeNativePaint *paint);

/// 从 NSArray 上根据所有成员，计算出一个hash
FOUNDATION_EXTERN inline uint64_t TMMFNVHashFloatArray(NSArray<NSNumber *> *array);

#pragma mark - Hash 计算
/// 计算 hash 值
FOUNDATION_EXTERN inline uint64_t TMMFNVHash(const void *data, size_t len);

/// 计算 4 和 float 的 hash 值
FOUNDATION_EXTERN inline uint64_t TMMComposeCoreHash4Floats(float a, float b, float c, float d);

/// 计算 6 个 float 的 hash 值
FOUNDATION_EXTERN inline uint64_t TMMComposeCoreHash6Floats(float a, float b, float c, float d, float e, float f);

/// 计算 8 个 float 的 hash 值
FOUNDATION_EXTERN inline uint64_t TMMComposeCoreHash8Floats(float a, float b, float c, float d, float e, float f, float g, float h);

/// 将数组内的所有元素计算出一个最终 hash
/// - Parameter array: NSArray <id <NSObject>>
FOUNDATION_EXTERN inline uint64_t TMMComposeCoreHashNSArray(NSArray <id <NSObject>> *array);

#pragma mark - Native 对象创建

/// 快速创建一个 TMMComposeNativePath
FOUNDATION_EXTERN TMMComposeNativePath *TMMComposeCoreNativeCreateNativePath(void);

/// 快速创建一个 TMMNativeRoundRect
FOUNDATION_EXTERN TMMNativeRoundRect *TMMComposeCoreNativeCreateDefaultRoundRect(void);

/// 根据路径将图片进行解码
/// - Parameter imagePath: NSString
FOUNDATION_EXTERN UIImage *_Nullable TMMDecodedImageFromPath(NSString *imagePath);

/// 创建一个 TMMComposeTextLayer
FOUNDATION_EXTERN TMMComposeTextLayer *TMMComposeTextCreate(void);

NS_ASSUME_NONNULL_END
