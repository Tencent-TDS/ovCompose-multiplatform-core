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

#import "TMMCanvasLayerDrawerV3.h"
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
#import "TMMUIKitPictureRecorder.h"
#import "TMMNativeClipLayer.h"
#import "OVComposeExperimentalConfig.h"
#import "OVMPFastLogCAPI.h"

#define FLOAT_EQUAL(a, b) (ABS(a - b) < 0.00001)

#pragma mark - private
NS_INLINE CAShapeLayer *CALayerGetMask(CALayer *layerForDrawing) {
    CAShapeLayer *maskLayer = layerForDrawing.mask;
    if (![maskLayer isKindOfClass:[CAShapeLayer class]]) {
        maskLayer = [CAShapeLayer layer];
        layerForDrawing.mask = maskLayer;
    }
    return maskLayer;
}

TMM_ALWAYS_INLINE static void CALayerApplyCommonCoordinatesInfo(CALayer *layer, const CALayerSaveState *saveState, float density, CGRect layerFrame) {
    const CGSize newSize = layerFrame.size;
    /*
     step1: 设置锚点，锚点受 translate 的影响
     通常会先 translate(x, y) -> rotate(degree) -> translate(-x, -y)
     1. translate 的值会立刻记录在 save -> translate(m, n) ... -> restore -> saveState->transformInfo.instantlyTranslate
     2. 渲染的元素的位置通过前面的 save -> translate(m, n) ... -> restore 这一过程会最终体现在入参的 layerFrame
     3. 通过入参的 layerFrame 的左上角和 instantlyTranslate 的差值，可计算出 CALayer 的锚点的准确值
     */
    const CGFloat defaultAnchorPointX = 0.5f;
    const CGFloat defaultAnchorPointY = 0.5f;
    CGFloat newAnchorPointX = 0.5f;
    CGFloat newAnchorPointY = 0.5f;
    if (saveState->hasRotateOrScale && newSize.width > 0 && newSize.height > 0) {
        // 计算出 rotate 或者 scale 的元素的准确锚点值
        const CGPoint instantlyTranslate = saveState->transformInfo.instantlyTranslate;
        const CGFloat diffX = instantlyTranslate.x - (layerFrame.origin.x * density);
        const CGFloat diffY = instantlyTranslate.y - (layerFrame.origin.y * density);
        newAnchorPointX = MIN(MAX(diffX / (newSize.width * density), 0), 1);
        newAnchorPointY = MIN(MAX(diffY / (newSize.height * density), 0), 1);
        const CGPoint currentAnchorPoint = layer.anchorPoint;
        if (!FLOAT_EQUAL(currentAnchorPoint.x, newAnchorPointX) ||
            !FLOAT_EQUAL(currentAnchorPoint.y, newAnchorPointY)) {
            layer.anchorPoint = CGPointMake(newAnchorPointX, newAnchorPointY);
        }
    } else {
        // 还原到原始的 (0.5, 0.5)
        const CGPoint currentAnchorPoint = layer.anchorPoint;
        if (!FLOAT_EQUAL(currentAnchorPoint.x, defaultAnchorPointX) ||
            !FLOAT_EQUAL(currentAnchorPoint.y, defaultAnchorPointY)) {
            layer.anchorPoint = CGPointMake(defaultAnchorPointX, defaultAnchorPointY);
        }
    }
    /*
     step2: 设置正确的 position
     layerFrame 是始终以 CGPoint defaultAnchorPoint = CGPointMake(0.5, 0.5) 来计算的。设置新的
     anchorPoint 后需校准 position，让 layer 在父视图中的位置也就是左上角和入参的 layerFrame 的 origin 一致
     核心思想：保持不变的是 frame.origin（图层左上角在父视图中的坐标）。我们利用这个不变性来建立一个等式。
     frame.origin 的计算公式是：
     frame.origin.x = position.x - bounds.width * anchorPoint.x
     frame.origin.y = position.y - bounds.height * anchorPoint.y
     进而推导出
     newPosition.x = oldPosition.x + (newAnchorPoint.x - oldAnchorPoint.x) * bounds.width
     newPosition.y = oldPosition.y + (newAnchorPoint.y - oldAnchorPoint.y) * bounds.height
     */
    const CGFloat oldPositionX = layerFrame.origin.x + (newSize.width / 2.0);
    const CGFloat oldPositionY = layerFrame.origin.y + (newSize.height / 2.0);
    CGFloat newPositionX = oldPositionX + (newAnchorPointX - defaultAnchorPointX) * newSize.width;
    CGFloat newPositionY = oldPositionY + (newAnchorPointY - defaultAnchorPointY) * newSize.height;
    if (!FLOAT_EQUAL(layer.position.x, newPositionX) ||
        !FLOAT_EQUAL(layer.position.y, newPositionY)) {
        layer.position = CGPointMake(newPositionX, newPositionY);
    }
    
    if (!CATransform3DEqualToTransform(saveState->transformInfo.transform, layer.transform)) {
        layer.transform = saveState->transformInfo.transform;
    }
}

