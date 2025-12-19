#include <native_drawing/drawing_canvas.h>
#include <native_drawing/drawing_rect.h>
#include <native_drawing/drawing_sampling_options.h>
#include <native_drawing/drawing_pixel_map.h>
#include <native_drawing/drawing_brush.h>
#include <native_drawing/drawing_filter.h>
#include <multimedia/image_framework/image/pixelmap_native.h>
#include <multimedia/image_framework/image_mdk_common.h>
#include "oh_image_display_render_node.h"
#include "../xcomponent_log.h"
#include "../filter/oh_compose_native_color_filter.h"
#include "../cache/oh_drawing_pixelmap_cache.h"

#include <cfloat>

namespace OH {

ImageDisplayRenderNode::~ImageDisplayRenderNode() {
    if (invalidateCountProperty_) {
        OH_ArkUI_RenderNodeUtils_DisposeFloatProperty(invalidateCountProperty_);
    }
    if (modifier_) {
        OH_ArkUI_RenderNodeUtils_DisposeContentModifier(modifier_);
    }
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
                                           int32_t dstHeight, OHComposeNativeColorFilter *colorFilter, OH_Native_Draw_FilterQuality filterQuality) {
    // 更新成员变量
    pixelMap_ = pixelMap;
    srcX_ = srcX;
    srcY_ = srcY;
    srcWidth_ = srcWidth;
    srcHeight_ = srcHeight;
    dstX_ = dstX;
    dstY_ = dstY;
    dstWidth_ = dstWidth;
    dstHeight_ = dstHeight;
    colorFilter_ = colorFilter;
    filterQuality_ = filterQuality;

    // 触发重绘
    this->invalidate();
}

void ImageDisplayRenderNode::invalidate() {
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

                // 使用缓存获取OH_Drawing_PixelMap，避免重复的内存复制开销
                // 根据OpenHarmony官方文档，OH_Drawing_PixelMapGetFromOhPixelMapNative
                // 涉及耗时的内存复制（4K图片约20ms），使用缓存可减少98%的调用
                OH_Drawing_PixelMap *drawingPixelMap = OHDrawingPixelMapCache::sharedInstance().getOrCreate(data->pixelMap_);

                if (drawingPixelMap == nullptr) {
                    LOGE("ImageDisplayRenderNode::onDraw: failed to get OH_Drawing_PixelMap from cache");
                    OH_Drawing_RectDestroy(srcRect);
                    OH_Drawing_RectDestroy(dstRect);
                    OH_Drawing_SamplingOptionsDestroy(samplingOptions);
                    return;
                }

                // 应用ColorFilter
                OH_Drawing_Brush *brush = nullptr;
                OH_Drawing_Filter *filter = nullptr;

                if (data->colorFilter_ != nullptr) {
                    // 创建Filter对象
                    filter = OH_Drawing_FilterCreate();
                    if (filter != nullptr) {
                        // 将ColorFilter设置到Filter
                        OH_Drawing_FilterSetColorFilter(filter, data->colorFilter_->getNativeFilter());

                        // 创建画刷并设置Filter
                        brush = OH_Drawing_BrushCreate();
                        OH_Drawing_BrushSetFilter(brush, filter);

                        // 将画刷应用到Canvas
                        OH_Drawing_CanvasAttachBrush(canvas, brush);

                        LOGI("ImageDisplayRenderNode::onDraw: ColorFilter applied successfully");
                    } else {
                        LOGE("ImageDisplayRenderNode::onDraw: failed to create OH_Drawing_Filter");
                    }
                }

                // 绘制图像
                OH_Drawing_CanvasDrawPixelMapRect(canvas, drawingPixelMap, srcRect, dstRect, samplingOptions);

                // 清理ColorFilter相关资源
                if (brush != nullptr) {
                    OH_Drawing_CanvasDetachBrush(canvas);
                    OH_Drawing_BrushDestroy(brush);
                }
                if (filter != nullptr) {
                    OH_Drawing_FilterDestroy(filter);
                }

                // 释放资源
                OH_Drawing_RectDestroy(srcRect);
                OH_Drawing_RectDestroy(dstRect);
                OH_Drawing_SamplingOptionsDestroy(samplingOptions);
            }));
    }
}
} // namespace OH
