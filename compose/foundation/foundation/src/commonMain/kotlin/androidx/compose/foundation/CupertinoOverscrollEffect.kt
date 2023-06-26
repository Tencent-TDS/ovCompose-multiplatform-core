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
import androidx.compose.runtime.Stable
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

enum class CupertinoScrollSource {
    DRAG, FLING
}

@OptIn(ExperimentalFoundationApi::class)
class CupertinoOverscrollEffect : OverscrollEffect {
    /*
     * Size of container is taking into consideration when computing rubber banding
     */
    private var scrollSize: Size = Size.Zero

    /*
     * Current offset in overscroll area
     * Negative for bottom-right
     * Positive for top-left
     * Zero if within the scrollable range
     * It will be mapped (if needed) to the actual visible offset using the rubber banding rule inside
     * [Modifier.offset] within [effectModifier]
     */
    private var overscrollOffset: Offset by mutableStateOf(Offset.Zero)

    /*
     * true, if overscroll needs rubberBand function to be applied before using in Modifier.offset
     * false, if not and overscroll already keeps the expected value:
     * for example, during spring animation, which serves the values which don't need any post-processing
     */
    private var isOverscrollRaw: Boolean = true
        set(value) {
            if (field != value) {
                overscrollOffset = if (value) {
                    overscrollOffset.inverseRubberBanded()
                } else {
                    overscrollOffset.rubberBanded()
                }
            }

            field = value
        }

    val visibleOverscrollOffset: IntOffset
        get() =
            if (isOverscrollRaw) {
                overscrollOffset.rubberBanded().round()
            } else {
                overscrollOffset.round()
            }
    /*
     * Density to be taken into consideration during computations; Cupertino formulas use
     * DPs, and scroll machinery uses raw values.
     */
    private var density: Float = 1f

    override val isInProgress: Boolean
        get() =
            // If visible overscroll offset has at least one pixel
            // this effect is considered to be in progress
            visibleOverscrollOffset.toOffset().getDistance() > 0.5f

    override val effectModifier = Modifier
        .onPlaced {
            scrollSize = it.size.toSize()
        }
        .offset {
            this@CupertinoOverscrollEffect.density = density

            visibleOverscrollOffset
        }

