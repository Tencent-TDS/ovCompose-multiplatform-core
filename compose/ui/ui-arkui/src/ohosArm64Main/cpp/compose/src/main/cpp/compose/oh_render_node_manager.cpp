#include "oh_render_node_manager.h"
#include "xcomponent_log.h"
#include "native_node_api.h"
#include <window_manager/oh_display_manager.h>

void OHRenderNodeManager::DestroyNativeRoot() {
    if (m_contentHandle && m_customNodeHandle) {
        OH_ArkUI_NodeContent_RemoveNode(m_contentHandle, m_customNodeHandle);
    }
    OH_ArkUI_RenderNodeUtils_RemoveRenderNode(m_customNodeHandle, m_renderRootNode->getHandle());
    NativeNodeApi::getInstance()->disposeNode(m_customNodeHandle);
}

void OHRenderNodeManager::onResize(int32_t width, int32_t height) {
    float scaledDensity;

    ArkUI_NumberValue widthValue[] = {
            static_cast<float>(width)};
    ArkUI_AttributeItem widthItem = {
            widthValue, sizeof(widthValue) / sizeof(ArkUI_NumberValue)};

    NativeNodeApi::getInstance()->setLengthMetricUnit(m_customNodeHandle, ArkUI_LengthMetricUnit::ARKUI_LENGTH_METRIC_UNIT_PX);
    NativeNodeApi::getInstance()->setAttribute(m_customNodeHandle, NODE_WIDTH, &widthItem);

    ArkUI_NumberValue heightValue[] = {
            static_cast<float>(height)};
    ArkUI_AttributeItem heightItem = {
            heightValue, sizeof(heightValue) / sizeof(ArkUI_NumberValue)};

    NativeNodeApi::getInstance()->setAttribute(m_customNodeHandle, NODE_HEIGHT, &heightItem);
    m_renderRootNode->setSize(static_cast<int32_t>(width), static_cast<int32_t>(height));
}

void OHRenderNodeManager::CreateNativeRoot(napi_env env, napi_value nodeContent) {
    auto result = OH_ArkUI_GetNodeContentFromNapiValue(env, nodeContent, &m_contentHandle);
    if (result != ARKUI_ERROR_CODE_NO_ERROR || m_contentHandle == nullptr) {
        LOGE("Failed to get node content from napi value");
        return;
    }

    m_customNodeHandle = NativeNodeApi::getInstance()->createNode(ARKUI_NODE_CUSTOM);
    if (m_customNodeHandle == nullptr) {
        LOGE("Failed to create custom node");
        return;
    }

    m_renderRootNode = std::make_unique<OH::BaseRenderNode>();
    OH_ArkUI_RenderNodeUtils_AddRenderNode(m_customNodeHandle, m_renderRootNode->getHandle());
    OH_ArkUI_NodeContent_AddNode(m_contentHandle, m_customNodeHandle);
    LOGI("Create native root successfully");
}

arkui_utils::OHNativeCanvasProxyFactory* OHRenderNodeManager::createNativeCanvasProxyFactory() {
    if (m_renderRootNode == nullptr) {
        LOGE("Render root node is null. Cannot create proxy factory.");
        return nullptr;
    }

    if (!m_canvasProxyFactory) {
        m_canvasProxyFactory = std::make_unique<arkui_utils::OHNativeCanvasProxyFactory>(m_renderRootNode.get());
    }
    return m_canvasProxyFactory.get();
}
