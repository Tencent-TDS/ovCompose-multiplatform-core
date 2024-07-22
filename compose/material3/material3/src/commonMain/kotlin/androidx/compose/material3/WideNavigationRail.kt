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

package androidx.compose.material3

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.internal.systemBarsForVisualComponents
import androidx.compose.material3.tokens.ColorSchemeKeyTokens
import androidx.compose.material3.tokens.ShapeKeyTokens
import androidx.compose.material3.tokens.TypographyKeyTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrain
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.util.fastFirst
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastSumBy

/**
 * Material design wide navigation rail.
 *
 * Wide navigation rails provide access to primary destinations in apps when using tablet and
 * desktop screens.
 *
 * The wide navigation rail should be used to display multiple [WideNavigationRailItem]s, each
 * representing a singular app destination, and, optionally, a header containing a menu button, a
 * [FloatingActionButton], and/or a logo. Each destination is typically represented by an icon and a
 * text label.
 *
 * The [WideNavigationRail] is collapsed by default, but it also supports being expanded via the
 * value of [expanded]. When collapsed, the rail should display three to seven navigation items. A
 * simple example looks like:
 *
 * @sample androidx.compose.material3.samples.WideNavigationRailCollapsedSample
 *
 * When expanded, the rail should display at least three navigation items. A simple example looks
 * like:
 *
 * @sample androidx.compose.material3.samples.WideNavigationRailExpandedSample
 *
 * Finally, the [WideNavigationRail] also supports automatically animating between the collapsed and
 * expanded values. That can be done like so:
 *
 * @sample androidx.compose.material3.samples.WideNavigationRailResponsiveSample
 *
 * The [WideNavigationRail] supports setting an [NavigationRailArrangement] for the items, so that
 * the items can be grouped at the top (the default), at the middle, or at the bottom of the rail.
 * The header will always be at the top.
 *
 * See [WideNavigationRailItem] for configuration specific to each item, and not the overall
 * [WideNavigationRail] component.
 *
 * @param modifier the [Modifier] to be applied to this wide navigation rail
 * @param expanded whether this wide navigation rail is expanded or collapsed (default).
 * @param shape defines the shape of this wide navigation rail's container.
 * @param colors [NavigationRailColors] that will be used to resolve the colors used for this wide
 *   navigation rail. See [WideNavigationRailDefaults.colors]
 * @param header optional header that may hold a [FloatingActionButton] or a logo
 * @param windowInsets a window insets of the wide navigation rail
 * @param arrangement the [NavigationRailArrangement] of this wide navigation rail
 * @param content the content of this wide navigation rail, typically [WideNavigationRailItem]s
 *
 * TODO: Implement modal expanded option and add relevant params.
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun WideNavigationRail(
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    shape: Shape = WideNavigationRailDefaults.containerShape,
    colors: NavigationRailColors = WideNavigationRailDefaults.colors(),
    header: @Composable (() -> Unit)? = null,
    windowInsets: WindowInsets = WideNavigationRailDefaults.windowInsets,
    arrangement: NavigationRailArrangement = WideNavigationRailDefaults.arrangement,
    content: @Composable () -> Unit
) {
    WideNavigationRailLayout(
        modifier = modifier,
        expanded = expanded,
        colors = colors,
        shape = shape,
        header = header,
        windowInsets = windowInsets,
        arrangement = arrangement,
        content = content
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WideNavigationRailLayout(
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    colors: NavigationRailColors,
    shape: Shape,
    header: @Composable (() -> Unit)?,
    windowInsets: WindowInsets,
    arrangement: NavigationRailArrangement,
    content: @Composable () -> Unit
) {
    var currentWidth by remember { mutableIntStateOf(0) }
    var actualMaxExpandedWidth by remember { mutableIntStateOf(0) }

    val minWidth by
        animateDpAsState(
            targetValue = if (!expanded) CollapsedRailWidth else ExpandedRailMinWidth,
            animationSpec = AnimationSpec
        )
    val widthFullRange by
        animateDpAsState(
            targetValue = if (!expanded) CollapsedRailWidth else ExpandedRailMaxWidth,
            animationSpec = AnimationSpec
        )
    val itemVerticalSpacedBy by
        animateDpAsState(
            targetValue = if (!expanded) VerticalPaddingBetweenTopIconItems else 0.dp,
            animationSpec = AnimationSpec
        )
    val itemMarginStart by
        animateDpAsState(
            targetValue = if (!expanded) 0.dp else ExpandedRailHorizontalItemPadding,
            animationSpec = AnimationSpec
        )

    Surface(
        color = colors.containerColor,
        contentColor = colors.contentColor,
        shape = shape,
        modifier = modifier,
    ) {
        Layout(
            modifier =
                Modifier.fillMaxHeight()
                    .windowInsetsPadding(windowInsets)
                    .widthIn(max = ExpandedRailMaxWidth)
                    .padding(top = WNRVerticalPadding)
                    .selectableGroup(),
            content = {
                if (header != null) {
                    Box(Modifier.layoutId(HeaderLayoutIdTag)) { header() }
                }
                content()
            },
            measurePolicy =
                object : MeasurePolicy {
                    override fun MeasureScope.measure(
                        measurables: List<Measurable>,
                        constraints: Constraints
                    ): MeasureResult {
                        val height = constraints.maxHeight
                        var itemsCount = measurables.size
                        var actualExpandedMinWidth = constraints.minWidth
                        val actualMinWidth =
                            if (constraints.minWidth == 0) {
                                actualExpandedMinWidth =
                                    ExpandedRailMinWidth.roundToPx()
                                        .coerceAtMost(constraints.maxWidth)
                                minWidth.roundToPx().coerceAtMost(constraints.maxWidth)
                            } else {
                                constraints.minWidth
                            }
                        // If there are no items, rail will be empty.
                        if (itemsCount < 1) {
                            return layout(actualMinWidth, height) {}
                        }
                        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
                        var itemsMeasurables = measurables

                        var constraintsOffset = 0
                        var headerPlaceable: Placeable? = null
                        if (header != null) {
                            headerPlaceable =
                                measurables
                                    .fastFirst { it.layoutId == HeaderLayoutIdTag }
                                    .measure(looseConstraints)
                            // Header is always first element in measurables list.
                            if (itemsCount > 1)
                                itemsMeasurables = measurables.subList(1, itemsCount)
                            // Real item count doesn't include the header.
                            itemsCount--
                            constraintsOffset = headerPlaceable.height
                        }

                        val itemsPlaceables =
                            if (itemsCount > 0) mutableListOf<Placeable>() else null
                        val itemMaxWidthConstraint =
                            if (expanded) looseConstraints.maxWidth else actualMinWidth
                        var expandedItemMaxWidth = 0
                        if (itemsPlaceables != null) {
                            itemsMeasurables.fastMap {
                                val measuredItem =
                                    it.measure(
                                        looseConstraints
                                            .offset(vertical = -constraintsOffset)
                                            .constrain(
                                                Constraints.fitPrioritizingWidth(
                                                    minWidth =
                                                        Math.min(
                                                            ItemMinWidth.roundToPx(),
                                                            itemMaxWidthConstraint
                                                        ),
                                                    minHeight = WNRItemMinHeight.roundToPx(),
                                                    maxWidth = itemMaxWidthConstraint,
                                                    maxHeight = looseConstraints.maxHeight,
                                                )
                                            )
                                    )
                                val maxIntrinsicWidth = it.maxIntrinsicWidth(constraintsOffset)
                                if (expanded && expandedItemMaxWidth < maxIntrinsicWidth) {
                                    expandedItemMaxWidth =
                                        maxIntrinsicWidth +
                                            (ExpandedRailHorizontalItemPadding * 2).roundToPx()
                                }
                                constraintsOffset = measuredItem.height
                                itemsPlaceables.add(measuredItem)
                            }
                        }

                        var width = actualMinWidth
                        // Limit collapsed rail to fixed width, but expanded rail can be as wide as
                        // constraints.maxWidth
                        if (expanded) {
                            val widestElementWidth =
                                maxOf(expandedItemMaxWidth, headerPlaceable?.width ?: 0)

                            if (
                                widestElementWidth > actualMinWidth &&
                                    widestElementWidth > actualExpandedMinWidth
                            ) {
                                val widthConstrain =
                                    maxOf(widestElementWidth, actualExpandedMinWidth)
                                        .coerceAtMost(constraints.maxWidth)
                                // Use widthFullRange so there's no jump in animation for when the
                                // expanded width has to be wider than actualExpandedMinWidth.
                                width = widthFullRange.roundToPx().coerceAtMost(widthConstrain)
                                actualMaxExpandedWidth = width
                            }
                        } else {
                            if (actualMaxExpandedWidth > 0) {
                                // Use widthFullRange so there's no jump in animation for the case
                                // when the expanded width was wider than actualExpandedMinWidth.
                                width =
                                    widthFullRange
                                        .roundToPx()
                                        .coerceIn(
                                            minimumValue = actualMinWidth,
                                            maximumValue = currentWidth
                                        )
                            }
                        }
                        currentWidth = width

                        return layout(width, height) {
                            var y = 0
                            var headerHeight = 0
                            if (headerPlaceable != null && headerPlaceable.height > 0) {
                                headerPlaceable.placeRelative(0, y)
                                headerHeight = headerPlaceable.height
                                if (arrangement == NavigationRailArrangement.Top) {
                                    y += headerHeight + WNRHeaderPadding.roundToPx()
                                }
                            }

                            val itemsHeight = itemsPlaceables?.fastSumBy { it.height } ?: 0
                            val verticalPadding = itemVerticalSpacedBy.roundToPx()
                            if (arrangement == NavigationRailArrangement.Center) {
                                y =
                                    (height -
                                        WNRVerticalPadding.roundToPx() -
                                        (itemsHeight + (itemsCount - 1) * verticalPadding)) / 2
                                y = y.coerceAtLeast(headerHeight)
                            } else if (arrangement == NavigationRailArrangement.Bottom) {
                                y =
                                    height -
                                        WNRVerticalPadding.roundToPx() -
                                        (itemsHeight + (itemsCount - 1) * verticalPadding)
                                y = y.coerceAtLeast(headerHeight)
                            }
                            itemsPlaceables?.fastForEach { item ->
                                val x = itemMarginStart.roundToPx()
                                item.placeRelative(x, y)
                                y += item.height + verticalPadding
                            }
                        }
                    }
                }
        )
    }
}

/** Class that describes the different supported item arrangements of the [WideNavigationRail]. */
@ExperimentalMaterial3ExpressiveApi
@JvmInline
value class NavigationRailArrangement private constructor(private val value: Int) {
    companion object {
        /* The items are grouped at the top on the wide navigation Rail. */
        val Top = NavigationRailArrangement(0)

        /* The items are centered on the wide navigation Rail. */
        val Center = NavigationRailArrangement(1)

        /* The items are grouped at the bottom on the wide navigation Rail. */
        val Bottom = NavigationRailArrangement(2)
    }

    override fun toString() =
        when (this) {
            Top -> "Top"
            Center -> "Center"
            Bottom -> "Bottom"
            else -> "Unknown"
        }
}

