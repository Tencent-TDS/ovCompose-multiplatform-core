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

import androidx.wear.protolayout.types.LayoutColor

/**
 * Represents colors used in card components, such as [titleCard] or [appCard].
 *
 * @param background [LayoutColor] which is used to as the background color for the card.
 * @param title the color used for title for the card.
 * @param content the content color for the card.
 * @param time the color used for time for the card.
 * @param label the color used for label for the card.
 * @param secondaryIcon the color used for icon in the data card type.
 * @param secondaryText the color used for secondary label for the data card type.
 */
public class CardColors(
    public val background: LayoutColor,
    public val title: LayoutColor,
    public val content: LayoutColor,
    public val time: LayoutColor = content,
    public val label: LayoutColor = title,
    public val secondaryIcon: LayoutColor = title,
    public val secondaryText: LayoutColor = time,
)

public object CardDefaults {
    /**
     * [CardColors] for the high-emphasis card representing the primary, most important or most
     * common action on a screen.
     *
     * These colors are using [ColorScheme.primary] for background color and [ColorScheme.onPrimary]
     * for content colors.
     */
    public fun MaterialScope.filledCardColors(): CardColors =
        CardColors(
            background = theme.colorScheme.primary,
            title = theme.colorScheme.onPrimary,
            content = theme.colorScheme.onPrimary.withOpacity(0.8f)
        )

    /**
     * [CardColors] for the medium-emphasis card.
     *
     * These colors are using [ColorScheme.surfaceContainer] for background color and
     * [ColorScheme.onSurface] and [ColorScheme.onSurfaceVariant] for content colors.
     */
    public fun MaterialScope.filledTonalCardColors(): CardColors =
        CardColors(
            background = theme.colorScheme.surfaceContainer,
            title = theme.colorScheme.onSurface,
            content = theme.colorScheme.onSurfaceVariant,
            secondaryIcon = theme.colorScheme.primary
        )

    /**
     * Alternative [CardColors] for the high-emphasis card.
     *
     * These colors are using [ColorScheme.primaryContainer] for background color and
     * [ColorScheme.primaryContainer] for content colors.
     */
    public fun MaterialScope.filledVariantCardColors(): CardColors =
        CardColors(
            background = theme.colorScheme.primaryContainer,
            title = theme.colorScheme.onPrimaryContainer,
            content = theme.colorScheme.onPrimaryContainer.withOpacity(0.9f)
        )

    /**
     * Alternative [CardColors] for the card with [backgroundImage] as a background.
     *
     * These colors are using [ColorScheme.onBackground] for content colors.
     */
    public fun MaterialScope.imageBackgroundCardColors(): CardColors =
        CardColors(
            background = theme.colorScheme.background,
            title = theme.colorScheme.onBackground,
            content = theme.colorScheme.onBackground
        )

    internal const val METADATA_TAG: String = "CR"
    internal const val DEFAULT_CONTENT_PADDING = 4f
}
