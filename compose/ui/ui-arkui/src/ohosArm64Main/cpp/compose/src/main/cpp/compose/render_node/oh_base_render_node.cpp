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

// Shadow methods implementation
BaseRenderNode *BaseRenderNode::setShadowColor(uint32_t color) {
    maybeThrow(OH_ArkUI_RenderNodeUtils_SetShadowColor(nodeHandle_, color));
    return this;
}

BaseRenderNode *BaseRenderNode::setShadowOffset(int32_t x, int32_t y) {
    maybeThrow(OH_ArkUI_RenderNodeUtils_SetShadowOffset(nodeHandle_, x, y));
    return this;
}

BaseRenderNode *BaseRenderNode::setShadowAlpha(float alpha) {
    maybeThrow(OH_ArkUI_RenderNodeUtils_SetShadowAlpha(nodeHandle_, alpha));
    return this;
}

BaseRenderNode *BaseRenderNode::setShadowElevation(float elevation) {
    maybeThrow(OH_ArkUI_RenderNodeUtils_SetShadowElevation(nodeHandle_, elevation));
    return this;
}

BaseRenderNode *BaseRenderNode::setShadowRadius(float radius) {
    maybeThrow(OH_ArkUI_RenderNodeUtils_SetShadowRadius(nodeHandle_, radius));
    return this;
}

BaseRenderNode *BaseRenderNode::clearShadow() {
    // Clear shadow by setting elevation, radius, and alpha to 0
    maybeThrow(OH_ArkUI_RenderNodeUtils_SetShadowElevation(nodeHandle_, 0.0f));
    maybeThrow(OH_ArkUI_RenderNodeUtils_SetShadowRadius(nodeHandle_, 0.0f));
    maybeThrow(OH_ArkUI_RenderNodeUtils_SetShadowAlpha(nodeHandle_, 0.0f));
    return this;
}

} // namespace OH