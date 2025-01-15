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

import android.R.attr.clickable
import androidx.annotation.Dimension
import androidx.annotation.Dimension.Companion.DP
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.LayoutElementBuilders.VERTICAL_ALIGN_CENTER
import androidx.wear.protolayout.LayoutElementBuilders.VerticalAlignment
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.ModifiersBuilders.Padding
import androidx.wear.protolayout.ModifiersBuilders.SEMANTICS_ROLE_BUTTON
import androidx.wear.protolayout.material3.ButtonDefaults.filledButtonColors
import androidx.wear.protolayout.material3.EdgeButtonDefaults.BOTTOM_MARGIN_DP
import androidx.wear.protolayout.material3.EdgeButtonDefaults.EDGE_BUTTON_HEIGHT_DP
import androidx.wear.protolayout.material3.EdgeButtonDefaults.HORIZONTAL_MARGIN_PERCENT_LARGE
import androidx.wear.protolayout.material3.EdgeButtonDefaults.HORIZONTAL_MARGIN_PERCENT_SMALL
import androidx.wear.protolayout.material3.EdgeButtonDefaults.ICON_SIZE_DP
import androidx.wear.protolayout.material3.EdgeButtonDefaults.METADATA_TAG
import androidx.wear.protolayout.material3.EdgeButtonDefaults.TEXT_SIDE_PADDING_DP
import androidx.wear.protolayout.material3.EdgeButtonDefaults.TEXT_TOP_PADDING_DP
import androidx.wear.protolayout.material3.EdgeButtonDefaults.TOP_CORNER_RADIUS
import androidx.wear.protolayout.material3.EdgeButtonStyle.Companion.DEFAULT
import androidx.wear.protolayout.material3.EdgeButtonStyle.Companion.TOP_ALIGN
import androidx.wear.protolayout.modifiers.LayoutModifier
import androidx.wear.protolayout.modifiers.background
import androidx.wear.protolayout.modifiers.clickable
import androidx.wear.protolayout.modifiers.clip
import androidx.wear.protolayout.modifiers.clipBottomLeft
import androidx.wear.protolayout.modifiers.clipBottomRight
import androidx.wear.protolayout.modifiers.contentDescription
import androidx.wear.protolayout.modifiers.padding
import androidx.wear.protolayout.modifiers.semanticsRole
import androidx.wear.protolayout.modifiers.tag
import androidx.wear.protolayout.modifiers.toProtoLayoutModifiers

/**
 * ProtoLayout Material3 component edge button that offers a single slot to take an icon or similar
 * round, small content.
 *
 * The edge button is intended to be used at the bottom of a round screen. It has a special shape
 * with its bottom almost follows the screen's curvature. It has fixed height, and takes 1 line of
 * text or a single icon. This button represents the most important action on the screen, and it
 * must occupy the whole horizontal space in its position as well as being anchored to the screen
 * bottom.
 *
 * This component is not intended to be used with an image background.
 *
 * @param onClick Associated [Clickable] for click events. When the button is clicked it will fire
 *   the associated action.
 * @param modifier Modifiers to set to this element. It's highly recommended to set a content
 *   description using [contentDescription].
 * @param colors The colors used for this button. If not set, [ButtonDefaults.filledButtonColors]
 *   will be used as high emphasis button. Other recommended colors are
 *   [ButtonDefaults.filledTonalButtonColors] and [ButtonDefaults.filledVariantButtonColors]. If
 *   using custom colors, it is important to choose a color pair from same role to ensure
 *   accessibility with sufficient color contrast.
 * @param iconContent The icon slot for content displayed in this button. It is recommended to use
 *   default styling that is automatically provided by only calling [icon] with the resource ID.
 * @sample androidx.wear.protolayout.material3.samples.edgeButtonSampleIcon
 */
