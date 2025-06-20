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

#ifndef ANDROIDX_COMPOSE_UI_ARKUI_UTILS_EXPORT_H
#define ANDROIDX_COMPOSE_UI_ARKUI_UTILS_EXPORT_H

#include "napi/native_api.h"
#include "xcomponent_common.h"

EXTERN_C_START
void androidx_compose_ui_arkui_utils_init(napi_env env, napi_value exports);
napi_value androidx_compose_ui_arkui_utils_wrapped(napi_env env, void *nativeController);

Boolean androidx_compose_ui_arkui_utils_xcomponent_prepareDraw(void *render);
Boolean androidx_compose_ui_arkui_utils_xcomponent_finishDraw(void *render);
void androidx_compose_ui_arkui_utils_xcomponent_registerFrameCallback(void *render);
void androidx_compose_ui_arkui_utils_xcomponent_unregisterFrameCallback(void *render);
EXTERN_C_END

#endif