/**
 * Represents the colors of the various elements of a wide navigation rail.
 *
 * @param containerColor the color used for the background of a wide navigation rail. Use
 *   [Color.Transparent] to have no color
 * @param contentColor the preferred color for content inside a wide navigation rail. Defaults to
 *   either the matching content color for [containerColor], or to the current [LocalContentColor]
 *   if [containerColor] is not a color from the theme
 */
@Immutable
class NavigationRailColors(
    val containerColor: Color,
    val contentColor: Color,
    /* TODO: Add color params related to the Modal option. */
) {
    /**
     * Returns a copy of this NavigationRailColors, optionally overriding some of the values. This
     * uses the Color.Unspecified to mean “use the value from the source”.
     */
    fun copy(
        containerColor: Color = this.containerColor,
        contentColor: Color = this.contentColor,
    ) =
        NavigationRailColors(
            containerColor = containerColor.takeOrElse { this.containerColor },
            contentColor = contentColor.takeOrElse { this.contentColor },
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is NavigationRailColors) return false

        if (containerColor != other.containerColor) return false
        if (contentColor != other.contentColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = containerColor.hashCode()
        result = 31 * result + contentColor.hashCode()

        return result
    }
}

/**
 * Material Design wide navigation rail item.
 *
 * It's recommend for navigation items to always have a text label. A [WideNavigationRailItem]
 * always displays labels (if they exist) when selected and unselected.
 *
 * The [WideNavigationRailItem] supports two different icon positions, top and start, which is
 * controlled by the [iconPosition] param:
 * - If the icon position is [NavigationItemIconPosition.Top] the icon will be displayed above the
 *   label. This configuration should be used with collapsed wide navigation rails.
 * - If the icon position is [NavigationItemIconPosition.Start] the icon will be displayed to the
 *   start of the label. This configuration should be used with expanded wide navigation rails.
 *
 * However, if an animated item is desired, the [iconPosition] can be controlled via the expanded
 * value of the associated [WideNavigationRail]. By default, it'll use the [railExpanded] to follow
 * the configuration described above.
 *
 * @param selected whether this item is selected
 * @param onClick called when this item is clicked
 * @param icon icon for this item, typically an [Icon]
 * @param modifier the [Modifier] to be applied to this item
 * @param enabled controls the enabled state of this item. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param label text label for this item
 * @param badge optional badge to show on this item, typically a [Badge]
 * @param railExpanded whether the associated [WideNavigationRail] is expanded or collapsed
 * @param iconPosition the [NavigationItemIconPosition] for the icon
 * @param colors [NavigationItemColors] that will be used to resolve the colors used for this item
 *   in different states. See [WideNavigationRailItemDefaults.colors]
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this item. You can use this to change the item's appearance or
 *   preview the item in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun WideNavigationRailItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: @Composable (() -> Unit)? = null,
    badge: (@Composable () -> Unit)? = null,
    railExpanded: Boolean = false,
    iconPosition: NavigationItemIconPosition =
        WideNavigationRailItemDefaults.iconPositionFor(railExpanded),
    colors: NavigationItemColors = WideNavigationRailItemDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }

    if (label != null) {
        AnimatedNavigationItem(
            selected = selected,
            onClick = onClick,
            icon = icon,
            indicatorShape = ActiveIndicatorShape.value,
            topIconIndicatorWidth = TopIconItemActiveIndicatorWidth,
            topIconLabelTextStyle = TopIconLabelTextFont.value,
            startIconLabelTextStyle = StartIconLabelTextFont.value,
            topIconIndicatorHorizontalPadding = ItemTopIconIndicatorHorizontalPadding,
            topIconIndicatorVerticalPadding = ItemTopIconIndicatorVerticalPadding,
            topIconIndicatorToLabelVerticalPadding = ItemTopIconIndicatorToLabelPadding,
            startIconIndicatorHorizontalPadding = ItemStartIconIndicatorHorizontalPadding,
            startIconIndicatorVerticalPadding = ItemStartIconIndicatorVerticalPadding,
            startIconToLabelHorizontalPadding = ItemStartIconToLabelPadding,
            startIconItemPadding = ExpandedRailHorizontalItemPadding,
            colors = colors,
            modifier = modifier,
            enabled = enabled,
            label = label,
            badge = badge,
            iconPosition = iconPosition,
            interactionSource = interactionSource,
        )
    } else {
        // If no label, default to circular indicator for the item.
        NavigationItem(
            selected = selected,
            onClick = onClick,
            icon = icon,
            labelTextStyle = TopIconLabelTextFont.value,
            indicatorShape = ActiveIndicatorShape.value,
            indicatorWidth = TopIconItemActiveIndicatorWidth,
            indicatorHorizontalPadding = WNRItemNoLabelIndicatorPadding,
            indicatorVerticalPadding = WNRItemNoLabelIndicatorPadding,
            indicatorToLabelVerticalPadding = 0.dp,
            startIconToLabelHorizontalPadding = 0.dp,
            topIconItemVerticalPadding = 0.dp,
            colors = colors,
            modifier = modifier,
            enabled = enabled,
            label = label,
            badge = badge,
            iconPosition = iconPosition,
            interactionSource = interactionSource,
        )
    }
}

/** Defaults used in [WideNavigationRail]. */
@ExperimentalMaterial3ExpressiveApi
object WideNavigationRailDefaults {
    /** Default container shape of a wide navigation rail. */
    // TODO: Replace with token.
    val containerShape: Shape
        @Composable get() = ShapeKeyTokens.CornerNone.value

