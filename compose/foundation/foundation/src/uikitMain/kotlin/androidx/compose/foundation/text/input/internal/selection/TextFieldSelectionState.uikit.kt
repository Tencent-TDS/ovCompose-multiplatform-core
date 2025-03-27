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

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectTapAndPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.text.TextDragObserver
import androidx.compose.foundation.text.determineCursorDesiredOffset
import androidx.compose.foundation.text.input.TextFieldCharSequence
import androidx.compose.foundation.text.input.internal.IndexTransformationType.Deletion
import androidx.compose.foundation.text.input.internal.IndexTransformationType.Insertion
import androidx.compose.foundation.text.input.internal.IndexTransformationType.Replacement
import androidx.compose.foundation.text.input.internal.IndexTransformationType.Untransformed
import androidx.compose.foundation.text.input.internal.SelectionWedgeAffinity
import androidx.compose.foundation.text.input.internal.WedgeAffinity
import androidx.compose.foundation.text.input.internal.findClosestRect
import androidx.compose.foundation.text.input.internal.fromDecorationToTextLayout
import androidx.compose.foundation.text.input.internal.getIndexTransformationType
import androidx.compose.foundation.text.input.internal.selection.TextFieldSelectionState.InputType
import androidx.compose.foundation.text.input.internal.selection.TextToolbarState.Cursor
import androidx.compose.foundation.text.input.internal.selection.TextToolbarState.None
import androidx.compose.foundation.text.input.internal.selection.TextToolbarState.Selection
import androidx.compose.foundation.text.selection.ClicksCounter
import androidx.compose.foundation.text.selection.MouseSelectionObserver
import androidx.compose.foundation.text.selection.SelectionAdjustment
import androidx.compose.foundation.text.selection.isPrecisePointer
import androidx.compose.foundation.text.selection.mouseSelectionBtf2
import androidx.compose.foundation.text.selection.touchSelectionFirstPress
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.util.fastAll
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
                    if (selectionState.textFieldState.visualText.isNotEmpty()) {
                        selectionState.showCursorHandle = true
                    }
                }

                selectionState.updateTextToolbarState(None)

                val coercedOffset =
                    selectionState.textLayoutState.coercedInVisibleBoundsOfInputText(offset)
                val cursorMoved = selectionState.placeCursorAtDesiredOffset(
                    selectionState.textLayoutState.fromDecorationToTextLayout(coercedOffset)
                )

                // TODO: It should be toggleable
                if (!cursorMoved) {
                    selectionState.updateTextToolbarState(Cursor)
                }
            }
        },
        // Should remain as is
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

