#ifndef OH_IMAGE_DISPLAY_RENDER_NODE_H
#define OH_IMAGE_DISPLAY_RENDER_NODE_H

#include "oh_base_render_node.h"
#include "compose/filter/oh_compose_native_color_filter.h"

#include <multimedia/image_framework/image/pixelmap_native.h>

namespace OH {
/**
 * ImageDisplayRenderNode - 图片显示渲染节点
 * 参考 iOS TMMImageDisplayLayer 实现
 *
 * 职责：
 * 1. 在 ContentModifier 中使用 OH_Drawing_CanvasDrawPixelMapRect 绘制图片
 * 2. 通过 srcRect 和 dstRect 参数实现裁剪和缩放
 * 3. 支持 ColorFilter 颜色滤镜效果
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
                       int32_t dstX, int32_t dstY, int32_t dstWidth, int32_t dstHeight, OHComposeNativeColorFilter* colorFilter,
                       OH_Native_Draw_FilterQuality filterQuality);

    OH_DrawingNode_Type getType() override;

private:
    void invalidate() override;
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
    OHComposeNativeColorFilter*  colorFilter_ = nullptr;
    OH_Native_Draw_FilterQuality filterQuality_ = OH_Native_Draw_FilterQuality::None;

    // 触发重绘
    ArkUI_FloatPropertyHandle invalidateCountProperty_ = nullptr;
    ArkUI_RenderContentModifierHandle modifier_ = nullptr;
};
} // namespace OH
#endif
