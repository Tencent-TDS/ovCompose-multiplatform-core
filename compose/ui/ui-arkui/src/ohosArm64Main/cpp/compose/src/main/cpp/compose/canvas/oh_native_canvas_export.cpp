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

#include "oh_native_canvas_export.h"

#include "libkn_api.h"
#include "../constants/oh_native_constants.h"
#include "../shader/oh_native_image_shader.h"
#include "../shader/oh_native_linear_gradient_shader.h"
#include "../shader/oh_native_radial_gradient_shader.h"
#include "../shader/oh_native_sweep_gradient_shader.h"
#include "../xcomponent_log.h"
#include "../paragraph/oh_native_paragraph.h"
#include "../paint/oh_compose_native_paint.h"
#include "oh_native_canvas_proxy.h"
#include "oh_native_canvas_proxy_factory.h"
#include "../render_node/oh_async_task_render_node.h"
#include "oh_native_canvas_layer_drawer.h"

#include <multimedia/image_framework/image/pixelmap_native.h>
#include <native_drawing/drawing_canvas.h>
#include <native_drawing/drawing_shader_effect.h>

// Kotlin/Native生成的回调函数声明

EXTERN_C_START
/// OHNativeCanvasProxy related methods
OHNativeCanvasProxy_Handle androidx_compose_ui_arkui_utils_createOHNativeCanvasProxy(void *factory) {
    auto canvasFactory = reinterpret_cast<androidx::compose::ui::arkui::utils::OHNativeCanvasProxyFactory *>(factory);
    return reinterpret_cast<OHNativeCanvasProxy_Handle>(canvasFactory->CreateOHNativeCanvasProxy());
    LOGI("androidx_compose_ui_arkui_utils_createOHNativeCanvasProxy");
}

void androidx_compose_ui_arkui_utils_DisposeOHNativeCanvasProxy(OHNativeCanvasProxy_Handle proxyHandle) {
    auto canvasProxy = reinterpret_cast<androidx::compose::ui::arkui::utils::OHNativeCanvasProxy *>(proxyHandle);
    LOGI("androidx_compose_ui_arkui_utils_DisposeOHNativeCanvasProxy %{public}p", canvasProxy);
    // delete canvasProxy here to avoid memory leak
    delete canvasProxy;
};

/// OHNativeCanvasProxy drawing methods
void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_beginDraw(OHNativeCanvasProxy_Handle proxyHandle) {
    auto canvasProxy = reinterpret_cast<androidx::compose::ui::arkui::utils::OHNativeCanvasProxy *>(proxyHandle);
    canvasProxy->beginDraw();
    LOGI("androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_beginDraw");
}

void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawRect(OHNativeCanvasProxy_Handle proxy, float left,
                                                                  float top, float right, float bottom,
                                                                  OHComposeNativePaint_Handle paint) {
    auto nativePaint = reinterpret_cast<OH::OHComposeNativePaint *>(paint);
    auto canvasProxy = reinterpret_cast<androidx::compose::ui::arkui::utils::OHNativeCanvasProxy *>(proxy);
    canvasProxy->drawRect(left, top, right, bottom, nativePaint);
    LOGI("androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawRect");
}

void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawRoundRect(OHNativeCanvasProxy_Handle proxy, float left,
                                                                       float top, float right, float bottom,
                                                                       float radiusX, float radiusY,
                                                                       OHComposeNativePaint_Handle paint) {
    LOGI("androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawRoundRect: "
         "start");
    auto nativePaint = reinterpret_cast<OH::OHComposeNativePaint *>(paint);
    auto canvasProxy = reinterpret_cast<androidx::compose::ui::arkui::utils::OHNativeCanvasProxy *>(proxy);
    canvasProxy->drawRoundRect(left, top, right, bottom, radiusX, radiusY, nativePaint);
}

