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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sign

data class RubberBand(
    /**
     * Arbitary value matching the one chosen by UIKit developers
     */
    val coefficient: Float = 0.55f,

    /**
     * Size of element encapsulating scrolling behavior
     */
    val ownSize: Size,

    /**
     * Size of scrollable content inside element encapsulating scrolling behavior
     */
    val contentSize: Size,
) {
    companion object {
        fun rubberBandedValue(value: Float, dimension: Float, coefficient: Float): Float =
            (1f - (1f / (value * coefficient / dimension + 1f))) * dimension
    }

    /**
     * Scrollable ranges across X and Y axis, for example if [ownSize] is 800x600
     * and [contentSize] is 800x1000, this value would be 0x400, meaning that trying to drag offset
     * below 0x400 would cause scrollable frame to move below actual content and rubber band effect
     * should be triggered
     */
    private val scrollableRanges: Size
        get() =
            Size(contentSize.width - ownSize.width, contentSize.height - ownSize.height)

    fun rubberBandedValue(value: Float, dimension: Float): Float =
        rubberBandedValue(value, dimension, coefficient)

    fun rubberBandedValue(value: Float, dimension: Float, max: Float): Float {
        val clampedValue = value.coerceIn(0f, max)
        val delta = clampedValue - value
        val sign = sign(delta)

        return if (sign == 0f) {
            value
        } else {
            clampedValue + sign * rubberBandedValue(abs(delta), dimension)
        }
    }

    fun rubberBandedOffset(offset: Offset): Offset =
        Offset(
            rubberBandedValue(offset.x, ownSize.width, scrollableRanges.width),
            rubberBandedValue(offset.y, ownSize.height, scrollableRanges.height)
        )
}

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