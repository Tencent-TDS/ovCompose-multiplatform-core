#ifndef OH_NATIVE_LINEAR_GRADIENT_SHADER_H
#define OH_NATIVE_LINEAR_GRADIENT_SHADER_H

#include <cstdint>
#include <vector>
#include "native_drawing/drawing_shader_effect.h"
#include "oh_native_basic_shader.h"

namespace OH {
class NativeLinearGradientShader : public NativeBasicShader {
public:
    NativeLinearGradientShader();
    ~NativeLinearGradientShader();

    uint64_t propertyHash() override;
    NativeLinearGradientShader *setTileMode(OH_Drawing_TileMode mode);
    NativeLinearGradientShader *setStart(float x, float y);
    NativeLinearGradientShader *setEnd(float x, float y);
    NativeLinearGradientShader *setColors(uint32_t *colors, float *colorPositions, uint32_t colorCount);

    OH_Drawing_TileMode tileMode;
    float startX;
    float startY;
    float endX;
    float endY;
    std::vector<float> colorPositions;
    std::vector<uint32_t> colors;
};
} // namespace OH
#endif