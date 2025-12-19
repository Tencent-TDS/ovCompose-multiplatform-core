#ifndef OH_NATIVE_LINEAR_GRADIENT_SHADER_H
#define OH_NATIVE_LINEAR_GRADIENT_SHADER_H

#include <vector>
#include "native_drawing/drawing_shader_effect.h"
#include "oh_native_basic_shader.h"

namespace OH {
class NativeLinearGradientShader final : public NativeBasicShader {
public:
    NativeLinearGradientShader();
    ~NativeLinearGradientShader() override;

    uint64_t propertyHash() override;
    OH_Native_Shader_Type getType() override;
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