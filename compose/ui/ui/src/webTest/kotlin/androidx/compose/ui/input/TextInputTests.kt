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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.OnCanvasTests
import androidx.compose.ui.WebApplicationScope
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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.events.CompositionEvent
import org.w3c.dom.events.CompositionEventInit
import org.w3c.dom.events.Event

private class InputInteractor(
    val textFieldValues: MutableList<MutableState<TextFieldValue>> = mutableListOf(),
    val focusRequesters: List<FocusRequester> = listOf(FocusRequester(), FocusRequester())
) {
    fun currentHtmlInput() = document.querySelector("textarea") as HTMLTextAreaElement
}

class TextInputTests : OnCanvasTests {
    private fun InputInteractor.sendToHtmlInput(vararg events: Event) {
        dispatchEvents(currentHtmlInput(), *events)
    }

    // delay in web tests called directly will be completely ignored
    private suspend fun waitFor(millis: Long) {
        withContext(Dispatchers.Default) { delay(millis) }
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

    private fun WebApplicationScope.assertTextFieldValue(expected: String, actual: MutableState<TextFieldValue>, message: String? = null) {
        assertEquals(expected = expected, actual = actual.value.text, message = message)
    }

    private fun WebApplicationScope.assertTextFieldValue(expected: String, actual: InputInteractor, message: String? = null) {
        assertEquals(expected = expected, actual.textFieldValues[0].value.text, message = message)
    }

    private suspend fun InputInteractor.createTextFieldWithChannel(
        content: @Composable () -> Unit = {
            val focusRequester = focusRequesters[0]

            val textState = remember {
                mutableStateOf(TextFieldValue())
            }

            this@createTextFieldWithChannel.textFieldValues.add(textState)

            BasicTextField(
                value = textState.value,
                onValueChange = { value ->
                    textState.value = value
                },
                modifier = Modifier.focusRequester(focusRequester)
            )
        }
    ) {
        createComposeWindow(content = content)

        focusRequesters[0].requestFocus()
        waitForHtmlInput()
    }

    private suspend fun WebApplicationScope.createTextFieldWithChannel(): InputInteractor {
        val inputInteractor = InputInteractor()
        inputInteractor.createTextFieldWithChannel()
        return inputInteractor
    }

    @Test
    fun regularInput() = runApplicationTest {
        val inputInteractor = createTextFieldWithChannel()

        inputInteractor.sendToHtmlInput(
            keyEvent("s"),
            keyEvent("t"),
            keyEvent("e"),
            keyEvent("p"),
            keyEvent("1")
        )

        awaitIdle()
        assertTextFieldValue("step1", inputInteractor.textFieldValues[0])

        inputInteractor.sendToHtmlInput(
            keyEvent("Backspace", code = "Backspace"),
            keyEvent("X"),
        )

        awaitIdle()
        assertTextFieldValue(
            "stepX",
            inputInteractor.textFieldValues[0],
            "Backspace should delete last symbol typed"
        )
    }

    @Test
    fun compositeInput() = runApplicationTest {
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

        awaitIdle()
        assertTextFieldValue("啊", inputInteractor.textFieldValues.first())

        inputInteractor.sendToHtmlInput(
            keyEvent("x"),
            keyEvent("x", type = "keyup")
        )

        awaitIdle()
        assertTextFieldValue("啊x", inputInteractor.textFieldValues.first())
    }

    @Test
    fun compositeInputWebkit() = runApplicationTest {
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

        awaitIdle()
        assertTextFieldValue("啊", inputInteractor)

        // We can not change timestamp for js events, so we just add some delay to enforce it
        waitFor(100)

        inputInteractor.sendToHtmlInput(
            keyEvent("b"),
            keyEvent("b", type = "keyup")
        )

        assertTextFieldValue("啊b", inputInteractor)
    }

    @Test
    fun mobileInput() = runApplicationTest {
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

        awaitIdle()
        assertTextFieldValue("abc", inputInteractor)
    }

    @Ignore
    @Test
    fun repeatedAccent() = runApplicationTest {
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
        assertTextFieldValue(
            "bc",
            inputInteractor,
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
    fun repeatedDefault() = runApplicationTest {
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
            Regex("a+bc").matches(inputInteractor.textFieldValues.first().value.text),
            "Repeat mode should be resolved as Default"
        )
    }

    @Test
    fun repeatedAccentMenuPressed() = runApplicationTest {
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

        assertTextFieldValue("à", inputInteractor, "Choose symbol from Accent Menu")
    }

    @Test
    fun repeatedAccentMenuIgnoreNonTyped() = runApplicationTest {
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

        assertTextFieldValue("abc", inputInteractor, "XXX")
    }

    fun repeatedAccentMenuClicked() = runApplicationTest {
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

        assertTextFieldValue("æ", inputInteractor, "Choose symbol from Accent Menu")
    }


    @Test
    fun keyboardEventPassedToTextField() = runApplicationTest {
        val inputInteractor = InputInteractor()

        inputInteractor.createTextFieldWithChannel {
            val textState1 = remember { mutableStateOf(TextFieldValue()) }
            val textState2 = remember { mutableStateOf(TextFieldValue()) }

            inputInteractor.textFieldValues.add(textState1)
            inputInteractor.textFieldValues.add(textState2)

            TextField(
                value = textState1.value,
                onValueChange = { value: TextFieldValue -> textState1.value = value},
                modifier = Modifier.focusRequester(inputInteractor.focusRequesters[0])
            )

            TextField(
                value = textState2.value,
                onValueChange = { value ->
                    textState2.value = value
                },
                modifier = Modifier.focusRequester(inputInteractor.focusRequesters[1])
            )
        }

        inputInteractor.focusRequesters[0].requestFocus()
        waitForHtmlInput()

        inputInteractor.sendToHtmlInput(
            keyEvent("s"),
            keyEvent("t"),
            keyEvent("e"),
            keyEvent("p"),
            keyEvent("1")
        )

        awaitIdle()
        assertTextFieldValue("step1", inputInteractor.textFieldValues[0])

        inputInteractor.focusRequesters[1].requestFocus()
        waitForHtmlInput()

        inputInteractor.sendToHtmlInput(
            keyEvent("s"),
            keyEvent("t"),
            keyEvent("e"),
            keyEvent("p"),
            keyEvent("2")
        )

        awaitIdle()
        assertTextFieldValue("step2", inputInteractor.textFieldValues[1])
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