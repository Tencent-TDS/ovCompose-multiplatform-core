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

#import "TMMCanvasViewProxyV3.h"
#import "TMMCanvasLayerDrawerV3.h"
#import "TMMComposeAdaptivedCanvasViewV2.h"
#import "TMMComposeNativeColorFilter.h"
#import "TMMComposeNativePath.h"
#import "TMMDrawUtils.h"
#import "TMMGaussianBlurFilter.h"
#import "TMMImageBitmapUtil.h"
#import "TMMImageDisplayLayer.h"
#import "TMMUIKitCanvasLayer.h"
#import "OVComposePictureRecorder.h"
#import "TMMXXHashFuncs.h"
#import "UIViewUtils.h"
#import "OVComposeExperimentalConfig.h"
#import "TMMComposeMemoryCache.h"
#import "TMMCGBitmapCanvasUtils.h"
#import "TMMCGBitmapCanvas.h"
#import "TMMLookInDebugUtils.h"
#import "CALayer+TMMPictureRecorder.h"
#import "TMMComposeInjectCommonServiceImpl.h"
#import "OVMPFastLogCAPI.h"

#define NSLog(...)

/// 高斯模糊有效的最小半径
static float const gBlurMinimumRadius = 0.001;

static NSString *gImageTag = @"[ovcompose-image]";

using namespace TMM;
using namespace ovcompose;

@interface TMMCanvasViewProxyV3 ()

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

/// 实验性质的配置，用于一些开关逻辑
@property (nonatomic, strong) OVComposeExperimentalConfig *experimentalConfig;

/// 给 lookin 使用的 debug id
@property (nonatomic, assign) NSInteger viewProxyDebugId;

/// semanticsId 栈，用于处理嵌套绘制
@property (nonatomic, strong) NSMutableArray<NSNumber *> *semanticsIdStack;

@end

@implementation TMMCanvasViewProxyV3 {
    ovcompose::PictureRecorder _pictureRecorder;
}

+ (instancetype)canvasViewProxyV3WithExperimentalConfigPtr:(intptr_t)configPtr {
    return [[self alloc] initWithExperimentalConfigPtr:configPtr];
}

- (instancetype)initWithExperimentalConfigPtr:(intptr_t)configPtr {
    if (self = [super init]) {
        _canvasView = [[TMMComposeAdaptivedCanvasViewV2 alloc] init];
        _adaptivedCanvas = (TMMUIKitCanvasLayer *)_canvasView.layer;
        // 这里只在 kotlin 侧的 Compose 容器初始化的时候，会传递过来，不需要关心 else 分支
        if (configPtr != 0) {
            OVComposeExperimentalConfig *experimentalConfig = OVComposeConfigPtrToExperimentalConfig(configPtr);
            _experimentalConfig = experimentalConfig;
            _pictureRecorder.enableCALayerClipOpt = experimentalConfig.enableCALayerClipOpt;
        }
        // 下面的方式是给 Lookin debug 视图的时候调用的
        [self debugForLookin];
    }
    return self;
}

#pragma mark - Debug
// 只有在 debug 开关开的情况下，才会将自己进行存储，并在 lookin 关闭的时候，去释放
- (void)debugForLookin {
    if (!TMMLookInDebugEnabel()) {
        return;
    }
    _semanticsIdStack = [NSMutableArray array];
    NSInteger inspectionId = TMMAllocViewProxyDebugId();
    self.viewProxyDebugId = inspectionId;
    TMMLookInDebugViewProxyMap()[@(inspectionId)] = self;
}

// 根据 debugId 获取 viewProxy
+ (UIView *)viewProxyHostingView:(NSNumber *)viewProxyDebugId {
    UIView *debugView = ((TMMCanvasViewProxyV3 *)TMMLookInDebugViewProxyMap()[viewProxyDebugId]).view;
    return debugView;
}

