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

#include "oh_async_task_render_node.h"
#include "../xcomponent_log.h"
#include "../trace/oh_systrace_section.h"
#include "../queue/oh_async_paint_queue.h"
#include <native_drawing/drawing_canvas.h>
#include <native_drawing/drawing_pixel_map.h>
#include <native_drawing/drawing_rect.h>
#include <native_drawing/drawing_sampling_options.h>
#include <cfloat>

namespace OH {

AsyncTaskRenderNode::~AsyncTaskRenderNode() {
    if (invalidateCountProperty_) {
        OH_ArkUI_RenderNodeUtils_DisposeFloatProperty(invalidateCountProperty_);
    }
    if (modifier_) {
        OH_ArkUI_RenderNodeUtils_DisposeContentModifier(modifier_);
    }
    // Note: pixelMap_ is managed by cache system, so we don't release it here
}

AsyncTaskRenderNode::AsyncTaskRenderNode() {
    this->AsyncTaskRenderNode::initModifier();
}

OH_DrawingNode_Type AsyncTaskRenderNode::getType() {
    return OH_DrawingNode_Type::AsyncTaskNode;
}

void AsyncTaskRenderNode::commitAsyncTask(std::function<int64_t()> asyncTask, int32_t width, int32_t height,
                                          std::function<void(int64_t)> onMainThreadUpdate) {
    OH::SystraceSection trace("AsyncTaskRenderNode::commitAsyncTask");

    // 保存参数
    width_ = width;
    height_ = height;

    // 提交任务到后台队列（参考 iOS TMMAsyncTaskLayer::commitAsyncTask）
    AsyncPaintQueue::getInstance().submitTask([asyncTask, onMainThreadUpdate]() {
        OH::SystraceSection trace("AsyncTaskRenderNode::backgroundTask");

        // 在后台线程执行 globalTask
        int64_t pixelMapPtr = asyncTask();

        // 调用回调，Kotlin侧会post到主线程
        // 等价于iOS的: dispatch_async(dispatch_get_main_queue(), ^{ ... })
        onMainThreadUpdate(pixelMapPtr);
    });
}

void AsyncTaskRenderNode::updatePixelMapAndInvalidate(int64_t pixelMapPtr) {
    OH::SystraceSection trace("AsyncTaskRenderNode::updatePixelMapAndInvalidate");

    // 此方法必须在主线程调用！
    // 由Kotlin侧的Dispatchers.Main确保
    if (pixelMapPtr == 0) {
        LOGE("AsyncTaskRenderNode::updatePixelMapAndInvalidate: pixelMapPtr is 0");
        return;
    }

    OH_PixelmapNative *pixelMap = reinterpret_cast<OH_PixelmapNative *>(pixelMapPtr);

    // 更新pixelMap_（主线程，无需mutex）
    pixelMap_ = pixelMap;

    // 触发重绘（主线程调用ContentModifier API）
    // 关键：检查资源是否已被 dispose（可能在析构函数中已被清理）
    // 如果已被 dispose，安全返回，避免 crash
    if (!invalidateCountProperty_) {
        LOGI("AsyncTaskRenderNode::invalidate: invalidateCountProperty_ already disposed, skipping");
        return;
    }
    invalidate();
}

void AsyncTaskRenderNode::invalidate() {

    // 读取当前值
    float currentCount = 0.0f;
    OH_ArkUI_RenderNodeUtils_GetFloatPropertyValue(invalidateCountProperty_, &currentCount);

    // 加1，处理溢出（回绕到0）
    float newCount = (currentCount >= FLT_MAX - 1.0f) ? 0.0f : (currentCount + 1.0f);

    // 设置新值，触发 onDraw 回调
    OH_ArkUI_RenderNodeUtils_SetFloatPropertyValue(invalidateCountProperty_, newCount);
}

void AsyncTaskRenderNode::initModifier() {
    if (!modifier_) {
        modifier_ = OH_ArkUI_RenderNodeUtils_CreateContentModifier();
        maybeThrow(OH_ArkUI_RenderNodeUtils_AttachContentModifier(nodeHandle_, modifier_));

        // 创建invalidateCount PropertyHandle
        invalidateCountProperty_ = OH_ArkUI_RenderNodeUtils_CreateFloatProperty(0.0f);
        maybeThrow(OH_ArkUI_RenderNodeUtils_AttachFloatProperty(modifier_, invalidateCountProperty_));

        maybeThrow(OH_ArkUI_RenderNodeUtils_SetContentModifierOnDraw(
            modifier_, this, [](ArkUI_DrawContext *context, void *userData) {
                auto *node = static_cast<AsyncTaskRenderNode *>(userData);
                auto *canvas1 = OH_ArkUI_DrawContext_GetCanvas(context);
                auto *canvas = static_cast<OH_Drawing_Canvas *>(canvas1);

                // onDraw回调在主线程执行，直接读取成员变量（无需mutex）
                OH_PixelmapNative *pixelMap = node->pixelMap_;

                if (pixelMap == nullptr) {
                    LOGI("AsyncTaskRenderNode::onDraw: pixelMap is null, skipping");
                    return;
                }

                // 绘制 PixelMap 到 Canvas
                // 源矩形：整个 PixelMap（从0,0开始）
                const float srcLeft = 0.0f;
                const float srcTop = 0.0f;
                const float srcRight = static_cast<float>(node->width_);
                const float srcBottom = static_cast<float>(node->height_);

                // 目标矩形：RenderNode 的整个区域（局部坐标，已考虑 density）
                const float dstLeft = 0.0f;
                const float dstTop = 0.0f;
                const float dstRight = static_cast<float>(node->width_) / node->density_;
                const float dstBottom = static_cast<float>(node->height_) / node->density_;

                // 创建源矩形和目标矩形
                OH_Drawing_Rect *srcRect = OH_Drawing_RectCreate(srcLeft, srcTop, srcRight, srcBottom);
                OH_Drawing_Rect *dstRect = OH_Drawing_RectCreate(dstLeft, dstTop, dstRight, dstBottom);

                // 创建 SamplingOptions（使用线性过滤）
                OH_Drawing_SamplingOptions *samplingOptions =
                    OH_Drawing_SamplingOptionsCreate(OH_Drawing_FilterMode::FILTER_MODE_LINEAR,
                                                     OH_Drawing_MipmapMode::MIPMAP_MODE_NONE);

                // 转换 OH_PixelmapNative 到 OH_Drawing_PixelMap
                OH_Drawing_PixelMap *drawingPixelMap = OH_Drawing_PixelMapGetFromOhPixelMapNative(pixelMap);

                if (drawingPixelMap == nullptr) {
                    LOGE("AsyncTaskRenderNode::onDraw: failed to convert OH_PixelmapNative to OH_Drawing_PixelMap");
                    OH_Drawing_RectDestroy(srcRect);
                    OH_Drawing_RectDestroy(dstRect);
                    OH_Drawing_SamplingOptionsDestroy(samplingOptions);
                    return;
                }

                // 绘制图像
                OH_Drawing_CanvasDrawPixelMapRect(canvas, drawingPixelMap, srcRect, dstRect, samplingOptions);

                // 释放资源
                OH_Drawing_RectDestroy(srcRect);
                OH_Drawing_RectDestroy(dstRect);
                OH_Drawing_SamplingOptionsDestroy(samplingOptions);
            }));
    }
}

} // namespace OH
