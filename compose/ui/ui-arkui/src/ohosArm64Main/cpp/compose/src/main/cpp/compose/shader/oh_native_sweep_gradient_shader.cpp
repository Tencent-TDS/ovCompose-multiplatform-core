#include <cstdint>
#include <cstddef>
#include <memory>
#include "oh_native_sweep_gradient_shader.h"
#include "../utils/oh_hash_funcs.h"

namespace OH {
NativeSweepGradientShader::NativeSweepGradientShader() {
    // ensure vectors are initialized to empty
    this->colors = std::vector<uint32_t>();
    this->colorPositions = std::vector<float>();
}
NativeSweepGradientShader::~NativeSweepGradientShader() {
}

uint64_t NativeSweepGradientShader::propertyHash() {
    float floats[5] = {centerX, centerY, static_cast<float>(FNVHashNumberArray<float>(colorPositions)),
                       static_cast<float>(FNVHashNumberArray<uint32_t>(colors))};
    return FNVHash(floats, sizeof(floats));
}
NativeSweepGradientShader *NativeSweepGradientShader::setCenter(float x, float y) {
    this->centerX = x;
    this->centerY = y;
    return this;
};
NativeSweepGradientShader *NativeSweepGradientShader::setColors(uint32_t *colors, float *colorPositions, uint32_t colorCount) {
    // If colors is nullptr or colorCount is 0, both colors and colorPositions are cleared,
    // since a valid gradient cannot be formed without color data. 
    if (colors == nullptr || colorCount == 0) {
        this->colors.clear();
        this->colorPositions.clear();
        return this;
    }

    this->colors = std::vector<uint32_t>(colors, colors + colorCount);

    if (colorPositions != nullptr) {
        this->colorPositions = std::vector<float>(colorPositions, colorPositions + colorCount);
    }
    return this;
};
} // namespace OH