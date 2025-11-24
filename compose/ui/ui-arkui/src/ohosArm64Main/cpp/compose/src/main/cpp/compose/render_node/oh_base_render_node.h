#ifndef OH_BASE_RENDER_NODE_H
#define OH_BASE_RENDER_NODE_H

#include <arkui/native_render.h>
#include <functional>
#include <unordered_map>
#include <vector>
#include <cstring>

#include "../constants/oh_native_enums.h"
#include "../xcomponent_log.h"
#include "../trace/oh_systrace_section.h"

namespace OH {

// 属性缓存结构，用于避免重复设置相同的值
struct PropertyCache {
    // Position
    bool positionSet = false;
    int32_t cachedX = 0;
    int32_t cachedY = 0;

    // Size
    bool sizeSet = false;
    int32_t cachedWidth = 0;
    int32_t cachedHeight = 0;

    // Translate
    bool translateSet = false;
    float cachedTranslateX = 0.0f;
    float cachedTranslateY = 0.0f;

    // Opacity
    bool opacitySet = false;
    float cachedOpacity = 1.0f; // 默认不透明

    // Pivot
    bool pivotSet = false;
    float cachedPivotX = 0.0f;
    float cachedPivotY = 0.0f;

    // BackgroundColor
    bool backgroundColorSet = false;
    uint32_t cachedBackgroundColor = 0;

    // BorderWidth
    bool borderWidthSet = false;
    float cachedBorderWidth = 0.0f;

    // BorderColor
    bool borderColorSet = false;
    uint32_t cachedBorderColor = 0;

    // BorderCornerRadius
    bool borderCornerRadiusSet = false;
    uint32_t cachedBorderCornerRadius = 0;

    // Transform matrix (4x4 = 16 floats)
    bool transformSet = false;
    float cachedTransform[16] = {0};

    bool markAsNodeGroupSet = false;

    void reset() {
        positionSet = false;
        sizeSet = false;
        translateSet = false;
        opacitySet = false;
        pivotSet = false;
        backgroundColorSet = false;
        borderWidthSet = false;
        borderColorSet = false;
        borderCornerRadiusSet = false;
        transformSet = false;
    }
};

class BaseRenderNode {
public:
    explicit BaseRenderNode(ArkUI_RenderNodeHandle node);

    BaseRenderNode();

    virtual ~BaseRenderNode();

    BaseRenderNode(const BaseRenderNode &) = delete;

    BaseRenderNode &operator=(const BaseRenderNode &) = delete;

    BaseRenderNode(BaseRenderNode &&) noexcept;

    BaseRenderNode &operator=(BaseRenderNode &&) noexcept;

    ArkUI_RenderNodeHandle getHandle() const {
        return nodeHandle_;
    }

    explicit operator ArkUI_RenderNodeHandle() const {
        return nodeHandle_;
    }

    template <typename T, auto CreateFunc, auto DisposeFunc>
    class ScopedOption {
    public:
        ScopedOption() :
            option_(CreateFunc()) {
        }
        ~ScopedOption() {
            if (option_)
                DisposeFunc(option_);
        }
        T *get() const {
            return option_;
        }
        explicit operator T *() const {
            return option_;
        }

    private:
        T *option_;
    };

    virtual OH_DrawingNode_Type getType();

    BaseRenderNode *addChild(BaseRenderNode *child) {
        maybeThrow(OH_ArkUI_RenderNodeUtils_AddChild(nodeHandle_, child->getHandle()));
        child->setParent(this);
        children_.push_back(child);
        return this;
    }

    void setParent(BaseRenderNode *parent) {
        parent_ = parent;
    }

    BaseRenderNode *markSelfAsNodeGroup() {
        OH::SystraceSection trace("BaseRenderNode::markSelfAsNodeGroup");
        if (!propertyCache_.markAsNodeGroupSet) {
            maybeThrow(OH_ArkUI_RenderNodeUtils_SetMarkNodeGroup(nodeHandle_, true));
        }
        return this;
    }

    BaseRenderNode *removeChild(BaseRenderNode *child) {
        maybeThrow(OH_ArkUI_RenderNodeUtils_RemoveChild(nodeHandle_, child->getHandle()));
        child->setParent(nullptr);
        // 延迟清理策略：不立即从 children_ 列表中移除，在 clearChildren() 时统一清理
        // 这样可以保持 addChild 的 O(1) 性能，避免 removeChild 时的 O(n) 遍历
        return this;
    }

    BaseRenderNode *removeFromParent() {
        if (parent_) {
            maybeThrow(OH_ArkUI_RenderNodeUtils_RemoveChild(parent_->getHandle(), nodeHandle_));
            // 延迟清理策略：不立即从父节点的 children_ 列表中移除，在父节点 clearChildren() 时统一清理
        }
        this->setParent(nullptr);
        return this;
    }

