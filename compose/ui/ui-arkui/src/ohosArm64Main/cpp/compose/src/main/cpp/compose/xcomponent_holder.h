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

#ifndef ANDROIDX_COMPOSE_UI_ARKUI_UTILS_XCOMPONENTHOLDER_H
#define ANDROIDX_COMPOSE_UI_ARKUI_UTILS_XCOMPONENTHOLDER_H

#include "napi/native_api.h"
#include <ace/xcomponent/native_interface_xcomponent.h>
#include <string>
#include <unordered_map>

namespace androidx::compose::ui::arkui::utils {
typedef struct ArkUIViewController ArkUIViewController;
class XComponentRender;
class XComponentHolder {
public:
    ~XComponentHolder();

    static XComponentHolder *GetInstance();

    void InitXComponent(napi_env env, napi_value exports);
    void InitArkViewController(napi_env env, const std::string &id, ArkUIViewController *controller);

    void RemoveXComponentRender(const std::string &id);
    void RemoveArkUIViewController(const std::string &id);

    XComponentRender *GetXComponentRender(const std::string &id) const;
    XComponentRender *GetXComponentRender(OH_NativeXComponent *component) const;
    ArkUIViewController *GetArkUIViewController(const std::string &id) const;
    ArkUIViewController *GetArkUIViewController(OH_NativeXComponent *component) const;

private:
    static XComponentHolder instance;

    std::unordered_map<std::string, XComponentRender *> renderMap;
    std::unordered_map<std::string, ArkUIViewController *> controllerMap;
};
} // namespace androidx::compose::ui::arkui::utils

#endif