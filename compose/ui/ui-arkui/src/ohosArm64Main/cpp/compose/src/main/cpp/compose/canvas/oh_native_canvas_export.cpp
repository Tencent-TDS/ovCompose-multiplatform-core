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
#include "../constants/oh_native_constants.h"
#include "../shader/oh_native_basic_shader.h"
#include "../shader/oh_native_image_shader.h"
#include "../shader/oh_native_linear_gradient_shader.h"
#include "../shader/oh_native_radial_gradient_shader.h"
#include "../shader/oh_native_sweep_gradient_shader.h"
#include "../xcomponent_log.h"
#include "native_drawing/drawing_shader_effect.h"
#include "oh_compose_native_paint.h"
#include "oh_native_canvas_proxy.h"
#include "oh_native_canvas_proxy_factory.h"

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

void androidx_compose_ui_arkui_utils_DisposeOHComposeNativePaint(OHComposeNativePaint_Handle paintHandle) {
    auto paint = reinterpret_cast<androidx::compose::ui::arkui::utils::OHComposeNativePaint *>(paintHandle);
    LOGI("androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_"
         "DisposeOHComposeNativePaint %{public}p",
         paint);
    // delete paint here to avoid memory leak
    delete paint;
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
    auto nativePaint = reinterpret_cast<androidx::compose::ui::arkui::utils::OHComposeNativePaint *>(paint);
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
    auto nativePaint = reinterpret_cast<androidx::compose::ui::arkui::utils::OHComposeNativePaint *>(paint);
    auto canvasProxy = reinterpret_cast<androidx::compose::ui::arkui::utils::OHNativeCanvasProxy *>(proxy);
    canvasProxy->drawRoundRect(left, top, right, bottom, radiusX, radiusY, nativePaint);
}

