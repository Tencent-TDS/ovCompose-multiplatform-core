#ifndef ANDROIDX_COMPOSE_UI_ARKUI_UTILS_OHNATIVECANVASPROXY_H
#define ANDROIDX_COMPOSE_UI_ARKUI_UTILS_OHNATIVECANVASPROXY_H

#include <arkui/native_render.h>
#include "../picture_recorder/oh_native_picture_recorder.h"


namespace androidx::compose::ui::arkui::utils {
class OHNativeCanvasProxy {
public:
    explicit OHNativeCanvasProxy(OH::BaseRenderNode *rootNode);
    ~OHNativeCanvasProxy();
    OH::BaseRenderNode* getRenderNode();
    void beginDraw();
    void attachToRootView();
    void setParent(OHNativeCanvasProxy *canvasParentProxy);
    void finishDraw();

    void drawRect(float left, float top, float right, float bottom);
    void drawLine(float x1, float y1, float x2, float y2);
    void drawLayer();
private:
    OH::BaseRenderNode *rootNode_;
    std::unique_ptr<OH::BaseRenderNode> canvasNode_;
    OH::PictureRecorder _pictureRecorder;
    float _scaledDensity;
};
} // namespace androidx::compose::ui::arkui::utils

#endif