TMM_ALWAYS_INLINE static void CAClipLayerApplyCoordinatesInfo(CALayer *layer, const CALayerSaveState *saveState, float density, CGRect layerFrame) {
    // 通过 bounds 设置正确的 width 和 height
    const CGSize newSize = layerFrame.size;
    const CGRect currentBounds = layer.bounds;
    // 注意：这一行至关重要 newBounds 需要根据 layerFrame.origin 进行偏移
    const CGRect newBounds = CGRectMake(layerFrame.origin.x, layerFrame.origin.y, newSize.width, newSize.height);
    if (!CGRectEqualToRect(newBounds, currentBounds)) {
        layer.bounds = newBounds;
    }
    
    CALayerApplyCommonCoordinatesInfo(layer, saveState, density, layerFrame);
}

TMM_ALWAYS_INLINE static void CALayerApplyCoordinatesInfo(CALayer *layer, const CALayerSaveState *saveState, float density, CGRect layerFrame) {
    /*
     通过 bounds 设置正确的 width 和 height
     */
    const CGSize newSize = layerFrame.size;
    const CGRect currentBounds = layer.bounds;
    const CGRect newBounds = CGRectMake(currentBounds.origin.x, currentBounds.origin.y, newSize.width, newSize.height);
    if (!CGRectEqualToRect(newBounds, currentBounds)) {
        layer.bounds = newBounds;
    }
    
    CALayerApplyCommonCoordinatesInfo(layer, saveState, density, layerFrame);
}

TMM_ALWAYS_INLINE static CGRect TMMCreateDrawRoundRectOutAlignedPixelRect(const CGFloat translateX, const CGFloat translateY,
                                                                          const float left, const float top, const float right,
                                                                          const float bottom, const float density, const CGFloat strokeWidth) {
    // 像素对齐 step1: 在像素坐标系中计算出理想的、包含描边的外矩形四条边的坐标
    const CGFloat pixelLeft = translateX + left - strokeWidth / 2.0f;
    const CGFloat pixelTop = translateY + top - strokeWidth / 2.0f;
    const CGFloat pixelRight  = translateX + right + strokeWidth / 2.0f;
    const CGFloat pixelBottom = translateY + bottom + strokeWidth / 2.0f;
    
    // 像素对齐 step2: 将四条边的坐标对齐到最近的整数像素
    const CGFloat alignedPixelLeft = round(pixelLeft);
    const CGFloat alignedPixelTop = round(pixelTop);
    const CGFloat alignedPixelWidth = round(pixelRight - pixelLeft);
    const CGFloat alignedPixelHeight = round(pixelBottom - pixelTop);
    
    // 像素对齐 step3: 计算出，对齐后的像素坐标来创建最终的 layerFrame (单位是点/dp)
    return CGRectMake(alignedPixelLeft / density,
                      alignedPixelTop / density,
                      alignedPixelWidth / density,
                      alignedPixelHeight / density);
}


