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

#import "TMMCanvasViewProxyV2.h"
#import "TMMCanvasLayerDrawer.h"
#import "TMMComposeAdaptivedCanvasViewV2.h"
#import "TMMComposeNativeColorFilter.h"
#import "TMMComposeNativePath.h"
#import "TMMDrawUtils.h"
#import "TMMGaussianBlurFilter.h"
#import "TMMImageBitmapUtil.h"
#import "TMMImageDisplayLayer.h"
#import "TMMUIKitCanvasLayer.h"
#import "TMMUIKitPictureRecorder.h"
#import "TMMXXHashFuncs.h"
#import "UIViewUtils.h"

#define NSLog(...)

/// 高斯模糊有效的最小半径
static float const gBlurMinimumRadius = 0.001;

using namespace TMM;

@interface TMMCanvasViewProxyV2 ()

/// 自身的 CanvasLayer
@property (nonatomic, strong) TMMUIKitCanvasLayer *adaptivedCanvas;

/// 自身的 CanvasView
@property (nonatomic, strong) TMMComposeAdaptivedCanvasViewV2 *canvasView;

/// 当前 canvas 上的高斯模糊的 filter
@property (nonatomic, strong) TMMGaussianBlurFilter *blurFilter;

/// 用来存储ColorFilter的信息，三种类型的ColorFilter都存储在这里了，目前看还没必要用三个类来表示。
@property (nonatomic, strong, nullable) TMMComposeNativeColorFilter *colorFilter;

/// 当前 Canvas 上的 Paint，注意会进行复用
@property (nonatomic, strong) TMMComposeNativePaint *paint;

/// 当前 Canvas 上的 BlurFilter 的 模糊参数
@property (nonatomic, assign) CGFloat blurRadius;

@end

@implementation TMMCanvasViewProxyV2 {
    PictureRecorder _pictureRecorder;
}

- (instancetype)init {
    if (self = [super init]) {
        _canvasView = [[TMMComposeAdaptivedCanvasViewV2 alloc] init];
        _adaptivedCanvas = (TMMUIKitCanvasLayer *)_canvasView.layer;
    }
    return self;
}

- (UIView *)view {
    return _canvasView;
}

- (void)prepareForReuse {
    [_paint prepareForReuse];
    [_canvasView prepareForReuse];
    _pictureRecorder.prepareForReuse();
}

- (TMMComposeNativePaint *)paint {
    if (_paint) {
        return _paint;
    }
    _paint = [[TMMComposeNativePaint alloc] init];
    return _paint;
}

- (void)addSubview:(UIView *)view {
    [self.canvasView addSubview:view];
}

- (void)removeFromSuperView {
    TMMCMPUIViewFastRemoveFromSuperview(self.canvasView);
}

- (void)setBounds:(float)originX originY:(float)originY boundsWidth:(float)boundsWidth boundsHeight:(float)boundsHeight {
    self.canvasView.bounds = CGRectMake(originX, originY, boundsWidth, boundsHeight);
}

- (void)setCenter:(float)centerX centerY:(float)centerY {
    self.canvasView.center = CGPointMake(centerX, centerY);
    NSLog(@"[PV2] layer:%p setCenter(%lf, %lf)", self.adaptivedCanvas, centerX, centerY);
}

- (void)setAnchorPoint:(float)pointX pointY:(float)pointY {
    CGPoint newPoint = CGPointMake(pointX, pointY);
    if (!CGPointEqualToPoint(self.adaptivedCanvas.anchorPoint, newPoint)) {
        ((CALayer *)self.adaptivedCanvas).anchorPoint = newPoint;
    }
}

- (void)setAlpha:(float)alpha {
    self.canvasView.alpha = alpha;
}

- (void)bringSelfToFront {
    [self.canvasView.superview bringSubviewToFront:self.canvasView];
}

