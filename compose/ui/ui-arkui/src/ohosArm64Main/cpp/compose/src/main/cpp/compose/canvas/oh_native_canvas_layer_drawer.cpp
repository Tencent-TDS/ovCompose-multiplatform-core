#include "oh_native_canvas_layer_drawer.h"
#include "../xcomponent_log.h"
#include "oh_native_enums.h"
#include "../constants/oh_native_constants.h"

namespace OH {
    void OHRenderNodeDrawRect(float left, float top, float right, float bottom,
            OH_Drawing_ShaderEffect* shader,
            const RenderNodeSaveState* saveState,
            BaseRenderNode* renderNodeForDrawing,
            androidx::compose::ui::arkui::utils::OHComposeNativePaint* paint) {
        const float strokeWidth = paint->strokeWidth;
        int32_t x = (left + strokeWidth);
        int32_t y = (top + strokeWidth);
    
        int32_t width = (right - left + strokeWidth);
        int32_t height = (bottom - top + strokeWidth);
    
        renderNodeForDrawing
            ->setTransform(const_cast<float*>(saveState->transform.data()))
            ->setTranslate(saveState->translateX, saveState->translateY)
            ->setPosition(x, y)
            ->setSize(width, height)
            ->setBorderCornerRadius(0);
        if (!shader) {
            //TODO：setMask(0)会导致不显示，需要了解具体怎么传值
            //renderNodeForDrawing->setMask(0);
            //TODO: paint实现后需要修改if (paint.style == TMMNativeDrawPaintingStyleStroke) {
            if (paint->style == OH_Native_Draw_PaintingStyle::OH_NATIVE_PAINTINT_STYLE_STROKE) {
                //TODO:paint实现后需要修改 const UIColor *paintColor = [paint colorFromColorValue];
                renderNodeForDrawing
                        ->setBorderWidth(strokeWidth)
                        ->setBorderColor(paint->color)
                        ->setBackgroundColor(CLEAR_COLOR);
            } else {
                renderNodeForDrawing->setBorderWidth(0)
                                //TODO:需要通过paint获取颜色
                        ->setBackgroundColor(paint->color);
            }
        } else {
    
        }
    }

    void OHRenderNodeDrawClipRect(float left, float top, float right, float bottom,
            const RenderNodeSaveState *saveState,
            BaseRenderNode* renderNodeForDrawing) {
        ArkUI_RectShapeOption* shape = OH_ArkUI_RenderNodeUtils_CreateRectShapeOption();
        if (shape) {
            OH_ArkUI_RenderNodeUtils_SetRectShapeOptionEdgeValue(shape, left, ARKUI_EDGE_DIRECTION_LEFT);
            OH_ArkUI_RenderNodeUtils_SetRectShapeOptionEdgeValue(shape, top, ARKUI_EDGE_DIRECTION_TOP);
            OH_ArkUI_RenderNodeUtils_SetRectShapeOptionEdgeValue(shape, right, ARKUI_EDGE_DIRECTION_RIGHT);
            OH_ArkUI_RenderNodeUtils_SetRectShapeOptionEdgeValue(shape, bottom, ARKUI_EDGE_DIRECTION_BOTTOM);
        }
        ArkUI_RenderNodeClipOption* clipOption = OH_ArkUI_RenderNodeUtils_CreateRenderNodeClipOptionFromRectShape(shape);
        OH_ArkUI_RenderNodeUtils_DisposeRectShapeOption(shape);
        if (clipOption) {
            renderNodeForDrawing->setTransform(const_cast<float*>(saveState->transform.data()))
                ->setTranslate(saveState->translateX, saveState->translateY)
                ->setClip(clipOption);
        }
    }

    void OHRenderNodeDrawLine(float x1, float y1, float x2, float y2,
            OH_Drawing_ShaderEffect* shader,
            const RenderNodeSaveState *saveState,
            BaseRenderNode* renderNodeForDrawing) {

        int32_t x = std::min(x1, x2);
        int32_t y = std::min(y1, y2);
        int32_t width = abs(x2 - x1);
        int32_t height = abs(y2 - y1);

        renderNodeForDrawing
                ->setTransform(const_cast<float*>(saveState->transform.data()))
                ->setTranslate(saveState->translateX, saveState->translateY)
                ->setPosition(x, y)
                ->setSize(width, height);

        if (!shader) {
            renderNodeForDrawing -> drawLine(x1, y1, x2, y2);
//            [(TMMNativeLineLayer *)layerForDrawing drawWithPointX1:pointX1
//            pointY1:pointY1
//            pointX2:pointX2
//            pointY2:pointY2
//            lineWidth:[paint strokeWidth]
//            lineColor:[paint colorFromColorValue]
//            strokeCap:[paint strokeCap]
//            density:density];
        } else {
//            [(TMMNativeLineGradientLayer *)layerForDrawing drawWithPointX1:pointX1
//            pointY1:pointY1
//            pointX2:pointX2
//            pointY2:pointY2
//            lineWidth:[paint strokeWidth]
//            shader:(TMMNativeLinearGradientShader *)shader
//            strokeCap:[paint strokeCap]];
        }
    }
} // namespace OH