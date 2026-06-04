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
import androidx.compose.ui.unit.ShortRect

/**
 * Defines the measure and lazy layout behavior of a `Placeholder` in in [LazyStaggeredGrid][LazyStaggeredGrid],
 * similar to [MeasurePolicy][androidx.compose.ui.layout.MeasurePolicy].
 * [LazyLayoutPlaceholderMeasurePolicy] and [LazyPlaceholderMeasurePolicy] are the way LazyStaggeredGrid placeholder (such as [LatticePlaceholder]) are built,
 * and they can also be used to achieve custom lazy placeholder.
 */
@ExperimentalFoundationApi
interface LazyPlaceholderMeasurePolicy {
    /**
     * @param placeholder Measure the placeholder item in LazyStaggeredGrid with constraints.
     * @param children Measure the child items in LazyStaggeredGrid of placeholder with constraints.
     * @param constraints Constraints for placeholder from LazyStaggeredGrid measure.
     * @param visible Compute the lazy loading area rect with local area rect in LazyStaggeredGrid.
     * @return The placeholder and its children to placed.
     */
    fun LazyLayoutMeasureScope.measure(
        isVertical: Boolean,
        placeholder: (constraints: Constraints) -> List<Placeable>,
        children: List<(constraints: Constraints) -> List<Placeable>>,
        constraints: Constraints,
        visible: (size: ShortRect) -> ShortRect
    ): List<Placeable>
}

/**
 * Define a placeholder with [serval][count] children measure and place [behavior][measurePolicy].
 * In LazyStaggeredGrid, the index of children follow placeholder.
 */
@ExperimentalFoundationApi
class LazyLayoutPlaceholderMeasurePolicy(
    val count: Int,
    val measurePolicy: LazyPlaceholderMeasurePolicy
) : LazyLayoutMeasurePolicy {

    /**
     * Transform [LazyLayoutMeasurePolicy] to implement [LazyLayoutPlaceholderMeasurePolicy]
     * which similar to [MeasurePolicy][androidx.compose.ui.layout.MeasurePolicy]
     */
    @OptIn(ExperimentalFoundationApi::class)
    override fun LazyLayoutMeasureScope.measure(
        index: Int,
        isVertical: Boolean,
        constraints: Constraints,
        visible: (size: ShortRect) -> ShortRect,
    ): List<Placeable> = measurePolicy.run {
        measure(
            isVertical = isVertical,
            placeholder = { measure(index, it) },
            children = List(count) { localIndex ->
                {
                    // measure items from index to index+localIndex
                    measure(index + localIndex, it)
                }
            },
            constraints = constraints,
            visible = visible
        )
    }
}