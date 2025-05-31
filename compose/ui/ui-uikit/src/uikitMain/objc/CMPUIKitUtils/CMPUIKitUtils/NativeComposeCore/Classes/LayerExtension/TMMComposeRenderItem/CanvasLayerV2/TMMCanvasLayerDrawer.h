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

#import "TMMCALayerSaveState.h"
#import "TMMNativeEnums.h"
#import <UIKit/UIKit.h>

@class TMMComposeNativePaint;
@class TMMNativeBasicShader;
@class TMMComposeNativeColorFilter;
@class TMMGaussianBlurFilter;

typedef intptr_t (^TMMNativeOneResultBlock)(void);

NS_ASSUME_NONNULL_BEGIN

/// 绘制裁剪图层（支持独立圆角）
///
/// - Parameters:
///   - left: 裁剪区域左边界（逻辑点）
///   - top: 裁剪区域上边界（逻辑点）
///   - right: 裁剪区域右边界（逻辑点）
///   - bottom: 裁剪区域下边界（逻辑点）
///   - topLeftCornerRadiusX: 左上角X轴圆角半径（逻辑点）
///   - topLeftCornerRadiusY: 左上角Y轴圆角半径（逻辑点）
///   - topRightCornerRadiusX: 右上角X轴圆角半径（逻辑点）
///   - topRightCornerRadiusY: 右上角Y轴圆角半径（逻辑点）
///   - bottomLeftCornerRadiusX: 左下角X轴圆角半径（逻辑点）
///   - bottomLeftCornerRadiusY: 左下角Y轴圆角半径（逻辑点）
///   - bottomRightCornerRadiusX: 右下角X轴圆角半径（逻辑点）
///   - bottomRightCornerRadiusY: 右下角Y轴圆角半径（逻辑点）
///   - density: 屏幕密度比例（用于像素精确计算）
///   - saveState: 图层保存状态（用于恢复上下文）
///   - clipLayer: 目标裁剪图层
FOUNDATION_EXTERN TMM_ALWAYS_INLINE void TMMCALayerDrawClipLayer(float left, float top, float right, float bottom, float topLeftCornerRadiusX,
                                                                 float topLeftCornerRadiusY, float topRightCornerRadiusX, float topRightCornerRadiusY,
                                                                 float bottomLeftCornerRadiusX, float bottomLeftCornerRadiusY,
                                                                 float bottomRightCornerRadiusX, float bottomRightCornerRadiusY, float density,
                                                                 const CALayerSaveState *saveState, CALayer *clipLayer);

/// 使用矩形区域绘制裁剪图层
///
/// - Parameters:
///   - left: 裁剪区域左边界（逻辑点）
///   - top: 裁剪区域上边界（逻辑点）
///   - right: 裁剪区域右边界（逻辑点）
///   - bottom: 裁剪区域下边界（逻辑点）
///   - density: 屏幕密度比例
///   - clipOp: 裁剪操作类型（并集/差集等）
///   - saveState: 图层保存状态
///   - clipLayer: 目标裁剪图层
FOUNDATION_EXTERN TMM_ALWAYS_INLINE void TMMCALayerDrawClipLayerWithRect(float left, float top, float right, float bottom, float density,
                                                                         TMMNativeDrawClipOp clipOp, const CALayerSaveState *saveState,
                                                                         CALayer *clipLayer);

/// 使用贝塞尔路径绘制裁剪图层
///
/// - Parameters:
///   - path: 裁剪路径对象（支持任意形状）
///   - clipOp: 裁剪操作类型
///   - density: 屏幕密度比例
///   - saveState: 图层保存状态
///   - clipLayer: 目标裁剪图层
FOUNDATION_EXTERN TMM_ALWAYS_INLINE void TMMCALayerDrawClipLayerWithPath(UIBezierPath *path, TMMNativeDrawClipOp clipOp, float density,
                                                                         const CALayerSaveState *saveState, CALayer *clipLayer);

/// 绘制直线
///
/// - Parameters:
///   - pointX1: 起点X坐标（逻辑点）
///   - pointY1: 起点Y坐标（逻辑点）
///   - pointX2: 终点X坐标（逻辑点）
///   - pointY2: 终点Y坐标（逻辑点）
///   - density: 屏幕密度比例
///   - paint: 绘制样式（颜色/线宽等）
///   - shader: 着色器效果
///   - saveState: 图层保存状态
///   - layerForDrawing: 目标绘制图层
FOUNDATION_EXTERN TMM_ALWAYS_INLINE void TMMCALayerDrawLine(CGFloat pointX1, CGFloat pointY1, CGFloat pointX2, CGFloat pointY2, float density,
                                                            TMMComposeNativePaint *paint, TMMNativeBasicShader *shader,
                                                            const CALayerSaveState *saveState, CALayer *layerForDrawing);

