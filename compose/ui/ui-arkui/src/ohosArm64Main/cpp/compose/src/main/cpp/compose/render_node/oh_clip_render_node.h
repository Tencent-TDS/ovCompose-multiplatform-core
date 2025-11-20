#ifndef OH_CLIP_RENDER_NODE_H
#define OH_CLIP_RENDER_NODE_H

#include "oh_base_render_node.h"
#include "../constants/oh_native_enums.h"
#include <native_drawing/drawing_path.h>
#include <variant>

namespace OH {

// 裁剪类型枚举
enum class ClipType {
    Rect,
    RoundRect,
    Path
};

// 矩形裁剪参数
struct ClipRectParams {
    float left;
    float top;
    float right;
    float bottom;
};

// 圆角矩形裁剪参数
struct ClipRoundRectParams {
    float left;
    float top;
    float right;
    float bottom;
    float radiusX;
    float radiusY;
};

// 路径裁剪参数
struct ClipPathParams {
    OH_Drawing_Path *path; // 需要复制并管理生命周期
};

// 使用 variant 存储不同类型的裁剪参数
using ClipParams = std::variant<ClipRectParams, ClipRoundRectParams, ClipPathParams>;

/**
 * 统一的裁剪 RenderNode
 * 支持 Rect、RoundRect、Path 三种裁剪类型
 */
class ClipRenderNode : public BaseRenderNode {
public:
    ~ClipRenderNode() override;
    ClipRenderNode();

    /**
     * 设置矩形裁剪
     */
    void setClipRect(float left, float top, float right, float bottom, OH_Native_Draw_ClipOp clipOp);

    /**
     * 设置圆角矩形裁剪
     */
    void setClipRoundRect(float left, float top, float right, float bottom,
                          float radiusX, float radiusY, OH_Native_Draw_ClipOp clipOp);

    /**
     * 设置路径裁剪
     */
    void setClipPath(OH_Drawing_Path *path, OH_Native_Draw_ClipOp clipOp);

    OH_DrawingNode_Type getType() override;

private:
    void invalidate();
    void initModifier() override;

    // 根据裁剪类型和参数创建路径
    OH_Drawing_Path *createClipPath() const;

    // 成员变量
    ClipType clipType_ = ClipType::Rect;
    ClipParams clipParams_;
    OH_Native_Draw_ClipOp clipOp_ = OH_Native_Draw_ClipOp::Intersect;

    // ContentModifier 相关
    ArkUI_FloatPropertyHandle invalidateCountProperty_ = nullptr;
    ArkUI_RenderContentModifierHandle modifier_ = nullptr;
};

} // namespace OH

#endif

