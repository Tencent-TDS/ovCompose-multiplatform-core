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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.OnCanvasTests
import androidx.compose.ui.events.InputEvent
import androidx.compose.ui.events.InputEventInit
import androidx.compose.ui.events.keyEvent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.browser.document
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.events.CompositionEvent
import org.w3c.dom.events.CompositionEventInit
import org.w3c.dom.events.Event


class TextInputTests : OnCanvasTests {

    private fun currentHtmlInput() = document.querySelector("textarea") as HTMLTextAreaElement

    private fun sendToHtmlInput(vararg events: Event) {
        dispatchEvents(currentHtmlInput(), *events)
    }

    private suspend fun waitForHtmlInput(): HTMLTextAreaElement {
        while (true) {
            val element = document.querySelector("textarea")
            if (element is HTMLTextAreaElement) {
                return element
            }
            yield()
        }
    }

    private class TextFieldTestScope {
        private val textFieldValueState = mutableStateOf(TextFieldValue())

        val focusRequester: FocusRequester = FocusRequester()

        private var textFieldValueChanged = false

        var textFieldValue: TextFieldValue
            get() = textFieldValueState.value
            set(value) {
                textFieldValueState.value = value
                textFieldValueChanged = true
            }

        var textFieldText: String
            get() = textFieldValue.text
            set(value) {
                textFieldValue = textFieldValue.copy(text = value)
            }

        suspend fun awaitTextFieldIdle() {
            do {
                textFieldValueChanged = false
                waitFor(16)
            } while (textFieldValueChanged)
        }

        fun assertTextEquals(expected: String, message: String? = null) {
            assertEquals(expected, textFieldText, message)
        }

    }

    private fun runTextFieldTest(
        testBlock: suspend TextFieldTestScope.() -> Unit
    ) = runTest {
        with(TextFieldTestScope()) {
            createComposeWindow {
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { textFieldValue = it },
                    modifier = Modifier.focusRequester(focusRequester)
                )
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
            }
            waitForHtmlInput()
            testBlock()
        }
    }

    @Test
    fun regularInput() = runTextFieldTest {
        sendToHtmlInput(
            keyEvent("s"),
            keyEvent("t"),
            keyEvent("e"),
            keyEvent("p"),
            keyEvent("1")
        )

        assertTextEquals("step1")

        awaitTextFieldIdle()
        sendToHtmlInput(
            keyEvent("Backspace", code = "Backspace"),
            keyEvent("X"),
        )

        assertTextEquals(
            expected = "stepX",
            message = "Backspace should delete last symbol typed"
        )
    }

    @Test
    fun compositeInput() = runTextFieldTest {
        val backingTextField = document.querySelector("textarea")
        assertIs<HTMLTextAreaElement>(backingTextField)

        sendToHtmlInput(
            keyEvent("a"),
            compositionStart(),
            beforeInput("insertCompositionText", "a"),
            keyEvent("a", type = "keyup", isComposing = true),
            keyEvent("1", code = "Digit1", isComposing = true),
            beforeInput("insertCompositionText", "啊"),
            compositionEnd("啊"),
            keyEvent("1", code = "Digit1", type = "keyup"),
        )

        assertTextEquals("啊")

        sendToHtmlInput(
            keyEvent("x"),
            keyEvent("x", type = "keyup")
        )

        assertTextEquals("啊x")
    }

    @Test
    fun compositeInputWebkit() = runTextFieldTest {
        val keyEvent = keyEvent("1", code = "Digit1")

        // We can not change timestamp for js events, so we just add some delay to enforce it
        waitFor(50)

        sendToHtmlInput(
            compositionStart(),
            keyEvent("a", isComposing = true),
            keyEvent("a", type = "keyup", isComposing = true),
            beforeInput("deleteCompositionText", null),
            beforeInput("insertFromComposition", "啊"),
            compositionEnd("啊"),
            keyEvent,
            keyEvent("1", type = "keyup", code = "Digit1"),
        )

        assertTextEquals("啊")

        // We can not change timestamp for js events, so we just add some delay to enforce it
        waitFor(100)

        sendToHtmlInput(
            keyEvent("b"),
            keyEvent("b", type = "keyup")
        )

        assertTextEquals("啊b")
    }

    @Test
    fun mobileInput() = runTextFieldTest {
        sendToHtmlInput(
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

        assertTextEquals("abc")
    }

    @Ignore
    @Test
    fun repeatedAccent() = runTextFieldTest {
        sendToHtmlInput(
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
        assertTextEquals(
            expected = "bc",
            message = "Repeat mode should be resolved as Accent Dialogue"
        )

        sendToHtmlInput(
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
    fun repeatedDefault() = runTextFieldTest {
        sendToHtmlInput(
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
            Regex("a+bc").matches(textFieldValue.text),
            "Repeat mode should be resolved as Default"
        )
    }

    @Test
    fun repeatedAccentMenuPressed() = runTextFieldTest {
        sendToHtmlInput(
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

        assertTextEquals("à", message = "Choose symbol from Accent Menu")
    }

    @Test
    fun repeatedAccentMenuIgnoreNonTyped() = runTextFieldTest {
        sendToHtmlInput(
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

        assertTextEquals("abc", message =  "XXX")
    }

    @Test
    fun repeatedAccentMenuClicked() = runTextFieldTest {
        sendToHtmlInput(
            keyEvent("a"),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", type = "keyup"),
            beforeInput(inputType = "insertText", data = "æ"),
        )

        assertTextEquals("æ", message = "Choose symbol from Accent Menu")
    }


    @Test
    fun keyboardEventPassedToTextField() = runTest {
        var textFieldValue1 by mutableStateOf(TextFieldValue())
        var textFieldValue2 by mutableStateOf(TextFieldValue())

        val focusRequester1 = FocusRequester()
        val focusRequester2 = FocusRequester()

        createComposeWindow {
            TextField(
                value = textFieldValue1,
                onValueChange = { textFieldValue1 = it },
                modifier = Modifier.focusRequester(focusRequester1)
            )

            TextField(
                value = textFieldValue2,
                onValueChange = { textFieldValue2 = it },
                modifier = Modifier.focusRequester(focusRequester2)
            )
        }

        println("waitForHtmlInput1")
        focusRequester1.requestFocus()
        waitForHtmlInput()

        sendToHtmlInput(
            keyEvent("s"),
            keyEvent("t"),
            keyEvent("e"),
            keyEvent("p"),
            keyEvent("1")
        )

        assertEquals("step1", textFieldValue1.text)

        focusRequester2.requestFocus()
        waitForHtmlInput()

        sendToHtmlInput(
            keyEvent("s"),
            keyEvent("t"),
            keyEvent("e"),
            keyEvent("p"),
            keyEvent("2")
        )

        assertEquals("step2", textFieldValue2.text)
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

// delay in web tests called directly will be completely ignored
private suspend fun waitFor(millis: Long) {
    withContext(Dispatchers.Default) { delay(millis) }
}