/// 绘制矩形
///
/// - Parameters:
///   - left: 矩形左边界（逻辑点）
///   - top: 矩形上边界（逻辑点）
///   - right: 矩形右边界（逻辑点）
///   - bottom: 矩形下边界（逻辑点）
///   - density: 屏幕密度比例
///   - paint: 绘制样式
///   - shader: 着色器效果
///   - saveState: 图层保存状态
///   - layerForDrawing: 目标绘制图层
FOUNDATION_EXTERN TMM_ALWAYS_INLINE void TMMCALayerDrawRect(float left, float top, float right, float bottom, float density,
                                                            TMMComposeNativePaint *paint, TMMNativeBasicShader *shader,
                                                            const CALayerSaveState *saveState, CALayer *layerForDrawing);

/// 绘制圆角矩形（支持椭圆圆角）
///
/// - Parameters:
///   - left: 矩形左边界（逻辑点）
///   - top: 矩形上边界（逻辑点）
///   - right: 矩形右边界（逻辑点）
///   - bottom: 矩形下边界（逻辑点）
///   - radiusX: X轴圆角半径（逻辑点）
///   - radiusY: Y轴圆角半径（逻辑点）
///   - density: 屏幕密度比例
///   - paint: 绘制样式
///   - shader: 着色器效果
///   - saveState: 图层保存状态
///   - layerForDrawing: 目标绘制图层
FOUNDATION_EXTERN TMM_ALWAYS_INLINE void TMMCALayerDrawRoundRect(float left, float top, float right, float bottom, float radiusX, float radiusY,
                                                                 float density, TMMComposeNativePaint *paint, TMMNativeBasicShader *shader,
                                                                 const CALayerSaveState *saveState, CALayer *layerForDrawing);

/// 绘制椭圆
///
/// - Parameters:
///   - left: 外接矩形左边界（逻辑点）
///   - top: 外接矩形上边界（逻辑点）
///   - right: 外接矩形右边界（逻辑点）
///   - bottom: 外接矩形下边界（逻辑点）
///   - density: 屏幕密度比例
///   - paint: 绘制样式
///   - saveState: 图层保存状态
///   - layerForDrawing: 目标绘制图层
FOUNDATION_EXTERN TMM_ALWAYS_INLINE void TMMCALayerDrawOval(float left, float top, float right, float bottom, float density,
                                                            TMMComposeNativePaint *paint, const CALayerSaveState *saveState,
                                                            CALayer *layerForDrawing);

/// 绘制圆形
///
/// - Parameters:
///   - centerX: 圆心X坐标（逻辑点）
///   - centerY: 圆心Y坐标（逻辑点）
///   - radius: 圆半径（逻辑点）
///   - density: 屏幕密度比例
///   - shader: 着色器效果
///   - paint: 绘制样式
///   - saveState: 图层保存状态
///   - hostingLayer: 宿主图层（用于坐标转换）
///   - layerForDrawing: 目标绘制图层
FOUNDATION_EXTERN TMM_ALWAYS_INLINE void TMMCALayerDrawCircle(float centerX, float centerY, float radius, float density, TMMNativeBasicShader *shader,
                                                              TMMComposeNativePaint *paint, const CALayerSaveState *saveState, CALayer *hostingLayer,
                                                              CALayer *layerForDrawing);

/// 绘制圆弧/扇形
///
/// - Parameters:
///   - left: 外接矩形左边界（逻辑点）
///   - top: 外接矩形上边界（逻辑点）
///   - right: 外接矩形右边界（逻辑点）
///   - bottom: 外接矩形下边界（逻辑点）
///   - density: 屏幕密度比例
///   - startAngle: 起始弧度（0表示3点钟方向）
///   - sweepAngle: 扫过弧度（正数逆时针）
///   - useCenter: 是否连接中心点形成扇形
///   - paint: 绘制样式
///   - saveState: 图层保存状态
///   - layerForDrawing: 目标绘制图层
FOUNDATION_EXTERN TMM_ALWAYS_INLINE void TMMCALayerDrawArc(float left, float top, float right, float bottom, float density, float startAngle,
                                                           float sweepAngle, bool useCenter, TMMComposeNativePaint *paint,
                                                           const CALayerSaveState *saveState, CALayer *layerForDrawing);

/// 使用贝塞尔路径绘制
///
/// - Parameters:
///   - path: 自定义路径对象
///   - shader: 着色器效果
///   - paint: 绘制样式
///   - pathOperation: 路径操作类型（描边/填充等）
///   - density: 屏幕密度比例
///   - saveState: 图层保存状态
///   - hostingLayer: 宿主图层
///   - layerForDrawing: 目标绘制图层
FOUNDATION_EXTERN TMM_ALWAYS_INLINE void TMMCALayerDrawPath(UIBezierPath *path, TMMNativeBasicShader *shader, TMMComposeNativePaint *paint,
                                                            TMMNativeDrawPathOperation pathOperation, float density,
                                                            const CALayerSaveState *saveState, CALayer *hostingLayer, CALayer *layerForDrawing);

