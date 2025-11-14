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

package androidx.compose.ui.platform.nativefoundation

import kotlin.native.ref.createCleaner

/**
 * Native资源持有者基类
 * 
 * 封装cleaner机制，统一管理Native资源的生命周期。
 * 所有需要在UI线程释放的Native资源都应该继承或使用此类。
 * 
 * 设计理念：
 * - 统一封装cleaner逻辑，避免代码重复
 * - 使用泛型保证类型安全
 * - 自动处理资源释放的线程安全问题
 * - 提供显式的dispose()方法用于立即释放
 * 
 * @param T Native句柄类型（必须是Any的子类型，因为cleaner需要引用）
 * @param handle Native资源句柄，可能为null
 * @param releaseFunction 释放函数，在主线程调用时执行实际的资源释放
 * 
 * 使用示例：
 * ```kotlin
 * class OHNativeCanvasProxy(handle: OHNativeCanvasProxy_Handle?) 
 *     : NativeResourceHolder<OHNativeCanvasProxy_Handle>(
 *         handle,
 *         ::androidx_compose_ui_arkui_utils_DisposeOHNativeCanvasProxy
 *     ) {
 *     // 可以直接使用 handle 属性访问资源
 *     fun someMethod() {
 *         handle?.let { /* 使用资源 */ }
 *     }
 * }
 * ```
 */
abstract class NativeResourceHolder<T : Any>(
    val handle: T?,
    private val releaseFunction: (T) -> Unit
) {
    /**
     * Cleaner payload，用于跟踪资源是否已释放
     * 防止重复释放和竞态条件
     */
    private class CleanerPayload<T : Any>(
        val handle: T?,
        val releaseFunction: (T) -> Unit
    ) {
        var disposed: Boolean = false
    }

    private val cleanerPayload = CleanerPayload(handle, releaseFunction)

    /**
     * 自动资源清理器
     * 
     * 注意：createCleaner的回调运行在Kotlin/Native的清理工作线程上，不能保证是主线程。
     * 这里不直接销毁，而是将释放任务放入队列，等待主线程主动flush。
     */
    @Suppress("unused")
    private val cleaner = createCleaner(cleanerPayload) { payload ->
        if (!payload.disposed && payload.handle != null) {
            NativeResourceReleaser.enqueue {
                // 双重检查，确保在主线程执行时资源还未被释放
                if (!payload.disposed) {
                    payload.disposed = true
                    payload.releaseFunction(payload.handle)
                }
            }
        }
    }

    /**
     * 显式释放方法：在明确生命周期结束且当前线程为主线程时调用
     * 
     * 调用后：
     * 1. 标记已释放，防止cleaner再次尝试释放
     * 2. 立即执行销毁（假设当前在UI线程）
     * 
     * 注意：此方法假设调用者确保当前在UI线程。如果不在UI线程，应该依赖cleaner机制。
     */
    fun dispose() {
        if (!cleanerPayload.disposed && cleanerPayload.handle != null) {
            cleanerPayload.disposed = true
            cleanerPayload.releaseFunction(cleanerPayload.handle)
        }
    }
}

