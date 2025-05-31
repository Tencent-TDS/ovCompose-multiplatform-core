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

#ifndef ANDROIDX_COMPOSE_UI_ARKUI_UTILS_XCOMPONENT_LOG_H
#define ANDROIDX_COMPOSE_UI_ARKUI_UTILS_XCOMPONENT_LOG_H

#ifdef DEBUG

#include <hilog/log.h>

#define LOGD(fmt, ...) OH_LOG_Print(LOG_APP, LOG_DEBUG, 1000, "compose_arkui", fmt, ##__VA_ARGS__)
#define LOGI(fmt, ...) OH_LOG_Print(LOG_APP, LOG_INFO, 1000, "compose-arkui", fmt, ##__VA_ARGS__)
#define LOGE(fmt, ...) OH_LOG_Print(LOG_APP, LOG_ERROR, 1000, "compose-arkui", fmt, ##__VA_ARGS__)

#else

#define LOGD(fmt, ...)
#define LOGI(fmt, ...)
#define LOGE(fmt, ...)

#endif

#endif // ANDROIDX_COMPOSE_UI_ARKUI_UTILS_XCOMPONENT_LOG_H
