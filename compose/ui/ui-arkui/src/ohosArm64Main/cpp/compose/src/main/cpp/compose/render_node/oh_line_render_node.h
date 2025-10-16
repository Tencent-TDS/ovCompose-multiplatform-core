#ifndef OH_LINE_RENDER_NODE_H
#define OH_LINE_RENDER_NODE_H

#include "oh_base_render_node.h"

namespace OH {
class LineRenderNode : public BaseRenderNode {
public:
    ~LineRenderNode();
    LineRenderNode();

    void drawLine(float x1, float y1, float x2, float y2, float lineWidth,
                  uint32_t lineColor, OH_Native_Draw_StrokeCap strokeCap);

private:
    void createOrUpdateStartPointProperty(float x, float y);
    void createOrUpdateEndPointProperty(float x, float y);
    void createOrUpdateWidthProperty(float width);
    void createOrUpdateColorProperty(uint32_t color);
    void initModifier();

    ArkUI_Vector2PropertyHandle startPointProperty_ = nullptr;
    ArkUI_Vector2PropertyHandle endPointProperty_ = nullptr;
    ArkUI_FloatPropertyHandle widthProperty_ = nullptr;
    ArkUI_ColorPropertyHandle colorProperty_ = nullptr;
    ArkUI_RenderContentModifierHandle modifier_ = nullptr;
    OH_Native_Draw_StrokeCap strokeCap;
};
} // namespace OH
#endif