- (void)setHidden:(BOOL)hidden {
    UIView *view = self.canvasView;
    if (view.hidden != hidden) {
        TMMCMPUIViewFastSetHidden(view, hidden);
    }
}

- (void)drawLayerWithSubproxy:(nullable TMMCanvasViewProxyV2 *)subproxy {
    if (subproxy.adaptivedCanvas) {
        NSLog(@"[PV2] <view:%p layer:%p> drawLayerWithSubproxy:<view:%p layer:%p>", self.view, self.adaptivedCanvas, subproxy.view,
              subproxy.adaptivedCanvas);
        _pictureRecorder.drawLayer(subproxy.adaptivedCanvas);
    }
}

- (void)setParent:(nullable id<ITMMCanvasViewProxy>)parentProxy {
    UIView *subview = self.canvasView;
    UIView *superview = parentProxy.view;
    /*
     这里的 if 判断不仅仅是为了提升性能，更是为了避免 bug，原因是 view 如何反复地添加到相同的父 view 上，
     会被 remove 再 add，导致 viewPager 的某一些动画会闪烁
     */
    if (subview.superview != superview) {
        NSLog(@"[PV2] layer:%p setParent:%@", self.adaptivedCanvas, superview.layer);

        TMMCMPUIViewFastAddSubview(superview, subview);
    }
}

- (void)attachToRootView:(UIView *)rootView {
    if (self.canvasView.superview != rootView) {
        TMMCMPUIViewFastAddSubview(rootView, self.canvasView);
    }
}

- (void)applyTransformMatrix:(float)rotationX
                   rotationY:(float)rotationY
                   rotationZ:(float)rotationZ
                      scaleX:(float)scaleX
                      scaleY:(float)scaleY
                translationX:(float)translationX
                translationY:(float)translationY
                transformM34:(double)transformM34 {
    // TODO review 这里是否合理
    CATransform3D transform = CATransform3DIdentity;
    // 处理translation
    transform = CATransform3DTranslate(transform, translationX, translationY, 0);
    // 进行rotation的变换
    transform = CATransform3DRotate(transform, rotationZ * M_PI / 180, 0, 0, 1);
    transform = CATransform3DRotate(transform, rotationY * M_PI / 180, 0, 1, 0);
    transform = CATransform3DRotate(transform, rotationX * M_PI / 180, 1, 0, 0);

    // 处理m34矩阵乘积
    if (transformM34 != 0) {
        CATransform3D concatMatrix = CATransform3DIdentity;
        concatMatrix.m34 = transformM34;
        transform = CATransform3DConcat(transform, concatMatrix);
    }
    // 进行scale的变换
    transform = CATransform3DScale(transform, scaleX, scaleY, 1);
    // 修改矩阵内部的值
    transform.m31 = 0;
    transform.m32 = 0;
    transform.m34 = 0;
    transform.m13 = 0;
    transform.m23 = 0;
    transform.m43 = 0;
    // 设置矩阵
    self.adaptivedCanvas.transform = transform;
    NSLog(@"[PV2] layer:%p applyTransformMatrix:(%lf, %lf, %lf)", self.adaptivedCanvas, rotationX, rotationY, rotationZ);
}

- (void)setClipsToBounds:(BOOL)clipsToBounds {
    self.canvasView.clipsToBounds = clipsToBounds;
}

/// 设置阴影信息
- (void)setShadowWithElevation:(float)shadowElevation
                  shadowRadius:(float)shadowRadius
                shadowColorRed:(float)shadowColorRed
               shadowColorBlue:(float)shadowColorBlue
              shadowColorGreen:(float)shadowColorGreen
              shadowColorAlpha:(float)shadowColorAlpha {
    UIColor *shadowColor = [UIColor colorWithRed:shadowColorRed green:shadowColorGreen blue:shadowColorBlue alpha:shadowColorAlpha];
    [self.adaptivedCanvas setShadowWithColor:shadowColor
                                   elevation:shadowElevation / PictureRecorder::density
                              ktShadowRadius:shadowRadius / PictureRecorder::density];
}

