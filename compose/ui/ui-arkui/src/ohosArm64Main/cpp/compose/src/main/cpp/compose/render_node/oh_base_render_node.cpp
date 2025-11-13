#include "oh_base_render_node.h"
#include "../shader/oh_native_linear_gradient_shader.h"

namespace OH {
BaseRenderNode::BaseRenderNode(const ArkUI_RenderNodeHandle node) : hash_(generateHash()), nodeHandle_(node) {
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
    : hash_(other.hash_), hostingHash_(other.hostingHash_), nodeHandle_(other.nodeHandle_),
      parent_(other.parent_), children_(std::move(other.children_)), propertyCache_(other.propertyCache_) {
    other.nodeHandle_ = nullptr;
    other.parent_ = nullptr;
    other.propertyCache_.reset(); // 重置源对象的缓存
}

BaseRenderNode &BaseRenderNode::operator=(BaseRenderNode &&other) noexcept {
    if (this != &other) {
        nodeHandle_ = other.nodeHandle_;
        hostingHash_ = other.hostingHash_;
        hash_ = other.hash_;
        parent_ = other.parent_;
        children_ = std::move(other.children_);
        propertyCache_ = other.propertyCache_;
        other.nodeHandle_ = nullptr;
        other.parent_ = nullptr;
        other.propertyCache_.reset(); // 重置源对象的缓存
    }
    return *this;
}

OH_DrawingNode_Type BaseRenderNode::getType() {
    return OH_DrawingNode_Type::BaseNode;
};
} // namespace OH