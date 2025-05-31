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

#import "TMMNativeEnums.h"
#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

/// Compose Canvas 绘制 Line 的 Layer
@interface TMMNativeLineLayer : CAShapeLayer

/// 绘制 Line
/// - Parameters:
///   - pointX1: 起始点 x1，注意这里没有除以 density 直接来自于 compose
///   - pointY1: 起始点 y1，注意这里没有除以 density 直接来自于 compose
///   - pointX2: 结束点 x2，注意这里没有除以 density 直接来自于 compose
///   - pointY2: 结束点 y2，注意这里没有除以 density 直接来自于 compose
///   - lineWidth: 线的宽度
///   - lineColor: 线的颜色
///   - strokeCap: strokeCap 类型
///   - density: 屏幕像素密度
- (void)drawWithPointX1:(CGFloat)pointX1
                pointY1:(CGFloat)pointY1
                pointX2:(CGFloat)pointX2
                pointY2:(CGFloat)pointY2
              lineWidth:(CGFloat)lineWidth
              lineColor:(UIColor *)lineColor
              strokeCap:(TMMNativeDrawStrokeCap)strokeCap
                density:(float)density;

@end

NS_ASSUME_NONNULL_END