- (void)clearShadow {
    [self.adaptivedCanvas clearShadow];
}

- (void)setBlurRadius:(CGFloat)blurRadius {
    _blurRadius = blurRadius;
    if (blurRadius > gBlurMinimumRadius) {
        if (!_blurFilter) {
            _blurFilter = [[TMMGaussianBlurFilter alloc] init];
        }
        _blurFilter.blurRadius = blurRadius;
    } else {
        _blurFilter = nil;
    }
}

- (void)setColorFilter:(TMMComposeNativeColorFilter *)paintColorFilter {
    if (paintColorFilter == nil) {
        _colorFilter = nil;
        return;
    }
    switch (paintColorFilter.type) {
        case TMMNativeColorFilterTypeBlend:
        case TMMNativeColorFilterTypeMatrix:
        case TMMNativeColorFilterTypeLighting:
            if (!_colorFilter) {
                _colorFilter = [[TMMComposeNativeColorFilter alloc] init];
            }
            [_colorFilter setColorFilterInfo:paintColorFilter];
            break;
        default:
            _colorFilter = nil;
            break;
    }
}

#pragma mark - TMMNativeComposeAdaptivedCanvas
- (void)translate:(float)dx dy:(float)dy {
    _pictureRecorder.translate(dx, dy);
    NSLog(@"[PV2] layer:%p translate:(%lf, %lf)", self.adaptivedCanvas, dx, dy);
}

- (void)scale:(float)sx sy:(float)sy {
    _pictureRecorder.scale(sx, sy);
    NSLog(@"[PV2] layer:%p scale:(%lf, %lf)", self.adaptivedCanvas, sx, sy);
}

- (void)rotate:(float)degrees {
    _pictureRecorder.rotate(degrees);
    NSLog(@"[PV2] layer:%p rotate:(%lf)", self.adaptivedCanvas, degrees);
}

- (void)skew:(float)sx sy:(float)sy {
}

- (void)concat:(nullable TMMNativeComposeMatrix *)matrix {
}

- (void)blur:(float)radiusX radiusY:(float)radiusY {
    self.blurRadius = radiusX;
}

- (void)enableZ {
}

- (void)disableZ {
}

- (void)clearClip {
    CALayer *adaptivedCanvas = self.adaptivedCanvas;
    if (adaptivedCanvas.cornerRadius > 0) {
        adaptivedCanvas.cornerRadius = 0;
    }
    if (adaptivedCanvas.mask) {
        adaptivedCanvas.mask = nil;
    }
}

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
    bottomRightCornerRadiusY:(float)bottomRightCornerRadiusY {
    const uint64_t drawingContentHash
        = hashFloats(left, top, right, bottom, topLeftCornerRadiusX, topLeftCornerRadiusY, topRightCornerRadiusX, topRightCornerRadiusY,
                     bottomLeftCornerRadiusX, bottomLeftCornerRadiusY, bottomRightCornerRadiusX, bottomRightCornerRadiusY);
    PictureRecorderUpdateInfo updateItem = _pictureRecorder.clip(drawingContentHash);
    bool isDirty = updateItem.isDirty;
    NSLog(@"[PV2] layer:%p clipRoundRect295:(%lf,%lf, %lf, %lf, %lf) dirty:%d", self.adaptivedCanvas, left, top, right, bottom, topLeftCornerRadiusX,
          isDirty);
    if (isDirty) {
        CALayer *layerForDrawing = _pictureRecorder.getOrCreateLayerForDrawing(updateItem.drawingType, updateItem.itemHash);
        TMMCALayerDrawClipLayer(left, top, right, bottom, topLeftCornerRadiusX, topLeftCornerRadiusY, topRightCornerRadiusX, topRightCornerRadiusY,
                                bottomLeftCornerRadiusX, bottomLeftCornerRadiusY, bottomRightCornerRadiusX, bottomRightCornerRadiusY,
                                PictureRecorder::density, &(updateItem.saveState), layerForDrawing);
    }
}

