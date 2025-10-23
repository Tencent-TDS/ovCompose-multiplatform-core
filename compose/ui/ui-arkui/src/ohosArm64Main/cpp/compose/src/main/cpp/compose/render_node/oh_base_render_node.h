#ifndef OH_BASE_RENDER_NODE_H
#define OH_BASE_RENDER_NODE_H

#include <functional>
#include <unordered_map>
#include <memory>
#include <arkui/native_render.h>
#include <native_drawing/drawing_path.h>
#include <native_drawing/drawing_color.h>
#include <native_drawing/drawing_pen.h>
#include <native_drawing/drawing_canvas.h>
#include "../xcomponent_log.h"
#include "../constants/oh_native_enums.h"

namespace OH {
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

        operator ArkUI_RenderNodeHandle() const {
            return nodeHandle_;
        }


        template<typename T, auto CreateFunc, auto DisposeFunc>
        class ScopedOption {
        public:
            ScopedOption() : option_(CreateFunc()) {}
            ~ScopedOption() { if (option_) DisposeFunc(option_); }
            T* get() const { return option_; }
            operator T*() const { return option_; }
        private:
            T* option_;
        };

        BaseRenderNode* addChild(BaseRenderNode* child) {
            maybeThrow(OH_ArkUI_RenderNodeUtils_AddChild(nodeHandle_, child->getHandle()));
            child->setParent(this);
            return this;
        }

        void setParent(BaseRenderNode* parent) {
            parent_ = parent;
        }

        BaseRenderNode* removeChild(BaseRenderNode* child) {
            maybeThrow(OH_ArkUI_RenderNodeUtils_RemoveChild(nodeHandle_, child->getHandle()));
            child->setParent(nullptr);
            return this;
        }

        BaseRenderNode* getParent() const {
            return parent_;
        }

        BaseRenderNode* setPosition(int32_t x, int32_t y) {
            maybeThrow(OH_ArkUI_RenderNodeUtils_SetPosition(nodeHandle_, x, y));
            return this;
        }

        BaseRenderNode* setSize(int32_t width, int32_t height) {
            maybeThrow(OH_ArkUI_RenderNodeUtils_SetSize(nodeHandle_, width, height));
            return this;
        }

        BaseRenderNode* setTransform(float* matrix) {
            maybeThrow(OH_ArkUI_RenderNodeUtils_SetTransform(nodeHandle_, matrix));
            return this;
        }

        BaseRenderNode* setTransform(const float* matrix) {
            maybeThrow(OH_ArkUI_RenderNodeUtils_SetTransform(nodeHandle_, const_cast<float*>(matrix)));
            return this;
        }

        BaseRenderNode* setTranslate(float translateX, float translateY) {
            maybeThrow(OH_ArkUI_RenderNodeUtils_SetTranslation(nodeHandle_, translateX, translateY));
            return this;
        }

        BaseRenderNode* setClip(ArkUI_RenderNodeClipOption* clipOption) {
            maybeThrow(OH_ArkUI_RenderNodeUtils_SetClip(nodeHandle_, clipOption));
            OH_ArkUI_RenderNodeUtils_DisposeRenderNodeClipOption(clipOption);
            return this;
        }

        BaseRenderNode* setMask(float left, float top, float right, float bottom) {
            ArkUI_RectShapeOption* shape = OH_ArkUI_RenderNodeUtils_CreateRectShapeOption();
            if (shape) {
                OH_ArkUI_RenderNodeUtils_SetRectShapeOptionEdgeValue(shape, left, ARKUI_EDGE_DIRECTION_LEFT);
                OH_ArkUI_RenderNodeUtils_SetRectShapeOptionEdgeValue(shape, top, ARKUI_EDGE_DIRECTION_TOP);
                OH_ArkUI_RenderNodeUtils_SetRectShapeOptionEdgeValue(shape, right, ARKUI_EDGE_DIRECTION_RIGHT);
                OH_ArkUI_RenderNodeUtils_SetRectShapeOptionEdgeValue(shape, bottom, ARKUI_EDGE_DIRECTION_BOTTOM);
                ArkUI_RenderNodeMaskOption* mask = OH_ArkUI_RenderNodeUtils_CreateRenderNodeMaskOptionFromRectShape(shape);
                if (mask) {
                    maybeThrow(OH_ArkUI_RenderNodeUtils_SetMask(nodeHandle_, mask));
                    OH_ArkUI_RenderNodeUtils_DisposeRenderNodeMaskOption(mask);
                }
                OH_ArkUI_RenderNodeUtils_DisposeRectShapeOption(shape);
            }
            return this;
        }

