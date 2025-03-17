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

/**
 * The purpose of this entity is to isolate synchronization between a TextFieldValue
 * and the DOM HTMLTextAreaElement we are actually listening events on in order to show
 * the virtual keyboard.
 */
internal class BackingTextArea(
    private val imeOptions: ImeOptions,
    private val onEditCommand: (List<EditCommand>) -> Unit,
    private val onImeActionPerformed: (ImeAction) -> Unit,
    private val processKeyboardEvent: (KeyboardEvent) -> Boolean
) {
    private val textArea: HTMLTextAreaElement = createHtmlInput()

    private var editPhase: EditPhase = EditPhase.Default

    private fun initEvents(htmlInput: EventTarget) {
        htmlInput.addEventListener("keydown", {evt ->
            evt as KeyboardEvent
            console.log(evt.type, evt.timeStamp, evt.isComposing, evt)

            // this won't prevent other input-related events, in particular "compositionupdate"
            // even if it was triggered via key press
            evt.preventDefault()

            if (editPhase is EditPhase.CompositeMode) {
                return@addEventListener
            }

            editPhase = EditPhase.WaitingComposeActivity

            val processed = processKeyboardEvent(evt)
            if (!processed) {
                editPhase = EditPhase.Default
            }
        })

        htmlInput.addEventListener("beforeinput", { evt ->
            evt as InputEvent
            console.log("[binput] %c%s %c %s", "font-weight: bold", evt.inputType, "font-weight: normal", evt.data)


            evt.preventDefault()
        })

        htmlInput.addEventListener("input", { evt ->
            evt as InputEvent
            console.log("[input] %c%s %c %s", "font-weight: bold", evt.inputType, "font-weight: normal", evt.data)

            evt.preventDefault()
        })

        htmlInput.addEventListener("compositionstart", {evt ->
            evt as CompositionEvent
            console.log(evt.type, evt.timeStamp, evt.data)
            onEditCommand(listOf(DeleteSurroundingTextInCodePointsCommand(1, 0)))

            editPhase  = EditPhase.CompositeMode
        })

        htmlInput.addEventListener("compositionupdate", {evt ->
            evt as CompositionEvent
            console.log(evt.type, evt.timeStamp, evt.data)

            onEditCommand(listOf(SetComposingTextCommand(evt.data!!, 1)))
            evt.preventDefault()
        })

        htmlInput.addEventListener("compositionend", {evt ->
            evt as CompositionEvent
            console.log(evt.type, evt.timeStamp, evt.data)
            evt.preventDefault()

            editPhase = EditPhase.WaitingComposeActivity
            onEditCommand(listOf(CommitTextCommand(evt.data!!, 1)))
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
        if (editPhase != EditPhase.WaitingComposeActivity) return

        println("updateState ${editPhase} ${textFieldValue.text}")

        textArea.value = textFieldValue.text
        textArea.setSelectionRange(textFieldValue.selection.start, textFieldValue.selection.end)

        editPhase = EditPhase.Default
    }

    fun dispose() {
        textArea.remove()
    }
}

private sealed interface EditPhase {
    data object Default : EditPhase
    data object WaitingComposeActivity : EditPhase
    data object CompositeMode: EditPhase
}
