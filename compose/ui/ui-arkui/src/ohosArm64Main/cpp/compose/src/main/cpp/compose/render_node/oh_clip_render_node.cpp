#include "oh_clip_render_node.h"
#include <native_drawing/drawing_canvas.h>
#include <native_drawing/drawing_rect.h>
#include <cfloat>
#include "../xcomponent_log.h"
#include "../trace/oh_systrace_section.h"
#include "../path/oh_native_path_export.h"

namespace OH {

ClipRenderNode::~ClipRenderNode() {
    // 释放复制的路径
    if (clipPath_) {
        OH_Drawing_PathDestroy(clipPath_);
        clipPath_ = nullptr;
    }

    if (invalidateCountProperty_) {
        OH_ArkUI_RenderNodeUtils_DisposeFloatProperty(invalidateCountProperty_);
    }
    if (modifier_) {
        OH_ArkUI_RenderNodeUtils_DisposeContentModifier(modifier_);
    }
}

ClipRenderNode::ClipRenderNode() {
    this->ClipRenderNode::initModifier();
}

OH_DrawingNode_Type ClipRenderNode::getType() {
    return OH_DrawingNode_Type::ClipNode;
}

void ClipRenderNode::setClipPath(OH_Drawing_Path *path, OH_Native_Draw_ClipOp clipOp) {
    OH::SystraceSection trace("ClipRenderNode:setClipPath");

    // 如果之前有路径，需要先释放旧路径
    if (clipPath_) {
        OH_Drawing_PathDestroy(clipPath_);
        clipPath_ = nullptr;
    }

    // 复制新路径并转换为相对坐标
    if (path) {
        clipPath_ = OH_Drawing_PathCopy(path);

        // 计算Path的bounds，并将Path平移到相对坐标
        // ClipRenderNode的position会设置为(left, top)
        // 因此需要将Path平移到(-left, -top)，这样Path就会相对于RenderNode的(0,0)点
        // float left = 0.0f, top = 0.0f, right = 0.0f, bottom = 0.0f;
        // OHPath_getBounds(clipPath_, &left, &top, &right, &bottom);

        // 将Path平移到相对坐标（相对于bounds的左上角）
        // 注意：这里平移的是-left和-top，使得Path的bounds从(0, 0)开始
        // OHPath_translate(clipPath_, -left, -top);
    }

    clipOp_ = clipOp;

    invalidate();
}

OH_Drawing_Path *ClipRenderNode::createClipPath() const {
    // 创建路径副本用于 onDraw 回调
    OH_Drawing_Path *path = OH_Drawing_PathCreate();

    if (clipPath_) {
        OH_Drawing_PathSetPath(path, clipPath_);
    }

    return path;
}

void ClipRenderNode::invalidate() {
    // 触发 ContentModifier 的 invalidate
    if (invalidateCountProperty_) {
        float currentCount = 0.0f;
        OH_ArkUI_RenderNodeUtils_GetFloatPropertyValue(invalidateCountProperty_, &currentCount);

        float newCount = (currentCount >= FLT_MAX - 1.0f) ? 0.0f : (currentCount + 1.0f);
        OH_ArkUI_RenderNodeUtils_SetFloatPropertyValue(invalidateCountProperty_, newCount);
    }
}

void ClipRenderNode::initModifier() {
    // 初始化 ContentModifier（用于 Path 裁剪）
    if (!modifier_) {
        modifier_ = OH_ArkUI_RenderNodeUtils_CreateContentModifier();
        maybeThrow(OH_ArkUI_RenderNodeUtils_AttachContentModifier(nodeHandle_, modifier_));

        // 创建invalidateCount PropertyHandle
        invalidateCountProperty_ = OH_ArkUI_RenderNodeUtils_CreateFloatProperty(0.0f);
        maybeThrow(OH_ArkUI_RenderNodeUtils_AttachFloatProperty(modifier_, invalidateCountProperty_));

        maybeThrow(OH_ArkUI_RenderNodeUtils_SetContentModifierOnDraw(
            modifier_, this, [](ArkUI_DrawContext *context, void *userData) {
                OH::SystraceSection trace("ClipRenderNode:onDraw");

                const auto *node = static_cast<ClipRenderNode *>(userData);

                if (!node->clipPath_) {
                    LOGI("ClipRenderNode::onDraw: No clip path set, RenderNode: %{public}p", userData);
                    return;
                }

                auto *canvas = static_cast<OH_Drawing_Canvas *>(OH_ArkUI_DrawContext_GetCanvas(context));

                if (!canvas) {
                    LOGE("ClipRenderNode::onDraw: canvas is null");
                    return;
                }

                // 根据 Path 参数创建路径
                OH_Drawing_Path *clipPath = node->createClipPath();
                if (!clipPath) {
                    LOGE("ClipRenderNode::onDraw: failed to create clip path");
                    return;
                }

                // 转换 ClipOp 枚举
                OH_Drawing_CanvasClipOp nativeClipOp =
                    (node->clipOp_ == OH_Native_Draw_ClipOp::Intersect) ? OH_Drawing_CanvasClipOp::INTERSECT : OH_Drawing_CanvasClipOp::DIFFERENCE;

                // 执行裁剪
                OH_Drawing_CanvasClipPath(canvas, clipPath, nativeClipOp, true);

                // 清理路径
                OH_Drawing_PathDestroy(clipPath);

                LOGI("ClipRenderNode::onDraw: Applied Path clip, clipNode=%{public}p", node);
            }));
    }
}

} // namespace OH
