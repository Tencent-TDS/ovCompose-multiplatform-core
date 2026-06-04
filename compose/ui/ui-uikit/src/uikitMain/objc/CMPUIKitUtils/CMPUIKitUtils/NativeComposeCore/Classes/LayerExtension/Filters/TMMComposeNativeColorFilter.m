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

#import "TMMComposeNativeColorFilter.h"
#import "TMMDrawUtils.h"

@implementation TMMComposeNativeColorFilter {
    CGFloat _matrix[20];
}

- (const CGFloat *)matrix {
    return _matrix;
}

- (void)updateColorMatrixValues:(float)r0c0 r0c1:(float)r0c1 r0c2:(float)r0c2 r0c3:(float)r0c3 r0c4:(float)r0c4
                           r1c0:(float)r1c0 r1c1:(float)r1c1 r1c2:(float)r1c2 r1c3:(float)r1c3 r1c4:(float)r1c4
                           r2c0:(float)r2c0 r2c1:(float)r2c1 r2c2:(float)r2c2 r2c3:(float)r2c3 r2c4:(float)r2c4
                           r3c0:(float)r3c0 r3c1:(float)r3c1 r3c2:(float)r3c2 r3c3:(float)r3c3 r3c4:(float)r3c4 {
    _matrix[0] = r0c0;
    _matrix[1] = r0c1;
    _matrix[2] = r0c2;
    _matrix[3] = r0c3;
    _matrix[4] = r0c4;
    
    _matrix[5] = r1c0;
    _matrix[6] = r1c1;
    _matrix[7] = r1c2;
    _matrix[8] = r1c3;
    _matrix[9] = r1c4;
    
    _matrix[10] = r2c0;
    _matrix[11] = r2c1;
    _matrix[12] = r2c2;
    _matrix[13] = r2c3;
    _matrix[14] = r2c4;
    
    _matrix[15] = r3c0;
    _matrix[16] = r3c1;
    _matrix[17] = r3c2;
    _matrix[18] = r3c3;
    _matrix[19] = r3c4;
}

- (void)setColorFilterInfo:(TMMComposeNativeColorFilter *_Nonnull)colorFilter {
    _type = colorFilter.type;
    _blendMode = colorFilter.blendMode;
    _colorValue = colorFilter.colorValue;

    // 复制初始化值
    memcpy(_matrix, colorFilter->_matrix, 20 * sizeof(CGFloat));
}

- (NSUInteger)hash {
    static CGFloat matrix[20] = { 0 };
    const uint64_t hashMatrix = TMMFNVHash(_matrix, sizeof(matrix));
    const float floats[6] = { (float)_type, (float)_colorValue, (float)_blendMode, (float)_multiply, (float)_add, (float)hashMatrix };
    return TMMFNVHash(floats, sizeof(floats));
}

@end
