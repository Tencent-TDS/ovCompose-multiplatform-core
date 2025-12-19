#include <native_drawing/drawing_canvas.h>
#include <native_drawing/drawing_path.h>
#include <native_drawing/drawing_pen.h>
#include <cfloat>
#include "oh_line_render_node.h"

namespace OH {
LineRenderNode::~LineRenderNode() {
    if (invalidateCountProperty_) {
        OH_ArkUI_RenderNodeUtils_DisposeFloatProperty(invalidateCountProperty_);
    }
    if (modifier_) {
        OH_ArkUI_RenderNodeUtils_DisposeContentModifier(modifier_);
    }
}

LineRenderNode::LineRenderNode() {
    this->LineRenderNode::initModifier();
}

OH_DrawingNode_Type LineRenderNode::getType() {
    return OH_DrawingNode_Type::LineNode;
};

void LineRenderNode::drawLine(const float x1, const float y1, const float x2, const float y2, const float lineWidth,
                              const uint32_t lineColor, OH_Native_Draw_StrokeCap strokeCap) {
    // 直接更新成员变量
    x1_ = x1;
    y1_ = y1;
    x2_ = x2;
    y2_ = y2;
    lineWidth_ = lineWidth;
    lineColor_ = lineColor;
    this->strokeCap = strokeCap;

    // 调用invalidate()触发onDraw
    invalidate();
}

void LineRenderNode::invalidate() {
    // 读取当前值
    float currentCount = 0.0f;
    OH_ArkUI_RenderNodeUtils_GetFloatPropertyValue(invalidateCountProperty_, &currentCount);

    // 加1，处理溢出（回绕到0）
    float newCount = (currentCount >= FLT_MAX - 1.0f) ? 0.0f : (currentCount + 1.0f);

    // 设置新值，触发onDraw回调
    OH_ArkUI_RenderNodeUtils_SetFloatPropertyValue(invalidateCountProperty_, newCount);
}

/**
 * @brief Initializes the content modifier for the LineRenderNode.
 *
 * This function creates and attaches a content modifier to the render node if
 * it does not already exist. It sets up the drawing callback for the modifier,
 * which is responsible for rendering a line on the canvas. The callback
 * retrieves the line's start and end points, width, color, and stroke cap style
 * from the node's properties, constructs a drawing path, and draws the line
 * using the appropriate pen settings. All drawing resources are properly
 * released after rendering.
 *
 * Throws an exception if any of the underlying ArkUI or drawing utility calls
 * fail.
 */
void LineRenderNode::initModifier() {
    if (!modifier_) {
        modifier_ = OH_ArkUI_RenderNodeUtils_CreateContentModifier();
        maybeThrow(OH_ArkUI_RenderNodeUtils_AttachContentModifier(nodeHandle_, modifier_));

        // 创建invalidateCount PropertyHandle
        invalidateCountProperty_ = OH_ArkUI_RenderNodeUtils_CreateFloatProperty(0.0f);
        maybeThrow(OH_ArkUI_RenderNodeUtils_AttachFloatProperty(modifier_, invalidateCountProperty_));

        maybeThrow(OH_ArkUI_RenderNodeUtils_SetContentModifierOnDraw(
            modifier_, this, [](ArkUI_DrawContext *context, void *userData) {
                const auto *data = static_cast<LineRenderNode *>(userData);
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
                const uint32_t lineColor = data->lineColor_;
                const OH_Native_Draw_StrokeCap nativeStokeCap = data->strokeCap;

                OH_Drawing_PenLineCapStyle lineCapStyle = OH_Drawing_PenLineCapStyle::LINE_FLAT_CAP;
                if (nativeStokeCap == OH_Native_Draw_StrokeCap::StrokeCapRound) {
                    lineCapStyle = OH_Drawing_PenLineCapStyle::LINE_ROUND_CAP;
                } else if (nativeStokeCap == OH_Native_Draw_StrokeCap::StrokeCapSquare) {
                    lineCapStyle = OH_Drawing_PenLineCapStyle::LINE_SQUARE_CAP;
                }

                auto path = OH_Drawing_PathCreate();
                OH_Drawing_PathMoveTo(path, relStartX, relStartY);
                OH_Drawing_PathLineTo(path, relEndX, relEndY);
                // 注意：对于直线，不应该调用PathClose，否则会绘制一个闭合的形状

                auto pen = OH_Drawing_PenCreate();
                OH_Drawing_PenSetWidth(pen, width);
                OH_Drawing_PenSetColor(pen, lineColor);
                OH_Drawing_PenSetCap(pen, lineCapStyle);
                OH_Drawing_CanvasAttachPen(canvas, pen);
                OH_Drawing_CanvasDrawPath(canvas, path);

                // 释放绘制资源
                OH_Drawing_CanvasDetachPen(canvas);
                OH_Drawing_PathDestroy(path);
                OH_Drawing_PenDestroy(pen);
            }));
    }
}
} // namespace OH