- (void)clipRect:(float)left top:(float)top right:(float)right bottom:(float)bottom clipOp:(TMMNativeDrawClipOp)clipOp {
    // 目前只支持 TMMNativeDrawClipOpIntersect，TMMNativeDrawClipOpDifference 目前 CALayer 暂时做不到
    const uint64_t drawingContentHash = hashFloats(left, top, right, bottom, (float)clipOp);
    PictureRecorderUpdateInfo updateItem = _pictureRecorder.clip(drawingContentHash);
    bool isDirty = updateItem.isDirty;
    NSLog(@"[PV2] layer:%p clipRect326:(left:%lf, top:%lf, right:%lf, bottom:%lf, clipOp:%ld) dirty:%d", self.adaptivedCanvas, left, top, right,
          bottom, static_cast<long>(clipOp), isDirty);
    if (isDirty) {
        CALayer *layerForDrawing = _pictureRecorder.getOrCreateLayerForDrawing(updateItem.drawingType, updateItem.itemHash);
        TMMCALayerDrawClipLayerWithRect(left, top, right, bottom, PictureRecorder::density, clipOp, &(updateItem.saveState), layerForDrawing);
    }
}

- (void)clipPath:(TMMComposeNativePath *)path clipOp:(TMMNativeDrawClipOp)clipOp {
    const uint64_t drawingContentHash = hashMerge([path hash], clipOp);
    PictureRecorderUpdateInfo updateItem = _pictureRecorder.clip(drawingContentHash);
    bool isDirty = updateItem.isDirty;
    NSLog(@"[PV2] layer:%p clipPath346:<Path:%@> dirty:%@", self.adaptivedCanvas, NSStringFromCGRect([[path bezierPath] bounds]), @(isDirty));
    if (isDirty) {
        UIBezierPath *bezierPath = [path bezierPath];
        CALayer *layerForDrawing = _pictureRecorder.getOrCreateLayerForDrawing(updateItem.drawingType, updateItem.itemHash);
        TMMCALayerDrawClipLayerWithPath(bezierPath, clipOp, PictureRecorder::density, &(updateItem.saveState), layerForDrawing);
    }
}

- (void)drawLayer:(CALayer *)layer {
    _pictureRecorder.drawLayer(layer);
    NSLog(@"[PV2] <view:%p layer:%p> drawLayer361:<view:%p layer:%p>", self.view, self.adaptivedCanvas, layer.delegate, layer);
}

- (void)drawLine:(float)pointX1 pointY1:(float)pointY1 pointX2:(float)pointX2 pointY2:(float)pointY2 paint:(TMMComposeNativePaint *)paint {
    TMMNativeBasicShader *shader = [paint shader];
    const TMMNativeDrawingType drawingType = shader ? TMMNativeDrawingTypeShaderLine : TMMNativeDrawingTypeLine;
    const uint64_t preHash = hashMerge(TMMNativeDataHashFromPaint(paint), drawingType);
    const uint64_t drawingContentHash = hashFloats(pointX1, pointY1, pointX2, pointY2, (float)preHash);
    PictureRecorderUpdateInfo updateItem = _pictureRecorder.draw(drawingType, drawingContentHash);

    bool isDirty = updateItem.isDirty;
    CALayer *layerForDrawing = nil;
    if (isDirty) {
        layerForDrawing = _pictureRecorder.getOrCreateLayerForDrawing(updateItem.drawingType, updateItem.itemHash);
        TMMCALayerDrawLine(pointX1, pointY1, pointX2, pointY2, PictureRecorder::density, paint, shader, &(updateItem.saveState), layerForDrawing);
    }
    NSLog(@"[PV2] layer:%p layerForDrawing:%@ drawLine385:(pointX1:%lf,...) shader:%@ dirty:%d", self.adaptivedCanvas, layerForDrawing, pointX1,
          shader, isDirty);
}