#pragma mark - ITMMCanvasViewProxy
- (NSInteger)getInspectionId {
    return self.viewProxyDebugId;
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
    NSLog(@"[PV2] [PV3] layer:%p setCenter(%lf, %lf)", self.adaptivedCanvas, centerX, centerY);
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

- (void)drawLayerWithSubproxy:(nullable TMMCanvasViewProxyV3 *)subproxy {
    if (subproxy.adaptivedCanvas) {
        NSLog(@"[PV2] <view:%p layer:%p> drawLayerWithSubproxy:<view:%p layer:%p>", self.view, self.adaptivedCanvas, subproxy.view,
              subproxy.adaptivedCanvas);
        _pictureRecorder.drawLayer(subproxy.adaptivedCanvas);
    }
}

- (void)setParent:(nullable id<ITMMCanvasViewProxy>)parentProxy {
    TMMComposeAdaptivedCanvasViewV2 *subview = self.canvasView;
    UIView *superview = parentProxy.view;
    /*
     这里的 if 判断不仅仅是为了提升性能，更是为了避免 bug，原因是 view 如何反复地添加到相同的父 view 上，
     会被 remove 再 add，导致 viewPager 的某一些动画会闪烁
     */
    if (subview.superview != superview) {
        NSLog(@"[PV2] layer:%p setParent:%@", self.adaptivedCanvas, superview.layer);
        subview.isAttached = YES;
        TMMCMPUIViewFastAddSubview(superview, subview);
    }
}

- (void)attachToRootView:(UIView *)rootView {
    TMMComposeAdaptivedCanvasViewV2 *canvasView = self.canvasView;
    if (canvasView.superview != rootView) {
        TMMCMPUIViewFastAddSubview(rootView, canvasView);
        canvasView.isAttached = YES;
    }
}

- (void)detached {
    self.canvasView.isAttached = NO;
}

- (void)applyTransformMatrix2:(float)rotationX
                    rotationY:(float)rotationY
                    rotationZ:(float)rotationZ
                       scaleX:(float)scaleX
                       scaleY:(float)scaleY
                 translationX:(float)translationX
                 translationY:(float)translationY
                 transformM34:(double)transformM34
         optimizeForTransform:(BOOL)optimizeForTransform {
    CATransform3D transform = CATransform3DIdentity;
    BOOL transformIsDirty = NO;
    if (transformM34 != 0) {
        transform.m34 = transformM34;
        transformIsDirty = YES;
    }
    
    if (ABS(scaleX - 1) >= DBL_EPSILON || ABS(scaleY - 1) >= DBL_EPSILON) {
        transform = CATransform3DScale(transform, scaleX, scaleY, 1);
        transformIsDirty = YES;
    }
    
    if (rotationZ != 0) {
        transform = CATransform3DRotate(transform, rotationZ * M_PI / 180, 0, 0, 1);
        transformIsDirty = YES;
    }
    if (rotationY != 0) {
        transform = CATransform3DRotate(transform, rotationY * M_PI / 180, 0, 1, 0);
        transformIsDirty = YES;
    }
    if (rotationX != 0) {
        transform = CATransform3DRotate(transform, rotationX * M_PI / 180, 1, 0, 0);
        transformIsDirty = YES;
    }
    
    if (translationX != 0 || translationY != 0) {
        CATransform3D translateTransform = CATransform3DTranslate(CATransform3DIdentity, translationX, translationY, 0);
        transform = CATransform3DConcat(transform, translateTransform);
        transformIsDirty = YES;
    }
    
    CALayer *adaptivedCanvas = self.adaptivedCanvas;
    
    // 设置矩阵
    adaptivedCanvas.transform = transform;
    
    if (optimizeForTransform && transformIsDirty) {
        if (adaptivedCanvas.shouldRasterize != transformIsDirty) {
            adaptivedCanvas.shouldRasterize = transformIsDirty;
        }
        
        CGFloat density = ovcompose::PictureRecorder::density;
        if (adaptivedCanvas.rasterizationScale != density) {
            adaptivedCanvas.rasterizationScale = density;
        }
    }
    
    NSLog(@"[PV2] layer:%p applyTransformMatrix2:(%f, %f, %f), rasterized: %d", self.adaptivedCanvas, rotationX, rotationY, rotationZ, self.adaptivedCanvas.shouldRasterize);
}

- (void)applyTransformMatrix:(float)rotationX
                   rotationY:(float)rotationY
                   rotationZ:(float)rotationZ
                      scaleX:(float)scaleX
                      scaleY:(float)scaleY
                translationX:(float)translationX
                translationY:(float)translationY
                transformM34:(double)transformM34
        optimizeForTransform:(BOOL)optimizeForTransform {
    
    if (self.experimentalConfig.enablePerspectiveTransformFix) {
        [self applyTransformMatrix2:rotationX
                          rotationY:rotationY
                          rotationZ:rotationZ
                             scaleX:scaleX
                             scaleY:scaleY
                       translationX:translationX
                       translationY:translationY
                       transformM34:transformM34
               optimizeForTransform:optimizeForTransform];
        return;
    }
    
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
    
    BOOL hasTransform = !CATransform3DIsIdentity(transform);
    if (optimizeForTransform && hasTransform) {
        if (self.adaptivedCanvas.shouldRasterize != hasTransform) {
            self.adaptivedCanvas.shouldRasterize = hasTransform;
        }
        if (self.adaptivedCanvas.rasterizationScale != [UIScreen mainScreen].scale && optimizeForTransform) {
            self.adaptivedCanvas.rasterizationScale = [UIScreen mainScreen].scale;
        }
    }
    
    NSLog(@"[PV2] layer:%p applyTransformMatrix:(%f, %f, %f), rasterized: %d", self.adaptivedCanvas, rotationX, rotationY, rotationZ, self.adaptivedCanvas.shouldRasterize);
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
                                   elevation:shadowElevation / ovcompose::PictureRecorder::density
                              ktShadowRadius:shadowRadius / ovcompose::PictureRecorder::density];
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
    ovcompose::PictureRecorderUpdateInfo updateItem = _pictureRecorder.clip(drawingContentHash);
    bool isDirty = updateItem.isDirty;
    NSLog(@"[PV2] layer:%p clipRoundRect295:(%lf,%lf, %lf, %lf, %lf) dirty:%d", self.adaptivedCanvas, left, top, right, bottom, topLeftCornerRadiusX,
          isDirty);
    if (isDirty) {
        CALayer *layerForDrawing = _pictureRecorder.getOrCreateLayerForDrawing(updateItem.drawingType, updateItem.itemHash);
        if (_pictureRecorder.enableCALayerClipOpt) {
            TMMCALayerOptimizedDrawClipLayerV3(left, top, right, bottom, topLeftCornerRadiusX, topLeftCornerRadiusY, topRightCornerRadiusX,
                                               topRightCornerRadiusY, bottomLeftCornerRadiusX, bottomLeftCornerRadiusY, bottomRightCornerRadiusX,
                                               bottomRightCornerRadiusY, ovcompose::PictureRecorder::density, &(updateItem.saveState), layerForDrawing);
            return;
        }
        TMMCALayerDrawClipLayerV3(left, top, right, bottom, topLeftCornerRadiusX, topLeftCornerRadiusY, topRightCornerRadiusX, topRightCornerRadiusY,
                                  bottomLeftCornerRadiusX, bottomLeftCornerRadiusY, bottomRightCornerRadiusX, bottomRightCornerRadiusY,
                                  ovcompose::PictureRecorder::density, &(updateItem.saveState), layerForDrawing, _experimentalConfig);
    }
}

