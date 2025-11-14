#include <native_drawing/drawing_canvas.h>
#include <native_drawing/drawing_rect.h>
#include <native_drawing/drawing_sampling_options.h>
#include <native_drawing/drawing_pixel_map.h>
#include <multimedia/image_framework/image_mdk_common.h>
#include "oh_image_clip_render_node.h"
#include "../xcomponent_log.h"

#include <cfloat>

namespace OH {
ImageClipRenderNode::~ImageClipRenderNode() {
    if (invalidateCountProperty_) {
        OH_ArkUI_RenderNodeUtils_DisposeFloatProperty(invalidateCountProperty_);
    }
    if (modifier_) {
        OH_ArkUI_RenderNodeUtils_DisposeContentModifier(modifier_);
    }
    // Note: clipOption is automatically disposed by setClip()
    // Note: pixelMap_ is not owned by ImageClipRenderNode, so we don't release it here
}

ImageClipRenderNode::ImageClipRenderNode() {
    this->ImageClipRenderNode::initModifier();
}

OH_DrawingNode_Type ImageClipRenderNode::getType() {
    return OH_DrawingNode_Type::ImageClipNode;
}

void ImageClipRenderNode::drawImageRect(OH_PixelmapNative *pixelMap, int32_t srcX, int32_t srcY, int32_t srcWidth,
                                        int32_t srcHeight, int32_t dstX, int32_t dstY, int32_t dstWidth,
                                        int32_t dstHeight, OH_Native_Draw_FilterQuality filterQuality) {
    LOGI("ImageClipRenderNode::drawImageRect: src=(%{public}d, %{public}d, %{public}d, %{public}d), "
         "dst=(%{public}d, %{public}d, %{public}d, %{public}d)",
         srcX, srcY, srcWidth, srcHeight, dstX, dstY, dstWidth, dstHeight);

    // 更新成员变量（直接使用上层传入的裁剪参数，不做额外计算）
    pixelMap_ = pixelMap;
    srcX_ = srcX;
    srcY_ = srcY;
    srcWidth_ = srcWidth;
    srcHeight_ = srcHeight;
    dstX_ = dstX;
    dstY_ = dstY;
    dstWidth_ = dstWidth;
    dstHeight_ = dstHeight;
    filterQuality_ = filterQuality;

    // 1. 设置 RenderNode 尺寸为目标尺寸（不是原始图像尺寸）
    this->setSize(dstWidth_, dstHeight_);

    // 2. 设置位置为 (0, 0)（作为子节点，相对于父节点）
    this->setPosition(0, 0);

    // 3. 触发 onDraw（在 onDraw 中直接绘制裁剪后的区域到目标尺寸）
    this->invalidate();
}

void ImageClipRenderNode::invalidate() {
    if (!invalidateCountProperty_) {
        return;
    }

    // 读取当前值
    float currentCount = 0.0f;
    OH_ArkUI_RenderNodeUtils_GetFloatPropertyValue(invalidateCountProperty_, &currentCount);

    // 加1，处理溢出（回绕到0）
    const float newCount = (currentCount >= FLT_MAX - 1.0f) ? 0.0f : (currentCount + 1.0f);

    // 设置新值，触发onDraw回调
    OH_ArkUI_RenderNodeUtils_SetFloatPropertyValue(invalidateCountProperty_, newCount);
}

void ImageClipRenderNode::initModifier() {
    if (!modifier_) {
        modifier_ = OH_ArkUI_RenderNodeUtils_CreateContentModifier();
        maybeThrow(OH_ArkUI_RenderNodeUtils_AttachContentModifier(nodeHandle_, modifier_));

        // 创建invalidateCount PropertyHandle
        invalidateCountProperty_ = OH_ArkUI_RenderNodeUtils_CreateFloatProperty(0.0f);
        maybeThrow(OH_ArkUI_RenderNodeUtils_AttachFloatProperty(modifier_, invalidateCountProperty_));

        maybeThrow(OH_ArkUI_RenderNodeUtils_SetContentModifierOnDraw(
            modifier_, this, [](ArkUI_DrawContext *context, void *userData) {
                const auto *data = static_cast<ImageClipRenderNode *>(userData);
                auto *canvas1 = OH_ArkUI_DrawContext_GetCanvas(context);
                auto *canvas = static_cast<OH_Drawing_Canvas *>(canvas1);

                if (data->pixelMap_ == nullptr) {
                    LOGI("ImageClipRenderNode::onDraw: pixelMap is null, skipping");
                    return;
                }

                // 源矩形：裁剪后的区域（已经过居中裁剪计算）
                const float srcLeft = static_cast<float>(data->srcX_);
                const float srcTop = static_cast<float>(data->srcY_);
                const float srcRight = static_cast<float>(data->srcX_ + data->srcWidth_);
                const float srcBottom = static_cast<float>(data->srcY_ + data->srcHeight_);

                // 目标矩形：RenderNode 的整个区域（0, 0, dstWidth_, dstHeight_）
                const float dstLeft = 0.0f;
                const float dstTop = 0.0f;
                const float dstRight = static_cast<float>(data->dstWidth_);
                const float dstBottom = static_cast<float>(data->dstHeight_);

                // 创建源矩形和目标矩形
                OH_Drawing_Rect *srcRect = OH_Drawing_RectCreate(srcLeft, srcTop, srcRight, srcBottom);
                OH_Drawing_Rect *dstRect = OH_Drawing_RectCreate(dstLeft, dstTop, dstRight, dstBottom);

                // 创建SamplingOptions（根据filterQuality）
                OH_Drawing_FilterMode filterMode = OH_Drawing_FilterMode::FILTER_MODE_NEAREST;
                if (data->filterQuality_ == OH_Native_Draw_FilterQuality::Low || data->filterQuality_ == OH_Native_Draw_FilterQuality::Medium || data->filterQuality_ == OH_Native_Draw_FilterQuality::High) {
                    filterMode = OH_Drawing_FilterMode::FILTER_MODE_LINEAR;
                }
                OH_Drawing_SamplingOptions *samplingOptions =
                    OH_Drawing_SamplingOptionsCreate(filterMode, OH_Drawing_MipmapMode::MIPMAP_MODE_NONE);

                // 转换OH_PixelmapNative到OH_Drawing_PixelMap
                OH_Drawing_PixelMap *drawingPixelMap = OH_Drawing_PixelMapGetFromOhPixelMapNative(data->pixelMap_);

                if (drawingPixelMap == nullptr) {
                    LOGE("ImageClipRenderNode::onDraw: failed to convert OH_PixelmapNative to OH_Drawing_PixelMap");
                    OH_Drawing_RectDestroy(srcRect);
                    OH_Drawing_RectDestroy(dstRect);
                    OH_Drawing_SamplingOptionsDestroy(samplingOptions);
                    return;
                }

                // 绘制图像（从裁剪后的源区域到目标矩形，Drawing API 处理缩放）
                LOGI("ImageClipRenderNode::onDraw: drawing cropped region, src=(%{public}f, %{public}f, %{public}f, "
                     "%{public}f), "
                     "dst=(%{public}f, %{public}f, %{public}f, %{public}f)",
                     srcLeft, srcTop, srcRight, srcBottom, dstLeft, dstTop, dstRight, dstBottom);
                OH_Drawing_CanvasDrawPixelMapRect(canvas, drawingPixelMap, srcRect, dstRect, samplingOptions);

                // 释放资源
                OH_Drawing_RectDestroy(srcRect);
                OH_Drawing_RectDestroy(dstRect);
                OH_Drawing_SamplingOptionsDestroy(samplingOptions);
            }));
    }
}
} // namespace OH
