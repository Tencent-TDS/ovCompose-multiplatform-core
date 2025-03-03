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
import androidx.compose.ui.events.keyEvent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.sendFromScope
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.browser.document
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.events.MouseEventInit

class TextInputTests : OnCanvasTests  {

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
    fun canSelectAWordUsingMouse() = runTest {
        val syncChannel = Channel<TextRange?>(
            1, onBufferOverflow = BufferOverflow.DROP_OLDEST
        )

        createComposeWindow {
            val textState = remember { TextFieldState("qwerty 1234567") }

            CompositionLocalProvider(LocalDensity provides Density(2f)) {
                Column {
                    TextField(state = textState)

                    LaunchedEffect(textState.selection) {
                        syncChannel.send(textState.selection)
                    }
                }
            }
        }

        var selection = syncChannel.receive()
        assertEquals(TextRange(14, 14), selection)

        val canvas = getCanvas()
        canvas.dispatchEvent(MouseEvent("mouseenter"))
        canvas.dispatchEvent(MouseEvent("mousedown", MouseEventInit(clientX = 0, clientY = 8, buttons = 1, button = 1)))
        canvas.dispatchEvent(MouseEvent("mouseup", MouseEventInit(clientX = 56, clientY = 8, buttons = 0, button = 1)))

        selection = syncChannel.receive()
        assertEquals(TextRange(0, 6), selection)
    }
}