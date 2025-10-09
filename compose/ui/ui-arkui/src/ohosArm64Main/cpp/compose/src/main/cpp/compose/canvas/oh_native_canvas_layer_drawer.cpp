#include "oh_native_canvas_layer_drawer.h"
#include "../xcomponent_log.h"

namespace OH {
    void OHRenderNodeDrawRect(float left, float top, float right, float bottom,
            float scale,
            bool isShader,
            const RenderNodeSaveState *saveState,
            BaseRenderNode* renderNodeForDrawing) {
        //TODO：需要获取画笔宽度 paint.strokeWidth
        const float strokeWidth = 0;
    
        int32_t x = (left + strokeWidth) * scale;
        int32_t y = (top + strokeWidth) * scale;
    
        int32_t width = (right - left + strokeWidth) * scale;
        int32_t height = (bottom - top + strokeWidth) * scale;
    
        renderNodeForDrawing
            ->setTransform(const_cast<float*>(saveState->transform.data()))
            ->setTranslate(saveState->translateX, saveState->translateY)
            ->setPosition(x, y)
            ->setSize(width, height)
            ->setBorderCornerRadius(0);
        if (!isShader) {
            //TODO：setMask(0)会导致不显示，需要了解具体怎么传值
            //renderNodeForDrawing->setMask(0);
            //TODO: paint实现后需要修改if (paint.style == TMMNativeDrawPaintingStyleStroke) {
            if (true) {
                //TODO:paint实现后需要修改 const UIColor *paintColor = [paint colorFromColorValue];
                renderNodeForDrawing
                        ->setBorderWidth(strokeWidth * scale)
                        ->setBorderColor(0xFF00FF00)
                        ->setBackgroundColor(0xFFFF0000);
            } else {
                renderNodeForDrawing->setBorderWidth(0)
                                //TODO:需要通过paint获取颜色
                        ->setBackgroundColor(0xFFFF0000);
            }
        } else {
    
        }
    }

    void OHRenderNodeDrawLine(float x1, float y1, float x2, float y2,
            float scale,
            bool isShader,
            const RenderNodeSaveState *saveState,
            BaseRenderNode* renderNodeForDrawing) {

        int32_t x = std::min(x1, x2) * scale;
        int32_t y = std::min(y1, y2) * scale;
        int32_t width = abs(x2 - x1) * scale;
        int32_t height = abs(y2 - y1) * scale;

        renderNodeForDrawing
                ->setTransform(const_cast<float*>(saveState->transform.data()))
                ->setTranslate(saveState->translateX, saveState->translateY)
                ->setPosition(x, y)
                ->setSize(width, height);

        if (!isShader) {
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