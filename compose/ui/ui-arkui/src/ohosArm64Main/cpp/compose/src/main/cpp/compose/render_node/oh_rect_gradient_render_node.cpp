#include <native_drawing/drawing_brush.h>
#include <native_drawing/drawing_rect.h>
#include "oh_rect_gradient_render_node.h"
#include "../shader/oh_native_shader_utils.h"
#include "../xcomponent_log.h"

namespace OH {
RectGradientRenderNode::~RectGradientRenderNode() {
    // dispose properties and modifier
    if (leftTopPosProperty_) {
        OH_ArkUI_RenderNodeUtils_DisposeVector2Property(leftTopPosProperty_);
    }
    if (rightBottomProperty_) {
        OH_ArkUI_RenderNodeUtils_DisposeVector2Property(rightBottomProperty_);
    }
    if (modifier_) {
        OH_ArkUI_RenderNodeUtils_DisposeContentModifier(modifier_);
    }
}

RectGradientRenderNode::RectGradientRenderNode() : BaseRenderNode() {
    this->initModifier();
}

OH_DrawingNode_Type RectGradientRenderNode::getType() { return OH_DrawingNode_Type::RectGradientNode; };

/**
 * @brief Draws a rectangle with the specified coordinates, stroke width, shader, and painting style.
 *
 * This method sets the painting style and shader for the rectangle, and updates the rectangle's
 * position and stroke width properties. It does not perform the actual drawing, but prepares
 * the necessary properties for rendering.
 *
 * @param left The x-coordinate of the left edge of the rectangle.
 * @param top The y-coordinate of the top edge of the rectangle.
 * @param right The x-coordinate of the right edge of the rectangle.
 * @param bottom The y-coordinate of the bottom edge of the rectangle.
 * @param strokeWidth The width of the rectangle's stroke.
 * @param shader Pointer to the NativeBasicShader used for rendering the rectangle.
 * @param style The painting style (fill or stroke) to be applied to the rectangle.
 */
void RectGradientRenderNode::drawRect(float left, float top, float right, float bottom, float strokeWidth,
                                      NativeBasicShader *shader, OH_Native_Draw_PaintingStyle style) {
    // create or update properties
    this->paintingStyle = style;
    this->shader = shader;
    this->createOrUpdateLeftTopPosProperty(left, top);
    this->createOrUpdateRightBottomPosProperty(right, bottom);
    this->createOrUpdateStrokeWidthProperty(strokeWidth);
}

/**
 * @brief Initializes the content modifier for the RectGradientRenderNode.
 *
 * This function creates and attaches a content modifier to the render node if it does not already exist.
 * It sets up a custom drawing callback that renders a rectangle with a gradient shader effect, supporting
 * both stroke and fill painting styles. The drawing logic retrieves the necessary properties (width, height,
 * position, and stroke width) and uses the appropriate drawing APIs to render the rectangle with the specified
 * gradient effect. All drawing resources are properly managed and released after use.
 *
 * The function ensures that the modifier is only initialized once and handles any errors that may occur
 * during the creation or attachment of the modifier.
 */
void RectGradientRenderNode::initModifier() {
    if (!modifier_) {
        modifier_ = OH_ArkUI_RenderNodeUtils_CreateContentModifier();
        maybeThrow(OH_ArkUI_RenderNodeUtils_AttachContentModifier(nodeHandle_, modifier_));
        maybeThrow(OH_ArkUI_RenderNodeUtils_SetContentModifierOnDraw(
            modifier_, this, [](ArkUI_DrawContext *context, void *userData) {
                auto *data = static_cast<RectGradientRenderNode *>(userData);
                auto *canvas1 = OH_ArkUI_DrawContext_GetCanvas(context);
                OH_Drawing_Canvas *canvas = reinterpret_cast<OH_Drawing_Canvas *>(canvas1);
                OH_Drawing_ShaderEffect *shaderEffect = CreateShaderEffect(data->shader);

                float left = 0.0f;
                float top = 0.0f;
                float right = 0.0f;
                float bottom = 0.0f;
                float strokeWidth = 0.0f;

                OH_ArkUI_RenderNodeUtils_GetVector2PropertyValue(data->leftTopPosProperty_, &left, &top);
                OH_ArkUI_RenderNodeUtils_GetVector2PropertyValue(data->rightBottomProperty_, &right, &bottom);

                if (data->paintingStyle == OH_Native_Draw_PaintingStyle::Stroke) {
                    LOGI("OHRenderNodeDrawRect: draw stroke with shader: %{public}p", shaderEffect);
                    // 创建画笔并绑定渐变
                    OH_Drawing_Pen *pen = OH_Drawing_PenCreate();
                    OH_Drawing_PenSetShaderEffect(pen, shaderEffect);
                    OH_ArkUI_RenderNodeUtils_GetFloatPropertyValue(data->strokeWidthProperty_, &strokeWidth);
                    LOGI("OHRenderNodeDrawRect: strokeWidth: %{public}f", strokeWidth);
                    OH_Drawing_PenSetWidth(pen, strokeWidth);

                    OH_Drawing_CanvasAttachPen(canvas, pen);

                    // 绘制矩形
                    OH_Drawing_Rect *rect = OH_Drawing_RectCreate(left, top, right, bottom);
                    OH_Drawing_CanvasDrawRect(canvas, rect);

                    // 释放绘制资源
                    OH_Drawing_CanvasDetachBrush(canvas);
                    OH_Drawing_RectDestroy(rect);
                    OH_Drawing_PenDestroy(pen);
                    OH_Drawing_ShaderEffectDestroy(shaderEffect);
                } else {
                    LOGI("OHRenderNodeDrawRect: draw fill with shader: %{public}p", shaderEffect);
                    // 创建画笔刷并绑定渐变
                    OH_Drawing_Brush *brush = OH_Drawing_BrushCreate();
                    OH_Drawing_BrushSetShaderEffect(brush, shaderEffect);
                    OH_Drawing_CanvasAttachBrush(canvas, brush);

                    // 绘制矩形
                    OH_Drawing_Rect *rect = OH_Drawing_RectCreate(left, top, right, bottom);
                    OH_Drawing_CanvasDrawRect(canvas, rect);

                    // 释放绘制资源
                    OH_Drawing_CanvasDetachBrush(canvas);
                    OH_Drawing_RectDestroy(rect);
                    OH_Drawing_BrushDestroy(brush);
                    OH_Drawing_ShaderEffectDestroy(shaderEffect);
                }
                LOGI("OHRenderNodeDrawRect: draw with shader finish: %{public}p", shaderEffect);
            }));
    }
}

void RectGradientRenderNode::createOrUpdateLeftTopPosProperty(float left, float top) {
    if (!leftTopPosProperty_) {
        leftTopPosProperty_ = OH_ArkUI_RenderNodeUtils_CreateVector2Property(left, top);
        maybeThrow(OH_ArkUI_RenderNodeUtils_AttachVector2Property(modifier_, leftTopPosProperty_));
    } else {
        maybeThrow(OH_ArkUI_RenderNodeUtils_SetVector2PropertyValue(leftTopPosProperty_, left, top));
    }
}

void RectGradientRenderNode::createOrUpdateRightBottomPosProperty(float right, float bottom) {
    if (!rightBottomProperty_) {
        rightBottomProperty_ = OH_ArkUI_RenderNodeUtils_CreateVector2Property(right, bottom);
        maybeThrow(OH_ArkUI_RenderNodeUtils_AttachVector2Property(modifier_, rightBottomProperty_));
    } else {
        maybeThrow(OH_ArkUI_RenderNodeUtils_SetVector2PropertyValue(rightBottomProperty_, right, bottom));
    }
}

void RectGradientRenderNode::createOrUpdateStrokeWidthProperty(float strokeWidth) {
    if (!strokeWidthProperty_) {
        strokeWidthProperty_ = OH_ArkUI_RenderNodeUtils_CreateFloatProperty(strokeWidth);
        maybeThrow(OH_ArkUI_RenderNodeUtils_AttachFloatProperty(modifier_, strokeWidthProperty_));
    } else {
        maybeThrow(OH_ArkUI_RenderNodeUtils_SetFloatPropertyValue(strokeWidthProperty_, strokeWidth));
    }
}
} // namespace OH