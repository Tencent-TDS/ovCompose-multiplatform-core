/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation

import androidx.compose.animation.RubberBand
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.unit.*

@OptIn(ExperimentalFoundationApi::class)
class IOSBasedOverscrollEffect : OverscrollEffect {
    companion object {
        val IOS_COEFFICIENT = 0.55f
        fun rubberBandedValue(value: Float, dimension: Float, coefficient: Float = IOS_COEFFICIENT) =
            (1f - (1f / (value * coefficient / dimension + 1f))) * dimension

        fun rubberBandedOffset(offset: Offset, size: Size, coefficient: Float = IOS_COEFFICIENT) =
            Offset(
                rubberBandedValue(offset.x, size.width, coefficient),
                rubberBandedValue(offset.y, size.height, coefficient)
            )
    }
    /*
     * Current absolute offset in Overscroll area
     * Negative for top-left
     * Positive for bottom-right
     * Zero if Offset is within the scrollable bounds
     */
    var scrollSize: Size? = null
    var offset: Offset by mutableStateOf(Offset.Zero)
    override fun applyToScroll(
        delta: Offset,
        source: NestedScrollSource,
        performScroll: (Offset) -> Offset
    ): Offset {
        val consumedDelta = performScroll(delta)

        val leftDelta = delta - consumedDelta

        offset += leftDelta

        return offset
    }

    override suspend fun applyToFling(
        velocity: Velocity,
        performFling: suspend (Velocity) -> Velocity
    ) {
        // TODO: implement similar to Android
        print(velocity)
        performFling(velocity)
    }

    override val isInProgress =
        offset.getDistanceSquared() > 0.5f

    override val effectModifier = Modifier
        .onPlaced {
            this.scrollSize = it.size.toSize()
        }
        .offset {
            scrollSize?.let {
                rubberBandedOffset(offset, it).round()
            } ?: IntOffset.Zero
        }
}