    /** Default arrangement for a wide navigation rail. */
    val arrangement: NavigationRailArrangement
        get() = NavigationRailArrangement.Top

    /** Default window insets for a wide navigation rail. */
    val windowInsets: WindowInsets
        @Composable
        get() =
            WindowInsets.systemBarsForVisualComponents.only(
                WindowInsetsSides.Vertical + WindowInsetsSides.Start
            )

    /**
     * Creates a [NavigationRailColors] with the provided colors according to the Material
     * specification.
     */
    @Composable fun colors() = MaterialTheme.colorScheme.defaultWideNavigationRailColors

    private val ColorScheme.defaultWideNavigationRailColors: NavigationRailColors
        get() {
            return defaultWideNavigationRailColorsCached
                ?: NavigationRailColors(
                        // TODO: Replace with tokens.
                        containerColor = fromToken(ColorSchemeKeyTokens.Surface),
                        contentColor = fromToken(ColorSchemeKeyTokens.OnSurfaceVariant),
                    )
                    .also { defaultWideNavigationRailColorsCached = it }
        }
}

/** Defaults used in [WideNavigationRailItem]. */
@ExperimentalMaterial3ExpressiveApi
object WideNavigationRailItemDefaults {
    /**
     * The default icon position of a [WideNavigationRailItem] given whether the associated
     * [WideNavigationRail] is collapsed or expanded.
     */
    fun iconPositionFor(railExpanded: Boolean) =
        if (railExpanded) NavigationItemIconPosition.Start else NavigationItemIconPosition.Top

