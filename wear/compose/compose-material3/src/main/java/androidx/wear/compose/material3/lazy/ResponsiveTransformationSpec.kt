/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.wear.compose.material3.lazy

import androidx.annotation.FloatRange
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScrollProgress
import androidx.wear.compose.foundation.lazy.inverseLerp
import kotlin.math.ceil

/**
 * Version of [TransformationSpec] that supports variable screen sizes.
 *
 * Use this API to define custom transformations for the items as they scroll across the screen.
 *
 * @sample androidx.wear.compose.material3.samples.ResponsiveTransformationSpecButtonSample
 */
public sealed interface ResponsiveTransformationSpec : TransformationSpec

/** Contains the default values used by [ResponsiveTransformationSpec] */
public object ResponsiveTransformationSpecDefaults {
    /** The default spec configuration point for the small screen size. */
    public val SmallScreenSizeDp: Int = 192

    /** The default spec configuration point for the large screen size. */
    public val LargeScreenSizeDp: Int = 240

    /**
     * Default [TransformationSpec] for small watch screen size [SmallScreenSizeDp].
     *
     * This spec should be used in [rememberResponsiveTransformationSpec] together with the specs
     * for another screen sizes to support different watch sizes.
     */
    public fun smallScreenSpec(
        /** The screen size of the small watch. */
        screenSizeDp: Int = SmallScreenSizeDp,

        /**
         * The minimum element height, as a ratio of the viewport height, to use for determining the
         * transition area height within the range
         * [minTransitionAreaHeightFraction]..[maxTransitionAreaHeightFraction]. Given a content
         * item, this defines the start and end points for transitioning the item. Item heights
         * lower than [minElementHeightFraction] will be treated as if [minElementHeightFraction].
         * Must be smaller than or equal to [maxElementHeightFraction].
         */
        @FloatRange(from = 0.0, to = 1.0) minElementHeightFraction: Float = 0.2f,

        /**
         * The maximum element height, as a ratio of the viewport height, to use for determining the
         * transition area height within the range
         * [minTransitionAreaHeightFraction]..[maxTransitionAreaHeightFraction]. Given a content
         * item, this defines the start and end points for transitioning the item. Item heights
         * higher than [maxElementHeightFraction] will be treated as if [maxElementHeightFraction].
         * Must be greater than or equal to [minElementHeightFraction].
         */
        @FloatRange(from = 0.0, to = 1.0) maxElementHeightFraction: Float = 0.6f,

        /**
         * The lower bound of the range of heights for the transition area, i.e. how tall the
         * transition area is for items of height [minElementHeightFraction] or shorter. Taller
         * items will have taller transition areas, up to [maxTransitionAreaHeightFraction]. This is
         * defined as a fraction (value between 0f..1f) of the viewport height. Must be less than or
         * equal to [maxTransitionAreaHeightFraction].
         *
         * Note that the transition area is the same for all variables, but each variable can define
         * a transformation zone inside it in which the transformations will actually occur. See
         * [TransformationVariableSpec].
         */
        @FloatRange(from = 0.0, to = 1.0) minTransitionAreaHeightFraction: Float = 0.35f,

        /**
         * The upper bound of the range of heights for the transition area, i.e. how tall the
         * transition area is for items of height [maxElementHeightFraction] or taller. Shorter
         * items will have shorter transition areas, down to [minTransitionAreaHeightFraction]. This
         * is defined as a fraction (value between 0f..1f) of the viewport height. Must be greater
         * than or equal to [minTransitionAreaHeightFraction].
         *
         * Note that the transition area is the same for all variables, but each variable can define
         * a transformation zone inside it in which the transformations will actually occur. See
         * [TransformationVariableSpec].
         */
        @FloatRange(from = 0.0, to = 1.0) maxTransitionAreaHeightFraction: Float = 0.55f,

        /**
         * An interpolator to use to determine how to apply transformations as the item transitions
         * across the transformation zones.
         */
        easing: Easing = CubicBezierEasing(0.3f, 0f, 0.7f, 1f),

        /** Configuration for how the container (background) of the item will fade in/out. */
        containerAlpha: TransformationVariableSpec = TransformationVariableSpec(0.5f),

        /** Configuration for how the content of the item will fade in/out. */
        contentAlpha: TransformationVariableSpec = TransformationVariableSpec(0.5f),

        /** Configuration for scaling the whole item (container and content). */
        scale: TransformationVariableSpec = TransformationVariableSpec(0.7f),
    ): ResponsiveTransformationSpec =
        ResponsiveTransformationSpecImpl(
            screenSizeDp = screenSizeDp,
            minElementHeightFraction = minElementHeightFraction,
            maxElementHeightFraction = maxElementHeightFraction,
            minTransitionAreaHeightFraction = minTransitionAreaHeightFraction,
            maxTransitionAreaHeightFraction = maxTransitionAreaHeightFraction,
            easing = easing,
            containerAlpha = containerAlpha,
            contentAlpha = contentAlpha,
            scale = scale,
        )

