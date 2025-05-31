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

#import "TMMNativeLineLayer.h"
#import "TMMComposeNativePath.h"
#import "TMMDrawUtils.h"

@implementation TMMNativeLineLayer

- (id<CAAction>)actionForKey:(NSString *)event {
    return nil;
}

- (void)drawWithPointX1:(CGFloat)pointX1
                pointY1:(CGFloat)pointY1
                pointX2:(CGFloat)pointX2
                pointY2:(CGFloat)pointY2
              lineWidth:(CGFloat)lineWidth
              lineColor:(UIColor *)lineColor
              strokeCap:(TMMNativeDrawStrokeCap)strokeCap
                density:(float)density {
    const CGFloat scaledWidth = lineWidth / density;
    // 起始和终点位置也需要计算相对于x/y的值
    const CGFloat startPointX = (pointX1 - MIN(pointX1, pointX2)) / density;
    const CGFloat startPointY = (pointY1 - MIN(pointY1, pointY2)) / density;
    const CGFloat endPointX = (pointX2 - MIN(pointX1, pointX2)) / density;
    const CGFloat endPointY = (pointY2 - MIN(pointY1, pointY2)) / density;

    UIBezierPath *path = [UIBezierPath bezierPath];
    [path moveToPoint:CGPointMake(startPointX, startPointY)];
    [path addLineToPoint:CGPointMake(endPointX, endPointY)];
    self.path = [path CGPath];
    self.strokeColor = lineColor.CGColor;
    self.lineWidth = scaledWidth;
    switch (strokeCap) {
        case TMMNativeDrawStrokeCapRound:
            self.lineCap = kCALineCapRound;
            break;
        case TMMNativeDrawStrokeCapButt:
            self.lineCap = kCALineCapButt;
            break;
        case TMMNativeDrawStrokeCapSquare:
            self.lineCap = kCALineCapSquare;
        default:
            self.lineCap = kCALineCapButt;
            break;
    }
}

#pragma mark - lookin debug
- (NSDictionary *)lookin_customDebugInfos {
    return @{
        @"title" : @"Line",
    };
}

@end
