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

#import "TMMCanvasDrawerUitls.h"

UIBezierPath *_Nullable TMMCreatRoundedRectPathWithRoundRectParamsV2(const CGRect rect, const float topLeftCornerRadiusX,
                                                                     const float topLeftCornerRadiusY, const float topRightCornerRadiusX,
                                                                     const float topRightCornerRadiusY, const float bottomLeftCornerRadiusX,
                                                                     const float bottomLeftCornerRadiusY, const float bottomRightCornerRadiusX,
                                                                     const float bottomRightCornerRadiusY) {
    const float width = rect.size.width;
    const float height = rect.size.height;
    const float originX = rect.origin.x;
    const float originY = rect.origin.y;

    const float topLeftRadius = topLeftCornerRadiusX;
    const float topRightRadius = topRightCornerRadiusX;
    const float bottomLeftRadius = bottomLeftCornerRadiusX;
    const float bottomRightRadius = bottomRightCornerRadiusX;

    if (width <= 0 || height <= 0) {
        return nil;
    }

    UIBezierPath *path = [UIBezierPath bezierPath];
    // Start at the top-left corner
    [path moveToPoint:CGPointMake(originX + topLeftRadius, originY)];

    // Top edge and top-right corner
    [path addLineToPoint:CGPointMake(originX + width - topRightRadius, originY)];
    [path addArcWithCenter:CGPointMake(originX + width - topRightRadius, originY + topRightRadius)
                    radius:topRightRadius
                startAngle:3 * M_PI / 2
                  endAngle:0
                 clockwise:YES];

    // Right edge and bottom-right corner
    [path addLineToPoint:CGPointMake(originX + width, originY + height - bottomRightRadius)];
    [path addArcWithCenter:CGPointMake(originX + width - bottomRightRadius, originY + height - bottomRightRadius)
                    radius:bottomRightRadius
                startAngle:0
                  endAngle:M_PI / 2
                 clockwise:YES];

    // Bottom edge and bottom-left corner
    [path addLineToPoint:CGPointMake(originX + bottomLeftRadius, originY + height)];
    [path addArcWithCenter:CGPointMake(originX + bottomLeftRadius, originY + height - bottomLeftRadius)
                    radius:bottomLeftRadius
                startAngle:M_PI / 2
                  endAngle:M_PI
                 clockwise:YES];

    // Left edge and top-left corner
    [path addLineToPoint:CGPointMake(originX, originY + topLeftRadius)];
    [path addArcWithCenter:CGPointMake(originX + topLeftRadius, originY + topLeftRadius)
                    radius:topLeftRadius
                startAngle:M_PI
                  endAngle:3 * M_PI / 2
                 clockwise:YES];

    // Close the path
    [path closePath];
    return path;
}