    /**
     * Default [TransformationSpec] for large watch screen size [LargeScreenSizeDp].
     *
     * This spec should be used in [rememberResponsiveTransformationSpec] together with the specs
     * for another screen sizes to support different watch sizes.
     */
    public fun largeScreenSpec(
        /** The screen size of the large watch. */
        screenSizeDp: Int = LargeScreenSizeDp,

        /**
         * The minimum element height, as a ratio of the viewport height, to use for determining the
         * transition area height within the range
         * [minTransitionAreaHeightFraction]..[maxTransitionAreaHeightFraction]. Given a content
         * item, this defines the start and end points for transitioning the item. Item heights
         * lower than [minElementHeightFraction] will be treated as if [minElementHeightFraction].
         * Must be smaller than or equal to [maxElementHeightFraction].
         */
        @FloatRange(from = 0.0, to = 1.0) minElementHeightFraction: Float = 0.15f,

        /**
         * The maximum element height, as a ratio of the viewport height, to use for determining the
         * transition area height within the range
         * [minTransitionAreaHeightFraction]..[maxTransitionAreaHeightFraction]. Given a content
         * item, this defines the start and end points for transitioning the item. Item heights
         * higher than [maxElementHeightFraction] will be treated as if [maxElementHeightFraction].
         * Must be greater than or equal to [minElementHeightFraction].
         */
        @FloatRange(from = 0.0, to = 1.0) maxElementHeightFraction: Float = 0.45f,

        /**
         * The lower bound of the range of heights for the transition area, i.e. how tall the
         * transition area is for items of height [minElementHeightFraction] or shorter. Taller
         * items will have taller transition areas, up to [maxTransitionAreaHeightFraction]. This is
         * defined as a fraction (value between 0f..1f) of the viewport height. Must be less than or
         * equal to [maxTransitionAreaHeightFraction].
         *
         * Note that the transition area is the same for all variables, but each variable can define
         * a transformation zone inside it in which the transformations will actually occur. See
         * [TransformationVariableSpec].
         */
        @FloatRange(from = 0.0, to = 1.0) minTransitionAreaHeightFraction: Float = 0.4f,

        /**
         * The upper bound of the range of heights for the transition area, i.e. how tall the
         * transition area is for items of height [maxElementHeightFraction] or taller. Shorter
         * items will have shorter transition areas, down to [minTransitionAreaHeightFraction]. This
         * is defined as a fraction (value between 0f..1f) of the viewport height. Must be greater
         * than or equal to [minTransitionAreaHeightFraction].
         *
         * Note that the transition area is the same for all variables, but each variable can define
         * a transformation zone inside it in which the transformations will actually occur. See
         * [TransformationVariableSpec].
         */
        @FloatRange(from = 0.0, to = 1.0) maxTransitionAreaHeightFraction: Float = 0.6f,

        /**
         * An interpolator to use to determine how to apply transformations as the item transitions
         * across the transformation zones.
         */
        easing: Easing = CubicBezierEasing(0.3f, 0f, 0.7f, 1f),

        /** Configuration for how the container (background) of the item will fade in/out. */
        containerAlpha: TransformationVariableSpec = TransformationVariableSpec(0.5f),

        /** Configuration for how the content of the item will fade in/out. */
        contentAlpha: TransformationVariableSpec = TransformationVariableSpec(0.5f),

        /** Configuration for scaling the whole item (container and content). */
        scale: TransformationVariableSpec = TransformationVariableSpec(0.6f),
    ): ResponsiveTransformationSpec =
        ResponsiveTransformationSpecImpl(
            screenSizeDp = screenSizeDp,
            minElementHeightFraction = minElementHeightFraction,
            maxElementHeightFraction = maxElementHeightFraction,
            minTransitionAreaHeightFraction = minTransitionAreaHeightFraction,
            maxTransitionAreaHeightFraction = maxTransitionAreaHeightFraction,
            easing = easing,
            containerAlpha = containerAlpha,
            contentAlpha = contentAlpha,
            scale = scale,
        )
}

