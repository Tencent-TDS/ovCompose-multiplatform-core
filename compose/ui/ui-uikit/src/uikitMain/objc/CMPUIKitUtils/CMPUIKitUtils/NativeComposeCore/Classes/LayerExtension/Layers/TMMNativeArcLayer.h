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

/// 圆弧 Layer
@interface TMMNativeArcLayer : CALayer

/// 更新圆弧/扇形所有参数
/// - Parameters:
///   - width: 椭圆横向直径
///   - height: 椭圆纵向直径
///   - startAngle: 起始角度
///   - sweepAngle: 扫过角度
///   - useCenter: 是否连接中心点
///   - color: 填充/描边色
///   - strokeWidth: 描边宽度
///   - density: 像素密度
- (void)updateArc:(CGFloat)width
           height:(CGFloat)height
       startAngle:(CGFloat)startAngle
       sweepAngle:(CGFloat)sweepAngle
        useCenter:(BOOL)useCenter
            color:(UIColor *)color
      strokeWidth:(CGFloat)strokeWidth
          density:(float)density;

@end
