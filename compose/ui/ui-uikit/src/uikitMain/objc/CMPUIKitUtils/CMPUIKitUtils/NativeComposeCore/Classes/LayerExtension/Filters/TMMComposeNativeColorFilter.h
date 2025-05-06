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
#import "TMMNativeEnums.h"

NS_ASSUME_NONNULL_BEGIN

/// 该类将会在 Kotlin 侧进行初始化，并携带 Kotlin 侧的 Paint 信息，最终传递给 Native 侧使用
/// 后续拆分为三个子类来做
@interface TMMComposeNativeColorFilter : NSObject

/// filter 类型
@property (nonatomic, assign) TMMNativeColorFilterType type;

/// kt 侧 设置 paintColor
@property (nonatomic, assign) uint64_t colorValue;

/// kt 侧 设置的 blendMode
@property (nonatomic, assign) TMMNativeDrawBlendMode blendMode;

// TMMNativeColorFilterTypeMatrix
@property (nonatomic, assign) CGFloat *matrix;

/// 矩阵 size
@property (nonatomic) NSUInteger matrixSize;

/// TMMNativeColorFilterTypeLighting
@property (nonatomic, assign) uint64_t multiply;

/// kt 侧 设置的 add
@property (nonatomic, assign) uint64_t add;

/// 设置 colorFilter
/// - Parameter colorFilter: TMMComposeNativeColorFilter
- (void)setColorFilterInfo:(TMMComposeNativeColorFilter *_Nonnull)colorFilter;

/// 根据 imageRef 获取一个处理后的 UIImage
/// - Parameter imageRef: CGImageRef
- (nullable UIImage *)filterImageWithImageRef:(CGImageRef)imageRef;

@end

NS_ASSUME_NONNULL_END
