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
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.browser.document
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.events.KeyboardEvent


internal interface ComposeCommandCommunicator {
    fun sendEditCommand(commands: List<EditCommand>)
    fun sendEditCommand(command: EditCommand) = sendEditCommand(listOf(command))

    fun sendKeyboardEvent(keyboardEvent: KeyboardEvent): Boolean
}


/**
 * The purpose of this entity is to isolate synchronization between a TextFieldValue
 * and the DOM HTMLTextAreaElement we are actually listening events on in order to show
 * the virtual keyboard.
 */
internal class BackingTextArea(
    imeOptions: ImeOptions,
    composeCommunicator : ComposeCommandCommunicator,
) {
    private val inputStrategy: DomInputStrategy = CommonDomInputStrategy(
        imeOptions.createDomElement(),
        composeCommunicator
    )

    private val textArea = inputStrategy.htmlInput

    fun register() {
        document.body?.appendChild(textArea)
    }

    fun focus() {
        textArea.focus()
    }

    fun blur() {
        textArea.blur()
    }

    fun updateHtmlInputPosition(offset: Offset) {
        textArea.style.left = "${offset.x}px"
        textArea.style.top = "${offset.y}px"

        focus()
    }

    fun updateState(textFieldValue: TextFieldValue) {
        inputStrategy.updateState(textFieldValue)
    }

    fun dispose() {
        textArea.remove()
    }
}

internal fun ImeOptions.createDomElement(): HTMLTextAreaElement {
    val htmlElement: HTMLTextAreaElement = document.createElement("textarea") as HTMLTextAreaElement

    htmlElement.setAttribute("autocorrect", "off")
    htmlElement.setAttribute("autocomplete", "off")
    htmlElement.setAttribute("autocapitalize", "off")
    htmlElement.setAttribute("spellcheck", "false")

    val inputMode = when (keyboardType) {
        KeyboardType.Text -> "text"
        KeyboardType.Ascii -> "text"
        KeyboardType.Number -> "number"
        KeyboardType.Phone -> "tel"
        KeyboardType.Uri -> "url"
        KeyboardType.Email -> "email"
        KeyboardType.Password -> "password"
        KeyboardType.NumberPassword -> "number"
        KeyboardType.Decimal -> "decimal"
        else -> "text"
    }

    val enterKeyHint = when (imeAction) {
        ImeAction.Default -> "enter"
        ImeAction.None -> "enter"
        ImeAction.Done -> "done"
        ImeAction.Go -> "go"
        ImeAction.Next -> "next"
        ImeAction.Previous -> "previous"
        ImeAction.Search -> "search"
        ImeAction.Send -> "send"
        else -> "enter"
    }

    htmlElement.setAttribute("inputmode", inputMode)
    htmlElement.setAttribute("enterkeyhint", enterKeyHint)

    htmlElement.style.apply {
        setProperty("position", "absolute")
        setProperty("user-select", "none")
        setProperty("forced-color-adjust", "none")
        setProperty("white-space", "pre-wrap")
        setProperty("align-content", "center")
        setProperty("top", "0")
        setProperty("left", "0")
        setProperty("padding", "0")
        setProperty("opacity", "0")
        setProperty("color", "transparent")
        setProperty("background", "transparent")
        setProperty("caret-color", "transparent")
        setProperty("outline", "none")
        setProperty("border", "none")
        setProperty("resize", "none")
        setProperty("text-shadow", "none")
        setProperty("z-index", "-1")
        // TODO: do we need pointer-events: none
        //setProperty("pointer-events", "none")
    }

    return htmlElement
}