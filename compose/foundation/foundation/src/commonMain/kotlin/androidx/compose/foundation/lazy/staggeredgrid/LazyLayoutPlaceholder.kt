/*
 * Tencent is pleased to support the open source community by making ovCompose available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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

package androidx.compose.foundation.lazy.staggeredgrid

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection

/**
 * A placeholder sized [size] that cannot be placed.
 */
@ExperimentalFoundationApi
open class Placeholder(size: IntSize) : Placeable() {
    init {
        measuredSize = size
    }

    override fun placeAt(
        position: IntOffset,
        zIndex: Float, layerBlock:
        (GraphicsLayerScope.() -> Unit)?
    ) = Unit

    override fun get(alignmentLine: AlignmentLine): Int = AlignmentLine.Unspecified
}

@OptIn(ExperimentalFoundationApi::class)
private object Nothing : Placeholder(IntSize.Zero)

/**
 * A util placeable which [offset] the [placeable] with [index].
 */
@ExperimentalFoundationApi
class OffsetPlaceable(
    internal val index: Int,
    internal val offset: IntOffset,
    private val placeable: Placeable = Nothing,
) : Placeable() {

    init {
        measuredSize = IntSize(placeable.measuredWidth, placeable.measuredHeight)
    }

    override fun placeAt(
        position: IntOffset,
        zIndex: Float,
        layerBlock: (GraphicsLayerScope.() -> Unit)?
    ) = placeable.place(position + offset, zIndex)

    override fun get(alignmentLine: AlignmentLine): Int = placeable[alignmentLine]

    companion object PlacementScope : Placeable.PlacementScope() {
        override val parentWidth: Int = 0
        override val parentLayoutDirection: LayoutDirection = LayoutDirection.Ltr
    }
}