void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawCircle(OHNativeCanvasProxy_Handle proxy, float centerX,
                                                                    float centerY, float radius,
                                                                    OHComposeNativePaint_Handle paint) {
    LOGI("androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawCircle: start");
    auto nativePaint = reinterpret_cast<OH::OHComposeNativePaint *>(paint);
    auto canvasProxy = reinterpret_cast<androidx::compose::ui::arkui::utils::OHNativeCanvasProxy *>(proxy);
    canvasProxy->drawCircle(centerX, centerY, radius, nativePaint);
}

void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawOval(OHNativeCanvasProxy_Handle proxy, float left,
                                                                  float top, float right, float bottom,
                                                                  OHComposeNativePaint_Handle paint) {
    LOGI("androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawOval: start");
    auto nativePaint = reinterpret_cast<OH::OHComposeNativePaint *>(paint);
    auto canvasProxy = reinterpret_cast<androidx::compose::ui::arkui::utils::OHNativeCanvasProxy *>(proxy);
    canvasProxy->drawOval(left, top, right, bottom, nativePaint);
}

void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawArc(OHNativeCanvasProxy_Handle proxy, float left,
                                                                 float top, float right, float bottom,
                                                                 float startAngle, float sweepAngle, bool useCenter,
                                                                 OHComposeNativePaint_Handle paint) {
    LOGI("androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawArc: start");
    auto nativePaint = reinterpret_cast<OH::OHComposeNativePaint *>(paint);
    auto canvasProxy = reinterpret_cast<androidx::compose::ui::arkui::utils::OHNativeCanvasProxy *>(proxy);
    canvasProxy->drawArc(left, top, right, bottom, startAngle, sweepAngle, useCenter, nativePaint);
}

void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawPath(OHNativeCanvasProxy_Handle proxy,
                                                                  OH_Drawing_Path_Handle path,
                                                                  OHComposeNativePaint_Handle paint) {
    LOGI("androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawPath: start");
    auto nativePaint = reinterpret_cast<OH::OHComposeNativePaint *>(paint);
    auto canvasProxy = reinterpret_cast<androidx::compose::ui::arkui::utils::OHNativeCanvasProxy *>(proxy);
    canvasProxy->drawPath(path, nativePaint);
}

void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawImageRect(OHNativeCanvasProxy_Handle proxy, void *pixelMap,
                                                                       int32_t srcX, int32_t srcY, int32_t srcWidth,
                                                                       int32_t srcHeight, int32_t dstX, int32_t dstY,
                                                                       int32_t dstWidth, int32_t dstHeight,
                                                                       OHComposeNativePaint_Handle paint) {
    LOGI("androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawImageRect: start");
    auto nativePaint = reinterpret_cast<OH::OHComposeNativePaint *>(paint);
    auto canvasProxy = reinterpret_cast<androidx::compose::ui::arkui::utils::OHNativeCanvasProxy *>(proxy);
    canvasProxy->drawImageRect(pixelMap, srcX, srcY, srcWidth, srcHeight, dstX, dstY, dstWidth, dstHeight, nativePaint);
}

void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawPoints(OHNativeCanvasProxy_Handle proxy,
                                                                    uint32_t pointMode, const float *points,
                                                                    size_t pointCount, OHComposeNativePaint_Handle paint) {
    LOGI("androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawPoints: pointMode=%{public}u, pointCount=%{public}zu", pointMode, pointCount);
    auto nativePaint = reinterpret_cast<OH::OHComposeNativePaint *>(paint);
    auto canvasProxy = reinterpret_cast<androidx::compose::ui::arkui::utils::OHNativeCanvasProxy *>(proxy);
    canvasProxy->drawPoints(static_cast<OH_Drawing_PointMode>(pointMode), points, pointCount, nativePaint);
}

void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawLine(OHNativeCanvasProxy_Handle proxy, float x1, float y1,
                                                                  float x2, float y2,
                                                                  OHComposeNativePaint_Handle paint) {
    auto nativePaint = reinterpret_cast<OH::OHComposeNativePaint *>(paint);
    auto canvasProxy = reinterpret_cast<androidx::compose::ui::arkui::utils::OHNativeCanvasProxy *>(proxy);
    canvasProxy->drawLine(x1, y1, x2, y2, nativePaint);
    LOGI("androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawLine");
}

