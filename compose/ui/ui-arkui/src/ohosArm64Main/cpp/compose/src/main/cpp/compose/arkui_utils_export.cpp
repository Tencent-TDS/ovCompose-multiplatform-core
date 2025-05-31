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

#include "arkui_utils_export.h"
#include "arkui_view_controller_wrapper.h"
#include "napi/native_api.h"
#include "xcomponent_common.h"
#include "xcomponent_holder.h"
#include "xcomponent_log.h"
#include "xcomponent_render.h"

EXTERN_C_START
void androidx_compose_ui_arkui_utils_init(napi_env env, napi_value exports) {
    LOGI("androidx_compose_ui_arkui_utils_init: start");
    auto holder = androidx::compose::ui::arkui::utils::XComponentHolder::GetInstance();
    holder->InitXComponent(env, exports);
}
napi_value androidx_compose_ui_arkui_utils_wrapped(napi_env env, void *nativeController) {
    LOGI("androidx_compose_ui_arkui_utils_wrapped: start");
    return androidx::compose::ui::arkui::utils::ArkUIViewControllerWrapper::Wrapped(reinterpret_cast<napi_env>(env),
                                                                                    nativeController);
}

Boolean androidx_compose_ui_arkui_utils_xcomponent_prepareDraw(void *render) {
    LOGI("androidx_compose_ui_arkui_xcomponent_c_prepareDraw render(%{public}p)", render);
    auto xComponentRender = reinterpret_cast<androidx::compose::ui::arkui::utils::XComponentRender *>(render);
    return xComponentRender->EglPrepareDraw();
}

Boolean androidx_compose_ui_arkui_utils_xcomponent_finishDraw(void *render) {
    LOGI("androidx_compose_ui_arkui_xcomponent_c_finishDraw render(%{public}p)", render);
    auto xComponentRender = reinterpret_cast<androidx::compose::ui::arkui::utils::XComponentRender *>(render);
    return xComponentRender->EglFinishDraw();
}

void androidx_compose_ui_arkui_utils_xcomponent_registerFrameCallback(void *render) {
    LOGI("androidx_compose_ui_arkui_utils_xcomponent_registerFrameCallback render(%{public}p)", render);
    auto xComponentRender = reinterpret_cast<androidx::compose::ui::arkui::utils::XComponentRender *>(render);
    xComponentRender->RegisterFrameCallback();
}
void androidx_compose_ui_arkui_utils_xcomponent_unregisterFrameCallback(void *render) {
    LOGI("androidx_compose_ui_arkui_xcomponent_c_unregisterFrameCallback render(%{public}p)", render);
    auto xComponentRender = reinterpret_cast<androidx::compose::ui::arkui::utils::XComponentRender *>(render);
    xComponentRender->UnregisterFrameCallback();
}
EXTERN_C_END