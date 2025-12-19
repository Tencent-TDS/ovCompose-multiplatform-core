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

#ifndef OH_ASYNC_PAINT_QUEUE_H
#define OH_ASYNC_PAINT_QUEUE_H

#include <functional>
#include <queue>
#include <thread>
#include <mutex>
#include <condition_variable>
#include <atomic>

namespace OH {

/**
 * AsyncPaintQueue - 异步绘制队列
 * 参考 iOS TMMAsyncTaskLayer 的 TextAsyncPaintQueue() 实现
 *
 * 职责：
 * 1. 提供全局单例的后台线程队列
 * 2. 串行执行异步绘制任务（FIFO 顺序）
 * 3. 后台线程优先级设置为高优先级
 *
 * 线程模型：
 * - 单个后台线程（类似 iOS 的 DISPATCH_QUEUE_SERIAL）
 * - 优先级为高优先级（对应 iOS 的 DISPATCH_QUEUE_PRIORITY_HIGH）
 * - 使用生产者-消费者模式实现任务队列
 *
 * 使用场景：
 * - 文本异步绘制（避免首帧渲染阻塞）
 * - 复杂图像处理（避免主线程卡顿）
 */
class AsyncPaintQueue {
public:
    /**
     * 获取单例实例
     */
    static AsyncPaintQueue &getInstance();

    /**
     * 提交任务到后台队列
     *
     * @param task 异步任务（将在后台线程执行）
     */
    void submitTask(std::function<void()> task);

    /**
     * 获取队列中待处理的任务数量
     */
    size_t getTaskCount();

    // 禁止拷贝和赋值
    AsyncPaintQueue(const AsyncPaintQueue &) = delete;
    AsyncPaintQueue &operator=(const AsyncPaintQueue &) = delete;

private:
    AsyncPaintQueue();
    ~AsyncPaintQueue();

    /**
     * 后台线程工作函数
     */
    void workerThreadFunc();

    // 后台工作线程
    std::thread workerThread_;

    // 任务队列及其保护锁
    std::queue<std::function<void()>> taskQueue_;
    std::mutex queueMutex_;
    std::condition_variable cv_;

    // 运行标志
    std::atomic<bool> running_;
};

} // namespace OH

#endif // OH_ASYNC_PAINT_QUEUE_H



