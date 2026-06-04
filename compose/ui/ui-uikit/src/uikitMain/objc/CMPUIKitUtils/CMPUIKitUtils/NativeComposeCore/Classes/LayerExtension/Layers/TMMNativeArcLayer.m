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

/// 描边端点样式
@property (nonatomic, assign) TMMNativeDrawStrokeCap strokeCap;

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
        strokeCap:(TMMNativeDrawStrokeCap)strokeCap
          density:(float)density {
    self.width = width;
    self.height = height;
    self.startAngle = startAngle;
    self.sweepAngle = sweepAngle;
    self.useCenter = useCenter;
    self.color = color;
    self.strokeWidth = strokeWidth;
    self.strokeCap = strokeCap;
    self.contentsScale = [UIScreen mainScreen].scale;
    [self setNeedsDisplay];
}

- (void)drawInContext:(CGContextRef)ctx {
    CGContextSaveGState(ctx);

    // 将角度转换为弧度
    CGFloat startAngleRad = self.startAngle * (M_PI / 180.0);
    CGFloat endAngleRad = (self.startAngle + self.sweepAngle) * (M_PI / 180.0);

    // 判断是填充模式还是描边模式：strokeWidth > 0 为描边，否则为填充
    BOOL isFillMode = (self.strokeWidth <= 0);

    // 计算椭圆的半径
    CGFloat radiusX, radiusY;
    CGFloat centerX = self.width / 2.0;
    CGFloat centerY = self.height / 2.0;

    if (isFillMode) {
        // 填充模式：使用完整区域
        radiusX = self.width / 2.0;
        radiusY = self.height / 2.0;
    } else {
        // 描边模式：减去描边宽度的一半，确保半径不为负值
        radiusX = MAX((self.width - self.strokeWidth) / 2.0, 0.0);
        radiusY = MAX((self.height - self.strokeWidth) / 2.0, 0.0);
    }

    // 使用 CGPath + CGAffineTransform 构建椭圆弧线路径
    // 这样可以避免 CGContext 的缩放变换影响描边线宽
    CGAffineTransform transform = CGAffineTransformMakeTranslation(centerX, centerY);
    transform = CGAffineTransformScale(transform, radiusX, radiusY);

    // 设置描边端点样式
    switch (self.strokeCap) {
        case TMMNativeDrawStrokeCapRound:
            CGContextSetLineCap(ctx, kCGLineCapRound);
            break;
        case TMMNativeDrawStrokeCapSquare:
            CGContextSetLineCap(ctx, kCGLineCapSquare);
            break;
        case TMMNativeDrawStrokeCapButt:
        default:
            CGContextSetLineCap(ctx, kCGLineCapButt);
            break;
    }

    CGMutablePathRef path = CGPathCreateMutable();

    if (self.useCenter) {
        // 扇形模式：从圆心出发，画弧线，再回到圆心
        CGPathMoveToPoint(path, &transform, 0, 0);
        CGPathAddArc(path, &transform, 0, 0, 1.0, startAngleRad, endAngleRad, false);
        CGPathCloseSubpath(path);
    } else {
        // 弧线模式：只画弧线段
        CGPathAddArc(path, &transform, 0, 0, 1.0, startAngleRad, endAngleRad, false);
        if (isFillMode) {
            // 填充模式下闭合路径（弦）
            CGPathCloseSubpath(path);
        }
    }

    CGContextAddPath(ctx, path);

    if (isFillMode) {
        // 填充模式
        CGContextSetFillColorWithColor(ctx, self.color.CGColor);
        CGContextFillPath(ctx);
    } else {
        // 描边模式
        CGContextSetStrokeColorWithColor(ctx, self.color.CGColor);
        CGContextSetLineWidth(ctx, self.strokeWidth);
        CGContextStrokePath(ctx);
    }

    CGPathRelease(path);

    CGContextRestoreGState(ctx);
}

#pragma mark - debug
- (NSDictionary *)lookin_customDebugInfos {
    return @{
        @"title" : @"ArcLayer",
        @"properties" : @[ @{
            @"title" : @"ArcLayer 信息",
            @"valueType" : @"string",
            @"section" : @"ArcLayer 详细信息",
            @"value" : [NSString stringWithFormat:@"%@", self]
        } ]
    };
}

@end
