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
import androidx.annotation.FloatRange
import androidx.wear.protolayout.DimensionBuilders.ContainerDimension
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.weight
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.HORIZONTAL_ALIGN_END
import androidx.wear.protolayout.LayoutElementBuilders.HORIZONTAL_ALIGN_START
import androidx.wear.protolayout.LayoutElementBuilders.HorizontalAlignment
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.LayoutElementBuilders.Row
import androidx.wear.protolayout.ModifiersBuilders.Padding
import androidx.wear.protolayout.material3.Typography.TypographyToken
import androidx.wear.protolayout.modifiers.padding

internal object GraphicDataCardDefaults {
    @FloatRange(from = 0.0, to = 100.0) private const val GRAPHIC_SPACE_PERCENTAGE: Float = 40f
    private const val GRAPH_SIDE_PADDING_WEIGHT_OFFSET = 2f
    private const val DATA_GRAPH_CARD_START_ALIGN_EXTRA_SPACE_DP = 2

    /**
     * Returns [LayoutElement] describing the inner content for the graph data card.
     *
     * This is a Row containing the following:
     * * graph content, 40% of the horizontal space
     * * remaining is a Column with title and content
     */
    internal fun buildContentForGraphicDataCard(
        title: LayoutElement,
        content: LayoutElement?,
        graphic: LayoutElement,
        style: GraphicDataCardStyle,
        height: ContainerDimension,
        @HorizontalAlignment horizontalAlignment: Int
    ): LayoutElement {
        val verticalElementBuilder: Column.Builder =
            Column.Builder().setWidth(expand()).setHorizontalAlignment(HORIZONTAL_ALIGN_START)
        val horizontalElementBuilder: Row.Builder =
            Row.Builder().setWidth(expand()).setHeight(height)

        ContainerWithSpacersBuilder<LayoutElement>(
                { it: LayoutElement? -> verticalElementBuilder.addContent(it!!) },
                title
            )
            .addElement(content, horizontalSpacer(style.titleToContentSpaceDp))

        // Side padding - start
        // Smaller padding should be applied to the graph side, and larger to the labels side.
        horizontalElementBuilder.addContent(
            verticalSpacer(
                weight(
                    style.sidePaddingWeight +
                        GRAPH_SIDE_PADDING_WEIGHT_OFFSET *
                            (if (horizontalAlignment == HORIZONTAL_ALIGN_START) -1 else 1)
                )
            )
        )

        // Wrap graphic in expandable box with weights
        val wrapGraphic =
            Box.Builder()
                .setWidth(weight(GRAPHIC_SPACE_PERCENTAGE))
                .setHeight(height)
                .addContent(graphic)
                .build()

        if (horizontalAlignment == HORIZONTAL_ALIGN_START) {
            horizontalElementBuilder.addContent(wrapGraphic)
            horizontalElementBuilder.addContent(
                verticalSpacer(
                    style.graphToTitleSpaceDp + DATA_GRAPH_CARD_START_ALIGN_EXTRA_SPACE_DP
                )
            )
        }

        horizontalElementBuilder.addContent(
            Box.Builder()
                .setHorizontalAlignment(HORIZONTAL_ALIGN_START)
                .setWidth(weight(100 - style.sidePaddingWeight * 2 - GRAPHIC_SPACE_PERCENTAGE))
                .addContent(verticalElementBuilder.build())
                .build()
        )

        if (horizontalAlignment == HORIZONTAL_ALIGN_END) {
            horizontalElementBuilder.addContent(verticalSpacer(style.graphToTitleSpaceDp))
            horizontalElementBuilder.addContent(wrapGraphic)
        }

        // Side padding - end
        // Smaller padding should be applied to the graph side, and larger to the labels side.
        horizontalElementBuilder.addContent(
            verticalSpacer(
                weight(
                    style.sidePaddingWeight +
                        GRAPH_SIDE_PADDING_WEIGHT_OFFSET *
                            (if (horizontalAlignment == HORIZONTAL_ALIGN_START) 1 else -1)
                )
            )
        )

        return horizontalElementBuilder.build()
    }
}

/** Provides style values for the graphic data card component. */
public class GraphicDataCardStyle
internal constructor(
    internal val innerPadding: Padding,
    @Dimension(unit = DP) internal val titleToContentSpaceDp: Int,
    @TypographyToken internal val titleTypography: Int,
    @TypographyToken internal val contentTypography: Int,
    @Dimension(unit = DP) internal val graphToTitleSpaceDp: Int,
    internal val sidePaddingWeight: Float,
) {
    public companion object {
        /** The default smaller spacer width or height that should be between different elements. */
        @Dimension(unit = DP) private const val SMALL_SPACE_DP: Int = 2

        private const val DEFAULT_VERTICAL_PADDING_DP = 8f

        /**
         * Default style variation for the [graphicDataCard] where all opinionated inner content is
         * displayed in a medium size.
         */
        public fun defaultGraphicDataCardStyle(): GraphicDataCardStyle =
            GraphicDataCardStyle(
                innerPadding =
                    padding(
                        top = DEFAULT_VERTICAL_PADDING_DP,
                        bottom = DEFAULT_VERTICAL_PADDING_DP
                    ),
                titleToContentSpaceDp = SMALL_SPACE_DP,
                titleTypography = Typography.DISPLAY_SMALL,
                contentTypography = Typography.LABEL_SMALL,
                graphToTitleSpaceDp = 6,
                sidePaddingWeight = 8f
            )

        /**
         * Default style variation for the [graphicDataCard] where all opinionated inner content is
         * displayed in a large size.
         */
        public fun largeGraphicDataCardStyle(): GraphicDataCardStyle =
            GraphicDataCardStyle(
                innerPadding =
                    padding(
                        top = DEFAULT_VERTICAL_PADDING_DP,
                        bottom = DEFAULT_VERTICAL_PADDING_DP
                    ),
                titleToContentSpaceDp = 0,
                titleTypography = Typography.DISPLAY_MEDIUM,
                contentTypography = Typography.LABEL_MEDIUM,
                graphToTitleSpaceDp = 8,
                sidePaddingWeight = 8.3f
            )
    }
}
