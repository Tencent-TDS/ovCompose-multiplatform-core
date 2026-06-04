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
import androidx.compose.foundation.lazy.layout.LazyLayoutMeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.ShortRect

/**
 * Define a measure policy to implement a interceptor of [LazyStaggeredGridMeasureProvider.getAndMeasure][LazyStaggeredGridMeasureProvider.getAndMeasure]
 * which gets and measures item by index in LazyStaggeredGrid.
 */
@ExperimentalFoundationApi
interface LazyLayoutMeasurePolicy {

    /**
     * @param index The index of the placeholder in LazyStaggeredGrid.
     * @param constraints Constraints for placeholder from LazyStaggeredGrid measure.
     * @param visible Compute the lazy loading area rect with local area rect in LazyStaggeredGrid.
     * @return The placeholder and its children to placed.
     */
    fun LazyLayoutMeasureScope.measure(
        index: Int,
        isVertical: Boolean,
        constraints: Constraints,
        visible: (size: ShortRect) -> ShortRect
    ): List<Placeable>

    /**
     * A empty placeable item implementation which is practically invisible in LazyStaggeredGrid.
     */
    companion object Empty : LazyLayoutMeasurePolicy {

        /**
         * Returns a placeable with zero size instead of emptyList,
         * otherwise the item will be not added to the LazyStaggeredGrid measure result.
         */
        override fun LazyLayoutMeasureScope.measure(
            index: Int,
            isVertical: Boolean,
            constraints: Constraints,
            visible: (size: ShortRect) -> ShortRect
        ): List<Placeable> = listOf(Placeholder(IntSize.Zero))
    }
}