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

#import "TMMNativeBezierPathUtil.h"
#import "TMMDrawUtils.h"
#import "TMMNativeRoundRect.h"

UIBezierPath *TMMCreatRoundedRectPathWithComposeRoundRect(TMMNativeRoundRect *roundRect) {
    return TMMCreatRoundedRectPathWithRoundRectParams(
        roundRect.left, roundRect.top, roundRect.right, roundRect.bottom, roundRect.topLeftCornerRadiusX, roundRect.topLeftCornerRadiusY,
        roundRect.topRightCornerRadiusX, roundRect.topRightCornerRadiusY, roundRect.bottomLeftCornerRadiusX, roundRect.bottomLeftCornerRadiusY,
        roundRect.bottomRightCornerRadiusX, roundRect.bottomRightCornerRadiusY);
}

UIBezierPath *TMMCreatRoundedRectPathWithRoundRectParams(float left, float top, float right, float bottom, float topLeftCornerRadiusX,
                                                         float topLeftCornerRadiusY, float topRightCornerRadiusX, float topRightCornerRadiusY,
                                                         float bottomLeftCornerRadiusX, float bottomLeftCornerRadiusY, float bottomRightCornerRadiusX,
                                                         float bottomRightCornerRadiusY) {
    const float density = TMMComposeCoreDeviceDensity();
    const float topLeftRadius = topLeftCornerRadiusX / density;
    const float topRightRadius = topRightCornerRadiusX / density;
    const float bottomLeftRadius = bottomLeftCornerRadiusX / density;
    const float bottomRightRadius = bottomRightCornerRadiusX / density;

    const float width = ABS((right - left) / density);
    const float height = ABS((top - bottom) / density);
    const float originX = left / density;
    const float originY = top / density;

    CGRect rect = CGRectMake(originX, originY, width, height);
    UIBezierPath *path = [UIBezierPath bezierPath];

    // Start at the top-left corner
    [path moveToPoint:CGPointMake(rect.origin.x + topLeftRadius, rect.origin.y)];

    // Top edge and top-right corner
    [path addLineToPoint:CGPointMake(rect.origin.x + rect.size.width - topRightRadius, rect.origin.y)];
    [path addArcWithCenter:CGPointMake(rect.origin.x + rect.size.width - topRightRadius, rect.origin.y + topRightRadius)
                    radius:topRightRadius
                startAngle:3 * M_PI / 2
                  endAngle:0
                 clockwise:YES];

    // Right edge and bottom-right corner
    [path addLineToPoint:CGPointMake(rect.origin.x + rect.size.width, rect.origin.y + rect.size.height - bottomRightRadius)];
    [path addArcWithCenter:CGPointMake(rect.origin.x + rect.size.width - bottomRightRadius, rect.origin.y + rect.size.height - bottomRightRadius)
                    radius:bottomRightRadius
                startAngle:0
                  endAngle:M_PI / 2
                 clockwise:YES];

    // Bottom edge and bottom-left corner
    [path addLineToPoint:CGPointMake(rect.origin.x + bottomLeftRadius, rect.origin.y + rect.size.height)];
    [path addArcWithCenter:CGPointMake(rect.origin.x + bottomLeftRadius, rect.origin.y + rect.size.height - bottomLeftRadius)
                    radius:bottomLeftRadius
                startAngle:M_PI / 2
                  endAngle:M_PI
                 clockwise:YES];

    // Left edge and top-left corner
    [path addLineToPoint:CGPointMake(rect.origin.x, rect.origin.y + topLeftRadius)];
    [path addArcWithCenter:CGPointMake(rect.origin.x + topLeftRadius, rect.origin.y + topLeftRadius)
                    radius:topLeftRadius
                startAngle:M_PI
                  endAngle:3 * M_PI / 2
                 clockwise:YES];

    // Close the path
    [path closePath];
    return path;
}
