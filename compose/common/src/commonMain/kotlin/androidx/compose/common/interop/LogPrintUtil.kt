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

/**
 * Compile-time constant to control verbose logging.
 * When set to false, the compiler will completely remove all verbose log code (zero overhead).
 * When set to true, verbose logging is controlled at runtime via [LogPrintUtil.isLogEnabled].
 * 
 * To disable verbose logging at compile time, set this to false before building.
 */
const val ENABLE_VERBOSE_LOG_COMPILE_TIME = false

interface LogPrintInterface {
    fun verbose(message: String)
}

expect object LogPrintUtil {
    var logPrintImpl: LogPrintInterface?
    var isLogEnabled: Boolean

    /**
     * Lazy evaluation version of verbose. The message lambda is only evaluated when logging is enabled.
     * This avoids unnecessary string concatenation when logging is disabled.
     * 
     * When [ENABLE_VERBOSE_LOG_COMPILE_TIME] is false, the compiler will completely remove
     * this code at compile time, resulting in zero overhead.
     * 
     * Usage:
     * ```
     * LogPrintUtil.verbose { "AdaptiveCanvas::translate, dx: $dx, dy: $dy" }
     * ```
     */
    inline fun verbose(message: () -> String)
}