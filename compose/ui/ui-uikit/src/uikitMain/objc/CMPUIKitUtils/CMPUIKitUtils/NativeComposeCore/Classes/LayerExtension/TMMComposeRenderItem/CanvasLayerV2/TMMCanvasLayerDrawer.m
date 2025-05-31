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

#import "TMMCanvasLayerDrawer.h"
#import "TMMAsyncTaskLayer.h"
#import "TMMCALayerSaveState.h"
#import "TMMCanvasDrawerUitls.h"
#import "TMMComposeMemoryCache.h"
#import "TMMComposeNativeColorFilter.h"
#import "TMMComposeNativePaint.h"
#import "TMMComposeNativePath.h"
#import "TMMDrawUtils.h"
#import "TMMGaussianBlurFilter.h"
#import "TMMImageBitmapUtil.h"
#import "TMMImageDisplayLayer.h"
#import "TMMNativeArcLayer.h"
#import "TMMNativeBasicShader.h"
#import "TMMNativeBezierPathUtil.h"
#import "TMMNativeCircleLayer.h"
#import "TMMNativeComposeGradientLayer.h"
#import "TMMNativeLineGradientLayer.h"
#import "TMMNativeLineLayer.h"
#import "TMMNativeRoundRectLayer.h"
#import "TVComposePointLayer.h"

#define FLOAT_EQUAL(a, b) (ABS(a - b) < 0.00001)

NS_INLINE CAShapeLayer *CALayerGetMask(CALayer *layerForDrawing) {
    CAShapeLayer *maskLayer = layerForDrawing.mask;
    if (![maskLayer isKindOfClass:[CAShapeLayer class]]) {
        maskLayer = [CAShapeLayer layer];
        layerForDrawing.mask = maskLayer;
    }
    return maskLayer;
}

void TMMCALayerDrawClipLayer(float left, float top, float right, float bottom, float topLeftCornerRadiusX, float topLeftCornerRadiusY,
                             float topRightCornerRadiusX, float topRightCornerRadiusY, float bottomLeftCornerRadiusX, float bottomLeftCornerRadiusY,
                             float bottomRightCornerRadiusX, float bottomRightCornerRadiusY, float density, const CALayerSaveState *saveState,
                             CALayer *clipLayer) {
    const CGFloat translateX = saveState->translateX;
    const CGFloat translateY = saveState->translateY;
    const CGRect layerFrame
        = CGRectMake((translateX + left) / density, (translateY + top) / density, (right - left) / density, (bottom - top) / density);
    // 注意：transform 需要在 frame 之前设置
    clipLayer.transform = saveState->transform;
    clipLayer.frame = layerFrame;

    CAShapeLayer *shapeLayer = CALayerGetMask(clipLayer);
    UIBezierPath *path = nil;

    if (FLOAT_EQUAL(topLeftCornerRadiusX, topRightCornerRadiusX) && FLOAT_EQUAL(topRightCornerRadiusX, bottomLeftCornerRadiusX)
        && FLOAT_EQUAL(bottomLeftCornerRadiusX, bottomRightCornerRadiusY)) {
        const float radius = topLeftCornerRadiusX / density;
        /*
         注意所有的 clipLayer 均是以 hostingLayer 为绝对坐标开始计算的，为了不影响 clipLayer 的孩子节点在 hostingLayer
         的展示，内部对 bounds 进行了偏移。偏移后，原本通过 UIBezierPath 进行的裁剪区域，也会被 bounds 进行偏移，因此这里
         UIBezierPath 裁剪的区域需要再进行绝对偏移
         */
        path = [UIBezierPath bezierPathWithRoundedRect:layerFrame cornerRadius:radius];
    } else {
        path = TMMCreatRoundedRectPathWithRoundRectParamsV2(layerFrame, topLeftCornerRadiusX / density, topLeftCornerRadiusY / density,
                                                            topRightCornerRadiusX / density, topRightCornerRadiusY / density,
                                                            bottomLeftCornerRadiusX / density, bottomLeftCornerRadiusY / density,
                                                            bottomRightCornerRadiusX / density, bottomRightCornerRadiusY / density);
    }
    shapeLayer.path = [path CGPath];
}

