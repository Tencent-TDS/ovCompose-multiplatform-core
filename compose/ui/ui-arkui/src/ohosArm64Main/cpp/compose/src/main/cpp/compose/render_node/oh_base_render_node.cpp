#include "oh_base_render_node.h"
#include "../shader/oh_native_linear_gradient_shader.h"

namespace OH {
BaseRenderNode::BaseRenderNode(ArkUI_RenderNodeHandle node) : hash_(generateHash()), nodeHandle_(node) {
    if (!nodeHandle_) {
        throw std::invalid_argument("Node handle cannot be null");
    }
}

BaseRenderNode::BaseRenderNode() : BaseRenderNode(OH_ArkUI_RenderNodeUtils_CreateNode()) {
}

BaseRenderNode::~BaseRenderNode() {
    if (nodeHandle_) {
        maybeThrow(OH_ArkUI_RenderNodeUtils_DisposeNode(nodeHandle_));
    }
}

BaseRenderNode::BaseRenderNode(BaseRenderNode &&other) noexcept
    : nodeHandle_(other.nodeHandle_), hostingHash_(other.hostingHash_), hash_(other.hash_) {
    other.nodeHandle_ = nullptr;
}

BaseRenderNode &BaseRenderNode::operator=(BaseRenderNode &&other) noexcept {
    if (this != &other) {
        nodeHandle_ = other.nodeHandle_;
        hostingHash_ = other.hostingHash_;
        hash_ = other.hash_;
        other.nodeHandle_ = nullptr;
    }
    return *this;
}

OH_DrawingNode_Type BaseRenderNode::getType() {
    return OH_DrawingNode_Type::BaseNode;
};
} // namespace OH