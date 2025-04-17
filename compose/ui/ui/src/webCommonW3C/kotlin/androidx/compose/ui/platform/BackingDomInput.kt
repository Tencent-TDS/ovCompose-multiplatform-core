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
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.browser.document
import org.w3c.dom.AddEventListenerOptions


internal interface ComposeCommandCommunicator {
    fun sendEditCommand(commands: List<EditCommand>)
    fun sendEditCommand(command: EditCommand) = sendEditCommand(listOf(command))

    fun sendKeyboardEvent(keyboardEvent: KeyEvent): Boolean
}


/**
 * The purpose of this entity is to isolate synchronization between a TextFieldValue
 * and the DOM HTMLTextAreaElement we are actually listening events on in order to show
 * the virtual keyboard.
 */
internal class BackingDomInput(
    imeOptions: ImeOptions,
    composeCommunicator : ComposeCommandCommunicator,
) {
    private val inputStrategy = DomInputStrategy(
        imeOptions,
        composeCommunicator
    )

    private val backingElement = inputStrategy.htmlInput

    fun register() {
        document.body?.appendChild(backingElement)
    }

    fun focus() {
        // we focusing in next frame as a part of fix for https://youtrack.jetbrains.com/issue/CMP-7836/
        // unfortunately on ios touch devices focus won't happen asynchronously
        // this leads to https://youtrack.jetbrains.com/issue/CMP-8013
        // which we are circumventing via focusing in a triggered click event
        backingElement.addEventListener("click", { evt ->
            evt.preventDefault()
            evt.stopPropagation()
            backingElement.focus()
        }, AddEventListenerOptions(once = true))
        backingElement.click()
    }

    fun blur() {
        backingElement.blur()
    }

    fun updateHtmlInputPosition(offset: Offset) {
        backingElement.style.left = "${offset.x}px"
        backingElement.style.top = "${offset.y}px"
    }

    fun updateState(textFieldValue: TextFieldValue) {
        inputStrategy.updateState(textFieldValue)
        focus()
    }

    fun dispose() {
        backingElement.remove()
    }
}