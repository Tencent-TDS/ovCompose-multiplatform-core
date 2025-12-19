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

#ifndef OH_ASYNC_TASK_RENDER_NODE_H
#define OH_ASYNC_TASK_RENDER_NODE_H

#include "oh_base_render_node.h"
#include <multimedia/image_framework/image/pixelmap_native.h>
#include <functional>
#include <atomic>

namespace OH {

/**
 * AsyncTaskRenderNode - 异步任务渲染节点
 * 参考 iOS TMMAsyncTaskLayer 实现
 *
 * 职责：
 * 1. 接收异步绘制任务（globalTask），在后台线程执行
 * 2. 后台线程执行完成后，获取 PixelMap 指针
 * 3. 在主线程更新 ContentModifier 显示内容
 * 4. 用于文本异步绘制，避免阻塞主线程
 *
 * 使用场景：
 * - 文本首次渲染时字体加载耗时
 * - 复杂文本布局计算耗时
 * - 避免频繁淘汰缓存导致的重新渲染阻塞
 */
class AsyncTaskRenderNode : public BaseRenderNode {
public:
    ~AsyncTaskRenderNode() override;
    AsyncTaskRenderNode();

    /**
     * 提交异步绘制任务
     *
     * 线程模型：
     * 1. asyncTask 在后台线程（AsyncPaintQueue）执行
     * 2. asyncTask 完成后，调用 onMainThreadUpdate(pixelMapPtr)
     * 3. onMainThreadUpdate 会post到Kotlin侧的 Dispatchers.Main
     * 4. Kotlin主线程回调C++的 updatePixelMapAndInvalidate()
     *
     * @param asyncTask 异步任务，返回值为 PixelMap 指针 (int64_t)
     * @param width 绘制宽度（像素）
     * @param height 绘制高度（像素）
     * @param onMainThreadUpdate 后台任务完成后的回调，会post到Kotlin主线程再回调C++
     */
    void commitAsyncTask(std::function<int64_t()> asyncTask, int32_t width, int32_t height,
                         std::function<void(int64_t)> onMainThreadUpdate);

    /**
     * 在主线程更新PixelMap并触发重绘
     *
     * 必须在主线程（UI线程）调用！
     * 根据文档（CMP接入OHOS统一渲染架构设计文档.md:6499）：
     * "ContentModifier操作应在主线程（UI线程）进行"
     *
     * 调用流程：
     * - Kotlin侧通过Dispatchers.Main.dispatch { ... }确保在主线程
     * - 再通过JNI调用此方法
     *
     * @param pixelMapPtr 新的PixelMap指针
     */
    void updatePixelMapAndInvalidate(int64_t pixelMapPtr);

    OH_DrawingNode_Type getType() override;

private:
    void initModifier() override;

    /**
     * 触发重绘
     *
     * 必须在主线程调用（由updatePixelMapAndInvalidate确保）
     */
    void invalidate() override;

    // 成员变量
    OH_PixelmapNative *pixelMap_ = nullptr;
    int32_t width_ = 0;
    int32_t height_ = 0;
    float density_ = 1.0f;

    // ContentModifier 和 Property
    ArkUI_FloatPropertyHandle invalidateCountProperty_ = nullptr;
    ArkUI_RenderContentModifierHandle modifier_ = nullptr;
};

} // namespace OH

#endif // OH_ASYNC_TASK_RENDER_NODE_H
