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

package androidx.compose.foundation.text

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.selection.SelectionAdjustment
import androidx.compose.foundation.text.selection.TextFieldSelectionManager
import androidx.compose.foundation.text.selection.getTextFieldSelection
import androidx.compose.foundation.text.selection.isSelectionHandleInVisibleBound
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue

internal actual fun getTextFieldPointerModifier(
    manager: TextFieldSelectionManager,
    enabled: Boolean,
    interactionSource: MutableInteractionSource?,
    state: TextFieldState,
    focusRequester: FocusRequester,
    readOnly: Boolean,
    offsetMapping: OffsetMapping
): Modifier = if (enabled) {
    if (isInTouchMode) {
        val selectionModifier = getSelectionModifier(manager)
        val tapHandlerModifier = getTapHandlerModifier(
            interactionSource,
            state,
            focusRequester,
            readOnly,
            offsetMapping,
            manager
        )
        Modifier
            .then(tapHandlerModifier)
            .then(selectionModifier)
            .pointerHoverIcon(textPointerIcon)
    } else {
        Modifier
            .mouseDragGestureDetector(
                observer = manager.mouseSelectionObserver,
                enabled = enabled
            )
            .pointerHoverIcon(textPointerIcon)
    }
} else {
    Modifier
}

@OptIn(InternalFoundationTextApi::class)
private fun getTapHandlerModifier(
    interactionSource: MutableInteractionSource?,
    state: TextFieldState,
    focusRequester: FocusRequester,
    readOnly: Boolean,
    offsetMapping: OffsetMapping,
    manager: TextFieldSelectionManager
) = Modifier.then(Modifier.pointerInput(interactionSource) {
    /*
    We need to move tap recognizer here from selection modifier (as it is in common)
    because otherwise we have onDoubleTap call and onTap call, and onDoubleTap will execute before onTap.
     */
    detectTapGestures(
        onDoubleTap = { touchPointOffset ->
            tapTextFieldToFocus(
                state,
                focusRequester,
                !readOnly
            )
            manager.doDoubleTapSelection(touchPointOffset)
        },
        onTap = { touchPointOffset ->
            tapTextFieldToFocus(
                state,
                focusRequester,
                !readOnly
            )
            if (state.hasFocus) {
                if (state.handleState != HandleState.Selection) {
                    state.layoutResult?.let { layoutResult ->
                        TextFieldDelegate.setCursorOffset(
                            touchPointOffset,
                            layoutResult,
                            state.processor,
                            offsetMapping,
                            state.onValueChange
                        )
                        // Won't enter cursor state when text is empty.
                        if (state.textDelegate.text.isNotEmpty()) {
                            state.handleState = HandleState.Cursor
                        }
                    }
                } else {
                    manager.deselect(touchPointOffset)
                }
            }
        }
    )
})

private fun getSelectionModifier(manager: TextFieldSelectionManager): Modifier {
    val selectionModifier =
        Modifier
            .then(Modifier.pointerInput(Unit) {
                detectDragGesturesAfterLongPress(onDragStart = {
                    manager.touchSelectionObserver.onStart(
                        startPoint = it
                    )
                },
                    onDrag = { _, delta -> manager.touchSelectionObserver.onDrag(delta = delta) },
                    onDragCancel = { manager.touchSelectionObserver.onCancel() },
                    onDragEnd = { manager.touchSelectionObserver.onStop() }
                )
            })
    return selectionModifier
}

private fun TextFieldSelectionManager.doDoubleTapSelection(touchPointOffset: Offset) {
    if (value.text.isEmpty()) return
    enterSelectionMode()
    state?.layoutResult?.let { layoutResult ->
        val offset = layoutResult.getOffsetForPosition(touchPointOffset)
        updateSelection(
            value = value,
            transformedStartOffset = offset,
            transformedEndOffset = offset,
            isStartHandle = false,
            adjustment = SelectionAdjustment.Word
        )
    }
}

/**
 * Copied from TextFieldSelectionManager.kt
 */
private fun TextFieldSelectionManager.updateSelection(
    value: TextFieldValue,
    transformedStartOffset: Int,
    transformedEndOffset: Int,
    isStartHandle: Boolean,
    adjustment: SelectionAdjustment
) {
    val transformedSelection = TextRange(
        offsetMapping.originalToTransformed(value.selection.start),
        offsetMapping.originalToTransformed(value.selection.end)
    )

    val newTransformedSelection = getTextFieldSelection(
        textLayoutResult = state?.layoutResult?.value,
        rawStartOffset = transformedStartOffset,
        rawEndOffset = transformedEndOffset,
        previousSelection = if (transformedSelection.collapsed) null else transformedSelection,
        isStartHandle = isStartHandle,
        adjustment = adjustment
    )

    val originalSelection = TextRange(
        start = offsetMapping.transformedToOriginal(newTransformedSelection.start),
        end = offsetMapping.transformedToOriginal(newTransformedSelection.end)
    )

    if (originalSelection == value.selection) return

    hapticFeedBack?.performHapticFeedback(HapticFeedbackType.TextHandleMove)

    val newValue = createTextFieldValue(
        annotatedString = value.annotatedString,
        selection = originalSelection
    )
    onValueChange(newValue)

    // showSelectionHandleStart/End might be set to false when scrolled out of the view.
    // When the selection is updated, they must also be updated so that handles will be shown
    // or hidden correctly.
    state?.showSelectionHandleStart = isSelectionHandleInVisibleBound(true)
    state?.showSelectionHandleEnd = isSelectionHandleInVisibleBound(false)
}

/**
 * Copied from TextFieldSelectionManager.kt
 */
private fun createTextFieldValue(
    annotatedString: AnnotatedString,
    selection: TextRange
): TextFieldValue {
    return TextFieldValue(
        annotatedString = annotatedString,
        selection = selection
    )
}
