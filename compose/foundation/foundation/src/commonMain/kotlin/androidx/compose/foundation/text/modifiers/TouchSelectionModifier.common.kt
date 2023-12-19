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

package androidx.compose.foundation.text.modifiers

import androidx.compose.foundation.text.TextDragObserver
import androidx.compose.foundation.text.detectDragGesturesAfterLongPressWithObserver
import androidx.compose.foundation.text.selection.SelectionAdjustment
import androidx.compose.foundation.text.selection.SelectionRegistrar
import androidx.compose.foundation.text.selection.hasSelection
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.text.TextLayoutResult

internal expect fun SelectionRegistrar.touchSelectionModifier(
    layoutCoordinates: () -> LayoutCoordinates?,
    textLayoutResult: () -> TextLayoutResult?,
    selectableId: Long
): Modifier

internal fun SelectionRegistrar.defaultTouchSelectionModifier(
    layoutCoordinates: () -> LayoutCoordinates?,
    textLayoutResult: () -> TextLayoutResult?,
    selectableId: Long
): Modifier {
    val longPressDragObserver = object : TextDragObserver {
        /**
         * The beginning position of the drag gesture. Every time a new drag gesture starts, it wil be
         * recalculated.
         */
        var lastPosition = Offset.Zero

        /**
         * The total distance being dragged of the drag gesture. Every time a new drag gesture starts,
         * it will be zeroed out.
         */
        var dragTotalDistance = Offset.Zero

        override fun onDown(point: Offset) {
            // Not supported for long-press-drag.
        }

        override fun onUp() {
            // Nothing to do.
        }

        override fun onStart(startPoint: Offset) {
            layoutCoordinates()?.let {
                if (!it.isAttached) return

                if (textLayoutResult().outOfBoundary(startPoint, startPoint)) {
                    notifySelectionUpdateSelectAll(
                        selectableId = selectableId
                    )
                } else {
                    notifySelectionUpdateStart(
                        layoutCoordinates = it,
                        startPosition = startPoint,
                        adjustment = SelectionAdjustment.Word
                    )
                }

                lastPosition = startPoint
            }
            // selection never started
            if (!hasSelection(selectableId)) return
            // Zero out the total distance that being dragged.
            dragTotalDistance = Offset.Zero
        }

        override fun onDrag(delta: Offset) {
            layoutCoordinates()?.let {
                if (!it.isAttached) return
                // selection never started, did not consume any drag
                if (!hasSelection(selectableId)) return

                dragTotalDistance += delta
                val newPosition = lastPosition + dragTotalDistance

                if (!textLayoutResult().outOfBoundary(lastPosition, newPosition)) {
                    // Notice that only the end position needs to be updated here.
                    // Start position is left unchanged. This is typically important when
                    // long-press is using SelectionAdjustment.WORD or
                    // SelectionAdjustment.PARAGRAPH that updates the start handle position from
                    // the dragBeginPosition.
                    val consumed = notifySelectionUpdate(
                        layoutCoordinates = it,
                        previousPosition = lastPosition,
                        newPosition = newPosition,
                        isStartHandle = false,
                        adjustment = SelectionAdjustment.CharacterWithWordAccelerate
                    )
                    if (consumed) {
                        lastPosition = newPosition
                        dragTotalDistance = Offset.Zero
                    }
                }
            }
        }

        override fun onStop() {
            if (hasSelection(selectableId)) {
                notifySelectionUpdateEnd()
            }
        }

        override fun onCancel() {
            if (hasSelection(selectableId)) {
                notifySelectionUpdateEnd()
            }
        }
    }
    return Modifier.pointerInput(longPressDragObserver) {
        detectDragGesturesAfterLongPressWithObserver(
            longPressDragObserver
        )
    }
}

private fun TextLayoutResult?.outOfBoundary(start: Offset, end: Offset): Boolean {
    this ?: return false

    val lastOffset = layoutInput.text.text.length
    val rawStartOffset = getOffsetForPosition(start)
    val rawEndOffset = getOffsetForPosition(end)

    return rawStartOffset >= lastOffset - 1 && rawEndOffset >= lastOffset - 1 ||
        rawStartOffset < 0 && rawEndOffset < 0
}
