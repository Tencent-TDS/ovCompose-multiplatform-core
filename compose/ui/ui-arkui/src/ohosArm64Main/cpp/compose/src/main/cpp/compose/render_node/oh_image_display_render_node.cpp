#include <native_drawing/drawing_canvas.h>
#include <native_drawing/drawing_rect.h>
#include <native_drawing/drawing_sampling_options.h>
#include <native_drawing/drawing_pixel_map.h>
#include <multimedia/image_framework/image/pixelmap_native.h>
#include <multimedia/image_framework/image_mdk_common.h>
#include "oh_image_display_render_node.h"
#include "../xcomponent_log.h"

#include <cfloat>

namespace OH {

//// 裁剪信息结构体
struct ImageClipInfo {
    bool needsClip;
    int32_t finalSrcX;
    int32_t finalSrcY;
    int32_t finalSrcWidth;
    int32_t finalSrcHeight;
};

// 合并的裁剪信息计算函数（高效版本）
static ImageClipInfo calculateClipInfo(OH_PixelmapNative *pixelMap, int32_t srcX, int32_t srcY, int32_t srcWidth,
                                       int32_t srcHeight) {
    ImageClipInfo info = {false, srcX, srcY, srcWidth, srcHeight};

    // 快速路径：检查 srcOffset
    if (srcX != 0 || srcY != 0) {
        info.needsClip = true;
        return info;
    }

    // 获取原始图像尺寸（一次性）
    OH_Pixelmap_ImageInfo *imageInfo = nullptr;
    if (OH_PixelmapImageInfo_Create(&imageInfo) != IMAGE_RESULT_SUCCESS || !imageInfo) {
        return info;
    }

    uint32_t imageWidth = 0;
    uint32_t imageHeight = 0;
    if (OH_PixelmapNative_GetImageInfo(pixelMap, imageInfo) == IMAGE_RESULT_SUCCESS) {
        OH_PixelmapImageInfo_GetWidth(imageInfo, &imageWidth);
        OH_PixelmapImageInfo_GetHeight(imageInfo, &imageHeight);
    }
    OH_PixelmapImageInfo_Release(imageInfo);

    // 检查是否裁剪了原图
    if (srcWidth < static_cast<int32_t>(imageWidth) || srcHeight < static_cast<int32_t>(imageHeight)) {
        info.needsClip = true;
        return info;
    }

    return info;
}

ImageDisplayRenderNode::~ImageDisplayRenderNode() {
    if (invalidateCountProperty_) {
        OH_ArkUI_RenderNodeUtils_DisposeFloatProperty(invalidateCountProperty_);
    }
    if (modifier_) {
        OH_ArkUI_RenderNodeUtils_DisposeContentModifier(modifier_);
    }
    // imageClipNode_ 会自动释放（unique_ptr）
    // Note: pixelMap_ is not owned by ImageDisplayRenderNode, so we don't release it here
}

ImageDisplayRenderNode::ImageDisplayRenderNode() {
    this->ImageDisplayRenderNode::initModifier();
}

OH_DrawingNode_Type ImageDisplayRenderNode::getType() {
    return OH_DrawingNode_Type::ImageDisplayNode;
}

void ImageDisplayRenderNode::drawImageRect(OH_PixelmapNative *pixelMap, int32_t srcX, int32_t srcY, int32_t srcWidth,
                                           int32_t srcHeight, int32_t dstX, int32_t dstY, int32_t dstWidth,
                                           int32_t dstHeight, OH_Native_Draw_FilterQuality filterQuality) {
    // 注意：参数变化检测已由上层 OHNativeCanvasProxy + PictureRecorder 通过 hash 机制完成
    // 只有参数变化时才会调用此函数，因此无需在此层重复检测

    // ========== 计算裁剪信息（合并 shouldClipImage + 宽高比调整）==========
    auto clipInfo = calculateClipInfo(pixelMap, srcX, srcY, srcWidth, srcHeight);

    // ========== 更新成员变量（存储原始输入参数）==========
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

    // ========== 根据裁剪需求选择渲染路径 ==========
    if (!clipInfo.needsClip) {
        // 路径A: 不需要裁剪，直接绘制
        if (imageClipNode_) {
            this->removeChild(imageClipNode_.get());
        }

        this->setSize(dstWidth, dstHeight);
        this->invalidate();

    } else {
        // 路径B: 需要裁剪，使用 ImageClipRenderNode
        this->setSize(dstWidth, dstHeight);

        if (!imageClipNode_) {
            imageClipNode_ = std::make_unique<ImageClipRenderNode>();
        }

        if (imageClipNode_->getParent() != this) {
            this->addChild(imageClipNode_.get());
        }

        imageClipNode_->drawImageRect(pixelMap, clipInfo.finalSrcX, clipInfo.finalSrcY, clipInfo.finalSrcWidth,
                                      clipInfo.finalSrcHeight, 0, 0, dstWidth, dstHeight, filterQuality);
    }
}

void ImageDisplayRenderNode::invalidate() {
    if (!invalidateCountProperty_) {
        return;
    }

    // 读取当前值
    float currentCount = 0.0f;
    OH_ArkUI_RenderNodeUtils_GetFloatPropertyValue(invalidateCountProperty_, &currentCount);

    // 加1，处理溢出（回绕到0）
    float newCount = (currentCount >= FLT_MAX - 1.0f) ? 0.0f : (currentCount + 1.0f);

    // 设置新值，触发onDraw回调
    OH_ArkUI_RenderNodeUtils_SetFloatPropertyValue(invalidateCountProperty_, newCount);
}

void ImageDisplayRenderNode::initModifier() {
    if (!modifier_) {
        modifier_ = OH_ArkUI_RenderNodeUtils_CreateContentModifier();
        maybeThrow(OH_ArkUI_RenderNodeUtils_AttachContentModifier(nodeHandle_, modifier_));

        // 创建invalidateCount PropertyHandle
        invalidateCountProperty_ = OH_ArkUI_RenderNodeUtils_CreateFloatProperty(0.0f);
        maybeThrow(OH_ArkUI_RenderNodeUtils_AttachFloatProperty(modifier_, invalidateCountProperty_));

        maybeThrow(OH_ArkUI_RenderNodeUtils_SetContentModifierOnDraw(
            modifier_, this, [](ArkUI_DrawContext *context, void *userData) {
                const auto *data = static_cast<ImageDisplayRenderNode *>(userData);
                auto *canvas1 = OH_ArkUI_DrawContext_GetCanvas(context);
                auto *canvas = static_cast<OH_Drawing_Canvas *>(canvas1);

                if (data->pixelMap_ == nullptr) {
                    LOGI("ImageDisplayRenderNode::onDraw: pixelMap is null, skipping");
                    return;
                }

                // 源矩形：从原图裁剪的区域
                const float srcLeft = static_cast<float>(data->srcX_);
                const float srcTop = static_cast<float>(data->srcY_);
                const float srcRight = static_cast<float>(data->srcX_ + data->srcWidth_);
                const float srcBottom = static_cast<float>(data->srcY_ + data->srcHeight_);

                // 目标矩形：绘制到 RenderNode 的整个区域（局部坐标）
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
                    LOGE("ImageDisplayRenderNode::onDraw: failed to convert OH_PixelmapNative to OH_Drawing_PixelMap");
                    OH_Drawing_RectDestroy(srcRect);
                    OH_Drawing_RectDestroy(dstRect);
                    OH_Drawing_SamplingOptionsDestroy(samplingOptions);
                    return;
                }

                // 绘制图像
                OH_Drawing_CanvasDrawPixelMapRect(canvas, drawingPixelMap, srcRect, dstRect, samplingOptions);

                // 释放资源
                OH_Drawing_RectDestroy(srcRect);
                OH_Drawing_RectDestroy(dstRect);
                OH_Drawing_SamplingOptionsDestroy(samplingOptions);
            }));
    }
}
} // namespace OH
