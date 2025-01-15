/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.protolayout.material3

import androidx.annotation.Dimension
import androidx.annotation.Dimension.Companion.DP
import androidx.wear.protolayout.ColorBuilders.ColorProp
import androidx.wear.protolayout.DimensionBuilders.AngularLayoutConstraint
import androidx.wear.protolayout.DimensionBuilders.ContainerDimension
import androidx.wear.protolayout.DimensionBuilders.DegreesProp
import androidx.wear.protolayout.DimensionBuilders.ExpandedDimensionProp
import androidx.wear.protolayout.DimensionBuilders.WrappedDimensionProp
import androidx.wear.protolayout.DimensionBuilders.degrees
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.Arc
import androidx.wear.protolayout.LayoutElementBuilders.ArcSpacer
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.DashedArcLine
import androidx.wear.protolayout.LayoutElementBuilders.DashedLinePattern
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.expression.AnimationParameterBuilders.AnimationParameters
import androidx.wear.protolayout.expression.AnimationParameterBuilders.AnimationSpec
import androidx.wear.protolayout.expression.AnimationParameterBuilders.Easing
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicColor
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat
import androidx.wear.protolayout.material3.CircularProgressIndicatorDefaults.LARGE_STROKE_WIDTH
import androidx.wear.protolayout.material3.CircularProgressIndicatorDefaults.METADATA_TAG
import androidx.wear.protolayout.material3.CircularProgressIndicatorDefaults.calculateRecommendedGapSize
import androidx.wear.protolayout.material3.CircularProgressIndicatorDefaults.filledProgressIndicatorColors
import androidx.wear.protolayout.modifiers.LayoutModifier
import androidx.wear.protolayout.modifiers.contentDescription
import androidx.wear.protolayout.modifiers.tag
import androidx.wear.protolayout.modifiers.toProtoLayoutModifiers
import androidx.wear.protolayout.types.LayoutColor
import kotlin.math.min

/**
 * Protolayout Material3 design circular progress indicator.
 *
 * @param staticProgress The static progress of this progress indicator where 0 represent no
 *   progress and 1 represents completion. Progress above 1 is also allowed. If [dynamicProgress] is
 *   also set, this static value will be ignored. By default it equals to 0.
 * @param dynamicProgress The static progress of this progress indicator where 0 represent no
 *   progress and 1 represents completion. Progress above 1 is also allowed. If not provided, the
 *   [staticProgress] is used.
 * @param modifier Modifiers to set to this element. It's highly recommended to set a content
 *   description using [contentDescription].
 * @param startAngleDegrees The starting position of the progress arc, measured clockwise in degrees
 *   from the 12 o'clock position.
 * @param endAngleDegrees The ending position of the progress arc, measured clockwise in degrees
 *   from 12 o'clock position. Its value must be bigger than [startAngleDegrees], otherwise an
 *   exception would be thrown. By default it equals to 'startAngleDegrees + 360'.
 * @param strokeWidth The stroke width for the progress indicator. The recommended values are
 *   [CircularProgressIndicatorDefaults.LARGE_STROKE_WIDTH] and
 *   [CircularProgressIndicatorDefaults.SMALL_STROKE_WIDTH].
 * @param gapSize The size in dp of the gap between the ends of the progress indicator and the
 *   track. The stroke end caps are not included in this distance.
 * @param colors [ProgressIndicatorColors] that will be used to resolve the indicator and track
 *   color for this progress indicator.
 * @param size The bounding box size of this progress indicator, applies to both width and height.
 *   The indicator arc and track arc are located on the largest circle that can be inscribed inside.
 *   It is highly recommended for the progress indicator in a graphics card to have its size as
 *   [ExpandedDimensionProp], which is the default, to fill the available space for the best result
 *   across different screen sizes. Setting [size] with a [WrappedDimensionProp] instance will cause
 *   failure and throws an [IllegalArgumentException].
 * @throws IllegalArgumentException When [size] is set to be [WrappedDimensionProp] instance or the
 *   provided [endAngleDegrees] is smaller than the [startAngleDegrees].
 * @sample androidx.wear.protolayout.material3.samples.singleSegmentCircularProgressIndicator
 */