void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_finishDraw(OHNativeCanvasProxy_Handle proxy) {
    auto canvasProxy = reinterpret_cast<androidx::compose::ui::arkui::utils::OHNativeCanvasProxy *>(proxy);
    canvasProxy->finishDraw();
    LOGI("androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_finishDraw");
}

void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawLayer(OHNativeCanvasProxy_Handle proxy,
                                                                   BaseRenderNode_Handle renderNodeHandle) {
    auto renderNode = reinterpret_cast<OH::BaseRenderNode *>(renderNodeHandle);
    auto canvasProxy = reinterpret_cast<androidx::compose::ui::arkui::utils::OHNativeCanvasProxy *>(proxy);
    canvasProxy->drawLayer(renderNode);
    LOGI("androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawLayer");
}

void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawParagraph(OHNativeCanvasProxy_Handle proxy,
                                                                       BaseRenderNode_Handle paragraphHandle) {
    auto paragraph = reinterpret_cast<OH::BaseRenderNode *>(paragraphHandle);
    auto canvasProxy = reinterpret_cast<androidx::compose::ui::arkui::utils::OHNativeCanvasProxy *>(proxy);
    if (paragraph->getType() != OH_DrawingNode_Type::ParagraphNode) {
        LOGE("androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawParagraph: "
             "invalid paragraph node type");
        throw std::invalid_argument("Invalid paragraph node type");
    }
    canvasProxy->drawParagraph(dynamic_cast<OH::Paragraph *>(paragraph));
    LOGI("androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawParagraph");
}

void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_attachToRootView(OHNativeCanvasProxy_Handle proxy) {
    auto canvasProxy = reinterpret_cast<androidx::compose::ui::arkui::utils::OHNativeCanvasProxy *>(proxy);
    canvasProxy->attachToRootView();
    LOGI("androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_attachToRootView");
}

void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_setParent(OHNativeCanvasProxy_Handle proxy,
                                                                   OHNativeCanvasProxy_Handle parentProxy) {
    auto canvasProxy = reinterpret_cast<androidx::compose::ui::arkui::utils::OHNativeCanvasProxy *>(proxy);
    auto canvasParentProxy = reinterpret_cast<androidx::compose::ui::arkui::utils::OHNativeCanvasProxy *>(parentProxy);
    canvasProxy->setParent(canvasParentProxy);
    LOGI("androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_setParent");
}

void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawLayerWithSubproxy(OHNativeCanvasProxy_Handle proxy,
                                                                               OHNativeCanvasProxy_Handle subProxy) {
    auto canvasProxy = reinterpret_cast<androidx::compose::ui::arkui::utils::OHNativeCanvasProxy *>(proxy);
    auto subCanvasProxy = reinterpret_cast<androidx::compose::ui::arkui::utils::OHNativeCanvasProxy *>(subProxy);
    canvasProxy->drawLayerWithSubproxy(subCanvasProxy);
    LOGI("androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_"
         "drawLayerWithSubproxy");
}

void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_markSelfAsNodeGroup(const OHNativeCanvasProxy_Handle proxy) {
    const auto canvasProxy = reinterpret_cast<androidx::compose::ui::arkui::utils::OHNativeCanvasProxy *>(proxy);
    canvasProxy->markSelfAsNodeGroup();
    LOGI("androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_markSelfAsNodeGroup");
}

void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_removeCanvasNodeFromParent(OHNativeCanvasProxy_Handle proxy) {
    auto canvasProxy = reinterpret_cast<androidx::compose::ui::arkui::utils::OHNativeCanvasProxy *>(proxy);
    canvasProxy->removeCanvasNodeFromParent();
    LOGI("androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_removeCanvasNodeFromParent");
}