- (void)drawRect:(float)left top:(float)top right:(float)right bottom:(float)bottom paint:(TMMComposeNativePaint *)paint {
    TMMNativeBasicShader *shader = [paint shader];
    const TMMNativeDrawingType drawingType = shader ? TMMNativeDrawingTypeShaderRect : TMMNativeDrawingTypeRect;
    const uint64_t preHash = hashMerge(TMMNativeDataHashFromPaint(paint), drawingType);
    const uint64_t drawingContentHash = hashFloats(left, top, right, bottom, (float)preHash);
    PictureRecorderUpdateInfo updateItem = _pictureRecorder.draw(drawingType, drawingContentHash);
    bool isDirty = updateItem.isDirty;
    CALayer *layerForDrawing = nil;
    if (isDirty) {
        layerForDrawing = _pictureRecorder.getOrCreateLayerForDrawing(updateItem.drawingType, updateItem.itemHash);
        TMMCALayerDrawRect(left, top, right, bottom, PictureRecorder::density, paint, shader, &(updateItem.saveState), layerForDrawing);
    }
    NSLog(@"[PV2] layer:%p layerForDrawing:%p drawRect407:(left:%lf, top:%lf, right:%lf, bottom:%lf) shader:%@ dirty:%d", self.adaptivedCanvas,
          layerForDrawing, left, top, right, bottom, shader, isDirty);
}

- (void)drawRoundRect:(float)left
                  top:(float)top
                right:(float)right
               bottom:(float)bottom
              radiusX:(float)radiusX
              radiusY:(float)radiusY
                paint:(TMMComposeNativePaint *)paint {
    TMMNativeBasicShader *shader = [paint shader];
    const TMMNativeDrawingType drawingType = shader ? TMMNativeDrawingTypeShaderRect : TMMNativeDrawingTypeRect;
    const uint64_t preHash = hashMerge(TMMNativeDataHashFromPaint(paint), drawingType);
    const uint64_t drawingContentHash = hashFloats(left, top, right, bottom, radiusX, radiusY, (float)preHash);

    PictureRecorderUpdateInfo updateItem = _pictureRecorder.draw(drawingType, drawingContentHash);

    bool isDirty = updateItem.isDirty;
    CALayer *layerForDrawing = nil;
    if (isDirty) {
        layerForDrawing = _pictureRecorder.getOrCreateLayerForDrawing(updateItem.drawingType, updateItem.itemHash);
        TMMCALayerDrawRoundRect(left, top, right, bottom, radiusX, radiusY, PictureRecorder::density, paint, shader, &(updateItem.saveState),
                                layerForDrawing);
    }
    NSLog(
        @"[PV2] layer:%p layerForDrawing:%p drawRoundRect434:(left:%lf, top:%lf, right:%lf, bottom:%lf, radiusX:%lf radiusY:%lf) shader:%@ dirty:%d",
        self.adaptivedCanvas, layerForDrawing, left, top, right, bottom, radiusX, radiusY, shader, isDirty);
}

- (void)drawOval:(float)left top:(float)top right:(float)right bottom:(float)bottom paint:(TMMComposeNativePaint *)paint {
}

- (void)drawCircle:(float)centerX centerY:(float)centerY radius:(float)radius paint:(TMMComposeNativePaint *)paint {
    TMMNativeBasicShader *shader = [paint shader];
    const TMMNativeDrawingType drawingType = shader ? TMMNativeDrawingTypeShaderCircle : TMMNativeDrawingTypeCircle;
    const uint64_t preHash = hashMerge(TMMNativeDataHashFromPaint(paint), TMMNativeDrawingTypeCircle);
    const uint64_t drawingContentHash = hashFloats(centerX, centerY, radius, (float)preHash);

    PictureRecorderUpdateInfo updateItem = _pictureRecorder.draw(drawingType, drawingContentHash);
    bool isDirty = updateItem.isDirty;
    CALayer *layerForDrawing = nil;
    if (isDirty) {
        layerForDrawing = _pictureRecorder.getOrCreateLayerForDrawing(updateItem.drawingType, updateItem.itemHash);
        TMMCALayerDrawCircle(centerX, centerY, radius, PictureRecorder::density, shader, paint, &(updateItem.saveState), self.adaptivedCanvas,
                             layerForDrawing);
    }
    NSLog(@"[PV2] layer:%p layerForDrawing:%p drawCircle:(centerX:%lf, centerY:%lf, radius:%lf) shader:%@ isDirty:%d", self.adaptivedCanvas,
          @(isDirty), centerX, centerY, radius, shader, isDirty);
}

