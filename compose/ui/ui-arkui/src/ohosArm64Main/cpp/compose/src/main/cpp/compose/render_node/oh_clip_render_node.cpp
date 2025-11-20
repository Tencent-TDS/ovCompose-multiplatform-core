#include "oh_clip_render_node.h"
#include <native_drawing/drawing_canvas.h>
#include <native_drawing/drawing_rect.h>
#include <native_drawing/drawing_round_rect.h>
#include <cfloat>
#include "../xcomponent_log.h"
#include "../trace/oh_systrace_section.h"
#include "../path/oh_native_path_export.h"

namespace OH {

ClipRenderNode::~ClipRenderNode() {
    // 如果当前是 Path 类型，需要释放复制的路径
    if (clipType_ == ClipType::Path) {
        if (auto *params = std::get_if<ClipPathParams>(&clipParams_)) {
            if (params->path) {
                OH_Drawing_PathDestroy(params->path);
            }
        }
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

void ClipRenderNode::setClipRect(float left, float top, float right, float bottom,
                                 OH_Native_Draw_ClipOp clipOp) {
    OH::SystraceSection trace("ClipRenderNode:setClipRect");

    // 如果之前是 Path 类型，需要先释放
    if (clipType_ == ClipType::Path) {
        if (auto *params = std::get_if<ClipPathParams>(&clipParams_)) {
            if (params->path) {
                OH_Drawing_PathDestroy(params->path);
            }
        }
    }

    clipType_ = ClipType::Rect;
    clipParams_ = ClipRectParams{left, top, right, bottom};
    clipOp_ = clipOp;

    invalidate();
}

void ClipRenderNode::setClipRoundRect(float left, float top, float right, float bottom,
                                      float radiusX, float radiusY,
                                      OH_Native_Draw_ClipOp clipOp) {
    OH::SystraceSection trace("ClipRenderNode:setClipRoundRect");

    // 如果之前是 Path 类型，需要先释放
    if (clipType_ == ClipType::Path) {
        if (auto *params = std::get_if<ClipPathParams>(&clipParams_)) {
            if (params->path) {
                OH_Drawing_PathDestroy(params->path);
            }
        }
    }

    clipType_ = ClipType::RoundRect;
    clipParams_ = ClipRoundRectParams{left, top, right, bottom, radiusX, radiusY};
    clipOp_ = clipOp;

    invalidate();
}

void ClipRenderNode::setClipPath(OH_Drawing_Path *path, OH_Native_Draw_ClipOp clipOp) {
    OH::SystraceSection trace("ClipRenderNode:setClipPath");

    // 如果之前是 Path 类型，需要先释放旧路径
    if (clipType_ == ClipType::Path) {
        if (auto *params = std::get_if<ClipPathParams>(&clipParams_)) {
            if (params->path) {
                OH_Drawing_PathDestroy(params->path);
            }
        }
    }

    // 复制新路径并转换为相对坐标
    OH_Drawing_Path *copiedPath = nullptr;
    if (path) {
        copiedPath = OH_Drawing_PathCopy(path);

        // 计算Path的bounds，并将Path平移到相对坐标
        // ClipRenderNode的position会设置为(left, top)
        // 因此需要将Path平移到(-left, -top)，这样Path就会相对于RenderNode的(0,0)点
        float left = 0.0f, top = 0.0f, right = 0.0f, bottom = 0.0f;
        OHPath_getBounds(copiedPath, &left, &top, &right, &bottom);

        // 将Path平移到相对坐标（相对于bounds的左上角）
        // 注意：这里平移的是-left和-top，使得Path的bounds从(0, 0)开始
        OHPath_translate(copiedPath, -left, -top);
    }

    clipType_ = ClipType::Path;
    clipParams_ = ClipPathParams{copiedPath};
    clipOp_ = clipOp;

    invalidate();
}

OH_Drawing_Path *ClipRenderNode::createClipPath() const {
    OH_Drawing_Path *path = OH_Drawing_PathCreate();

    switch (clipType_) {
    case ClipType::Rect: {
        if (auto *params = std::get_if<ClipRectParams>(&clipParams_)) {
            OH_Drawing_PathAddRect(path, params->left, params->top, params->right, params->bottom, PATH_DIRECTION_CW);
        }
        break;
    }

    case ClipType::RoundRect: {
        if (auto *params = std::get_if<ClipRoundRectParams>(&clipParams_)) {
            OH_Drawing_Rect *rect = OH_Drawing_RectCreate(
                params->left, params->top, params->right, params->bottom);
            OH_Drawing_RoundRect *roundRect = OH_Drawing_RoundRectCreate(
                rect, params->radiusX, params->radiusY);
            OH_Drawing_PathAddRoundRect(path, roundRect, PATH_DIRECTION_CW);
            OH_Drawing_RoundRectDestroy(roundRect);
            OH_Drawing_RectDestroy(rect);
        }
        break;
    }

    case ClipType::Path: {
        if (auto *params = std::get_if<ClipPathParams>(&clipParams_)) {
            if (params->path) {
                OH_Drawing_PathSetPath(path, params->path);
            }
        }
        break;
    }
    }

    return path;
}

void ClipRenderNode::invalidate() {
    if (!invalidateCountProperty_) {
        return;
    }

    float currentCount = 0.0f;
    OH_ArkUI_RenderNodeUtils_GetFloatPropertyValue(invalidateCountProperty_, &currentCount);

    float newCount = (currentCount >= FLT_MAX - 1.0f) ? 0.0f : (currentCount + 1.0f);
    OH_ArkUI_RenderNodeUtils_SetFloatPropertyValue(invalidateCountProperty_, newCount);
}

void ClipRenderNode::initModifier() {
    if (!modifier_) {
        modifier_ = OH_ArkUI_RenderNodeUtils_CreateContentModifier();
        maybeThrow(OH_ArkUI_RenderNodeUtils_AttachContentModifier(nodeHandle_, modifier_));

        invalidateCountProperty_ = OH_ArkUI_RenderNodeUtils_CreateFloatProperty(0.0f);
        maybeThrow(OH_ArkUI_RenderNodeUtils_AttachFloatProperty(modifier_, invalidateCountProperty_));

        maybeThrow(OH_ArkUI_RenderNodeUtils_SetContentModifierOnDraw(
            modifier_, this, [](ArkUI_DrawContext *context, void *userData) {
                OH::SystraceSection trace("ClipRenderNode:onDraw");

                const auto *node = static_cast<ClipRenderNode *>(userData);
                auto *canvas = static_cast<OH_Drawing_Canvas *>(
                    OH_ArkUI_DrawContext_GetCanvas(context));

                if (!canvas) {
                    LOGE("ClipRenderNode::onDraw: canvas is null");
                    return;
                }

                // 根据裁剪类型和参数创建路径
                OH_Drawing_Path *clipPath = node->createClipPath();
                if (!clipPath) {
                    LOGE("ClipRenderNode::onDraw: failed to create clip path");
                    return;
                }

                // 获取路径的 bounds 用于诊断
                float pathLeft = 0.0f, pathTop = 0.0f, pathRight = 0.0f, pathBottom = 0.0f;
                OH_Drawing_Rect *boundsRect = OH_Drawing_RectCreate(0.0f, 0.0f, 0.0f, 0.0f);
                if (boundsRect != nullptr) {
                    OH_Drawing_PathGetBounds(clipPath, boundsRect);
                    pathLeft = OH_Drawing_RectGetLeft(boundsRect);
                    pathTop = OH_Drawing_RectGetTop(boundsRect);
                    pathRight = OH_Drawing_RectGetRight(boundsRect);
                    pathBottom = OH_Drawing_RectGetBottom(boundsRect);
                    OH_Drawing_RectDestroy(boundsRect);
                }

                // 转换 ClipOp 枚举
                OH_Drawing_CanvasClipOp nativeClipOp =
                    (node->clipOp_ == OH_Native_Draw_ClipOp::Intersect) ? OH_Drawing_CanvasClipOp::INTERSECT : OH_Drawing_CanvasClipOp::DIFFERENCE;

                // 执行裁剪
                OH_Drawing_CanvasClipPath(canvas, clipPath, nativeClipOp, true);

                // 清理路径
                OH_Drawing_PathDestroy(clipPath);

                LOGI("ClipRenderNode::onDraw: Applied clip, type=%{public}d, clipOp=%{public}d, pathBounds=(%f,%f,%f,%f)",
                     static_cast<int>(node->clipType_), node->clipOp_, pathLeft, pathTop, pathRight, pathBottom);
            }));
    }
}

} // namespace OH
