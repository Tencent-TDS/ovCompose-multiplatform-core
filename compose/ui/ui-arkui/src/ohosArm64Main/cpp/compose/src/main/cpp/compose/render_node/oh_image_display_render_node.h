#ifndef OH_IMAGE_DISPLAY_RENDER_NODE_H
#define OH_IMAGE_DISPLAY_RENDER_NODE_H

#include "oh_base_render_node.h"
#include "oh_image_clip_render_node.h"
#include <multimedia/image_framework/image/pixelmap_native.h>
#include <memory>

namespace OH {
/**
 * ImageDisplayRenderNode - 图片显示渲染节点
 * 参考 iOS TMMImageDisplayLayer 实现
 *
 * 职责：
 * 1. 判断是否需要裁剪（CALayerShouldClipImage 逻辑）
 * 2. 如果不需要裁剪：直接在 ContentModifier 中绘制
 * 3. 如果需要裁剪：使用 ImageClipRenderNode 作为子节点
 */
class ImageDisplayRenderNode : public BaseRenderNode {
public:
    ~ImageDisplayRenderNode() override;
    ImageDisplayRenderNode();

    /**
     * 绘制图像矩形
     *
     * @param pixelMap 原始图像的 PixelMap
     * @param srcX 源图像裁剪起始 X 坐标
     * @param srcY 源图像裁剪起始 Y 坐标
     * @param srcWidth 源图像裁剪宽度
     * @param srcHeight 源图像裁剪高度
     * @param dstX 目标位置 X 坐标
     * @param dstY 目标位置 Y 坐标
     * @param dstWidth 目标宽度
     * @param dstHeight 目标高度
     * @param filterQuality 图像过滤质量
     */
    void drawImageRect(OH_PixelmapNative *pixelMap, int32_t srcX, int32_t srcY, int32_t srcWidth, int32_t srcHeight,
                       int32_t dstX, int32_t dstY, int32_t dstWidth, int32_t dstHeight,
                       OH_Native_Draw_FilterQuality filterQuality);

    OH_DrawingNode_Type getType() override;

private:
    void invalidate();
    void initModifier() override;

    // 图像绘制参数（同时用于渲染和变化检测）
    OH_PixelmapNative *pixelMap_ = nullptr;
    int32_t srcX_ = 0;
    int32_t srcY_ = 0;
    int32_t srcWidth_ = 0;
    int32_t srcHeight_ = 0;
    int32_t dstX_ = 0;
    int32_t dstY_ = 0;
    int32_t dstWidth_ = 0;
    int32_t dstHeight_ = 0;
    OH_Native_Draw_FilterQuality filterQuality_ = OH_Native_Draw_FilterQuality::None;

    // ImageClipRenderNode 子节点（需要裁剪时使用）
    std::unique_ptr<ImageClipRenderNode> imageClipNode_;

    // 触发重绘
    ArkUI_FloatPropertyHandle invalidateCountProperty_ = nullptr;
    ArkUI_RenderContentModifierHandle modifier_ = nullptr;
};
} // namespace OH
#endif