void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawLine(OHNativeCanvasProxy_Handle proxy, float x1, float y1,
                                                                  float x2, float y2,
                                                                  OHComposeNativePaint_Handle paint) {
    auto nativePaint = reinterpret_cast<androidx::compose::ui::arkui::utils::OHComposeNativePaint *>(paint);
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

/// OHComposeNativePaint related methods
OHComposeNativePaint_Handle
androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_Paint(OHNativeCanvasProxy_Handle proxy) {
    auto canvasProxy = reinterpret_cast<androidx::compose::ui::arkui::utils::OHNativeCanvasProxy *>(proxy);
    return reinterpret_cast<OHComposeNativePaint_Handle>(canvasProxy->Paint());
}

void androidx_compose_ui_arkui_utils_OHComposeNativePaint_setAlpha(OHComposeNativePaint_Handle paint, float alpha) {
    auto nativePaint = reinterpret_cast<androidx::compose::ui::arkui::utils::OHComposeNativePaint *>(paint);
    nativePaint->alpha = alpha;
    LOGI("androidx_compose_ui_arkui_utils_OHComposeNativePaint_setAlpha: "
         "alpha=%{public}f",
         alpha);
}

void androidx_compose_ui_arkui_utils_OHComposeNativePaint_setIsAntiAlias(OHComposeNativePaint_Handle paint,
                                                                         bool isAntiAlias) {
    auto nativePaint = reinterpret_cast<androidx::compose::ui::arkui::utils::OHComposeNativePaint *>(paint);
    nativePaint->isAntiAlias = isAntiAlias;
    LOGI("androidx_compose_ui_arkui_utils_OHComposeNativePaint_setIsAntiAlias: "
         "isAntiAlias=%{public}d",
         isAntiAlias);
}

// 此处接受kotlin侧64为的colorValue，需要转成C的32位的color
void androidx_compose_ui_arkui_utils_OHComposeNativePaint_setColor(OHComposeNativePaint_Handle paint,
                                                                   uint64_t colorValue) {
    auto nativePaint = reinterpret_cast<androidx::compose::ui::arkui::utils::OHComposeNativePaint *>(paint);
    nativePaint->color = static_cast<uint32_t>(colorValue >> 32);
    LOGI("androidx_compose_ui_arkui_utils_OHComposeNativePaint_setColor: "
         "color=0x%{public}X",
         nativePaint->color);
}

void androidx_compose_ui_arkui_utils_OHComposeNativePaint_setStrokeWidth(OHComposeNativePaint_Handle paint,
                                                                         float strokeWidth) {
    auto nativePaint = reinterpret_cast<androidx::compose::ui::arkui::utils::OHComposeNativePaint *>(paint);
    nativePaint->strokeWidth = strokeWidth;
    LOGI("androidx_compose_ui_arkui_utils_OHComposeNativePaint_setStrokeWidth: "
         "strokeWidth=%{public}f",
         strokeWidth);
}

void androidx_compose_ui_arkui_utils_OHComposeNativePaint_setBlendMode(OHComposeNativePaint_Handle paint,
                                                                       uint32_t blendMode) {
    auto nativePaint = reinterpret_cast<androidx::compose::ui::arkui::utils::OHComposeNativePaint *>(paint);
    nativePaint->blendMode = static_cast<OH_Drawing_BlendMode>(blendMode);
    LOGI("addroidx_compose_ui_arkui_utils_OHComposeNativePaint_setBlendMode: "
         "blendMode=%{public}d",
         blendMode);
}

void androidx_compose_ui_arkui_utils_OHComposeNativePaint_setStyle(OHComposeNativePaint_Handle paint, uint32_t style) {
    auto nativePaint = reinterpret_cast<androidx::compose::ui::arkui::utils::OHComposeNativePaint *>(paint);
    nativePaint->style = static_cast<OH_Native_Draw_PaintingStyle>(style);
    LOGI("androidx_compose_ui_arkui_utils_OHComposeNativePaint_setStyle: "
         "style=%{public}d",
         style);
}

void androidx_compose_ui_arkui_utils_OHComposeNativePaint_setStrokeCap(OHComposeNativePaint_Handle paint,
                                                                       uint32_t strokeCap) {
    auto nativePaint = reinterpret_cast<androidx::compose::ui::arkui::utils::OHComposeNativePaint *>(paint);
    nativePaint->strokeCap = static_cast<OH_Native_Draw_StrokeCap>(strokeCap);
    LOGI("androidx_compose_ui_arkui_utils_OHComposeNativePaint_setStrokeCap: "
         "strokeCap=%{public}d",
         strokeCap);
}

void androidx_compose_ui_arkui_utils_OHComposeNativePaint_setStrokeJoin(OHComposeNativePaint_Handle paint,
                                                                        uint32_t strokeJoin) {
    auto nativePaint = reinterpret_cast<androidx::compose::ui::arkui::utils::OHComposeNativePaint *>(paint);
    nativePaint->strokeJoin = static_cast<OH_Native_Draw_StrokeJoin>(strokeJoin);
    LOGI("androidx_compose_ui_arkui_utils_OHComposeNativePaint_setStrokeJoin: "
         "strokeJoin=%{public}d",
         strokeJoin);
}

void androidx_compose_ui_arkui_utils_OHComposeNativePaint_setStrokeMiterLimit(OHComposeNativePaint_Handle paint,
                                                                              float miterLimit) {
    auto nativePaint = reinterpret_cast<androidx::compose::ui::arkui::utils::OHComposeNativePaint *>(paint);
    nativePaint->strokeMiterLimit = miterLimit;
    LOGI("androidx_compose_ui_arkui_utils_OHComposeNativePaint_"
         "setStrokeMiterLimit: miterLimit=%{public}f",
         miterLimit);
}

void androidx_compose_ui_arkui_utils_OHComposeNativePaint_setFilterQuality(OHComposeNativePaint_Handle paint,
                                                                           uint32_t quality) {
    auto nativePaint = reinterpret_cast<androidx::compose::ui::arkui::utils::OHComposeNativePaint *>(paint);
    nativePaint->filterQuality = static_cast<OH_Native_Draw_FilterQuality>(quality);
    LOGI("androidx_compose_ui_arkui_utils_OHComposeNativePaint_setFilterQuality: "
         "quality=%{public}d",
         quality);
}

void androidx_compose_ui_arkui_utils_OHComposeNativePaint_setShader(OHComposeNativePaint_Handle paint,
                                                                    NativeBasicShader_Handle shader) {
    auto nativePaint = reinterpret_cast<androidx::compose::ui::arkui::utils::OHComposeNativePaint *>(paint);
    auto nativeShader = reinterpret_cast<OH::NativeBasicShader *>(shader);
    nativePaint->shader = nativeShader;
    LOGI("androidx_compose_ui_arkui_utils_OHComposeNativePaint_setShader: "
         "shader=%{public}p",
         shader);
}

void androidx_compose_ui_arkui_utils_OHComposeNativePaint_setPathEffect(OHComposeNativePaint_Handle paint,
                                                                        OH_Drawing_PathEffect_Handle pathEffect) {
    auto nativePaint = reinterpret_cast<androidx::compose::ui::arkui::utils::OHComposeNativePaint *>(paint);
    auto nativePathEffect = reinterpret_cast<OH_Drawing_PathEffect *>(paint);
    nativePaint->pathEffect = nativePathEffect;
    LOGI("androidx_compose_ui_arkui_utils_OHComposeNativePaint_setPathEffect: "
         "pathEffect=%{public}p",
         pathEffect);
}

void androidx_compose_ui_arkui_utils_OHComposeNativePaint_setColorFilter(OHComposeNativePaint_Handle paint,
                                                                         OH_Drawing_ColorFilter_Handle colorFilter) {
    const auto nativePaint = reinterpret_cast<androidx::compose::ui::arkui::utils::OHComposeNativePaint *>(paint);
    const auto nativeColorFilter = reinterpret_cast<OH_Drawing_ColorFilter *>(colorFilter);
    nativePaint->colorFilter = nativeColorFilter;
    LOGI("androidx_compose_ui_arkui_utils_OHComposeNativePaint_setColorFilter: "
         "colorFilter=%{public}p",
         colorFilter);
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

NativeBasicShader_Handle androidx_compose_ui_arkui_utils_createNativeImageShader(OH_Drawing_Image *image,
                                                                                 uint32_t tileModeX,
                                                                                 uint32_t tileModeY) {
    const auto shader = new OH::NativeImageShader();
    shader->setTileMode(static_cast<OH_Drawing_TileMode>(tileModeX), static_cast<OH_Drawing_TileMode>(tileModeY));
    shader->image = image;
    LOGI("androidx_compose_ui_arkui_utils_createNativeImageShader: "
         "image=%{public}p, tileModeX=%{public}u, "
         "tileModeY=%{public}u",
         image, tileModeX, tileModeY);
    return reinterpret_cast<NativeBasicShader_Handle>(shader);
}

EXTERN_C_END