#pragma mark - public
void TMMCALayerOptimizedDrawClipLayerV3(float left, float top, float right, float bottom, float topLeftCornerRadiusX,
                                        float topLeftCornerRadiusY, float topRightCornerRadiusX, float topRightCornerRadiusY,
                                        float bottomLeftCornerRadiusX, float bottomLeftCornerRadiusY,
                                        float bottomRightCornerRadiusX, float bottomRightCornerRadiusY, float density,
                                        const CALayerSaveState *saveState, CALayer *clipLayer) {
    const CGFloat translateX = saveState->translateX;
    const CGFloat translateY = saveState->translateY;
    const CGRect layerFrame
        = CGRectMake((translateX + left) / density, (translateY + top) / density, (right - left) / density, (bottom - top) / density);
    CAClipLayerApplyCoordinatesInfo(clipLayer, saveState, density, layerFrame);
    
    if (FLOAT_EQUAL(topLeftCornerRadiusX, topRightCornerRadiusX) && FLOAT_EQUAL(topRightCornerRadiusX, bottomLeftCornerRadiusX)
        && FLOAT_EQUAL(bottomLeftCornerRadiusX, bottomRightCornerRadiusY)) {
        const float radius = topLeftCornerRadiusX / density;
        /*
         如果是四个圆角相等裁剪。则直接利用 CALayer 的 cornerRadius 和 masksToBounds，性能好于 CAShapeLayer，
         实测在程序运行过程中绝大多数情况都如此
         */
        if (clipLayer.mask) {
            clipLayer.mask = nil;
        }
        clipLayer.cornerRadius = radius;
        clipLayer.masksToBounds = YES;
    } else {
        CAShapeLayer *shapeLayer = CALayerGetMask(clipLayer);
        UIBezierPath *path = TMMCreatRoundedRectPathWithRoundRectParamsV2(layerFrame, topLeftCornerRadiusX / density, topLeftCornerRadiusY / density,
                                                            topRightCornerRadiusX / density, topRightCornerRadiusY / density,
                                                            bottomLeftCornerRadiusX / density, bottomLeftCornerRadiusY / density,
                                                            bottomRightCornerRadiusX / density, bottomRightCornerRadiusY / density);
        shapeLayer.path = [path CGPath];
    }
}

