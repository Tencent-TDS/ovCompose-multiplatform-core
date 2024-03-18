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

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.PlatformTextInputService
import androidx.compose.ui.text.input.TextFieldValue
import org.jetbrains.skiko.SkikoInput

internal class JSTextInputService(
    private val jsInputModeManager: DefaultInputModeManager,
    private val imeTextInputService: ImeTextInputService
) : PlatformTextInputService {

    data class CurrentInput(
        var value: TextFieldValue,
        var imeOptions: ImeOptions,
        val onEditCommand: ((List<EditCommand>) -> Unit),
        var onImeActionPerformed: (ImeAction) -> Unit
    )

    private var currentInput: CurrentInput? = null

    override fun startInput(
        value: TextFieldValue,
        imeOptions: ImeOptions,
        onEditCommand: (List<EditCommand>) -> Unit,
        onImeActionPerformed: (ImeAction) -> Unit
    ) {
        currentInput = CurrentInput(
            value,
            imeOptions,
            onEditCommand,
            onImeActionPerformed
        )

        if (jsInputModeManager.inputMode == InputMode.Touch) {
            imeTextInputService.setInput(currentInput)
            showSoftwareKeyboard()
        }
    }

    override fun stopInput() {
        currentInput = null
        imeTextInputService.clear()
    }

    override fun showSoftwareKeyboard() {
        if (jsInputModeManager.inputMode == InputMode.Touch) {
            imeTextInputService.setInput(currentInput)
            imeTextInputService.showSoftwareKeyboard()
        }
    }

    override fun hideSoftwareKeyboard() {
        if (jsInputModeManager.inputMode == InputMode.Touch) {
            imeTextInputService.hideSoftwareKeyboard()
        }
    }

    override fun notifyFocusedRect(rect: Rect) {
        if (jsInputModeManager.inputMode == InputMode.Touch) {
            imeTextInputService.updatePosition(rect)
        }
    }

    override fun updateState(oldValue: TextFieldValue?, newValue: TextFieldValue) {
        currentInput?.let { input ->
            input.value = newValue
            if (jsInputModeManager.inputMode == InputMode.Touch) {
                imeTextInputService.updateState(newValue)
            }
        }
    }

    val input = SkikoInput.Empty
}
