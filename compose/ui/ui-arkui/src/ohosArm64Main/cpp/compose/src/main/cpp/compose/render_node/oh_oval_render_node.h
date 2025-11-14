#ifndef OH_OVAL_RENDER_NODE_H
#define OH_OVAL_RENDER_NODE_H

#include "oh_base_render_node.h"

namespace OH {
class OvalRenderNode : public BaseRenderNode {
public:
    ~OvalRenderNode() override;
    OvalRenderNode();

    void drawOval(float left, float top, float right, float bottom, float strokeWidth, uint32_t color,
                  OH_Native_Draw_PaintingStyle style);

    OH_DrawingNode_Type getType() override;

private:
    void invalidate();
    void initModifier() override;

    // 普通成员变量存储属性值
    float left_ = 0.0f;
    float top_ = 0.0f;
    float right_ = 0.0f;
    float bottom_ = 0.0f;
    float strokeWidth_ = 0.0f;
    uint32_t color_ = 0;
    OH_Native_Draw_PaintingStyle paintingStyle = OH_Native_Draw_PaintingStyle::Fill;

    // 只保留一个PropertyHandle用于触发onDraw
    ArkUI_FloatPropertyHandle invalidateCountProperty_ = nullptr;
    ArkUI_RenderContentModifierHandle modifier_ = nullptr;
};
} // namespace OH
#endif
