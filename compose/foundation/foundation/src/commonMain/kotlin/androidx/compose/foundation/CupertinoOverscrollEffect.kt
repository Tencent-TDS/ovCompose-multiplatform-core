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

import androidx.compose.animation.core.*
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.*
import kotlin.coroutines.coroutineContext
import kotlin.math.abs
import kotlin.math.sign
import kotlinx.coroutines.isActive

private enum class CupertinoScrollSource {
    DRAG, FLING
}

@OptIn(ExperimentalFoundationApi::class)
class CupertinoOverscrollEffect(
    /*
     * Density to be taken into consideration during computations; Cupertino formulas use
     * DPs, and scroll machinery uses raw values.
     */
    private val density: Float,
    layoutDirection: LayoutDirection
) : OverscrollEffect {
    /*
     * Offset to Float converter to do orientation-dependant calculations using the raw Float data
     */
    internal var scrollValueConverter: ScrollValueConverter? = null

    private val reverseHorizontal =
        when (layoutDirection) {
            LayoutDirection.Ltr -> false
            LayoutDirection.Rtl -> true
        }


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

    private var lastFlingUncosumedDelta: Offset = Offset.Zero
    private val visibleOverscrollOffset: IntOffset
        get() =
            overscrollOffset.reverseHorizontalIfNeeded().rubberBanded().round()

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

//    internal suspend fun playInitialSpringIfNeeded(velocity: Float): Float {
//        val scrollValueConverter = scrollValueConverter ?: return velocity
//
//        val currentOverscroll = scrollValueConverter.convertOffsetToFloat(overscrollOffset)
//
//        return if ((velocity > 0f && currentOverscroll > 0f) || (velocity < 0f && currentOverscroll < 0f)) {
//            playSpringAnimation(0f, velocity, true)
//        } else {
//            velocity
//        }
//    }

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
        // if source is fling:
        // 1. no delta will be consumed
        // 2. overscroll will stay the same
        if (source == CupertinoScrollSource.FLING) {
            return delta to overscroll
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

                lastFlingUncosumedDelta = Offset.Zero

                delta - unconsumedDelta
            }

            CupertinoScrollSource.FLING -> {
                // If unconsumedDelta is not Zero, [CupertinoFlingEffect] will cancel fling and
                // start spring animation instead
                lastFlingUncosumedDelta = unconsumedDelta

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
        scrollValueConverter?.let {

            val availableFlingVelocity = playInitialSpringAnimationIfNeeded(velocity, it)

            val velocityConsumedByFling = performFling(availableFlingVelocity)
            val postFlingVelocity = availableFlingVelocity - velocityConsumedByFling

            playSpringAnimation(
                it.convertOffsetToFloat(lastFlingUncosumedDelta),
                it.convertOffsetToFloat(postFlingVelocity.toOffset()),
                false,
                it
            )

        } ?: performFling(velocity)
    }

    private fun Velocity.toOffset(): Offset =
        Offset(x, y)

    private fun Offset.toVelocity(): Velocity =
        Velocity(x, y)

    private suspend fun playInitialSpringAnimationIfNeeded(initialVelocity: Velocity, scrollValueConverter: ScrollValueConverter): Velocity {
        val velocity = scrollValueConverter.convertOffsetToFloat(initialVelocity.toOffset())
        val overscroll = scrollValueConverter.convertOffsetToFloat(overscrollOffset)

        return if ((velocity < 0f && overscroll > 0f) || (velocity > 0f && overscroll < 0f)) {
            scrollValueConverter.convertFloatToOffset(
                playSpringAnimation(0f, velocity, true, scrollValueConverter)
            ).toVelocity()
        } else {
            initialVelocity
        }
    }

    private suspend fun playSpringAnimation(
        unconsumedDelta: Float,
        initialVelocity: Float,
        flingFromOverscroll: Boolean,
        scrollValueConverter: ScrollValueConverter
    ): Float {
        val initialValue = scrollValueConverter.convertOffsetToFloat(overscrollOffset) + unconsumedDelta
        var currentVelocity = initialVelocity

        val initialSign = sign(initialValue)

        // All input values are divided by density so all internal calculations are performed as if
        // they operated on DPs. Callback value is then scaled back to raw pixels.
        val visibilityThreshold = 0.5f / density

        val spec = if (flingFromOverscroll) {
            spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = 400f,
                visibilityThreshold = visibilityThreshold
            )
        } else {
            spring(
                stiffness = 200f,
                visibilityThreshold = visibilityThreshold
            )
        }

        AnimationState(
            Float.VectorConverter,
            initialValue / density,
            initialVelocity / density
        ).animateTo(
            targetValue = 0f,
            animationSpec = spec
        ) {
            overscrollOffset = scrollValueConverter.convertFloatToOffset(value * density)
            currentVelocity = velocity * density

            // If it was fling from overscroll, cancel animation and return velocity
            if (flingFromOverscroll && initialSign != 0f && sign(value) != initialSign) {
                this.cancelAnimation()
            }
        }

        if (coroutineContext.isActive) {
            // The spring is critically damped, so in case spring-fling-spring sequence
            // is slightly offset and velocity is of the opposite sign, will end up with no animation
            overscrollOffset = Offset.Zero
        }

        if (!flingFromOverscroll) {
            currentVelocity = 0f
        }

        return currentVelocity
    }

    private fun Offset.reverseHorizontalIfNeeded(): Offset =
        Offset(
            if (reverseHorizontal) -x else x,
            y
        )

    private fun Offset.rubberBanded(): Offset {
        if (scrollSize.width == 0f || scrollSize.height == 0f) {
            return Offset.Zero
        }

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