    /**
     * Creates a [NavigationItemColors] with the provided colors according to the Material
     * specification.
     */
    @Composable fun colors() = MaterialTheme.colorScheme.defaultWideNavigationRailItemColors

    private val ColorScheme.defaultWideNavigationRailItemColors: NavigationItemColors
        get() {
            return defaultWideNavigationRailItemColorsCached
                ?: NavigationItemColors(
                        selectedIconColor = fromToken(ActiveIconColor),
                        selectedTextColor = fromToken(ActiveLabelTextColor),
                        selectedIndicatorColor = fromToken(ActiveIndicatorColor),
                        unselectedIconColor = fromToken(InactiveIconColor),
                        unselectedTextColor = fromToken(InactiveLabelTextColor),
                        disabledIconColor =
                            fromToken(InactiveIconColor).copy(alpha = DisabledAlpha),
                        disabledTextColor =
                            fromToken(InactiveLabelTextColor).copy(alpha = DisabledAlpha),
                    )
                    .also { defaultWideNavigationRailItemColorsCached = it }
        }
}

private const val HeaderLayoutIdTag: String = "header"

/* TODO: Replace below values with tokens. */
private val AnimationSpec: AnimationSpec<Dp> = spring(dampingRatio = 0.8f, stiffness = 380f)
private val IconSize = 24.0.dp
private val TopIconItemActiveIndicatorWidth = 56.dp
private val TopIconItemActiveIndicatorHeight = 32.dp
private val StartIconItemActiveIndicatorHeight = 56.dp
private val NoLabelItemActiveIndicatorHeight = 56.dp
private val TopIconLabelTextFont = TypographyKeyTokens.LabelMedium
private val StartIconLabelTextFont = TypographyKeyTokens.LabelLarge
private val ActiveIndicatorShape = ShapeKeyTokens.CornerFull
// TODO: Update to OnSecondaryContainer once value matches Secondary.
private val ActiveIconColor = ColorSchemeKeyTokens.Secondary
private val ActiveLabelTextColor = ColorSchemeKeyTokens.Secondary
private val ActiveIndicatorColor = ColorSchemeKeyTokens.SecondaryContainer
private val InactiveIconColor = ColorSchemeKeyTokens.OnSurfaceVariant
private val InactiveLabelTextColor = ColorSchemeKeyTokens.OnSurfaceVariant
private val CollapsedRailWidth = 96.dp
private val ExpandedRailMinWidth = 220.dp
private val ExpandedRailMaxWidth = 360.dp
private val ExpandedRailHorizontalItemPadding = 20.dp
private val ItemStartIconIndicatorHorizontalPadding = 16.dp
private val ItemStartIconToLabelPadding = 8.dp

/*@VisibleForTesting*/
internal val WNRItemMinHeight = 48.dp
/*@VisibleForTesting*/
// Vertical padding between the contents of the wide navigation rail and its top/bottom.
internal val WNRVerticalPadding = 44.dp
/*@VisibleForTesting*/
// Padding at the bottom of the rail's header. This padding will only be added when the header is
// not null and the rail arrangement is Top.
internal val WNRHeaderPadding: Dp = 40.dp
/*@VisibleForTesting*/
internal val WNRItemNoLabelIndicatorPadding = (NoLabelItemActiveIndicatorHeight - IconSize) / 2

private val VerticalPaddingBetweenTopIconItems = 4.dp
private val ItemMinWidth = CollapsedRailWidth
private val ItemTopIconIndicatorVerticalPadding = (TopIconItemActiveIndicatorHeight - IconSize) / 2
private val ItemTopIconIndicatorHorizontalPadding = (TopIconItemActiveIndicatorWidth - IconSize) / 2
private val ItemStartIconIndicatorVerticalPadding =
    (StartIconItemActiveIndicatorHeight - IconSize) / 2
private val ItemTopIconIndicatorToLabelPadding: Dp = 4.dp
