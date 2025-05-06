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

#import "TVComposePointLayer.h"
#import "TMMComposeNativePath.h"
#import "TMMDrawUtils.h"

@interface TVComposePointLayer ()

/// 所有的 points
@property (nonatomic, strong) NSArray<NSNumber *> *points;

/// 描边 size
@property (nonatomic, assign) float strokeSize;

/// 绘制颜色
@property (nonatomic, strong) UIColor *color;

/// 端帽类型
@property (nonatomic, assign) TMMNativeDrawStrokeCap strokeCap;
@end

@implementation TVComposePointLayer

- (id<CAAction>)actionForKey:(NSString *)event {
    return nil;
}

- (void)updatePoints:(NSArray<NSNumber *> *)points strokeSize:(float)strokeSize color:(UIColor *)color strokeCap:(TMMNativeDrawStrokeCap)strokeCap {
    self.points = points;
    self.strokeSize = strokeSize;
    self.color = color;
    self.strokeCap = strokeCap;
}

- (void)drawInContext:(CGContextRef)ctx {
    if (self.color != NULL) {
        CGContextSetFillColorWithColor(ctx, self.color.CGColor);
    }

    const CGFloat density = TMMComposeCoreDeviceDensity();

    for (NSUInteger i = 0; (i < self.points.count) && (i + 1 < self.points.count); i += 2) {
        CGRect rect = CGRectMake(self.points[i].floatValue / density, self.points[i + 1].floatValue / density, self.strokeSize / density,
                                 self.strokeSize / density);
        handleDrawPoint(self.strokeCap, ctx, rect);
    }
}

void handleDrawPoint(TMMNativeDrawStrokeCap strokeCap, CGContextRef ctx, CGRect rect) {
    switch (strokeCap) {
        case TMMNativeDrawStrokeCapRound:
            CGContextFillEllipseInRect(ctx, rect);
            break;
        case TMMNativeDrawStrokeCapButt:
        case TMMNativeDrawStrokeCapSquare:
            CGContextFillRect(ctx, rect);
            break;
    }
}

@end
