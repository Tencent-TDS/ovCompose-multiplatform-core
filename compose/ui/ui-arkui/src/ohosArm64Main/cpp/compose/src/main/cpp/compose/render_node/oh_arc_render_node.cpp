#include <native_drawing/drawing_brush.h>
#include <native_drawing/drawing_canvas.h>
#include <native_drawing/drawing_error_code.h>
#include <native_drawing/drawing_pen.h>
#include <native_drawing/drawing_rect.h>
#include <cfloat>
#include "oh_arc_render_node.h"
#include "../xcomponent_log.h"

namespace OH {
ArcRenderNode::~ArcRenderNode() {
    if (invalidateCountProperty_) {
        OH_ArkUI_RenderNodeUtils_DisposeFloatProperty(invalidateCountProperty_);
    }
    if (modifier_) {
        OH_ArkUI_RenderNodeUtils_DisposeContentModifier(modifier_);
    }
}

ArcRenderNode::ArcRenderNode() {
    this->ArcRenderNode::initModifier();
}

OH_DrawingNode_Type ArcRenderNode::getType() {
    return OH_DrawingNode_Type::ArcNode;
};

void ArcRenderNode::drawArc(const float left, const float top, const float right, const float bottom,
                            const float startAngle, const float sweepAngle, const bool useCenter,
                            const float strokeWidth, const uint32_t color, OH_Native_Draw_PaintingStyle style) {
    // 直接更新成员变量
    left_ = left;
    top_ = top;
    right_ = right;
    bottom_ = bottom;
    startAngle_ = startAngle;
    sweepAngle_ = sweepAngle;
    useCenter_ = useCenter;
    strokeWidth_ = strokeWidth;
    color_ = color;
    paintingStyle = style;

    // 调用invalidate()触发onDraw
    invalidate();
}

void ArcRenderNode::invalidate() {
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

void ArcRenderNode::initModifier() {
    if (!modifier_) {
        modifier_ = OH_ArkUI_RenderNodeUtils_CreateContentModifier();
        maybeThrow(OH_ArkUI_RenderNodeUtils_AttachContentModifier(nodeHandle_, modifier_));

        // 创建invalidateCount PropertyHandle
        invalidateCountProperty_ = OH_ArkUI_RenderNodeUtils_CreateFloatProperty(0.0f);
        maybeThrow(OH_ArkUI_RenderNodeUtils_AttachFloatProperty(modifier_, invalidateCountProperty_));

        maybeThrow(OH_ArkUI_RenderNodeUtils_SetContentModifierOnDraw(
            modifier_, this, [](ArkUI_DrawContext *context, void *userData) {
                const auto *data = static_cast<ArcRenderNode *>(userData);
                auto *canvas1 = OH_ArkUI_DrawContext_GetCanvas(context);
                auto *canvas = static_cast<OH_Drawing_Canvas *>(canvas1);

                // 使用相对坐标（相对于RenderNode的(0,0)点）
                // RenderNode的position已经设置为(left - strokeWidth/2, top - strokeWidth/2)
                // 因此onDraw中应该使用(0, 0)作为左上角
                const float relLeft = 0.0f;
                const float relTop = 0.0f;
                const float relRight = data->right_ - data->left_;
                const float relBottom = data->bottom_ - data->top_;
                const float startAngle = data->startAngle_;
                const float sweepAngle = data->sweepAngle_;
                const bool useCenter = data->useCenter_;
                const float strokeWidth = data->strokeWidth_;
                const uint32_t color = data->color_;

                OH_Drawing_Rect *rect = OH_Drawing_RectCreate(relLeft, relTop, relRight, relBottom);

                if (data->paintingStyle == OH_Native_Draw_PaintingStyle::Stroke) {
                    // Stroke模式，使用OH_Drawing_CanvasDrawArcWithCenter（API 18+）
                    OH_Drawing_Pen *pen = OH_Drawing_PenCreate();
                    OH_Drawing_PenSetWidth(pen, strokeWidth);
                    OH_Drawing_PenSetColor(pen, color);
                    OH_Drawing_CanvasAttachPen(canvas, pen);
                    OH_Drawing_ErrorCode result =
                        OH_Drawing_CanvasDrawArcWithCenter(canvas, rect, startAngle, sweepAngle, useCenter);
                    if (result != OH_DRAWING_SUCCESS) {
                        LOGE("OH_Drawing_CanvasDrawArcWithCenter failed with error: %{public}d", result);
                    }
                    OH_Drawing_CanvasDetachPen(canvas);
                    OH_Drawing_PenDestroy(pen);
                } else {
                    // Fill模式，使用OH_Drawing_CanvasDrawArcWithCenter
                    OH_Drawing_Brush *brush = OH_Drawing_BrushCreate();
                    OH_Drawing_BrushSetColor(brush, color);
                    OH_Drawing_CanvasAttachBrush(canvas, brush);
                    OH_Drawing_ErrorCode result =
                        OH_Drawing_CanvasDrawArcWithCenter(canvas, rect, startAngle, sweepAngle, useCenter);
                    if (result != OH_DRAWING_SUCCESS) {
                        LOGE("OH_Drawing_CanvasDrawArcWithCenter failed with error: %{public}d", result);
                    }
                    OH_Drawing_CanvasDetachBrush(canvas);
                    OH_Drawing_BrushDestroy(brush);
                }

                OH_Drawing_RectDestroy(rect);
            }));
    }
}
} // namespace OH
