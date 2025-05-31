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

#import <UIKit/UIKit.h>
#import "TMMComposeNativePaint.h"
#import "TMMNativeEnums.h"

NS_ASSUME_NONNULL_BEGIN

@class TMMComposeNativePath;
@class TMMNativeComposeMatrix;

typedef intptr_t (^TMMNativeOneResultBlock)(void);

@protocol ITMMNativePictureRecorder <NSObject>

- (void)drawLayer:(CALayer *)layer;

- (void)drawLine:(float)pointX1 pointY1:(float)pointY1 pointX2:(float)pointX2 pointY2:(float)pointY2 paint:(TMMComposeNativePaint *)paint;

- (void)drawRect:(float)left top:(float)top right:(float)right bottom:(float)bottom paint:(TMMComposeNativePaint *)paint;

- (void)drawRoundRect:(float)left
                  top:(float)top
                right:(float)right
               bottom:(float)bottom
              radiusX:(float)radiusX
              radiusY:(float)radiusY
                paint:(TMMComposeNativePaint *)paint;

- (void)drawOval:(float)left top:(float)top right:(float)right bottom:(float)bottom paint:(TMMComposeNativePaint *)paint;

- (void)drawCircle:(float)centerX centerY:(float)centerY radius:(float)radius paint:(TMMComposeNativePaint *)paint;

- (void)drawArc:(float)left
            top:(float)top
          right:(float)right
         bottom:(float)bottom
     startAngle:(float)startAngle
     sweepAngle:(float)sweepAngle
      useCenter:(BOOL)useCenter
          paint:(TMMComposeNativePaint *)paint;

- (void)drawPath:(TMMComposeNativePath *)path paint:(TMMComposeNativePaint *)paint;

- (void)drawImageRect:(void *)imagePointer
           srcOffsetX:(float)srcOffsetX
           srcOffsetY:(float)srcOffsetY
         srcSizeWidth:(int)srcSizeWidth
        srcSizeHeight:(int)srcSizeHeight
           dstOffsetX:(float)dstOffsetX
           dstOffsetY:(float)dstOffsetY
         dstSizeWidth:(int)dstSizeWidth
        dstSizeHeight:(int)dstSizeHeight
                paint:(TMMComposeNativePaint *)paint;

/// 对 Skbitmap 的文本图片绘制
/// - Parameters:
///   - skBitmap: skbitmap 的指针
///   - cacheKey: 缓存 key
///   - width: 图片的宽度
///   - height: 图片的高度
- (void)drawTextSkBitmap:(intptr_t)skBitmap cacheKey:(int32_t)cacheKey width:(int)width height:(int)height;

/// 根据缓存 key 对文本进行绘制
/// - Parameters:
///   - cacheKey: 缓存 key
///   - width: 图片的宽度
///   - height: 图片的高度
- (void)drawTextSkBitmapWithUIImagePtr:(intptr_t)imagePtr width:(int)width height:(int)height;

/// 异步执行文本的绘制任务
/// - Parameters:
///   - globalTask: 由 Kt 侧传入的任务，将会在异步执行
///   - cacheKey: 文本的缓存 cache
///   - width: 文本图片的宽度
///   - height: 文本图片的高度
- (void)asyncDrawIntoCanvas:(TMMNativeOneResultBlock)globalTask cacheKey:(int32_t)cacheKey width:(int)width height:(int)height;

/// 根据 Skia 的 bitmap 的 ptr 获取一个 UIImage
/// - Parameters:
///   - imageBitmap: imageBitmap
///   - cacheKey: 文本的缓存 cache
- (intptr_t)imageFromImageBitmap:(intptr_t)imageBitmap cacheKey:(int32_t)cacheKey;

- (void)drawRawPoints:(NSArray<NSNumber *> *)points paint:(TMMComposeNativePaint *)paint;

- (void)drawVertices:(id)vertices blendMode:(TMMNativeDrawBlendMode)blendMode paint:(TMMComposeNativePaint *)paint;

- (void)beginDraw;

- (void)finishDraw;

- (void)restore;

- (void)save;

@end

NS_ASSUME_NONNULL_END
