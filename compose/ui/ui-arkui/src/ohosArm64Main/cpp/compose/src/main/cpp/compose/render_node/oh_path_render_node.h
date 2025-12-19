#ifndef OH_PATH_RENDER_NODE_H
#define OH_PATH_RENDER_NODE_H

#include "oh_base_render_node.h"
#include "../constants/oh_native_enums.h"
#include "../filter/oh_compose_native_color_filter.h"
#include <native_drawing/drawing_types.h>

namespace OH {
class PathRenderNode : public BaseRenderNode {
public:
    ~PathRenderNode() override;
    PathRenderNode();

    void drawPath(OH_Drawing_Path *path, float strokeWidth, uint32_t color, OH_Native_Draw_PaintingStyle style,
                  OHComposeNativeColorFilter *colorFilter = nullptr);

    OH_DrawingNode_Type getType() override;

private:
    void invalidate() override;
    void initModifier() override;

    // 普通成员变量存储属性值
    OH_Drawing_Path *pathProperty_ = nullptr;
    float strokeWidth_ = 0.0f;
    uint32_t color_ = 0;
    OHComposeNativeColorFilter *colorFilter_ = nullptr;
    OH_Native_Draw_PaintingStyle paintingStyle = OH_Native_Draw_PaintingStyle::Fill;
    // Path bounds的左上角坐标（用于将Path平移到相对坐标）
    float pathOffsetX_ = 0.0f;
    float pathOffsetY_ = 0.0f;

    // 只保留一个PropertyHandle用于触发onDraw
    ArkUI_FloatPropertyHandle invalidateCountProperty_ = nullptr;
    ArkUI_RenderContentModifierHandle modifier_ = nullptr;
};
} // namespace OH
#endif
