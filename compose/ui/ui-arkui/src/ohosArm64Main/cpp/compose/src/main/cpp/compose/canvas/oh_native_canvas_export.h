/*
 * Tencent is pleased to support the open source community by making ovCompose
 * available. Copyright (C) 2025 Tencent. All rights reserved.
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

#include <napi/native_api.h>
#include <native_drawing/drawing_types.h>

#include "../constants/oh_native_constants.h"

#ifndef ANDROIDX_COMPOSE_UI_ARKUI_UTILS_OHNATIVECANVAS_EXPORT_H
#define ANDROIDX_COMPOSE_UI_ARKUI_UTILS_OHNATIVECANVAS_EXPORT_H

EXTERN_C_START
OHNativeCanvasProxy_Handle androidx_compose_ui_arkui_utils_createOHNativeCanvasProxy(void *factory);
void androidx_compose_ui_arkui_utils_DisposeOHNativeCanvasProxy(OHNativeCanvasProxy_Handle proxyHandle);
void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_beginDraw(OHNativeCanvasProxy_Handle proxyHandle);
OHComposeNativePaint_Handle androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_Paint(OHNativeCanvasProxy_Handle proxy);
void androidx_compose_ui_arkui_utils_DisposeOHComposeNativePaint(OHComposeNativePaint_Handle paintHandle);

// OHNativeCanvasProxy state operations
void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_save(OHNativeCanvasProxy_Handle proxy);
void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_restore(OHNativeCanvasProxy_Handle proxy);
void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_translate(OHNativeCanvasProxy_Handle proxy, float dx,
                                                                   float dy);
void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_scale(OHNativeCanvasProxy_Handle proxy, float sx, float sy);
void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_rotate(OHNativeCanvasProxy_Handle proxy, float degrees);
void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_skew(OHNativeCanvasProxy_Handle proxy, float sx, float sy);
void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_concat(OHNativeCanvasProxy_Handle proxy,
                                                                const float *matrix16);
void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_setBounds(OHNativeCanvasProxy_Handle proxy, int32_t originX,
                                                                   int32_t originY, int32_t boundsWidth,
                                                                   int32_t boundsHeight);
void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_setPosition(OHNativeCanvasProxy_Handle proxy, int32_t x,
                                                                     int32_t y);
void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_setPivot(OHNativeCanvasProxy_Handle proxy, float px, float py);
void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_setOpacity(OHNativeCanvasProxy_Handle proxy, float alpha);
void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_applyTransformMatrix(OHNativeCanvasProxy_Handle proxy,
                                                                              float rotationX, float rotationY,
                                                                              float rotationZ, float scaleX,
                                                                              float scaleY, float translateX,
                                                                              float translateY, double transformM34);

// OHNativeCanvasProxy drawCommand
void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawRect(OHNativeCanvasProxy_Handle proxy, float left,
                                                                  float top, float right, float bottom,
                                                                  OHComposeNativePaint_Handle paint);
void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_finishDraw(OHNativeCanvasProxy_Handle proxy);
void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawLayer(OHNativeCanvasProxy_Handle proxy,
                                                                   BaseRenderNode_Handle renderNodeHandle);
void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawParagraph(OHNativeCanvasProxy_Handle proxy,
                                                                       BaseRenderNode_Handle paragraphHandle);
void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_attachToRootView(OHNativeCanvasProxy_Handle proxy);
void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_setParent(OHNativeCanvasProxy_Handle proxy,
                                                                   OHNativeCanvasProxy_Handle parentProxy);
void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawLine(OHNativeCanvasProxy_Handle proxy, float x1, float y1,
                                                                  float x2, float y2,
                                                                  OHComposeNativePaint_Handle paint);
void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawLayerWithSubproxy(OHNativeCanvasProxy_Handle proxy,
                                                                               OHNativeCanvasProxy_Handle subProxy);
void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_markSelfAsNodeGroup(OHNativeCanvasProxy_Handle proxy);
void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_clipRect(OHNativeCanvasProxy_Handle proxy, float left,
                                                                  float top, float right, float bottom,
                                                                  uint32_t clipOp);

void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_clipPath(OHNativeCanvasProxy_Handle proxy,
                                                                  OH_Drawing_Path *path, uint32_t clipOp);

void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_clipRoundRect(OHNativeCanvasProxy_Handle proxy, float left,
                                                                       float top, float right, float bottom,
                                                                       float radiusX, float radiusY, uint32_t clipOp);
void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawRoundRect(OHNativeCanvasProxy_Handle proxy, float left,
                                                                       float top, float right, float bottom,
                                                                       float radiusX, float radiusY,
                                                                       OHComposeNativePaint_Handle paint);
void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawCircle(OHNativeCanvasProxy_Handle proxy, float centerX,
                                                                    float centerY, float radius,
                                                                    OHComposeNativePaint_Handle paint);
void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawOval(OHNativeCanvasProxy_Handle proxy, float left,
                                                                  float top, float right, float bottom,
                                                                  OHComposeNativePaint_Handle paint);
void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawArc(OHNativeCanvasProxy_Handle proxy, float left,
                                                                 float top, float right, float bottom,
                                                                 float startAngle, float sweepAngle, bool useCenter,
                                                                 OHComposeNativePaint_Handle paint);
void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawPath(OHNativeCanvasProxy_Handle proxy,
                                                                  OH_Drawing_Path *path,
                                                                  OHComposeNativePaint_Handle paint);
void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawImageRect(OHNativeCanvasProxy_Handle proxy,
                                                                       void *pixelMap, int32_t srcX, int32_t srcY,
                                                                       int32_t srcWidth, int32_t srcHeight,
                                                                       int32_t dstX, int32_t dstY, int32_t dstWidth,
                                                                       int32_t dstHeight,
                                                                       OHComposeNativePaint_Handle paint);
void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawPoints(OHNativeCanvasProxy_Handle proxy,
                                                                    uint32_t pointMode, const float *points,
                                                                    size_t pointCount, OHComposeNativePaint_Handle paint);

// Text image drawing methods
void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawTextPixelMap(OHNativeCanvasProxy_Handle proxy,
                                                                          void *pixelMapNative, int32_t cacheKey,
                                                                          int32_t width, int32_t height);
void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawTextPixelMapWithPtr(OHNativeCanvasProxy_Handle proxy,
                                                                                 void *pixelMapPtr, int32_t width,
                                                                                 int32_t height);
bool androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_needRedrawImageWithHashCode(OHNativeCanvasProxy_Handle proxy,
                                                                                     int32_t hashCode, int32_t width,
                                                                                     int32_t height);
int64_t androidx_compose_ui_arkui_utils_OHNativeComposePixelMapFromImageBitmap(void *pixelMapNative, int32_t cacheKey);
void *androidx_compose_ui_arkui_utils_OHNativeComposeHasTextImageCache(int32_t cacheKey);

// ImageBitmap to NativePixelMap conversion
void *androidx_compose_ui_arkui_utils_createNativePixelMapFromPixels(
    uint8_t *pixelData, size_t dataLength, int32_t width, int32_t height, bool hasAlpha);

// OHComposeNativePaint set paint properties
void androidx_compose_ui_arkui_utils_OHComposeNativePaint_syncAll(OHComposeNativePaint_Handle paint, float alpha,
                                                                  bool isAntiAlias, uint64_t color, float strokeWidth,
                                                                  uint32_t blendMode, uint32_t style,
                                                                  uint32_t strokeCap, uint32_t strokeJoin,
                                                                  uint32_t filterQuality, float strokeMiterLimit,
                                                                  NativeBasicShader_Handle shader);

// NativeShader related methods
NativeBasicShader_Handle
androidx_compose_ui_arkui_utils_createNativeLinearGradientShader(float startX, float startY, float endX, float endY,
                                                                 uint32_t *colors, float *colorPositions,
                                                                 uint32_t colorCount, uint32_t tileMode);
NativeBasicShader_Handle
androidx_compose_ui_arkui_utils_createNativeRadialGradientShader(float centerX, float centerY, float radius,
                                                                 uint32_t *colors, float *colorPositions,
                                                                 uint32_t colorCount, uint32_t tileMode);
NativeBasicShader_Handle androidx_compose_ui_arkui_utils_createNativeSweepGradientShader(float centerX, float centerY,
                                                                                         uint32_t *colors,
                                                                                         float *colorPositions,
                                                                                         uint32_t colorCount);
NativeBasicShader_Handle androidx_compose_ui_arkui_utils_createNativeImageShader(OH_Drawing_Image *image,
                                                                                 uint32_t tileModeX,
                                                                                 uint32_t tileModeY);
EXTERN_C_END

#endif