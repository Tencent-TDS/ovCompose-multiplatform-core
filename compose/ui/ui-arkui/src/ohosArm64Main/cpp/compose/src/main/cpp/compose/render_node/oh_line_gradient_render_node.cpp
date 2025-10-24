#include "oh_line_gradient_render_node.h"
#include "../shader/oh_native_shader_utils.h"

namespace OH {
LineGradientRenderNode::~LineGradientRenderNode() {
    // dispose properties and modifier
    if (startPointProperty_) {
        OH_ArkUI_RenderNodeUtils_DisposeVector2Property(startPointProperty_);
    }
    if (endPointProperty_) {
        OH_ArkUI_RenderNodeUtils_DisposeVector2Property(endPointProperty_);
    }
    if (widthProperty_) {
        OH_ArkUI_RenderNodeUtils_DisposeFloatProperty(widthProperty_);
    }
    if (modifier_) {
        OH_ArkUI_RenderNodeUtils_DisposeContentModifier(modifier_);
    }
}

LineGradientRenderNode::LineGradientRenderNode() : BaseRenderNode() {
    this->initModifier();
}

OH_DrawingNode_Type LineGradientRenderNode::getType() { return OH_DrawingNode_Type::LineGradientNode; };

/**
 * @brief Draws a line with a linear gradient shader and specified stroke properties.
 *
 * This method sets the start and end points, line width, shader, and stroke cap for the line.
 * It updates the internal properties accordingly to prepare for rendering.
 *
 * @param x1 The x-coordinate of the start point of the line.
 * @param y1 The y-coordinate of the start point of the line.
 * @param x2 The x-coordinate of the end point of the line.
 * @param y2 The y-coordinate of the end point of the line.
 * @param lineWidth The width of the line.
 * @param shader Pointer to the NativeLinearGradientShader to be used for the line.
 * @param strokeCap The style of the stroke cap to be applied to the ends of the line.
 */
void LineGradientRenderNode::drawLine(float x1, float y1, float x2, float y2, float lineWidth,
                                      NativeLinearGradientShader *shader, OH_Native_Draw_StrokeCap strokeCap) {
    // create or update properties
    this->shader = shader;
    this->strokeCap = strokeCap;
    this->createOrUpdateStartPointProperty(x1, y1);
    this->createOrUpdateEndPointProperty(x2, y2);
    this->createOrUpdateWidthProperty(lineWidth);
}

/**
 * @brief Initializes the content modifier for the LineGradientRenderNode.
 *
 * This method creates and attaches a content modifier if it does not already exist.
 * It sets up a custom drawing callback that renders a line with a gradient shader effect
 * using the current properties of the node, such as start and end points, stroke width,
 * and stroke cap style. The drawing callback constructs a path, configures a pen with
 * the appropriate shader and style, and draws the path onto the canvas. All drawing
 * resources are properly released after rendering.
 *
 * Throws an exception if any of the modifier or drawing operations fail.
 */
void LineGradientRenderNode::initModifier() {
    if (!modifier_) {
        modifier_ = OH_ArkUI_RenderNodeUtils_CreateContentModifier();
        maybeThrow(OH_ArkUI_RenderNodeUtils_AttachContentModifier(nodeHandle_, modifier_));
        maybeThrow(OH_ArkUI_RenderNodeUtils_SetContentModifierOnDraw(
            modifier_, this, [](ArkUI_DrawContext *context, void *userData) {
                auto *data = static_cast<LineGradientRenderNode *>(userData);
                auto *canvas1 = OH_ArkUI_DrawContext_GetCanvas(context);
                OH_Drawing_Canvas *canvas = reinterpret_cast<OH_Drawing_Canvas *>(canvas1);
                OH_Drawing_ShaderEffect *shaderEffect = CreateShaderEffect(data->shader);

                auto path = OH_Drawing_PathCreate();
                float startX = 0;
                float startY = 0;
                float endX = 0;
                float endY = 0;
                float width = 0;
                OH_Drawing_PenLineCapStyle lineCapStyle = OH_Drawing_PenLineCapStyle::LINE_FLAT_CAP;
                OH_Native_Draw_StrokeCap nativeStokeCap = data->strokeCap;

                OH_ArkUI_RenderNodeUtils_GetVector2PropertyValue(data->startPointProperty_, &startX, &startY);
                OH_ArkUI_RenderNodeUtils_GetVector2PropertyValue(data->endPointProperty_, &endX, &endY);
                OH_ArkUI_RenderNodeUtils_GetFloatPropertyValue(data->widthProperty_, &width);
                if (nativeStokeCap == OH_Native_Draw_StrokeCap::StrokeCapRound) {
                    lineCapStyle = OH_Drawing_PenLineCapStyle::LINE_ROUND_CAP;
                } else if (nativeStokeCap == OH_Native_Draw_StrokeCap::StrokeCapSquare) {
                    lineCapStyle = OH_Drawing_PenLineCapStyle::LINE_SQUARE_CAP;
                }

                OH_Drawing_PathMoveTo(path, startX, startY);
                OH_Drawing_PathLineTo(path, endX, endY);
                OH_Drawing_PathClose(path);

                auto pen = OH_Drawing_PenCreate();
                OH_Drawing_PenSetShaderEffect(pen, shaderEffect);
                OH_Drawing_PenSetWidth(pen, width);
                OH_Drawing_PenSetCap(pen, lineCapStyle);
                OH_Drawing_CanvasAttachPen(canvas, pen);
                OH_Drawing_CanvasDrawPath(canvas, path);

                // 释放绘制资源
                OH_Drawing_CanvasDetachPen(canvas);
                OH_Drawing_PathClose(path);
                OH_Drawing_PenDestroy(pen);
                OH_Drawing_ShaderEffectDestroy(shaderEffect);
            }));
    }
}

void LineGradientRenderNode::createOrUpdateStartPointProperty(float x, float y) {
    if (!startPointProperty_) {
        startPointProperty_ = OH_ArkUI_RenderNodeUtils_CreateVector2Property(x, y);
        maybeThrow(OH_ArkUI_RenderNodeUtils_AttachVector2Property(modifier_, startPointProperty_));
    } else {
        maybeThrow(OH_ArkUI_RenderNodeUtils_SetVector2PropertyValue(startPointProperty_, x, y));
    }
}
void LineGradientRenderNode::createOrUpdateEndPointProperty(float x, float y) {
    if (!endPointProperty_) {
        endPointProperty_ = OH_ArkUI_RenderNodeUtils_CreateVector2Property(x, y);
        maybeThrow(OH_ArkUI_RenderNodeUtils_AttachVector2Property(modifier_, endPointProperty_));
    } else {
        maybeThrow(OH_ArkUI_RenderNodeUtils_SetVector2PropertyValue(endPointProperty_, x, y));
    }
}
void LineGradientRenderNode::createOrUpdateWidthProperty(float width) {
    if (!widthProperty_) {
        widthProperty_ = OH_ArkUI_RenderNodeUtils_CreateFloatProperty(width);
        maybeThrow(OH_ArkUI_RenderNodeUtils_AttachFloatProperty(modifier_, widthProperty_));
    } else {
        maybeThrow(OH_ArkUI_RenderNodeUtils_SetFloatPropertyValue(widthProperty_, width));
    }
}
} // namespace OH