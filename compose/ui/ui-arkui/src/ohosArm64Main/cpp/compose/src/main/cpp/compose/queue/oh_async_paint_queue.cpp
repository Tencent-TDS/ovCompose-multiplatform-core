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

#include "oh_async_paint_queue.h"
#include "../xcomponent_log.h"
#include "../trace/oh_systrace_section.h"
#include <pthread.h>
#include <sched.h>

namespace OH {

AsyncPaintQueue::AsyncPaintQueue() : running_(true) {
    LOGI("AsyncPaintQueue: Initializing background thread");
    
    // 启动后台工作线程
    workerThread_ = std::thread(&AsyncPaintQueue::workerThreadFunc, this);
    
    // 设置线程名称（便于调试）
    pthread_setname_np(workerThread_.native_handle(), "AsyncPaintQueue");
    
    // 设置线程优先级为高优先级
    // 参考 iOS 的 DISPATCH_QUEUE_PRIORITY_HIGH
    struct sched_param param;
    param.sched_priority = sched_get_priority_max(SCHED_OTHER);
    if (pthread_setschedparam(workerThread_.native_handle(), SCHED_OTHER, &param) != 0) {
        LOGE("AsyncPaintQueue: Failed to set thread priority, continuing with default priority");
    }
}

AsyncPaintQueue::~AsyncPaintQueue() {
    LOGI("AsyncPaintQueue: Shutting down background thread");
    
    // 设置运行标志为 false
    running_ = false;
    
    // 唤醒工作线程
    cv_.notify_one();
    
    // 等待工作线程结束
    if (workerThread_.joinable()) {
        workerThread_.join();
    }
    
    LOGI("AsyncPaintQueue: Background thread shut down complete");
}

AsyncPaintQueue& AsyncPaintQueue::getInstance() {
    // C++11 保证静态局部变量的线程安全初始化
    static AsyncPaintQueue instance;
    return instance;
}

void AsyncPaintQueue::submitTask(std::function<void()> task) {
    if (!task) {
        LOGE("AsyncPaintQueue::submitTask: task is null, ignoring");
        return;
    }
    
    {
        std::lock_guard<std::mutex> lock(queueMutex_);
        taskQueue_.push(std::move(task));
    }
    
    // 唤醒工作线程
    cv_.notify_one();
}

size_t AsyncPaintQueue::getTaskCount() {
    std::lock_guard<std::mutex> lock(queueMutex_);
    return taskQueue_.size();
}

void AsyncPaintQueue::workerThreadFunc() {
    LOGI("AsyncPaintQueue: Worker thread started");
    
    while (running_) {
        std::function<void()> task;
        
        {
            std::unique_lock<std::mutex> lock(queueMutex_);
            
            // 等待任务或退出信号
            cv_.wait(lock, [this] {
                return !taskQueue_.empty() || !running_;
            });
            
            // 检查是否需要退出
            if (!running_ && taskQueue_.empty()) {
                break;
            }
            
            // 取出任务
            if (!taskQueue_.empty()) {
                task = std::move(taskQueue_.front());
                taskQueue_.pop();
            }
        }
        
        // 执行任务（在锁外执行，避免阻塞队列）
        if (task) {
            try {
                OH::SystraceSection trace("AsyncPaintQueue:executeTask");
                task();
            } catch (const std::exception& e) {
                LOGE("AsyncPaintQueue: Task execution failed with exception: %{public}s", e.what());
            } catch (...) {
                LOGE("AsyncPaintQueue: Task execution failed with unknown exception");
            }
        }
    }
    
    LOGI("AsyncPaintQueue: Worker thread exiting");
}

} // namespace OH


