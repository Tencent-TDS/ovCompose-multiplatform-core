/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.PlatformTextInputService
import androidx.compose.ui.text.input.TextFieldValue

internal interface InputAwareInputService {
    fun getOffset(rect: Rect): Offset
    fun processKeyboardEvent(keyboardEvent: KeyEvent): Boolean
}

internal abstract class WebTextInputService : PlatformTextInputService, InputAwareInputService {

    private var mBackingDomInput: BackingDomInput? = null
        set(value) {
            field?.dispose()
            field = value
        }

    override fun startInput(
        value: TextFieldValue,
        imeOptions: ImeOptions,
        onEditCommand: (List<EditCommand>) -> Unit,
        onImeActionPerformed: (ImeAction) -> Unit
    ) {
        mBackingDomInput =
            BackingDomInput(
                imeOptions = imeOptions,
                composeCommunicator = object : ComposeCommandCommunicator {
                    override fun sendKeyboardEvent(keyboardEvent: KeyEvent): Boolean {
                        return this@WebTextInputService.processKeyboardEvent(keyboardEvent)
                    }

                    override fun sendEditCommand(commands: List<EditCommand>) {
                        onEditCommand(commands)
                    }
                }
            )
        mBackingDomInput?.register()

        showSoftwareKeyboard()
    }

    override fun stopInput() {
        mBackingDomInput?.dispose()
    }

    override fun showSoftwareKeyboard() {
        mBackingDomInput?.focus()
    }

    override fun hideSoftwareKeyboard() {
        mBackingDomInput?.blur()
    }

    override fun updateState(oldValue: TextFieldValue?, newValue: TextFieldValue) {
        mBackingDomInput?.updateState(newValue)
    }

    override fun notifyFocusedRect(rect: Rect) {
        super.notifyFocusedRect(rect)
        mBackingDomInput?.updateHtmlInputPosition(getOffset(rect))
    }

}