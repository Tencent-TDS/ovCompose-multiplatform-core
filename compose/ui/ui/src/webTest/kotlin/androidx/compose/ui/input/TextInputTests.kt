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

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.OnCanvasTests
import androidx.compose.ui.events.InputEvent
import androidx.compose.ui.events.InputEventInit
import androidx.compose.ui.events.keyEvent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.sendFromScope
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.browser.document
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.events.CompositionEvent
import org.w3c.dom.events.CompositionEventInit
import org.w3c.dom.events.Event

private class InputInteractor(
    val composeChannel: Channel<String> = Channel<String>(
        1, onBufferOverflow = BufferOverflow.DROP_OLDEST
    ),
    val focusRequesters: List<FocusRequester> = listOf(FocusRequester(), FocusRequester())
) {
    fun currentHtmlInput() = document.querySelector("textarea") as HTMLTextAreaElement
    suspend fun waitForInput() = composeChannel.receive()
}

class TextInputTests : OnCanvasTests {
    // delay in web tests called directly will be completely ignored
    private suspend fun waitFor(millis: Long) {
        withContext(Dispatchers.Default) { delay(millis) }
    }

    private fun InputInteractor.sendToHtmlInput(vararg events: Event) {
        dispatchEvents(currentHtmlInput(), *events)
    }

    private suspend fun waitForHtmlInput(): HTMLTextAreaElement {
        val htmlChannel: Channel<HTMLTextAreaElement> = Channel<HTMLTextAreaElement>(
            1, onBufferOverflow = BufferOverflow.SUSPEND
        )

        withContext(Dispatchers.Default) {
            coroutineScope {
                while (true) {
                    val element = document.querySelector("textarea")
                    if (element is HTMLTextAreaElement) {
                        htmlChannel.sendFromScope(element)
                        return@coroutineScope
                    }
                    delay(100)
                }
            }
        }

        return htmlChannel.receive()
    }

    private suspend fun InputInteractor.createTextFieldWithChannel(
        content: @Composable () -> Unit = {
            val focusRequester = focusRequesters[0]

            val textState = remember {
                mutableStateOf("")
            }

            BasicTextField(
                value = textState.value,
                onValueChange = { value ->
                    textState.value = value
                    composeChannel.sendFromScope(value)
                },
                modifier = Modifier.focusRequester(focusRequester)
            )
        }
    ) {
        createComposeWindow(content = content)

        focusRequesters[0].requestFocus()
        waitForHtmlInput()
    }

    private suspend fun createTextFieldWithChannel(): InputInteractor {
        val inputInteractor = InputInteractor()
        inputInteractor.createTextFieldWithChannel()
        return inputInteractor
    }

    @Test
    fun regularInput() = runTest {
        val inputInteractor = createTextFieldWithChannel()

        inputInteractor.sendToHtmlInput(
            keyEvent("s"),
            keyEvent("t"),
            keyEvent("e"),
            keyEvent("p"),
            keyEvent("1")
        )

        assertEquals("step1", inputInteractor.waitForInput())

        inputInteractor.sendToHtmlInput(
            keyEvent("Backspace", code = "Backspace"),
            keyEvent("X"),
        )

        assertEquals(
            "stepX",
            inputInteractor.waitForInput(),
            "Backspace should delete last symbol typed"
        )
    }

    @Test
    fun compositeInput() = runTest {
        val inputInteractor = createTextFieldWithChannel()

        val backingTextField = document.querySelector("textarea")
        assertIs<HTMLTextAreaElement>(backingTextField)

        inputInteractor.sendToHtmlInput(
            keyEvent("a"),
            compositionStart(),
            beforeInput("insertCompositionText", "a"),
            keyEvent("a", type = "keyup", isComposing = true),
            keyEvent("1", code = "Digit1", isComposing = true),
            beforeInput("insertCompositionText", "啊"),
            compositionEnd("啊"),
            keyEvent("1", code = "Digit1", type = "keyup"),
        )

        assertEquals("啊", inputInteractor.waitForInput())

        inputInteractor.sendToHtmlInput(
            keyEvent("x"),
            keyEvent("x", type = "keyup")
        )

        assertEquals("啊x", inputInteractor.waitForInput())
    }

    @Test
    fun compositeInputWebkit() = runTest {
        val inputInteractor = createTextFieldWithChannel()

        val keyEvent = keyEvent("1", code = "Digit1")

        // We can not change timestamp for js events, so we just add some delay to enforce it
        waitFor(50)

        inputInteractor.sendToHtmlInput(
            compositionStart(),
            keyEvent("a", isComposing = true),
            keyEvent("a", type = "keyup", isComposing = true),
            beforeInput("deleteCompositionText", null),
            beforeInput("insertFromComposition", "啊"),
            compositionEnd("啊"),
            keyEvent,
            keyEvent("1", type = "keyup", code = "Digit1"),
        )

        assertEquals("啊", inputInteractor.waitForInput())

        // We can not change timestamp for js events, so we just add some delay to enforce it
        waitFor(100)

        inputInteractor.sendToHtmlInput(
            keyEvent("b"),
            keyEvent("b", type = "keyup")
        )

        assertEquals("啊b", inputInteractor.waitForInput())
    }

    @Test
    fun mobileInput() = runTest {
        val inputInteractor = createTextFieldWithChannel()

        inputInteractor.sendToHtmlInput(
            mobileKeyDown(),
            compositionStart(),
            beforeInput("insertCompositionText", "a"),
            mobileKeyUp(),
            mobileKeyDown(),
            beforeInput("insertCompositionText", "ab"),
            mobileKeyUp(),
            mobileKeyDown(),
            beforeInput("insertCompositionText", "abc"),
            mobileKeyUp()
        )

        assertEquals("abc", inputInteractor.waitForInput())
    }