// TODO: b/346958146 - link EdgeButton visuals in DAC
public fun MaterialScope.iconEdgeButton(
    onClick: Clickable,
    modifier: LayoutModifier = LayoutModifier,
    colors: ButtonColors = filledButtonColors(),
    iconContent: (MaterialScope.() -> LayoutElement)
): LayoutElement =
    edgeButton(onClick = onClick, modifier = modifier, colors = colors, style = DEFAULT) {
        withStyle(defaultIconStyle = IconStyle(size = ICON_SIZE_DP.toDp(), tintColor = colors.icon))
            .iconContent()
    }

/**
 * ProtoLayout Material3 component edge button that offers a single slot to take a text or similar
 * long and wide content.
 *
 * The edge button is intended to be used at the bottom of a round screen. It has a special shape
 * with its bottom almost follows the screen's curvature. It has fixed height, and takes 1 line of
 * text or a single icon. This button represents the most important action on the screen, and it
 * must occupy the whole horizontal space in its position as well as being anchored to the screen
 * bottom.
 *
 * This component is not intended to be used with an image background.
 *
 * @param onClick Associated [Clickable] for click events. When the button is clicked it will fire
 *   the associated action.
 * @param modifier Modifiers to set to this element. It's highly recommended to set a content
 *   description using [contentDescription].
 * @param colors The colors used for this button. If not set, [ButtonDefaults.filledButtonColors]
 *   will be used as high emphasis button. Other recommended colors are
 *   [ButtonDefaults.filledTonalButtonColors] and [ButtonDefaults.filledVariantButtonColors]. If
 *   using custom colors, it is important to choose a color pair from same role to ensure
 *   accessibility with sufficient color contrast.
 * @param labelContent The label slot for content displayed in this button. It is recommended to use
 *   default styling that is automatically provided by only calling [text] with the content.
 * @sample androidx.wear.protolayout.material3.samples.edgeButtonSampleText
 */
// TODO(b/346958146): link EdgeButton visuals in DAC
public fun MaterialScope.textEdgeButton(
    onClick: Clickable,
    modifier: LayoutModifier = LayoutModifier,
    colors: ButtonColors = filledButtonColors(),
    labelContent: (MaterialScope.() -> LayoutElement)
): LayoutElement =
    edgeButton(onClick = onClick, modifier = modifier, colors = colors, style = TOP_ALIGN) {
        withStyle(
                defaultTextElementStyle =
                    TextElementStyle(
                        typography = Typography.LABEL_MEDIUM,
                        color = colors.icon,
                        scalable = false
                    )
            )
            .labelContent()
    }

/**
 * ProtoLayout Material3 component edge button that offers a single slot to take any content.
 *
 * The edge button is intended to be used at the bottom of a round screen. It has a special shape
 * with its bottom almost follows the screen's curvature. It has fixed height, and takes 1 line of
 * text or a single icon. This button represents the most important action on the screen, and it
 * must occupy the whole horizontal space in its position as well as being anchored to the screen
 * bottom.
 *
 * This component is not intended to be used with an image background.
 *
 * @param onClick Associated [Clickable] for click events. When the button is clicked it will fire
 *   the associated action.
 * @param colors The colors used for this button. If not set, [ButtonDefaults.filledButtonColors]
 *   will be used as high emphasis button. Other recommended colors are
 *   [ButtonDefaults.filledTonalButtonColors] and [ButtonDefaults.filledVariantButtonColors]. If
 *   using custom colors, it is important to choose a color pair from same role to ensure
 *   accessibility with sufficient color contrast.
 * @param modifier Modifiers to set to this element. It's highly recommended to set a content
 *   description using [contentDescription].
 * @param style The style used for the inner content, specifying how the content should be aligned.
 *   It is recommended to use [EdgeButtonStyle.TOP_ALIGN] for long, wide content. If not set,
 *   defaults to [EdgeButtonStyle.DEFAULT] which center-aligns the content.
 * @param content The inner content to be put inside of this edge button.
 * @sample androidx.wear.protolayout.material3.samples.edgeButtonSampleIcon
 */
