/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.foundation.text.input.internal.selection

import androidx.compose.foundation.gestures.detectTapAndPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.text.determineCursorDesiredOffset
import androidx.compose.foundation.text.input.internal.IndexTransformationType.Deletion
import androidx.compose.foundation.text.input.internal.IndexTransformationType.Insertion
import androidx.compose.foundation.text.input.internal.IndexTransformationType.Replacement
import androidx.compose.foundation.text.input.internal.IndexTransformationType.Untransformed
import androidx.compose.foundation.text.input.internal.SelectionWedgeAffinity
import androidx.compose.foundation.text.input.internal.WedgeAffinity
import androidx.compose.foundation.text.input.internal.findClosestRect
import androidx.compose.foundation.text.input.internal.fromDecorationToTextLayout
import androidx.compose.foundation.text.input.internal.getIndexTransformationType
import androidx.compose.foundation.text.input.internal.selection.TextToolbarState.None
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.text.TextRange
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

internal actual suspend fun PointerInputScope.detectTextFieldTapGestures(
    selectionState: TextFieldSelectionState,
    interactionSource: MutableInteractionSource?,
    requestFocus: () -> Unit,
    showKeyboard: () -> Unit
) {
    detectTapAndPress(
        onTap = { offset ->
            requestFocus()

            if (selectionState.enabled && selectionState.isFocused) {
                if (!selectionState.readOnly) {
                    showKeyboard()
                    // TODO: looks unnecessary
//                    if (selectionState.textFieldState.visualText.isNotEmpty()) {
//                        selectionState.showCursorHandle = true
//                    }
                }

                // do not show any TextToolbar.
                selectionState.updateTextToolbarState(None)

                val coercedOffset = selectionState.textLayoutState.coercedInVisibleBoundsOfInputText(offset)
                selectionState.placeCursorAtDesiredOffset(
                    selectionState.textLayoutState.fromDecorationToTextLayout(coercedOffset)
                )
            }
        },
        // Should remain untouched
        onPress = { offset ->
            interactionSource?.let { interactionSource ->
                coroutineScope {
                    launch {
                        // Remove any old interactions if we didn't fire stop / cancel properly
                        selectionState.pressInteraction?.let { oldValue ->
                            val interaction = PressInteraction.Cancel(oldValue)
                            interactionSource.emit(interaction)
                            selectionState.pressInteraction = null
                        }

                        val press = PressInteraction.Press(offset)
                        interactionSource.emit(press)
                        selectionState.pressInteraction = press
                    }
                    val success = tryAwaitRelease()
                    selectionState.pressInteraction?.let { pressInteraction ->
                        val endInteraction =
                            if (success) {
                                PressInteraction.Release(pressInteraction)
                            } else {
                                PressInteraction.Cancel(pressInteraction)
                            }
                        interactionSource.emit(endInteraction)
                    }
                    selectionState.pressInteraction = null
                }
            }
        }
    )
}

internal fun TextFieldSelectionState.placeCursorAtDesiredOffset(offset: Offset): Boolean {
    val layoutResult = textLayoutState.layoutResult ?: return false

    // First step: calculate the proposed cursor index.
    val proposedIndex = layoutResult.getOffsetForPosition(offset)
    if (proposedIndex == -1) return false

    // Second step: adjust proposed cursor position as iOS does
    val previousIndex = layoutResult.getOffsetForPosition(handleDragPosition)
    // TODO: Check which type of text is required here
    val currentText = textFieldState.untransformedText.text as String
    val index = determineCursorDesiredOffset(proposedIndex, previousIndex, layoutResult, currentText)

    // Third step: if a transformation is applied, determine if the proposed cursor position
    // would be in a range where the cursor is not allowed to be. If so, push it to the
    // appropriate edge of that range.
    var newAffinity: SelectionWedgeAffinity? = null
    val untransformedCursor =
        textFieldState.getIndexTransformationType(index) { type, untransformed, retransformed ->
            when (type) {
                Untransformed -> untransformed.start

                // Deletion. Doesn't matter which end of the deleted range we put the cursor,
                // they'll both map to the same transformed offset.
                Deletion -> untransformed.start

                // The untransformed offset will be the same no matter which side we put the
                // cursor on, so we need to set the affinity to the closer edge.
                Insertion -> {
                    val wedgeStartCursorRect = layoutResult.getCursorRect(retransformed.start)
                    val wedgeEndCursorRect = layoutResult.getCursorRect(retransformed.end)
                    newAffinity =
                        if (
                            offset.findClosestRect(wedgeStartCursorRect, wedgeEndCursorRect) < 0
                        ) {
                            SelectionWedgeAffinity(WedgeAffinity.Start)
                        } else {
                            SelectionWedgeAffinity(WedgeAffinity.End)
                        }
                    untransformed.start
                }

                // Set the untransformed cursor to the edge that corresponds to the closer edge
                // in the transformed text.
                Replacement -> {
                    val wedgeStartCursorRect = layoutResult.getCursorRect(retransformed.start)
                    val wedgeEndCursorRect = layoutResult.getCursorRect(retransformed.end)
                    if (offset.findClosestRect(wedgeStartCursorRect, wedgeEndCursorRect) < 0) {
                        untransformed.start
                    } else {
                        untransformed.end
                    }
                }
            }
        }
    val untransformedCursorRange = TextRange(untransformedCursor)

    // Nothing changed, skip onValueChange and hapticFeedback.
    if (
        untransformedCursorRange == textFieldState.untransformedText.selection &&
        (newAffinity == null || newAffinity == textFieldState.selectionWedgeAffinity)
    ) {
        return false
    }

    textFieldState.selectUntransformedCharsIn(untransformedCursorRange)
    newAffinity?.let { textFieldState.selectionWedgeAffinity = it }
    return true
}