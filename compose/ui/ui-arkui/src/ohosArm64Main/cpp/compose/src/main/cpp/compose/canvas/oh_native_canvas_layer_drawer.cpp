#include "oh_native_canvas_layer_drawer.h"

#include <arkui/native_render.h>
#include <arkui/native_type.h>
#include <native_drawing/drawing_path.h>
#include <native_drawing/drawing_rect.h>

#include "../constants/oh_native_constants.h"
#include "../constants/oh_native_enums.h"
#include "../render_node/oh_arc_render_node.h"
#include "../render_node/oh_image_display_render_node.h"
#include "../render_node/oh_line_gradient_render_node.h"
#include "../render_node/oh_line_render_node.h"
#include "../render_node/oh_oval_render_node.h"
#include "../render_node/oh_path_render_node.h"
#include "../render_node/oh_rect_gradient_render_node.h"
#include "../shader/oh_native_image_shader.h"
#include "../shader/oh_native_linear_gradient_shader.h"
#include "../xcomponent_log.h"
#include "compose/trace/oh_systrace_section.h"

namespace OH {
void OHRenderNodeDrawRect(const float left, const float top, const float right, const float bottom,
                          NativeBasicShader *shader, const RenderNodeSaveState *saveState,
                          BaseRenderNode *renderNodeForDrawing,
                          const androidx::compose::ui::arkui::utils::OHComposeNativePaint *paint) {
    OH::SystraceSection trace("LayerDrawer:OHRenderNodeDrawRect");
    const float strokeWidth = paint->strokeWidth;
    const int32_t x = left - strokeWidth / 2;
    const int32_t y = top - strokeWidth / 2;

    const int32_t width = right - left + strokeWidth;
    const int32_t height = bottom - top + strokeWidth;

    renderNodeForDrawing->setTransform(saveState->transform.data())
        ->setTranslate(saveState->translateX, saveState->translateY)
        ->setPosition(x, y)
        ->setSize(width, height)
        ->setBorderCornerRadius(0);
    if (!shader) {
        if (paint->style == OH_Native_Draw_PaintingStyle::Stroke) {
            renderNodeForDrawing->setBorderWidth(strokeWidth)
                ->setBorderColor(paint->color)
                ->setBackgroundColor(CLEAR_COLOR);
        } else {
            renderNodeForDrawing->setBorderWidth(0)->setBackgroundColor(paint->color);
        }
    } else {
        // apply shader
        LOGI("OHRenderNodeDrawRect: apply shader start: %{public}p", shader);
        static_cast<RectGradientRenderNode *>(renderNodeForDrawing)
            ->drawRect(left, top, right, bottom, strokeWidth, shader, paint->style);
    }
}

void OHRenderNodeDrawClipRect(const float left, const float top, const float right, const float bottom,
                              const RenderNodeSaveState *saveState, BaseRenderNode *renderNodeForDrawing) {
    OH::SystraceSection trace("LayerDrawer:OHRenderNodeDrawClipRect");
    ArkUI_RectShapeOption *shape = OH_ArkUI_RenderNodeUtils_CreateRectShapeOption();
    if (shape) {
        OH_ArkUI_RenderNodeUtils_SetRectShapeOptionEdgeValue(shape, left, ARKUI_EDGE_DIRECTION_LEFT);
        OH_ArkUI_RenderNodeUtils_SetRectShapeOptionEdgeValue(shape, top, ARKUI_EDGE_DIRECTION_TOP);
        OH_ArkUI_RenderNodeUtils_SetRectShapeOptionEdgeValue(shape, right, ARKUI_EDGE_DIRECTION_RIGHT);
        OH_ArkUI_RenderNodeUtils_SetRectShapeOptionEdgeValue(shape, bottom, ARKUI_EDGE_DIRECTION_BOTTOM);
    }
    ArkUI_RenderNodeClipOption *clipOption = OH_ArkUI_RenderNodeUtils_CreateRenderNodeClipOptionFromRectShape(shape);
    OH_ArkUI_RenderNodeUtils_DisposeRectShapeOption(shape);
    if (clipOption) {
        renderNodeForDrawing->setTransform(const_cast<float *>(saveState->transform.data()))
            ->setTranslate(saveState->translateX, saveState->translateY)
            ->setClip(clipOption);
    }
}

void OHRenderNodeDrawRoundRect(const float left, const float top, const float right, const float bottom,
                               const float radiusX, const float radiusY, const NativeBasicShader *shader,
                               const RenderNodeSaveState *saveState, BaseRenderNode *renderNodeForDrawing,
                               const androidx::compose::ui::arkui::utils::OHComposeNativePaint *paint) {
    OH::SystraceSection trace("LayerDrawer:OHRenderNodeDrawRoundRect");
    const float strokeWidth = paint->strokeWidth;
    const int32_t x = left - strokeWidth / 2;
    const int32_t y = top - strokeWidth / 2;

    const int32_t width = right - left + strokeWidth;
    const int32_t height = bottom - top + strokeWidth;

    renderNodeForDrawing->setTransform(const_cast<float *>(saveState->transform.data()))
        ->setTranslate(saveState->translateX, saveState->translateY)
        ->setPosition(x, y)
        ->setSize(width, height)
        ->setBorderCornerRadius(radiusX);
    if (!shader) {
        if (paint->style == OH_Native_Draw_PaintingStyle::Stroke) {
            renderNodeForDrawing->setBorderWidth(strokeWidth)
                ->setBorderColor(paint->color)
                ->setBackgroundColor(CLEAR_COLOR);
        } else {
            renderNodeForDrawing->setBorderWidth(0)->setBackgroundColor(paint->color);
        }
    } else {
    }
}

void OHRenderNodeDrawLine(const float x1, const float y1, const float x2, const float y2, NativeBasicShader *shader,
                          const RenderNodeSaveState *saveState, BaseRenderNode *renderNodeForDrawing,
                          const androidx::compose::ui::arkui::utils::OHComposeNativePaint *paint) {
    const float strokeWidth = paint->strokeWidth;
    const float halfStroke = strokeWidth / 2.0f;
    // 考虑strokeWidth，调整position和size
    const int32_t x = static_cast<int32_t>(std::min(x1, x2) - halfStroke);
    const int32_t y = static_cast<int32_t>(std::min(y1, y2) - halfStroke);
    const int32_t width = static_cast<int32_t>(abs(x2 - x1) + strokeWidth);
    const int32_t height = static_cast<int32_t>(abs(y2 - y1) + strokeWidth);

    renderNodeForDrawing->setTransform(const_cast<float *>(saveState->transform.data()))
        ->setTranslate(saveState->translateX, saveState->translateY)
        ->setPosition(x, y)
        ->setSize(width, height);

    if (!shader) {
        static_cast<LineRenderNode *>(renderNodeForDrawing)
            ->drawLine(x1, y1, x2, y2, strokeWidth, paint->color, paint->strokeCap);
    } else {
        static_cast<LineGradientRenderNode *>(renderNodeForDrawing)
            ->drawLine(x1, y1, x2, y2, strokeWidth, dynamic_cast<NativeLinearGradientShader *>(shader),
                       paint->strokeCap);
    }
}

void OHRenderNodeDrawCircle(const float centerX, const float centerY, const float radius,
                            const NativeBasicShader *shader, const RenderNodeSaveState *saveState,
                            BaseRenderNode *renderNodeForDrawing,
                            const androidx::compose::ui::arkui::utils::OHComposeNativePaint *paint) {
    OH::SystraceSection trace("LayerDrawer:OHRenderNodeDrawCircle");

    const float strokeWidth = paint->strokeWidth;
    const int32_t x = centerX - radius - strokeWidth / 2;
    const int32_t y = centerY - radius - strokeWidth / 2;

    const int32_t width = 2 * radius + strokeWidth;
    const int32_t height = 2 * radius + strokeWidth;

    renderNodeForDrawing->setTransform(const_cast<float *>(saveState->transform.data()))
        ->setTranslate(saveState->translateX, saveState->translateY)
        ->setPosition(x, y)
        ->setSize(width, height)
        ->setBorderCornerRadius(static_cast<uint32_t>(radius));
    if (!shader) {
        if (paint->style == OH_Native_Draw_PaintingStyle::Stroke) {
            renderNodeForDrawing->setBorderWidth(strokeWidth)
                ->setBorderColor(paint->color)
                ->setBackgroundColor(CLEAR_COLOR);
        } else {
            renderNodeForDrawing->setBorderWidth(0)->setBackgroundColor(paint->color);
        }
    } else {
    }
}

void OHRenderNodeDrawOval(const float left, const float top, const float right, const float bottom,
                          const NativeBasicShader *shader, const RenderNodeSaveState *saveState,
                          BaseRenderNode *renderNodeForDrawing,
                          const androidx::compose::ui::arkui::utils::OHComposeNativePaint *paint) {
    OH::SystraceSection trace("LayerDrawer:OHRenderNodeDrawOval");
    const float strokeWidth = paint->strokeWidth;
    const int32_t x = left - strokeWidth / 2;
    const int32_t y = top - strokeWidth / 2;
    const int32_t width = right - left + strokeWidth;
    const int32_t height = bottom - top + strokeWidth;

    renderNodeForDrawing->setTransform(const_cast<float *>(saveState->transform.data()))
        ->setTranslate(saveState->translateX, saveState->translateY)
        ->setPosition(x, y)
        ->setSize(width, height);

    if (!shader) {
        static_cast<OvalRenderNode *>(renderNodeForDrawing)
            ->drawOval(left, top, right, bottom, strokeWidth, paint->color, paint->style);
    } else {
        // TODO: 实现带shader的椭圆绘制（需要创建OvalGradientRenderNode）
    }
}

void OHRenderNodeDrawArc(const float left, const float top, const float right, const float bottom,
                         const float startAngle, const float sweepAngle, const bool useCenter,
                         const NativeBasicShader *shader, const RenderNodeSaveState *saveState,
                         BaseRenderNode *renderNodeForDrawing,
                         const androidx::compose::ui::arkui::utils::OHComposeNativePaint *paint) {
    OH::SystraceSection trace("LayerDrawer:OHRenderNodeDrawArc");
    const float strokeWidth = paint->strokeWidth;
    const int32_t x = left - strokeWidth / 2;
    const int32_t y = top - strokeWidth / 2;
    const int32_t width = right - left + strokeWidth;
    const int32_t height = bottom - top + strokeWidth;

    renderNodeForDrawing->setTransform(const_cast<float *>(saveState->transform.data()))
        ->setTranslate(saveState->translateX, saveState->translateY)
        ->setPosition(x, y)
        ->setSize(width, height);

    if (!shader) {
        static_cast<ArcRenderNode *>(renderNodeForDrawing)
            ->drawArc(left, top, right, bottom, startAngle, sweepAngle, useCenter, strokeWidth, paint->color,
                      paint->style);
    } else {
        // TODO: 实现带shader的圆弧绘制（需要创建ArcGradientRenderNode）
    }
}

void OHRenderNodeDrawPath(OH_Drawing_Path *path, const NativeBasicShader *shader, const RenderNodeSaveState *saveState,
                          BaseRenderNode *renderNodeForDrawing,
                          const androidx::compose::ui::arkui::utils::OHComposeNativePaint *paint) {
    OH::SystraceSection trace("LayerDrawer:OHRenderNodeDrawPath");
    const float strokeWidth = paint->strokeWidth;

    // Calculate bounds from path using OH_Drawing_PathGetBounds
    int32_t x = 0;
    int32_t y = 0;
    int32_t width = 100;  // Default fallback
    int32_t height = 100; // Default fallback

    if (path != nullptr) {
        OH_Drawing_Rect *boundsRect = OH_Drawing_RectCreate(0.0f, 0.0f, 0.0f, 0.0f);
        if (boundsRect != nullptr) {
            OH_Drawing_PathGetBounds(path, boundsRect);

            // Get rect values
            float left = OH_Drawing_RectGetLeft(boundsRect);
            float top = OH_Drawing_RectGetTop(boundsRect);
            float right = OH_Drawing_RectGetRight(boundsRect);
            float bottom = OH_Drawing_RectGetBottom(boundsRect);

            // Account for stroke width
            const float halfStroke = strokeWidth / 2.0f;
            x = static_cast<int32_t>(left - halfStroke);
            y = static_cast<int32_t>(top - halfStroke);
            width = static_cast<int32_t>((right - left) + strokeWidth);
            height = static_cast<int32_t>((bottom - top) + strokeWidth);

            // Ensure minimum size
            if (width <= 0) width = 1;
            if (height <= 0) height = 1;

            OH_Drawing_RectDestroy(boundsRect);
        }
    }

    renderNodeForDrawing->setTransform(const_cast<float *>(saveState->transform.data()))
        ->setTranslate(saveState->translateX, saveState->translateY)
        ->setPosition(x, y)
        ->setSize(width, height);

    if (!shader) {
        static_cast<PathRenderNode *>(renderNodeForDrawing)
            ->drawPath(path, strokeWidth, paint->color, paint->style);
    } else {
        // TODO: 实现带shader的路径绘制（需要创建PathGradientRenderNode）
    }
}

void OHRenderNodeDrawImageRect(OH_PixelmapNative *pixelMap, int32_t srcX, int32_t srcY, int32_t srcWidth,
                               int32_t srcHeight, int32_t dstX, int32_t dstY, int32_t dstWidth, int32_t dstHeight,
                               const RenderNodeSaveState *saveState, BaseRenderNode *renderNodeForDrawing,
                               const androidx::compose::ui::arkui::utils::OHComposeNativePaint *paint) {
    OH::SystraceSection trace("LayerDrawer:OHRenderNodeDrawImageRect");
    LOGI("OHRenderNodeDrawImageRect: src=(%{public}d, %{public}d, %{public}d, %{public}d), "
         "dst=(%{public}d, %{public}d, %{public}d, %{public}d)",
         srcX, srcY, srcWidth, srcHeight, dstX, dstY, dstWidth, dstHeight);

    // 设置RenderNode的transform和translate（ImageDisplayRenderNode会自己设置position和size）
    renderNodeForDrawing->setTransform(const_cast<float *>(saveState->transform.data()))
        ->setTranslate(saveState->translateX, saveState->translateY);
    auto *imageNode = static_cast<ImageDisplayRenderNode *>(renderNodeForDrawing);

    // 调用ImageDisplayRenderNode的drawImageRect方法
    // ImageDisplayRenderNode会判断是否需要裁剪，并自行设置position和size
    imageNode->drawImageRect(pixelMap, srcX, srcY, srcWidth, srcHeight, dstX, dstY, dstWidth, dstHeight,
                             paint->filterQuality);
}

void OHRenderNodeDrawText(const RenderNodeSaveState *saveState, Paragraph *paragraphNode) {
    const float originX = saveState->translateX;
    const float originY = saveState->translateY;
    const int32_t width = paragraphNode->getWidth();
    const int32_t height = paragraphNode->getHeight();

    paragraphNode->setTransform(saveState->transform.data())
        ->setTranslate(originX, originY)
        ->setPosition(originX, originY)
        ->setSize(width, height);

    LOGI(
        "OHRenderNodeDrawText: start drawing paragraph, translateX/Y: "
        "%{public}f, %{public}f, "
        "position: 0, 0, size: %{public}d, %{public}d, transform applied",
        originX, originY, width, height);
    paragraphNode->paint(0, 0);
}

void OHRenderNodeDrawThrow(const int32_t status) {
    if (status != ARKUI_ERROR_CODE_NO_ERROR) {
        LOGE("OHRenderNodeDraw operation failed with status: %{public}d", status);
        throw std::runtime_error("OHRenderNode operation failed");
    }
}
} // namespace OH