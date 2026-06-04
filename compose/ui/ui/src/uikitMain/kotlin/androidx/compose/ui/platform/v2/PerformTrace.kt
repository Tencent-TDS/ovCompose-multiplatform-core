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

package androidx.compose.ui.platform.v2

import androidx.compose.ui.uikit.utils.TMMNativeTraceBegin
import androidx.compose.ui.uikit.utils.TMMNativeTraceEnd
import androidx.compose.ui.uikit.utils.TMMNativeTraceScene

object PerformanceTrace {

    var traceImpl: SyncTraceInterface? = null
        set(value) {
            if (traceImpl == null) {
                field = value
            }
        }

    val enabled: Boolean get() = traceImpl != null

    var globalVsyncId: Long = 0
        private set

    fun increaseVsyncId(): Long {
        if (traceImpl != null) {
            globalVsyncId++
        }
        return globalVsyncId
    }

    inline fun <T> traceSync(scene: Int, taskId: Long, block: () -> T): T {
        val trace = traceImpl
        return if (trace == null) {
            block()
        } else {
            trace.startTrace(scene, taskId)
            val result = block()
            trace.endTrace(scene, taskId)
            result
        }
    }

    inline fun <T> traceSync(scene: Int, block: () -> T): T {
        val trace = traceImpl
        return if (trace == null) {
            block()
        } else {
            val vsyncId = globalVsyncId
            trace.startTrace(scene, vsyncId)
            val result = block()
            trace.endTrace(scene, vsyncId)
            result
        }
    }
}

interface SyncTraceInterface {
    fun startTrace(scene: Int, taskId: Long)

    fun endTrace(scene: Int, taskId: Long)
}

object DefaultSignPostSyncTrace : SyncTraceInterface {
    override fun startTrace(scene: Int, taskId: Long) {
        TMMNativeTraceBegin(scene.toLong(), taskId)
    }

    override fun endTrace(scene: Int, taskId: Long) {
        TMMNativeTraceEnd(scene.toLong(), taskId)
    }
}