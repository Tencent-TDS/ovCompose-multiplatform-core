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

import androidx.compose.foundation.text.CupertinoTouchSelectionObserver
import androidx.compose.foundation.text.cupertinoTouchSelectionDetector
import androidx.compose.foundation.text.selection.SelectionAdjustment
import androidx.compose.foundation.text.selection.SelectionRegistrar
import androidx.compose.foundation.text.selection.hasSelection
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.text.TextLayoutResult

internal actual fun SelectionRegistrar.touchSelectionModifier(
    layoutCoordinates: () -> LayoutCoordinates?,
    textLayoutResult: () -> TextLayoutResult?,
    selectableId: Long
): Modifier {
    val mouseSelectionObserver = object : CupertinoTouchSelectionObserver {
        var lastPosition = Offset.Zero

        override fun onStart(
            downPosition: Offset,
            adjustment: SelectionAdjustment
        ): Boolean {
            layoutCoordinates()?.let {
                if (!it.isAttached) return false

                notifySelectionUpdateStart(
                    layoutCoordinates = it,
                    startPosition = downPosition,
                    adjustment = adjustment
                )

                lastPosition = downPosition
                return hasSelection(selectableId)
            }

            return false
        }

        override fun onDrag(
            dragPosition: Offset,
            adjustment: SelectionAdjustment
        ): Boolean {
            layoutCoordinates()?.let {
                if (!it.isAttached) return false
                if (!hasSelection(selectableId)) return false

                val consumed = notifySelectionUpdate(
                    layoutCoordinates = it,
                    previousPosition = lastPosition,
                    newPosition = dragPosition,
                    isStartHandle = false,
                    adjustment = adjustment
                )
                if (consumed) {
                    lastPosition = dragPosition
                }
            }
            return true
        }
    }
    return Modifier.composed {
        // TODO(https://youtrack.jetbrains.com/issue/COMPOSE-79) how we can rewrite this without `composed`?
        val currentIOSTouchSelectionObserver by rememberUpdatedState(mouseSelectionObserver)
        pointerInput(Unit) {
            cupertinoTouchSelectionDetector(currentIOSTouchSelectionObserver)
        }
    }
}
