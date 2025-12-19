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
        const auto imageSamplingOp  = OH_Drawing_SamplingOptionsCreate(FILTER_MODE_NEAREST, MIPMAP_MODE_NONE);
        // 创建图片着色器
        OH_Drawing_ShaderEffect *shaderEffect = OH_Drawing_ShaderEffectCreatePixelMapShader(
            imageShader->image, imageShader->tileModeX, imageShader->tileModeY, imageSamplingOp, nullptr);
        OH_Drawing_SamplingOptionsDestroy(imageSamplingOp);
        return shaderEffect;
    }
    case OH_Native_Shader_Type::NoneTypeShader:
        throw std::runtime_error("Unsupported shader type");
    default:
        throw std::runtime_error("Unsupported shader type");
    }
}

// 创建ShaderEffect，支持坐标缩放（用于相对坐标转换）
// drawWidth, drawHeight: 绘制区域的宽度和高度（相对坐标）
// 这个函数用于在ContentModifier的onDraw回调中，将shader的绝对坐标缩放为相对坐标
// 参考iOS实现：使用除法进行缩放，将绝对坐标归一化到绘制区域的相对坐标
OH_ALWAYS_INLINE OH_Drawing_ShaderEffect *CreateShaderEffectWithScaledSize(
    NativeBasicShader *shader, float drawWidth, float drawHeight) {
    switch (shader->getType()) {
    case OH_Native_Shader_Type::LinearGradientShader: {
        const auto linearShader = dynamic_cast<NativeLinearGradientShader *>(shader);
        // 计算shader的原始宽度和高度（绝对坐标）
        const float shaderWidth = linearShader->endX - linearShader->startX;
        const float shaderHeight = linearShader->endY - linearShader->startY;
        
        // 计算缩放比例（参考iOS归一化方案）
        float scaleX = 1.0f;
        float scaleY = 1.0f;
        if (shaderWidth > 0.0f) {
            scaleX = drawWidth / shaderWidth;
        }
        if (shaderHeight > 0.0f) {
            scaleY = drawHeight / shaderHeight;
        }
        
        // 缩放坐标：newCoord = oldCoord * scale
        const float newStartX = linearShader->startX * scaleX;
        const float newStartY = linearShader->startY * scaleY;
        const float newEndX = linearShader->endX * scaleX;
        const float newEndY = linearShader->endY * scaleY;
        
        // 创建线性渐变着色器（使用缩放后的相对坐标）
        OH_Drawing_Point *start = OH_Drawing_PointCreate(newStartX, newStartY);
        OH_Drawing_Point *end = OH_Drawing_PointCreate(newEndX, newEndY);
        const float *colorPos = linearShader->colorPositions.empty() ? nullptr : linearShader->colorPositions.data();
        const uint32_t *colors = linearShader->colors.data();
        const auto size = static_cast<uint32_t>(linearShader->colors.size());
        OH_Drawing_ShaderEffect *shaderEffect =
            OH_Drawing_ShaderEffectCreateLinearGradient(start, end, colors, colorPos, size, linearShader->tileMode);
        OH_Drawing_PointDestroy(start);
        OH_Drawing_PointDestroy(end);
        LOGI("CreateShaderEffectWithScaledSize: linear shaderEffect %{public}p, scale=(%{public}f, %{public}f), "
             "original=(%{public}f, %{public}f)->(%{public}f, %{public}f), scaled=(%{public}f, %{public}f)->(%{public}f, %{public}f)",
             shaderEffect, scaleX, scaleY,
             linearShader->startX, linearShader->startY, linearShader->endX, linearShader->endY,
             newStartX, newStartY, newEndX, newEndY);
        return shaderEffect;
    }
    case OH_Native_Shader_Type::RadialGradientShader: {
        const auto radialShader = dynamic_cast<NativeRadialGradientShader *>(shader);
        // 对于径向渐变，需要缩放中心点和半径
        // 使用绘制区域的一半作为参考范围（假设中心点在绘制区域中心）
        const float refWidth = drawWidth > 0.0f ? drawWidth : 1.0f;
        const float refHeight = drawHeight > 0.0f ? drawHeight : 1.0f;
        
        // 计算缩放比例（使用绘制区域尺寸作为参考）
        float scaleX = 1.0f;
        float scaleY = 1.0f;
        // 假设原始shader的坐标范围大约是Canvas尺寸，使用drawWidth/drawHeight作为缩放参考
        // 这里简化处理：直接使用drawWidth和drawHeight作为缩放后的坐标
        const float newCenterX = radialShader->centerX * (drawWidth / refWidth);
        const float newCenterY = radialShader->centerY * (drawHeight / refHeight);
        // 半径也需要缩放，使用较小的缩放比例
        const float radiusScale = (scaleX < scaleY) ? scaleX : scaleY;
        const float newRadius = radialShader->radius * radiusScale;
        
        OH_Drawing_Point *center = OH_Drawing_PointCreate(newCenterX, newCenterY);
        const float *colorPos = radialShader->colorPositions.empty() ? nullptr : radialShader->colorPositions.data();
        const uint32_t *colors = radialShader->colors.data();
        const auto size = static_cast<uint32_t>(radialShader->colors.size());
        OH_Drawing_ShaderEffect *shaderEffect = OH_Drawing_ShaderEffectCreateRadialGradient(
            center, newRadius, colors, colorPos, size, radialShader->tileMode);
        OH_Drawing_PointDestroy(center);
        LOGI("CreateShaderEffectWithScaledSize: radial shaderEffect %{public}p, scale=(%{public}f, %{public}f)",
             shaderEffect, scaleX, scaleY);
        return shaderEffect;
    }
    case OH_Native_Shader_Type::SweepGradientShader: {
        const auto sweepShader = static_cast<NativeSweepGradientShader *>(shader);
        // 对于扫描渐变，需要缩放中心点
        const float refWidth = drawWidth > 0.0f ? drawWidth : 1.0f;
        const float refHeight = drawHeight > 0.0f ? drawHeight : 1.0f;
        
        const float newCenterX = sweepShader->centerX * (drawWidth / refWidth);
        const float newCenterY = sweepShader->centerY * (drawHeight / refHeight);
        
        OH_Drawing_Point *center = OH_Drawing_PointCreate(newCenterX, newCenterY);
        const float *colorPos = sweepShader->colorPositions.empty() ? nullptr : sweepShader->colorPositions.data();
        const uint32_t *colors = sweepShader->colors.data();
        const auto size = static_cast<uint32_t>(sweepShader->colors.size());
        OH_Drawing_ShaderEffect *shaderEffect =
            OH_Drawing_ShaderEffectCreateSweepGradient(center, colors, colorPos, size, OH_Drawing_TileMode::CLAMP);
        OH_Drawing_PointDestroy(center);
        LOGI("CreateShaderEffectWithScaledSize: sweep shaderEffect %{public}p", shaderEffect);
        return shaderEffect;
    }
    case OH_Native_Shader_Type::ImageShader: {
        // ImageShader不需要坐标转换，直接使用原始方法
        return CreateShaderEffect(shader);
    }
    case OH_Native_Shader_Type::NoneTypeShader:
        throw std::runtime_error("Unsupported shader type");
    default:
        // 对于不支持缩放的shader类型，回退到原始方法
        return CreateShaderEffect(shader);
    }
}
} // namespace OH
#endif // OH_NATIVE_SHADER_UTILS_H