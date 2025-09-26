#include "oh_render_node_manager.h"
#include "xcomponent_log.h"
#include "native_node_api.h"

void OHRenderNodeManager::DestroyNativeRoot() {
    if (m_contentHandle && m_customNodeHandle) {
        OH_ArkUI_NodeContent_RemoveNode(m_contentHandle, m_customNodeHandle);
    }
}

void OHRenderNodeManager::CreateNativeRoot(napi_env env, napi_value nodeContent) {
    OH_ArkUI_GetNodeContentFromNapiValue(env, nodeContent, &m_contentHandle);
    m_customNodeHandle = NativeNodeApi::getInstance()->createNode(ARKUI_NODE_CUSTOM);
    m_renderRootNode = OH_ArkUI_RenderNodeUtils_CreateNode();
    OH_ArkUI_RenderNodeUtils_AddRenderNode(m_customNodeHandle, m_renderRootNode);
    OH_ArkUI_NodeContent_AddNode(m_contentHandle, m_customNodeHandle);
}

arkui_utils::OHNativeCanvasProxyFactory* OHRenderNodeManager::createNativeCanvasProxyFactory() {
    if (m_renderRootNode == nullptr) {
        LOGE("Render root node is null. Cannot create proxy factory.");
        return nullptr;
    }

    if (!m_canvasProxyFactory) {
        m_canvasProxyFactory = std::make_unique<arkui_utils::OHNativeCanvasProxyFactory>(m_renderRootNode);
    }
    return m_canvasProxyFactory.get();
}
