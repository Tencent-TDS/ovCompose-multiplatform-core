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

#ifndef OH_NATIVE_COLOR_FILTER_EXPORT_H
#define OH_NATIVE_COLOR_FILTER_EXPORT_H

#include "../constants/oh_native_constants.h"

#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
 * @brief Create a tint color filter
 * @param colorValue ARGB color value
 * @param blendMode Blend mode enum value
 * @return Handle to OHComposeNativeColorFilter
 */
OHComposeNativeColorFilter_Handle androidx_compose_ui_arkui_utils_createNativeTintColorFilter(
    uint64_t colorValue,
    int32_t blendMode
);

/**
 * @brief Create a color matrix filter
 * @param matrix Pointer to float array of 20 values
 * @param matrixSize Size of the matrix array
 * @return Handle to OHComposeNativeColorFilter
 */
OHComposeNativeColorFilter_Handle androidx_compose_ui_arkui_utils_createNativeColorMatrixColorFilter(
    const float* matrix,
    uint32_t matrixSize
);

/**
 * @brief Create a lighting color filter
 * @param multiplyColor ARGB multiply color
 * @param addColor ARGB add color
 * @return Handle to OHComposeNativeColorFilter
 */
OHComposeNativeColorFilter_Handle androidx_compose_ui_arkui_utils_createNativeLightingColorFilter(
    uint64_t multiplyColor,
    uint64_t addColor
);

/**
 * @brief Dispose a native color filter
 * @param filterHandle Handle to OHComposeNativeColorFilter
 */
void androidx_compose_ui_arkui_utils_disposeNativeColorFilter(void* filterHandle);
#ifdef __cplusplus
}
#endif

#endif // OH_NATIVE_COLOR_FILTER_EXPORT_H

