#include "oh_native_linear_gradient_shader.h"
#include "../utils/oh_hash_funcs.h"

namespace OH {
NativeLinearGradientShader::NativeLinearGradientShader() {
    // ensure vectors are initialized to empty
    this->colorPositions = std::vector<float>();
    this->colors = std::vector<uint32_t>();
}

OH_Native_Shader_Type NativeLinearGradientShader::getType() { return OH_Native_Shader_Type::LinearGradientShader; };
NativeLinearGradientShader::~NativeLinearGradientShader() = default;

uint64_t NativeLinearGradientShader::propertyHash() {
    const float floats[7] = {static_cast<float>(tileMode),
                             startX,
                             startY,
                             endX,
                             endY,
                             static_cast<float>(FNVHashNumberArray<float>(colorPositions)),
                             static_cast<float>(FNVHashNumberArray<uint32_t>(colors))};
    return FNVHash(floats, sizeof(floats));
}

NativeLinearGradientShader *NativeLinearGradientShader::setTileMode(const OH_Drawing_TileMode mode) {
    tileMode = mode;
    return this;
}

NativeLinearGradientShader *NativeLinearGradientShader::setStart(const float x, const float y) {
    startX = x;
    startY = y;
    return this;
}

NativeLinearGradientShader *NativeLinearGradientShader::setEnd(const float x, const float y) {
    endX = x;
    endY = y;
    return this;
}

NativeLinearGradientShader *NativeLinearGradientShader::setColors(uint32_t *colors, float *colorPositions,
                                                                  const uint32_t colorCount) {
    // If colors is nullptr or colorCount is 0, both colors and colorPositions are
    // cleared, since a valid gradient cannot be formed without color data. If
    // only colorPositions is nullptr, we generate default positions below.
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
}
} // namespace OH