void TMMCALayerDrawClipLayerWithRect(float left, float top, float right, float bottom, float density, TMMNativeDrawClipOp clipOp,
                                     const CALayerSaveState *saveState, CALayer *clipLayer) {
    const CGFloat translateX = saveState->translateX;
    const CGFloat translateY = saveState->translateY;
    const CGRect layerFrame
        = CGRectMake((translateX + left) / density, (translateY + top) / density, (right - left) / density, (bottom - top) / density);
    // 注意：transform 需要在 frame 之前设置
    clipLayer.transform = saveState->transform;
    clipLayer.frame = layerFrame;

    CAShapeLayer *shapeLayer = CALayerGetMask(clipLayer);
    /*
     注意所有的 clipLayer 均是以 hostingLayer 为绝对坐标开始计算的，为了不影响 clipLayer 的孩子节点在 hostingLayer
     的展示，内部对 bounds 进行了偏移。偏移后，原本通过 UIBezierPath 进行的裁剪区域，也会被 bounds 进行偏移，因此这里
     UIBezierPath 裁剪的区域需要再进行绝对偏移
     */
    UIBezierPath *path = [UIBezierPath bezierPathWithRect:layerFrame];
    shapeLayer.path = [path CGPath];
}

void TMMCALayerDrawClipLayerWithPath(UIBezierPath *path, TMMNativeDrawClipOp clipOp, float density, const CALayerSaveState *saveState,
                                     CALayer *clipLayer) {
    // 目前只支持 TMMNativeDrawClipOpIntersect
    //  先设置 transform，再设置 frame
    clipLayer.transform = saveState->transform;

    const CGRect bounds = [path bounds];
    const CGFloat originX = (saveState->translateX + bounds.origin.x) / density;
    const CGFloat originY = (saveState->translateY + bounds.origin.y) / density;
    const CGFloat width = bounds.size.width / density;
    const CGFloat height = bounds.size.height / density;

    clipLayer.frame = CGRectMake(originX, originY, width, height);
    CAShapeLayer *shapeLayer = CALayerGetMask(clipLayer);
    shapeLayer.path = [path CGPath];
}

// 线条绘制
void TMMCALayerDrawLine(CGFloat pointX1, CGFloat pointY1, CGFloat pointX2, CGFloat pointY2, float density, TMMComposeNativePaint *paint,
                        TMMNativeBasicShader *shader, const CALayerSaveState *saveState, CALayer *layerForDrawing) {
    const CGFloat originX = saveState->translateX;
    const CGFloat originY = saveState->translateY;
    const CGFloat layerWidht = fabs(pointX2 - pointX1);
    const CGFloat layerHeight = fabs(pointY2 - pointY1);
    const CGRect layerFrame = CGRectMake((originX + MIN(pointX1, pointX2)) / density, (originY + MIN(pointY1, pointY2)) / density,
                                         layerWidht / density, layerHeight / density);
    // 注意：transform 需要在 frame 之前设置
    layerForDrawing.transform = saveState->transform;
    layerForDrawing.frame = layerFrame;
    if (!shader) {
        [(TMMNativeLineLayer *)layerForDrawing drawWithPointX1:pointX1
                                                       pointY1:pointY1
                                                       pointX2:pointX2
                                                       pointY2:pointY2
                                                     lineWidth:[paint strokeWidth]
                                                     lineColor:[paint colorFromColorValue]
                                                     strokeCap:[paint strokeCap]
                                                       density:density];
    } else {
        [(TMMNativeLineGradientLayer *)layerForDrawing drawWithPointX1:pointX1
                                                               pointY1:pointY1
                                                               pointX2:pointX2
                                                               pointY2:pointY2
                                                             lineWidth:[paint strokeWidth]
                                                                shader:(TMMNativeLinearGradientShader *)shader
                                                             strokeCap:[paint strokeCap]];
    }
}

