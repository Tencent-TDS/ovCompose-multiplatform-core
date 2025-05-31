/*
 * Tencent is pleased to support the open source community by making ovCompose available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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

package androidx.compose.ui.arkui.backhandler

import androidx.annotation.MainThread
import androidx.compose.ui.arkui.messenger.MessengerOwner
import androidx.compose.ui.arkui.messenger.send
import androidx.compose.ui.util.fastForEachReversed


typealias Cancel = () -> Unit

/**
 * 提供返回事件分发器实例的组件接口，通常由根界面组件实现。
 *
 * 实现此接口的类应负责将系统返回事件路由到其[onBackPressedDispatcher]，
 * 并确保分发器能够正确处理组件树各层级的返回事件。
 *
 * ### 实现要求
 * - 应优先处理最近注册的回调
 * - 当无回调消费事件时执行默认回退逻辑
 *
 * @see OnBackPressedDispatcher 返回事件分发机制的核心实现
 * @author gavinbaoliu
 * @since 2025/3/3
 */
interface OnBackPressedDispatcherOwner {

    /**
     * 用于处理系统返回按钮事件的分发器。
     */
    val onBackPressedDispatcher: OnBackPressedDispatcher
}

/**
 * 处理系统返回按钮事件的分发控制器，协调多个回调之间的优先级和消费关系。
 *
 * ### 实现要求
 * - 回调执行顺序应为后注册先回调
 * - 当任一回调返回`true`时应终止事件传递
 *
 * @see OnBackPressedDispatcherOwner 提供分发器实例的宿主接口
 */
interface OnBackPressedDispatcher {

    /**
     * 触发返回事件分发流程。
     *
     * @return `true` 表示事件已被消费，`false` 表示事件未被消费
     */
    @MainThread
    fun onBackPressed(): Boolean

    /**
     * 注册返回事件回调，并返回用于取消注册的函数。
     *
     * 示例：
     * ```
     * val cancel = dispatcher.addOnBackPressedCallback {
     *     if (shouldHandleBack) {
     *         // 处理逻辑
     *         true
     *     } else {
     *         false
     *     }
     * }
     * // 取消注册
     * cancel()
     * ```
     *
     * @param callback 当返回事件发生时触发的回调，返回true表示已消费事件
     * @return 用于取消注册的函数。
     */
    @MainThread
    fun addOnBackPressedCallback(callback: () -> Boolean): Cancel
}

internal class PlatformOnBackPressedDispatcher(
    messenger: MessengerOwner
) : OnBackPressedDispatcher, MessengerOwner by messenger {

    private val callbacks: MutableList<() -> Boolean> = ArrayList(INITIAL_CAPACITY)

    override fun onBackPressed(): Boolean {
        // toList() 创建回调列表的不可变快照，防止在迭代过程中因回调函数自身修改原始列表
        callbacks.toList().fastForEachReversed { callback ->
            if (callback()) return true
        }
        return onBackPressedFallback()
    }

    override fun addOnBackPressedCallback(callback: () -> Boolean): Cancel {
        callbacks.add(callback)
        return { callbacks.remove(callback) }
    }

    private inline fun onBackPressedFallback(): Boolean =
        messenger.send(SEND_ON_BACK_PRESSED) == "true"

    companion object {
        private const val INITIAL_CAPACITY = 2
        private const val SEND_ON_BACK_PRESSED = "compose.ui.BackHandler:onBackPressed"
    }
}