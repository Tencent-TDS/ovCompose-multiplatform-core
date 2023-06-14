/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.animation

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import kotlin.math.ln
import kotlin.math.pow


data class DecelerationTimingParameters(
    val initialValue: Offset,
    val initialVelocity: Offset,
    val decelerationRate: Float,
    val threshold: Float
) {
    val destination: Offset
        get() = initialValue - initialVelocity / decelerationCoefficient

    val duration: Float
        get() =
            if (initialVelocity.getDistance() > 0f) {
                0f
            } else {
                ln(-decelerationCoefficient * threshold / initialVelocity.getDistance()) / decelerationCoefficient
            }

    fun valueAt(time: Float): Offset =
        initialValue + initialVelocity * (decelerationRate.pow(1000f * time) - 1f) / decelerationCoefficient

    fun durationTo(value: Offset): Float? =
        if (value.distanceToSegment(initialValue, destination) < threshold) {
            null
        } else {
            ln(1f + decelerationCoefficient * (value - initialValue).getDistance() / initialVelocity.getDistance()) / decelerationCoefficient
        }

    fun velocityAt(time: Float): Offset =
        initialVelocity * decelerationRate.pow(1000f * time)

    private val decelerationCoefficient: Float
        get() = 1000f * ln(decelerationRate)

    init {
        require(decelerationRate > 0f && decelerationRate < 1f)
    }
}