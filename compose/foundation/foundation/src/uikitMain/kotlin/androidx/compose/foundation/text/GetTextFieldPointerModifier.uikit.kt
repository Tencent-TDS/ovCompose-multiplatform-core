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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.OffsetMapping

@OptIn(InternalFoundationTextApi::class)
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
    detectTapGestures(
        onDoubleTap = { touchPointOffset ->
            tapTextFieldToFocus(
                state,
                focusRequester,
                !readOnly
            )

            if (manager.value.text.isEmpty()) return
            manager.enterSelectionMode()
            state?.layoutResult?.let { layoutResult ->
                val offset = layoutResult.getOffsetForPosition(touchPointOffset)
                manager.updateSelection(
                    value = manager.value,
                    transformedStartOffset = offset,
                    transformedEndOffset = offset,
                    isStartHandle = false,
                    adjustment = SelectionAdjustment.Word
                )
                manager.dragBeginOffsetInText = offset
            }

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