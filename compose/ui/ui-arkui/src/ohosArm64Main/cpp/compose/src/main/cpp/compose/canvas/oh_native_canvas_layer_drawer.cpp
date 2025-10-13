#include <native_drawing/drawing_brush.h>
#include <native_drawing/drawing_types.h>
#include <native_drawing/drawing_canvas.h>
#include <native_drawing/drawing_path.h>
#include <native_drawing/drawing_shader_effect.h>
#include <native_drawing/drawing_point.h>
#include <native_drawing/drawing_rect.h>
#include <arkui/native_render.h>
#include <arkui/native_type.h>
#include "oh_native_canvas_layer_drawer.h"
#include "../xcomponent_log.h"
#include "oh_native_enums.h"
#include "../constants/oh_native_constants.h"
#include "../shader/oh_native_linear_gradient_shader.h"
#include "../shader/oh_native_radial_gradient_shader.h"
#include "../shader/oh_native_sweep_gradient_shader.h"
#include "../shader/oh_native_image_shader.h"

namespace OH {
void OHRenderNodeDrawRect(float left, float top, float right, float bottom,
                          NativeBasicShader *shader,
                          const RenderNodeSaveState *saveState,
                          BaseRenderNode *renderNodeForDrawing,
                          androidx::compose::ui::arkui::utils::OHComposeNativePaint *paint) {
    const float strokeWidth = paint->strokeWidth;
    int32_t x = (left + strokeWidth);
    int32_t y = (top + strokeWidth);

    int32_t width = (right - left + strokeWidth);
    int32_t height = (bottom - top + strokeWidth);

    renderNodeForDrawing
        ->setTransform(const_cast<float *>(saveState->transform.data()))
        ->setTranslate(saveState->translateX, saveState->translateY)
        ->setPosition(x, y)
        ->setSize(width, height)
        ->setBorderCornerRadius(0);
    if (!shader) {
        // TODO：setMask(0)会导致不显示，需要了解具体怎么传值
        // renderNodeForDrawing->setMask(0);
        if (paint->style == OH_Native_Draw_PaintingStyle::OH_NATIVE_PAINTING_STYLE_STROKE) {
            renderNodeForDrawing
                ->setBorderWidth(strokeWidth)
                ->setBorderColor(paint->color)
                ->setBackgroundColor(CLEAR_COLOR);
        } else {
            renderNodeForDrawing->setBorderWidth(0)
                ->setBackgroundColor(paint->color);
        }
    } else {
        // apply shader
        LOGI("OHRenderNodeDrawRect: apply shader start: %{public}p", shader);

        struct AnimatableUserData {
            ArkUI_FloatAnimatablePropertyHandle width;
            ArkUI_FloatAnimatablePropertyHandle height;
            ArkUI_Vector2AnimatablePropertyHandle v2;
            NativeBasicShader *shader;
        };

        AnimatableUserData *userData1 = new AnimatableUserData;
        userData1->width = OH_ArkUI_RenderNodeUtils_CreateFloatAnimatableProperty(width);
        userData1->height = OH_ArkUI_RenderNodeUtils_CreateFloatAnimatableProperty(height);
        userData1->v2 = OH_ArkUI_RenderNodeUtils_CreateVector2AnimatableProperty(x, y);
        userData1->shader = shader;

        auto modifier = OH_ArkUI_RenderNodeUtils_CreateContentModifier();
        OHRenderNodeDrawThrow(OH_ArkUI_RenderNodeUtils_AttachContentModifier(renderNodeForDrawing->getHandle(), modifier));

        // 关联 modifier 和 property
        OH_ArkUI_RenderNodeUtils_AttachFloatAnimatableProperty(modifier, userData1->width);
        OH_ArkUI_RenderNodeUtils_AttachFloatAnimatableProperty(modifier, userData1->height);
        OH_ArkUI_RenderNodeUtils_AttachVector2AnimatableProperty(modifier, userData1->v2);

        OHRenderNodeDrawThrow(
            OH_ArkUI_RenderNodeUtils_SetContentModifierOnDraw(
                modifier, userData1,
                [](ArkUI_DrawContext *context, void *userData) {
                    AnimatableUserData *data = static_cast<AnimatableUserData *>(userData);
                    auto *shaderEffect = CreateShaderEffect(data->shader);
                    float width = 0.0f;
                    float height = 0.0f;
                    float x = 0.0f;
                    float y = 0.0f;

                    LOGI("OHRenderNodeDrawRect: draw with shader start: %{public}p", shaderEffect);
                    OH_ArkUI_RenderNodeUtils_GetFloatAnimatablePropertyValue(data->width, &width);
                    OH_ArkUI_RenderNodeUtils_GetFloatAnimatablePropertyValue(data->height, &height);
                    OH_ArkUI_RenderNodeUtils_GetVector2AnimatablePropertyValue(data->v2, &x, &y);

                    // 创建画笔刷并绑定渐变
                    OH_Drawing_Brush *brush = OH_Drawing_BrushCreate();
                    OH_Drawing_BrushSetShaderEffect(brush, shaderEffect);

                    // 获取Canvas对象
                    auto *canvas_ptr = OH_ArkUI_DrawContext_GetCanvas(context);
                    OH_Drawing_Canvas *canvas = static_cast<OH_Drawing_Canvas *>(canvas_ptr);
                    OH_Drawing_CanvasAttachBrush(canvas, brush);

                    // 绘制矩形
                    OH_Drawing_Rect *rect = OH_Drawing_RectCreate(x, y, x + width, y + height);
                    OH_Drawing_CanvasDrawRect(canvas, rect);

                    // 释放绘制资源
                    OH_Drawing_CanvasDetachBrush(canvas);
                    OH_Drawing_RectDestroy(rect);
                    OH_Drawing_BrushDestroy(brush);
                    OH_Drawing_ShaderEffectDestroy(shaderEffect);
                    LOGI("OHRenderNodeDrawRect: draw with shader finish: %{public}p", shaderEffect);
                }));
    }
}

void OHRenderNodeDrawClipRect(float left, float top, float right, float bottom,
                              const RenderNodeSaveState *saveState,
                              BaseRenderNode *renderNodeForDrawing) {
    ArkUI_RectShapeOption *shape = OH_ArkUI_RenderNodeUtils_CreateRectShapeOption();
    if (shape) {
        OH_ArkUI_RenderNodeUtils_SetRectShapeOptionEdgeValue(shape, left, ARKUI_EDGE_DIRECTION_LEFT);
        OH_ArkUI_RenderNodeUtils_SetRectShapeOptionEdgeValue(shape, top, ARKUI_EDGE_DIRECTION_TOP);
        OH_ArkUI_RenderNodeUtils_SetRectShapeOptionEdgeValue(shape, right, ARKUI_EDGE_DIRECTION_RIGHT);
        OH_ArkUI_RenderNodeUtils_SetRectShapeOptionEdgeValue(shape, bottom, ARKUI_EDGE_DIRECTION_BOTTOM);
    }
    ArkUI_RenderNodeClipOption *clipOption = OH_ArkUI_RenderNodeUtils_CreateRenderNodeClipOptionFromRectShape(shape);
    OH_ArkUI_RenderNodeUtils_DisposeRectShapeOption(shape);
    if (clipOption) {
        renderNodeForDrawing->setTransform(const_cast<float *>(saveState->transform.data()))
            ->setTranslate(saveState->translateX, saveState->translateY)
            ->setClip(clipOption);
    }
}

void OHRenderNodeDrawLine(float x1, float y1, float x2, float y2,
                          OH_Drawing_ShaderEffect *shader,
                          const RenderNodeSaveState *saveState,
                          BaseRenderNode *renderNodeForDrawing) {
    int32_t x = std::min(x1, x2);
    int32_t y = std::min(y1, y2);
    int32_t width = abs(x2 - x1);
    int32_t height = abs(y2 - y1);

    renderNodeForDrawing
        ->setTransform(const_cast<float *>(saveState->transform.data()))
        ->setTranslate(saveState->translateX, saveState->translateY)
        ->setPosition(x, y)
        ->setSize(width, height);

    if (!shader) {
        renderNodeForDrawing->drawLine(x1, y1, x2, y2);
        //            [(TMMNativeLineLayer *)layerForDrawing drawWithPointX1:pointX1
        //            pointY1:pointY1
        //            pointX2:pointX2
        //            pointY2:pointY2
        //            lineWidth:[paint strokeWidth]
        //            lineColor:[paint colorFromColorValue]
        //            strokeCap:[paint strokeCap]
        //            density:density];
    } else {
        //            [(TMMNativeLineGradientLayer *)layerForDrawing drawWithPointX1:pointX1
        //            pointY1:pointY1
        //            pointX2:pointX2
        //            pointY2:pointY2
        //            lineWidth:[paint strokeWidth]
        //            shader:(TMMNativeLinearGradientShader *)shader
        //            strokeCap:[paint strokeCap]];
    }
}

OH_Drawing_ShaderEffect *CreateShaderEffect(NativeBasicShader *shader) {
    if (typeid(*shader) == typeid(NativeLinearGradientShader)) {
        auto linearShader = static_cast<NativeLinearGradientShader *>(shader);
        // 创建线性渐变着色器
        OH_Drawing_Point *start = OH_Drawing_PointCreate(linearShader->startX, linearShader->startY);
        OH_Drawing_Point *end = OH_Drawing_PointCreate(linearShader->endX, linearShader->endY);
        // If colorPositions is empty, pass nullptr to OH native api to generate default positions
        float *colorPos = linearShader->colorPositions.empty() ? nullptr : linearShader->colorPositions.data();
        uint32_t *colors = linearShader->colors.data();
        uint32_t size = static_cast<uint32_t>(linearShader->colors.size());
        OH_Drawing_ShaderEffect *shaderEffect = OH_Drawing_ShaderEffectCreateLinearGradient(
            start, end, colors, colorPos, size, linearShader->tileMode);
        OH_Drawing_PointDestroy(start);
        OH_Drawing_PointDestroy(end);
        LOGI("CreateShaderEffect: linear shaderEffect %{public}p", shaderEffect);
        return shaderEffect;
    } else if (typeid(*shader) == typeid(NativeRadialGradientShader)) {
        auto radialShader = static_cast<NativeRadialGradientShader *>(shader);
        // 创建径向渐变着色器
        OH_Drawing_Point *center = OH_Drawing_PointCreate(radialShader->centerX, radialShader->centerY);
        // If colorPositions is empty, pass nullptr to OH native api to generate default positions
        float *colorPos = radialShader->colorPositions.empty() ? nullptr : radialShader->colorPositions.data();
        uint32_t *colors = radialShader->colors.data();
        uint32_t size = static_cast<uint32_t>(radialShader->colors.size());
        OH_Drawing_ShaderEffect *shaderEffect = OH_Drawing_ShaderEffectCreateRadialGradient(
            center, radialShader->radius, colors, colorPos, size, radialShader->tileMode);
        OH_Drawing_PointDestroy(center);
        LOGI("CreateShaderEffect: radial shaderEffect %{public}p", shaderEffect);
        return shaderEffect;
    } else if (typeid(*shader) == typeid(NativeSweepGradientShader)) {
        auto sweepShader = static_cast<NativeSweepGradientShader *>(shader);
        // 创建扫描渐变着色器
        OH_Drawing_Point *center = OH_Drawing_PointCreate(sweepShader->centerX, sweepShader->centerY);
        float *colorPos = sweepShader->colorPositions.empty() ? nullptr : sweepShader->colorPositions.data();
        uint32_t *colors = sweepShader->colors.data();
        uint32_t size = static_cast<uint32_t>(sweepShader->colors.size());
        // Using CLAMP as the default tileMode since Compose's API doesn't require this parameter
        // but the OH native API does require
        OH_Drawing_ShaderEffect *shaderEffect = OH_Drawing_ShaderEffectCreateSweepGradient(
            center, colors, colorPos, size, OH_Drawing_TileMode::CLAMP);
        OH_Drawing_PointDestroy(center);
        LOGI("CreateShaderEffect: sweep shaderEffect %{public}p", shaderEffect);
        return shaderEffect;
    } else if (typeid(*shader) == typeid(NativeImageShader)) {
        auto imageShader = static_cast<NativeImageShader *>(shader);
        // 创建图片着色器
        OH_Drawing_ShaderEffect *shaderEffect = OH_Drawing_ShaderEffectCreateImageShader(
            imageShader->image, imageShader->tileModeX, imageShader->tileModeY, nullptr, nullptr);
        return shaderEffect;
    } else {
        throw std::runtime_error("Unsupported shader type");
    }
}

void OHRenderNodeDrawThrow(int32_t status) {
    if (status != ARKUI_ERROR_CODE_NO_ERROR) {
        LOGE("OHRenderNodeDraw operation failed with status: %{public}d", status);
        throw std::runtime_error("OHRenderNode operation failed");
    }
}
} // namespace OH