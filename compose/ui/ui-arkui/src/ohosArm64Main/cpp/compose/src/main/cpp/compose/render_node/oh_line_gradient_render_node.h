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
    void invalidate();
    void initModifier() override;

    // 普通成员变量存储属性值
    float x1_ = 0.0f;
    float y1_ = 0.0f;
    float x2_ = 0.0f;
    float y2_ = 0.0f;
    float lineWidth_ = 0.0f;
    NativeBasicShader *shader = nullptr;
    OH_Native_Draw_StrokeCap strokeCap = OH_Native_Draw_StrokeCap::StrokeCapButt;

    // 只保留一个PropertyHandle用于触发onDraw
    ArkUI_FloatPropertyHandle invalidateCountProperty_ = nullptr;
    ArkUI_RenderContentModifierHandle modifier_ = nullptr;
};
} // namespace OH
#endif