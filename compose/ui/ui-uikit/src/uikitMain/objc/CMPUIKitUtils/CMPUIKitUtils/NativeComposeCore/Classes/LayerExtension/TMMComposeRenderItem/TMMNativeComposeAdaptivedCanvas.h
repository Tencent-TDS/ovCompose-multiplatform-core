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

#import <Foundation/Foundation.h>
#import "TMMNativeEnums.h"
#import "TMMComposeNativePaint.h"
#import "ITMMNativePictureRecorder.h"

NS_ASSUME_NONNULL_BEGIN

@class TMMComposeNativePath;
@class TMMNativeComposeMatrix;

/// 该接口用于适配 Compose 的 Canvas
@protocol TMMNativeComposeAdaptivedCanvas <ITMMNativePictureRecorder>

- (void)translate:(float)dx dy:(float)dy;

- (void)scale:(float)sx sy:(float)sy;

- (void)rotate:(float)degrees;

- (void)skew:(float)sx sy:(float)sy;

- (void)concat:(nullable TMMNativeComposeMatrix *)matrix;

- (void)clipRoundRect:(float)left
                         top:(float)top
                       right:(float)right
                      bottom:(float)bottom
        topLeftCornerRadiusX:(float)topLeftCornerRadiusX
        topLeftCornerRadiusY:(float)topLeftCornerRadiusY
       topRightCornerRadiusX:(float)topRightCornerRadiusX
       topRightCornerRadiusY:(float)topRightCornerRadiusY
     bottomLeftCornerRadiusX:(float)bottomLeftCornerRadiusX
     bottomLeftCornerRadiusY:(float)bottomLeftCornerRadiusY
    bottomRightCornerRadiusX:(float)bottomRightCornerRadiusX
    bottomRightCornerRadiusY:(float)bottomRightCornerRadiusY;

- (void)clipRect:(float)left top:(float)top right:(float)right bottom:(float)bottom clipOp:(TMMNativeDrawClipOp)clipOp;

- (void)clipPath:(TMMComposeNativePath *)path clipOp:(TMMNativeDrawClipOp)clipOp;

- (void)blur:(float)radiusX radiusY:(float)radiusY;

- (void)enableZ;

- (void)disableZ;

/// ⚠️⚠️⚠️ 重要： Kt 侧 restore 后可以直接从 clipRect 变为 clipRoundRect，不同类型是互斥的
- (void)clearClip;

@end

NS_ASSUME_NONNULL_END
