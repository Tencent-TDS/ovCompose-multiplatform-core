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

#include <window_manager/oh_display_manager.h>
#include <arkui/native_render.h>
#include "../xcomponent_log.h"
#include "oh_native_canvas_proxy.h"
#include "../utils/oh_hash_funcs.h"
#include "../constants/oh_native_enums.h"
#include "oh_native_canvas_layer_drawer.h"
#include "../trace/oh_systrace_section.h"

namespace androidx::compose::ui::arkui::utils {
    OHNativeCanvasProxy::OHNativeCanvasProxy(OH::BaseRenderNode *rootNode) : rootNode_(rootNode) {
        ArkUI_RenderNodeHandle renderNodeHandle = OH_ArkUI_RenderNodeUtils_CreateNode();
        canvasNode_ = std::make_unique<OH::BaseRenderNode>(renderNodeHandle);
    }

    OHComposeNativePaint *OHNativeCanvasProxy::Paint() {
        if (paint_ == nullptr) {
            paint_ = new OHComposeNativePaint();
        }
        return paint_;
    }

    void OHNativeCanvasProxy::beginDraw() {
        LOGI("OHNativeCanvasProxy::BeginDraw: start");
        _pictureRecorder.startRecording(*canvasNode_);
    }

    void OHNativeCanvasProxy::attachToRootView() {
        LOGI("OHNativeCanvasProxy::attachToRootView: start");
        if (canvasNode_->getParent() != rootNode_) {
            rootNode_->addChild(canvasNode_.get());
        }
    }

    void OHNativeCanvasProxy::setParent(OHNativeCanvasProxy *canvasParentProxy) {
        LOGI("OHNativeCanvasProxy::setParent: start");
        OH::BaseRenderNode *parentNode = canvasParentProxy->getRenderNode();
        if (canvasNode_->getParent() != parentNode) {
            // parentNode->addChild(canvasNode_.get());
            canvasNode_->setParent(parentNode);
        }
    }

    void OHNativeCanvasProxy::setPosition(int32_t x, int32_t y) {
        LOGI("OHNativeCanvasProxy::setPosition: start");
        if (canvasNode_ != nullptr) {
            canvasNode_->setPosition(x, y);
        }
    }

    void OHNativeCanvasProxy::setBounds(int32_t originX, int32_t originY, int32_t boundsWidth, int32_t boundsHeight) {
        LOGI("OHNativeCanvasProxy::setBounds: start");
        if (canvasNode_ != nullptr) {
            canvasNode_->setBounds(originX, originY, boundsWidth, boundsHeight);
        }
    }

    void OHNativeCanvasProxy::setPivot(float px, float py) {
        LOGI("OHNativeCanvasProxy::setPivot: start");
        if (canvasNode_ != nullptr) {
            canvasNode_->setPivot(px, py);
        }
    }

    void OHNativeCanvasProxy::setOpacity(float opacity) {
        LOGI("OHNativeCanvasProxy::setOpacity: start");
        if (canvasNode_ != nullptr) {
            canvasNode_->setOpacity(opacity);
        }
    }

    void OHNativeCanvasProxy::clipRect(float left, float top, float right, float bottom, OH_Native_Draw_ClipOp clipOp) {
        LOGI("OHNativeCanvasProxy::clipRect: start");
        const uint64_t drawingContentHash = OH::hashCombineSequential(left, top, right, bottom, static_cast<float>(clipOp));
        OH::PictureRecorderUpdateInfo updateItem = _pictureRecorder.clip(drawingContentHash);
        bool isDirty = updateItem.isDirty;
        if (isDirty) {
            OH::BaseRenderNode *renderNodeForDrawing = _pictureRecorder.getOrCreateRenderNodeForDrawing(updateItem.drawingType, updateItem.itemHash);
            OH::OHRenderNodeDrawClipRect(left, top, right, bottom, &(updateItem.saveState), renderNodeForDrawing);
        }
    }

    void OHNativeCanvasProxy::save() {
        _pictureRecorder.save();
    }

    void OHNativeCanvasProxy::restore() {
        _pictureRecorder.restore();
    }

    void OHNativeCanvasProxy::translate(float dx, float dy) {
        _pictureRecorder.translate(dx, dy);
    }

    void OHNativeCanvasProxy::drawLayerWithSubproxy(OHNativeCanvasProxy* subproxy) {
        LOGI("OHNativeCanvasProxy::drawLayerWithSubproxy: start");
        if (subproxy->canvasNode_ != nullptr) {
            _pictureRecorder.drawRenderNode(subproxy->canvasNode_.get());
        }
        
    }

    OH::BaseRenderNode *OHNativeCanvasProxy::getRenderNode() {
        LOGI("OHNativeCanvasProxy::getRenderNode: start");
        return canvasNode_.get();
    }