    private fun NestedScrollSource.toCupertinoScrollSource(): CupertinoScrollSource? =
        when (this) {
            NestedScrollSource.Drag -> CupertinoScrollSource.DRAG
            NestedScrollSource.Fling -> CupertinoScrollSource.FLING
            else -> null
        }
    /*
     * Takes input scroll delta and current overscroll value. Returns pair of
     * 1. Available delta to perform actual content scroll.
     * 2. New overscroll value.
     */
    @Stable
    private fun availableDelta(delta: Float, overscroll: Float, source: CupertinoScrollSource): Pair<Float, Float> {
        // if source is fling, and delta is going into the overscroll area, none of it will be consumed,
        // overscroll will stay the same
        if (source == CupertinoScrollSource.FLING) {
            if ((delta < 0f && overscroll <= 0f) || (delta > 0f && overscroll >= 0f)) {
                return delta to overscroll
            }
        }

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
     * It will update [overscroll] resulting in visual change because of [Modifier.offset] depending on it
     */

    private fun availableDelta(delta: Offset, source: CupertinoScrollSource): Offset {
        val (x, overscrollX) = availableDelta(delta.x, overscrollOffset.x, source)
        val (y, overscrollY) = availableDelta(delta.y, overscrollOffset.y, source)

        overscrollOffset = Offset(overscrollX, overscrollY)

        return Offset(x, y)
    }

    private fun applyToScroll(
        delta: Offset,
        source: CupertinoScrollSource,
        performScroll: (Offset) -> Offset
    ): Offset {
        // This will update remap overscrollOffset to our space (rubberBanded vs raw)
        // inside [isOverscrollRaw] setter
        isOverscrollRaw = when (source) {
            CupertinoScrollSource.DRAG -> true
            CupertinoScrollSource.FLING -> false
        }

        // Calculate how much delta is available after being consumed by scrolling inside overscroll area
        val deltaLeftForPerformScroll = availableDelta(delta, source)

        // Then pass remaining delta to scroll closure
        val deltaConsumedByPerformScroll = performScroll(deltaLeftForPerformScroll)

        // Delta which is left after `performScroll` was invoked with availableDelta
        val unconsumedDelta = deltaLeftForPerformScroll - deltaConsumedByPerformScroll

        return when (source) {
            CupertinoScrollSource.DRAG -> {
                // [unconsumedDelta] is going into overscroll again in case a user drags and hits the
                // overscroll->content->overscroll or content->overscroll scenario within single frame
                overscrollOffset += unconsumedDelta

                // Entire delta is always consumed by this effect
                // TODO: clarify what is expected nested scrolls behavior?
                delta
            }

            CupertinoScrollSource.FLING -> {
                println("$delta $unconsumedDelta")

                // If unconsumedDelta is not Zero, [CupertinoFlingEffect] will cancel fling and
                // start spring animation instead
                delta - unconsumedDelta
            }

        }
    }

    override fun applyToScroll(
        delta: Offset,
        source: NestedScrollSource,
        performScroll: (Offset) -> Offset
    ): Offset =
        source.toCupertinoScrollSource()?.let {
            applyToScroll(delta, it, performScroll)
        } ?: performScroll(delta)

    override suspend fun applyToFling(
        velocity: Velocity,
        performFling: suspend (Velocity) -> Velocity
    ) {
        performFling(velocity)
    }

    suspend fun playSpringAnimation(delta: Offset, initialVelocity: Offset) {
        // Convert raw overscroll offset to actual visible one to perform correct spring animation
        if (isOverscrollRaw) {
            isOverscrollRaw = false
        }

        val initialValue = overscrollOffset - delta

        // All input values are divided by density
        AnimationState(Offset.VectorConverter, initialValue / density, -initialVelocity / density).animateTo(
            targetValue = Offset.Zero,
            animationSpec = spring(stiffness = 200f, visibilityThreshold = Offset(0.5f / density, 0.5f / density))
        ) {
            overscrollOffset = value * density

            println(overscrollOffset)
        }

        println("Finished")
    }

    private fun Offset.rubberBanded(): Offset =
        remap(scrollSize, density, RUBBER_BAND_COEFFICIENT, ::rubberBandedValue)

    private fun Offset.inverseRubberBanded(): Offset =
        remap(scrollSize, density, RUBBER_BAND_COEFFICIENT, ::inverseRubberBandedValue)

    /*
     * Maps raw delta offset [value] on an axis within scroll container with [dimension]
     * to actual visible offset
     */
    private fun rubberBandedValue(value: Float, dimension: Float, coefficient: Float) =
        sign(value) * (1f - (1f / (abs(value) * coefficient / dimension + 1f))) * dimension

    /*
     * Inverse of [rubberBandedValue] function
     */
    private fun inverseRubberBandedValue(value: Float, dimension: Float, coefficient: Float) =
        if (value >= 0) {
            (dimension / coefficient) * (1f / (1f - value / dimension) - 1f)
        } else {
            -((dimension / coefficient) * (1f / (1f - abs(value) / dimension) - 1f))
        }

    // Remap Offset on per-dimension basis for applying rubberBanding function or inverse of it
    private fun Offset.remap(size: Size, density: Float, coefficient: Float, function: (value: Float, dimension: Float, coefficient: Float) -> Float): Offset {
        val dpOffset = this / density
        val dpSize = size / density

        return Offset(
            function(dpOffset.x, dpSize.width, coefficient),
            function(dpOffset.y, dpSize.height, coefficient)
        ) * density
    }

    companion object Companion {
        private const val RUBBER_BAND_COEFFICIENT = 0.55f
    }
}