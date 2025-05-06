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

#import "TMMNativeRadialGradientShader.h"
#import "TMMDrawUtils.h"

@interface TMMNativeRadialGradientShader ()

/// 渐变颜色
@property (nonatomic, strong, readwrite) NSMutableArray<id> *colors;

/// 渐变停止点位
@property (nonatomic, strong) NSMutableArray<NSNumber *> *privateColorStops;

@end

@implementation TMMNativeRadialGradientShader

- (void)setCenter:(float)centerX centerY:(float)centerY {
    _center = CGPointMake(centerX, centerY);
}

- (void)setRadius:(float)radius {
    _radius = radius;
}

- (void)addColor:(float)colorRed colorGreen:(float)colorGreen colorBlue:(float)colorBlue colorAlpha:(float)colorAlpha {
    UIColor *color = [UIColor colorWithRed:colorRed green:colorGreen blue:colorBlue alpha:colorAlpha];
    [self.colors addObject:(id)color.CGColor];
}

- (void)addColorStops:(float)colorStop {
    [self.privateColorStops addObject:@(colorStop)];
}

#pragma mark - getters
- (NSMutableArray<id> *)colors {
    if (_colors) {
        return _colors;
    }
    _colors = @[].mutableCopy;
    return _colors;
}

- (NSMutableArray<NSNumber *> *)privateColorStops {
    if (_privateColorStops) {
        return _privateColorStops;
    }
    _privateColorStops = @[].mutableCopy;
    return _privateColorStops;
}

- (NSArray<NSNumber *> *)colorStops {
    return _privateColorStops;
}

- (uint64_t)propertyHash {
    CGFloat floats[6]
        = { (CGFloat)_tileMode, _center.x, _center.y, _radius, TMMComposeCoreHashNSArray(_colors), TMMComposeCoreHashNSArray(_privateColorStops) };
    return TMMFNVHash(floats, sizeof(floats));
}

@end