    BaseRenderNode *getParent() const {
        return parent_;
    }

    BaseRenderNode *setPosition(const int32_t x, const int32_t y) {
        OH::SystraceSection trace("BaseRenderNode::setPosition");
        // 检查缓存，避免重复设置相同的值
        if (!propertyCache_.positionSet || propertyCache_.cachedX != x || propertyCache_.cachedY != y) {
            maybeThrow(OH_ArkUI_RenderNodeUtils_SetPosition(nodeHandle_, x, y));
            propertyCache_.positionSet = true;
            propertyCache_.cachedX = x;
            propertyCache_.cachedY = y;
        }
        return this;
    }

    BaseRenderNode *setSize(const int32_t width, const int32_t height) {
        OH::SystraceSection trace("BaseRenderNode::setSize");
        // 检查缓存，避免重复设置相同的值
        if (!propertyCache_.sizeSet || propertyCache_.cachedWidth != width || propertyCache_.cachedHeight != height) {
            maybeThrow(OH_ArkUI_RenderNodeUtils_SetSize(nodeHandle_, width, height));
            propertyCache_.sizeSet = true;
            propertyCache_.cachedWidth = width;
            propertyCache_.cachedHeight = height;
        }
        return this;
    }

    BaseRenderNode *setTransform(float *matrix) {
        OH::SystraceSection trace("BaseRenderNode::setTransform");
        // 检查缓存，避免重复设置相同的矩阵
        if (!propertyCache_.transformSet || !transForm3DMatrixEquals(propertyCache_.cachedTransform, matrix)) {
            maybeThrow(OH_ArkUI_RenderNodeUtils_SetTransform(nodeHandle_, matrix));
            propertyCache_.transformSet = true;
            std::memcpy(propertyCache_.cachedTransform, matrix, sizeof(float) * 16);
        }
        return this;
    }

    BaseRenderNode *setTransform(const float *matrix) {
        OH::SystraceSection trace("BaseRenderNode::setTransform");
        // 检查缓存，避免重复设置相同的矩阵
        if (!propertyCache_.transformSet || !transForm3DMatrixEquals(propertyCache_.cachedTransform, matrix)) {
            maybeThrow(OH_ArkUI_RenderNodeUtils_SetTransform(nodeHandle_, const_cast<float *>(matrix)));
            propertyCache_.transformSet = true;
            std::memcpy(propertyCache_.cachedTransform, matrix, sizeof(float) * 16);
        }
        return this;
    }

    BaseRenderNode *setTranslate(const float translateX, const float translateY) {
        OH::SystraceSection trace("BaseRenderNode::setTranslate");

        const float EPSILON = 0.01f;
        const bool xChanged =
            !propertyCache_.translateSet || (std::abs(propertyCache_.cachedTranslateX - translateX) >= EPSILON);
        const bool yChanged =
            !propertyCache_.translateSet || (std::abs(propertyCache_.cachedTranslateY - translateY) >= EPSILON);

        if (xChanged || yChanged) {
            maybeThrow(OH_ArkUI_RenderNodeUtils_SetTranslation(nodeHandle_, translateX, translateY));
            propertyCache_.translateSet = true;
            propertyCache_.cachedTranslateX = translateX;
            propertyCache_.cachedTranslateY = translateY;
        }
        return this;
    }

    BaseRenderNode *setClip(ArkUI_RenderNodeClipOption *clipOption) {
        OH::SystraceSection trace("BaseRenderNode::setClip");
        maybeThrow(OH_ArkUI_RenderNodeUtils_SetClip(nodeHandle_, clipOption));
        OH_ArkUI_RenderNodeUtils_DisposeRenderNodeClipOption(clipOption);
        return this;
    }

    BaseRenderNode *clearClip() {
        // TODO: implement clear clip in native render node utils
        return this;
    }

