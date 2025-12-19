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

#include "oh_compose_native_color_filter.h"
#include <cstring>

namespace OH {

OHComposeNativeColorFilter::OHComposeNativeColorFilter() : colorFilter_(nullptr), type_(ColorFilterType::Tint), colorValue_(0), blendMode_(OH_Native_Draw_BlendMode::BlendModeSrcOver), multiplyColor_(0xFFFFFFFF), addColor_(0) {
    // 初始化单位矩阵
    std::memset(matrix_, 0, sizeof(matrix_));
    matrix_[0] = matrix_[6] = matrix_[12] = matrix_[18] = 1.0f;
}

OHComposeNativeColorFilter::~OHComposeNativeColorFilter() {
    if (colorFilter_ != nullptr) {
        OH_Drawing_ColorFilterDestroy(colorFilter_);
        colorFilter_ = nullptr;
    }
}

OHComposeNativeColorFilter *OHComposeNativeColorFilter::setTintFilter(
    uint64_t color,
    OH_Native_Draw_BlendMode blendMode) {
    // 销毁旧的filter
    if (colorFilter_ != nullptr) {
        OH_Drawing_ColorFilterDestroy(colorFilter_);
        colorFilter_ = nullptr;
    }

    type_ = ColorFilterType::Tint;
    colorValue_ = color;
    blendMode_ = blendMode;

    // 转换为OH_Drawing_BlendMode
    OH_Drawing_BlendMode drawingBlendMode = static_cast<OH_Drawing_BlendMode>(blendMode);

    // 创建blend模式的ColorFilter
    colorFilter_ = OH_Drawing_ColorFilterCreateBlendMode(
        static_cast<uint32_t>(color),
        drawingBlendMode);

    return this;
}

OHComposeNativeColorFilter *OHComposeNativeColorFilter::setMatrixFilter(
    const float *matrix,
    uint32_t matrixSize) {
    // 销毁旧的filter
    if (colorFilter_ != nullptr) {
        OH_Drawing_ColorFilterDestroy(colorFilter_);
        colorFilter_ = nullptr;
    }

    type_ = ColorFilterType::Matrix;

    // 保存matrix用于hash计算
    if (matrix != nullptr && matrixSize >= 20) {
        std::memcpy(matrix_, matrix, 20 * sizeof(float));
    }

    // 创建ColorMatrix filter
    // OH_Drawing_ColorFilterCreateMatrix接受float数组
    colorFilter_ = OH_Drawing_ColorFilterCreateMatrix(matrix_);

    return this;
}

OHComposeNativeColorFilter *OHComposeNativeColorFilter::setLightingFilter(
    uint64_t multiply,
    uint64_t add) {
    // 销毁旧的filter
    if (colorFilter_ != nullptr) {
        OH_Drawing_ColorFilterDestroy(colorFilter_);
        colorFilter_ = nullptr;
    }

    type_ = ColorFilterType::Lighting;
    multiplyColor_ = multiply;
    addColor_ = add;

    // 创建lighting filter
    // OH_Drawing API可能没有直接的lighting filter，需要通过ColorMatrix实现
    // Lighting filter: result = (src * multiply) + add
    // 转换为ColorMatrix:
    // R' = R * multiply.R + add.R
    // G' = G * multiply.G + add.G
    // B' = B * multiply.B + add.B
    // A' = A * multiply.A + add.A

    // 提取ARGB分量
    float multiplyA = ((multiply >> 24) & 0xFF) / 255.0f;
    float multiplyR = ((multiply >> 16) & 0xFF) / 255.0f;
    float multiplyG = ((multiply >> 8) & 0xFF) / 255.0f;
    float multiplyB = (multiply & 0xFF) / 255.0f;

    float addA = ((add >> 24) & 0xFF);
    float addR = ((add >> 16) & 0xFF);
    float addG = ((add >> 8) & 0xFF);
    float addB = (add & 0xFF);

    // 构建ColorMatrix (4x5矩阵，按行优先)
    float lightingMatrix[20] = {
        multiplyR, 0.0f, 0.0f, 0.0f, addR, // R
        0.0f, multiplyG, 0.0f, 0.0f, addG, // G
        0.0f, 0.0f, multiplyB, 0.0f, addB, // B
        0.0f, 0.0f, 0.0f, multiplyA, addA  // A
    };

    std::memcpy(matrix_, lightingMatrix, sizeof(lightingMatrix));
    colorFilter_ = OH_Drawing_ColorFilterCreateMatrix(matrix_);

    return this;
}

uint64_t OHComposeNativeColorFilter::propertyHash() const {
    uint64_t hash = static_cast<uint64_t>(type_);

    switch (type_) {
    case ColorFilterType::Tint:
        hash ^= colorValue_;
        hash ^= static_cast<uint64_t>(blendMode_) << 32;
        break;

    case ColorFilterType::Matrix:
        // 对matrix的前几个值进行hash
        for (int i = 0; i < 5; i++) {
            uint32_t intBits;
            std::memcpy(&intBits, &matrix_[i * 4], sizeof(uint32_t));
            hash ^= static_cast<uint64_t>(intBits) << (i * 8);
        }
        break;

    case ColorFilterType::Lighting:
        hash ^= multiplyColor_;
        hash ^= addColor_ << 32;
        break;
    }

    return hash;
}

} // namespace OH