/**
 * Computes and remembers the appropriate [TransformationSpec] for the current screen size, given
 * one or more [TransformationSpec]s for different screen sizes.
 *
 * This shows use of the [ResponsiveTransformationSpec] which is a recommended [TransformationSpec]
 * for large-screen aware Wear apps:
 *
 * @sample androidx.wear.compose.material3.samples.ResponsiveTransformationSpecButtonSample
 */
@Composable
public fun rememberResponsiveTransformationSpec(
    vararg specs: ResponsiveTransformationSpec
): ResponsiveTransformationSpec {
    val screenSizeDp = LocalConfiguration.current.screenHeightDp
    val transformationSpecs =
        if (specs.isEmpty()) {
                listOf(
                    ResponsiveTransformationSpecDefaults.smallScreenSpec(),
                    ResponsiveTransformationSpecDefaults.largeScreenSpec()
                )
            } else {
                specs.toList()
            }
            .map { it as ResponsiveTransformationSpecImpl }

    return remember { responsiveTransformationSpec(screenSizeDp, transformationSpecs) }
}

/** This class contains all parameters needed to configure the transformations for a single item */
internal class ResponsiveTransformationSpecImpl(
    /** The screen size of the watch. */
    val screenSizeDp: Int,

    /**
     * The minimum element height, as a ratio of the viewport height, to use for determining the
     * transition area height within the range
     * [minTransitionAreaHeightFraction]..[maxTransitionAreaHeightFraction]. Given a content item,
     * this defines the start and end points for transitioning the item. Item heights lower than
     * [minElementHeightFraction] will be treated as if [minElementHeightFraction]. Must be smaller
     * than or equal to [maxElementHeightFraction].
     */
    val minElementHeightFraction: Float,

    /**
     * The maximum element height, as a ratio of the viewport height, to use for determining the
     * transition area height within the range
     * [minTransitionAreaHeightFraction]..[maxTransitionAreaHeightFraction]. Given a content item,
     * this defines the start and end points for transitioning the item. Item heights higher than
     * [maxElementHeightFraction] will be treated as if [maxElementHeightFraction]. Must be greater
     * than or equal to [minElementHeightFraction].
     */
    val maxElementHeightFraction: Float,

    /**
     * The lower bound of the range of heights for the transition area, i.e. how tall the transition
     * area is for items of height [minElementHeightFraction] or shorter. Taller items will have
     * taller transition areas, up to [maxTransitionAreaHeightFraction]. This is defined as a
     * fraction (value between 0f..1f) of the viewport height. Must be less than or equal to
     * [maxTransitionAreaHeightFraction].
     *
     * Note that the transition area is the same for all variables, but each variable can define a
     * transformation zone inside it in which the transformations will actually occur. See
     * [TransformationVariableSpec].
     */
    val minTransitionAreaHeightFraction: Float,

    /**
     * The upper bound of the range of heights for the transition area, i.e. how tall the transition
     * area is for items of height [maxElementHeightFraction] or taller. Shorter items will have
     * shorter transition areas, down to [minTransitionAreaHeightFraction]. This is defined as a
     * fraction (value between 0f..1f) of the viewport height. Must be greater than or equal to
     * [minTransitionAreaHeightFraction].
     *
     * Note that the transition area is the same for all variables, but each variable can define a
     * transformation zone inside it in which the transformations will actually occur. See
     * [TransformationVariableSpec].
     */
    val maxTransitionAreaHeightFraction: Float,

    /**
     * An interpolator to use to determine how to apply transformations as the item transitions
     * across the transformation zones.
     */
    val easing: Easing,

    /** Configuration for how the container (background) of the item will fade in/out. */
    val containerAlpha: TransformationVariableSpec,

    /** Configuration for how the content of the item will fade in/out. */
    val contentAlpha: TransformationVariableSpec,

    /** Configuration for scaling the whole item (container and content). */
    val scale: TransformationVariableSpec,
) : ResponsiveTransformationSpec {
    init {
        // The element height range must be non-empty.
        require(minElementHeightFraction < maxElementHeightFraction) {
            "minElementHeight must be smaller than maxElementHeight"
        }
    }

    override fun getTransformedHeight(
        measuredHeight: Int,
        scrollProgress: TransformingLazyColumnItemScrollProgress
    ): Int =
        with(transformProgress(scrollProgress, this)) {
            ceil(compute(scale, easing) * measuredHeight).fastRoundToInt()
        }

    override fun GraphicsLayerScope.applyContentTransformation(
        scrollProgress: TransformingLazyColumnItemScrollProgress,
    ) {
        if (scrollProgress.isUnspecified) return
        with(
            transformationState(
                spec = this@ResponsiveTransformationSpecImpl,
                itemHeight = size.height,
                scrollProgress = scrollProgress
            )
        ) {
            compositingStrategy = CompositingStrategy.Offscreen
            alpha = contentAlpha
        }
    }

    override fun GraphicsLayerScope.applyContainerTransformation(
        scrollProgress: TransformingLazyColumnItemScrollProgress
    ) {
        if (scrollProgress.isUnspecified) return
        with(
            transformationState(
                spec = this@ResponsiveTransformationSpecImpl,
                itemHeight = size.height,
                scrollProgress = scrollProgress
            )
        ) {
            compositingStrategy = CompositingStrategy.Offscreen
            translationY = -1f * size.height * (1f - scale) / 2f
            alpha = containerAlpha
            scaleX = scale
            scaleY = scale
        }
    }

    override fun TransformedPainterScope.createTransformedPainter(
        painter: Painter,
        shape: Shape,
        border: BorderStroke?
    ): Painter =
        BackgroundPainter(
            transformState = {
                transformationState(
                    spec = this@ResponsiveTransformationSpecImpl,
                    itemHeight = itemHeight,
                    scrollProgress = scrollProgress,
                )
            },
            shape = shape,
            border = border,
            backgroundPainter = painter
        )
}

