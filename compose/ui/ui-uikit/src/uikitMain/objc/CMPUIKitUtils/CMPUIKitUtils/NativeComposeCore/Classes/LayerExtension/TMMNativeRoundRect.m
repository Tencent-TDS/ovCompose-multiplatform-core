/*
 * Tencent is pleased to support the open source community by making ovCompose
 * available. Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights
 * reserved.
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

#import "TMMNativeRoundRect.h"
#import "TMMDrawUtils.h"

@implementation TMMNativeRoundRect

- (NSUInteger)dataHash {
    const float floats[12] = {
        _left,
        _top,
        _right,
        _bottom,
        _topLeftCornerRadiusX,
        _topLeftCornerRadiusY,
        _topRightCornerRadiusX,
        _topRightCornerRadiusY,
        _bottomRightCornerRadiusX,
        _bottomRightCornerRadiusY,
        _bottomLeftCornerRadiusX,
        _bottomLeftCornerRadiusY,
    };
    return TMMFNVHash(floats, sizeof(floats));
}

- (BOOL)containsWithPointX:(float)pointX pointY:(float)pointY {
    return NO;
}

- (NSString *)description {
    return [NSString stringWithFormat:@"<RoundRect:%p left:%f right:%f top:%f bottom:%f ...>", self, self.left, self.right, self.top, self.bottom];
}

@end
