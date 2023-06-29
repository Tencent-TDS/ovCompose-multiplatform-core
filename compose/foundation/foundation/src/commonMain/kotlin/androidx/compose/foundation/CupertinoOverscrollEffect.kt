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
import androidx.compose.foundation.gestures.ScrollValueConverter
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

private enum class CupertinoScrollSource {
    DRAG, FLING
}

@OptIn(ExperimentalFoundationApi::class)
class CupertinoOverscrollEffect(
    /*
     * Density to be taken into consideration during computations; Cupertino formulas use
     * DPs, and scroll machinery uses raw values.
     */
    private val density: Float
) : OverscrollEffect {
    /*
     * Offset to Float converter to do orientation-dependant calculations using the raw Float data
     * coming from bound [CupertinoFlingBehavior]. If this value is null, it wasn't bound and there would be
     * no overscroll effect present (because it relies on fling calls to maintain its correct state)
     * TODO: Current Compose API semantics are too weak for rich interaction between FlingBehavior and
     *  OvercrollEffect, like one used in iOS. Needs a proper proposal
     */
    internal var scrollValueConverter: ScrollValueConverter? = null

    /*
     * Size of container is taking into consideration when computing rubber banding
     */
    private var scrollSize: Size = Size.Zero

    /*
     * Current offset in overscroll area
     * Negative for bottom-right
     * Positive for top-left
     * Zero if within the scrollable range
     * It will be mapped to the actual visible offset using the rubber banding rule inside
     * [Modifier.offset] within [effectModifier]
     */
    private var overscrollOffset: Offset by mutableStateOf(Offset.Zero)

    private val visibleOverscrollOffset: IntOffset
        get() =
            overscrollOffset.rubberBanded().round()

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
            visibleOverscrollOffset
        }

    /*
     * Determines if the fling inertial motion is weak to surpass overscroll offset
     * (and hence needs to be replaced with spring animation)
     *
     * @param targetValue The target value for the fling inertia.
     * @return true if the fling inertia is weak, false otherwise.
     */
    internal fun isFlingInertiaWeak(targetValue: Float): Boolean {
        val scrollValueConverter = scrollValueConverter

        return if (scrollValueConverter != null) {
            val currentOverscroll = scrollValueConverter.convertOffsetToFloat(visibleOverscrollOffset.toOffset())

            (targetValue > 0f && currentOverscroll > 0f && targetValue < currentOverscroll) ||
                (targetValue < 0f && currentOverscroll < 0f && targetValue > currentOverscroll)
        } else {
            false
        }
    }

    private fun NestedScrollSource.toCupertinoScrollSource(): CupertinoScrollSource? =
        when (this) {
            NestedScrollSource.Drag -> CupertinoScrollSource.DRAG
            NestedScrollSource.Fling -> CupertinoScrollSource.FLING
            else -> null
        }

    /*
     * Takes input scroll delta, current overscroll value, and scroll source. Returns pair of
     * 1. Available delta to perform actual content scroll.
     * 2. New overscroll value.
     */
    @Stable
    private fun availableDelta(
        delta: Float,
        overscroll: Float,
        source: CupertinoScrollSource
    ): Pair<Float, Float> {
        // if source is fling, and delta is going into the overscroll area
        // 1. none of it will be consumed
        // 2. overscroll will stay the same
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

    /*
     * Semantics of this method match the [OverscrollEffect.applyToScroll] one,
     * The only difference is NestedScrollSource being remapped to CupertinoScrollSource to narrow
     * processed states invariant
     */
    private fun applyToScroll(
        delta: Offset,
        source: CupertinoScrollSource,
        performScroll: (Offset) -> Offset
    ): Offset {
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
    ): Offset {
        val cupertinoScrollSource = source.toCupertinoScrollSource()
        val scrollValueConverter = scrollValueConverter

        return if (scrollValueConverter != null && cupertinoScrollSource != null) {
            applyToScroll(delta, cupertinoScrollSource, performScroll)
        } else {
            performScroll(delta)
        }
    }

    override suspend fun applyToFling(
        velocity: Velocity,
        performFling: suspend (Velocity) -> Velocity
    ) {
        performFling(velocity)
    }

    suspend fun playSpringAnimation(unconsumedDelta: Float, initialVelocity: Float) {
        scrollValueConverter?.let {
            playSpringAnimation(
                it.convertFloatToOffset(unconsumedDelta),
                it.convertFloatToOffset(initialVelocity)
            )
        }
    }

    private suspend fun playSpringAnimation(unconsumedDelta: Offset, initialVelocity: Offset): Float {
        val initialValue = overscrollOffset - unconsumedDelta

        // All input values are divided by density so all internal calculations are performed as if
        // they operated on DPs. Callback value is then scaled back to raw pixels.
        AnimationState(
            Offset.VectorConverter,
            initialValue / density,
            -initialVelocity / density
        ).animateTo(
            targetValue = Offset.Zero,
            animationSpec = spring(
                stiffness = 200f,
                visibilityThreshold = Offset(0.5f, 0.5f) / density
            )
        ) {
            overscrollOffset = value * density
        }

        return 0f
    }

    private fun Offset.rubberBanded(): Offset {
        val dpOffset = this / density
        val dpSize = scrollSize / density

        return Offset(
            rubberBandedValue(dpOffset.x, dpSize.width, RUBBER_BAND_COEFFICIENT),
            rubberBandedValue(dpOffset.y, dpSize.height, RUBBER_BAND_COEFFICIENT)
        ) * density
    }

    /*
     * Maps raw delta offset [value] on an axis within scroll container with [dimension]
     * to actual visible offset
     */
    private fun rubberBandedValue(value: Float, dimension: Float, coefficient: Float) =
        sign(value) * (1f - (1f / (abs(value) * coefficient / dimension + 1f))) * dimension

    companion object Companion {
        private const val RUBBER_BAND_COEFFICIENT = 0.55f
    }
}