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
#include "../xcomponent_log.h"
#include "oh_native_canvas_export.h"
#include "oh_native_canvas_proxy.h"
#include "oh_native_canvas_proxy_factory.h"

EXTERN_C_START

OHNativeCanvasProxy_Handle androidx_compose_ui_arkui_utils_createOHNativeCanvasProxy(void *factory) {
    LOGI("androidx_compose_ui_arkui_utils_createOHNativeCanvasProxy: start");
    auto canvasFactory = reinterpret_cast<androidx::compose::ui::arkui::utils::OHNativeCanvasProxyFactory *>(factory);
    return reinterpret_cast<OHNativeCanvasProxy_Handle>(canvasFactory->CreateOHNativeCanvasProxy());
}

void androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_beginDraw(OHNativeCanvasProxy_Handle proxy) {
    LOGI("androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_beginDraw: start");
    auto canvasProxy = reinterpret_cast<androidx::compose::ui::arkui::utils::OHNativeCanvasProxy *>(proxy);
    canvasProxy->BeginDraw();
}

EXTERN_C_END