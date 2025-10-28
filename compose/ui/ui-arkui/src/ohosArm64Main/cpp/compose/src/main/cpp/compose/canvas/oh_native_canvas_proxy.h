#ifndef ANDROIDX_COMPOSE_UI_ARKUI_UTILS_OHNATIVECANVASPROXY_H
#define ANDROIDX_COMPOSE_UI_ARKUI_UTILS_OHNATIVECANVASPROXY_H

#include "../paragraph/oh_native_paragraph_builder.h"
#include "oh_compose_native_paint.h"
#include "../render_node/oh_base_render_node.h"
#include "../picture_recorder/oh_native_picture_recorder.h"


namespace androidx::compose::ui::arkui::utils {
class OHNativeCanvasProxy {
public:
    explicit OHNativeCanvasProxy(OH::BaseRenderNode *rootNode);
    ~OHNativeCanvasProxy();
    OHComposeNativePaint* Paint();
    OH::BaseRenderNode* getRenderNode();
    void beginDraw();
    void attachToRootView();
    void setParent(OHNativeCanvasProxy *canvasParentProxy);
    void finishDraw();
    void save();
    void restore();
    void translate(float dx, float dy);
    void setPosition(int32_t x, int32_t y);
    void setBounds(int32_t originX, int32_t originY, int32_t boundsWidth, int32_t boundsHeight);
    void setPivot(float px, float py);
    void setOpacity(float opacity);

    void drawRect(float left, float top, float right, float bottom, OHComposeNativePaint* paint);
    void drawRoundRect(float left, float top, float right, float bottom, float radiusX, float radiusY, OHComposeNativePaint* paint);
    void drawLine(float x1, float y1, float x2, float y2, OHComposeNativePaint* paint);
    void drawLayer(OH::BaseRenderNode* renderNode);
	void drawParagraph(OH::Paragraph* paragraph);	
    void clipRect(float left, float top, float right, float bottom, OH_Native_Draw_ClipOp clipOp);
    void drawLayerWithSubproxy(OHNativeCanvasProxy *subProxy);
private:
    OHComposeNativePaint *paint_;
    OH::BaseRenderNode *rootNode_;
    std::unique_ptr<OH::BaseRenderNode> canvasNode_;
    OH::PictureRecorder _pictureRecorder;
};
} // namespace androidx::compose::ui::arkui::utils

#endif