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

#import "TMMNativeComposeGradientLayer.h"
#import "TMMCALayerSaveState.h"
#import "TMMDrawUtils.h"
#import "TMMNativeImageShader.h"
#import "TMMNativeLinearGradientShader.h"
#import "TMMNativeRadialGradientShader.h"
#import "TMMNativeSweepGradientShader.h"

@interface TMMNativeComposeGradientLayer ()

/// 当前的 shader
@property (nonatomic, strong) TMMNativeBasicShader *shader;

/// 原始图
@property (nonatomic, strong) UIImage *originImage;

@end

@implementation TMMNativeComposeGradientLayer

- (id<CAAction>)actionForKey:(NSString *)event {
    return nil;
}

- (void)clear {
    self.backgroundColor = [UIColor clearColor].CGColor;
    if (self.contents) {
        self.contents = nil;
    }
}

- (void)applyShader:(TMMNativeBasicShader *)shader {
    if (self.shader != shader) {
        self.shader = shader;
        if ([shader isKindOfClass:[TMMNativeLinearGradientShader class]]) {
            [self clear];
            [self applyLinearGradientShader:(TMMNativeLinearGradientShader *)shader];
        } else if ([shader isKindOfClass:[TMMNativeRadialGradientShader class]]) {
            [self clear];
            [self applyRadialGradientShader:(TMMNativeRadialGradientShader *)shader];
        } else if ([shader isKindOfClass:[TMMNativeSweepGradientShader class]]) {
            [self clear];
            [self applySweepGradientShader:(TMMNativeSweepGradientShader *)shader];
        } else if ([shader isKindOfClass:[TMMNativeImageShader class]]) {
            TMMNativeImageShader *imageShader = (TMMNativeImageShader *)shader;
            [self applyRepeatImage:[imageShader image] shader:imageShader];
        }
    }

    if (!shader) {
        self.colors = nil;
        self.locations = nil;
    }
}

- (void)applyRepeatImage:(UIImage *)originImage shader:(TMMNativeImageShader *)shader {
    TMMNativeTileMode tileModeX = [(TMMNativeImageShader *)shader tileModeX];
    if (originImage && tileModeX == TMMNativeTileModeRepeated) {
        if (self.originImage != originImage) {
            self.originImage = originImage;
            UIImage *contentImage = [originImage resizableImageWithCapInsets:UIEdgeInsetsZero resizingMode:UIImageResizingModeTile];
            self.contents = (__bridge id _Nullable)([contentImage CGImage]);
        }

        if (self.locations) {
            self.locations = nil;
        }
        if (self.colors) {
            self.colors = nil;
        }

        if (!CGPointEqualToPoint(self.startPoint, CGPointZero)) {
            self.startPoint = CGPointZero;
        }
        if (!CGPointEqualToPoint(self.endPoint, CGPointZero)) {
            self.endPoint = CGPointZero;
        }
    }
}

#pragma mark - private
- (void)applyLinearGradientShader:(TMMNativeLinearGradientShader *)shader {
    const CGFloat denstity = TMMComposeCoreDeviceDensity();
    const CGSize layerSize = self.frame.size;
    const CGPoint from = shader.from;
    const CGPoint to = shader.to;
    self.type = kCAGradientLayerAxial;
    self.colors = shader.colors;
    self.locations = shader.colorStops;
    if (from.y <= 0 && to.y <= 0) {
        const float fromX = from.x / denstity;
        const float toX = to.x / denstity;
        // horizontalGradient
        self.startPoint = (CGPoint) {
            .x = MIN(fromX / layerSize.width, 1),
            .y = 0,
        };
        self.endPoint = (CGPoint) {
            .x = MIN(toX / layerSize.width, 1),
            .y = 0,
        };
    } else if (from.x <= 0 && to.x <= 0) {
        const float fromY = from.y / denstity;
        const float toY = to.y / denstity;
        // verticalGradient
        self.startPoint = (CGPoint) {
            .x = 0,
            .y = MIN(fromY / layerSize.height, 1),
        };
        self.endPoint = (CGPoint) {
            .x = 0,
            .y = MIN(toY / layerSize.height, 1),
        };
    } else {
        // linearGradient
        const float fromX = from.x / denstity;
        const float fromY = from.y / denstity;
        const float toX = to.x / denstity;
        const float toY = to.y / denstity;
        self.startPoint = (CGPoint) { .x = MIN(fromX / layerSize.width, 1), .y = MIN(fromY / layerSize.height, 1) };
        self.endPoint = (CGPoint) { .x = MIN(toX / layerSize.width, 1), .y = MIN(toY / layerSize.height, 1) };
    }
}

- (void)applyRadialGradientShader:(TMMNativeRadialGradientShader *)shader {
    const CGFloat denstity = TMMComposeCoreDeviceDensity();
    const CGSize layerSize = self.frame.size;
    CGFloat startPointX = (shader.center.x / denstity) / layerSize.width;
    CGFloat startPointY = (shader.center.y / denstity) / layerSize.height;
    const float radius = (shader.radius / denstity);
    self.type = kCAGradientLayerRadial;
    self.colors = shader.colors;
    self.locations = shader.colorStops;
    self.startPoint = CGPointMake(startPointX, startPointY);
    self.endPoint = (CGPoint) {
        .x = (startPointX + radius / layerSize.width),
        .y = (startPointY + radius / layerSize.height),
    };
}

- (void)applySweepGradientShader:(TMMNativeSweepGradientShader *)shader {
    if (@available(iOS 12.0, *)) {
        const CGFloat denstity = TMMComposeCoreDeviceDensity();
        const float startX = shader.center.x / denstity;
        const float startY = shader.center.y / denstity;
        const CGSize layerSize = self.frame.size;
        self.type = kCAGradientLayerConic;
        self.colors = shader.colors;
        self.locations = shader.colorStops;
        self.startPoint = (CGPoint) {
            .x = MIN((startX / layerSize.width), 1),
            .y = MIN((startY / layerSize.height), 1),
        };
        self.endPoint = CGPointMake(1, self.startPoint.y);
    }
}

#pragma mark - debug
- (NSDictionary *)lookin_customDebugInfos {
    return @{
        @"title" : @"GradientLayer",
        @"properties" :
            @[ @{ @"title" : @"Layer 信息", @"valueType" : @"string", @"section" : @"Laeyr 详细信息", @"value" : [self lookin_description] } ]
    };
}

- (NSString *)lookin_description {
    return [NSString stringWithFormat:@"{<GradientLayer:%@> transform:%@}", self, TMMNSStringFromCATransform3D(self.transform)];
}

@end
