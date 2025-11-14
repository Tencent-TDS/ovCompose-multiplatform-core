#include "oh_rect_gradient_render_node.h"
#include <native_drawing/drawing_brush.h>
#include <native_drawing/drawing_canvas.h>
#include <native_drawing/drawing_pen.h>
#include <native_drawing/drawing_rect.h>
#include <cfloat>
#include "../shader/oh_native_shader_utils.h"
#include "../xcomponent_log.h"

namespace OH {
RectGradientRenderNode::~RectGradientRenderNode() {
    if (invalidateCountProperty_) {
        OH_ArkUI_RenderNodeUtils_DisposeFloatProperty(invalidateCountProperty_);
    }
    if (modifier_) {
        OH_ArkUI_RenderNodeUtils_DisposeContentModifier(modifier_);
    }
}

RectGradientRenderNode::RectGradientRenderNode() {
    this->RectGradientRenderNode::initModifier();
}

OH_DrawingNode_Type RectGradientRenderNode::getType() {
    return OH_DrawingNode_Type::RectGradientNode;
};

/**
 * @brief Draws a rectangle with the specified coordinates, stroke width,
 * shader, and painting style.
 *
 * This method sets the painting style and shader for the rectangle, and updates
 * the rectangle's position and stroke width properties. It does not perform the
 * actual drawing, but prepares the necessary properties for rendering.
 *
 * @param left The x-coordinate of the left edge of the rectangle.
 * @param top The y-coordinate of the top edge of the rectangle.
 * @param right The x-coordinate of the right edge of the rectangle.
 * @param bottom The y-coordinate of the bottom edge of the rectangle.
 * @param strokeWidth The width of the rectangle's stroke.
 * @param shader Pointer to the NativeBasicShader used for rendering the
 * rectangle.
 * @param style The painting style (fill or stroke) to be applied to the
 * rectangle.
 */
void RectGradientRenderNode::drawRect(const float left, const float top, const float right, const float bottom,
                                      const float strokeWidth, NativeBasicShader *shader,
                                      OH_Native_Draw_PaintingStyle style) {
    // 直接更新成员变量
    left_ = left;
    top_ = top;
    right_ = right;
    bottom_ = bottom;
    strokeWidth_ = strokeWidth;
    this->shader = shader;
    this->paintingStyle = style;

    // 调用invalidate()触发onDraw
    invalidate();
}

void RectGradientRenderNode::invalidate() {
    if (!invalidateCountProperty_) {
        return;
    }

    // 读取当前值
    float currentCount = 0.0f;
    OH_ArkUI_RenderNodeUtils_GetFloatPropertyValue(invalidateCountProperty_, &currentCount);

    // 加1，处理溢出（回绕到0）
    float newCount = (currentCount >= FLT_MAX - 1.0f) ? 0.0f : (currentCount + 1.0f);

    // 设置新值，触发onDraw回调
    OH_ArkUI_RenderNodeUtils_SetFloatPropertyValue(invalidateCountProperty_, newCount);
}

void RectGradientRenderNode::initModifier() {
    if (!modifier_) {
        modifier_ = OH_ArkUI_RenderNodeUtils_CreateContentModifier();
        maybeThrow(OH_ArkUI_RenderNodeUtils_AttachContentModifier(nodeHandle_, modifier_));

        // 创建invalidateCount PropertyHandle
        invalidateCountProperty_ = OH_ArkUI_RenderNodeUtils_CreateFloatProperty(0.0f);
        maybeThrow(OH_ArkUI_RenderNodeUtils_AttachFloatProperty(modifier_, invalidateCountProperty_));

        maybeThrow(OH_ArkUI_RenderNodeUtils_SetContentModifierOnDraw(
            modifier_, this, [](ArkUI_DrawContext *context, void *userData) {
                const auto *data = static_cast<RectGradientRenderNode *>(userData);
                auto *canvas1 = OH_ArkUI_DrawContext_GetCanvas(context);
                auto *canvas = static_cast<OH_Drawing_Canvas *>(canvas1);

                // 使用相对坐标（相对于RenderNode的(0,0)点）
                // RenderNode的position已经设置为(left - strokeWidth/2, top - strokeWidth/2)
                // 因此onDraw中应该使用(0, 0)作为左上角
                const float relLeft = 0.0f;
                const float relTop = 0.0f;
                const float relRight = data->right_ - data->left_;
                const float relBottom = data->bottom_ - data->top_;
                const float strokeWidth = data->strokeWidth_;
                
                // 使用CreateShaderEffectWithScaledSize将渐变坐标从绝对坐标缩放为相对坐标
                // 参考iOS归一化方案：使用除法进行缩放
                // drawWidth和drawHeight是绘制区域的宽度和高度（相对坐标）
                const float drawWidth = relRight - relLeft;
                const float drawHeight = relBottom - relTop;
                OH_Drawing_ShaderEffect *shaderEffect = CreateShaderEffectWithScaledSize(
                    data->shader, drawWidth, drawHeight);

                if (data->paintingStyle == OH_Native_Draw_PaintingStyle::Stroke) {
                    LOGI("OHRenderNodeDrawRect: draw stroke with shader: %{public}p", shaderEffect);
                    // 创建画笔并绑定渐变
                    OH_Drawing_Pen *pen = OH_Drawing_PenCreate();
                    OH_Drawing_PenSetShaderEffect(pen, shaderEffect);
                    LOGI("OHRenderNodeDrawRect: strokeWidth: %{public}f", strokeWidth);
                    OH_Drawing_PenSetWidth(pen, strokeWidth);

                    OH_Drawing_CanvasAttachPen(canvas, pen);

                    // 绘制矩形
                    OH_Drawing_Rect *rect = OH_Drawing_RectCreate(relLeft, relTop, relRight, relBottom);
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
                    OH_Drawing_Rect *rect = OH_Drawing_RectCreate(relLeft, relTop, relRight, relBottom);
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
} // namespace OH