#ifndef ANDROIDX_COMPOSE_UI_ARKUI_UTILS_OHNATIVECANVASPROXY_H
#define ANDROIDX_COMPOSE_UI_ARKUI_UTILS_OHNATIVECANVASPROXY_H

#include "arkui/native_render.h"


namespace androidx::compose::ui::arkui::utils {
class OHNativeCanvasProxy {
public:
    explicit OHNativeCanvasProxy(ArkUI_RenderNode *rootNode);
    ~OHNativeCanvasProxy();
    void BeginDraw();
private:
    ArkUI_RenderNode *rootNode_;
};
} // namespace androidx::compose::ui::arkui::utils

#endif