- (void)drawArc:(float)left
            top:(float)top
          right:(float)right
         bottom:(float)bottom
     startAngle:(float)startAngle
     sweepAngle:(float)sweepAngle
      useCenter:(BOOL)useCenter
          paint:(TMMComposeNativePaint *)paint {
    const TMMNativeDrawingType drawingType = TMMNativeDrawingTypeArc;
    const uint64_t preHash = hashMerge(TMMNativeDataHashFromPaint(paint), drawingType);
    const uint64_t drawingContentHash = hashFloats(left, top, right, bottom, startAngle, sweepAngle, (useCenter ? 1.0f : 0.0f), (float)preHash);

    PictureRecorderUpdateInfo updateItem = _pictureRecorder.draw(drawingType, drawingContentHash);
    if (updateItem.isDirty) {
        CALayer *layerForDrawing = _pictureRecorder.getOrCreateLayerForDrawing(updateItem.drawingType, updateItem.itemHash);
        TMMCALayerDrawArc(left, top, right, bottom, PictureRecorder::density, startAngle, sweepAngle, useCenter, paint, &(updateItem.saveState),
                          layerForDrawing);
    }
}

- (void)drawPath:(TMMComposeNativePath *)path paint:(TMMComposeNativePaint *)paint {
    TMMNativeBasicShader *shader = [paint shader];
    const TMMNativeDrawingType drawingType = shader ? TMMNativeDrawingTypeShaderPath : TMMNativeDrawingTypePath;
    const uint64_t drawingContentHash = hashMerge([path hash], TMMNativeDataHashFromPaint(paint), drawingType);
    PictureRecorderUpdateInfo updateItem = _pictureRecorder.draw(drawingType, drawingContentHash);
    bool isDirty = updateItem.isDirty;
    CALayer *layerForDrawing = nil;
    if (isDirty) {
        UIBezierPath *bezierPath = [path bezierPath];
        layerForDrawing = _pictureRecorder.getOrCreateLayerForDrawing(updateItem.drawingType, updateItem.itemHash);
        TMMCALayerDrawPath(bezierPath, shader, paint, path.pathOperation, PictureRecorder::density, &(updateItem.saveState), self.adaptivedCanvas,
                           layerForDrawing);
    }
    NSLog(@"[PV2] layer:%p layerForDrawing:%@ drawPath:%@ shader:%@ dirty:%d", self.adaptivedCanvas, layerForDrawing, path, shader, isDirty);
}

