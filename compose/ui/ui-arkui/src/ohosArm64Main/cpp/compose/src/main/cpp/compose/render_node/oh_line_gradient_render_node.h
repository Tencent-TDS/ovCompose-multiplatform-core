#ifndef OH_LINE_GRADIENT_RENDER_NODE_H
#define OH_LINE_GRADIENT_RENDER_NODE_H

#include "../shader/oh_native_linear_gradient_shader.h"
#include "oh_base_render_node.h"

namespace OH {
class LineGradientRenderNode : public BaseRenderNode {
public:
    ~LineGradientRenderNode() override;
    LineGradientRenderNode();

    void drawLine(float x1, float y1, float x2, float y2, float lineWidth, NativeLinearGradientShader *shader,
                  OH_Native_Draw_StrokeCap stokeCap);

    OH_DrawingNode_Type getType() override;

private:
    void createOrUpdateStartPointProperty(float x, float y);
    void createOrUpdateEndPointProperty(float x, float y);
    void createOrUpdateWidthProperty(float width);
    void initModifier() override;

    ArkUI_Vector2PropertyHandle startPointProperty_ = nullptr;
    ArkUI_Vector2PropertyHandle endPointProperty_ = nullptr;
    ArkUI_FloatPropertyHandle widthProperty_ = nullptr;
    ArkUI_RenderContentModifierHandle modifier_ = nullptr;
    NativeBasicShader *shader = nullptr;
    OH_Native_Draw_StrokeCap strokeCap;
};
} // namespace OH
#endif