/// OHNativeCanvasProxy state operations
void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_clipRect(OHNativeCanvasProxy_Handle proxy, float left,
                                                                  float top, float right, float bottom,
                                                                  uint32_t clipOp) {
    auto canvasProxy = reinterpret_cast<androidx::compose::ui::arkui::utils::OHNativeCanvasProxy *>(proxy);
    canvasProxy->clipRect(left, top, right, bottom, static_cast<OH_Native_Draw_ClipOp>(clipOp));
    LOGI("androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_clipRect: start "
         "left:%{public}f, top:%{public}f, "
         "right:%{public}f, bottom:%{public}f, clipOp:%{public}u",
         left, top, right, bottom, clipOp);
}

void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_clipPath(OHNativeCanvasProxy_Handle proxy,
                                                                  OH_Drawing_Path_Handle path, const uint32_t clipOp) {
    auto canvasProxy = reinterpret_cast<androidx::compose::ui::arkui::utils::OHNativeCanvasProxy *>(proxy);
    canvasProxy->clipPath(path, static_cast<OH_Native_Draw_ClipOp>(clipOp));
    LOGI("androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_clipPath: "
         "path:%{public}p, clipOp:%{public}d",
         path, clipOp);
}

void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_clipRoundRect(OHNativeCanvasProxy_Handle proxy,
                                                                       const float left, const float top,
                                                                       const float right, const float bottom,
                                                                       const float radiusX, const float radiusY,
                                                                       const uint32_t clipOp) {
    auto canvasProxy = reinterpret_cast<androidx::compose::ui::arkui::utils::OHNativeCanvasProxy *>(proxy);
    canvasProxy->clipRoundRect(left, top, right, bottom, radiusX, radiusY, static_cast<OH_Native_Draw_ClipOp>(clipOp));
    LOGI("androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_clipRoundRect: "
         "rect:(%{public}f, %{public}f, %{public}f, %{public}f), "
         "radius:(%{public}f, %{public}f), clipOp:%{public}d",
         left, top, right, bottom, radiusX, radiusY, clipOp);
}

void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_clearClip(OHNativeCanvasProxy_Handle proxy) {
    auto canvasProxy = reinterpret_cast<androidx::compose::ui::arkui::utils::OHNativeCanvasProxy *>(proxy);
    canvasProxy->clearClip();
    LOGI("androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_clearClip");
}

void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_saveLayer(OHNativeCanvasProxy_Handle proxy, float left,
                                                                   float top, float right, float bottom,
                                                                   OHComposeNativePaint_Handle paint) {
    auto canvasProxy = reinterpret_cast<androidx::compose::ui::arkui::utils::OHNativeCanvasProxy *>(proxy);
    auto nativePaint = reinterpret_cast<OH::OHComposeNativePaint *>(paint);
    canvasProxy->saveLayer(left, top, right, bottom, nativePaint);
    LOGI("androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_saveLayer: bounds=(%f,%f,%f,%f)", left, top, right,
         bottom);
}

void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_enableZ(OHNativeCanvasProxy_Handle proxy) {
    auto canvasProxy = reinterpret_cast<androidx::compose::ui::arkui::utils::OHNativeCanvasProxy *>(proxy);
    canvasProxy->enableZ();
    LOGI("androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_enableZ");
}

void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_disableZ(OHNativeCanvasProxy_Handle proxy) {
    auto canvasProxy = reinterpret_cast<androidx::compose::ui::arkui::utils::OHNativeCanvasProxy *>(proxy);
    canvasProxy->disableZ();
    LOGI("androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_disableZ");
}

void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_setPosition(OHNativeCanvasProxy_Handle proxy, int32_t x,
                                                                     int32_t y) {
    auto canvasProxy = reinterpret_cast<androidx::compose::ui::arkui::utils::OHNativeCanvasProxy *>(proxy);
    canvasProxy->setPosition(x, y);
    LOGI("androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_setPosition: "
         "x:%{public}d, y:%{public}d",
         x, y);
}

