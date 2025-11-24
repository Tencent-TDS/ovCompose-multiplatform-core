#include <native_drawing/drawing_canvas.h>
#include <native_drawing/drawing_path.h>
#include <native_drawing/drawing_pen.h>
#include <cfloat>
#include "oh_line_gradient_render_node.h"
#include "../shader/oh_native_shader_utils.h"

namespace OH {
LineGradientRenderNode::~LineGradientRenderNode() {
    if (invalidateCountProperty_) {
        OH_ArkUI_RenderNodeUtils_DisposeFloatProperty(invalidateCountProperty_);
    }
    if (modifier_) {
        OH_ArkUI_RenderNodeUtils_DisposeContentModifier(modifier_);
    }
}

LineGradientRenderNode::LineGradientRenderNode() {
    this->LineGradientRenderNode::initModifier();
}

OH_DrawingNode_Type LineGradientRenderNode::getType() {
    return OH_DrawingNode_Type::LineGradientNode;
};

void LineGradientRenderNode::drawLine(const float x1, const float y1, const float x2, const float y2,
                                      const float lineWidth, NativeLinearGradientShader *shader,
                                      const OH_Native_Draw_StrokeCap strokeCap) {
    // 直接更新成员变量
    x1_ = x1;
    y1_ = y1;
    x2_ = x2;
    y2_ = y2;
    lineWidth_ = lineWidth;
    this->shader = shader;
    this->strokeCap = strokeCap;

    // 调用invalidate()触发onDraw
    invalidate();
}

void LineGradientRenderNode::invalidate() {
    // 读取当前值
    float currentCount = 0.0f;
    OH_ArkUI_RenderNodeUtils_GetFloatPropertyValue(invalidateCountProperty_, &currentCount);

    // 加1，处理溢出（回绕到0）
    const float newCount = (currentCount >= FLT_MAX - 1.0f) ? 0.0f : (currentCount + 1.0f);

    // 设置新值，触发onDraw回调
    OH_ArkUI_RenderNodeUtils_SetFloatPropertyValue(invalidateCountProperty_, newCount);
}

void LineGradientRenderNode::initModifier() {
    if (!modifier_) {
        modifier_ = OH_ArkUI_RenderNodeUtils_CreateContentModifier();
        maybeThrow(OH_ArkUI_RenderNodeUtils_AttachContentModifier(nodeHandle_, modifier_));

        // 创建invalidateCount PropertyHandle
        invalidateCountProperty_ = OH_ArkUI_RenderNodeUtils_CreateFloatProperty(0.0f);
        maybeThrow(OH_ArkUI_RenderNodeUtils_AttachFloatProperty(modifier_, invalidateCountProperty_));

        maybeThrow(OH_ArkUI_RenderNodeUtils_SetContentModifierOnDraw(
            modifier_, this, [](ArkUI_DrawContext *context, void *userData) {
                const auto *data = static_cast<LineGradientRenderNode *>(userData);
                auto *canvas1 = OH_ArkUI_DrawContext_GetCanvas(context);
                auto *canvas = static_cast<OH_Drawing_Canvas *>(canvas1);

                // 计算最小坐标（RenderNode的position设置为min(x1, x2) - halfStroke, min(y1, y2) - halfStroke）
                const float minX = (data->x1_ < data->x2_) ? data->x1_ : data->x2_;
                const float minY = (data->y1_ < data->y2_) ? data->y1_ : data->y2_;
                const float halfStroke = data->lineWidth_ / 2.0f;

                // 使用相对坐标（相对于RenderNode的(0,0)点）
                // 由于RenderNode的position已经减去了halfStroke，所以相对坐标需要加上halfStroke
                const float relStartX = data->x1_ - minX + halfStroke;
                const float relStartY = data->y1_ - minY + halfStroke;
                const float relEndX = data->x2_ - minX + halfStroke;
                const float relEndY = data->y2_ - minY + halfStroke;
                const float width = data->lineWidth_;
                const OH_Native_Draw_StrokeCap nativeStokeCap = data->strokeCap;

                // 使用CreateShaderEffectWithScaledSize将渐变坐标从绝对坐标缩放为相对坐标
                // 参考iOS归一化方案：使用除法进行缩放
                // drawWidth和drawHeight是直线的宽度和高度（相对坐标）
                const float drawWidth = fabs(relEndX - relStartX);
                const float drawHeight = fabs(relEndY - relStartY);
                OH_Drawing_ShaderEffect *shaderEffect = CreateShaderEffectWithScaledSize(
                    data->shader, drawWidth, drawHeight);

                OH_Drawing_PenLineCapStyle lineCapStyle = OH_Drawing_PenLineCapStyle::LINE_FLAT_CAP;
                if (nativeStokeCap == OH_Native_Draw_StrokeCap::StrokeCapRound) {
                    lineCapStyle = OH_Drawing_PenLineCapStyle::LINE_ROUND_CAP;
                } else if (nativeStokeCap == OH_Native_Draw_StrokeCap::StrokeCapSquare) {
                    lineCapStyle = OH_Drawing_PenLineCapStyle::LINE_SQUARE_CAP;
                }

                const auto path = OH_Drawing_PathCreate();
                OH_Drawing_PathMoveTo(path, relStartX, relStartY);
                OH_Drawing_PathLineTo(path, relEndX, relEndY);
                // 注意：对于直线，不应该调用PathClose，否则会绘制一个闭合的形状

                const auto pen = OH_Drawing_PenCreate();
                OH_Drawing_PenSetShaderEffect(pen, shaderEffect);
                OH_Drawing_PenSetWidth(pen, width);
                OH_Drawing_PenSetCap(pen, lineCapStyle);
                OH_Drawing_CanvasAttachPen(canvas, pen);
                OH_Drawing_CanvasDrawPath(canvas, path);

                // 释放绘制资源
                OH_Drawing_CanvasDetachPen(canvas);
                OH_Drawing_PathDestroy(path);
                OH_Drawing_PenDestroy(pen);
                OH_Drawing_ShaderEffectDestroy(shaderEffect);
            }));
    }
}
} // namespace OH