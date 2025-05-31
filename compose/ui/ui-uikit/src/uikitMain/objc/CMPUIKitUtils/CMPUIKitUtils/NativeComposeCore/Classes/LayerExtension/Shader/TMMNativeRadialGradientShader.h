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

#import "TMMNativeBasicShader.h"
#import "TMMNativeEnums.h"

NS_ASSUME_NONNULL_BEGIN

/// 圆渐变的 shader，从 Kotlin 侧直接初始化
@interface TMMNativeRadialGradientShader : TMMNativeBasicShader

/// 渐变模式
@property (nonatomic, assign) TMMNativeTileMode tileMode;

/// 渐变颜色中心点
@property (nonatomic, assign, readonly) CGPoint center;

/// 圆形渐变的半径值
@property (nonatomic, assign, readonly) float radius;

/// 渐变颜色数组
@property (nonatomic, strong, readonly) NSMutableArray<id> *colors;

/// 渐变颜色的 stop 值数组
@property (nonatomic, strong, readonly, nullable) NSArray<NSNumber *> *colorStops;

/// 设置渐变颜色的中心点, x、y 分开传值，可避免在 kotlin 侧创建小对象
- (void)setCenter:(float)centerX centerY:(float)centerY;

/// 设置圆形渐变的半径值
- (void)setRadius:(float)radius;

/// 添加渐变的颜色，直接添加，可避免在 kotlin 侧创建小对象
- (void)addColor:(float)colorRed colorGreen:(float)colorGreen colorBlue:(float)colorBlue colorAlpha:(float)colorAlpha;

/// 添加渐变颜色的 stop 值 ，可避免在 kotlin 侧创建小对象
- (void)addColorStops:(float)colorStop;

@end

NS_ASSUME_NONNULL_END