void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_setBounds(OHNativeCanvasProxy_Handle proxy, int32_t originX,
                                                                   int32_t originY, int32_t boundsWidth,
                                                                   int32_t boundsHeight) {
    auto canvasProxy = reinterpret_cast<androidx::compose::ui::arkui::utils::OHNativeCanvasProxy *>(proxy);
    canvasProxy->setBounds(originX, originY, boundsWidth, boundsHeight);
    LOGI("androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_setBounds: "
         "originX:%{public}d, originY:%{public}d, "
         "boundsWidth:%{public}d, boundsHeight:%{public}d",
         originX, originY, boundsWidth, boundsHeight);
}

void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_setPivot(OHNativeCanvasProxy_Handle proxy, float px,
                                                                  float py) {
    auto canvasProxy = reinterpret_cast<androidx::compose::ui::arkui::utils::OHNativeCanvasProxy *>(proxy);
    canvasProxy->setPivot(px, py);
    LOGI("androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_setPivot: "
         "px:%{public}f, py:%{public}f",
         px, py);
}

void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_setOpacity(OHNativeCanvasProxy_Handle proxy, float opacity) {
    auto canvasProxy = reinterpret_cast<androidx::compose::ui::arkui::utils::OHNativeCanvasProxy *>(proxy);
    canvasProxy->setOpacity(opacity);
    LOGI("androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_setOpacity: "
         "opacity:%{public}f",
         opacity);
}

void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_applyTransformMatrix(OHNativeCanvasProxy_Handle handle,
                                                                              const float rotationX, const float rotationY,
                                                                              const float rotationZ, const float scaleX,
                                                                              const float scaleY, const float translateX,
                                                                              const float translateY, const double transformM34) {
    const auto canvasProxy = reinterpret_cast<androidx::compose::ui::arkui::utils::OHNativeCanvasProxy *>(handle);
    canvasProxy->applyTransformMatrix(rotationX, rotationY, rotationZ, scaleX, scaleY, translateX, translateY,
                                      transformM34);
    LOGI("androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_applyTransformMatrix: "
         "rotationX=%{public}f, rotationY=%{public}f, rotationZ=%{public}f, "
         "scaleX=%{public}f, scaleY=%{public}f, translateX=%{public}f, translateY=%{public}f, "
         "transformM34=%{public}lf",
         rotationX, rotationY, rotationZ, scaleX, scaleY, translateX, translateY, transformM34);
}

void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_save(OHNativeCanvasProxy_Handle proxy) {
    auto canvasProxy = reinterpret_cast<androidx::compose::ui::arkui::utils::OHNativeCanvasProxy *>(proxy);
    canvasProxy->save();
    LOGI("androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_save");
}

void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_restore(OHNativeCanvasProxy_Handle proxy) {
    auto canvasProxy = reinterpret_cast<androidx::compose::ui::arkui::utils::OHNativeCanvasProxy *>(proxy);
    canvasProxy->restore();
    LOGI("androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_restore");
}

void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_translate(OHNativeCanvasProxy_Handle proxy, float dx,
                                                                   float dy) {
    auto canvasProxy = reinterpret_cast<androidx::compose::ui::arkui::utils::OHNativeCanvasProxy *>(proxy);
    canvasProxy->translate(dx, dy);
    LOGI("androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_translate: "
         "dx:%{public}f, dy:%{public}f",
         dx, dy);
}

void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_scale(OHNativeCanvasProxy_Handle proxy, float sx, float sy) {
    auto canvasProxy = reinterpret_cast<androidx::compose::ui::arkui::utils::OHNativeCanvasProxy *>(proxy);
    canvasProxy->scale(sx, sy);
    LOGI("androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_scale: "
         "sx:%{public}f, sy:%{public}f",
         sx, sy);
}

void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_rotate(OHNativeCanvasProxy_Handle proxy, float degrees) {
    auto canvasProxy = reinterpret_cast<androidx::compose::ui::arkui::utils::OHNativeCanvasProxy *>(proxy);
    canvasProxy->rotate(degrees);
    LOGI("androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_rotate: "
         "degrees:%{public}f",
         degrees);
}

