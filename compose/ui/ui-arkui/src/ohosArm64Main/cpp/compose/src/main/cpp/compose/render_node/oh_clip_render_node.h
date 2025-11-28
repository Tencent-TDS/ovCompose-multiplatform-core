#ifndef OH_CLIP_RENDER_NODE_H
#define OH_CLIP_RENDER_NODE_H

#include "oh_base_render_node.h"
#include "../constants/oh_native_enums.h"
#include <native_drawing/drawing_path.h>

namespace OH {

/**
 * 路径裁剪 RenderNode
 * 只支持 Path 类型的裁剪，使用 ContentModifier + onDraw 实现
 * Rect 和 RoundRect 裁剪直接使用 BaseRenderNode + SetClip API
 */
class ClipRenderNode : public BaseRenderNode {
public:
    ~ClipRenderNode() override;
    ClipRenderNode();

    /**
     * 设置路径裁剪
     */
    void setClipPath(OH_Drawing_Path *path, OH_Native_Draw_ClipOp clipOp);

    OH_DrawingNode_Type getType() override;

private:
    void invalidate() override;
    void initModifier() override;

    // 根据路径参数创建路径（用于 onDraw 回调）
    OH_Drawing_Path *createClipPath() const;

    // 成员变量
    OH_Drawing_Path *clipPath_ = nullptr; // 需要复制并管理生命周期
    OH_Native_Draw_ClipOp clipOp_ = OH_Native_Draw_ClipOp::Intersect;

    // ContentModifier 相关
    ArkUI_FloatPropertyHandle invalidateCountProperty_ = nullptr;
    ArkUI_RenderContentModifierHandle modifier_ = nullptr;
};

} // namespace OH

#endif
