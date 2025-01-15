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

import android.graphics.Color
import androidx.annotation.Dimension
import androidx.annotation.Dimension.Companion.DP
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.HorizontalAlignment
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.LayoutElementBuilders.Row
import androidx.wear.protolayout.ModifiersBuilders.Padding
import androidx.wear.protolayout.material3.ButtonDefaults.DEFAULT_CONTENT_PADDING
import androidx.wear.protolayout.material3.Typography.TypographyToken
import androidx.wear.protolayout.modifiers.padding
import androidx.wear.protolayout.types.LayoutColor
import androidx.wear.protolayout.types.argb

/**
 * Represents the container and content colors used in buttons, such as [textEdgeButton] or
 * [iconEdgeButton].
 */
public class ButtonColors(
    /** The container color to be used for a button. */
    public val container: LayoutColor = Color.BLACK.argb,
    /** The icon tint color to be used for a button. */
    public val icon: LayoutColor = Color.BLACK.argb,
    /** The label color to be used for a button. */
    public val label: LayoutColor = Color.BLACK.argb,
    /** The secondary label color to be used for a button. */
    public val secondaryLabel: LayoutColor = Color.BLACK.argb,
)

public object ButtonDefaults {
    /**
     * Returns [LayoutElement] describing the inner content for the pill shape button.
     *
     * This is a [Row] containing the following:
     * * icon
     * * spacing if icon is present
     * * labels that are in [Column]
     */
    internal fun buildContentForPillShapeButton(
        label: LayoutElement,
        secondaryLabel: LayoutElement?,
        icon: LayoutElement?,
        @HorizontalAlignment horizontalAlignment: Int,
        style: ButtonStyle
    ): LayoutElement {
        val labels: Column.Builder =
            Column.Builder().setWidth(expand()).setHorizontalAlignment(horizontalAlignment)

        val row: Row.Builder = Row.Builder()

        ContainerWithSpacersBuilder<LayoutElement>(labels::addContent, label)
            .addElement(secondaryLabel, horizontalSpacer(style.labelsSpaceDp))

        ContainerWithSpacersBuilder<LayoutElement>(row::addContent, icon)
            .addElement(labels.build(), verticalSpacer(style.iconToLabelsSpaceDp))

        return row.build()
    }

    /**
     * [ButtonColors] for the high-emphasis button representing the primary, most important or most
     * common action on a screen.
     *
     * These colors are using [ColorScheme.primary] for background color and [ColorScheme.onPrimary]
     * for content color.
     */
    public fun MaterialScope.filledButtonColors(): ButtonColors =
        ButtonColors(
            container = theme.colorScheme.primary,
            icon = theme.colorScheme.onPrimary,
            label = theme.colorScheme.onPrimary,
            secondaryLabel = theme.colorScheme.onPrimary.withOpacity(0.8f)
        )

    /**
     * [ButtonColors] for the medium-emphasis button.
     *
     * These colors are using [ColorScheme.surfaceContainer] for background color,
     * [ColorScheme.onSurface] for content color and [ColorScheme.primary] for icon.
     */
    public fun MaterialScope.filledTonalButtonColors(): ButtonColors =
        ButtonColors(
            container = theme.colorScheme.surfaceContainer,
            icon = theme.colorScheme.primary,
            label = theme.colorScheme.onSurface,
            secondaryLabel = theme.colorScheme.onSurfaceVariant
        )

    /**
     * Alternative [ButtonColors] for the high-emphasis button.
     *
     * These colors are using [ColorScheme.primaryContainer] for background color and
     * [ColorScheme.onPrimaryContainer] for content color.
     */
    public fun MaterialScope.filledVariantButtonColors(): ButtonColors =
        ButtonColors(
            container = theme.colorScheme.primaryContainer,
            icon = theme.colorScheme.onPrimaryContainer,
            label = theme.colorScheme.onPrimaryContainer,
            secondaryLabel = theme.colorScheme.onPrimaryContainer.withOpacity(0.9f)
        )

