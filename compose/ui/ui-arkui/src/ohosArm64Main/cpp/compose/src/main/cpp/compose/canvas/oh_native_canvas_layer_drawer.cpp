#include <arkui/native_render.h>
#include <arkui/native_type.h>
#include "oh_native_canvas_layer_drawer.h"
#include "../xcomponent_log.h"
#include "../constants/oh_native_enums.h"
#include "../render_node/oh_line_render_node.h"
#include "../render_node/oh_line_gradient_render_node.h"
#include "../render_node/oh_rect_gradient_render_node.h"
#include "../constants/oh_native_constants.h"
#include "../shader/oh_native_linear_gradient_shader.h"
#include "../shader/oh_native_radial_gradient_shader.h"
#include "../shader/oh_native_sweep_gradient_shader.h"
#include "../shader/oh_native_image_shader.h"

namespace OH {
void OHRenderNodeDrawRect(float left, float top, float right, float bottom, NativeBasicShader *shader,
                          const RenderNodeSaveState *saveState, BaseRenderNode *renderNodeForDrawing,
                          androidx::compose::ui::arkui::utils::OHComposeNativePaint *paint) {
    const float strokeWidth = paint->strokeWidth;
    int32_t x = (left - strokeWidth / 2);
    int32_t y = (top - strokeWidth / 2);

    int32_t width = (right - left + strokeWidth);
    int32_t height = (bottom - top + strokeWidth);

    renderNodeForDrawing->setTransform(const_cast<float *>(saveState->transform.data()))
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
        ((RectGradientRenderNode *)renderNodeForDrawing)->drawRect(left, top, right, bottom, strokeWidth, shader, paint->style);
    }
}

void OHRenderNodeDrawClipRect(float left, float top, float right, float bottom, const RenderNodeSaveState *saveState,
                              BaseRenderNode *renderNodeForDrawing) {
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

void OHRenderNodeDrawRoundRect(float left, float top, float right, float bottom, float radiusX, float radiusY,
                               NativeBasicShader *shader, const RenderNodeSaveState *saveState,
                               BaseRenderNode *renderNodeForDrawing,
                               androidx::compose::ui::arkui::utils::OHComposeNativePaint *paint) {
    const float strokeWidth = paint->strokeWidth;
    int32_t x = (left - strokeWidth / 2);
    int32_t y = (top - strokeWidth / 2);

    int32_t width = (right - left + strokeWidth);
    int32_t height = (bottom - top + strokeWidth);

    renderNodeForDrawing->setTransform(const_cast<float *>(saveState->transform.data()))
        ->setTranslate(saveState->translateX, saveState->translateY)
        ->setPosition(x, y)
        ->setSize(width, height)
        ->setBorderCornerRadius(radiusX);
    if (!shader) {
        // TODO：setMask(0)会导致不显示，需要了解具体怎么传值
        // renderNodeForDrawing->setMask(0);
        if (paint->style == OH_Native_Draw_PaintingStyle::Stroke) {
            renderNodeForDrawing->setBorderWidth(strokeWidth)
                ->setBorderColor(paint->color)
                ->setBackgroundColor(CLEAR_COLOR);
        } else {
            renderNodeForDrawing
                ->setBorderWidth(0)
                // TODO:需要通过paint获取颜色
                ->setBackgroundColor(paint->color);
        }
    } else {
    }
}

void OHRenderNodeDrawLine(float x1, float y1, float x2, float y2, NativeBasicShader *shader,
                          const RenderNodeSaveState *saveState, BaseRenderNode *renderNodeForDrawing,
                          androidx::compose::ui::arkui::utils::OHComposeNativePaint *paint) {
    const float strokeWidth = paint->strokeWidth;
    int32_t x = std::min(x1, x2);
    int32_t y = std::min(y1, y2);
    int32_t width = abs(x2 - x1);
    int32_t height = abs(y2 - y1);

    renderNodeForDrawing->setTransform(const_cast<float *>(saveState->transform.data()))
        ->setTranslate(saveState->translateX, saveState->translateY)
        ->setPosition(x, y)
        ->setSize(width, height);

    if (!shader) {
        ((LineRenderNode *)renderNodeForDrawing)->drawLine(x1, y1, x2, y2, strokeWidth, paint->color, paint->strokeCap);
    } else {
        ((LineGradientRenderNode *)renderNodeForDrawing)
            ->drawLine(x1, y1, x2, y2, strokeWidth, static_cast<NativeLinearGradientShader *>(shader),
                       paint->strokeCap);
    }
}

void OHRenderNodeDrawThrow(int32_t status) {
    if (status != ARKUI_ERROR_CODE_NO_ERROR) {
        LOGE("OHRenderNodeDraw operation failed with status: %{public}d", status);
        throw std::runtime_error("OHRenderNode operation failed");
    }
}
} // namespace OH