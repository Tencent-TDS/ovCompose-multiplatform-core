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

/// Compose Canvas 绘制 Circle 的 Layer
@interface TMMNativeCircleLayer : CAShapeLayer

/// 绘制圆
/// - Parameters:
///   - centerX: 中心点 x，注意这里没有除以 density 直接来自于 compose
///   - centerY: 中心点 y，注意这里没有除以 density 直接来自于 compose
///   - radius: 圆的半径，注意这里没有除以 density 直接来自于 compose
///   - paintColor: 填充颜色
///   - paintStyle: 填充类型
///   - strokeWidth: 边框宽度
- (void)drawWithCenterX:(CGFloat)centerX
                centerY:(CGFloat)centerY
                 radius:(CGFloat)radius
             paintColor:(UIColor *)paintColor
             paintStyle:(TMMNativeDrawPaintingStyle)paintStyle
            strokeWidth:(CGFloat)strokeWidth;

@end

NS_ASSUME_NONNULL_END
