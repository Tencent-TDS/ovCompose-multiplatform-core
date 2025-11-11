/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.common.interop

import platform.ohos.OH_HiTrace_FinishTrace
import platform.ohos.OH_HiTrace_StartTrace

actual object TraceUtil {
    private var _isTraceEnabled = true
    private val DefaultTrace = object : SyncTraceInterface {
        override fun startTrace(scene: String) {
            OH_HiTrace_StartTrace(scene)
        }

        override fun endTrace(sectionName: String?) {
            OH_HiTrace_FinishTrace()
        }
    }

    actual var traceImpl: SyncTraceInterface? = DefaultTrace

    private var _vsyncId = 0L
    actual val globalVsyncId: Long get() = _vsyncId
    actual fun increaseVsyncId(): Long {
        if (traceImpl != null) _vsyncId++
        return _vsyncId
    }

    actual inline fun <T> traceSync(sectionName: String, block: () -> T): T {
        if (ENABLE_VERBOSE_TRACE_COMPILE_TIME) {
            if (isTraceEnabled) traceImpl?.startTrace("$sectionName[VsyncId:$globalVsyncId]")
            return try {
                block()
            } finally {
                if (isTraceEnabled) traceImpl?.endTrace(sectionName)
            }
        } else {
            return block()
        }
    }

    actual var isTraceEnabled: Boolean
        get() = _isTraceEnabled
        set(value) {
            _isTraceEnabled = value
        }
}