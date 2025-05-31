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

#include "xcomponent_utils.h"
#include "xcomponent_log.h"

namespace androidx::compose::ui::arkui::utils::XComponentUtils {
std::string GetXComponentId(OH_NativeXComponent *component) {
    if (component == nullptr) {
        LOGE("XComponentUtils: GetXComponentId: component is null");
        return "";
    }

    char id[OH_XCOMPONENT_ID_BUFFER_LEN_MAX] = {0};
    uint64_t idSize = OH_XCOMPONENT_ID_LEN_MAX + 1;

    auto status = OH_NativeXComponent_GetXComponentId(component, id, &idSize);
    if (status != OH_NATIVEXCOMPONENT_RESULT_SUCCESS) {
        LOGE("XComponentUtils: GetXComponentId: OH_NativeXComponent_GetXComponentId failed(%{public}d)", status);
        return "";
    }
    return std::string(id);
}
} // namespace androidx::compose::ui::arkui::utils::XComponentUtils