    void OHNativeCanvasProxy::finishDraw() {
        LOGI("OHNativeCanvasProxy::finishDraw: start");
        _pictureRecorder.finishRecording(*canvasNode_);
    }

    void OHNativeCanvasProxy::drawRect(float left, float top, float right, float bottom, OHComposeNativePaint* paint) {
        LOGI("OHNativeCanvasProxy::drawRect: start");
        OH::SystraceSection("OHNativeCanvasProxy::drawRect");
        //TODO:需要paint
        OH::NativeBasicShader* shader = paint->shader;
        const OH_Native_Drawing_Type drawingType = shader ? OH_Native_Drawing_Type::DrawingTypeShaderRect : OH_Native_Drawing_Type::DrawingTypeRect;
        //TODO:需要paint
//    const uint64_t preHash = hashMerge(TMMNativeDataHashFromPaint(paint), drawingType);
        const uint64_t preHash = 0;
        const uint64_t drawingContentHash = OH::hashCombineSequential(left, top, right, bottom, static_cast<float>(preHash));
        OH::PictureRecorderUpdateInfo updateItem = _pictureRecorder.draw(drawingType, drawingContentHash);
        bool isDirty = updateItem.isDirty;
        OH::BaseRenderNode *renderNodeForDrawing = nullptr;
        if (isDirty) {
            renderNodeForDrawing = _pictureRecorder.getOrCreateRenderNodeForDrawing(updateItem.drawingType, updateItem.itemHash);
            OH::OHRenderNodeDrawRect(left, top, right, bottom, shader, &(updateItem.saveState), renderNodeForDrawing, paint);
        }
    }

    void OHNativeCanvasProxy::drawRoundRect(float left, float top, float right, float bottom, float radiusX, float radiusY, OHComposeNativePaint* paint) {
        LOGI("OHNativeCanvasProxy::drawRoundRect: start");
        OH::NativeBasicShader* shader = paint->shader;
        const OH_Native_Drawing_Type drawingType = shader ? OH_Native_Drawing_Type::DrawingTypeShaderRect : OH_Native_Drawing_Type::DrawingTypeRect;
        //    const uint64_t preHash = hashMerge(TMMNativeDataHashFromPaint(paint), drawingType);
                const uint64_t preHash = 0;
        const uint64_t drawingContentHash = OH::hashCombineSequential(left, top, right, bottom, static_cast<float>(preHash));
        OH::PictureRecorderUpdateInfo updateItem = _pictureRecorder.draw(drawingType, drawingContentHash);
        bool isDirty = updateItem.isDirty;
        OH::BaseRenderNode *renderNodeForDrawing = nullptr;
        if (isDirty) {
            renderNodeForDrawing = _pictureRecorder.getOrCreateRenderNodeForDrawing(updateItem.drawingType, updateItem.itemHash);
            OH::OHRenderNodeDrawRoundRect(left, top, right, bottom, radiusX, radiusY, shader, &(updateItem.saveState), renderNodeForDrawing, paint);
        }
    }
    
    void OHNativeCanvasProxy::drawLine(float x1, float y1, float x2, float y2, OHComposeNativePaint* paint) {
        LOGI("OHNativeCanvasProxy::drawLine: start");
        //TODO:需要paint
        //TMMNativeBasicShader *shader = [paint shader];
        const OH_Native_Drawing_Type drawingType = paint->shader ? OH_Native_Drawing_Type::DrawingTypeShaderLine : OH_Native_Drawing_Type::DrawingTypeLine;
        //const uint64_t preHash = hashMerge(TMMNativeDataHashFromPaint(paint), drawingType);
        const uint64_t preHash = 0;
        const uint64_t drawingContentHash = OH::hashCombineSequential(x1, y1, x2, y2, static_cast<float>(preHash));
        OH::PictureRecorderUpdateInfo updateItem = _pictureRecorder.draw(drawingType, drawingContentHash);

        bool isDirty = updateItem.isDirty;
        OH::BaseRenderNode *renderNodeForDrawing = nullptr;
        if (isDirty) {
            renderNodeForDrawing = _pictureRecorder.getOrCreateRenderNodeForDrawing(updateItem.drawingType, updateItem.itemHash);
            OH::OHRenderNodeDrawLine(x1, y1, x2, y2, paint->shader, &(updateItem.saveState), renderNodeForDrawing, paint);
        }
    }

    void OHNativeCanvasProxy::drawLayer(OH::BaseRenderNode* renderNode) {
        //TODO：ios上传入了CALayer，oh需要对应的renderNode？
        _pictureRecorder.drawRenderNode(renderNode);
    };

    OHNativeCanvasProxy::~OHNativeCanvasProxy() {
        if (paint_ != nullptr) {
            delete paint_;
            paint_ = nullptr;
        }
    }
} // namespace androidx::compose::ui::arkui::utils
