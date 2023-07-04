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

package androidx.compose.foundation.cupertino

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.OverscrollEffect
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
import kotlin.coroutines.coroutineContext
import kotlin.math.abs
import kotlin.math.sign
import kotlinx.coroutines.isActive

private enum class CupertinoScrollSource {
    DRAG, FLING
}

private enum class CupertinoOverscrollDirection {
    UNKNOWN, VERTICAL, HORIZONTAL
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
     * Direction of scrolling for this overscroll effect, derived from arguments during
     * [applyToScroll] calls. Technically this effect supports both dimensions, but current API requires
     * that different stages of animations spawned by this effect for both dimensions
     * end at the same time, which is not the case:
     * Spring->Fling->Spring, Fling->Spring, Spring->Fling effects can have different timing per dimension
     * (see Notes of https://github.com/JetBrains/compose-multiplatform-core/pull/609),
     * which is not possible to express without changing API. Hence this effect will be fixed to latest
     * received delta.
     */
    private var direction: CupertinoOverscrollDirection = CupertinoOverscrollDirection.UNKNOWN

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
    ): Offset {
        direction = direction.combinedWith(delta.toCupertinoOverscrollDirection())

        return source.toCupertinoScrollSource()?.let {
            applyToScroll(delta, it, performScroll)
        } ?: performScroll(delta)
    }

    override suspend fun applyToFling(
        velocity: Velocity,
        performFling: suspend (Velocity) -> Velocity
    ) {
        val availableFlingVelocity = playInitialSpringAnimationIfNeeded(velocity)

        val velocityConsumedByFling = performFling(availableFlingVelocity)
        val postFlingVelocity = availableFlingVelocity - velocityConsumedByFling

        playSpringAnimation(
            lastFlingUncosumedDelta.toFloat(),
            postFlingVelocity.toFloat(),
            false
        )
    }

    private fun Offset.toCupertinoOverscrollDirection(): CupertinoOverscrollDirection {
        val epsilon = 1E-4f

        val hasXPart = abs(x) > epsilon
        val hasYPart = abs(y) > epsilon

        return if (hasXPart xor hasYPart) {
            if (hasXPart) {
                CupertinoOverscrollDirection.HORIZONTAL
            } else {
                // hasYPart != hasXPart and hasXPart is false
                CupertinoOverscrollDirection.VERTICAL
            }
        } else {
            // hasXPart and hasYPart are equal
            CupertinoOverscrollDirection.UNKNOWN
        }
    }

    private fun CupertinoOverscrollDirection.combinedWith(other: CupertinoOverscrollDirection): CupertinoOverscrollDirection =
        when (this) {
            CupertinoOverscrollDirection.UNKNOWN -> when (other) {
                CupertinoOverscrollDirection.UNKNOWN -> CupertinoOverscrollDirection.UNKNOWN
                CupertinoOverscrollDirection.VERTICAL -> CupertinoOverscrollDirection.VERTICAL
                CupertinoOverscrollDirection.HORIZONTAL -> CupertinoOverscrollDirection.HORIZONTAL
            }

            CupertinoOverscrollDirection.VERTICAL -> when (other) {
                CupertinoOverscrollDirection.UNKNOWN, CupertinoOverscrollDirection.VERTICAL -> CupertinoOverscrollDirection.VERTICAL
                CupertinoOverscrollDirection.HORIZONTAL -> CupertinoOverscrollDirection.HORIZONTAL
            }

            CupertinoOverscrollDirection.HORIZONTAL -> when (other) {
                CupertinoOverscrollDirection.UNKNOWN, CupertinoOverscrollDirection.HORIZONTAL -> CupertinoOverscrollDirection.HORIZONTAL
                CupertinoOverscrollDirection.VERTICAL -> CupertinoOverscrollDirection.VERTICAL
            }
        }
    private fun Velocity.toOffset(): Offset =
        Offset(x, y)

    private fun Offset.toVelocity(): Velocity =
        Velocity(x, y)

    private fun Velocity.toFloat(): Float =
        toOffset().toFloat()

    private fun Float.toVelocity(): Velocity =
        toOffset().toVelocity()

    private fun Offset.toFloat(): Float =
        when (direction) {
            CupertinoOverscrollDirection.UNKNOWN -> 0f
            CupertinoOverscrollDirection.VERTICAL -> y
            CupertinoOverscrollDirection.HORIZONTAL -> x
        }

    private fun Float.toOffset(): Offset =
        when (direction) {
            CupertinoOverscrollDirection.UNKNOWN -> Offset.Zero
            CupertinoOverscrollDirection.VERTICAL -> Offset(0f, this)
            CupertinoOverscrollDirection.HORIZONTAL -> Offset(this, 0f)
        }

    private suspend fun playInitialSpringAnimationIfNeeded(initialVelocity: Velocity): Velocity {
        val velocity = initialVelocity.toFloat()
        val overscroll = overscrollOffset.toFloat()

        return if ((velocity < 0f && overscroll > 0f) || (velocity > 0f && overscroll < 0f)) {
            playSpringAnimation(0f, velocity, true).toVelocity()
        } else {
            initialVelocity
        }
    }

    private suspend fun playSpringAnimation(
        unconsumedDelta: Float,
        initialVelocity: Float,
        flingFromOverscroll: Boolean
    ): Float {
        val initialValue = overscrollOffset.toFloat() + unconsumedDelta

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
            overscrollOffset = (value * density).toOffset()
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