#ifndef OH_NATIVE_SHADER_UTILS_H
#define OH_NATIVE_SHADER_UTILS_H

#include <native_drawing/drawing_point.h>
#include <native_drawing/drawing_shader_effect.h>
#include "../constants/oh_native_constants.h"
#include "../constants/oh_native_enums.h"
#include "../xcomponent_log.h"
#include "oh_native_basic_shader.h"
#include "oh_native_image_shader.h"
#include "oh_native_linear_gradient_shader.h"
#include "oh_native_radial_gradient_shader.h"
#include "oh_native_sweep_gradient_shader.h"

namespace OH {

OH_ALWAYS_INLINE OH_Drawing_ShaderEffect *CreateShaderEffect(NativeBasicShader *shader) {
    switch (shader->getType()) {
    case OH_Native_Shader_Type::LinearGradientShader: {
        const auto linearShader = dynamic_cast<NativeLinearGradientShader *>(shader);
        // 创建线性渐变着色器
        OH_Drawing_Point *start = OH_Drawing_PointCreate(linearShader->startX, linearShader->startY);
        OH_Drawing_Point *end = OH_Drawing_PointCreate(linearShader->endX, linearShader->endY);
        // If colorPositions is empty, pass nullptr to OH native api to generate
        // default positions
        const float *colorPos = linearShader->colorPositions.empty() ? nullptr : linearShader->colorPositions.data();
        const uint32_t *colors = linearShader->colors.data();
        const auto size = static_cast<uint32_t>(linearShader->colors.size());
        OH_Drawing_ShaderEffect *shaderEffect =
            OH_Drawing_ShaderEffectCreateLinearGradient(start, end, colors, colorPos, size, linearShader->tileMode);
        OH_Drawing_PointDestroy(start);
        OH_Drawing_PointDestroy(end);
        LOGI("CreateShaderEffect: linear shaderEffect %{public}p", shaderEffect);
        return shaderEffect;
    }
    case OH_Native_Shader_Type::RadialGradientShader: {
        const auto radialShader = dynamic_cast<NativeRadialGradientShader *>(shader);
        // 创建径向渐变着色器
        OH_Drawing_Point *center = OH_Drawing_PointCreate(radialShader->centerX, radialShader->centerY);
        // If colorPositions is empty, pass nullptr to OH native api to generate
        // default positions
        const float *colorPos = radialShader->colorPositions.empty() ? nullptr : radialShader->colorPositions.data();
        const uint32_t *colors = radialShader->colors.data();
        const auto size = static_cast<uint32_t>(radialShader->colors.size());
        OH_Drawing_ShaderEffect *shaderEffect = OH_Drawing_ShaderEffectCreateRadialGradient(
            center, radialShader->radius, colors, colorPos, size, radialShader->tileMode);
        OH_Drawing_PointDestroy(center);
        LOGI("CreateShaderEffect: radial shaderEffect %{public}p", shaderEffect);
        return shaderEffect;
    }
    case OH_Native_Shader_Type::SweepGradientShader: {
        const auto sweepShader = static_cast<NativeSweepGradientShader *>(shader);
        // 创建扫描渐变着色器
        OH_Drawing_Point *center = OH_Drawing_PointCreate(sweepShader->centerX, sweepShader->centerY);
        const float *colorPos = sweepShader->colorPositions.empty() ? nullptr : sweepShader->colorPositions.data();
        const uint32_t *colors = sweepShader->colors.data();
        const auto size = static_cast<uint32_t>(sweepShader->colors.size());
        // Using CLAMP as the default tileMode since Compose's API doesn't require
        // this parameter but the OH native API does require
        OH_Drawing_ShaderEffect *shaderEffect =
            OH_Drawing_ShaderEffectCreateSweepGradient(center, colors, colorPos, size, OH_Drawing_TileMode::CLAMP);
        OH_Drawing_PointDestroy(center);
        LOGI("CreateShaderEffect: sweep shaderEffect %{public}p", shaderEffect);
        return shaderEffect;
    }
    case OH_Native_Shader_Type::ImageShader: {
        const auto imageShader = dynamic_cast<NativeImageShader *>(shader);
        // 创建图片着色器
        OH_Drawing_ShaderEffect *shaderEffect = OH_Drawing_ShaderEffectCreateImageShader(
            imageShader->image, imageShader->tileModeX, imageShader->tileModeY, nullptr, nullptr);
        return shaderEffect;
    }
    case OH_Native_Shader_Type::NoneTypeShader:
        throw std::runtime_error("Unsupported shader type");
    default:
        throw std::runtime_error("Unsupported shader type");
    }
}
} // namespace OH
#endif // OH_NATIVE_SHADER_UTILS_H