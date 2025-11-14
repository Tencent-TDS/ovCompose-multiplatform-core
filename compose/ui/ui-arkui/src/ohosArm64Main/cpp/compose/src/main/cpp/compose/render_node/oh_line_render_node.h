#ifndef OH_LINE_RENDER_NODE_H
#define OH_LINE_RENDER_NODE_H

#include "oh_base_render_node.h"

namespace OH {
class LineRenderNode : public BaseRenderNode {
public:
    ~LineRenderNode() override;
    LineRenderNode();

    void drawLine(float x1, float y1, float x2, float y2, float lineWidth, uint32_t lineColor,
                  OH_Native_Draw_StrokeCap strokeCap);

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
    uint32_t lineColor_ = 0;
    OH_Native_Draw_StrokeCap strokeCap = OH_Native_Draw_StrokeCap::StrokeCapButt;

    // 只保留一个PropertyHandle用于触发onDraw
    ArkUI_FloatPropertyHandle invalidateCountProperty_ = nullptr;
    ArkUI_RenderContentModifierHandle modifier_ = nullptr;
};
} // namespace OH
#endif