void TMMCALayerDrawClipLayerV3(float left, float top, float right, float bottom, float topLeftCornerRadiusX, float topLeftCornerRadiusY,
                               float topRightCornerRadiusX, float topRightCornerRadiusY, float bottomLeftCornerRadiusX, float bottomLeftCornerRadiusY,
                               float bottomRightCornerRadiusX, float bottomRightCornerRadiusY, float density, const CALayerSaveState *saveState,
                               CALayer *clipLayer, OVComposeExperimentalConfig *experimentalConfig) {
    const CGFloat translateX = saveState->translateX;
    const CGFloat translateY = saveState->translateY;
    const CGRect layerFrame
        = CGRectMake((translateX + left) / density, (translateY + top) / density, (right - left) / density, (bottom - top) / density);
    CAClipLayerApplyCoordinatesInfo((TMMNativeClipLayer *)clipLayer, saveState, density, layerFrame);

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

void TMMCALayerDrawClipLayerWithRectV3(float left, float top, float right, float bottom, float density, TMMNativeDrawClipOp clipOp,
                                       const CALayerSaveState *saveState, CALayer *clipLayer, OVComposeExperimentalConfig *experimentalConfig) {
    const CGFloat translateX = saveState->translateX;
    const CGFloat translateY = saveState->translateY;
    const CGRect layerFrame
        = CGRectMake((translateX + left) / density, (translateY + top) / density, (right - left) / density, (bottom - top) / density);
    // 注意：transform 需要在 frame 之前设置
    CAClipLayerApplyCoordinatesInfo((TMMNativeClipLayer *)clipLayer, saveState, density, layerFrame);

    CAShapeLayer *shapeLayer = CALayerGetMask(clipLayer);
    /*
     注意所有的 clipLayer 均是以 hostingLayer 为绝对坐标开始计算的，为了不影响 clipLayer 的孩子节点在 hostingLayer
     的展示，内部对 bounds 进行了偏移。偏移后，原本通过 UIBezierPath 进行的裁剪区域，也会被 bounds 进行偏移，因此这里
     UIBezierPath 裁剪的区域需要再进行绝对偏移
     */
    UIBezierPath *path = [UIBezierPath bezierPathWithRect:layerFrame];
    shapeLayer.path = [path CGPath];
}

void TMMCALayerOptimizedDrawClipLayerWithRectV3(float left, float top, float right, float bottom, float density, TMMNativeDrawClipOp clipOp,
                                                const CALayerSaveState *saveState, CALayer *clipLayer) {
    const CGFloat translateX = saveState->translateX;
    const CGFloat translateY = saveState->translateY;
    const CGRect layerFrame
        = CGRectMake((translateX + left) / density, (translateY + top) / density, (right - left) / density, (bottom - top) / density);
    CAClipLayerApplyCoordinatesInfo(clipLayer, saveState, density, layerFrame);
    if (clipLayer.mask) {
        clipLayer.mask = nil;
    }
    if (!clipLayer.masksToBounds) {
        clipLayer.masksToBounds = YES;
    }
}

void TMMCALayerDrawClipLayerWithPathV3(UIBezierPath *path, TMMNativeDrawClipOp clipOp, float density, const CALayerSaveState *saveState,
                                       CALayer *clipLayer, OVComposeExperimentalConfig *experimentalConfig) {
    // 目前只支持 TMMNativeDrawClipOpIntersect
    const CGRect bounds = [path bounds];
    const CGFloat originX = (saveState->translateX + bounds.origin.x) / density;
    const CGFloat originY = (saveState->translateY + bounds.origin.y) / density;
    const CGFloat width = bounds.size.width / density;
    const CGFloat height = bounds.size.height / density;

    CGRect layerFrame = CGRectMake(originX, originY, width, height);
    CAClipLayerApplyCoordinatesInfo((TMMNativeClipLayer *)clipLayer, saveState, density, layerFrame);
    CAShapeLayer *shapeLayer = CALayerGetMask(clipLayer);
    shapeLayer.path = [path CGPath];
}

// 线条绘制
void TMMCALayerDrawLineV3(CGFloat pointX1, CGFloat pointY1, CGFloat pointX2, CGFloat pointY2, float density, TMMComposeNativePaint *paint,
                          TMMNativeBasicShader *shader, const CALayerSaveState *saveState, CALayer *layerForDrawing, OVComposeExperimentalConfig *experimentalConfig) {
    const CGFloat originX = saveState->translateX;
    const CGFloat originY = saveState->translateY;
    const CGFloat layerWidht = fabs(pointX2 - pointX1);
    const CGFloat layerHeight = fabs(pointY2 - pointY1);
    const CGRect layerFrame = CGRectMake((originX + MIN(pointX1, pointX2)) / density, (originY + MIN(pointY1, pointY2)) / density,
                                         layerWidht / density, layerHeight / density);
    // 注意：transform 需要在 frame 之前设置
    CALayerApplyCoordinatesInfo(layerForDrawing, saveState, density, layerFrame);
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
void TMMCALayerDrawRectV3(float left, float top, float right, float bottom, float density, TMMComposeNativePaint *paint, TMMNativeBasicShader *shader,
                          const CALayerSaveState *saveState, CALayer *layerForDrawing, OVComposeExperimentalConfig *experimentalConfig) {
    const CGFloat translateX = saveState->translateX;
    const CGFloat translateY = saveState->translateY;

    const float strokeWidth = paint.strokeWidth;
    const CGRect layerFrame = CGRectMake((translateX + left - strokeWidth / 2) / density, (translateY + top - strokeWidth / 2) / density,
                                         (right - left + strokeWidth) / density, (bottom - top + strokeWidth) / density);
    CALayerApplyCoordinatesInfo(layerForDrawing, saveState, density, layerFrame);
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
void TMMCALayerDrawRoundRectV3(float left, float top, float right, float bottom, float radiusX, float radiusY, float density,
                               TMMComposeNativePaint *paint, TMMNativeBasicShader *shader, const CALayerSaveState *saveState,
                               CALayer *layerForDrawing, OVComposeExperimentalConfig *experimentalConfig) {
    const UIColor *paintColor = [paint colorFromColorValue];
    const CGFloat strokeWidth = paint.strokeWidth;
    
    // 计算出对齐后的像素坐标来创建最终的 layerFrame (单位是点/dp)
    const CGRect layerFrame = TMMCreateDrawRoundRectOutAlignedPixelRect(saveState->translateX, saveState->translateY, left, top, right, bottom, density, strokeWidth);
    CALayerApplyCoordinatesInfo(layerForDrawing, saveState, density, layerFrame);
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
        if (layerForDrawing.mask) {
            layerForDrawing.mask = nil;
        }
        const float outerRadius = MIN(((radiusX + strokeWidth / 2) / density), maxRadius);
        [(TMMNativeComposeGradientLayer *)layerForDrawing applyShader:shader];
        if (!FLOAT_EQUAL(layerForDrawing.cornerRadius, outerRadius)) {
            layerForDrawing.cornerRadius = outerRadius;
        }
        if (paint.style == TMMNativeDrawPaintingStyleStroke) {
            UIBezierPath *fullPath = [UIBezierPath bezierPathWithRect:layerForDrawing.bounds];
            const CGFloat borderWidthInPoints = strokeWidth / density;
            const CGFloat innerRadius = MAX(0.0, outerRadius - (strokeWidth / density));
            CGRect innerRect = CGRectInset(layerForDrawing.bounds, borderWidthInPoints, borderWidthInPoints);
            UIBezierPath *innerRoundedPath = [UIBezierPath bezierPathWithRoundedRect:innerRect cornerRadius:innerRadius];

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
void TMMCALayerDrawOvalV3(float left, float top, float right, float bottom, float density, TMMComposeNativePaint *paint,
                        const CALayerSaveState *saveState, CALayer *layerForDrawing, OVComposeExperimentalConfig *experimentalConfig) {
    // 之前也为空
}

// 圆形绘制
void TMMCALayerDrawCircleV3(float centerX, float centerY, float radius, float density, TMMNativeBasicShader *shader, TMMComposeNativePaint *paint,
                            const CALayerSaveState *saveState, CALayer *hostingLayer, CALayer *layerForDrawing, OVComposeExperimentalConfig *experimentalConfig) {
    const CGFloat originX = saveState->translateX;
    const CGFloat originY = saveState->translateY;
    const CGFloat originWidth = CGRectGetWidth(hostingLayer.frame);
    const CGFloat originHeight = CGRectGetHeight(hostingLayer.frame);

    const CGFloat x = originX / density;
    const CGFloat y = originY / density;
    const CGRect layerFrame = CGRectMake(x, y, originWidth - x, originHeight - y);
    // 注意：transform 需要在 frame 之前设置
    CALayerApplyCoordinatesInfo(layerForDrawing, saveState, density, layerFrame);
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
void TMMCALayerDrawArcV3(float left, float top, float right, float bottom, float density, float startAngle, float sweepAngle, bool useCenter,
                         TMMComposeNativePaint *paint, const CALayerSaveState *saveState, CALayer *layerForDrawing, OVComposeExperimentalConfig *experimentalConfig) {
    const CGFloat originX = saveState->translateX;
    const CGFloat originY = saveState->translateY;
    // 注意：strokeWidthPx 保持像素单位，用于 layerFrame 计算（left/top/right/bottom 都是像素单位）
    const float strokeWidthPx = [paint strokeWidth];
    const float strokeWidthPt = strokeWidthPx / density;
    const CGRect layerFrame = CGRectMake((originX + left - strokeWidthPx / 2) / density, (originY + top - strokeWidthPx / 2) / density,
                                         (right - left + strokeWidthPx) / density, (bottom - top + strokeWidthPx) / density);
    [(TMMNativeArcLayer *)layerForDrawing updateArc:layerFrame.size.width
                                             height:layerFrame.size.height
                                         startAngle:startAngle
                                         sweepAngle:sweepAngle
                                          useCenter:useCenter
                                              color:[paint colorFromColorValue]
                                        strokeWidth:strokeWidthPt
                                          strokeCap:[paint strokeCap]
                                            density:density];
    CALayerApplyCoordinatesInfo(layerForDrawing, saveState, density, layerFrame);
}

// 路径绘制
void TMMCALayerDrawPathV3(UIBezierPath *path, TMMNativeBasicShader *shader, TMMComposeNativePaint *paint, TMMNativeDrawPathOperation pathOperation,
                          float density, const CALayerSaveState *saveState, CALayer *hostingLayer, CALayer *layerForDrawing, OVComposeExperimentalConfig *experimentalConfig) {
    const CGFloat originX = saveState->translateX / density;
    const CGFloat originY = saveState->translateY / density;
    const CGFloat width = CGRectGetWidth(hostingLayer.frame) - originX;
    const CGFloat height = CGRectGetHeight(hostingLayer.frame) - originY;
    const CGRect layerFrame = CGRectMake(originX, originY, width, height);
    CALayerApplyCoordinatesInfo(layerForDrawing, saveState, density, layerFrame);
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
void TMMCALayerDrawImageV3(intptr_t imagePointer, float topLeftOffsetX, float topLeftOffsetY, float density, TMMComposeNativePaint *paint,
                           const CALayerSaveState *saveState, CALayer *layerForDrawing, OVComposeExperimentalConfig *experimentalConfig) {}

// 文字位图绘制
void TMMCALayerDrawTextSkBitmapV3(intptr_t skBitmap, int32_t cacheKey, int width, int height, float density, const CALayerSaveState *saveState,
                                  CALayer *layerForDrawing, OVComposeExperimentalConfig *experimentalConfig) {
    const float originX = saveState->translateX / density;
    const float originY = saveState->translateY / density;
    const float layerWidth = ceil(width / density);
    const float layerHeight = ceil(height / density);

    UIImage *image = TMMNativeComposeUIImageFromSkBitmap(skBitmap, cacheKey, experimentalConfig);
    CGRect layerForDrawingFrame = CGRectMake(originX, originY, layerWidth, layerHeight);
    CALayerApplyCoordinatesInfo(layerForDrawing, saveState, density, layerForDrawingFrame);
    layerForDrawing.contentsGravity = kCAGravityTop;
    layerForDrawing.contentsScale = density;
    layerForDrawing.contents = (__bridge id)image.CGImage;
    intptr_t layerPtr = (intptr_t)(__bridge void *)layerForDrawing;
    intptr_t imagePtr = (intptr_t)image.CGImage;
    ovmp_fastlog_log6(OVMP_FASTLOG_TAG_TEXT,
                      ovmp_get_current_trace_id(),
                      OVMPFastLogPhaseTextOnExecDrawTMMCALayerDrawTextSkBitmapV3,
                      layerPtr,
                      skBitmap,
                      cacheKey,
                      imagePtr);
#ifdef DEBUG
    [(TMMAsyncTaskLayer *)layerForDrawing setDebugTag:cacheKey];
#endif
}

// 空文字绘制
void TMMCALayerDrawNullTextV3(int32_t cacheKey, int width, int height, float density, const CALayerSaveState *saveState, CALayer *layerForDrawing, OVComposeExperimentalConfig *experimentalConfig) {
    const float originX = saveState->translateX / density;
    const float originY = saveState->translateY / density;
    const float layerWidth = ceil(width / density);
    const float layerHeight = ceil(height / density);

    CGRect layerForDrawingFrame = CGRectMake(originX, originY, layerWidth, layerHeight);
    CALayerApplyCoordinatesInfo(layerForDrawing, saveState, density, layerForDrawingFrame);
    layerForDrawing.contentsGravity = kCAGravityTop;
    layerForDrawing.contentsScale = density;
    layerForDrawing.contents = nil;
    intptr_t layerPtr = (intptr_t)(__bridge void *)layerForDrawing;
    ovmp_fastlog_log6(OVMP_FASTLOG_TAG_TEXT,
                      ovmp_get_current_trace_id(),
                      OVMPFastLogPhaseTextOnExecDrawNullTextV3,
                      layerPtr,
                      0,
                      cacheKey,
                      0);
#ifdef DEBUG
    [(TMMAsyncTaskLayer *)layerForDrawing setDebugTag:cacheKey];
#endif
}

void TMMCALayerDrawTextSkBitmapWithUIImagePtrV3(intptr_t imagePtr, int width, int height, float density, const CALayerSaveState *saveState,
                                                CALayer *layerForDrawing, OVComposeExperimentalConfig *experimentalConfig) {
    const CGFloat translateX = saveState->translateX / density;
    const CGFloat translateY = saveState->translateY / density;
    
    // 需要取消之前的异步任务，否则可能异步任务执行较晚后覆盖掉当前的同步代码设置的 contents 结果
    [(TMMAsyncTaskLayer *)layerForDrawing cancelAsyncTask];
#ifdef DEBUG
    [(TMMAsyncTaskLayer *)layerForDrawing setDebugTag:imagePtr];
#endif

    // 与 TMMCALayerDrawTextSkBitmapV3 保持一致，对 width/height 做 ceil 取整，避免不同渲染路径 layerFrame size 不一致导致文本微跳
    CGRect layerFrame = CGRectMake(translateX, translateY, ceil(width / density), ceil(height / density));
    CALayerApplyCoordinatesInfo(layerForDrawing, saveState, density, layerFrame);
    CFTypeRef cfImageRef = (CFTypeRef)imagePtr;
    UIImage *image = (__bridge UIImage *)cfImageRef;
    if (image) {
        layerForDrawing.contentsGravity = kCAGravityTop;
        layerForDrawing.contents = (__bridge id)image.CGImage;
        layerForDrawing.contentsScale = density;
        // 下面一行 release 对应着 TMMNativeComposeHasTextImageCache() 函数的 retain
        if (experimentalConfig.textFixLeakType == TMMTextFixLeakTypeNone) {
            CFRelease(cfImageRef);
        }
    } else {
        layerForDrawing.contents = nil;
    }
    
    if (experimentalConfig.textFixLeakType != TMMTextFixLeakTypeNone && cfImageRef != NULL) {
        // 统一释放：无论 image 是否为 nil，只要 cfImageRef 不为 NULL，就需要释放
        // 对应着 TMMNativeComposeHasTextImageCache() 函数的 retain
        CFRelease(cfImageRef);
    }
}

void TMMCALayerDrawTextSkBitmapUIImageV3(UIImage *image, int width, int height, float density, const CALayerSaveState *saveState,
                                                CALayer *layerForDrawing, OVComposeExperimentalConfig *experimentalConfig) {
    const CGFloat translateX = saveState->translateX / density;
    const CGFloat translateY = saveState->translateY / density;
    
    // 需要取消之前的异步任务，否则可能异步任务执行较晚后覆盖掉当前的同步代码设置的 contents 结果
    [(TMMAsyncTaskLayer *)layerForDrawing cancelAsyncTask];

    // 与 TMMCALayerDrawTextSkBitmapV3 保持一致，对 width/height 做 ceil 取整，避免不同渲染路径 layerFrame size 不一致导致文本微跳
    CGRect layerFrame = CGRectMake(translateX, translateY, ceil(width / density), ceil(height / density));
    CALayerApplyCoordinatesInfo(layerForDrawing, saveState, density, layerFrame);
    intptr_t layerPtr = (intptr_t)(__bridge void *)layerForDrawing;
    const int64_t taskId = ovmp_get_current_trace_id();
    if (image) {
        layerForDrawing.contentsGravity = kCAGravityTop;
        layerForDrawing.contents = (__bridge id)image.CGImage;
        layerForDrawing.contentsScale = density;
        intptr_t imagePtr = (intptr_t)image.CGImage;
        ovmp_fastlog_log6(OVMP_FASTLOG_TAG_TEXT,
                          taskId,
                          OVMPFastLogPhaseTextOnExecTMMCALayerDrawTextSkBitmapUIImageV3UpdateImage,
                          layerPtr,
                          imagePtr,
                          width,
                          height);
    } else {
        layerForDrawing.contents = nil;
        ovmp_fastlog_log6(OVMP_FASTLOG_TAG_TEXT,
                          taskId,
                          OVMPFastLogPhaseTextOnExecTMMCALayerDrawTextSkBitmapUIImageV3ClearContents,
                          layerPtr,
                          width,
                          height,
                          0);
    }
}

// 图像矩形绘制
void TMMCALayerDrawImageRectV3(intptr_t imagePointer, float srcOffsetX, float srcOffsetY, int srcSizeWidth, int srcSizeHeight, float dstOffsetX,
                               float dstOffsetY, int dstSizeWidth, int dstSizeHeight, float density, TMMComposeNativeColorFilter *colorFilter,
                               TMMGaussianBlurFilter *blurFilter, TMMComposeNativePaint *paint, const CALayerSaveState *saveState,
                               CALayer *layerForDrawing, OVComposeExperimentalConfig *experimentalConfig) {
    const CGFloat originX = (saveState->translateX + dstOffsetX) / density;
    const CGFloat originY = (saveState->translateY + dstOffsetY) / density;
    const CGRect layerFrame = CGRectMake(originX, originY, dstSizeWidth / density, dstSizeHeight / density);

    TMMImageDisplayLayer *imageDisplayLayer = (TMMImageDisplayLayer *)layerForDrawing;
    CGImageRef image = (CGImageRef)imagePointer;
    imageDisplayLayer.experimentalConfig = experimentalConfig;
    [imageDisplayLayer setImage:image
                      srcOffset:CGPointMake(srcOffsetX, srcOffsetY)
                        srcSize:CGSizeMake(srcSizeWidth, srcSizeHeight)
                      dstOffset:CGPointMake(dstOffsetX, dstOffsetY)
                        dstSize:CGSizeMake(dstSizeWidth, dstSizeHeight)
                    colorFilter:colorFilter
                     blurFilter:blurFilter
                          paint:paint
                        density:density];
    // layerForDrawing.transform 在设置图片的时候会被自己内部修改，因此这里要合并两个 transform
    CATransform3D originTransform = layerForDrawing.transform;
    CALayerApplyCoordinatesInfo(layerForDrawing, saveState, density, layerFrame);
    if (!CATransform3DEqualToTransform(originTransform, saveState->transformInfo.transform)) {
        layerForDrawing.transform = CATransform3DConcat(originTransform, saveState->transformInfo.transform);
    }
}

void TMMCALayerDrawTextAsyncTaskV3(TMMNativeOneResultBlock globalTask, int32_t cacheKey, int width, int height, float density,
                                   const CALayerSaveState *saveState, CALayer *layerForDrawing, OVComposeExperimentalConfig *experimentalConfig,
                                   TMMNativeTextImageDrawCompleted drawCompleted) {
    const CGFloat translateX = saveState->translateX / density;
    const CGFloat translateY = saveState->translateY / density;
    // 与 TMMCALayerDrawTextSkBitmapV3 保持一致，对 width/height 做 ceil 取整，避免不同渲染路径 layerFrame size 不一致导致文本微跳
    CGRect layerFrame = CGRectMake(translateX, translateY, ceil(width / density), ceil(height / density));
    CALayerApplyCoordinatesInfo(layerForDrawing, saveState, density, layerFrame);
    [(TMMAsyncTaskLayer *)layerForDrawing commitAsyncTask:globalTask
                                                  density:density
                                       experimentalConfig:experimentalConfig
                                           completedBlock:drawCompleted];
}


void TMMCALayerDrawTextSkBitmapWithUIImageV3(UIImage *image, int width, int height, float density, const CALayerSaveState *saveState,
                                           CALayer *layerForDrawing) {
    const float originX = saveState->translateX / density;
    const float originY = saveState->translateY / density;
    const float layerWidth = ceil(width / density);
    const float layerHeight = ceil(height / density);
    CGRect layerForDrawingFrame = CGRectMake(originX, originY, layerWidth, layerHeight);

    layerForDrawing.transform = saveState->transform;
    layerForDrawing.frame = layerForDrawingFrame;
    layerForDrawing.contentsGravity = kCAGravityTop;
    layerForDrawing.contentsScale = density;
    layerForDrawing.contents = (__bridge id)image.CGImage;
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
void TMMCALayerDrawRawPointsV3(NSArray<NSNumber *> *points, TMMComposeNativePaint *paint, float density, const CALayerSaveState *saveState,
                               CALayer *layerForDrawing, OVComposeExperimentalConfig *experimentalConfig) {
    float strokeSize = paint.strokeWidth;
    UIColor *color = [paint colorFromColorValue];
    TMMNativeDrawStrokeCap strokeCap = paint.strokeCap;
    const CGRect layerFrame = getLayerFrameFromArray(points, saveState->translateX, saveState->translateY, paint.strokeWidth);

    [(TVComposePointLayer *)layerForDrawing updatePoints:points strokeSize:strokeSize color:color strokeCap:strokeCap];
    CALayerApplyCoordinatesInfo(layerForDrawing, saveState, density, layerFrame);
    layerForDrawing.drawsAsynchronously = NO;
    [layerForDrawing setNeedsDisplay];
}
