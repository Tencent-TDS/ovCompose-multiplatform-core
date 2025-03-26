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

package androidx.compose.ui.text.input

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.TextRange

/**
 * An adapter for `foundation.TextFieldState`, which is not accessible in the `ui` module.
 *
 * The text itself is provided by the [CharSequence] supertype.
 * The selection is provided by [selection].
 * The composition is provided by [composition].
 */
@ExperimentalComposeUiApi
interface TextEditorState : CharSequence {
    /**
     * The selection in the text field.
     */
    val selection: TextRange

    /**
     * The composition in the text field.
     */
    val composition: TextRange?
}

/**
 * The scope in which the [PlatformTextInputService2] implementation can make changes to the
 * [TextEditorState].
 */
@ExperimentalComposeUiApi
interface TextEditingScope {
    /**
     * Deletes text around the cursor.
     *
     * This intends to replicate [DeleteSurroundingTextInCodePointsCommand] for
     * [PlatformTextInputService2].
     */
    fun deleteSurroundingTextInCodePoints(lengthBeforeCursor: Int, lengthAfterCursor: Int)

    /**
     * Commits text and repositions the cursor.
     *
     * This intends to replicate [CommitTextCommand] for [PlatformTextInputService2].
     */
    fun commitText(text: CharSequence, newCursorPosition: Int)

    /**
     * Sets the composing text and repositions the cursor.
     *
     * This intends to replicate [SetComposingTextCommand] for [PlatformTextInputService2].
     */
    fun setComposingText(text: CharSequence, newCursorPosition: Int)
}

/**
 * The interface for classes responsible for platform-specific behaviors of text fields.
 */
@ExperimentalComposeUiApi
interface PlatformTextInputService2 {
    /**
     * Starts the text input session for given text field.
     */
    fun startInput(
        state: TextEditorState,
        imeOptions: ImeOptions,
        editText: (block: TextEditingScope.() -> Unit) -> Unit,
    )

    /**
     * Ends the current text input session.
     */
    fun stopInput()

    /**
     * Notifies the implementation of the rectangle where the actual text editing happens (at the
     * caret).
     *
     * @param rect The rectangle, relative to root.
     */
    fun focusedRectChanged(rect: Rect)
}