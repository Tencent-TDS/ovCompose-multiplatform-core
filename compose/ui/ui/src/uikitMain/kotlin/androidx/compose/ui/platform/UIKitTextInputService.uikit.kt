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
import kotlin.system.getTimeMillis
import org.jetbrains.skiko.SkikoInput
import org.jetbrains.skiko.SkikoTextRange

internal class UIKitTextInputService(
    showSoftwareKeyboard: () -> Unit,
    hideSoftwareKeyboard: () -> Unit,
    private val updateView: () -> Unit,
    private val recomposition: () -> Unit,
) : PlatformTextInputService {

    data class CurrentInput(
        var value: TextFieldValue,
        val onEditCommand: (List<EditCommand>) -> TextFieldValue
    )

    private val _showSoftwareKeyboard: () -> Unit = showSoftwareKeyboard
    private val _hideSoftwareKeyboard: () -> Unit = hideSoftwareKeyboard
    private var currentInput: CurrentInput? = null

    override fun startInput(
        value: TextFieldValue,
        imeOptions: ImeOptions,
        onEditCommand: (List<EditCommand>) -> TextFieldValue,
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
        println("DIMA updateState, time: ${getTimeMillis()}")
        println("oldValue: $oldValue")
        println("newValue: $newValue")
        if (oldValue != null && newValue.text.length > oldValue.text.length + 2) {
            Exception().printStackTrace()
        }
        currentInput?.let { input ->
            input.value = newValue
        }
        updateView()
    }

    val skikoInput = object : SkikoInput {

        /**
         * A Boolean value that indicates whether the text-entry object has any text.
         * https://developer.apple.com/documentation/uikit/uikeyinput/1614457-hastext
         */
        override fun hasText(): Boolean = getState()?.text?.isNotEmpty() ?: false

        /**
         * Inserts a character into the displayed text.
         * Add the character text to your class’s backing store at the index corresponding to the cursor and redisplay the text.
         * https://developer.apple.com/documentation/uikit/uikeyinput/1614543-inserttext
         * @param text A string object representing the character typed on the system keyboard.
         */
        override fun insertText(text: String) {
            sendEditCommand(CommitTextCommand(text, 1))
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
        }

        /**
         * The text position for the end of a document.
         * https://developer.apple.com/documentation/uikit/uitextinput/1614555-endofdocument
         */
        override fun endOfDocument(): Long = getState()?.text?.length?.toLong() ?: 0L

        /**
         * The range of selected text in a document.
         * If the text range has a length, it indicates the currently selected text.
         * If it has zero length, it indicates the caret (insertion point).
         * If the text-range object is nil, it indicates that there is no current selection.
         * https://developer.apple.com/documentation/uikit/uitextinput/1614541-selectedtextrange
         */
        override fun selectedTextRange(): SkikoTextRange? {
            return SkikoTextRange(0, 1)
            //todo implement selection and menu in multistage input
//            val selection = getState()?.selection
//            return if (selection != null) {
//                SkikoTextRange(selection.start, selection.end)
//            } else {
//                null
//            }
        }

        /**
         * Returns the text in the specified range.
         * https://developer.apple.com/documentation/uikit/uitextinput/1614527-text
         * @param range A range of text in a document.
         * @return A substring of a document that falls within the specified range.
         */
        override fun textInRange(range: SkikoTextRange): String? {
            val text = getState()?.text
            return if (!text.isNullOrEmpty() && range.start >= 0 && range.end >= 0 && text.length >= range.end) {
                val substring = text.substring(range.start, range.end)
                substring.replace("\n", "").ifEmpty { null } //todo maybe redundant
            } else {
                null
            }
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
            log()
//            Exception().printStackTrace()
            val composition = getState()?.composition
            return if (composition != null) {
                SkikoTextRange(composition.start, composition.end)
            } else {
                null
            }
        }

        /**
         * Unmarks the currently marked text.
         * After this method is called, the value of markedTextRange is nil.
         * https://developer.apple.com/documentation/uikit/uitextinput/1614512-unmarktext
         */
        override fun unmarkText() {
            sendEditCommand(FinishComposingTextCommand())
        }

        private fun log(vararg messages: Any) {
//            println("/----begin----------${counter++}\\")
//            println("time: ${getTimeMillis()}")
//            println(messages.map { it.toString() })
//            println("value: ${getState()}")
//            println(Exception().stackTraceToString().split("\n")[4].split("        ").last())
//            println("\\-----end-----------/")
//            println("")
        }

    }

    var recursion = 0
    private fun sendEditCommand(vararg commands: EditCommand) {
        currentInput?.let { input ->
            recursion++
            if (recursion <= 1) {
                val newValue = input.onEditCommand(commands.toList())
//                println("DIMA before recomposition ${getTimeMillis()}")
//                recomposition()
//                println("DIMA after recomposition ${getTimeMillis()}")
                if (false) {
                    input.value = newValue
                }
            } else {
                throw Error("BAD recursion depth: $recursion")
            }
            recursion--
        }
    }

    private fun getState():TextFieldValue? {
        return currentInput?.value
    }

}

private var counter:Int = 0
