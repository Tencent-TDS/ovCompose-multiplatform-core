#ifndef OH_POINTS_RENDER_NODE_H
#define OH_POINTS_RENDER_NODE_H

#include "oh_base_render_node.h"
#include "../paint/oh_compose_native_paint.h"
#include <native_drawing/drawing_canvas.h>
#include <vector>

namespace OH {
class PointsRenderNode : public BaseRenderNode {
public:
    ~PointsRenderNode() override;
    PointsRenderNode();

    void drawPoints(OH_Drawing_PointMode pointMode, const float *points, size_t pointCount,
                    OHComposeNativePaint *paint);

    OH_DrawingNode_Type getType() override;

private:
    void invalidate();
    void initModifier() override;

    // 普通成员变量存储属性值
    OH_Drawing_PointMode pointMode_ = OH_Drawing_PointMode::POINT_MODE_POINTS;
    std::vector<float> points_;                                                  // 存储点数据 [x1, y1, x2, y2, ...]
    OHComposeNativePaint *paint_ = nullptr; // 存储 paint 信息（不拥有所有权）

    // 只保留一个PropertyHandle用于触发onDraw
    ArkUI_FloatPropertyHandle invalidateCountProperty_ = nullptr;
    ArkUI_RenderContentModifierHandle modifier_ = nullptr;
};
} // namespace OH
#endif
