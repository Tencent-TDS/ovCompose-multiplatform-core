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

package androidx.compose.ui.interop

import platform.ohos.OH_HiTrace_FinishTrace
import platform.ohos.OH_HiTrace_StartTrace

/**
 * 统计CMC内部与18255工程内部的核心函数，暂时没有考虑多线程的问题。
 */
object OhosTrace {

    private val DefaultTrace = object : SyncTraceInterface {
        override fun startTrace(scene: String) {
            OH_HiTrace_StartTrace(scene)
        }

        override fun endTrace() {
            OH_HiTrace_FinishTrace()
        }
    }

    var traceImpl: SyncTraceInterface? = DefaultTrace

    var globalVsyncId: Long = 0
        private set

    fun increaseVsyncId(): Long {
        if (traceImpl != null) {
            globalVsyncId++
        }
        return globalVsyncId
    }

    inline fun <T> traceSync(sectionName: String, block: () -> T): T {
        traceImpl?.startTrace("$sectionName[VsyncId:$globalVsyncId]")
        val result = block()
        traceImpl?.endTrace()
        return result
    }
}

interface SyncTraceInterface {
    fun startTrace(scene: String)

    fun endTrace()
}
