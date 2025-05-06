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

#import "TMMNativeArcLayer.h"
#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>

@interface TMMNativeArcLayer()

/// 圆弧的宽度（决定椭圆横向半径）
@property (nonatomic, assign) CGFloat width;

/// 圆弧的高度（决定椭圆纵向半径）
@property (nonatomic, assign) CGFloat height;

/// 起始角度
@property (nonatomic, assign) CGFloat startAngle;

/// 扫过的角度
@property (nonatomic, assign) CGFloat sweepAngle;

/// 是否连接到中心点绘制扇形（YES=扇形，NO=圆弧）
@property (nonatomic, assign) BOOL useCenter;

/// 填充/描边颜色（useCenter=YES时填充，NO时描边）
@property (nonatomic, strong) UIColor *color;

/// 描边线宽（仅useCenter=NO时生效）
@property (nonatomic, assign) CGFloat strokeWidth;

@end

@implementation TMMNativeArcLayer

- (id<CAAction>)actionForKey:(NSString *)event {
    return nil;
}

- (void)updateArc:(CGFloat)width
           height:(CGFloat)height
       startAngle:(CGFloat)startAngle
       sweepAngle:(CGFloat)sweepAngle
        useCenter:(BOOL)useCenter
            color:(UIColor *)color
      strokeWidth:(CGFloat)strokeWidth
          density:(float)density {
    self.width = width;
    self.height = height;
    self.startAngle = startAngle;
    self.sweepAngle = sweepAngle;
    self.useCenter = useCenter;
    self.color = color;
    self.strokeWidth = strokeWidth;
    self.contentsScale = [UIScreen mainScreen].scale;
    [self setNeedsDisplay];
}

- (void)drawInContext:(CGContextRef)ctx {
    CGContextSaveGState(ctx);
    CGPoint center = CGPointMake(self.width / 2, self.height / 2);

    // Convert angles from degrees to radians
    CGFloat startAngleRad = self.startAngle * (M_PI / 180);
    CGFloat sweepAngleRad = self.sweepAngle * (M_PI / 180);

    CGContextSetStrokeColorWithColor(ctx, self.color.CGColor);

    CGFloat radius = (MIN(self.width, self.height) - self.strokeWidth) / 2.0;

    // Move to start point
    CGContextMoveToPoint(ctx, center.x + radius * cos(startAngleRad), center.y + radius * sin(startAngleRad));

    // Set the line width
    CGContextSetLineWidth(ctx, self.strokeWidth);

    // Add arc
    CGContextAddArc(ctx, center.x, center.y, radius, startAngleRad, startAngleRad + sweepAngleRad, 0);

    // Stroke the path
    CGContextStrokePath(ctx);

    CGContextRestoreGState(ctx);
}

@end
