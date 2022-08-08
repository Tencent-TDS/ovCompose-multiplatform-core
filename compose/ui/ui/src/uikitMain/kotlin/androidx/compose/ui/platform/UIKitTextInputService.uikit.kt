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
import kotlinx.cinterop.CValue
import org.jetbrains.skiko.SkikoInput
import org.jetbrains.skiko.SkikoTextPosition
import org.jetbrains.skiko.SkikoTextRange
import platform.Foundation.NSRange
import platform.UIKit.UITextRange

internal class UIKitTextInputService(
    showSoftwareKeyboard: () -> Unit,
    hideSoftwareKeyboard: () -> Unit,
) : PlatformTextInputService {

    data class CurrentInput(
        var value: TextFieldValue,
        val onEditCommand: ((List<EditCommand>) -> Unit),
    )

    private val _showSoftwareKeyboard: () -> Unit = showSoftwareKeyboard
    private val _hideSoftwareKeyboard: () -> Unit = hideSoftwareKeyboard
    private var currentInput: CurrentInput? = null

    override fun startInput(
        value: TextFieldValue,
        imeOptions: ImeOptions,
        onEditCommand: (List<EditCommand>) -> Unit,
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
        currentInput?.let { input ->
            input.value = newValue
        }
    }

    val skikoInput = object : SkikoInput {
        override fun hasText(): Boolean = currentInput?.value?.text?.isNotEmpty() ?: false

        override fun insertText(text: String) {
            sendEditCommand(CommitTextCommand(text, 1))
        }

        override fun deleteBackward() {
            sendEditCommand(
                FinishComposingTextCommand(),
                BackspaceCommand()
            )
        }

        override fun endOfDocument(): Long = currentInput?.value?.text?.length?.toLong() ?: 0L

        override fun selectedTextRange(): UITextRange? {
            val selection = currentInput?.value?.selection
            if (selection != null) {
                return SkikoTextRange(selection.start, selection.end)
            } else {
                return null
            }
        }

        override fun textInRange(range: UITextRange): String? {
            val text = currentInput?.value?.text ?: return null
            val from = ((range as SkikoTextRange).start() as SkikoTextPosition).position
            val to = (range.end() as SkikoTextPosition).position
            if (text.isNotEmpty() && from >= 0 && to >= 0 && text.length >= to) {
                val substring = text.substring(from.toInt(), to.toInt())
                return substring.replace("\n", "").ifEmpty { null } //todo
            } else {
                return null
            }
        }
        
        override fun replaceRange(range: UITextRange, withText: String) {
            val start = ((range as SkikoTextRange).start() as SkikoTextPosition).position.toInt()
            val end = (range.end() as SkikoTextPosition).position.toInt()
            sendEditCommand(
                FinishComposingTextCommand(),
                SetComposingRegionCommand(start, end),
                SetComposingTextCommand(withText, 1),
                FinishComposingTextCommand(),
            )
        }

        override fun setMarkedText(markedText: String?, selectedRange: CValue<NSRange>) {
            if (markedText != null) {
                sendEditCommand(
                    SetComposingTextCommand(markedText, 1)
                )
            }
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

        override fun markedTextRange(): UITextRange? {
            val composition = currentInput?.value?.composition
            if (composition != null) {
                return SkikoTextRange(composition.start, composition.end)
            }
            return null
        }

        override fun unmarkText() {
            sendEditCommand(FinishComposingTextCommand())
        }

        private fun sendEditCommand(vararg commands: EditCommand) {
            currentInput?.onEditCommand?.invoke(commands.toList())
        }

    }

}
