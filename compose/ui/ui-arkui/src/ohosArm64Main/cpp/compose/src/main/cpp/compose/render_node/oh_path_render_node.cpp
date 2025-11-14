#include <native_drawing/drawing_brush.h>
#include <native_drawing/drawing_canvas.h>
#include <native_drawing/drawing_path.h>
#include <native_drawing/drawing_pen.h>
#include <cfloat>
#include "oh_path_render_node.h"
#include "../xcomponent_log.h"
#include "../path/oh_native_path_export.h"

namespace OH {
PathRenderNode::~PathRenderNode() {
    if (pathProperty_) {
        OH_Drawing_PathDestroy(pathProperty_);
        pathProperty_ = nullptr;
    }
    if (invalidateCountProperty_) {
        OH_ArkUI_RenderNodeUtils_DisposeFloatProperty(invalidateCountProperty_);
    }
    if (modifier_) {
        OH_ArkUI_RenderNodeUtils_DisposeContentModifier(modifier_);
    }
}

PathRenderNode::PathRenderNode() {
    this->PathRenderNode::initModifier();
}

OH_DrawingNode_Type PathRenderNode::getType() {
    return OH_DrawingNode_Type::PathNode;
}

void PathRenderNode::drawPath(OH_Drawing_Path *path, const float strokeWidth, const uint32_t color,
                              OH_Native_Draw_PaintingStyle style) {
    // 更新pathProperty_
    if (pathProperty_) {
        OH_Drawing_PathDestroy(pathProperty_);
    }
    if (path) {
        pathProperty_ = OH_Drawing_PathCopy(path);

        // 计算Path的bounds，并将Path平移到相对坐标
        // RenderNode的position会设置为(left - halfStroke, top - halfStroke)
        // 因此需要将Path平移到(-left, -top)，这样Path就会相对于RenderNode的(0,0)点
        float left = 0.0f, top = 0.0f, right = 0.0f, bottom = 0.0f;
        OHPath_getBounds(pathProperty_, &left, &top, &right, &bottom);

        // 存储bounds的左上角坐标（用于验证）
        pathOffsetX_ = left;
        pathOffsetY_ = top;

        // 将Path平移到相对坐标（相对于bounds的左上角）
        // 注意：这里平移的是-left和-top，使得Path的bounds从(0, 0)开始
        OHPath_translate(pathProperty_, -left, -top);
    } else {
        pathProperty_ = nullptr;
        pathOffsetX_ = 0.0f;
        pathOffsetY_ = 0.0f;
    }

    // 直接更新成员变量
    strokeWidth_ = strokeWidth;
    color_ = color;
    paintingStyle = style;

    // 调用invalidate()触发onDraw
    invalidate();
}

void PathRenderNode::invalidate() {
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

void PathRenderNode::initModifier() {
    if (!modifier_) {
        modifier_ = OH_ArkUI_RenderNodeUtils_CreateContentModifier();
        maybeThrow(OH_ArkUI_RenderNodeUtils_AttachContentModifier(nodeHandle_, modifier_));

        // 创建invalidateCount PropertyHandle
        invalidateCountProperty_ = OH_ArkUI_RenderNodeUtils_CreateFloatProperty(0.0f);
        maybeThrow(OH_ArkUI_RenderNodeUtils_AttachFloatProperty(modifier_, invalidateCountProperty_));

        maybeThrow(OH_ArkUI_RenderNodeUtils_SetContentModifierOnDraw(
            modifier_, this, [](ArkUI_DrawContext *context, void *userData) {
                const auto *data = static_cast<PathRenderNode *>(userData);
                auto *canvas1 = OH_ArkUI_DrawContext_GetCanvas(context);
                auto *canvas = static_cast<OH_Drawing_Canvas *>(canvas1);

                if (!data->pathProperty_) {
                    LOGE("PathRenderNode::initModifier: pathProperty_ is nullptr");
                    return;
                }

                // Path已经在drawPath时平移到相对坐标（相对于bounds的左上角）
                // RenderNode的position已经设置为(left - halfStroke, top - halfStroke)
                // 因此onDraw中直接使用Path即可，Path已经是相对坐标
                const float strokeWidth = data->strokeWidth_;
                const uint32_t color = data->color_;

                if (data->paintingStyle == OH_Native_Draw_PaintingStyle::Stroke) {
                    OH_Drawing_Pen *pen = OH_Drawing_PenCreate();
                    OH_Drawing_PenSetWidth(pen, strokeWidth);
                    OH_Drawing_PenSetColor(pen, color);
                    OH_Drawing_CanvasAttachPen(canvas, pen);
                    OH_Drawing_CanvasDrawPath(canvas, data->pathProperty_);
                    OH_Drawing_CanvasDetachPen(canvas);
                    OH_Drawing_PenDestroy(pen);
                } else { // Fill style
                    OH_Drawing_Brush *brush = OH_Drawing_BrushCreate();
                    OH_Drawing_BrushSetColor(brush, color);
                    OH_Drawing_CanvasAttachBrush(canvas, brush);
                    OH_Drawing_CanvasDrawPath(canvas, data->pathProperty_);
                    OH_Drawing_CanvasDetachBrush(canvas);
                    OH_Drawing_BrushDestroy(brush);
                }
            }));
    }
}

} // namespace OH
