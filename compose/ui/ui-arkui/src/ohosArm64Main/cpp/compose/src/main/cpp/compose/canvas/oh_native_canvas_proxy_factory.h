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

#ifndef ANDROIDX_COMPOSE_UI_ARKUI_UTILS_OHNATIVECANVASPROXYFACTORY_H
#define ANDROIDX_COMPOSE_UI_ARKUI_UTILS_OHNATIVECANVASPROXYFACTORY_H

#include "../render_node/oh_base_render_node.h"
#include "oh_native_canvas_proxy.h"

namespace androidx::compose::ui::arkui::utils {
typedef struct OHNativeCanvasProxy* OHNativeCanvasProxy_Handle;
class OHNativeCanvasProxyFactory {
public:
    explicit OHNativeCanvasProxyFactory(OH::BaseRenderNode* rootNode);
    ~OHNativeCanvasProxyFactory();
    OHNativeCanvasProxy_Handle CreateOHNativeCanvasProxy() const;
    OH::BaseRenderNode* getRootRenderNode();

   private:
    OH::BaseRenderNode* rootNode_;
};
}  // namespace androidx::compose::ui::arkui::utils

#endif
