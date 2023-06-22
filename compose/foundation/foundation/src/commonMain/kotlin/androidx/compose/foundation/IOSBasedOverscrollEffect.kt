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

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
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
     * Zero if within the scrollable range
     * It will be mapped to the actual visible offset using the rubber banding rule inside
     * [Modifier.offset] within [effectModifier]
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

    /*
     * Returns the amount of scroll delta available after user performed scroll inside overscroll area
     * For example, if user is currently in -100 point, scrolling down with delta 40 will make it -60
     * It will update [overscroll] resulting in visual change because of [Modifier.offset] depending on it
     * But [performScroll] closure from within [applyToScroll] will be called with Offset.Zero
     */
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
    ): Offset =
        when (source) {
            NestedScrollSource.Drag -> {
                // First we consume current delta against existing overscroll.
                // Rubber band effect is symmetrical for drag directions
                // hence it will scale your drag non-linearly even if you want to scroll out of
                // overscroll area
                val deltaLeftForPerformScroll = availableDelta(delta)

                // Then pass remaining delta to scroll closure
                val deltaConsumedByPerformScroll = performScroll(deltaLeftForPerformScroll)

                // All that remains is going into overscroll again
                val unconsumedDelta = deltaLeftForPerformScroll - deltaConsumedByPerformScroll

                overscroll += unconsumedDelta

                // Entire delta is always consumed by this effect
                delta
            }

            NestedScrollSource.Fling -> {
                // Return how much delta was consumed by actual scroll
                performScroll(delta)
            }
            else -> {
                performScroll(delta)

                delta
            }
        }

    override suspend fun applyToFling(
        velocity: Velocity,
        performFling: suspend (Velocity) -> Velocity
    ) {
        performFling(velocity)
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