/// 绘制图像（简单版）
///
/// - Parameters:
///   - imagePointer: 图像指针（需转换为intptr_t）
///   - topLeftOffsetX: 左上角X偏移（逻辑点）
///   - topLeftOffsetY: 左上角Y偏移（逻辑点）
///   - density: 屏幕密度比例
///   - paint: 绘制样式
///   - saveState: 图层保存状态
///   - layerForDrawing: 目标绘制图层
FOUNDATION_EXTERN TMM_ALWAYS_INLINE void TMMCALayerDrawImage(intptr_t imagePointer, float topLeftOffsetX, float topLeftOffsetY, float density,
                                                             TMMComposeNativePaint *paint, const CALayerSaveState *saveState,
                                                             CALayer *layerForDrawing);

/// 绘制Skia位图文字
///
/// - Parameters:
///   - skBitmap: Skia位图指针
///   - cacheKey: 缓存键值（用于复用）
///   - width: 位图宽度（像素）
///   - height: 位图高度（像素）
///   - density: 屏幕密度比例
///   - saveState: 图层保存状态
///   - layerForDrawing: 目标绘制图层
FOUNDATION_EXTERN TMM_ALWAYS_INLINE void TMMCALayerDrawTextSkBitmap(intptr_t skBitmap, int32_t cacheKey, int width, int height, float density,
                                                                    const CALayerSaveState *saveState, CALayer *layerForDrawing);

/// 使用 UIImage 指针绘制文字位图
///
/// - Parameters:
///   - imagePtr: UIImage 对象指针
///   - width: 位图宽度（像素）
///   - height: 位图高度（像素）
///   - density: 屏幕密度比例
///   - saveState: 图层保存状态
///   - layerForDrawing: 目标绘制图层
FOUNDATION_EXTERN TMM_ALWAYS_INLINE void TMMCALayerDrawTextSkBitmapWithUIImagePtr(intptr_t imagePtr, int width, int height, float density,
                                                                                  const CALayerSaveState *saveState, CALayer *layerForDrawing);

/// 绘制图像矩形区域（支持裁剪和滤镜）
///
/// - Parameters:
///   - imagePointer: 图像指针
///   - srcOffsetX: 源区域X偏移（像素）
///   - srcOffsetY: 源区域Y偏移（像素）
///   - srcSizeWidth: 源区域宽度（像素）
///   - srcSizeHeight: 源区域高度（像素）
///   - dstOffsetX: 目标区域X偏移（逻辑点）
///   - dstOffsetY: 目标区域Y偏移（逻辑点）
///   - dstSizeWidth: 目标区域宽度（逻辑点）
///   - dstSizeHeight: 目标区域高度（逻辑点）
///   - density: 屏幕密度比例
///   - colorFilter: 颜色滤镜
///   - blurFilter: 模糊滤镜
///   - paint: 绘制样式
///   - saveState: 图层保存状态
///   - layerForDrawing: 目标绘制图层
FOUNDATION_EXTERN TMM_ALWAYS_INLINE void TMMCALayerDrawImageRect(intptr_t imagePointer, float srcOffsetX, float srcOffsetY, int srcSizeWidth,
                                                                 int srcSizeHeight, float dstOffsetX, float dstOffsetY, int dstSizeWidth,
                                                                 int dstSizeHeight, float density, TMMComposeNativeColorFilter *colorFilter,
                                                                 TMMGaussianBlurFilter *blurFilter, TMMComposeNativePaint *paint,
                                                                 const CALayerSaveState *saveState, CALayer *layerForDrawing);

/// 异步绘制文本
///
/// - Parameters:
///   - globalTask: 全局绘制任务块
///   - cacheKey: 缓存键值
///   - width: 绘制宽度（像素）
///   - height: 绘制高度（像素）
///   - density: 屏幕密度比例
///   - saveState: 图层保存状态
///   - layerForDrawing: 目标绘制图层
FOUNDATION_EXTERN TMM_ALWAYS_INLINE void TMMCALayerDrawTextAsyncTask(TMMNativeOneResultBlock globalTask, int32_t cacheKey, int width, int height,
                                                                     float density, const CALayerSaveState *saveState, CALayer *layerForDrawing);

/// 绘制原始点集
///
/// - Parameters:
///   - points: 点坐标数组（格式：[x1,y1,x2,y2,...]）
///   - paint: 绘制样式
///   - density: 屏幕密度比例
///   - saveState: 图层保存状态
///   - layerForDrawing: 目标绘制图层
FOUNDATION_EXTERN TMM_ALWAYS_INLINE void TMMCALayerDrawRawPoints(NSArray<NSNumber *> *points, TMMComposeNativePaint *paint, float density,
                                                                 const CALayerSaveState *saveState, CALayer *layerForDrawing);

NS_ASSUME_NONNULL_END
