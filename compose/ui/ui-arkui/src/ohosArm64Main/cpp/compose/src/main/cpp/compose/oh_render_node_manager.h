#pragma once

#include <list>
#include <memory>
#include <unordered_map>

#include "napi/native_api.h"
#include <arkui/native_node_napi.h>
#include <js_native_api_types.h>
#include <arkui/native_render.h>
#include "canvas/oh_native_canvas_proxy_factory.h"
#include "render_node/oh_base_render_node.h"

namespace arkui_utils = androidx::compose::ui::arkui::utils;

class OHRenderNodeManager {
public:
    static OHRenderNodeManager* GetInstance() {
        static OHRenderNodeManager instance;
        return &instance;
    }

    void DestroyNativeRoot();
    void CreateNativeRoot(napi_env env, napi_value nodeContent);
    void onResize(int32_t width, int32_t height);

    arkui_utils::OHNativeCanvasProxyFactory* createNativeCanvasProxyFactory();

private:
    ArkUI_NodeContentHandle m_contentHandle = nullptr;
    ArkUI_NodeHandle m_customNodeHandle = nullptr;
    std::unique_ptr<OH::BaseRenderNode> m_renderRootNode = nullptr;
    std::unique_ptr<arkui_utils::OHNativeCanvasProxyFactory> m_canvasProxyFactory = nullptr;
};
