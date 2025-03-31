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

package androidx.compose.ui.input

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.TextField
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.OnCanvasTests
import androidx.compose.ui.events.InputEvent
import androidx.compose.ui.events.InputEventInit
import androidx.compose.ui.events.keyEvent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.sendFromScope
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.browser.document
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.events.MouseEventInit

class TextInputTests : OnCanvasTests  {

    @Test
    fun singleInput() = runTest {
        val textInputChannel = Channel<String>(
            1, onBufferOverflow = BufferOverflow.DROP_OLDEST
        )

        val (firstFocusRequester) = FocusRequester.createRefs()

        createComposeWindow {
            TextField(
                value = "",
                onValueChange = { value ->
                    textInputChannel.sendFromScope(value)
                },
                modifier = Modifier.focusRequester(firstFocusRequester)
            )

            SideEffect {
                firstFocusRequester.requestFocus()
            }
        }

        val backingTextField = document.querySelector("textarea")
        assertIs<HTMLTextAreaElement>(backingTextField)

        dispatchEvents(
            backingTextField,
            keyEvent("s"),
            keyEvent("t"),
            keyEvent("e"),
            keyEvent("p"),
            keyEvent("1")
        )

        assertEquals("step1", textInputChannel.receive())

        dispatchEvents(
            backingTextField,
            keyEvent("Backspace", code = "Backspace"),
            keyEvent("X"),
        )

        assertEquals("stepX", textInputChannel.receive(), "Backspace should delete last symbol typed")
    }

    @Test
    fun singleInputRepeatedAccent() = runTest {
        val textInputChannel = Channel<String>(
            1, onBufferOverflow = BufferOverflow.DROP_OLDEST
        )

        val (firstFocusRequester) = FocusRequester.createRefs()

        createComposeWindow {
            TextField(
                value = "",
                onValueChange = { value ->
                    textInputChannel.sendFromScope(value)
                },
                modifier = Modifier.focusRequester(firstFocusRequester)
            )

            SideEffect {
                firstFocusRequester.requestFocus()
            }
        }

        val backingTextField = document.querySelector("textarea")
        assertIs<HTMLTextAreaElement>(backingTextField)

        dispatchEvents(
            backingTextField,
            keyEvent("a"),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
        )

        assertEquals("a", textInputChannel.receive(), "Repeat mode should be resolved as Accent Dialogue")
    }

    @Test
    fun singleInputRepeatedDefault() = runTest {
        val textInputChannel = Channel<String>(
            1, onBufferOverflow = BufferOverflow.DROP_OLDEST
        )

        val (firstFocusRequester) = FocusRequester.createRefs()

        createComposeWindow {
            TextField(
                value = "",
                onValueChange = { value ->
                    textInputChannel.sendFromScope(value)
                },
                modifier = Modifier.focusRequester(firstFocusRequester)
            )

            SideEffect {
                firstFocusRequester.requestFocus()
            }
        }

        val backingTextField = document.querySelector("textarea")
        assertIs<HTMLTextAreaElement>(backingTextField)

        dispatchEvents(
            backingTextField,
            keyEvent("a"),
            keyEvent("a", repeat = true),
            InputEvent(type = "beforeinput", InputEventInit(data = "a", inputType = "insertText")),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
        )

        assertEquals("aaaaaa", textInputChannel.receive(), "Repeat mode should be resolved as Default")
    }


    @Test
    fun keyboardEventPassedToTextField() = runTest {

        val textInputChannel = Channel<String>(
            1, onBufferOverflow = BufferOverflow.DROP_OLDEST
        )

        val (firstFocusRequester, secondFocusRequester) = FocusRequester.createRefs()

        createComposeWindow {
            TextField(
                value = "",
                onValueChange = { value ->
                    textInputChannel.sendFromScope(value)
                },
                modifier = Modifier.focusRequester(firstFocusRequester)
            )

            TextField(
                value = "",
                onValueChange = { value ->
                    textInputChannel.sendFromScope(value)
                },
                modifier = Modifier.focusRequester(secondFocusRequester)
            )

            SideEffect {
                firstFocusRequester.requestFocus()
            }
        }

        val backingTextField = document.querySelector("textarea")
        assertIs<HTMLTextAreaElement>(backingTextField)

        dispatchEvents(
            backingTextField,
            keyEvent("s"),
            keyEvent("t"),
            keyEvent("e"),
            keyEvent("p"),
            keyEvent("1")
        )


        assertEquals("step1", textInputChannel.receive())

        secondFocusRequester.requestFocus()

        dispatchEvents(
            backingTextField,
            keyEvent("s"),
            keyEvent("t"),
            keyEvent("e"),
            keyEvent("p"),
            keyEvent("2")
        )

        assertEquals("step2", textInputChannel.receive())
    }