    BaseRenderNode *setMask(const float left, const float top, const float right, const float bottom) {
        OH::SystraceSection trace("BaseRenderNode::setMask");
        if (ArkUI_RectShapeOption *shape = OH_ArkUI_RenderNodeUtils_CreateRectShapeOption()) {
            OH_ArkUI_RenderNodeUtils_SetRectShapeOptionEdgeValue(shape, left, ARKUI_EDGE_DIRECTION_LEFT);
            OH_ArkUI_RenderNodeUtils_SetRectShapeOptionEdgeValue(shape, top, ARKUI_EDGE_DIRECTION_TOP);
            OH_ArkUI_RenderNodeUtils_SetRectShapeOptionEdgeValue(shape, right, ARKUI_EDGE_DIRECTION_RIGHT);
            OH_ArkUI_RenderNodeUtils_SetRectShapeOptionEdgeValue(shape, bottom, ARKUI_EDGE_DIRECTION_BOTTOM);
            if (ArkUI_RenderNodeMaskOption *mask =
                    OH_ArkUI_RenderNodeUtils_CreateRenderNodeMaskOptionFromRectShape(shape)) {
                maybeThrow(OH_ArkUI_RenderNodeUtils_SetMask(nodeHandle_, mask));
                OH_ArkUI_RenderNodeUtils_DisposeRenderNodeMaskOption(mask);
            }
            OH_ArkUI_RenderNodeUtils_DisposeRectShapeOption(shape);
        }
        return this;
    }

    BaseRenderNode *setBounds(const int32_t x, const int32_t y, const int32_t width, const int32_t height) {
        OH::SystraceSection trace("BaseRenderNode::setBounds");
        // 检查缓存，避免重复设置相同的值
        if (!propertyCache_.sizeSet || !propertyCache_.positionSet || propertyCache_.cachedX != x || propertyCache_.cachedY != y
            || propertyCache_.cachedWidth != width || propertyCache_.cachedHeight != height) {
            maybeThrow(OH_ArkUI_RenderNodeUtils_SetBounds(nodeHandle_, x, y, width, height));
            propertyCache_.sizeSet = true;
            propertyCache_.positionSet = true;
            propertyCache_.cachedX = x;
            propertyCache_.cachedY = y;
            propertyCache_.cachedWidth = width;
            propertyCache_.cachedHeight = height;
        }
        return this;
    }

    BaseRenderNode *setPivot(const float px, const float py) {
        OH::SystraceSection trace("BaseRenderNode::setPivot");
        // 检查缓存，避免重复设置相同的值
        if (!propertyCache_.pivotSet || propertyCache_.cachedPivotX != px || propertyCache_.cachedPivotY != py) {
            maybeThrow(OH_ArkUI_RenderNodeUtils_SetPivot(nodeHandle_, px, py));
            propertyCache_.pivotSet = true;
            propertyCache_.cachedPivotX = px;
            propertyCache_.cachedPivotY = py;
        }
        return this;
    }

    BaseRenderNode *setOpacity(const float opacity) {
        OH::SystraceSection trace("BaseRenderNode::setOpacity");
        // 检查缓存，避免重复设置相同的值
        if (!propertyCache_.opacitySet || propertyCache_.cachedOpacity != opacity) {
            maybeThrow(OH_ArkUI_RenderNodeUtils_SetOpacity(nodeHandle_, opacity));
            propertyCache_.opacitySet = true;
            propertyCache_.cachedOpacity = opacity;
        }
        return this;
    }

    BaseRenderNode *setBorderWidth(const float borderWidth) {
        OH::SystraceSection trace("BaseRenderNode::setBorderWidth");
        // 检查缓存，避免重复设置相同的值
        if (!propertyCache_.borderWidthSet || propertyCache_.cachedBorderWidth != borderWidth) {
            using BorderWidthOption =
                ScopedOption<ArkUI_NodeBorderWidthOption, OH_ArkUI_RenderNodeUtils_CreateNodeBorderWidthOption,
                             OH_ArkUI_RenderNodeUtils_DisposeNodeBorderWidthOption>;
            using BorderStyleOption =
                ScopedOption<ArkUI_NodeBorderStyleOption, OH_ArkUI_RenderNodeUtils_CreateNodeBorderStyleOption,
                             OH_ArkUI_RenderNodeUtils_DisposeNodeBorderStyleOption>;
            BorderStyleOption borderStyleOption;
            OH_ArkUI_RenderNodeUtils_SetNodeBorderStyleOptionEdgeStyle(
                borderStyleOption.get(), ARKUI_BORDER_STYLE_SOLID, ARKUI_EDGE_DIRECTION_ALL);

            BorderWidthOption borderWidthOption;
            OH_ArkUI_RenderNodeUtils_SetNodeBorderWidthOptionEdgeWidth(borderWidthOption.get(), borderWidth,
                                                                       ARKUI_EDGE_DIRECTION_ALL);
            maybeThrow(OH_ArkUI_RenderNodeUtils_SetBorderStyle(nodeHandle_, borderStyleOption.get()));
            maybeThrow(OH_ArkUI_RenderNodeUtils_SetBorderWidth(nodeHandle_, borderWidthOption.get()));
            propertyCache_.borderWidthSet = true;
            propertyCache_.cachedBorderWidth = borderWidth;
        }
        return this;
    }

