/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density

private const val DEFAULT_LONG_PRESS_TIMEOUT_MILLIS = 500L
private const val DEFAULT_DOUBLE_TAP_TIMEOUT_MILLIS = 300L
private const val DEFAULT_DOUBLE_TAP_MIN_TIME_MILLIS = 40L
private val DEFAULT_TOUCH_SLOP_DP = 18.dp

class DefaultViewConfiguration(private val density: Density) : ViewConfiguration {
    override val longPressTimeoutMillis: Long
        get() = DEFAULT_LONG_PRESS_TIMEOUT_MILLIS

    override val doubleTapTimeoutMillis: Long
        get() = DEFAULT_DOUBLE_TAP_TIMEOUT_MILLIS

    override val doubleTapMinTimeMillis: Long
        get() = DEFAULT_DOUBLE_TAP_MIN_TIME_MILLIS

    override val touchSlop: Float
        get() = with(density) { DEFAULT_TOUCH_SLOP_DP.toPx() }
}

internal class DefaultViewConfigurationWithDensityProvider(
    private val densityProvider: () -> Density
) : ViewConfiguration {
    override val longPressTimeoutMillis: Long
        get() = DEFAULT_LONG_PRESS_TIMEOUT_MILLIS

    override val doubleTapTimeoutMillis: Long
        get() = DEFAULT_DOUBLE_TAP_TIMEOUT_MILLIS

    override val doubleTapMinTimeMillis: Long
        get() = DEFAULT_DOUBLE_TAP_MIN_TIME_MILLIS

    override val touchSlop: Float
        get() = with(densityProvider()) { DEFAULT_TOUCH_SLOP_DP.toPx() }
}