    @Ignore
    @Test
    fun repeatedAccent() = runTest {
        val inputInteractor = createTextFieldWithChannel()

        inputInteractor.sendToHtmlInput(
            keyEvent("a"),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", type = "keyup"),
            keyEvent("b"),
            beforeInput("insertText", "b"),
            keyEvent("b", type = "keyup"),
            keyEvent("c"),
            beforeInput("insertText", "c"),
            keyEvent("c", type = "keyup")
        )

        // TODO: this does not behave as desktop, ideally we should have "abc" here
        assertEquals(
            "bc",
            inputInteractor.waitForInput(),
            "Repeat mode should be resolved as Accent Dialogue"
        )

        inputInteractor.sendToHtmlInput(
            keyEvent("a"),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", type = "keyup"),
            keyEvent("b"),
            beforeInput("insertText", "b"),
            keyEvent("b", type = "keyup"),
            keyEvent("c"),
            beforeInput("insertText", "c"),
            keyEvent("c", type = "keyup")
        )

    }

    @Test
    fun repeatedDefault() = runTest {
        val inputInteractor = createTextFieldWithChannel()

        inputInteractor.sendToHtmlInput(
            keyEvent("a"),
            keyEvent("a", repeat = true),
            beforeInput("insertText", "a"),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("b"),
            keyEvent("c")
        )

        assertTrue(
            Regex("a+bc").matches(inputInteractor.waitForInput()),
            "Repeat mode should be resolved as Default"
        )
    }

    @Test
    fun repeatedAccentMenuPressed() = runTest {
        val inputInteractor = createTextFieldWithChannel()

        inputInteractor.sendToHtmlInput(
            keyEvent("a"),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", type = "keyup"),
            keyEvent("1", code = "Digit1"),
            beforeInput(inputType = "insertText", data = "à"),
            keyEvent("1", code = "Digit1", type = "keyup"),
        )

        assertEquals("à", inputInteractor.waitForInput(), "Choose symbol from Accent Menu")
    }

    @Test
    fun repeatedAccentMenuIgnoreNonTyped() = runTest {
        val inputInteractor = createTextFieldWithChannel()

        inputInteractor.sendToHtmlInput(
            keyEvent("ArrowLeft", code = "ArrowLeft"),
            keyEvent("ArrowLeft", code = "ArrowLeft", repeat = true),
            keyEvent("ArrowLeft", code = "ArrowLeft", repeat = true),
            keyEvent("ArrowLeft", code = "ArrowLeft", repeat = true),
            keyEvent("ArrowLeft", code = "ArrowLeft", repeat = true),
            keyEvent("ArrowLeft", code = "ArrowLeft", repeat = true),
            keyEvent("ArrowLeft", code = "ArrowLeft", type = "keyup"),
            keyEvent("a"),
            keyEvent("a", type = "keyup"),
            keyEvent("b"),
            keyEvent("b", type = "keyup"),
            keyEvent("c"),
            keyEvent("c", type = "keyup"),
        )

        assertEquals("abc", inputInteractor.waitForInput(), "XXX")
    }

    fun repeatedAccentMenuClicked() = runTest {
        val inputInteractor = InputInteractor()
        inputInteractor.createTextFieldWithChannel()

        inputInteractor.sendToHtmlInput(
            keyEvent("a"),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", type = "keyup"),
            beforeInput(inputType = "insertText", data = "æ"),
        )

        assertEquals("æ", inputInteractor.waitForInput(), "Choose symbol from Accent Menu")
    }


    @Test
    fun keyboardEventPassedToTextField() = runTest {
        val inputInteractor = InputInteractor()

        inputInteractor.createTextFieldWithChannel {
            val textState1 = remember { mutableStateOf("") }
            val textState2 = remember { mutableStateOf("") }

            TextField(
                value = textState1.value,
                onValueChange = { value ->
                    textState1.value = value
                    inputInteractor.composeChannel.sendFromScope(value)
                },
                modifier = Modifier.focusRequester(inputInteractor.focusRequesters[0])
            )

            TextField(
                value = textState2.value,
                onValueChange = { value ->
                    textState2.value = value
                    inputInteractor.composeChannel.sendFromScope(value)
                },
                modifier = Modifier.focusRequester(inputInteractor.focusRequesters[1])
            )
        }

        inputInteractor.focusRequesters[0].requestFocus()

        inputInteractor.sendToHtmlInput(
            keyEvent("s"),
            keyEvent("t"),
            keyEvent("e"),
            keyEvent("p"),
            keyEvent("1")
        )

        assertEquals("step1", inputInteractor.waitForInput())

        inputInteractor.focusRequesters[1].requestFocus()
        waitForHtmlInput()

        inputInteractor.sendToHtmlInput(
            keyEvent("s"),
            keyEvent("t"),
            keyEvent("e"),
            keyEvent("p"),
            keyEvent("2")
        )

        assertEquals("step2", inputInteractor.waitForInput())
    }
}


private fun compositionStart(data: String = "") =
    CompositionEvent("compositionstart", CompositionEventInit(data = data))

private fun compositionEnd(data: String) =
    CompositionEvent("compositionend", CompositionEventInit(data = data))

private fun beforeInput(inputType: String, data: String?) =
    InputEvent("beforeinput", InputEventInit(inputType = inputType, data = data))

private fun mobileKeyDown() = keyEvent(type = "keydown", key = "Unidentified", code = "")
private fun mobileKeyUp() = keyEvent(type = "keydown", key = "Unidentified", code = "")