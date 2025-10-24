
#ifndef OH_NATIVE_RADIAL_GRADIENT_SHADER_H
#define OH_NATIVE_RADIAL_GRADIENT_SHADER_H

#include <cstdint>
#include <vector>
#include "native_drawing/drawing_shader_effect.h"
#include "oh_native_basic_shader.h"

namespace OH {
class NativeRadialGradientShader : public NativeBasicShader {
public:
    NativeRadialGradientShader();
    ~NativeRadialGradientShader();

    uint64_t propertyHash() override;
    OH_Native_Shader_Type getType() override ;
    NativeRadialGradientShader *setTileMode(OH_Drawing_TileMode mode);
    NativeRadialGradientShader *setCenter(float x, float y);
    NativeRadialGradientShader *setRadius(float radius);
    NativeRadialGradientShader *setColors(uint32_t *colors, float *colorPositions, uint32_t colorCount);

    OH_Drawing_TileMode tileMode;
    float centerX;
    float centerY;
    float radius;
    std::vector<float> colorPositions;
    std::vector<uint32_t> colors;
};
} // namespace OH
#endif