// TODO(b/346958146): link EdgeButton visuals in DAC
private fun MaterialScope.edgeButton(
    onClick: Clickable,
    colors: ButtonColors,
    modifier: LayoutModifier = LayoutModifier,
    style: EdgeButtonStyle = DEFAULT,
    content: MaterialScope.() -> LayoutElement
): LayoutElement {
    val containerWidth = deviceConfiguration.screenWidthDp.toDp()
    val horizontalMarginPercent: Float =
        if (deviceConfiguration.screenWidthDp.isBreakpoint()) HORIZONTAL_MARGIN_PERCENT_LARGE
        else HORIZONTAL_MARGIN_PERCENT_SMALL
    val edgeButtonWidth: Float =
        (100f - 2f * horizontalMarginPercent) * deviceConfiguration.screenWidthDp / 100f
    val bottomCornerRadiusX = edgeButtonWidth / 2f
    val bottomCornerRadiusY = EDGE_BUTTON_HEIGHT_DP - TOP_CORNER_RADIUS

    var mod =
        (LayoutModifier.semanticsRole(SEMANTICS_ROLE_BUTTON) then modifier)
            .clickable(onClick)
            .background(colors.container)
            .clip(TOP_CORNER_RADIUS)
            .clipBottomLeft(bottomCornerRadiusX, bottomCornerRadiusY)
            .clipBottomRight(bottomCornerRadiusX, bottomCornerRadiusY)

    style.padding?.let { mod = mod.padding(it) }

    val button = Box.Builder().setHeight(EDGE_BUTTON_HEIGHT_DP.toDp()).setWidth(dp(edgeButtonWidth))
    button
        .setVerticalAlignment(style.verticalAlignment)
        .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
        .addContent(content())

    return Box.Builder()
        .setHeight((EDGE_BUTTON_HEIGHT_DP + BOTTOM_MARGIN_DP).toDp())
        .setWidth(containerWidth)
        .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_TOP)
        .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
        .addContent(button.setModifiers(mod.toProtoLayoutModifiers()).build())
        .setModifiers(LayoutModifier.tag(METADATA_TAG).toProtoLayoutModifiers())
        .build()
}

/** Provides style values for edge button component. */
public class EdgeButtonStyle
private constructor(
    @VerticalAlignment internal val verticalAlignment: Int = VERTICAL_ALIGN_CENTER,
    internal val padding: Padding? = null
) {
    public companion object {
        /**
         * Style variation for having content of the edge button anchored to the top.
         *
         * This should be used for text-like content, or the content that is wide, to accommodate
         * for more space.
         */
        @JvmField
        public val TOP_ALIGN: EdgeButtonStyle =
            EdgeButtonStyle(
                verticalAlignment = LayoutElementBuilders.VERTICAL_ALIGN_TOP,
                padding =
                    padding(
                        start = TEXT_SIDE_PADDING_DP,
                        top = TEXT_TOP_PADDING_DP,
                        end = TEXT_SIDE_PADDING_DP
                    )
            )

        /**
         * Default style variation for having content of the edge button center aligned.
         *
         * This should be used for icon-like or small, round content that doesn't occupy a lot of
         * space.
         */
        @JvmField public val DEFAULT: EdgeButtonStyle = EdgeButtonStyle()
    }
}

internal object EdgeButtonDefaults {
    @Dimension(DP) internal const val TOP_CORNER_RADIUS: Float = 17f
    /** The horizontal margin used for width of the EdgeButton, below the 225dp breakpoint. */
    internal const val HORIZONTAL_MARGIN_PERCENT_SMALL: Float = 24f
    /** The horizontal margin used for width of the EdgeButton, above the 225dp breakpoint. */
    internal const val HORIZONTAL_MARGIN_PERCENT_LARGE: Float = 26f
    internal const val BOTTOM_MARGIN_DP: Int = 3
    internal const val EDGE_BUTTON_HEIGHT_DP: Int = 46
    internal const val METADATA_TAG: String = "EB"
    internal const val ICON_SIZE_DP = 24
    internal const val TEXT_TOP_PADDING_DP = 12f
    internal const val TEXT_SIDE_PADDING_DP = 8f
}

internal fun LayoutElement.isSlotEdgeButton(): Boolean =
    this is Box && METADATA_TAG == this.modifiers?.metadata?.toTagName()
