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

#include "oh_native_color_filter_export.h"
#include "oh_compose_native_color_filter.h"
#include "../paint/oh_compose_native_paint.h"
#include "../xcomponent_log.h"
#include "compose/constants/oh_native_constants.h"

#include <multimedia/image_framework/image/pixelmap_native.h>

extern "C" {

OHComposeNativeColorFilter_Handle androidx_compose_ui_arkui_utils_createNativeTintColorFilter(
    uint64_t colorValue,
    int32_t blendMode) {
    
    OH::OHComposeNativeColorFilter* filter = new OH::OHComposeNativeColorFilter();
    filter->setTintFilter(
        colorValue, 
        static_cast<OH_Native_Draw_BlendMode>(blendMode)
    );
    
    return reinterpret_cast<OHComposeNativeColorFilter_Handle>(filter);
}

OHComposeNativeColorFilter_Handle androidx_compose_ui_arkui_utils_createNativeColorMatrixColorFilter(
    const float* matrix,
    uint32_t matrixSize) {
    
    if (matrix == nullptr || matrixSize < 20) {
        LOGE("createNativeColorMatrixColorFilter: invalid matrix");
        return nullptr;
    }
    
    OH::OHComposeNativeColorFilter* filter = new OH::OHComposeNativeColorFilter();
    filter->setMatrixFilter(matrix, matrixSize);
    
    return reinterpret_cast<OHComposeNativeColorFilter_Handle>(filter);
}

OHComposeNativeColorFilter_Handle androidx_compose_ui_arkui_utils_createNativeLightingColorFilter(
    uint64_t multiplyColor,
    uint64_t addColor) {
    
    OH::OHComposeNativeColorFilter* filter = new OH::OHComposeNativeColorFilter();
    filter->setLightingFilter(multiplyColor, addColor);
    
    return reinterpret_cast<OHComposeNativeColorFilter_Handle>(filter);
}

void androidx_compose_ui_arkui_utils_disposeNativeColorFilter(void* filterHandle) {
    if (filterHandle != nullptr) {
        OH::OHComposeNativeColorFilter* filter = 
            static_cast<OH::OHComposeNativeColorFilter*>(filterHandle);
        delete filter;
    }
}
} // extern "C"