- (void)clipRect:(float)left top:(float)top right:(float)right bottom:(float)bottom clipOp:(TMMNativeDrawClipOp)clipOp {
    // 目前只支持 TMMNativeDrawClipOpIntersect，TMMNativeDrawClipOpDifference 目前 CALayer 暂时做不到
    const uint64_t drawingContentHash = hashFloats(left, top, right, bottom, (float)clipOp);
    ovcompose::PictureRecorderUpdateInfo updateItem = _pictureRecorder.clip(drawingContentHash);
    bool isDirty = updateItem.isDirty;
    NSLog(@"[PV2] layer:%p clipRect326:(left:%lf, top:%lf, right:%lf, bottom:%lf, clipOp:%ld) dirty:%d", self.adaptivedCanvas, left, top, right,
          bottom, static_cast<long>(clipOp), isDirty);
    if (isDirty) {
        CALayer *layerForDrawing = _pictureRecorder.getOrCreateLayerForDrawing(updateItem.drawingType, updateItem.itemHash);
        if (_pictureRecorder.enableCALayerClipOpt) {
            TMMCALayerOptimizedDrawClipLayerWithRectV3(left, top, right, bottom, ovcompose::PictureRecorder::density, clipOp, &(updateItem.saveState), layerForDrawing);
            return;
        }
        TMMCALayerDrawClipLayerWithRectV3(left, top, right, bottom, ovcompose::PictureRecorder::density, clipOp, &(updateItem.saveState), layerForDrawing, _experimentalConfig);
    }
}

- (void)clipPath:(TMMComposeNativePath *)path clipOp:(TMMNativeDrawClipOp)clipOp {
    const uint64_t drawingContentHash = hashMerge([path hash], clipOp);
    ovcompose::PictureRecorderUpdateInfo updateItem = _pictureRecorder.clip(drawingContentHash);
    bool isDirty = updateItem.isDirty;
    NSLog(@"[PV2] layer:%p clipPath346:<Path:%@> dirty:%@", self.adaptivedCanvas, NSStringFromCGRect([[path bezierPath] bounds]), @(isDirty));
    if (isDirty) {
        UIBezierPath *bezierPath = [path bezierPath];
        CALayer *layerForDrawing = _pictureRecorder.getOrCreateLayerForDrawing(updateItem.drawingType, updateItem.itemHash);
        TMMCALayerDrawClipLayerWithPathV3(bezierPath, clipOp, ovcompose::PictureRecorder::density, &(updateItem.saveState), layerForDrawing, _experimentalConfig);
    }
}

- (void)clipEnd { 
    _pictureRecorder.clipEnd();
}

- (void)drawLayer:(CALayer *)layer {
    _pictureRecorder.drawLayer(layer);
    NSLog(@"[PV2] <view:%p layer:%p> drawLayer361:<view:%p layer:%p>", self.view, self.adaptivedCanvas, layer.delegate, layer);
}

