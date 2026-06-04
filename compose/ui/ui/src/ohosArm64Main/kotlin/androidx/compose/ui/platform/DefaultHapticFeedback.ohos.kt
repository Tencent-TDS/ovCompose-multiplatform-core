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

package androidx.compose.ui.platform

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.hapticfeedback.HapticFeedbackType.Companion.LongPress
import androidx.compose.ui.hapticfeedback.HapticFeedbackType.Companion.TextHandleMove
import kotlinx.cinterop.cValue
import platform.devices.OH_Vibrator_PlayVibration
import platform.devices.VIBRATOR_USAGE_TOUCH
import platform.devices.Vibrator_Attribute

internal actual class DefaultHapticFeedback : HapticFeedback {
    companion object {
        private const val VIBRATION_DURATION_SHORT = 30
    }

    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        when (hapticFeedbackType) {
            LongPress, TextHandleMove -> {
                val attr = cValue<Vibrator_Attribute> {
                    vibratorId = 0
                    usage = VIBRATOR_USAGE_TOUCH
                }
                OH_Vibrator_PlayVibration(VIBRATION_DURATION_SHORT, attr)
            }
            else -> Unit
        }
    }
}