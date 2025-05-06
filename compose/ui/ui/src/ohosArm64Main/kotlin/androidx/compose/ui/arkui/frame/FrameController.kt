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

package androidx.compose.ui.arkui.frame

import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_xcomponent_registerFrameCallback
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_xcomponent_unregisterFrameCallback
import kotlinx.atomicfu.atomic
import kotlinx.cinterop.COpaquePointer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Frame Controller
 *
 * @author nathanwwang
 */
internal class FrameController(private val renderProvider: () -> COpaquePointer?) {

    companion object {
        private const val TAG = "FrameController"

        private const val UNREGISTER_FRAME_CALLBACK_DELAY_TIME_MS = 50L
    }

    private var delayJob: Job? = null
    private val delayScope = CoroutineScope(Dispatchers.Main)

    private val delayStarted = atomic(0)
    private val disposed = atomic(false)

    /**
     * 需要FrameCallback，如果有延迟取消注册frameCallback的任务，则取消该任务
     */
    fun requireFrameCallback() {
        if (disposed.value) {
            androidx.compose.ui.graphics.kLog("$TAG Already disposed!!!")
            return
        }
        if (delayStarted.compareAndSet(1, 0)) {
            androidx.compose.ui.graphics.kLog("$TAG cancel delay unregister frameCallback.")
            delayJob?.cancel()
            delayJob = null
        }
        registerFrameCallback()
    }

    /**
     * 之后不再需要FrameCallback了，延迟50L毫秒，如果期间不再需要，则移除frameCallback的注册
     */
    fun releaseFrameCallback() {
        if (disposed.value) {
            androidx.compose.ui.graphics.kLog("$TAG Already disposed!!!")
            return
        }
        if (delayStarted.compareAndSet(0, 1)) {
            androidx.compose.ui.graphics.kLog("$TAG schedule delay unregister frameCallback.")
            delayJob?.cancel()
            delayJob = delayScope.postDelayed(UNREGISTER_FRAME_CALLBACK_DELAY_TIME_MS) {
                delayStarted.compareAndSet(1, 0)
                unregisterFrameCallback()
            }
        }
    }

    /**
     * 清理数据，调用后此controller不能再使用
     */
    fun dispose() {
        if (disposed.compareAndSet(false, true)) {
            delayJob?.cancel()
            delayJob = null
        }
    }

    private fun CoroutineScope.postDelayed(delayMillis: Long, action: () -> Unit): Job {
        return launch {
            delay(delayMillis)
            action()
        }
    }

    private fun registerFrameCallback() {
        val render = renderProvider.invoke() ?: return
        androidx_compose_ui_arkui_utils_xcomponent_registerFrameCallback(render)
    }

    private fun unregisterFrameCallback() {
        val render = renderProvider.invoke() ?: return
        androidx_compose_ui_arkui_utils_xcomponent_unregisterFrameCallback(render)
    }
}