- (void)drawLine:(float)pointX1 pointY1:(float)pointY1 pointX2:(float)pointX2 pointY2:(float)pointY2 paint:(TMMComposeNativePaint *)paint {
    TMMNativeBasicShader *shader = [paint shader];
    const TMMNativeDrawingType drawingType = shader ? TMMNativeDrawingTypeShaderLine : TMMNativeDrawingTypeLine;
    const uint64_t preHash = hashMerge(TMMNativeDataHashFromPaint(paint), drawingType);
    // 修复 preHash 转 float 精度丢失导致的 hash 碰撞问题
    uint64_t drawingContentHash = hashMerge(preHash, hashFloats(pointX1, pointY1, pointX2, pointY2));
    
    ovcompose::PictureRecorderUpdateInfo updateItem = _pictureRecorder.draw(drawingType, drawingContentHash);
    
    bool isDirty = updateItem.isDirty;
    CALayer *layerForDrawing = nil;
    if (isDirty) {
        layerForDrawing = _pictureRecorder.getOrCreateLayerForDrawing(updateItem.drawingType, updateItem.itemHash);
        [self bindCALayerToCurrentComponent:layerForDrawing];
        TMMCALayerDrawLineV3(pointX1, pointY1, pointX2, pointY2, ovcompose::PictureRecorder::density, paint, shader, &(updateItem.saveState), layerForDrawing, _experimentalConfig);
    }
    NSLog(@"[PV2] layer:%p layerForDrawing:%@ drawLine385:(pointX1:%lf,...) shader:%@ dirty:%d", self.adaptivedCanvas, layerForDrawing, pointX1,
          shader, isDirty);
}