public fun MaterialScope.circularProgressIndicator(
    staticProgress: Float = 0F,
    dynamicProgress: DynamicFloat? = null,
    modifier: LayoutModifier = LayoutModifier,
    startAngleDegrees: Float = 0F,
    endAngleDegrees: Float = startAngleDegrees + 360F,
    @Dimension(unit = DP) strokeWidth: Float = LARGE_STROKE_WIDTH,
    @Dimension(unit = DP) gapSize: Float = calculateRecommendedGapSize(strokeWidth),
    colors: ProgressIndicatorColors = filledProgressIndicatorColors(),
    size: ContainerDimension = expand(),
): LayoutElement {
    if (size is WrappedDimensionProp) {
        throw IllegalArgumentException("CircularProgressIndicator could not have size as wrap")
    }

    val modifiers = (LayoutModifier.tag(METADATA_TAG) then modifier).toProtoLayoutModifiers()
    val validEndAngleDegrees = checkAndAdjustEndAngle(startAngleDegrees, endAngleDegrees)

    return singleSegmentImpl(
            startAngleDegrees = startAngleDegrees,
            endAngleDegrees = validEndAngleDegrees,
            staticProgress = staticProgress,
            dynamicProgress = dynamicProgress,
            strokeWidth = strokeWidth,
            gapSize = gapSize,
            colors = colors
        )
        .setModifiers(modifiers)
        .setWidth(size)
        .setHeight(size)
        .build()
}

/**
 * Layout the content for single segment progress indicator using [DashedArcLine].
 *
 * Note that we require valid start and end angles for calling this method.
 */
private fun MaterialScope.singleSegmentImpl(
    startAngleDegrees: Float,
    endAngleDegrees: Float,
    staticProgress: Float,
    dynamicProgress: DynamicFloat?,
    @Dimension(unit = DP) strokeWidth: Float,
    @Dimension(unit = DP) gapSize: Float,
    colors: ProgressIndicatorColors
): Box.Builder {
    val sweepAngle = endAngleDegrees - startAngleDegrees
    val progressInDegrees = progressInDegrees(sweepAngle, staticProgress, dynamicProgress)
    val trackInDegrees = trackInDegrees(sweepAngle, progressInDegrees)

    // Indicator: anchor end to startAngle, counter clockwise
    // | half gap spacer | progress arc | full gap |
    //                                  Track: anchor end to endAngle, clockwise
    //                                  | full gap |  indicator arc | half gap spacer|
    val linePattern =
        DashedLinePattern.Builder()
            // We set doubled gap size, as zero is the center of the gap, and we only draw
            // from zero, this gives us one gap size as commented above. This allows
            //    1. have a gap between indicator and track arclines
            //    2. have a gap between head and tail when the sweep angle is 360 and only one
            //       arcline is displayed (progress is zero or one).
            .setGapSize(gapSize * 2)
            .setGapLocations(0f)
            .build()
    val spacer =
        ArcSpacer.Builder().setAngularLength(dp(gapSize / 2)).setThickness(dp(strokeWidth)).build()
    return Box.Builder()
        .addContent( // the track
            createArc(
                    anchorAngle = degrees(endAngleDegrees),
                    anchorType = LayoutElementBuilders.ARC_ANCHOR_END,
                    arcLength = trackInDegrees,
                    arcColor = trackColor(staticProgress, dynamicProgress, colors),
                    strokeWidth = strokeWidth,
                    linePattern = linePattern,
                    arcDirection = LayoutElementBuilders.ARC_DIRECTION_CLOCKWISE
                )
                .addContent(spacer)
                .build()
        )
        .addContent( // the indicator
            createArc(
                    anchorAngle = degrees(-startAngleDegrees),
                    anchorType = LayoutElementBuilders.ARC_ANCHOR_END,
                    arcLength = progressInDegrees,
                    arcColor = colors.indicator.prop,
                    strokeWidth = strokeWidth,
                    linePattern = linePattern,
                    arcDirection = LayoutElementBuilders.ARC_DIRECTION_COUNTER_CLOCKWISE
                )
                .addContent(spacer)
                .build()
        )
}

public object CircularProgressIndicatorDefaults {
    /**
     * Returns the recommended [ProgressIndicatorColors] object to be used when placing the progress
     * indicator inside a graphic card with [CardDefaults.filledCardColors].
     */
    public fun MaterialScope.filledProgressIndicatorColors(): ProgressIndicatorColors =
        ProgressIndicatorColors(
            theme.colorScheme.onPrimary,
            theme.colorScheme.onPrimary.withOpacity(0.2F),
            theme.colorScheme.onPrimary.withOpacity(0.6F)
        )

    /**
     * Returns the recommended [ProgressIndicatorColors] object to be used when placing the progress
     * indicator inside a graphic card with [CardDefaults.filledTonalCardColors].
     */
    public fun MaterialScope.filledTonalProgressIndicatorColors(): ProgressIndicatorColors =
        ProgressIndicatorColors(
            theme.colorScheme.primary,
            theme.colorScheme.primary.withOpacity(0.2F),
            theme.colorScheme.primary.withOpacity(0.6F)
        )

    /**
     * Returns the recommended [ProgressIndicatorColors] object to be used when placing the progress
     * indicator inside a graphic card with [CardDefaults.filledVariantCardColors].
     */
    public fun MaterialScope.filledVariantProgressIndicatorColors(): ProgressIndicatorColors =
        ProgressIndicatorColors(
            theme.colorScheme.onPrimaryContainer,
            theme.colorScheme.onPrimaryContainer.withOpacity(0.2F),
            theme.colorScheme.onPrimaryContainer.withOpacity(0.6F),
        )