void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_skew(OHNativeCanvasProxy_Handle proxy, float sx, float sy) {
    auto canvasProxy = reinterpret_cast<androidx::compose::ui::arkui::utils::OHNativeCanvasProxy *>(proxy);
    canvasProxy->skew(sx, sy);
    LOGI("androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_skew: "
         "sx:%{public}f, sy:%{public}f",
         sx, sy);
}

void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_concat(OHNativeCanvasProxy_Handle proxy,
                                                                const float *matrix16) {
    auto canvasProxy = reinterpret_cast<androidx::compose::ui::arkui::utils::OHNativeCanvasProxy *>(proxy);
    canvasProxy->concat(matrix16);
    LOGI("androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_concat");
}

/// OHComposeNativePaint related methods
OHComposeNativePaint_Handle androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_Paint(OHNativeCanvasProxy_Handle proxy) {
    auto canvasProxy = reinterpret_cast<androidx::compose::ui::arkui::utils::OHNativeCanvasProxy *>(proxy);
    return reinterpret_cast<OHComposeNativePaint_Handle>(canvasProxy->Paint());
}

/// NativeShader related methods
NativeBasicShader_Handle androidx_compose_ui_arkui_utils_createNativeLinearGradientShader(
    const float startX, const float startY, const float endX, const float endY, uint32_t *colors, float *colorPositions,
    uint32_t colorCount, uint32_t tileMode) {
    const auto shader = new OH::NativeLinearGradientShader();
    shader->setStart(startX, startY)
        ->setEnd(endX, endY)
        ->setColors(colors, colorPositions, colorCount)
        ->setTileMode(static_cast<OH_Drawing_TileMode>(tileMode));
    LOGI("androidx_compose_ui_arkui_utils_createNativeLinearGradientShader: "
         "startX=%{public}f, startY=%{public}f, "
         "endX=%{public}f, endY=%{public}f, tileMode=%{public}u",
         startX, startY, endX, endY, tileMode);
    return reinterpret_cast<NativeBasicShader_Handle>(shader);
}

NativeBasicShader_Handle androidx_compose_ui_arkui_utils_createNativeRadialGradientShader(
    const float centerX, const float centerY, const float radius, uint32_t *colors, float *colorPositions,
    const uint32_t colorCount, uint32_t tileMode) {
    const auto shader = new OH::NativeRadialGradientShader();
    shader->setCenter(centerX, centerY)
        ->setRadius(radius)
        ->setColors(colors, colorPositions, colorCount)
        ->setTileMode(static_cast<OH_Drawing_TileMode>(tileMode));
    LOGI("androidx_compose_ui_arkui_utils_createNativeRadialGradientShader: "
         "centerX=%{public}f, centerY=%{public}f, "
         "radius=%{public}f, tileMode=%{public}u",
         centerX, centerY, radius, tileMode);
    return reinterpret_cast<NativeBasicShader_Handle>(shader);
}

NativeBasicShader_Handle androidx_compose_ui_arkui_utils_createNativeSweepGradientShader(
    const float centerX, const float centerY, uint32_t *colors, float *colorPositions, const uint32_t colorCount) {
    const auto shader = new OH::NativeSweepGradientShader();
    shader->setCenter(centerX, centerY)->setColors(colors, colorPositions, colorCount);
    LOGI("androidx_compose_ui_arkui_utils_createNativeSweepGradientShader: "
         "centerX=%{public}f, centerY=%{public}f",
         centerX, centerY);
    return reinterpret_cast<NativeBasicShader_Handle>(shader);
}

NativeBasicShader_Handle androidx_compose_ui_arkui_utils_createNativeImageShader(void *pixelMapHandle,
                                                                                 uint32_t tileModeX,
                                                                                 uint32_t tileModeY) {
    const auto shader = new OH::NativeImageShader();
    shader->setTileMode(static_cast<OH_Drawing_TileMode>(tileModeX), static_cast<OH_Drawing_TileMode>(tileModeY));
    shader->image = static_cast<OH_Drawing_PixelMap *>(pixelMapHandle);
    LOGI("androidx_compose_ui_arkui_utils_createNativeImageShader: "
         "image=%{public}p, tileModeX=%{public}u, "
         "tileModeY=%{public}u",
         pixelMapHandle, tileModeX, tileModeY);
    return reinterpret_cast<NativeBasicShader_Handle>(shader);
}

