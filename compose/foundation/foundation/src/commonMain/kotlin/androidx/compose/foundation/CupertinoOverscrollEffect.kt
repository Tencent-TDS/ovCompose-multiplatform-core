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

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateTo
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
class CupertinoOverscrollEffect : OverscrollEffect {
    companion object Companion {
        private const val RUBBER_BAND_COEFFICIENT = 0.55f
        private fun rubberBandedValue(value: Float, dimension: Float, coefficient: Float = RUBBER_BAND_COEFFICIENT) =
            sign(value) * (1f - (1f / (abs(value) * coefficient / dimension + 1f))) * dimension

        fun rubberBandedOffset(offset: Offset, size: Size, density: Float, coefficient: Float = RUBBER_BAND_COEFFICIENT): Offset {
            val dpOffset = offset / density
            val dpSize = size / density

            return Offset(
                rubberBandedValue(dpOffset.x, dpSize.width, coefficient),
                rubberBandedValue(dpOffset.y, dpSize.height, coefficient)
            ) * density
        }
    }
    /*
     * size of container is taking into consideration when computing rubber banding,
     * we store it in this variable once layout pass is performed
     */
    var scrollSize: Size? = null

    /*
     * Current offset in overscroll area
     * Negative for bottom-right
     * Positive for top-left
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
                // hence it will scale the drag non-linearly even if you want to scroll out of
                // overscroll area
                val deltaLeftForPerformScroll = availableDelta(delta)

                // Then pass remaining delta to scroll closure
                val deltaConsumedByPerformScroll = performScroll(deltaLeftForPerformScroll)

                // All that remains is going into overscroll again
                val unconsumedDelta = deltaLeftForPerformScroll - deltaConsumedByPerformScroll

                println("$delta $deltaLeftForPerformScroll $deltaConsumedByPerformScroll $unconsumedDelta $overscroll")

                overscroll += unconsumedDelta

                // Entire delta is always consumed by this effect
                delta
            }

            NestedScrollSource.Fling -> {
                performScroll(delta)
            }
            else -> performScroll(delta)
        }

    override suspend fun applyToFling(
        velocity: Velocity,
        performFling: suspend (Velocity) -> Velocity
    ) {
        performFling(velocity)
    }

    suspend fun playSpringAnimation(delta: Offset, initialVelocity: Offset) {
        val initialValue = overscroll - delta

        AnimationState(Offset.VectorConverter, initialValue, -initialVelocity).animateTo(
            targetValue = Offset.Zero,
            animationSpec = spring(stiffness = 200f, visibilityThreshold = Offset(0.5f, 0.5f))
        ) {
            println(value)
            overscroll = value
        }
    }

    override val isInProgress =
        overscroll.getDistanceSquared() > 0.5f

    override val effectModifier = Modifier
        .onPlaced {
            scrollSize = it.size.toSize()
        }
        .offset {
            scrollSize?.let {
                rubberBandedOffset(overscroll, it, density).round()
            } ?: IntOffset.Zero
        }
}