// 矩形绘制
void TMMCALayerDrawRect(float left, float top, float right, float bottom, float density, TMMComposeNativePaint *paint, TMMNativeBasicShader *shader,
                        const CALayerSaveState *saveState, CALayer *layerForDrawing) {
    const CGFloat translateX = saveState->translateX;
    const CGFloat translateY = saveState->translateY;

    const float strokeWidth = paint.strokeWidth;
    const CGRect layerFrame = CGRectMake((translateX + left - strokeWidth / 2) / density, (translateY + top - strokeWidth / 2) / density,
                                         (right - left + strokeWidth) / density, (bottom - top + strokeWidth) / density);
    // 注意：transform 需要在 frame 之前设置
    layerForDrawing.transform = saveState->transform;
    layerForDrawing.frame = layerFrame;
    if (layerForDrawing.cornerRadius > 0) {
        layerForDrawing.cornerRadius = 0;
    }
    if (!shader) {
        if (layerForDrawing.mask) {
            layerForDrawing.mask = nil;
        }
        const UIColor *paintColor = [paint colorFromColorValue];
        // 如果是 Stroke 类型
        if (paint.style == TMMNativeDrawPaintingStyleStroke) {
            layerForDrawing.borderWidth = strokeWidth / density;
            layerForDrawing.borderColor = paintColor.CGColor;
            layerForDrawing.backgroundColor = [UIColor clearColor].CGColor;
        } else {
            // 如果是 FILL 类型，则填充 Layer
            if (layerForDrawing.borderWidth > 0.1) {
                layerForDrawing.borderWidth = 0;
            }
            layerForDrawing.backgroundColor = paintColor.CGColor;
        }
    } else {
        [(TMMNativeComposeGradientLayer *)layerForDrawing applyShader:shader];

        if (paint.style == TMMNativeDrawPaintingStyleStroke) {
            UIBezierPath *fullPath = [UIBezierPath bezierPathWithRect:layerForDrawing.bounds];

            CGRect innerRect = CGRectMake((left + strokeWidth / 2) / density, (top + strokeWidth / 2) / density,
                                          (right - left - strokeWidth) / density, (bottom - top - strokeWidth) / density);
            UIBezierPath *innerRectPath = [UIBezierPath bezierPathWithRect:innerRect];

            [fullPath appendPath:innerRectPath];
            fullPath.usesEvenOddFillRule = YES;

            CAShapeLayer *maskLayer = CALayerGetMask(layerForDrawing);
            maskLayer.path = fullPath.CGPath;
            maskLayer.fillColor = [UIColor blackColor].CGColor;
            maskLayer.fillRule = kCAFillRuleEvenOdd;
        }
    }
}

// 圆角矩形
void TMMCALayerDrawRoundRect(float left, float top, float right, float bottom, float radiusX, float radiusY, float density,
                             TMMComposeNativePaint *paint, TMMNativeBasicShader *shader, const CALayerSaveState *saveState,
                             CALayer *layerForDrawing) {
    const CGFloat translateX = saveState->translateX;
    const CGFloat translateY = saveState->translateY;
    const float strokeWidth = paint.strokeWidth;
    const UIColor *paintColor = [paint colorFromColorValue];
    const CGRect layerFrame = CGRectMake((translateX + left - strokeWidth / 2) / density, (translateY + top - strokeWidth / 2) / density,
                                         (right - left + strokeWidth) / density, (bottom - top + strokeWidth) / density);
    // 注意：transform 需要在 frame 之前设置
    layerForDrawing.transform = saveState->transform;
    layerForDrawing.frame = layerFrame;
    // 对比宽高，找到允许的最大半径  圆角计算最大半径应该加上strokeWidth
    float maxRadius = MIN(((bottom - top + strokeWidth) / 2 / density), (right - left + strokeWidth) / 2 / density);
    // 对比Radius和maxRadius，找到最小的Radius，避免Radius比宽高一半还大，导致样式与Skia不同的问题
    // radiusX在 Compose 侧已经收缩了边框的一半，CALayer在切圆角时是需要加上边框的，所以使用的应该是 radiusX + strokeWidth / 2
    // 注：会收缩一半的前提是 原 corner.x >= strokeWidth / 2,否则是Fill绘制

    if (!shader) {
        const float radius = MIN(((radiusX + strokeWidth / 2) / density), maxRadius);
        if (!FLOAT_EQUAL(layerForDrawing.cornerRadius, radius)) {
            layerForDrawing.cornerRadius = radius;
        }
        if (layerForDrawing.mask) {
            layerForDrawing.mask = nil;
        }
        // 如果是 Stroke 类型
        if (paint.style == TMMNativeDrawPaintingStyleStroke) {
            const float borderWidth = strokeWidth / density;
            layerForDrawing.borderWidth = borderWidth;
            layerForDrawing.borderColor = paintColor.CGColor;
            // layer 可能有复用，这里需要清除一下 color，否则会导致仅仅绘制 border 的时候，带了背景色
            layerForDrawing.backgroundColor = [UIColor clearColor].CGColor;
        } else {
            // 如果是 FILL 类型，则填充Layer
            layerForDrawing.borderWidth = 0;
            layerForDrawing.backgroundColor = paintColor.CGColor;
        }
    } else {
        const float radius = MIN(((radiusX + strokeWidth / 2) / density), maxRadius);
        [(TMMNativeComposeGradientLayer *)layerForDrawing applyShader:shader];
        if (!FLOAT_EQUAL(layerForDrawing.cornerRadius, radius)) {
            layerForDrawing.cornerRadius = radius;
        }
        if (paint.style == TMMNativeDrawPaintingStyleStroke) {
            UIBezierPath *fullPath = [UIBezierPath bezierPathWithRect:layerForDrawing.bounds];

            CGRect innerRect = CGRectMake((left + strokeWidth / 2) / density, (top + strokeWidth / 2) / density,
                                          (right - left - strokeWidth) / density, (bottom - top - strokeWidth) / density);
            UIBezierPath *innerRoundedPath = [UIBezierPath bezierPathWithRoundedRect:innerRect cornerRadius:radius];

            [fullPath appendPath:innerRoundedPath];
            fullPath.usesEvenOddFillRule = YES;

            CAShapeLayer *maskLayer = CALayerGetMask(layerForDrawing);
            maskLayer.path = fullPath.CGPath;
            maskLayer.fillColor = [UIColor blackColor].CGColor;
            maskLayer.fillRule = kCAFillRuleEvenOdd;
        }
    }
}

