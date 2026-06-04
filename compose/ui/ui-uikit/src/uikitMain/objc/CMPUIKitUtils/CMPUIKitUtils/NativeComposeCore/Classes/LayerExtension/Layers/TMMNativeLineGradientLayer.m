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

#import "TMMNativeLineGradientLayer.h"

#import "TMMComposeNativePath.h"
#import "TMMDrawUtils.h"

@interface TMMNativeLineGradientLayer ()

/// 像素密度
@property (nonatomic, assign, readonly) CGFloat density;

@end

@implementation TMMNativeLineGradientLayer

- (instancetype)init {
    self = [super init];
    if (self) {
        _density = TMMComposeCoreDeviceDensity();
    }
    return self;
}

- (id<CAAction>)actionForKey:(NSString *)event {
    return nil;
}

- (void)drawWithPointX1:(CGFloat)pointX1
                pointY1:(CGFloat)pointY1
                pointX2:(CGFloat)pointX2
                pointY2:(CGFloat)pointY2
              lineWidth:(CGFloat)lineWidth
                 shader:(TMMNativeLinearGradientShader *)shader
              strokeCap:(TMMNativeDrawStrokeCap)strokeCap {
    const CGFloat scaledWidth = lineWidth / _density;
    const CGFloat strokeRadius = scaledWidth / 2;

    const CGFloat lineLength = sqrt((pointY2 - pointY1) * (pointY2 - pointY1) + (pointX2 - pointX1) * (pointX2 - pointX1));
    if (lineLength < 0.0000001) {
        // Zero-length line. No need to draw.
        self.mask = nil;
        self.colors = nil;
        self.locations = nil;
        self.startPoint = CGPointMake(0, 0);
        self.endPoint = CGPointMake(0, 0);
        return;
    }

    const CGFloat cosA = fabs(pointX2 - pointX1) / lineLength;
    const CGFloat sinA = fabs(pointY2 - pointY1) / lineLength;

    const CGFloat offsetY = strokeRadius * cosA;
    const CGFloat offsetX = strokeRadius * sinA;

    self.bounds = CGRectMake(self.bounds.origin.x - offsetX, self.bounds.origin.y - offsetY, self.bounds.size.width + 2 * offsetX,
                             self.bounds.size.height + 2 * offsetY);

    // 计算起始和结束点
    const CGFloat startPointX = (pointX1 - MIN(pointX1, pointX2)) / _density;
    const CGFloat startPointY = (pointY1 - MIN(pointY1, pointY2)) / _density;
    const CGFloat endPointX = (pointX2 - MIN(pointX1, pointX2)) / _density;
    const CGFloat endPointY = (pointY2 - MIN(pointY1, pointY2)) / _density;

    // 创建线条路径
    UIBezierPath *path = [UIBezierPath bezierPath];
    [path moveToPoint:CGPointMake(startPointX, startPointY)];
    [path addLineToPoint:CGPointMake(endPointX, endPointY)];

    // 创建形状层
    CAShapeLayer *shapeLayer = self.mask;
    if (!shapeLayer) {
        shapeLayer = [CAShapeLayer layer];
        // 将形状层设置为渐变层的遮罩
        self.mask = shapeLayer;
    }
    shapeLayer.path = [path CGPath];
    shapeLayer.lineWidth = scaledWidth;
    shapeLayer.fillColor = [UIColor clearColor].CGColor; // 确保填充颜色为透明
    shapeLayer.strokeColor = [UIColor blackColor].CGColor;

    // 设置线条的端点样式
    switch (strokeCap) {
        case TMMNativeDrawStrokeCapRound:
            shapeLayer.lineCap = kCALineCapRound;
            break;
        case TMMNativeDrawStrokeCapButt:
            shapeLayer.lineCap = kCALineCapButt;
            break;
        case TMMNativeDrawStrokeCapSquare:
            shapeLayer.lineCap = kCALineCapSquare;
            break;
        default:
            shapeLayer.lineCap = kCALineCapButt;
            break;
    }

    // 设置渐变层
    self.colors = shader.colors;
    self.locations = shader.colorStops;

    // 设置渐变层的框架
    CGRect boundingBox = CGRectMake(MIN(startPointX, endPointX), MIN(startPointY, endPointY), fabs(endPointX - startPointX) + 2 * offsetX,
                                    fabs(endPointY - startPointY) + 2 * offsetY);
    shapeLayer.frame = boundingBox;

#define NORMALIZE(point, size) CGPointMake(point.x / _density / size.width, point.y / _density / size.height)
    // 注意，虽然 startPoint 和 endPoint 的 Y 轴方向与 Compose 相反，但由于这里只需要位置的比值来对应 colors，
    // 因此只要这里的 Y 值始终与 colorStops 一致，效果就完全能对齐 Android
    self.startPoint = NORMALIZE(shader.from, boundingBox.size);
    self.endPoint = NORMALIZE(shader.to, boundingBox.size);
#undef NORMALIZE
}

#pragma mark - lookin debug
- (NSDictionary *)lookin_customDebugInfos {
    return @{
        @"title" : @"Line",
    };
}

@end
