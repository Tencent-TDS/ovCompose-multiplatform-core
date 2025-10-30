#ifndef OH_RECT_GRADIENT_RENDER_NODE_H
#define OH_RECT_GRADIENT_RENDER_NODE_H

#include "../shader/oh_native_linear_gradient_shader.h"
#include "oh_base_render_node.h"

namespace OH {
class RectGradientRenderNode : public BaseRenderNode {
public:
    ~RectGradientRenderNode() override;
    RectGradientRenderNode();

    void drawRect(float left, float top, float right, float bottom, float strokeWidth, NativeBasicShader *shader,
                  OH_Native_Draw_PaintingStyle style);

    OH_DrawingNode_Type getType() override;

private:
    void createOrUpdateLeftTopPosProperty(float left, float top);
    void createOrUpdateRightBottomPosProperty(float right, float bottom);
    void createOrUpdateStrokeWidthProperty(float strokeWidth);
    void initModifier() override;

    ArkUI_Vector2PropertyHandle leftTopPosProperty_ = nullptr;
    ArkUI_Vector2PropertyHandle rightBottomProperty_ = nullptr;
    ArkUI_FloatPropertyHandle strokeWidthProperty_ = nullptr;
    ArkUI_RenderContentModifierHandle modifier_ = nullptr;
    NativeBasicShader *shader = nullptr;
    OH_Native_Draw_PaintingStyle paintingStyle;
};
} // namespace OH
#endif