private fun TextFieldSelectionState.placeCursorAtDesiredOffset(offset: Offset): Boolean {
    val layoutResult = textLayoutState.layoutResult ?: return false

    // First step: calculate the proposed cursor index.
    val proposedIndex = layoutResult.getOffsetForPosition(offset)
    if (proposedIndex == -1) return false

    // Second step: adjust proposed cursor position as iOS does
    val previousIndex = layoutResult.getOffsetForPosition(handleDragPosition)
    // TODO: Check which type of text is required here
    val currentText = textFieldState.untransformedText.text as String
    val index =
        determineCursorDesiredOffset(proposedIndex, previousIndex, layoutResult, currentText)

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

/** Runs platform-specific text selection gestures logic. */
internal actual suspend fun PointerInputScope.getTextFieldSelectionGestures(
    selectionState: TextFieldSelectionState,
    mouseSelectionObserver: MouseSelectionObserver,
    textDragObserver: TextDragObserver
) {
    val uiKitTextDragObserver = UIKitTextFieldTextDragObserver(selectionState)
    val clicksCounter = ClicksCounter(viewConfiguration)
    awaitEachGesture {
        while (true) {
            val downEvent = awaitDown()
            clicksCounter.update(downEvent.changes[0])
            val isPrecise = downEvent.isPrecisePointer
            if (
                isPrecise &&
                downEvent.buttons.isPrimaryPressed &&
                downEvent.changes.fastAll { !it.isConsumed }
            ) {
                // Use default BTF2 logic for mouse
                mouseSelectionBtf2(mouseSelectionObserver, clicksCounter, downEvent)
            } else if (!isPrecise) {
                when (clicksCounter.clicks) {
                    1 -> {
                        // The default BTF2 logic, except
                        // moving text cursor without selection requires custom TextDragObserver
                        touchSelectionFirstPress(
                            observer = uiKitTextDragObserver,
                            downEvent = downEvent
                        )
                    }

                    2 -> {
                        doRepeatingTapSelection(
                            downEvent.changes.first(),
                            selectionState,
                            SelectionAdjustment.Word
                        )
                    }

                    else -> {
                        val downChange = downEvent.changes.first()
                        clearSelection(
                            downChange,
                            selectionState
                        ) // Previous selection must be cleared, otherwise this closure won't get third (and further) click
                        doRepeatingTapSelection(
                            downChange,
                            selectionState,
                            SelectionAdjustment.Paragraph
                        )
                    }
                }
            }
        }
    }
}

private fun doRepeatingTapSelection(
    touchChange: PointerInputChange,
    selectionState: TextFieldSelectionState,
    selectionAdjustment: SelectionAdjustment
) {
    val selectionOffset = selectionState.textLayoutState.getOffsetForPosition(
        position = touchChange.position
    )
    touchChange.consume()

    val newSelection = selectionState.updateSelection(
        selectionState.textFieldState.visualText,
        selectionOffset,
        selectionOffset,
        isStartHandle = false,
        adjustment = selectionAdjustment,
    )

    selectionState.textFieldState.selectCharsIn(newSelection)
    selectionState.updateTextToolbarState(Selection)
}

private fun clearSelection(
    touchChange: PointerInputChange,
    selectionState: TextFieldSelectionState
) {
    val selectionOffset = selectionState.textLayoutState.getOffsetForPosition(
        position = touchChange.position
    )
    val clearedSelection = selectionState.updateSelection(
        TextFieldCharSequence(selectionState.textFieldState.visualText, TextRange.Zero),
        selectionOffset,
        selectionOffset,
        isStartHandle = false,
        adjustment = SelectionAdjustment.None,
    )
    selectionState.textFieldState.selectCharsIn(clearedSelection)
}

private class UIKitTextFieldTextDragObserver(
    private val textFieldSelectionState: TextFieldSelectionState,
    private val requestFocus: () -> Unit = {}
) : TextDragObserver {
    private var dragBeginPosition: Offset = Offset.Unspecified
    private var dragTotalDistance: Offset = Offset.Zero

    private fun onDragStop() {
        // Only execute clear-up if drag was actually ongoing.
        if (dragBeginPosition.isSpecified) {
            textFieldSelectionState.clearHandleDragging()
            dragBeginPosition = Offset.Unspecified
            dragTotalDistance = Offset.Zero
            textFieldSelectionState.directDragGestureInitiator = InputType.None
            requestFocus()
        }
    }

    override fun onDown(point: Offset) = Unit

    override fun onUp() = Unit

    override fun onStop() = onDragStop()

    override fun onCancel() = onDragStop()

    override fun onStart(startPoint: Offset) {
        if (!textFieldSelectionState.enabled) return

        textFieldSelectionState.directDragGestureInitiator = InputType.Touch

        dragBeginPosition = startPoint
        dragTotalDistance = Offset.Zero

        textFieldSelectionState.hapticFeedBack?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        // Long Press at the blank area, the cursor should show up at the end of the line.
        if (!textFieldSelectionState.textLayoutState.isPositionOnText(startPoint)) {
            val offset = textFieldSelectionState.textLayoutState.getOffsetForPosition(startPoint)
            textFieldSelectionState.textFieldState.placeCursorBeforeCharAt(offset)
        } else {
            if (textFieldSelectionState.textFieldState.visualText.isEmpty()) return
            val coercedOffset =
                textFieldSelectionState.textLayoutState.coercedInVisibleBoundsOfInputText(startPoint)
            textFieldSelectionState.placeCursorAtNearestOffset(
                textFieldSelectionState.textLayoutState.fromDecorationToTextLayout(coercedOffset)
            )
        }
        textFieldSelectionState.showCursorHandle = true
    }

    override fun onDrag(delta: Offset) {
        if (!textFieldSelectionState.enabled || textFieldSelectionState.textFieldState.visualText.isEmpty()) return

        dragTotalDistance += delta

        val currentDragPosition = dragBeginPosition + dragTotalDistance

        val coercedOffset =
            textFieldSelectionState.textLayoutState.coercedInVisibleBoundsOfInputText(
                currentDragPosition
            )
        // A common function must be used here because in iOS during a drag the cursor should move without adjustments,
        // as it does with a single tap
        textFieldSelectionState.placeCursorAtNearestOffset(
            textFieldSelectionState.textLayoutState.fromDecorationToTextLayout(coercedOffset)
        )
    }
}

// Copied from SelectionGestures.kt
// TODO: maybe should be refactored to use same AwaitDown with BTF1
private suspend fun AwaitPointerEventScope.awaitDown(): PointerEvent {
    var event: PointerEvent
    do {
        event = awaitPointerEvent(PointerEventPass.Main)
    } while (!event.changes.fastAll { it.changedToDownIgnoreConsumed() })
    return event
}