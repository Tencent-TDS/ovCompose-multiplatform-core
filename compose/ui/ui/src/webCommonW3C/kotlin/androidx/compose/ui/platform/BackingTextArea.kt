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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.text.input.CommitTextCommand
import androidx.compose.ui.text.input.DeleteSurroundingTextInCodePointsCommand
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.SetComposingTextCommand
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.browser.document
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.events.CompositionEvent
import org.w3c.dom.events.EventTarget
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.KeyboardEventInit

/**
 * The purpose of this entity is to isolate synchronization between a TextFieldValue
 * and the DOM HTMLTextAreaElement we are actually listening events on in order to show
 * the virtual keyboard.
 */
internal class BackingTextArea(
    private val imeOptions: ImeOptions,
    private val onEditCommand: (List<EditCommand>) -> Unit,
    private val onImeActionPerformed: (ImeAction) -> Unit,
    private val processKeyboardEvent: (KeyboardEvent) -> Unit
) {
    private val textArea: HTMLTextAreaElement = createHtmlInput()

    private var syncMode: EditSyncMode = EditSyncMode.FromHtml

    private fun processEvent(evt: KeyboardEvent): Boolean {
        // source element is currently in composition session (after "compositionstart" but before "compositionend")
        if (evt.isComposing) return false
        // Unidentified keys is what we get when we press key on a virtual keyboard
        // TODO: In theory nothing stops us from passing Unidentified keys but this yet to be investigated:
        // First, this way we will pass (and attempt to process) "dummy" KeyboardEvents that were designed not to have physical representation at all
        // Second, we need more tests on keyboard in general before doing this anyways
        if (evt.key == "Unidentified") return false
        processKeyboardEvent(evt)
        return true
    }

    private fun initEvents(htmlInput: EventTarget) {
        htmlInput.addEventListener("keydown", { evt ->
            console.log(evt.type, evt)
            evt as KeyboardEvent
            if (evt.isComposing) return@addEventListener

            processKeyboardEvent(evt)
            evt.preventDefault()
        })

        htmlInput.addEventListener("compositionstart", { evt ->
            console.log(evt.type, evt)
        })

        htmlInput.addEventListener("compositionupdate", { evt ->
            console.log(evt.type, evt)
        })

        htmlInput.addEventListener("compositionend", { evt ->
            console.log(evt.type, evt)
            evt.preventDefault()
        })

        htmlInput.addEventListener("beforeinput", { evt ->
            evt as InputEvent
            console.log(evt.type, evt.inputType, evt.data, evt)

            if (syncMode is EditSyncMode.FromCompose) {
                evt.preventDefault()
                return@addEventListener
            }

            if (evt.inputType == "insertFromComposition") {
                evt.preventDefault()
                syncMode = EditSyncMode.FromCompose
                onEditCommand(listOf(CommitTextCommand(evt.data!!, 1)))
            } else if (evt.inputType == "insertCompositionText") {
                syncMode = EditSyncMode.FromHtml
                onEditCommand(listOf(SetComposingTextCommand(evt.data!!, 1)))
            } else if (evt.inputType == "insertText") {
//                evt.preventDefault()
//                syncMode = EditSyncMode.FromCompose
//                onEditCommand(listOf(CommitTextCommand(evt.data!!, 1)))
            }
        })

        htmlInput.addEventListener("input", { evt ->
            evt as InputEvent
            console.log(evt.type, evt.inputType, evt.data, evt)
        })
    }

    private fun createHtmlInput(): HTMLTextAreaElement {
        val htmlInput = document.createElement("textarea") as HTMLTextAreaElement

        htmlInput.setAttribute("autocorrect", "off")
        htmlInput.setAttribute("autocomplete", "off")
        htmlInput.setAttribute("autocapitalize", "off")
        htmlInput.setAttribute("spellcheck", "false")

        val inputMode = when (imeOptions.keyboardType) {
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

        val enterKeyHint = when (imeOptions.imeAction) {
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

        htmlInput.setAttribute("inputmode", inputMode)
        htmlInput.setAttribute("enterkeyhint", enterKeyHint)

        htmlInput.style.apply {
            setProperty("position", "absolute")
            setProperty("user-select", "none")
            setProperty("forced-color-adjust", "none")
            setProperty("white-space", "pre-wrap")
            setProperty("align-content", "center")
            setProperty("top", "0")
            setProperty("left", "0")
            setProperty("padding", "0")
//            setProperty("opacity", "0")
//            setProperty("color", "transparent")
//            setProperty("background", "transparent")
//            setProperty("caret-color", "transparent")
//            setProperty("outline", "none")
//            setProperty("border", "none")
//            setProperty("resize", "none")
//            setProperty("text-shadow", "none")
        }

        initEvents(htmlInput)

        return htmlInput
    }

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
//        textArea.style.left = "${offset.x}px"
//        textArea.style.top = "${offset.y}px"

        focus()
    }

    fun updateState(textFieldValue: TextFieldValue) {
        console.log("update state", syncMode, textFieldValue.text, textFieldValue)
        if (syncMode is EditSyncMode.FromHtml) return
        try {
            textArea.value = textFieldValue.text
            textArea.setSelectionRange(textFieldValue.selection.start, textFieldValue.selection.end)

        } finally {
            syncMode = EditSyncMode.FromHtml
        }
    }

    fun dispose() {
        textArea.remove()
    }
}

// TODO: reuse in tests
private external interface KeyboardEventInitExtended : KeyboardEventInit {
    var keyCode: Int?
}

@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
private fun KeyboardEventInit.withKeyCode(key: Key) =
    (this as KeyboardEventInitExtended).apply {
        this.keyCode = key.keyCode.toInt()
    }


private sealed interface EditSyncMode {
    data object FromCompose : EditSyncMode
    data object FromHtml : EditSyncMode
}