// 椭圆绘制
void TMMCALayerDrawOval(float left, float top, float right, float bottom, float density, TMMComposeNativePaint *paint,
                        const CALayerSaveState *saveState, CALayer *layerForDrawing) {
    // 之前也为空
}

// 圆形绘制
void TMMCALayerDrawCircle(float centerX, float centerY, float radius, float density, TMMNativeBasicShader *shader, TMMComposeNativePaint *paint,
                          const CALayerSaveState *saveState, CALayer *hostingLayer, CALayer *layerForDrawing) {
    const CGFloat originX = saveState->translateX;
    const CGFloat originY = saveState->translateY;
    // TODO review 这里的  circle 的 originWidth 应该是有问题的
    const CGFloat originWidth = CGRectGetWidth(hostingLayer.frame);
    const CGFloat originHeight = CGRectGetHeight(hostingLayer.frame);

    const CGFloat x = originX / density;
    const CGFloat y = originY / density;
    const CGRect layerFrame = CGRectMake(x, y, originWidth - x, originHeight - y);
    // 注意：transform 需要在 frame 之前设置
    layerForDrawing.transform = saveState->transform;
    layerForDrawing.frame = layerFrame;
    if (!shader) {
        [(TMMNativeCircleLayer *)layerForDrawing drawWithCenterX:centerX
                                                         centerY:centerY
                                                          radius:radius
                                                      paintColor:[paint colorFromColorValue]
                                                      paintStyle:[paint style]
                                                     strokeWidth:[paint strokeWidth]];
    } else {
        [(TMMNativeComposeGradientLayer *)layerForDrawing applyShader:shader];
        // 初始化圆形遮罩
        CAShapeLayer *circleMask = CALayerGetMask(layerForDrawing);

        // 创建一个圆形路径
        UIBezierPath *circlePath = [UIBezierPath bezierPathWithOvalInRect:CGRectMake(0, 0, radius * 2 / density, radius * 2 / density)];

        // 设置 mask 的路径
        circleMask.path = circlePath.CGPath;

        // 设置居中 position
        circleMask.position = CGPointMake(x, y);
    }
}

// 圆弧绘制
void TMMCALayerDrawArc(float left, float top, float right, float bottom, float density, float startAngle, float sweepAngle, bool useCenter,
                       TMMComposeNativePaint *paint, const CALayerSaveState *saveState, CALayer *layerForDrawing) {
    const CGFloat originX = saveState->translateX;
    const CGFloat originY = saveState->translateY;
    const float strokeWidth = [paint strokeWidth] / density;
    const CGRect layerFrame = CGRectMake((originX + left - strokeWidth / 2) / density, (originY + top - strokeWidth / 2) / density,
                                         (right - left + strokeWidth) / density, (bottom - top + strokeWidth) / density);
    [(TMMNativeArcLayer *)layerForDrawing updateArc:layerFrame.size.width
                                             height:layerFrame.size.height
                                         startAngle:startAngle
                                         sweepAngle:sweepAngle
                                          useCenter:useCenter
                                              color:[paint colorFromColorValue]
                                        strokeWidth:strokeWidth
                                            density:density];
    // 注意：transform 需要在 frame 之前设置
    layerForDrawing.transform = saveState->transform;
    layerForDrawing.frame = layerFrame;
}