void *androidx_compose_ui_arkui_utils_createNativePixelMapFromPixels(
    uint8_t *pixelData, size_t dataLength, int32_t width, int32_t height, bool hasAlpha) {
    LOGI("androidx_compose_ui_arkui_utils_createNativePixelMapFromPixels: "
         "width=%{public}d, height=%{public}d, hasAlpha=%{public}d, dataLength=%{public}zu",
         width, height, hasAlpha, dataLength);

    if (pixelData == nullptr || dataLength == 0 || width <= 0 || height <= 0) {
        LOGE("androidx_compose_ui_arkui_utils_createNativePixelMapFromPixels: invalid parameters");
        return nullptr;
    }

    // 创建InitializationOptions
    OH_Pixelmap_InitializationOptions *options = nullptr;
    Image_ErrorCode result = OH_PixelmapInitializationOptions_Create(&options);
    if (result != IMAGE_SUCCESS || options == nullptr) {
        LOGE("androidx_compose_ui_arkui_utils_createNativePixelMapFromPixels: failed to create options, error=%{public}d", result);
        return nullptr;
    }

    // 设置图像属性
    OH_PixelmapInitializationOptions_SetWidth(options, static_cast<uint32_t>(width));
    OH_PixelmapInitializationOptions_SetHeight(options, static_cast<uint32_t>(height));
    OH_PixelmapInitializationOptions_SetPixelFormat(options, PIXEL_FORMAT_BGRA_8888);
    OH_PixelmapInitializationOptions_SetSrcPixelFormat(options, PIXEL_FORMAT_BGRA_8888);
    OH_PixelmapInitializationOptions_SetAlphaType(options, hasAlpha ? PIXELMAP_ALPHA_TYPE_UNPREMULTIPLIED : PIXELMAP_ALPHA_TYPE_OPAQUE);
    OH_PixelmapInitializationOptions_SetEditable(options, false); // 只读

    // 创建NativePixelMap
    OH_PixelmapNative *pixelMap = nullptr;
    result = OH_PixelmapNative_CreatePixelmap(pixelData, dataLength, options, &pixelMap);

    // 释放options
    OH_PixelmapInitializationOptions_Release(options);

    if (result != IMAGE_SUCCESS || pixelMap == nullptr) {
        LOGE("androidx_compose_ui_arkui_utils_createNativePixelMapFromPixels: failed to create pixelmap, error=%{public}d", result);
        return nullptr;
    }

    LOGI("androidx_compose_ui_arkui_utils_createNativePixelMapFromPixels: success, pixelMap=%{public}p", pixelMap);
    return reinterpret_cast<void *>(pixelMap);
}

// Text image drawing methods
void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawTextPixelMap(OHNativeCanvasProxy_Handle proxy,
                                                                          void *pixelMapNative, int32_t cacheKey,
                                                                          int32_t width, int32_t height) {
    LOGI("androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawTextPixelMap: cacheKey=%{public}d, width=%{public}d, height=%{public}d",
         cacheKey, width, height);
    auto canvasProxy = reinterpret_cast<androidx::compose::ui::arkui::utils::OHNativeCanvasProxy *>(proxy);
    canvasProxy->drawTextPixelMap(pixelMapNative, cacheKey, width, height);
}

void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawTextPixelMapWithPtr(OHNativeCanvasProxy_Handle proxy,
                                                                                 void *pixelMapPtr, int32_t width,
                                                                                 int32_t height) {
    LOGI("androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawTextPixelMapWithPtr: width=%{public}d, height=%{public}d",
         width, height);
    auto canvasProxy = reinterpret_cast<androidx::compose::ui::arkui::utils::OHNativeCanvasProxy *>(proxy);
    canvasProxy->drawTextPixelMapWithPtr(pixelMapPtr, width, height);
}

