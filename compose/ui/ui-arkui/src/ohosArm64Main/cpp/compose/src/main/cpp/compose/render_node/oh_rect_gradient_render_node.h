#ifndef OH_RECT_GRADIENT_RENDER_NODE_H
#define OH_RECT_GRADIENT_RENDER_NODE_H

#include "../shader/oh_native_linear_gradient_shader.h"
#include "../filter/oh_compose_native_color_filter.h"
#include "oh_base_render_node.h"

namespace OH {
class RectGradientRenderNode : public BaseRenderNode {
public:
    ~RectGradientRenderNode() override;
    RectGradientRenderNode();

    void drawRect(float left, float top, float right, float bottom, float strokeWidth, NativeBasicShader *shader,
                  OH_Native_Draw_PaintingStyle style, OHComposeNativeColorFilter *colorFilter = nullptr);

    OH_DrawingNode_Type getType() override;

private:
    void invalidate() override;
    void initModifier() override;

    // 普通成员变量存储属性值
    float left_ = 0.0f;
    float top_ = 0.0f;
    float right_ = 0.0f;
    float bottom_ = 0.0f;
    float strokeWidth_ = 0.0f;
    NativeBasicShader *shader = nullptr;
    OHComposeNativeColorFilter *colorFilter_ = nullptr;
    OH_Native_Draw_PaintingStyle paintingStyle = OH_Native_Draw_PaintingStyle::Fill;

    // 只保留一个PropertyHandle用于触发onDraw
    ArkUI_FloatPropertyHandle invalidateCountProperty_ = nullptr;
    ArkUI_RenderContentModifierHandle modifier_ = nullptr;
};
} // namespace OH
#endif