// 路径绘制
void TMMCALayerDrawPath(UIBezierPath *path, TMMNativeBasicShader *shader, TMMComposeNativePaint *paint, TMMNativeDrawPathOperation pathOperation,
                        float density, const CALayerSaveState *saveState, CALayer *hostingLayer, CALayer *layerForDrawing) {
    const CGFloat originX = saveState->translateX / density;
    ;
    const CGFloat originY = saveState->translateY / density;
    ;
    const CGFloat width = CGRectGetWidth(hostingLayer.frame) - originX;
    const CGFloat height = CGRectGetHeight(hostingLayer.frame) - originY;
    const CGRect layerFrame = CGRectMake(originX, originY, width, height);
    // 注意：transform 需要在 frame 之前设置
    layerForDrawing.transform = saveState->transform;
    layerForDrawing.frame = layerFrame;
    if (!shader) {
        [(TMMNativeRoundRectLayer *)layerForDrawing updateUIBezierPath:path
                                                           strokeWidth:[paint strokeWidth] / density
                                                                 color:[paint colorFromColorValue]
                                                             strokeCap:[paint strokeCap]
                                                         pathOperation:pathOperation];
    } else {
        [(TMMNativeComposeGradientLayer *)layerForDrawing applyShader:shader];
        if (paint.style == TMMNativeDrawPaintingStyleFill) {
            CAShapeLayer *maskLayer = CALayerGetMask(layerForDrawing);
            if (pathOperation == TMMNativeDrawPathOperationDifference) {
                // 如果pathOperation为Diffrence，则设置fillRule，对重叠区域进行裁剪
                maskLayer.fillRule = kCAFillRuleEvenOdd;
            } else {
                maskLayer.fillRule = kCAFillRuleNonZero;
            }
            maskLayer.path = path.CGPath;
            maskLayer.fillColor = [UIColor blackColor].CGColor;
        }
    }
}

// 图像绘制
void TMMCALayerDrawImage(intptr_t imagePointer, float topLeftOffsetX, float topLeftOffsetY, float density, TMMComposeNativePaint *paint,
                         const CALayerSaveState *saveState, CALayer *layerForDrawing) { }

// 文字位图绘制
void TMMCALayerDrawTextSkBitmap(intptr_t skBitmap, int32_t cacheKey, int width, int height, float density, const CALayerSaveState *saveState,
                                CALayer *layerForDrawing) {
    const float originX = saveState->translateX / density;
    const float originY = saveState->translateY / density;
    const float layerWidth = ceil(width / density);
    const float layerHeight = ceil(height / density);

    UIImage *image = TMMNativeComposeUIImageFromSkBitmap(skBitmap, cacheKey);
    CGRect layerForDrawingFrame = CGRectMake(originX, originY, layerWidth, layerHeight);

    layerForDrawing.transform = saveState->transform;
    layerForDrawing.frame = layerForDrawingFrame;
    layerForDrawing.contentsGravity = kCAGravityTop;
    layerForDrawing.contentsScale = density;
    layerForDrawing.contents = (__bridge id)image.CGImage;
}

void TMMCALayerDrawTextSkBitmapWithUIImagePtr(intptr_t imagePtr, int width, int height, float density, const CALayerSaveState *saveState,
                                              CALayer *layerForDrawing) {
    const CGFloat translateX = saveState->translateX / density;
    const CGFloat translateY = saveState->translateY / density;

    // 注意：transform 需要在 frame 之前设置
    layerForDrawing.transform = saveState->transform;
    layerForDrawing.frame = CGRectMake(translateX, translateY, ceil(width / density), ceil(height / density));
    CFTypeRef cfImageRef = (CFTypeRef)imagePtr;
    UIImage *image = (__bridge UIImage *)cfImageRef;
    if (image) {
        layerForDrawing.contentsGravity = kCAGravityTop;
        layerForDrawing.contents = (__bridge id)image.CGImage;
        layerForDrawing.contentsScale = density;
        // 下面一行 release 对应着 TMMNativeComposeHasTextImageCache() 函数的 retain
        CFRelease(cfImageRef);
    } else {
        layerForDrawing.contents = nil;
    }
}