- (void)drawImageRect:(void *)imagePointer
           srcOffsetX:(float)srcOffsetX
           srcOffsetY:(float)srcOffsetY
         srcSizeWidth:(int)srcSizeWidth
        srcSizeHeight:(int)srcSizeHeight
           dstOffsetX:(float)dstOffsetX
           dstOffsetY:(float)dstOffsetY
         dstSizeWidth:(int)dstSizeWidth
        dstSizeHeight:(int)dstSizeHeight
                paint:(TMMComposeNativePaint *)paint {
    const intptr_t address = (intptr_t)(imagePointer);
    const TMMNativeDrawingType drawingType = TMMNativeDrawingTypeImageRect;
    const uint64_t preHash = hashMerge(TMMNativeDataHashFromPaint(paint), drawingType);
    const uint64_t drawingContentHash = hashFloats((float)address, srcOffsetX, srcOffsetY, (float)srcSizeWidth, (float)srcSizeHeight, dstOffsetX,
                                                   dstOffsetY, (float)dstSizeWidth, (float)dstSizeHeight, (float)preHash, (float)self.blurRadius);
    PictureRecorderUpdateInfo updateItem = _pictureRecorder.draw(drawingType, drawingContentHash);
    bool isDirty = updateItem.isDirty;
    CALayer *layerForDrawing = nil;
    if (isDirty) {
        self.colorFilter = paint.colorFilter;
        layerForDrawing = _pictureRecorder.getOrCreateLayerForDrawing(updateItem.drawingType, updateItem.itemHash);
        TMMCALayerDrawImageRect(address, srcOffsetX, srcOffsetY, srcSizeWidth, srcSizeHeight, dstOffsetX, dstOffsetY, dstSizeWidth, dstSizeHeight,
                                PictureRecorder::density, self.colorFilter, self.blurFilter, paint, &(updateItem.saveState), layerForDrawing);
    }
    NSLog(@"[PV2] layer:%p layerForDrawing:%p drawImageRect543(ptr:%p, srcOffsetX:%lf, srcOffsetY:%lf, srcSizeWidth:%d, srcSizeHeight:%d, "
          @"dstOffsetX:%f, dstOffsetY:%f, dstSizeWidth:%d, dstSizeHeight:%d) dirty:%d",
          self.adaptivedCanvas, layerForDrawing, imagePointer, srcOffsetX, srcOffsetY, srcSizeWidth, srcSizeHeight, dstOffsetX, dstOffsetY,
          dstSizeWidth, dstSizeHeight, isDirty);
}

- (void)drawTextSkBitmap:(intptr_t)skBitmap cacheKey:(int)cacheKey width:(int)width height:(int)height {
    const TMMNativeDrawingType drawingType = TMMNativeDrawingTypeImageData;
    const uint64_t drawingContentHash = hashFloats((float)skBitmap, (float)cacheKey, (float)width, (float)height, (float)drawingType);
    PictureRecorderUpdateInfo updateItem = _pictureRecorder.draw(drawingType, drawingContentHash);
    bool isDirty = updateItem.isDirty;
    NSLog(@"[PV2] layer:%p drawTextSkBitmap552(ptr:%ld, width:%d, height:%d) dirty:%d", self.adaptivedCanvas, skBitmap, width, height, isDirty);
    if (isDirty) {
        CALayer *layerForDrawing = _pictureRecorder.getOrCreateLayerForDrawing(updateItem.drawingType, updateItem.itemHash);
        TMMCALayerDrawTextSkBitmap(skBitmap, cacheKey, width, height, PictureRecorder::density, &(updateItem.saveState), layerForDrawing);
    }
}

- (void)drawTextSkBitmapWithUIImagePtr:(intptr_t)imagePtr width:(int)width height:(int)height {
    const TMMNativeDrawingType drawingType = TMMNativeDrawingTypeImageData;
    const uint64_t drawingContentHash = hashFloats((float)imagePtr, (float)width, (float)height);
    PictureRecorderUpdateInfo updateItem = _pictureRecorder.draw(drawingType, drawingContentHash);
    bool isDirty = updateItem.isDirty;
    if (isDirty) {
        CALayer *layerForDrawing = _pictureRecorder.getOrCreateLayerForDrawing(updateItem.drawingType, updateItem.itemHash);
        TMMCALayerDrawTextSkBitmapWithUIImagePtr(imagePtr, width, height, PictureRecorder::density, &(updateItem.saveState), layerForDrawing);
    }
    NSLog(@"[PV2] layer:%p dirty:%@ drawTextSkBitmapWithUIImagePtr566(imagePtr:%ld, width:%d, height:%d)", self.adaptivedCanvas, @(isDirty), imagePtr,
          width, height);
}