    BaseRenderNode *setBorderColor(const uint32_t borderColors) {
        OH::SystraceSection trace("BaseRenderNode::setBorderColor");
        // 检查缓存，避免重复设置相同的值
        if (!propertyCache_.borderColorSet || propertyCache_.cachedBorderColor != borderColors) {
            using BorderColorOption =
                ScopedOption<ArkUI_NodeBorderColorOption, OH_ArkUI_RenderNodeUtils_CreateNodeBorderColorOption,
                             OH_ArkUI_RenderNodeUtils_DisposeNodeBorderColorOption>;
            BorderColorOption colorOption;
            OH_ArkUI_RenderNodeUtils_SetNodeBorderColorOptionEdgeColor(colorOption.get(), borderColors,
                                                                       ARKUI_EDGE_DIRECTION_ALL);
            maybeThrow(OH_ArkUI_RenderNodeUtils_SetBorderColor(nodeHandle_, colorOption.get()));
            propertyCache_.borderColorSet = true;
            propertyCache_.cachedBorderColor = borderColors;
        }
        return this;
    }

    BaseRenderNode *setBorderCornerRadius(const uint32_t cornerRadius) {
        OH::SystraceSection trace("BaseRenderNode::setBorderRadius");
        // 检查缓存，避免重复设置相同的值
        if (!propertyCache_.borderCornerRadiusSet || propertyCache_.cachedBorderCornerRadius != cornerRadius) {
            using BorderRadiusOption =
                ScopedOption<ArkUI_NodeBorderRadiusOption, OH_ArkUI_RenderNodeUtils_CreateNodeBorderRadiusOption,
                             OH_ArkUI_RenderNodeUtils_DisposeNodeBorderRadiusOption>;
            BorderRadiusOption borderRadius;
            OH_ArkUI_RenderNodeUtils_SetNodeBorderRadiusOptionCornerRadius(borderRadius.get(), cornerRadius,
                                                                           ARKUI_CORNER_DIRECTION_ALL);
            maybeThrow(OH_ArkUI_RenderNodeUtils_SetBorderRadius(nodeHandle_, borderRadius.get()));
            propertyCache_.borderCornerRadiusSet = true;
            propertyCache_.cachedBorderCornerRadius = cornerRadius;
        }
        return this;
    }

    BaseRenderNode *setBackgroundColor(const uint32_t backgroundColor) {
        OH::SystraceSection trace("BaseRenderNode::setBackgroundColor");
        // 检查缓存，避免重复设置相同的值
        if (!propertyCache_.backgroundColorSet || propertyCache_.cachedBackgroundColor != backgroundColor) {
            maybeThrow(OH_ArkUI_RenderNodeUtils_SetBackgroundColor(nodeHandle_, backgroundColor));
            propertyCache_.backgroundColorSet = true;
            propertyCache_.cachedBackgroundColor = backgroundColor;
        }
        return this;
    }

    uint32_t getHash() const {
        return hash_;
    }

    uint32_t getHostingHash() const {
        return hostingHash_;
    }

    void setHostingHash(const uint32_t hostingHash) {
        hostingHash_ = hostingHash;
    }

    // 清理所有子节点的 parent 指针（在节点被 dispose 之前调用）
    // 延迟清理策略：遍历 children_ 列表，清理所有子节点的 parent 指针
    // 即使某些子节点已经被 removeChild 移除，也不会影响功能（因为 parent 指针已经被清理）
    void clearChildren() {
        for (BaseRenderNode *child : children_) {
            if (child && child->getParent() == this) {
                // 只清理 parent 指向当前节点的子节点（防止重复清理）
                child->setParent(nullptr);
            }
        }
        children_.clear();
    }

protected:
    virtual void initModifier() {};
    virtual void invalidate() {};
    static uint32_t generateHash() {
        static std::atomic<uint32_t> counter{0};
        return ++counter;
    }

    static void maybeThrow(const int32_t status) {
        if (status != ARKUI_ERROR_CODE_NO_ERROR) {
            LOGE("OHRenderNode operation failed with status: %{public}d", status);
            throw std::runtime_error("OHRenderNode operation failed");
        }
    }

    static bool transForm3DMatrixEquals(const float *a, const float *b) noexcept {
        for (int i = 0; i < 16; ++i) {
            if (a[i] != b[i]) return false;
        }
        return true;
    }

    uint32_t hash_;
    uint32_t hostingHash_ = 0;
    ArkUI_RenderNodeHandle nodeHandle_;
    BaseRenderNode *parent_ = nullptr;
    std::vector<BaseRenderNode *> children_;
    PropertyCache propertyCache_; // 属性缓存，避免重复设置相同的值
};
} // namespace OH
#endif