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

#import "TMMNativeCircleLayer.h"
#import "TMMComposeNativePath.h"
#import "TMMDrawUtils.h"

@interface TMMNativeCircleLayer ()

/// 像素密度
@property (nonatomic, assign, readonly) CGFloat density;

@end

@implementation TMMNativeCircleLayer

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

- (void)drawWithCenterX:(CGFloat)centerX
                centerY:(CGFloat)centerY
                 radius:(CGFloat)radius
             paintColor:(UIColor *)paintColor
             paintStyle:(TMMNativeDrawPaintingStyle)paintStyle
            strokeWidth:(CGFloat)strokeWidth {
    CGPoint center = CGPointMake(centerX / _density, centerY / _density);
    UIBezierPath *path = [UIBezierPath bezierPathWithArcCenter:center radius:radius / _density startAngle:0 endAngle:2 * M_PI clockwise:YES];
    self.path = path.CGPath;
    CGColorRef clearColor = [UIColor clearColor].CGColor;
    self.strokeColor = clearColor;
    self.fillColor = clearColor;
    self.backgroundColor = clearColor;
    switch (paintStyle) {
        case TMMNativeDrawPaintingStyleFill:
            self.fillColor = [paintColor CGColor];
            break;
        case TMMNativeDrawPaintingStyleStroke:
            self.strokeColor = [paintColor CGColor];
            // 空心圆的宽度
            self.lineWidth = strokeWidth / _density;
            break;
        default:
            break;
    }
}

#pragma mark - lookin debug
- (NSDictionary *)lookin_customDebugInfos {
    return @{
        @"title" : @"Circle",
    };
}

@end
