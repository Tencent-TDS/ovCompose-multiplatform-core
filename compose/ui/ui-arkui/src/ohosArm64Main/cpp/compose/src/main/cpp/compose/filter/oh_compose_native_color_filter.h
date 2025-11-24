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

#ifndef OH_COMPOSE_NATIVE_COLOR_FILTER_H
#define OH_COMPOSE_NATIVE_COLOR_FILTER_H

#include <cstdint>
#include <native_drawing/drawing_color_filter.h>
#include "../constants/oh_native_enums.h"

namespace OH {

/**
 * @brief ColorFilter types supported by Compose
 */
enum class ColorFilterType {
    Tint,    // BlendMode color filter
    Matrix,  // ColorMatrix filter
    Lighting // Lighting filter
};

/**
 * @brief Native color filter wrapper for Compose
 *
 * This class wraps OH_Drawing_ColorFilter and provides Compose-specific
 * color filter functionality including tint, matrix, and lighting filters.
 */
class OHComposeNativeColorFilter {
public:
    OHComposeNativeColorFilter();
    ~OHComposeNativeColorFilter();

    /**
     * @brief Create a tint color filter with blend mode
     * @param color ARGB color value
     * @param blendMode Blend mode for compositing
     * @return this pointer for chaining
     */
    OHComposeNativeColorFilter *setTintFilter(uint64_t color, OH_Native_Draw_BlendMode blendMode);

    /**
     * @brief Create a color matrix filter
     * @param matrix Float array of 20 values (4x5 matrix)
     * @param matrixSize Size of the matrix array (should be 20)
     * @return this pointer for chaining
     */
    OHComposeNativeColorFilter *setMatrixFilter(const float *matrix, uint32_t matrixSize);

    /**
     * @brief Create a lighting color filter
     * @param multiply Color to multiply with source
     * @param add Color to add to source
     * @return this pointer for chaining
     */
    OHComposeNativeColorFilter *setLightingFilter(uint64_t multiply, uint64_t add);

    /**
     * @brief Get the underlying OH_Drawing_ColorFilter
     * @return Native color filter handle
     */
    OH_Drawing_ColorFilter *getNativeFilter() const {
        return colorFilter_;
    }

    /**
     * @brief Get the filter type
     */
    ColorFilterType getType() const {
        return type_;
    }

    /**
     * @brief Compute property hash for caching
     */
    uint64_t propertyHash() const;

private:
    OH_Drawing_ColorFilter *colorFilter_;
    ColorFilterType type_;

    // Filter parameters for hash computation
    uint64_t colorValue_;
    OH_Native_Draw_BlendMode blendMode_;
    uint64_t multiplyColor_;
    uint64_t addColor_;
    float matrix_[20];
};

} // namespace OH

#endif // OH_COMPOSE_NATIVE_COLOR_FILTER_H
