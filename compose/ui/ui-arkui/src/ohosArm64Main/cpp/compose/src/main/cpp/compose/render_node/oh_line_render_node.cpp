#include "oh_line_render_node.h"

namespace OH {
LineRenderNode::~LineRenderNode() {
    if (startPointProperty_) {
        OH_ArkUI_RenderNodeUtils_DisposeVector2Property(startPointProperty_);
    }
    if (endPointProperty_) {
        OH_ArkUI_RenderNodeUtils_DisposeVector2Property(endPointProperty_);
    }
    if (widthProperty_) {
        OH_ArkUI_RenderNodeUtils_DisposeFloatProperty(widthProperty_);
    }
    if (colorProperty_) {
        OH_ArkUI_RenderNodeUtils_DisposeColorProperty(colorProperty_);
    }
    if (modifier_) {
        OH_ArkUI_RenderNodeUtils_DisposeContentModifier(modifier_);
    }
}

LineRenderNode::LineRenderNode() : BaseRenderNode() {
    this->initModifier();
}

OH_DrawingNode_Type LineRenderNode::getType() { return OH_DrawingNode_Type::LineNode; };

/**
 * @brief Draws a line with the specified start and end coordinates, width, color, and stroke cap style.
 *
 * This method updates the line's properties, including its start and end points, width, color, and stroke cap.
 *
 * @param x1 The x-coordinate of the start point of the line.
 * @param y1 The y-coordinate of the start point of the line.
 * @param x2 The x-coordinate of the end point of the line.
 * @param y2 The y-coordinate of the end point of the line.
 * @param lineWidth The width of the line.
 * @param lineColor The color of the line, represented as a 32-bit unsigned integer.
 * @param strokeCap The style of the stroke cap to use for the line ends.
 */
void LineRenderNode::drawLine(float x1, float y1, float x2, float y2, float lineWidth, uint32_t lineColor,
                              OH_Native_Draw_StrokeCap strokeCap) {
    // create or update properties
    this->strokeCap = strokeCap;
    this->createOrUpdateStartPointProperty(x1, y1);
    this->createOrUpdateEndPointProperty(x2, y2);
    this->createOrUpdateWidthProperty(lineWidth);
    this->createOrUpdateColorProperty(lineColor);
}

/**
 * @brief Initializes the content modifier for the LineRenderNode.
 *
 * This function creates and attaches a content modifier to the render node if it does not already exist.
 * It sets up the drawing callback for the modifier, which is responsible for rendering a line on the canvas.
 * The callback retrieves the line's start and end points, width, color, and stroke cap style from the node's properties,
 * constructs a drawing path, and draws the line using the appropriate pen settings.
 * All drawing resources are properly released after rendering.
 *
 * Throws an exception if any of the underlying ArkUI or drawing utility calls fail.
 */
void LineRenderNode::initModifier() {
    if (!modifier_) {
        modifier_ = OH_ArkUI_RenderNodeUtils_CreateContentModifier();
        maybeThrow(OH_ArkUI_RenderNodeUtils_AttachContentModifier(nodeHandle_, modifier_));
        // 关联modifier和property。
        maybeThrow(OH_ArkUI_RenderNodeUtils_SetContentModifierOnDraw(
            modifier_, this, [](ArkUI_DrawContext *context, void *userData) {
                auto *data = static_cast<LineRenderNode *>(userData);
                auto *canvas1 = OH_ArkUI_DrawContext_GetCanvas(context);
                OH_Drawing_Canvas *canvas = reinterpret_cast<OH_Drawing_Canvas *>(canvas1);

                auto path = OH_Drawing_PathCreate();
                float startX = 0;
                float startY = 0;
                float endX = 0;
                float endY = 0;
                float width = 0;
                uint32_t lineColor = 0;
                OH_Drawing_PenLineCapStyle lineCapStyle = OH_Drawing_PenLineCapStyle::LINE_FLAT_CAP;
                OH_Native_Draw_StrokeCap nativeStokeCap = data->strokeCap;

                OH_ArkUI_RenderNodeUtils_GetVector2PropertyValue(data->startPointProperty_, &startX, &startY);
                OH_ArkUI_RenderNodeUtils_GetVector2PropertyValue(data->endPointProperty_, &endX, &endY);
                OH_ArkUI_RenderNodeUtils_GetFloatPropertyValue(data->widthProperty_, &width);
                OH_ArkUI_RenderNodeUtils_GetColorPropertyValue(data->colorProperty_, &lineColor);
                if (nativeStokeCap == OH_Native_Draw_StrokeCap::StrokeCapRound) {
                    lineCapStyle = OH_Drawing_PenLineCapStyle::LINE_ROUND_CAP;
                } else if (nativeStokeCap == OH_Native_Draw_StrokeCap::StrokeCapSquare) {
                    lineCapStyle = OH_Drawing_PenLineCapStyle::LINE_SQUARE_CAP;
                }

                OH_Drawing_PathMoveTo(path, startX, startY);
                OH_Drawing_PathLineTo(path, endX, endY);
                OH_Drawing_PathClose(path);

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

void LineRenderNode::createOrUpdateStartPointProperty(float x, float y) {
    if (!startPointProperty_) {
        startPointProperty_ = OH_ArkUI_RenderNodeUtils_CreateVector2Property(x, y);
        maybeThrow(OH_ArkUI_RenderNodeUtils_AttachVector2Property(modifier_, startPointProperty_));
    } else {
        maybeThrow(OH_ArkUI_RenderNodeUtils_SetVector2PropertyValue(startPointProperty_, x, y));
    }
}

void LineRenderNode::createOrUpdateEndPointProperty(float x, float y) {
    if (!endPointProperty_) {
        endPointProperty_ = OH_ArkUI_RenderNodeUtils_CreateVector2Property(x, y);
        maybeThrow(OH_ArkUI_RenderNodeUtils_AttachVector2Property(modifier_, endPointProperty_));
    } else {
        maybeThrow(OH_ArkUI_RenderNodeUtils_SetVector2PropertyValue(endPointProperty_, x, y));
    }
}

void LineRenderNode::createOrUpdateWidthProperty(float width) {
    if (!widthProperty_) {
        widthProperty_ = OH_ArkUI_RenderNodeUtils_CreateFloatProperty(width);
        maybeThrow(OH_ArkUI_RenderNodeUtils_AttachFloatProperty(modifier_, widthProperty_));
    } else {
        maybeThrow(OH_ArkUI_RenderNodeUtils_SetFloatPropertyValue(widthProperty_, width));
    }
}

void LineRenderNode::createOrUpdateColorProperty(uint32_t color) {
    if (!colorProperty_) {
        colorProperty_ = OH_ArkUI_RenderNodeUtils_CreateColorProperty(color);
        maybeThrow(OH_ArkUI_RenderNodeUtils_AttachColorProperty(modifier_, colorProperty_));
    } else {
        maybeThrow(OH_ArkUI_RenderNodeUtils_SetColorPropertyValue(colorProperty_, color));
    }
}
} // namespace OH