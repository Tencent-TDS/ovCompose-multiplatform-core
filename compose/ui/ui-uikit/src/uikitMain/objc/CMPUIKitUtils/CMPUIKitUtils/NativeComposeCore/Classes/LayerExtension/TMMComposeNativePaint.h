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
@interface TMMComposeNativePaint : NSObject

/// kt 侧 设置的 alpha，透明度
@property (nonatomic, assign) float alpha;

/// kt 侧 设置的 color
@property (nonatomic, assign) uint64_t colorValue;

/// kt 侧 设置的 blendMode
@property (nonatomic, assign) TMMNativeDrawBlendMode blendMode;

/// kt 侧 设置的 paintingStyle
@property (nonatomic, assign) TMMNativeDrawPaintingStyle style;

/// kt 侧 设置的 strokeWidth strokeWidth
@property (nonatomic, assign) float strokeWidth;

/// kt 侧 设置的 strokeCap
@property (nonatomic, assign) TMMNativeDrawStrokeCap strokeCap;

/// kt 侧 设置的 strokeJoin
@property (nonatomic, assign) TMMNativeDrawStrokeJoin strokeJoin;

/// kt 侧 设置的 strokeMiterLimit
@property (nonatomic, assign) float strokeMiterLimit;

/// kt 侧设置的 filterQuality
@property (nonatomic, assign) TMMNativeDrawFilterQuality filterQuality;

/// kt 侧设置的，是否开启抗锯齿
@property (nonatomic, assign) BOOL isAntiAlias;

/// kt 侧 设置的 shader，通常是 TMMNativeBasicShader 子类
@property (nonatomic, strong, nullable) id shader;

/// kt 侧 设置的 pathEffect
@property (nonatomic, strong, nullable) id pathEffect;

/// kt 侧设置的 颜色 filter，通常是 TMMGaussianBlurFilter
@property (nonatomic, strong, nullable) id colorFilter;

/// 从 colorValue 中获取一个 UIColor
- (UIColor *)colorFromColorValue;

/// 准备复用
- (void)prepareForReuse;

@end

NS_ASSUME_NONNULL_END
