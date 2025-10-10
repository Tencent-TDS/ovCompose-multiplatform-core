/*
 * Tencent is pleased to support the open source community by making ovCompose available.
 * Copyright (C) 2025 Tencent. All rights reserved.
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

#include "napi/native_api.h"
#include "oh_native_enums.h"
#include "native_drawing/drawing_types.h"
#include <arkui/native_render.h>

#ifndef ANDROIDX_COMPOSE_UI_ARKUI_UTILS_OHNATIVECANVAS_EXPORT_H
#define ANDROIDX_COMPOSE_UI_ARKUI_UTILS_OHNATIVECANVAS_EXPORT_H


EXTERN_C_START
typedef struct OHNativeCanvasProxy* OHNativeCanvasProxy_Handle;
typedef struct OHComposeNativePaint* OHComposeNativePaint_Handle;
typedef struct OH_Drawing_ShaderEffect* OH_Drawing_ShaderEffect_Handle;
typedef struct OH_Drawing_ColorFilter* OH_Drawing_ColorFilter_Handle;
typedef struct OH_Drawing_PathEffect* OH_Drawing_PathEffect_Handle;
typedef struct BaseRenderNode* BaseRenderNode_Handle;
OHNativeCanvasProxy_Handle androidx_compose_ui_arkui_utils_createOHNativeCanvasProxy(void *factory);
void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_beginDraw(OHNativeCanvasProxy_Handle proxy);
OHComposeNativePaint_Handle androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_Paint(OHNativeCanvasProxy_Handle proxy);

// OHNativeCanvasProxy state operations
void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_save(OHNativeCanvasProxy_Handle proxy);
void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_restore(OHNativeCanvasProxy_Handle proxy);
void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_translate(OHNativeCanvasProxy_Handle proxy, float dx, float dy);
void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_setBounds(OHNativeCanvasProxy_Handle proxy, int32_t originX, int32_t originY, int32_t boundsWidth, int32_t boundsHeight);
void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_setPosition(OHNativeCanvasProxy_Handle proxy, int32_t x, int32_t y);
void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_setPivot(OHNativeCanvasProxy_Handle proxy, float px, float py);
void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_setOpacity(OHNativeCanvasProxy_Handle proxy, float alpha);

// OHNativeCanvasProxy drawCommand
void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawRect(OHNativeCanvasProxy_Handle proxy, float left, float top, float right, float bottom, OHComposeNativePaint_Handle paint);
void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_finishDraw(OHNativeCanvasProxy_Handle proxy);
void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawLayer(OHNativeCanvasProxy_Handle proxy, BaseRenderNode_Handle renderNodeHandle);
void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_attachToRootView(OHNativeCanvasProxy_Handle proxy);
void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_setParent(OHNativeCanvasProxy_Handle proxy, OHNativeCanvasProxy_Handle parentProxy);
void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawLine(OHNativeCanvasProxy_Handle proxy, float x1, float y1, float x2, float y2, OHComposeNativePaint_Handle paint);
void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawLayerWithSubproxy(OHNativeCanvasProxy_Handle proxy, OHNativeCanvasProxy_Handle subProxy);
void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_clipRect(OHNativeCanvasProxy_Handle proxy, float left, float top, float right, float bottom, uint32_t clipOp);

// OHComposeNativePaint set paint properties
void androidx_compose_ui_arkui_utils_OHComposeNativePaint_setAlpha(OHComposeNativePaint_Handle paint, float alpha);
void androidx_compose_ui_arkui_utils_OHComposeNativePaint_setIsAntiAlias(OHComposeNativePaint_Handle paint, bool isAntiAlias);
void androidx_compose_ui_arkui_utils_OHComposeNativePaint_setColor(OHComposeNativePaint_Handle paint, uint64_t color);
void androidx_compose_ui_arkui_utils_OHComposeNativePaint_setStrokeWidth(OHComposeNativePaint_Handle paint, float strokeWidth);
void androidx_compose_ui_arkui_utils_OHComposeNativePaint_setBlendMode(OHComposeNativePaint_Handle paint, uint32_t blendMode);
void androidx_compose_ui_arkui_utils_OHComposeNativePaint_setStyle(OHComposeNativePaint_Handle paint, uint32_t style);
void androidx_compose_ui_arkui_utils_OHComposeNativePaint_setStrokeCap(OHComposeNativePaint_Handle paint, uint32_t strokeCap);
void androidx_compose_ui_arkui_utils_OHComposeNativePaint_setStrokeJoin(OHComposeNativePaint_Handle paint, uint32_t strokeJoin);
void androidx_compose_ui_arkui_utils_OHComposeNativePaint_setStrokeMiterLimit(OHComposeNativePaint_Handle paint, float miterLimit);
void androidx_compose_ui_arkui_utils_OHComposeNativePaint_setFilterQuality(OHComposeNativePaint_Handle paint, uint32_t quality);
void androidx_compose_ui_arkui_utils_OHComposeNativePaint_setShader(OHComposeNativePaint_Handle paint, OH_Drawing_ShaderEffect_Handle shader);
void androidx_compose_ui_arkui_utils_OHComposeNativePaint_setPathEffect(OHComposeNativePaint_Handle paint, OH_Drawing_PathEffect_Handle pathEffect);
void androidx_compose_ui_arkui_utils_OHComposeNativePaint_setColorFilter(OHComposeNativePaint_Handle paint, OH_Drawing_ColorFilter_Handle colorFilter);
EXTERN_C_END

#endif