/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.platform

import androidx.compose.ui.text.input.BackspaceCommand
import androidx.compose.ui.text.input.CommitTextCommand
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.FinishComposingTextCommand
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.PlatformTextInputService
import androidx.compose.ui.text.input.SetComposingRegionCommand
import androidx.compose.ui.text.input.SetComposingTextCommand
import androidx.compose.ui.text.input.TextFieldValue
import org.jetbrains.skiko.SkikoInput
import org.jetbrains.skiko.SkikoTextRange

sealed interface Strategy {
    object S1_BAD:Strategy
    object S2_GOOD:Strategy
}
private val TODO_STRATEGY:Strategy = Strategy.S1_BAD

internal class UIKitTextInputService(
    showSoftwareKeyboard: () -> Unit,
    hideSoftwareKeyboard: () -> Unit,
) : PlatformTextInputService {

    data class CurrentInput(
        var value: TextFieldValue,
        val onEditCommand: (List<EditCommand>, (TextFieldValue) -> Unit) -> Unit
    )

    private val _showSoftwareKeyboard: () -> Unit = showSoftwareKeyboard
    private val _hideSoftwareKeyboard: () -> Unit = hideSoftwareKeyboard
    private var currentInput: CurrentInput? = null

    override fun startInput(
        value: TextFieldValue,
        imeOptions: ImeOptions,
        onEditCommand: (List<EditCommand>, onUpdateValue: (TextFieldValue) -> Unit) -> Unit,
        onImeActionPerformed: (ImeAction) -> Unit
    ) {
        currentInput = CurrentInput(
            value,
            onEditCommand
        )
        showSoftwareKeyboard()
    }

    override fun stopInput() {
        currentInput = null
    }

    override fun showSoftwareKeyboard() {
        _showSoftwareKeyboard()
    }

    override fun hideSoftwareKeyboard() {
        _hideSoftwareKeyboard()
    }

    override fun updateState(oldValue: TextFieldValue?, newValue: TextFieldValue) {
        println("DIMA UPDATE STATE Strategy.S1_BAD, newValue: $newValue")
        Exception().printStackTrace()
        currentInput?.let { input ->
            if (TODO_STRATEGY == Strategy.S1_BAD) {
                input.value = newValue
            }
        }
    }

    val skikoInput = object : SkikoInput {

        /**
         * A Boolean value that indicates whether the text-entry object has any text.
         * https://developer.apple.com/documentation/uikit/uikeyinput/1614457-hastext
         */
        override fun hasText(): Boolean {
            val result = getState()?.text?.isNotEmpty() ?: false
            log()
            return result
        }

        /**
         * Inserts a character into the displayed text.
         * Add the character text to your class’s backing store at the index corresponding to the cursor and redisplay the text.
         * https://developer.apple.com/documentation/uikit/uikeyinput/1614543-inserttext
         * @param text A string object representing the character typed on the system keyboard.
         */
        override fun insertText(text: String) {
            sendEditCommand(CommitTextCommand(text, 1))
            log("text: $text")
        }

        /**
         * Deletes a character from the displayed text.
         * Remove the character just before the cursor from your class’s backing store and redisplay the text.
         * https://developer.apple.com/documentation/uikit/uikeyinput/1614572-deletebackward
         */
        override fun deleteBackward() {
            sendEditCommand(
                BackspaceCommand()
            )
            log()
        }

        /**
         * The text position for the end of a document.
         * https://developer.apple.com/documentation/uikit/uitextinput/1614555-endofdocument
         */
        override fun endOfDocument(): Long {
            val result = getState()?.text?.length?.toLong() ?: 0L
            log()
            return result
        }

        /**
         * The range of selected text in a document.
         * If the text range has a length, it indicates the currently selected text.
         * If it has zero length, it indicates the caret (insertion point).
         * If the text-range object is nil, it indicates that there is no current selection.
         * https://developer.apple.com/documentation/uikit/uitextinput/1614541-selectedtextrange
         */
        override fun selectedTextRange(): SkikoTextRange? {
            val selection = getState()?.selection
            val result = if (selection != null) {
                SkikoTextRange(selection.start, selection.end)
            } else {
                null
            }
            log("result: $result")
            return result
        }

        /**
         * Returns the text in the specified range.
         * https://developer.apple.com/documentation/uikit/uitextinput/1614527-text
         * @param range A range of text in a document.
         * @return A substring of a document that falls within the specified range.
         */
        override fun textInRange(range: SkikoTextRange): String? {
            val text = getState()?.text
            val result =
                if (text != null && text.isNotEmpty() && range.start >= 0 && range.end >= 0 && text.length >= range.end) {
                    val substring = text.substring(range.start, range.end)
                    substring.replace("\n", "").ifEmpty { null } //todo
                } else {
                    null
                }
            log("range: $range, result: $result")
            return result
        }

        /**
         * Replaces the text in a document that is in the specified range.
         * https://developer.apple.com/documentation/uikit/uitextinput/1614558-replace
         * @param range A range of text in a document.
         * @param text A string to replace the text in range.
         */
        override fun replaceRange(range: SkikoTextRange, text: String) {
            sendEditCommand(
                SetComposingRegionCommand(range.start, range.end),
                SetComposingTextCommand(text, 1),
                FinishComposingTextCommand(),
            )
            log("range: $range, text: $text")
        }

        /**
         * Inserts the provided text and marks it to indicate that it is part of an active input session.
         * Setting marked text either replaces the existing marked text or,
         * if none is present, inserts it in place of the current selection.
         * https://developer.apple.com/documentation/uikit/uitextinput/1614465-setmarkedtext
         * @param markedText The text to be marked.
         * @param selectedRange A range within markedText that indicates the current selection.
         * This range is always relative to markedText.
         */
        override fun setMarkedText(markedText: String?, selectedRange: SkikoTextRange) {
            if (markedText != null) {
                sendEditCommand(
                    SetComposingTextCommand(markedText, 1)
                )
            }
            log("markedText: $markedText, selectedRange: $selectedRange")
//            // [markedText] is text about to confirm by user
//            // see more: https://developer.apple.com/documentation/uikit/uitextinput?language=objc
//            val (locationRelative, lengthRelative) = selectedRange.useContents {
//                location to length
//            }
//            val cursor = inputText.lastIndex
//            val location = cursor + 1
//            val length = markedText?.length ?: 0
//
//            markedText?.let {
//                _markedTextRange = SkikoTextRange(
//                    SkikoTextPosition(location.toLong()),
//                    SkikoTextPosition(location.toLong() + length.toLong())
//                )
//                _selectedTextRange = SkikoTextRange(
//                    SkikoTextPosition(location.toLong()),
//                    SkikoTextPosition(location.toLong() + length.toLong())
//                )
//                _markedText = markedText
//            }
        }

        /**
         * The range of currently marked text in a document.
         * If there is no marked text, the value of the property is nil.
         * Marked text is provisionally inserted text that requires user confirmation;
         * it occurs in multistage text input.
         * The current selection, which can be a caret or an extended range, always occurs within the marked text.
         * https://developer.apple.com/documentation/uikit/uitextinput/1614489-markedtextrange
         */
        override fun markedTextRange(): SkikoTextRange? {
            val composition = getState()?.composition
            val result = if (composition != null) {
                SkikoTextRange(composition.start, composition.end)
            } else {
                null
            }
            log("result: $result")
            return result
        }

        /**
         * Unmarks the currently marked text.
         * After this method is called, the value of markedTextRange is nil.
         * https://developer.apple.com/documentation/uikit/uitextinput/1614512-unmarktext
         */
        override fun unmarkText() {
            sendEditCommand(FinishComposingTextCommand())
            log()
        }

        private fun log(vararg messages: Any) {
            println("/----begin----------${counter++}\\")
            println(messages.map { it.toString() })
            println("value: ${getState()}")
            println(Exception().stackTraceToString().split("\n")[4].split("        ").last())
            println("\\-----end-----------/")
            println("")
        }

    }

    private fun sendEditCommand(vararg commands: EditCommand) {
        currentInput?.onEditCommand?.invoke(commands.toList()) { value ->
            println("DIMA UPDATE STATE Strategy.S2_GOOD, value: $value")
            Exception().printStackTrace()
            if (TODO_STRATEGY == Strategy.S2_GOOD) {
                currentInput?.let { input ->
                    input.value = value
                }
            }
        }
    }

    private fun getState():TextFieldValue? {
        return currentInput?.value
    }

}

private var counter:Int = 0
