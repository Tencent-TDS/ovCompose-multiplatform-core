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

package androidx.compose.runtime.monitor

// region Tencent Code
enum class Severity {
    ERROR,
    WARNING
}

data class Diagnostic(
    val name: String,
    val severity: Severity,
    val message: String,
    val error: Throwable?
)

object ComposeDiagnosticMonitor {

    private const val KEY_READ_WRITE_STATE_ON_WRONG_THREAD = "read_write_state_on_wrong_thread"
    private const val KEY_STATE_MERGE_CONFLICT = "state_merge_conflict"
    private const val KEY_ANIMATION_VECTOR_NAN = "key_animation_vector_nan"
    private const val KEY_FLING_POSITION_INDEX_OUT_OF_BOUNDS = "fling_position_index_out_of_bounds"

    var fallbackStateError = false
    var stateMergeConflictFix = false
    var isFixAnimationNaN = false

    var adapter: ComposeMonitorAdapter = DefaultComposeMonitorAdapter()

    fun reportStateOnWrongThread(error: Throwable) {
        reportErrorImpl(KEY_READ_WRITE_STATE_ON_WRONG_THREAD, error)
    }

    fun reportStateMergeConflict(error: Throwable) {
        reportErrorImpl(KEY_STATE_MERGE_CONFLICT, error)
    }

    fun reportAnimationNaNError(message: String, error: Throwable) {
        reportErrorImpl(KEY_ANIMATION_VECTOR_NAN, error, message)
    }

    fun reportAnimationIndexOutOfBounds(error: Throwable, message: String? = null) {
        reportErrorImpl(KEY_FLING_POSITION_INDEX_OUT_OF_BOUNDS, error, message)
    }

    private fun reportErrorImpl(name: String, error: Throwable, message: String? = null) {
        Diagnostic(
            name = name,
            severity = Severity.ERROR,
            message = message ?: error.message ?: "",
            error = error
        ).let {
            adapter.onReportDiagnostic(it)
        }
    }
}

private var composeDrawFrameId: Long = 0L

fun increaseDiagnosticDrawFrameId() {
    // 每 8 毫秒累加 1，大约需要 2.338 万亿年 才会溢出
    composeDrawFrameId += 1
}

fun diagnosticDrawFrameId(): Long = composeDrawFrameId
// endregion