void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_asyncDrawIntoCanvas(OHNativeCanvasProxy_Handle proxy,
                                                                             int64_t globalTaskPtr, int32_t paragraphHashCode,
                                                                             int32_t width, int32_t height,
                                                                             int64_t onMainThreadUpdatePtr) {
    LOGI("androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_asyncDrawIntoCanvas: paragraphHashCode=%{public}d, width=%{public}d, height=%{public}d",
         paragraphHashCode, width, height);

    auto canvasProxy = reinterpret_cast<androidx::compose::ui::arkui::utils::OHNativeCanvasProxy *>(proxy);

    // 包装Kotlin的globalTask lambda
    auto globalTask = [globalTaskPtr]() -> int64_t {
        return invokeKotlinAsyncTask(globalTaskPtr);
    };

    // 包装Kotlin的onMainThreadUpdate lambda
    // 参数：(renderNodePtr, pixelMapPtr)
    auto onMainThreadUpdate = [onMainThreadUpdatePtr](void *renderNodePtr, int64_t pixelMapPtr) {
        // 调用Kotlin侧的回调函数
        // Kotlin lambda签名：(Long, Long) -> Unit
        invokeKotlinMainThreadCallback(onMainThreadUpdatePtr, reinterpret_cast<int64_t>(renderNodePtr), pixelMapPtr);
    };

    canvasProxy->asyncDrawIntoCanvas(globalTask, paragraphHashCode, width, height, onMainThreadUpdate);
}

bool androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_needRedrawImageWithHashCode(OHNativeCanvasProxy_Handle proxy,
                                                                                     int32_t hashCode, int32_t width,
                                                                                     int32_t height) {
    LOGI("androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_needRedrawImageWithHashCode: hashCode=%{public}d, width=%{public}d, height=%{public}d",
         hashCode, width, height);
    auto canvasProxy = reinterpret_cast<androidx::compose::ui::arkui::utils::OHNativeCanvasProxy *>(proxy);
    return canvasProxy->needRedrawImageWithHashCode(hashCode, width, height);
}

int64_t androidx_compose_ui_arkui_utils_OHNativeComposePixelMapFromImageBitmap(void *pixelMapNative, int32_t cacheKey) {
    LOGI("androidx_compose_ui_arkui_utils_OHNativeComposePixelMapFromImageBitmap: cacheKey=%{public}d", cacheKey);
    OH_PixelmapNative *pixelMap = OH::OHNativeComposePixelMapFromImageBitmap(
        reinterpret_cast<OH_PixelmapNative *>(pixelMapNative), cacheKey);
    if (pixelMap != nullptr) {
        return reinterpret_cast<int64_t>(pixelMap);
    }
    return 0;
}

void *androidx_compose_ui_arkui_utils_OHNativeComposeHasTextImageCache(int32_t cacheKey) {
    LOGI("androidx_compose_ui_arkui_utils_OHNativeComposeHasTextImageCache: cacheKey=%{public}d", cacheKey);
    OH_PixelmapNative *pixelMap = OH::OHNativeComposeHasTextImageCache(cacheKey);
    return reinterpret_cast<void *>(pixelMap);
}

void androidx_compose_ui_arkui_utils_OHAsyncTaskRenderNode_updatePixelMapOnMainThread(
    void *renderNodePtr,
    int64_t pixelMapPtr) {
    LOGI("androidx_compose_ui_arkui_utils_OHAsyncTaskRenderNode_updatePixelMapOnMainThread: renderNodePtr=%{public}p, pixelMapPtr=%{public}lld",
         renderNodePtr, pixelMapPtr);

    auto *asyncTaskNode = reinterpret_cast<OH::AsyncTaskRenderNode *>(renderNodePtr);
    asyncTaskNode->updatePixelMapAndInvalidate(pixelMapPtr);
}

EXTERN_C_END