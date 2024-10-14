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

package androidx.wear.compose.foundation.lazy

import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScrollProgress.Companion.bottomItemScrollProgress

/** Represents a placeable item in the [TransformingLazyColumn] layout. */
internal data class TransformingLazyColumnMeasuredItem(
    /** The index of the item in the list. */
    override val index: Int,
    /** The [Placeable] representing the content of the item. */
    val placeable: Placeable,
    /** The constraints of the container holding the item. */
    val containerConstraints: Constraints,
    /** The vertical offset of the item from the top of the list after transformations applied. */
    override var offset: Int,
    /**
     * The horizontal padding before the item. This doesn't affect vertical calculations, but needs
     * to be added to during final placement.
     */
    val leftPadding: Int,
    /**
     * The horizontal padding after the item. This doesn't affect vertical calculations, but needs
     * to be added to during final placement.
     */
    val rightPadding: Int,
    /** Scroll progress of the item used to calculate transformations applied. */
    override var scrollProgress: TransformingLazyColumnItemScrollProgress,
    /** The horizontal alignment to apply during placement. */
    val horizontalAlignment: Alignment.Horizontal,
    /** The [LayoutDirection] of the `Layout`. */
    private val layoutDirection: LayoutDirection,
    override val key: Any,
    override val contentType: Any?,
) : TransformingLazyColumnVisibleItemInfo {

    /** The height of the item after transformations applied. */
    override val transformedHeight: Int
        get() =
            (placeable.parentData as? HeightProviderParentData)?.let {
                it.heightProvider(placeable.height, scrollProgress)
            } ?: placeable.height

    override val measuredHeight = placeable.height

    fun place(scope: Placeable.PlacementScope) =
        with(scope) {
            placeable.placeWithLayer(
                x =
                    leftPadding +
                        horizontalAlignment.align(
                            space = containerConstraints.maxWidth - rightPadding - leftPadding,
                            size = placeable.width,
                            layoutDirection = layoutDirection
                        ),
                y = offset,
            )
        }

    fun pinToCenter() {
        scrollProgress =
            bottomItemScrollProgress(
                containerConstraints.maxHeight / 2 - placeable.height / 2,
                placeable.height,
                containerConstraints.maxHeight
            )
        offset = containerConstraints.maxHeight / 2 - transformedHeight / 2
    }
}
