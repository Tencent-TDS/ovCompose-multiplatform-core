#ifndef OH_NATIVE_SWEEP_GRADIENT_SHADER_H
#define OH_NATIVE_SWEEP_GRADIENT_SHADER_H

#include <vector>
#include "oh_native_basic_shader.h"

namespace OH {
class NativeSweepGradientShader final : public NativeBasicShader {
public:
    NativeSweepGradientShader();
    ~NativeSweepGradientShader() override;

    uint64_t propertyHash() override;
    OH_Native_Shader_Type getType() override;
    NativeSweepGradientShader *setCenter(float x, float y);
    NativeSweepGradientShader *setColors(uint32_t *colors, float *colorPositions, uint32_t colorCount);

    float centerX;
    float centerY;
    std::vector<float> colorPositions;
    std::vector<uint32_t> colors;
};
} // namespace OH
#endif