// 图像矩形绘制
void TMMCALayerDrawImageRect(intptr_t imagePointer, float srcOffsetX, float srcOffsetY, int srcSizeWidth, int srcSizeHeight, float dstOffsetX,
                             float dstOffsetY, int dstSizeWidth, int dstSizeHeight, float density, TMMComposeNativeColorFilter *colorFilter,
                             TMMGaussianBlurFilter *blurFilter, TMMComposeNativePaint *paint, const CALayerSaveState *saveState,
                             CALayer *layerForDrawing) {
    const CGFloat originX = (saveState->translateX + dstOffsetX) / density;
    const CGFloat originY = (saveState->translateY + dstOffsetY) / density;
    const CGRect layerFrame = CGRectMake(originX, originY, dstSizeWidth / density, dstSizeHeight / density);

    TMMImageDisplayLayer *imageDisplayLayer = (TMMImageDisplayLayer *)layerForDrawing;
    CGImageRef image = (CGImageRef)imagePointer;
    [imageDisplayLayer setImage:image
                      srcOffset:CGPointMake(srcOffsetX, srcOffsetY)
                        srcSize:CGSizeMake(srcSizeWidth, srcSizeHeight)
                      dstOffset:CGPointMake(dstOffsetX, dstOffsetY)
                        dstSize:CGSizeMake(dstSizeWidth, dstSizeHeight)
                    colorFilter:colorFilter
                     blurFilter:blurFilter
                          paint:paint
                        density:density];
    // 注意：transform 需要在 frame 之前设置
    // 还需要处理 transform，layerForDrawing.transform 在设置图片的时候会被自己内部修改，因此这里要合并两个transform
    layerForDrawing.transform = CATransform3DConcat(layerForDrawing.transform, saveState->transform);
    layerForDrawing.frame = layerFrame;
}

void TMMCALayerDrawTextAsyncTask(TMMNativeOneResultBlock globalTask, int32_t cacheKey, int width, int height, float density,
                                 const CALayerSaveState *saveState, CALayer *layerForDrawing) {
    const CGFloat translateX = saveState->translateX;
    const CGFloat translateY = saveState->translateY;
    // 注意：transform 需要在 frame 之前设置
    layerForDrawing.transform = saveState->transform;
    layerForDrawing.frame = CGRectMake(translateX, translateY, ceil(width / density), ceil(height / density));
    [(TMMAsyncTaskLayer *)layerForDrawing commitAsyncTask:globalTask density:density];
}

static CGRect getLayerFrameFromArray(NSArray<NSNumber *> *points, CGFloat translateX, CGFloat translateY, CGFloat strokeWidth) {
    if (points.count < 2) {
        return CGRectMake(0, 0, 0, 0);
    }
    const CGFloat density = TMMComposeCoreDeviceDensity();
    CGFloat minX, minY, maxX, maxY;
    minX = maxX = [points[0] floatValue];
    minY = maxY = [points[1] floatValue];
    for (NSUInteger i = 0; i < points.count && i + 1 < points.count; i += 2) {
        CGFloat x = points[i].floatValue;
        CGFloat y = points[i + 1].floatValue;

        if (x < minX) {
            minX = x;
        } else if (x > maxX) {
            maxX = x;
        }

        if (y < minY) {
            minY = y;
        } else if (y > maxY) {
            maxY = y;
        }
    }

    return CGRectMake((translateX - strokeWidth / 2) / density, (translateY - strokeWidth / 2) / density, (maxX - minX + strokeWidth) / density,
                      (maxY - minY + strokeWidth) / density);
}

// 原始layerForDrawing.transform = saveState->transform;点绘制
void TMMCALayerDrawRawPoints(NSArray<NSNumber *> *points, TMMComposeNativePaint *paint, float density, const CALayerSaveState *saveState,
                             CALayer *layerForDrawing) {
    float strokeSize = paint.strokeWidth;
    UIColor *color = [paint colorFromColorValue];
    TMMNativeDrawStrokeCap strokeCap = paint.strokeCap;
    const CGRect layerFrame = getLayerFrameFromArray(points, saveState->translateX, saveState->translateY, paint.strokeWidth);

    [(TVComposePointLayer *)layerForDrawing updatePoints:points strokeSize:strokeSize color:color strokeCap:strokeCap];

    layerForDrawing.frame = layerFrame;
    layerForDrawing.drawsAsynchronously = NO;
    [layerForDrawing setNeedsDisplay];
}
