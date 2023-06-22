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

package androidx.compose.foundation

import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animate
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.unit.*
import kotlin.math.abs
import kotlin.math.sign

@OptIn(ExperimentalFoundationApi::class)
class IOSBasedOverscrollEffect : OverscrollEffect {
    companion object {
        val IOS_COEFFICIENT = 0.55f
        fun rubberBandedValue(value: Float, dimension: Float, coefficient: Float = IOS_COEFFICIENT) =
            sign(value) * (1f - (1f / (abs(value) * coefficient / dimension + 1f))) * dimension

        fun rubberBandedOffset(offset: Offset, size: Size, coefficient: Float = IOS_COEFFICIENT) =
            Offset(
                rubberBandedValue(offset.x, size.width, coefficient),
                rubberBandedValue(offset.y, size.height, coefficient)
            )
    }
    /*
     * size of container is taking into consideration when computing rubber banding,
     * we store it in this variable once layout pass is performed
     */
    var scrollSize: Size? = null

    /*
     * Current offset in overscroll area
     * Negative for top-left
     * Positive for bottom-right
     * Zero if Offset dimension is within the scrollable range
     * It will be mapped to the actual visible offset using the rubber banding rule
     */
    private var overscroll: Offset by mutableStateOf(Offset.Zero)

    /*
     * Takes input scroll delta and current overscroll value. Returns pair of
     * 1. Available delta to perform actual content scroll.
     * 2. New overscroll value.
     */
    private fun availableDelta(delta: Float, overscroll: Float): Pair<Float, Float> {
        val newOverscroll = overscroll + delta

        return if (delta >= 0f && overscroll <= 0f) {
            if (newOverscroll > 0f) {
                newOverscroll to 0f
            } else {
                0f to newOverscroll
            }
        } else if (delta <= 0f && overscroll >= 0f) {
            if (newOverscroll < 0f) {
                newOverscroll to 0f
            } else {
                0f to newOverscroll
            }
        } else {
            0f to newOverscroll
        }
    }

    private fun availableDelta(delta: Offset): Offset {
        val (x, overscrollX) = availableDelta(delta.x, overscroll.x)
        val (y, overscrollY) = availableDelta(delta.y, overscroll.y)

        overscroll = Offset(overscrollX, overscrollY)

        return Offset(x, y)
    }

    override fun applyToScroll(
        delta: Offset,
        source: NestedScrollSource,
        performScroll: (Offset) -> Offset
    ): Offset {
        // First we consume current delta into overscroll
        val availableDelta = availableDelta(delta)

        // Then pass remaining delta to scroll closure
        val consumedDelta = performScroll(availableDelta)

        // All that remained is going into overscroll again
        val unconsumedDelta = availableDelta - consumedDelta

        overscroll += unconsumedDelta

        return overscroll
    }

    override suspend fun applyToFling(
        velocity: Velocity,
        performFling: suspend (Velocity) -> Velocity
    ) {
        val flingResult = performFling(velocity)

        animate(Offset.VectorConverter, overscroll, Offset.Zero, Offset(velocity.x, velocity.y)) { value, velocity ->
            overscroll = value
        }

        println("$velocity $flingResult")
    }

    override val isInProgress =
        overscroll.getDistanceSquared() > 0.5f

    override val effectModifier = Modifier
        .onPlaced {
            scrollSize = it.size.toSize()
        }
        .offset {
            scrollSize?.let {
                rubberBandedOffset(overscroll, it).round()
            } ?: IntOffset.Zero
        }
}