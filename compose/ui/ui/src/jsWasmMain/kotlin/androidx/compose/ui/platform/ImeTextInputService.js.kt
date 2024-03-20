/*
 * Copyright 2023 The Android Open Source Project
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
import androidx.compose.ui.text.input.CommitTextCommand
import androidx.compose.ui.text.input.DeleteAllCommand
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.SetSelectionCommand
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Density
import kotlinx.browser.document
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.asList
import org.w3c.dom.events.*
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.HTMLCanvasElement

internal open class ImeTextInputService(
    private val canvas: HTMLCanvasElement,
    private val density: Density
) {
    private val canvasId = canvas.id.takeIf { it.isNotBlank() } ?: canvas.hashCode().toString()
    private val inputId = "compose-software-input-$canvasId"

    private val imeKeyboardListener = ImeKeyboardListenerImpl()

    private var composeInput: JSTextInputService.CurrentInput? = null

    private var currentHtmlInput: HTMLTextAreaElement? = null

    private fun createHtmlInput(): HTMLTextAreaElement {
        val htmlInput = document.createElement("textarea") as HTMLTextAreaElement

        // handle ActionKey(Enter)
        val keyHandler: (Event) -> Unit = keyHandler@{ event ->
            event as KeyboardEvent
            if (event.key == "Enter" && event.type == "keydown") {
                runImeActionIfRequired()
            }
        }

        htmlInput.apply {
            setAttribute("autocorrect", "off")
            setAttribute("autocomplete", "off")
            setAttribute("autocapitalize", "off")
            setAttribute("spellcheck", "false")
            setAttribute("readonly", "true")
            className = inputId
            id = inputId
            style.apply {
                setProperty("position", "absolute")
                setProperty("user-select", "none")
                setProperty("forced-color-adjust", "none")
                setProperty("white-space", "pre-wrap")
                setProperty("align-content", "center")
                setProperty("top", "0px")
                setProperty("left", "0px")
                setProperty("padding", "0px")
                setProperty("opacity", "0")
                setProperty("color", "transparent")
                setProperty("background", "transparent")
                setProperty("caret-color", "transparent")
                setProperty("outline", "none")
                setProperty("border", "none")
                setProperty("resize", "none")
                setProperty("text-shadow", "none")
            }
            // disable native context menu
            val eventHandler: (MouseEvent) -> Any = eventHandler@{ event ->
                event.preventDefault()
                event.stopPropagation()
                event.stopImmediatePropagation()
                return@eventHandler false
            }

            oncontextmenu = eventHandler as ((MouseEvent) -> Unit)
            addEventListener("keyup", keyHandler, false)
            addEventListener("keydown", keyHandler, false)
            addEventListener("input", eventHandler@{ event ->
                val el = (event.target as HTMLTextAreaElement)
                val text = el.value
                val cursorPosition = el.selectionEnd
                sendImeValueToCompose(text, cursorPosition)
            }, false)
        }
        document.body?.appendChild(htmlInput)

        return htmlInput
    }

    private fun createOrGetHtmlInput(): HTMLTextAreaElement {
        // Use the same input to prevent flashing.
        return (currentHtmlInput ?: createHtmlInput()) as HTMLTextAreaElement
    }

    fun clear() {
        // console.log("clear")
        composeInput = null
        currentHtmlInput = null
        document.getElementsByClassName(inputId).asList().forEach {
            it as HTMLTextAreaElement
            document.body?.removeChild(it)
        }
    }

    fun showSoftwareKeyboard() {
        // Safari accepts the focus event only inside a touch event handler.
        // if event from js call, it's not will work
        composeInput?.let { composeInput ->
            val htmlInput = createOrGetHtmlInput()

            val inputMode = when (composeInput.imeOptions.keyboardType) {
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
            val enterKeyHint = when (composeInput.imeOptions.imeAction) {
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
            val start = composeInput.value.selection.start ?: htmlInput.value.length - 1
            val end = composeInput.value.selection.start ?: htmlInput.value.length - 1

            htmlInput.setAttribute("inputmode", inputMode)
            htmlInput.setAttribute("enterkeyhint", enterKeyHint)
            htmlInput.value = composeInput.value.text

            htmlInput.setSelectionRange(start, end)
            currentHtmlInput = htmlInput
            imeKeyboardListener.onHide {
                clear()
            }
        }
    }

    fun hideSoftwareKeyboard() {
        clear()
    }

    fun updateState(newValue: TextFieldValue) {
        currentHtmlInput?.let { it ->
            it.value = newValue.text
            it.setSelectionRange(newValue.selection.start, newValue.selection.end)
        }
    }

    fun updatePosition(rect: Rect) {
        val scale = density.density
        canvas.getBoundingClientRect()?.let { offset ->
            val offsetX = offset.left.toFloat().coerceAtLeast(0f) + (rect.left / scale)
            val offsetY = offset.top.toFloat().coerceAtLeast(0f) + (rect.top / scale)

            currentHtmlInput?.let { html ->

                html.style.apply {
                    setProperty("left", "${offsetX}px")
                    setProperty("top", "${offsetY}px")
                }

                val hasFocus = html == document.activeElement

                if (!hasFocus) {
                    html.removeAttribute("readonly")
                    html.focus()
                }
            }
        }
    }

    private fun sendImeValueToCompose(text: String, newCursorPosition: Int? = null) {
        composeInput?.let { input ->
            val value = if (text == "\n") {
                ""
            } else {
                text
            }

            if (newCursorPosition != null) {
                input.onEditCommand(
                    listOf(
                        DeleteAllCommand(),
                        CommitTextCommand(value, 1),
                        SetSelectionCommand(newCursorPosition, newCursorPosition)
                    )
                )
            } else {
                input.onEditCommand(
                    listOf(
                        CommitTextCommand(value, 1)
                    )
                )
            }
        }
    }

    private fun imeActionRequired(): Boolean =
        composeInput?.imeOptions?.run {
            singleLine || (
                imeAction != ImeAction.None
                    && imeAction != ImeAction.Default
                    && imeAction != ImeAction.Search
                )
        } ?: false

    private fun runImeActionIfRequired(): Boolean {
        val currentImeOptions = composeInput?.imeOptions
        val currentImeActionHandler = composeInput?.onImeActionPerformed
        val imeAction = currentImeOptions?.imeAction ?: return false
        val imeActionHandler = currentImeActionHandler ?: return false
        if (!imeActionRequired()) {
            return false
        }
        if (imeAction == ImeAction.Default) {
            imeActionHandler(ImeAction.Done)
        } else {
            imeActionHandler(imeAction)
        }
        return true
    }

    fun setInput(input: JSTextInputService.CurrentInput?) {
        composeInput = input
    }
}