    internal const val METADATA_TAG_BUTTON: String = "BTN"
    internal val DEFAULT_CONTENT_PADDING = padding(8f)
    @Dimension(DP) internal const val IMAGE_BUTTON_DEFAULT_SIZE_DP = 52
}

/** Provides style values for the icon button component. */
public class IconButtonStyle
internal constructor(
    @Dimension(unit = DP) internal val iconSize: Float,
    internal val innerPadding: Padding = DEFAULT_CONTENT_PADDING
) {
    public companion object {
        /**
         * Default style variation for the [iconButton] where all opinionated inner content is
         * displayed in a medium size.
         */
        public fun defaultIconButtonStyle(): IconButtonStyle = IconButtonStyle(26f)

        /**
         * Default style variation for the [iconButton] where all opinionated inner content is
         * displayed in a large size.
         */
        public fun largeIconButtonStyle(): IconButtonStyle = IconButtonStyle(32f)
    }
}

/** Provides style values for the text button component. */
public class TextButtonStyle
internal constructor(
    @TypographyToken internal val labelTypography: Int,
    internal val innerPadding: Padding = DEFAULT_CONTENT_PADDING
) {
    public companion object {
        /**
         * Default style variation for the [textButton] where all opinionated inner content is
         * displayed in a small size.
         */
        public fun smallTextButtonStyle(): TextButtonStyle =
            TextButtonStyle(Typography.LABEL_MEDIUM)

        /**
         * Default style variation for the [textButton] where all opinionated inner content is
         * displayed in a medium size.
         */
        public fun defaultTextButtonStyle(): TextButtonStyle =
            TextButtonStyle(Typography.LABEL_LARGE)

        /**
         * Default style variation for the [textButton] where all opinionated inner content is
         * displayed in a large size.
         */
        public fun largeTextButtonStyle(): TextButtonStyle =
            TextButtonStyle(Typography.DISPLAY_SMALL)

        /**
         * Default style variation for the [textButton] where all opinionated inner content is
         * displayed in an extra large size.
         */
        public fun extraLargeTextButtonStyle(): TextButtonStyle =
            TextButtonStyle(Typography.DISPLAY_MEDIUM)
    }
}

/** Provides style values for the pill shape button component. */
public class ButtonStyle
internal constructor(
    @TypographyToken internal val labelTypography: Int,
    @TypographyToken internal val secondaryLabelTypography: Int,
    @Dimension(DP) internal val iconSize: Float,
    internal val innerPadding: Padding,
    @Dimension(DP) internal val labelsSpaceDp: Int,
    @Dimension(DP) internal val iconToLabelsSpaceDp: Int,
) {
    public companion object {
        /**
         * Default style variation for the [button] where all opinionated inner content is displayed
         * in a small size.
         */
        public fun smallButtonStyle(): ButtonStyle =
            ButtonStyle(
                labelTypography = Typography.LABEL_MEDIUM,
                secondaryLabelTypography = Typography.BODY_SMALL,
                iconSize = 24f,
                innerPadding = padding(horizontal = 14f, vertical = 10f),
                labelsSpaceDp = 2,
                iconToLabelsSpaceDp = 6
            )

        /**
         * Default style variation for the [button] where all opinionated inner content is displayed
         * in a medium size.
         */
        public fun defaultButtonStyle(): ButtonStyle =
            ButtonStyle(
                labelTypography = Typography.TITLE_MEDIUM,
                secondaryLabelTypography = Typography.LABEL_SMALL,
                iconSize = 26f,
                innerPadding = padding(horizontal = 14f, vertical = 6f),
                labelsSpaceDp = 0,
                iconToLabelsSpaceDp = 8
            )

        /**
         * Default style variation for the [button] where all opinionated inner content is displayed
         * in a large size.
         */
        public fun largeButtonStyle(): ButtonStyle =
            ButtonStyle(
                labelTypography = Typography.LABEL_LARGE,
                secondaryLabelTypography = Typography.LABEL_SMALL,
                iconSize = 32f,
                innerPadding = padding(horizontal = 14f, vertical = 8f),
                labelsSpaceDp = 0,
                iconToLabelsSpaceDp = 10
            )
    }
}