- (void)drawRect:(float)left top:(float)top right:(float)right bottom:(float)bottom paint:(TMMComposeNativePaint *)paint {
    TMMNativeBasicShader *shader = [paint shader];
    const TMMNativeDrawingType drawingType = shader ? TMMNativeDrawingTypeShaderRect : TMMNativeDrawingTypeRect;
    const uint64_t preHash = hashMerge(TMMNativeDataHashFromPaint(paint), drawingType);
    // 修复 preHash 转 float 精度丢失导致的 hash 碰撞问题
    uint64_t drawingContentHash = hashMerge(preHash, hashFloats(left, top, right, bottom));
    
    ovcompose::PictureRecorderUpdateInfo updateItem = _pictureRecorder.draw(drawingType, drawingContentHash);
    bool isDirty = updateItem.isDirty;
    CALayer *layerForDrawing = nil;
    if (isDirty) {
        layerForDrawing = _pictureRecorder.getOrCreateLayerForDrawing(updateItem.drawingType, updateItem.itemHash);
        [self bindCALayerToCurrentComponent:layerForDrawing];
        TMMCALayerDrawRectV3(left, top, right, bottom, ovcompose::PictureRecorder::density, paint, shader, &(updateItem.saveState), layerForDrawing, _experimentalConfig);
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
    // 修复 preHash 转 float 精度丢失导致的 hash 碰撞问题
    uint64_t drawingContentHash = hashMerge(preHash, hashFloats(left, top, right, bottom, radiusX, radiusY));
    
    ovcompose::PictureRecorderUpdateInfo updateItem = _pictureRecorder.draw(drawingType, drawingContentHash);
    
    bool isDirty = updateItem.isDirty;
    CALayer *layerForDrawing = nil;
    if (isDirty) {
        layerForDrawing = _pictureRecorder.getOrCreateLayerForDrawing(updateItem.drawingType, updateItem.itemHash);
        [self bindCALayerToCurrentComponent:layerForDrawing];
        TMMCALayerDrawRoundRectV3(left, top, right, bottom, radiusX, radiusY, ovcompose::PictureRecorder::density, paint, shader, &(updateItem.saveState),
                                  layerForDrawing, _experimentalConfig);
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
    // 修复 preHash 转 float 精度丢失导致的 hash 碰撞问题
    uint64_t drawingContentHash = hashMerge(preHash, hashFloats(centerX, centerY, radius));
    
    ovcompose::PictureRecorderUpdateInfo updateItem = _pictureRecorder.draw(drawingType, drawingContentHash);
    bool isDirty = updateItem.isDirty;
    CALayer *layerForDrawing = nil;
    if (isDirty) {
        layerForDrawing = _pictureRecorder.getOrCreateLayerForDrawing(updateItem.drawingType, updateItem.itemHash);
        [self bindCALayerToCurrentComponent:layerForDrawing];
        TMMCALayerDrawCircleV3(centerX, centerY, radius, ovcompose::PictureRecorder::density, shader, paint, &(updateItem.saveState), self.adaptivedCanvas, layerForDrawing, _experimentalConfig);
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
    // 修复 preHash 转 float 精度丢失导致的 hash 碰撞问题
    uint64_t drawingContentHash = hashMerge(preHash, hashFloats(left, top, right, bottom, startAngle, sweepAngle, (useCenter ? 1.0f : 0.0f)));
    
    ovcompose::PictureRecorderUpdateInfo updateItem = _pictureRecorder.draw(drawingType, drawingContentHash);
    if (updateItem.isDirty) {
        CALayer *layerForDrawing = _pictureRecorder.getOrCreateLayerForDrawing(updateItem.drawingType, updateItem.itemHash);
        [self bindCALayerToCurrentComponent:layerForDrawing];
        TMMCALayerDrawArcV3(left, top, right, bottom, ovcompose::PictureRecorder::density, startAngle, sweepAngle, useCenter, paint, &(updateItem.saveState),
                            layerForDrawing, _experimentalConfig);
    }
}

- (void)drawPath:(TMMComposeNativePath *)path paint:(TMMComposeNativePaint *)paint {
    TMMNativeBasicShader *shader = [paint shader];
    const TMMNativeDrawingType drawingType = shader ? TMMNativeDrawingTypeShaderPath : TMMNativeDrawingTypePath;
    const uint64_t drawingContentHash = hashMerge([path dataHash], TMMNativeDataHashFromPaint(paint), drawingType);
    ovcompose::PictureRecorderUpdateInfo updateItem = _pictureRecorder.draw(drawingType, drawingContentHash);
    bool isDirty = updateItem.isDirty;
    CALayer *layerForDrawing = nil;
    if (isDirty) {
        UIBezierPath *bezierPath = [path bezierPath];
        layerForDrawing = _pictureRecorder.getOrCreateLayerForDrawing(updateItem.drawingType, updateItem.itemHash);
        [self bindCALayerToCurrentComponent:layerForDrawing];
        TMMCALayerDrawPathV3(bezierPath, shader, paint, path.pathOperation, ovcompose::PictureRecorder::density, &(updateItem.saveState), self.adaptivedCanvas,
                             layerForDrawing, _experimentalConfig);
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
    // 修复 address 转 float 精度丢失导致的 hash 碰撞问题
    const uint64_t addressHash = hashMerge((NSUInteger)address, preHash);
    uint64_t drawingContentHash = hashMerge(addressHash, hashFloats(srcOffsetX, srcOffsetY, (float)srcSizeWidth, (float)srcSizeHeight, dstOffsetX,
                                                       dstOffsetY, (float)dstSizeWidth, (float)dstSizeHeight, (float)self.blurRadius));

    ovcompose::PictureRecorderUpdateInfo updateItem = _pictureRecorder.draw(drawingType, drawingContentHash);
    bool isDirty = updateItem.isDirty;
    CALayer *layerForDrawing = nil;
    const intptr_t viewPorxyPtr = (intptr_t)self;
    ovmp_alloc_trace_id();
    int64_t traceId = ovmp_get_current_trace_id();
    ovmp_set_current_trace_id(traceId);
    // itemHash 是 Layer 分配的索引，用于定位 hash 碰撞导致的 Layer 复用问题
    ovmp_fastlog_log8(OVMP_FASTLOG_TAG_IMAGE,
                      OVMPFastLogPhaseImageOnCommitDrawImage,
                      traceId,
                      viewPorxyPtr,
                      address,
                      preHash,
                      drawingContentHash,
                      updateItem.itemHash,
                      isDirty);

    if (isDirty) {
        self.colorFilter = paint.colorFilter;
        layerForDrawing = _pictureRecorder.getOrCreateLayerForDrawing(updateItem.drawingType, updateItem.itemHash);
        [self bindCALayerToCurrentComponent:layerForDrawing];
        TMMCALayerDrawImageRectV3(address, srcOffsetX, srcOffsetY, srcSizeWidth, srcSizeHeight, dstOffsetX, dstOffsetY, dstSizeWidth, dstSizeHeight,
                                  ovcompose::PictureRecorder::density, self.colorFilter, self.blurFilter, paint, &(updateItem.saveState), layerForDrawing, _experimentalConfig);
    }
    NSLog(@"[PV2] layer:%p layerForDrawing:%p drawImageRect543(ptr:%p, srcOffsetX:%lf, srcOffsetY:%lf, srcSizeWidth:%d, srcSizeHeight:%d, "
          @"dstOffsetX:%f, dstOffsetY:%f, dstSizeWidth:%d, dstSizeHeight:%d) dirty:%d",
          self.adaptivedCanvas, layerForDrawing, imagePointer, srcOffsetX, srcOffsetY, srcSizeWidth, srcSizeHeight, dstOffsetX, dstOffsetY,
          dstSizeWidth, dstSizeHeight, isDirty);
}

- (void)drawTextSkBitmap:(intptr_t)skBitmap cacheKey:(int)cacheKey width:(int)width height:(int)height {
    const TMMNativeDrawingType drawingType = TMMNativeDrawingTypeImageData;
    // 始终使用 hashMerge 避免 skBitmap 指针和 cacheKey 转 float 精度丢失导致的 hash 碰撞
    // 当 cacheKey（paragraphHashCode）值较大时，转 float 会丢失低位精度，
    // 导致不同的 selection 范围产生相同的 drawingContentHash，CALayer 不更新
    uint64_t drawingContentHash = hashMerge((NSUInteger)skBitmap, hashMerge((NSUInteger)cacheKey, hashMerge((NSUInteger)width, hashMerge((NSUInteger)height, (NSUInteger)drawingType))));

    ovcompose::PictureRecorderUpdateInfo updateItem = _pictureRecorder.draw(drawingType, drawingContentHash);
    bool isDirty = updateItem.isDirty;
    const intptr_t viewPorxyPtr = (intptr_t)self;
    // itemHash 是 Layer 分配的索引，用于定位 hash 碰撞导致的 Layer 复用问题
    ovmp_fastlog_log8(OVMP_FASTLOG_TAG_TEXT,
                      ovmp_get_current_trace_id(),
                      OVMPFastLogPhaseTextOnCommitDrawTextSkiaBitmapWithCacheKey,
                      viewPorxyPtr,
                      skBitmap,
                      cacheKey,
                      drawingContentHash,
                      updateItem.itemHash,
                      isDirty);
    NSLog(@"[PV2] layer:%p drawTextSkBitmap552(ptr:%ld, width:%d, height:%d) dirty:%d", self.adaptivedCanvas, skBitmap, width, height, isDirty);
    if (isDirty) {
        CALayer *layerForDrawing = _pictureRecorder.getOrCreateLayerForDrawing(updateItem.drawingType, updateItem.itemHash);
        [self bindCALayerToCurrentComponent:layerForDrawing];
        TMMCALayerDrawTextSkBitmapV3(skBitmap, cacheKey, width, height, ovcompose::PictureRecorder::density, &(updateItem.saveState), layerForDrawing, _experimentalConfig);
    }
}

- (void)drawNullText:(int)cacheKey width:(int)width height:(int)height {
    const TMMNativeDrawingType drawingType = TMMNativeDrawingTypeImageData;
    // 使用 hashMerge 替代 hashFloats，避免 cacheKey 转 float 精度丢失
    const uint64_t drawingContentHash = hashMerge((NSUInteger)(uint32_t)cacheKey, hashMerge((NSUInteger)width, hashMerge((NSUInteger)height, (NSUInteger)drawingType)));
    ovcompose::PictureRecorderUpdateInfo updateItem = _pictureRecorder.draw(drawingType, drawingContentHash);
    bool isDirty = updateItem.isDirty;
    NSLog(@"[PV2] layer:%p drawTextSkBitmap552(ptr:%ld, width:%d, height:%d) dirty:%d", self.adaptivedCanvas, width, height, isDirty);
    const intptr_t viewPorxyPtr = (intptr_t)self;
    ovmp_fastlog_log8(OVMP_FASTLOG_TAG_TEXT,
                      ovmp_get_current_trace_id(),
                      OVMPFastLogPhaseTextOnCommitDrawNullText,
                      viewPorxyPtr,
                      cacheKey,
                      drawingContentHash,
                      updateItem.itemHash,
                      ((int64_t)width << 16) | (int64_t)height,
                      isDirty);
    if (isDirty) {
        CALayer *layerForDrawing = _pictureRecorder.getOrCreateLayerForDrawing(updateItem.drawingType, updateItem.itemHash);
        TMMCALayerDrawNullTextV3(cacheKey, width, height, ovcompose::PictureRecorder::density, &(updateItem.saveState), layerForDrawing, _experimentalConfig);
    }
}

- (void)drawTextSkBitmapWithUIImagePtr:(intptr_t)imagePtr width:(int)width height:(int)height {
    const TMMNativeDrawingType drawingType = TMMNativeDrawingTypeImageData;
    // 修复 imagePtr 转 float 精度丢失导致的 hash 碰撞问题
    uint64_t drawingContentHash = hashMerge((NSUInteger)imagePtr, hashMerge((NSUInteger)width, (NSUInteger)height));

    ovcompose::PictureRecorderUpdateInfo updateItem = _pictureRecorder.draw(drawingType, drawingContentHash);
    bool isDirty = updateItem.isDirty;
    const intptr_t viewPorxyPtr = (intptr_t)self;
    ovmp_fastlog_log8(OVMP_FASTLOG_TAG_TEXT,
                      ovmp_get_current_trace_id(),
                      OVMPFastLogPhaseTextOnExecSkBitmapWithUIImagePtr,
                      viewPorxyPtr,
                      imagePtr,
                      drawingContentHash,
                      updateItem.itemHash,
                      ((int64_t)width << 16) | (int64_t)height,
                      isDirty);
    if (isDirty) {
        CALayer *layerForDrawing = _pictureRecorder.getOrCreateLayerForDrawing(updateItem.drawingType, updateItem.itemHash);
        [self bindCALayerToCurrentComponent:layerForDrawing];
        TMMCALayerDrawTextSkBitmapWithUIImagePtrV3(imagePtr, width, height, ovcompose::PictureRecorder::density, &(updateItem.saveState), layerForDrawing, _experimentalConfig);
    }
    NSLog(@"[PV2] layer:%p dirty:%@ drawTextSkBitmapWithUIImagePtr566(imagePtr:%ld, width:%d, height:%d)", self.adaptivedCanvas, @(isDirty), imagePtr,
          width, height);
}

- (BOOL)drawImageWithCacheKeyIfNeed:(int32_t)cacheKey width:(int)width height:(int)height {
    UIImage *image = [[TMMComposeMemoryCache sharedInstance] objectForKey:@(cacheKey)];
    if (!image) {
        return YES;
    }
    
    const TMMNativeDrawingType drawingType = TMMNativeDrawingTypeImageData;
    // 使用 hashMerge 替代 hashFloats，避免 cacheKey（paragraphHashCode）转 float 时精度丢失
    // 当 cacheKey 值较大时（如 -1275470843 和 -1275470811），转 float 会丢失低位精度，
    // 导致不同 selection 范围产生相同的 drawingContentHash，PictureRecorder 判断 isDirty=false，CALayer 不更新
    const uint64_t drawingContentHash = hashMerge((NSUInteger)(uint32_t)cacheKey, hashMerge((NSUInteger)width, (NSUInteger)height));
    ovcompose::PictureRecorderUpdateInfo updateItem = _pictureRecorder.draw(drawingType, drawingContentHash);
    bool isDirty = updateItem.isDirty;
    const intptr_t viewPorxyPtr = (intptr_t)self;
    ovmp_fastlog_log8(OVMP_FASTLOG_TAG_TEXT,
                      ovmp_get_current_trace_id(),
                      OVMPFastLogPhaseTextOnExecDrawImageWithCacheKeyIfNeed,
                      viewPorxyPtr,
                      cacheKey,
                      drawingContentHash,
                      updateItem.itemHash,
                      ((int64_t)width << 16) | (int64_t)height,
                      isDirty);
    if (isDirty) {
        CALayer *layerForDrawing = _pictureRecorder.getOrCreateLayerForDrawing(updateItem.drawingType, updateItem.itemHash);
        TMMCALayerDrawTextSkBitmapUIImageV3(image, width, height, ovcompose::PictureRecorder::density, &(updateItem.saveState), layerForDrawing, _experimentalConfig);
    }
    return NO;
}

- (void)asyncDrawIntoCanvas:(TMMNativeOneResultBlock)globalTask cacheKey:(int32_t)cacheKey width:(int)width height:(int)height drawCompleted:(TMMNativeTextImageDrawCompleted)drawCompleted {
    const TMMNativeDrawingType drawingType = TMMNativeDrawingTypeImageData;
    // 与 drawImageWithCacheKeyIfNeed 使用相同的 hash 计算方式
    // 确保同一个 Text 组件在缓存命中和未命中时分配到相同的 itemIndex，避免 Layer contents 混乱
    // 使用 hashMerge 替代 hashFloats，避免 cacheKey 转 float 精度丢失导致 hash 碰撞
    const uint64_t drawingContentHash = hashMerge((NSUInteger)(uint32_t)cacheKey, hashMerge((NSUInteger)width, (NSUInteger)height));
    
    ovcompose::PictureRecorderUpdateInfo updateItem = _pictureRecorder.draw(drawingType, drawingContentHash);
    bool isDirty = updateItem.isDirty;
    const intptr_t viewPorxyPtr = (intptr_t)self;
    ovmp_fastlog_log8(OVMP_FASTLOG_TAG_TEXT,
                      ovmp_get_current_trace_id(),
                      OVMPFastLogPhaseTextOnCommitaAsyncDrawIntoCanvas,
                      viewPorxyPtr,
                      cacheKey,
                      drawingContentHash,
                      updateItem.itemHash,
                      ((int64_t)width << 16) | (int64_t)height,
                      isDirty);
    if (updateItem.isDirty) {
        CALayer *layerForDrawing = _pictureRecorder.getOrCreateLayerForDrawing(updateItem.drawingType, updateItem.itemHash);
        [self bindCALayerToCurrentComponent:layerForDrawing];
        TMMCALayerDrawTextAsyncTaskV3(globalTask, cacheKey, width, height, ovcompose::PictureRecorder::density, &(updateItem.saveState), layerForDrawing, _experimentalConfig, drawCompleted);
    }
}

- (intptr_t)imageFromImageBitmap:(intptr_t)imageBitmap cacheKey:(int32_t)cacheKey {
    UIImage *image = TMMNativeComposeUIImageFromSkBitmap(imageBitmap, cacheKey, self.experimentalConfig);
    CFTypeRef imageRef = (__bridge_retained CFTypeRef)image;
    return (intptr_t)imageRef;
}

- (intptr_t)imageFromCanvasPtr:(void *)canvasPtr colorArgb:(int)colorArgb {
    if (canvasPtr == NULL) {
        return 0;
    }
    UIImage *image = TMMCGBitmapCanvasGetImage(canvasPtr, colorArgb);
    if (image == NULL) {
        return 0;
    }
    CFTypeRef imageRef = (__bridge_retained CFTypeRef)image;
    return (intptr_t)imageRef;
}

- (void)drawRawPoints:(NSArray<NSNumber *> *)points paint:(TMMComposeNativePaint *)paint {
    // 获取hash，辅助PictureRecord功能
    uint64_t pointHash = TMMFNVHashFloatArray(points);
    uint64_t paintHash = TMMNativeDataHashFromPaint(paint);
    
    const TMMNativeDrawingType drawingType = TMMNativeDrawingTypePoints;
    const uint64_t drawingContentHash = hashMerge(pointHash, paintHash);
    
    ovcompose::PictureRecorderUpdateInfo updateItem = _pictureRecorder.draw(drawingType, drawingContentHash);
    if (updateItem.isDirty) {
        CALayer *layerForDrawing = _pictureRecorder.getOrCreateLayerForDrawing(updateItem.drawingType, updateItem.itemHash);
        [self bindCALayerToCurrentComponent:layerForDrawing];
        TMMCALayerDrawRawPointsV3(points, paint, ovcompose::PictureRecorder::density, &(updateItem.saveState), layerForDrawing, _experimentalConfig);
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
    const float density = ovcompose::PictureRecorder::density;
    UIView *targetView = self.canvasView;
    return OVCMPSnapshotImageFromUIView(targetView, CGRectGetWidth(targetView.frame), CGRectGetHeight(targetView.frame), density);
}

- (UIImage *)getSnapshotImageWithWidth:(int)width height:(int)height {
    const float density = ovcompose::PictureRecorder::density;
    return OVCMPSnapshotImageFromUIView(self.canvasView, width / density, height / density, density);
}

- (NSString *)description {
    return [NSString stringWithFormat:@"<ViewProxyV3:%p, view:%p layer:%p>", self, self.canvasView, self.canvasView.layer];
}

- (NSInteger)nativePtr {
    intptr_t ptr = (intptr_t)(__bridge void *)self.canvasView;
    return ptr;
}

#pragma mark - LayoutInspector CALayer Mapping

- (void)beginComponentDraw:(int32_t)semanticsId {
    if (!TMMLookInDebugEnabel()) {
        return;
    }
    [self.semanticsIdStack addObject:@(semanticsId)];
}

- (void)endComponentDraw:(int32_t)semanticsId {
    if (!TMMLookInDebugEnabel()) {
        return;
    }
    if (self.semanticsIdStack.count > 0) {
        [self.semanticsIdStack removeLastObject];
    }
}

- (void)bindCALayerToCurrentComponent:(CALayer *)layer {
    if (!TMMLookInDebugEnabel()) {
        return;
    }
    NSNumber *currentId = self.semanticsIdStack.lastObject;
    if (currentId && layer != nil) {
        layer.tmmComposeSemanticsId = currentId.intValue;
    }
}

- (NSArray<CALayer *> *)getCALayersForComponent:(int32_t)semanticsId {
    if (semanticsId <= 0) {
        return @[];
    }

    NSMutableArray<CALayer *> *result = [NSMutableArray array];
    // 递归遍历所有 sublayers，找到 semanticsId 匹配的 CALayer
    [self collectLayersWithSemanticsId:semanticsId fromLayer:self.adaptivedCanvas intoArray:result];
    return result;
}

/// 递归遍历 layer 及其所有 sublayers，收集 semanticsId 匹配的 CALayer
- (void)collectLayersWithSemanticsId:(int32_t)semanticsId fromLayer:(CALayer *)layer intoArray:(NSMutableArray<CALayer *> *)result {
    if (layer == nil) {
        return;
    }

    // 检查当前 layer 是否匹配
    if (layer.tmmComposeSemanticsId == semanticsId) {
        [result addObject:layer];
    }

    // 递归检查 sublayers
    for (CALayer *sublayer in layer.sublayers) {
        [self collectLayersWithSemanticsId:semanticsId fromLayer:sublayer intoArray:result];
    }
}

/// For tests only.
-(ovcompose::PictureRecorder) pictureRecorder{
    return _pictureRecorder;
}

@end
