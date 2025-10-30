/*
 * Tencent is pleased to support the open source community by making ovCompose available.
 * Copyright (C) 2025 Tencent. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "oh_native_canvas_proxy.h"
#include <arkui/native_render.h>
#include "../constants/oh_native_enums.h"
#include "../paragraph/oh_native_paragraph.h"
#include "../trace/oh_systrace_section.h"
#include "../utils/oh_hash_funcs.h"
#include "../xcomponent_log.h"
#include "oh_native_canvas_layer_drawer.h"

namespace androidx::compose::ui::arkui::utils {
OHNativeCanvasProxy::OHNativeCanvasProxy(OH::BaseRenderNode* rootNode) : rootNode_(rootNode) {
    ArkUI_RenderNodeHandle renderNodeHandle = OH_ArkUI_RenderNodeUtils_CreateNode();
    canvasNode_ = std::make_unique<OH::BaseRenderNode>(renderNodeHandle);
}

OHComposeNativePaint* OHNativeCanvasProxy::Paint() {
    if (paint_ == nullptr) {
        paint_ = new OHComposeNativePaint();
    }
    return paint_;
}

void OHNativeCanvasProxy::beginDraw() {
    LOGI("OHNativeCanvasProxy::BeginDraw: start");
    _pictureRecorder.startRecording(*canvasNode_);
}

void OHNativeCanvasProxy::attachToRootView() const {
    LOGI("OHNativeCanvasProxy::attachToRootView: start");
    if (canvasNode_->getParent() != rootNode_) {
        rootNode_->addChild(canvasNode_.get());
    }
}

void OHNativeCanvasProxy::setParent(const OHNativeCanvasProxy* canvasParentProxy) const {
    LOGI("OHNativeCanvasProxy::setParent: start");
    if (OH::BaseRenderNode* parentNode = canvasParentProxy->getRenderNode(); canvasNode_->getParent() != parentNode) {
        canvasNode_->setParent(parentNode);
    }
}

void OHNativeCanvasProxy::setPosition(const int32_t x, const int32_t y) const {
    LOGI("OHNativeCanvasProxy::setPosition: start");
    if (canvasNode_ != nullptr) {
        canvasNode_->setPosition(x, y);
    }
}

void OHNativeCanvasProxy::setBounds(const int32_t originX, const int32_t originY, const int32_t boundsWidth,
                                    const int32_t boundsHeight) const {
    LOGI("OHNativeCanvasProxy::setBounds: start");
    if (canvasNode_ != nullptr) {
        canvasNode_->setBounds(originX, originY, boundsWidth, boundsHeight);
    }
}

void OHNativeCanvasProxy::setPivot(const float px, const float py) const {
    LOGI("OHNativeCanvasProxy::setPivot: start");
    if (canvasNode_ != nullptr) {
        canvasNode_->setPivot(px, py);
    }
}

void OHNativeCanvasProxy::setOpacity(const float opacity) const {
    LOGI("OHNativeCanvasProxy::setOpacity: start");
    if (canvasNode_ != nullptr) {
        canvasNode_->setOpacity(opacity);
    }
}

void OHNativeCanvasProxy::clipRect(const float left, const float top, const float right, const float bottom,
                                   const OH_Native_Draw_ClipOp clipOp) {
    LOGI("OHNativeCanvasProxy::clipRect: start");
    const uint64_t drawingContentHash = OH::hashCombineSequential(left, top, right, bottom, static_cast<float>(clipOp));
    OH::PictureRecorderUpdateInfo updateItem = _pictureRecorder.clip(drawingContentHash);
    if (updateItem.isDirty) {
        OH::BaseRenderNode* renderNodeForDrawing =
            _pictureRecorder.getOrCreateRenderNodeForDrawing(updateItem.drawingType, updateItem.itemHash);
        OH::OHRenderNodeDrawClipRect(left, top, right, bottom, &(updateItem.saveState), renderNodeForDrawing);
    }
}

void OHNativeCanvasProxy::save() {
    _pictureRecorder.save();
}

void OHNativeCanvasProxy::restore() {
    _pictureRecorder.restore();
}

void OHNativeCanvasProxy::translate(const float dx, const float dy) {
    _pictureRecorder.translate(dx, dy);
}

void OHNativeCanvasProxy::drawLayerWithSubproxy(const OHNativeCanvasProxy* subProxy) {
    LOGI("OHNativeCanvasProxy::drawLayerWithSubproxy: start");
    if (subProxy->canvasNode_ != nullptr) {
        const auto canvasNode = subProxy->canvasNode_.get();
        _pictureRecorder.drawRenderNode(canvasNode, canvasNode->getType());
    }
}

OH::BaseRenderNode* OHNativeCanvasProxy::getRenderNode() const {
    LOGI("OHNativeCanvasProxy::getRenderNode: start");
    return canvasNode_.get();
}

void OHNativeCanvasProxy::finishDraw() {
    LOGI("OHNativeCanvasProxy::finishDraw: start");
    _pictureRecorder.finishRecording(*canvasNode_);
}

void OHNativeCanvasProxy::drawRect(const float left, const float top, const float right, const float bottom,
                                   OHComposeNativePaint* paint) {
    LOGI("OHNativeCanvasProxy::drawRect: start");
    // OH::SystraceSection("OHNativeCanvasProxy::drawRect");
    OH::NativeBasicShader* shader = paint->shader;
    const OH_Native_Drawing_Type drawingType =
        shader ? OH_Native_Drawing_Type::DrawingTypeShaderRect : OH_Native_Drawing_Type::DrawingTypeRect;
    const uint64_t preHash = OH::hashMerge(OH::nativeDataHashFromPaint(paint), drawingType);
    const uint64_t drawingContentHash =
        OH::hashCombineSequential(left, top, right, bottom, static_cast<float>(preHash));
    OH::PictureRecorderUpdateInfo updateItem = _pictureRecorder.draw(drawingType, drawingContentHash);
    if (updateItem.isDirty) {
        OH::BaseRenderNode* renderNodeForDrawing = nullptr;
        renderNodeForDrawing =
            _pictureRecorder.getOrCreateRenderNodeForDrawing(updateItem.drawingType, updateItem.itemHash);
        OH::OHRenderNodeDrawRect(left, top, right, bottom, shader, &(updateItem.saveState), renderNodeForDrawing,
                                 paint);
    }
}

void OHNativeCanvasProxy::drawRoundRect(const float left, const float top, const float right, const float bottom,
                                        const float radiusX, const float radiusY, OHComposeNativePaint* paint) {
    LOGI("OHNativeCanvasProxy::drawRoundRect: start");
    OH::NativeBasicShader* shader = paint->shader;
    const OH_Native_Drawing_Type drawingType =
        shader ? OH_Native_Drawing_Type::DrawingTypeShaderRect : OH_Native_Drawing_Type::DrawingTypeRect;
    const uint64_t preHash = OH::hashMerge(OH::nativeDataHashFromPaint(paint), drawingType);
    const uint64_t drawingContentHash =
        OH::hashCombineSequential(left, top, right, bottom, static_cast<float>(preHash));
    OH::PictureRecorderUpdateInfo updateItem = _pictureRecorder.draw(drawingType, drawingContentHash);
    if (updateItem.isDirty) {
        OH::BaseRenderNode* renderNodeForDrawing = nullptr;
        renderNodeForDrawing =
            _pictureRecorder.getOrCreateRenderNodeForDrawing(updateItem.drawingType, updateItem.itemHash);
        OH::OHRenderNodeDrawRoundRect(left, top, right, bottom, radiusX, radiusY, shader, &(updateItem.saveState),
                                      renderNodeForDrawing, paint);
    }
}

void OHNativeCanvasProxy::drawLine(const float x1, const float y1, const float x2, const float y2,
                                   OHComposeNativePaint* paint) {
    LOGI("OHNativeCanvasProxy::drawLine: start");
    const OH_Native_Drawing_Type drawingType =
        paint->shader ? OH_Native_Drawing_Type::DrawingTypeShaderLine : OH_Native_Drawing_Type::DrawingTypeLine;
    const uint64_t preHash = OH::hashMerge(OH::nativeDataHashFromPaint(paint), drawingType);
    const uint64_t drawingContentHash = OH::hashCombineSequential(x1, y1, x2, y2, static_cast<float>(preHash));
    OH::PictureRecorderUpdateInfo updateItem = _pictureRecorder.draw(drawingType, drawingContentHash);

    if (updateItem.isDirty) {
        OH::BaseRenderNode* renderNodeForDrawing = nullptr;
        renderNodeForDrawing =
            _pictureRecorder.getOrCreateRenderNodeForDrawing(updateItem.drawingType, updateItem.itemHash);
        OH::OHRenderNodeDrawLine(x1, y1, x2, y2, paint->shader, &(updateItem.saveState), renderNodeForDrawing, paint);
    }
};

void OHNativeCanvasProxy::drawLayer(OH::BaseRenderNode* renderNode) {
    OH::PictureRecorderUpdateInfo updateItem = _pictureRecorder.drawRenderNode(renderNode, renderNode->getType());
};

void OHNativeCanvasProxy::drawParagraph(OH::Paragraph* paragraph) {
    LOGI("OHNativeCanvasProxy::drawParagraph: start %{public}f", paragraph->getHeight());
    OH::PictureRecorderUpdateInfo updateItem = _pictureRecorder.drawRenderNode(paragraph, paragraph->getType());
    if (updateItem.isDirty) {
        OH::OHRenderNodeDrawText(&(updateItem.saveState), paragraph);
    }
};

OHNativeCanvasProxy::~OHNativeCanvasProxy() = default;
}  // namespace androidx::compose::ui::arkui::utils
