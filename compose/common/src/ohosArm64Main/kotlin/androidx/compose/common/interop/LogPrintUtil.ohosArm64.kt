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

import platform.ohos.LOG_APP
import platform.ohos.LOG_DEBUG
import platform.ohos.OH_LOG_Print


/**
 * Compile-time constant to control verbose logging.
 * When set to false, the compiler will completely remove all verbose log code (zero overhead).
 * When set to true, verbose logging is controlled at runtime via [LogPrintUtil.isLogEnabled].
 *
 * To disable verbose logging at compile time, set this to false before building.
 */
const val ENABLE_VERBOSE_LOG_COMPILE_TIME = true

actual object LogPrintUtil {
    private var _isLogEnabled: Boolean = true

    private val DefaultLogger = object : LogPrintInterface {
        override fun verbose(message: String) {
            OH_LOG_Print(
                type = LOG_APP,
                level = LOG_DEBUG,
                domain = 0x0000u,
                tag = "ovComposeKt",
                fmt = message
            )
        }
    }
    actual var logPrintImpl: LogPrintInterface? = DefaultLogger

    actual inline fun verbose(message: () -> String) {
        if (ENABLE_VERBOSE_LOG_COMPILE_TIME && isLogEnabled) {
            this.logPrintImpl?.verbose(message())
        }
    }

    actual var isLogEnabled: Boolean
        get() = _isLogEnabled
        set(value) {
            _isLogEnabled = value
        }
}