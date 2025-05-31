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

#import "TMMComposeNativePaint.h"

@interface TMMComposeNativePaint ()
@end

@implementation TMMComposeNativePaint

- (void)setColorValue:(uint64_t)colorValue {
    _colorValue = colorValue;
}

- (UIColor *)colorFromColorValue {
    return [UIColor colorWithRed:((_colorValue >> 48) & 0xff) / 255.0f
                           green:((_colorValue >> 40) & 0xff) / 255.0f
                            blue:((_colorValue >> 32) & 0xff) / 255.0f
                           alpha:((_colorValue >> 56) & 0xff) / 255.0f];
}

- (void)prepareForReuse {
    // 以下属性重设为 TMMComposeNativePaint 初始化时的属性值
    _alpha = 0.0f;
    _colorValue = 0;
    _blendMode = TMMNativeDrawBlendModeClear;
    _strokeWidth = 0;
    _strokeCap = TMMNativeDrawStrokeCapButt;
    _strokeJoin = TMMNativeDrawStrokeJoinMiter;
    _strokeWidth = 0;
    _shader = nil;
    _pathEffect = nil;
    _colorFilter = nil;
    _isAntiAlias = YES;
    _filterQuality = TMMNativeDrawFilterQualityNone;
}

@end
