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

#import "TMMUIKitCanvasLayer.h"

@interface TMMUIKitCanvasLayer()

/// 用于裁剪 shadow 的 maskLayer
@property (nonatomic, strong) CAShapeLayer *maskLayer;

/// 用于记录遮罩的 bounds
@property (nonatomic, assign) CGRect maskBounds;

/// kt 侧影半径
@property (nonatomic, assign) float ktShadowRadius;

/// 是否裁剪子 Layer
@property (nonatomic, assign) BOOL cachedMaskToBounds;

@end

@implementation TMMUIKitCanvasLayer

- (void)prepareForReuse {
    if (self.shadowColor) {
        self.shadowColor = [UIColor clearColor].CGColor;
    }

    if (self.shadowRadius > 0) {
        self.shadowRadius = 0;
    }

    if (self.shadowOpacity > 0) {
        self.shadowOpacity = 0;
    }

    if (self.shadowPath) {
        self.shadowPath = NULL;
    }
    if (!CGPointEqualToPoint(self.anchorPoint, CGPointMake(0.5, 0.5))) {
        self.anchorPoint = CGPointMake(0.5, 0.5);
    }
    self.transform = CATransform3DIdentity;
    if (!CGPointEqualToPoint(self.position, CGPointZero)) {
        self.position = CGPointZero;
    }
    if (self.cornerRadius != 0) {
        self.cornerRadius = 0;
    }
    if (self.mask) {
        self.mask = nil;
    }
    NSArray<CALayer *> *sublayers = self.sublayers;
    for (NSInteger i = sublayers.count - 1; i >= 0; i--) {
        [sublayers[i] removeFromSuperlayer];
    }
}

- (void)layoutSublayers {
    [super layoutSublayers];
    [self updateMaskLayerPathIfNeeded];
}

- (void)setShadowWithColor:(UIColor *)color
                 elevation:(float)elevation
            ktShadowRadius:(float)ktShadowRadius {
    if (elevation > 0) {
        self.cachedMaskToBounds = self.masksToBounds;
        if (self.masksToBounds) {
            self.masksToBounds = NO;
        }
        self.ktShadowRadius = ktShadowRadius;
        self.shadowRadius = ceil(elevation / 2);
        self.shadowColor = color.CGColor;
        self.shadowOffset = CGSizeMake(0, 0);
        self.shadowOpacity = 0.19f;
    }
}

- (void)clearShadow {
    if (self.shadowRadius > 0) {
        self.shadowRadius = 0;
        self.shadowColor = [UIColor clearColor].CGColor;
        self.shadowPath = NULL;
        self.masksToBounds = self.cachedMaskToBounds;
    }
}

/// 更新遮罩的 path
- (void)updateMaskLayerPathIfNeeded {
    CGRect currentBounds = self.bounds;
    if (!CGRectEqualToRect(self.maskBounds, currentBounds)) {
        self.maskBounds = self.bounds;
        UIBezierPath *shadowPath = [UIBezierPath bezierPathWithRoundedRect:currentBounds cornerRadius:self.ktShadowRadius];
        self.shadowPath = shadowPath.CGPath;
    }
}

#pragma mark - lookin debug
- (NSDictionary *)lookin_customDebugInfos {
    return @{
        @"title" : @"ViewLayer",
        @"properties" : @[ @{
            @"title" : @"ViewLayer",
            @"valueType" : @"string", // 固定为这个就行
            @"section" : @"Layer 信息",
            @"value" : [NSString stringWithFormat:@"%@", self]
        } ]
    };
}

@end
