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

import androidx.compose.common.UIKitNativeTraceBegin
import androidx.compose.common.UIKitNativeTraceEnd
import kotlinx.cinterop.ExperimentalForeignApi

actual object TraceUtil {
    private val DefaultTrace = DefaultSignPostSyncTrace
    actual var traceImpl: SyncTraceInterface? = DefaultTrace
    private var _vsyncId = 0L
    private var _isTraceEnabled = true
    actual val globalVsyncId: Long get() = _vsyncId

    actual fun increaseVsyncId(): Long {
        if (this.traceImpl != null) _vsyncId++
        return _vsyncId
    }

    actual inline fun <T> traceSync(sectionName: String, block: () -> T): T {
        if (isTraceEnabled) this.traceImpl?.startTrace("$sectionName[VsyncId:${this.globalVsyncId}]")
        return try {
            block()
        } finally {
            if (isTraceEnabled) this.traceImpl?.endTrace(sectionName)
        }
    }

    actual var isTraceEnabled: Boolean
        get() = _isTraceEnabled
        set(value) {
            _isTraceEnabled = value
        }
}

object DefaultSignPostSyncTrace : SyncTraceInterface {
    @OptIn(ExperimentalForeignApi::class)
    override fun startTrace(sectionName: String) {
        UIKitNativeTraceBegin(sectionName)
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun endTrace(sectionName: String?) {
        UIKitNativeTraceEnd(sectionName)
    }
}
