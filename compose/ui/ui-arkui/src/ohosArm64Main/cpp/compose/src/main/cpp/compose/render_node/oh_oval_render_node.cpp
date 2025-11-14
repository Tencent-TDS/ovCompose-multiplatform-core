#include <native_drawing/drawing_brush.h>
#include <native_drawing/drawing_canvas.h>
#include <native_drawing/drawing_pen.h>
#include <native_drawing/drawing_rect.h>
#include <cfloat>
#include "oh_oval_render_node.h"
#include "../xcomponent_log.h"

namespace OH {
OvalRenderNode::~OvalRenderNode() {
    if (invalidateCountProperty_) {
        OH_ArkUI_RenderNodeUtils_DisposeFloatProperty(invalidateCountProperty_);
    }
    if (modifier_) {
        OH_ArkUI_RenderNodeUtils_DisposeContentModifier(modifier_);
    }
}

OvalRenderNode::OvalRenderNode() {
    this->OvalRenderNode::initModifier();
}

OH_DrawingNode_Type OvalRenderNode::getType() {
    return OH_DrawingNode_Type::OvalNode;
};

void OvalRenderNode::drawOval(const float left, const float top, const float right, const float bottom,
                              const float strokeWidth, const uint32_t color, OH_Native_Draw_PaintingStyle style) {
    OH::SystraceSection trace("OvalRenderNode:drawOval");
    // 直接更新成员变量
    left_ = left;
    top_ = top;
    right_ = right;
    bottom_ = bottom;
    strokeWidth_ = strokeWidth;
    color_ = color;
    paintingStyle = style;

    // 调用invalidate()触发onDraw
    invalidate();
}

void OvalRenderNode::invalidate() {
    OH::SystraceSection trace("OvalRenderNode:invalidate");
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

void OvalRenderNode::initModifier() {
    if (!modifier_) {
        modifier_ = OH_ArkUI_RenderNodeUtils_CreateContentModifier();
        maybeThrow(OH_ArkUI_RenderNodeUtils_AttachContentModifier(nodeHandle_, modifier_));

        // 创建invalidateCount PropertyHandle
        invalidateCountProperty_ = OH_ArkUI_RenderNodeUtils_CreateFloatProperty(0.0f);
        maybeThrow(OH_ArkUI_RenderNodeUtils_AttachFloatProperty(modifier_, invalidateCountProperty_));

        maybeThrow(OH_ArkUI_RenderNodeUtils_SetContentModifierOnDraw(
            modifier_, this, [](ArkUI_DrawContext *context, void *userData) {
                OH::SystraceSection trace("OvalRenderNode:modifierOnDraw");
                const auto *data = static_cast<OvalRenderNode *>(userData);
                auto *canvas1 = OH_ArkUI_DrawContext_GetCanvas(context);
                auto *canvas = static_cast<OH_Drawing_Canvas *>(canvas1);

                const float left = 0;
                const float top = 0;
                const float right = data->right_ - data->left_;
                const float bottom = data->bottom_ - data->top_;
                const float strokeWidth = data->strokeWidth_;
                const uint32_t color = data->color_;

                OH_Drawing_Rect *rect = OH_Drawing_RectCreate(left, top, right, bottom);

                if (data->paintingStyle == OH_Native_Draw_PaintingStyle::Stroke) {
                    OH_Drawing_Pen *pen = OH_Drawing_PenCreate();
                    OH_Drawing_PenSetWidth(pen, strokeWidth);
                    OH_Drawing_PenSetColor(pen, color);
                    OH_Drawing_CanvasAttachPen(canvas, pen);
                    OH_Drawing_CanvasDrawOval(canvas, rect);
                    OH_Drawing_CanvasDetachPen(canvas);
                    OH_Drawing_PenDestroy(pen);
                } else {
                    OH_Drawing_Brush *brush = OH_Drawing_BrushCreate();
                    OH_Drawing_BrushSetColor(brush, color);
                    OH_Drawing_CanvasAttachBrush(canvas, brush);
                    OH_Drawing_CanvasDrawOval(canvas, rect);
                    OH_Drawing_CanvasDetachBrush(canvas);
                    OH_Drawing_BrushDestroy(brush);
                }

                OH_Drawing_RectDestroy(rect);
            }));
    }
}
} // namespace OH