    @Test
    fun basicTextField2HasBackingHtmlTextAreaElement() = runTest {
        val focusRequester = FocusRequester()

        createComposeWindow {
            val textFieldState2 = remember { TextFieldState("I am TextField 2") }
            BasicTextField(
                textFieldState2,
                Modifier.padding(16.dp).fillMaxWidth().focusRequester(focusRequester)
            )
        }

        var textArea = document.querySelector("textarea")
        assertNull(textArea)

        focusRequester.requestFocus()

        yield()
        textArea = document.querySelector("textarea")
        assertIs<HTMLTextAreaElement>(textArea)
    }

    @Test
    fun canSelectUsingMouse() = runTest {
        val syncChannel = Channel<TextRange?>(
            1, onBufferOverflow = BufferOverflow.DROP_OLDEST
        )

        val focusRequester = FocusRequester()

        var textFieldWidth = 0

        createComposeWindow {
            val textState = remember { TextFieldState("qwerty 1234567") }

            CompositionLocalProvider(LocalDensity provides Density(2f)) {
                Column {
                    TextField(state = textState, modifier = Modifier.focusRequester(focusRequester).onGloballyPositioned {
                        textFieldWidth = it.size.width
                    })

                    LaunchedEffect(textState.selection) {
                        syncChannel.send(textState.selection)
                    }
                }
            }
        }

        focusRequester.requestFocus()
        yield()

        val textArea = document.querySelector("textarea")
        assertIs<HTMLTextAreaElement>(textArea)

        var selection = syncChannel.receive()
        assertEquals(TextRange(14, 14), selection)
        assertTrue(textFieldWidth > 0)

        val canvas = getCanvas()
        canvas.dispatchEvent(MouseEvent("mouseenter"))
        yield()
        canvas.dispatchEvent(MouseEvent("mousedown", MouseEventInit(clientX = 8, clientY = 20, buttons = 1, button = 1)))
        yield()
        canvas.dispatchEvent(MouseEvent("mouseup", MouseEventInit(clientX = 8, clientY = 20, buttons = 0, button = 1)))

        selection = syncChannel.receive()
        assertEquals(TextRange(0, 0), selection)

        val textAreaRect = textArea.getBoundingClientRect()
        // Do a manual hit-test
        val elementsAtPos = document.elementsFromPoint(
            textAreaRect.left + textAreaRect.width / 2 ,
            textAreaRect.top + textAreaRect.height / 2
        )

        // We expect the canvas to be on the top despite the coordinates match the textarea.
        // So it will be the first to process all the point inputs
        assertEquals(canvas, elementsAtPos[0], "First element under mouse supposed to be canvas")
        assertTrue(elementsAtPos.toList().any { it == textArea }) // such a weird check to make the test common for js and wasm
        withContext(Dispatchers.Default) {
            delay(250) // to separate the mouse events
        }


        // Try to select the text using mouse:
        val startX = textAreaRect.left.toInt() + 1
        val startY = textAreaRect.top.toInt() + 8
        val endX = startX + textFieldWidth
        canvas.dispatchEvent(MouseEvent("mousemove", MouseEventInit(clientX = startX, clientY = startY, buttons = 1, button = 1)))
        canvas.dispatchEvent(MouseEvent("mousedown", MouseEventInit(clientX = startX, clientY = startY, buttons = 1, button = 1)))
        canvas.dispatchEvent(MouseEvent("mousemove", MouseEventInit(clientX = endX, clientY = startY, buttons = 1, button = 1)))
        canvas.dispatchEvent(MouseEvent("mouseup", MouseEventInit(clientX = endX, clientY = startY, buttons = 0, button = 1)))

        selection = syncChannel.receive()
        assertEquals(TextRange(0, 14), selection)
    }
}