private fun lerp(
    start: ResponsiveTransformationSpecImpl,
    stop: ResponsiveTransformationSpecImpl,
    progress: Float
) =
    ResponsiveTransformationSpecImpl(
        screenSizeDp = lerp(start.screenSizeDp, stop.screenSizeDp, progress),
        minElementHeightFraction =
            lerp(start.minElementHeightFraction, stop.minElementHeightFraction, progress),
        maxElementHeightFraction =
            lerp(start.maxElementHeightFraction, stop.maxElementHeightFraction, progress),
        minTransitionAreaHeightFraction =
            lerp(
                start.minTransitionAreaHeightFraction,
                stop.minTransitionAreaHeightFraction,
                progress
            ),
        maxTransitionAreaHeightFraction =
            lerp(
                start.maxTransitionAreaHeightFraction,
                stop.maxTransitionAreaHeightFraction,
                progress
            ),
        easing = { fraction ->
            lerp(start.easing.transform(fraction), stop.easing.transform(fraction), progress)
        },
        containerAlpha = lerp(start.containerAlpha, stop.containerAlpha, progress),
        contentAlpha = lerp(start.contentAlpha, stop.contentAlpha, progress),
        scale = lerp(start.scale, stop.scale, progress),
    )

/**
 * Computes the appropriate [ResponsiveTransformationSpecImpl] for a given screen size, given one or
 * more [ResponsiveTransformationSpecImpl]s for different screen sizes.
 */
internal fun responsiveTransformationSpec(
    screenSizeDp: Int,
    specs: List<ResponsiveTransformationSpecImpl>
): ResponsiveTransformationSpecImpl {
    require(specs.isNotEmpty()) { "Must provide at least one TransformationSpec" }

    val sortedSpecs = specs.sortedBy { it.screenSizeDp }

    if (screenSizeDp <= sortedSpecs.first().screenSizeDp) return sortedSpecs.first()
    if (screenSizeDp >= sortedSpecs.last().screenSizeDp) return sortedSpecs.last()

    var ix = 1 // We checked and it's greater than the first element's screen size.
    while (ix < sortedSpecs.size && screenSizeDp > sortedSpecs[ix].screenSizeDp) ix++

    return lerp(
        sortedSpecs[ix - 1],
        sortedSpecs[ix],
        inverseLerp(
            sortedSpecs[ix - 1].screenSizeDp.toFloat(),
            sortedSpecs[ix].screenSizeDp.toFloat(),
            screenSizeDp.toFloat()
        )
    )
}