        BaseRenderNode* setBounds(int32_t x, int32_t y, int32_t width, int32_t height) {
            maybeThrow(OH_ArkUI_RenderNodeUtils_SetBounds(nodeHandle_, x, y, width, height));
            return this;
        }

        BaseRenderNode* setPivot(float px, float py) {
            maybeThrow(OH_ArkUI_RenderNodeUtils_SetPivot(nodeHandle_, px, py));
            return this;
        }

        BaseRenderNode* setOpacity(float opacity) {
            maybeThrow(OH_ArkUI_RenderNodeUtils_SetOpacity(nodeHandle_, opacity));
            return this;
        }

        BaseRenderNode* setBorderWidth(float borderWidth) {
            using BorderWidthOption = ScopedOption<ArkUI_NodeBorderWidthOption,
                    OH_ArkUI_RenderNodeUtils_CreateNodeBorderWidthOption,
                    OH_ArkUI_RenderNodeUtils_DisposeNodeBorderWidthOption>;
            BorderWidthOption borderWidthOption;
            OH_ArkUI_RenderNodeUtils_SetNodeBorderWidthOptionEdgeWidth(borderWidthOption, borderWidth, ARKUI_EDGE_DIRECTION_ALL);
            maybeThrow(OH_ArkUI_RenderNodeUtils_SetBorderWidth(nodeHandle_, borderWidthOption));
            return this;
        }

        BaseRenderNode* setBorderColor(uint32_t borderColors) {
            using BorderColorOption = ScopedOption<ArkUI_NodeBorderColorOption,
                    OH_ArkUI_RenderNodeUtils_CreateNodeBorderColorOption,
                    OH_ArkUI_RenderNodeUtils_DisposeNodeBorderColorOption>;
            BorderColorOption colorOption;
            OH_ArkUI_RenderNodeUtils_SetNodeBorderColorOptionEdgeColor(colorOption, borderColors, ARKUI_EDGE_DIRECTION_ALL);
            maybeThrow(OH_ArkUI_RenderNodeUtils_SetBorderColor(nodeHandle_, colorOption));
            return this;
        }

        BaseRenderNode* setBorderCornerRadius(uint32_t cornerRadius) {
            using BorderRadiusOption = ScopedOption<ArkUI_NodeBorderRadiusOption,
                    OH_ArkUI_RenderNodeUtils_CreateNodeBorderRadiusOption,
                    OH_ArkUI_RenderNodeUtils_DisposeNodeBorderRadiusOption>;
            BorderRadiusOption borderRadius;
            OH_ArkUI_RenderNodeUtils_SetNodeBorderRadiusOptionCornerRadius(borderRadius, cornerRadius, ARKUI_CORNER_DIRECTION_ALL);
            maybeThrow(OH_ArkUI_RenderNodeUtils_SetBorderRadius(nodeHandle_, borderRadius));
            return this;
        }

        BaseRenderNode* setBackgroundColor(uint32_t backgroundColor) {
            maybeThrow(OH_ArkUI_RenderNodeUtils_SetBackgroundColor(nodeHandle_, backgroundColor));
            return this;
        }

        uint32_t getHash() const {
            return hash_;
        }

        void setHostingHash(uint32_t hostingHash) {
            hostingHash_ = hostingHash;
        }

    protected:
        virtual void initModifier() {};
        static uint32_t generateHash() {
            static std::atomic<uint32_t> counter{0};
            return ++counter;
        }

        void maybeThrow(int32_t status) {
            if (status != ARKUI_ERROR_CODE_NO_ERROR) {
                LOGE("OHRenderNode operation failed with status: %{public}d", status);
                throw std::runtime_error("OHRenderNode operation failed");
            }
        }

        uint32_t hash_;
        uint32_t hostingHash_ = 0;
        ArkUI_RenderNodeHandle nodeHandle_;
        BaseRenderNode* parent_ = nullptr;
    };
} // namespace OH
#endif