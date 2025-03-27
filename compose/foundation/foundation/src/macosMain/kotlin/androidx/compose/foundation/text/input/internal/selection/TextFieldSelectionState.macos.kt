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

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.TextDragObserver
import androidx.compose.foundation.text.selection.MouseSelectionObserver
import androidx.compose.ui.input.pointer.PointerInputScope

/** Runs platform-specific text tap gestures logic. */
internal actual suspend fun PointerInputScope.detectTextFieldTapGestures(
    selectionState: TextFieldSelectionState,
    interactionSource: MutableInteractionSource?,
    requestFocus: () -> Unit,
    showKeyboard: () -> Unit
) = defaultDetectTextFieldTapGestures(selectionState, interactionSource, requestFocus, showKeyboard)

/** Runs platform-specific text selection gestures logic. */
internal actual suspend fun PointerInputScope.getTextFieldSelectionGestures(
    selectionState: TextFieldSelectionState,
    mouseSelectionObserver: MouseSelectionObserver,
    textDragObserver: TextDragObserver
) = defaultTextFieldSelectionGestures(mouseSelectionObserver, textDragObserver)