    /**
     * The recommended animation spec for animations from current progress to a new progress value.
     */
    public val recommendedAnimationSpec: AnimationSpec =
        AnimationSpec.Builder()
            .setAnimationParameters(
                AnimationParameters.Builder()
                    .setDurationMillis(450)
                    .setEasing(Easing.cubicBezier(0.2f, 0f, 0f, 1f))
                    .build()
            )
            .build()

    /** Large stroke width for circular progress indicator. */
    @Dimension(unit = DP) public const val LARGE_STROKE_WIDTH: Float = 8F

    /** Small stroke width for circular progress indicator. */
    @Dimension(unit = DP) public const val SMALL_STROKE_WIDTH: Float = 4F

    /**
     * Returns recommended size of the gap based on [strokeWidth].
     *
     * The absolute value can be customized with `gapSize` parameter on [circularProgressIndicator].
     */
    @Dimension(unit = DP)
    public fun calculateRecommendedGapSize(@Dimension(unit = DP) strokeWidth: Float): Float =
        strokeWidth / 3F

    internal const val METADATA_TAG: String = "M3CPI"
}

/**
 * Represents the indicator and track colors used in progress indicator.
 *
 * @param indicator Color used to draw the indicator of progress indicator.
 * @param track Color used to draw the track of progress indicator.
 * @param trackOverflow Color used to draw the track for progress overflow (>1).
 */
public class ProgressIndicatorColors(
    public val indicator: LayoutColor,
    public val track: LayoutColor,
    public val trackOverflow: LayoutColor = track
)

/**
 * A small offset to make the progress arc remaining when the progress is 1, with the module
 * operation applied for handling overflow.
 */
private const val TRIVIAL_ARC_OFFSET: Float = 0.05f

/*
 * Check the endAngle is valid with the provided startAngle, and adjust the sweep angle to be 360
 * maximum.
 * @throws IllegalArgumentException When the  engAngle is smaller than the startAngle.
 */
private fun checkAndAdjustEndAngle(startAngle: Float, endAngle: Float): Float {
    if (endAngle < startAngle) {
        throw IllegalArgumentException("End angle must be bigger than start angle")
    }

    // the maximum sweep angle is 360
    return min(endAngle, startAngle + 360F)
}

private fun progressInDegrees(
    sweepAngle: Float,
    staticProgress: Float,
    dynamicProgress: DynamicFloat?,
): DegreesProp =
    DegreesProp.Builder((staticProgress * sweepAngle - TRIVIAL_ARC_OFFSET).mod(sweepAngle))
        .apply {
            dynamicProgress?.let {
                setDynamicValue(it.times(sweepAngle).minus(TRIVIAL_ARC_OFFSET).rem(sweepAngle))
            }
        }
        .build()

private fun trackInDegrees(sweepAngle: Float, progressInDegrees: DegreesProp): DegreesProp =
    DegreesProp.Builder(sweepAngle - progressInDegrees.value)
        .apply {
            progressInDegrees.dynamicValue?.let {
                setDynamicValue(DynamicFloat.constant(sweepAngle).minus(it))
            }
        }
        .build()

private fun trackColor(
    staticProgress: Float,
    dynamicProgress: DynamicFloat?,
    colors: ProgressIndicatorColors
): ColorProp =
    ColorProp.Builder(
            if (staticProgress > 1) colors.trackOverflow.prop.argb else colors.track.prop.argb
        )
        .apply {
            dynamicProgress?.let {
                setDynamicValue(
                    DynamicColor.onCondition(it.gt(1F))
                        .use(colors.trackOverflow.prop.argb)
                        .elseUse(colors.track.prop.argb)
                )
            }
        }
        .build()

private fun createArc(
    anchorAngle: DegreesProp,
    anchorType: Int,
    arcLength: DegreesProp,
    arcColor: ColorProp,
    @Dimension(unit = DP) strokeWidth: Float,
    linePattern: DashedLinePattern,
    arcDirection: Int
): Arc.Builder {
    return Arc.Builder()
        .setAnchorAngle(anchorAngle)
        .setAnchorType(anchorType)
        .setArcDirection(arcDirection)
        .addContent(
            DashedArcLine.Builder()
                .setColor(arcColor)
                .setThickness(strokeWidth)
                .setLinePattern(linePattern)
                .setLength(arcLength)
                .setLayoutConstraintsForDynamicLength(
                    // We use one Arc container to put one arcline, so it is fine to put 360 here
                    // as layout constraint.
                    AngularLayoutConstraint.Builder(360f).setAngularAlignment(anchorType).build()
                )
                .build()
        )
}