- (void)asyncDrawIntoCanvas:(TMMNativeOneResultBlock)globalTask cacheKey:(int32_t)cacheKey width:(int)width height:(int)height {
    const TMMNativeDrawingType drawingType = TMMNativeDrawingTypeImageData;
    const uint64_t drawingContentHash = hashFloats((float)[globalTask hash], (float)cacheKey, (float)width, (float)drawingType);

    PictureRecorderUpdateInfo updateItem = _pictureRecorder.draw(drawingType, drawingContentHash);
    if (updateItem.isDirty) {
        CALayer *layerForDrawing = _pictureRecorder.getOrCreateLayerForDrawing(updateItem.drawingType, updateItem.itemHash);
        TMMCALayerDrawTextAsyncTask(globalTask, cacheKey, width, height, PictureRecorder::density, &(updateItem.saveState), layerForDrawing);
    }
}

- (intptr_t)imageFromImageBitmap:(intptr_t)imageBitmap cacheKey:(int32_t)cacheKey {
    UIImage *image = TMMNativeComposeUIImageFromSkBitmap(imageBitmap, cacheKey);
    CFTypeRef imageRef = (__bridge_retained CFTypeRef)image;
    return (intptr_t)imageRef;
}

- (void)drawRawPoints:(NSArray<NSNumber *> *)points paint:(TMMComposeNativePaint *)paint {
    // 获取hash，辅助PictureRecord功能
    uint64_t pointHash = TMMFNVHashFloatArray(points);
    uint64_t paintHash = TMMNativeDataHashFromPaint(paint);

    const TMMNativeDrawingType drawingType = TMMNativeDrawingTypePoints;
    const uint64_t drawingContentHash = hashMerge(pointHash, paintHash);

    PictureRecorderUpdateInfo updateItem = _pictureRecorder.draw(drawingType, drawingContentHash);
    if (updateItem.isDirty) {
        CALayer *layerForDrawing = _pictureRecorder.getOrCreateLayerForDrawing(updateItem.drawingType, updateItem.itemHash);
        TMMCALayerDrawRawPoints(points, paint, PictureRecorder::density, &(updateItem.saveState), layerForDrawing);
    }
}

- (void)drawVertices:(id)vertices blendMode:(TMMNativeDrawBlendMode)blendMode paint:(TMMComposeNativePaint *)paint {
}

- (void)beginDraw {
    NSLog(@"[PV2] layer:%p ---------beginDraw---------", self.adaptivedCanvas);
    _pictureRecorder.startRecording(self.adaptivedCanvas);
}

- (void)finishDraw {
    _pictureRecorder.finishRecording(self.adaptivedCanvas);
    NSLog(@"[PV2] layer:%p ---------finishDraw---------", self.adaptivedCanvas);
}

- (void)restore {
    _pictureRecorder.restore();
    NSLog(@"[PV2] layer:%p --->restore", self.adaptivedCanvas);
}

- (void)save {
    _pictureRecorder.save();
    NSLog(@"[PV2] layer:%p --->save", self.adaptivedCanvas);
}

- (UIImage *)getSnapshotImage {
    const float density = PictureRecorder::density;
    UIView *targetView = self.canvasView;
    return OVCMPSnapshotImageFromUIView(targetView, CGRectGetWidth(targetView.frame), CGRectGetHeight(targetView.frame), density);
}

- (UIImage *)getSnapshotImageWithWidth:(int)width height:(int)height {
    const float density = PictureRecorder::density;
    return OVCMPSnapshotImageFromUIView(self.canvasView, width / density, height / density, density);
}

- (NSString *)description {
    return [NSString stringWithFormat:@"<ViewProxyV2:%p, view:%p layer:%p>", self, self.canvasView, self.canvasView.layer];
}

#ifdef COCOAPODS
- (PictureRecorder &)pictureRecorder {
    return _pictureRecorder;
}
#endif

@end
