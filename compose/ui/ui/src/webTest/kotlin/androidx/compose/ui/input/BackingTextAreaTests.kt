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

import androidx.compose.ui.events.InputEvent
import androidx.compose.ui.events.InputEventInit
import androidx.compose.ui.events.keyEvent
import androidx.compose.ui.platform.BackingTextArea
import androidx.compose.ui.text.input.CommitTextCommand
import androidx.compose.ui.text.input.DeleteSurroundingTextInCodePointsCommand
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.SetComposingTextCommand
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.browser.document
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.events.CompositionEvent
import org.w3c.dom.events.CompositionEventInit

class BackingTextAreaTests {

    @Test
    fun disposeTest() {
        val backingTextArea = BackingTextArea(
            imeOptions = ImeOptions.Default,
            onEditCommand = {},
            onImeActionPerformed = {},
            processKeyboardEvent = {}
        )
        var textArea = document.querySelector("textarea")
        assertNull(textArea)

        backingTextArea.register()

        textArea = document.querySelector("textarea")
        assertIs<HTMLTextAreaElement>(textArea)

        backingTextArea.dispose()

        textArea = document.querySelector("textarea")
        assertNull(textArea)
    }

    @Test
    fun onProcessKeyboardEventTest() {
        val processedKeys = mutableListOf<String>()

        val backingTextArea = BackingTextArea(
            imeOptions = ImeOptions.Default,
            onEditCommand = {},
            onImeActionPerformed = {},
            processKeyboardEvent = { evt ->
                processedKeys.add(evt.key)
            }
        )

        backingTextArea.register()
        val textArea = document.querySelector("textarea")!!

        with (textArea) {
            dispatchEvent(keyEvent("H"))
            dispatchEvent(keyEvent("Unidentified"))
            dispatchEvent(keyEvent("E"))
            dispatchEvent(keyEvent("L"))
            dispatchEvent(keyEvent("Unidentified"))
            dispatchEvent(keyEvent("L"))
            dispatchEvent(keyEvent("O"))
        }

        assertEquals("H:E:L:L:O", processedKeys.joinToString(":"))
    }

    @Test
    fun onProcessComposeEventsTest() {
        var lastEditCommand: List<EditCommand> = listOf()

        val backingTextArea = BackingTextArea(
            imeOptions = ImeOptions.Default,
            onEditCommand = { command ->
                lastEditCommand = command
            },
            onImeActionPerformed = {},
            processKeyboardEvent = {}
        )

        backingTextArea.register()
        val textArea = document.querySelector("textarea")!!

        textArea.dispatchEvent(CompositionEvent("compositionstart"))

        assertEquals(
            emptyList(),
            lastEditCommand,
            "when compositionstart is triggered, last keyboard event should be ignored only if keyboard event happened"
        )

        textArea.dispatchEvent(keyEvent("Unidentified"))
        textArea.dispatchEvent(CompositionEvent("compositionstart"))

        assertEquals(
            listOf(),
            lastEditCommand,
            "when compositionstart is triggered, last keyboard event should be ignored only if meaningful keyboard event happened"
        )


        textArea.dispatchEvent(keyEvent("w"))
        textArea.dispatchEvent(CompositionEvent("compositionstart"))

        assertEquals(
            listOf(DeleteSurroundingTextInCodePointsCommand(1, 0)),
            lastEditCommand,
            "when compositionstart is triggered, last keyboard event should be ignored only if meaningful keyboard event happened"
        )

        textArea.dispatchEvent(CompositionEvent("compositionupdate", CompositionEventInit(data = "r")))
        assertEquals(
            listOf(SetComposingTextCommand("r", 1)),
            lastEditCommand,
            "each compositionupdate sends SetComposingTextCommand"
        )

        textArea.dispatchEvent(CompositionEvent("compositionend", CompositionEventInit(data = "问")))
        assertEquals(
            listOf(CommitTextCommand("问", 1)),
            lastEditCommand,
            "each compositionend send CommitTextCommand"
        )
    }

    @Test
    fun onEditCommandTest() {
        var lastEditCommand: List<EditCommand> = listOf()

        val backingTextArea = BackingTextArea(
            imeOptions = ImeOptions.Default,
            onEditCommand = { command ->
                lastEditCommand = command
            },
            onImeActionPerformed = {},
            processKeyboardEvent = {}
        )

        backingTextArea.register()
        val textArea = document.querySelector("textarea")!!

        textArea.dispatchEvent(InputEvent("input", InputEventInit(inputType = "insertText", data = "Bonjour")))

        assertEquals(listOf(CommitTextCommand("Bonjour", 1)), lastEditCommand)

        textArea.dispatchEvent(InputEvent("input", InputEventInit(inputType = "deleteContentBackward", data = "")))

        textArea.dispatchEvent(InputEvent("input", InputEventInit(inputType = "insertCompositionText", data = "Servus")))

        assertEquals(listOf(CommitTextCommand("Bonjour", 1)), lastEditCommand, "insertCompositionText should be ignored")

        textArea.dispatchEvent(CompositionEvent("compositionupdate", CompositionEventInit(data = "再见")))

        assertEquals(listOf(SetComposingTextCommand(text="再见", newCursorPosition=1)), lastEditCommand, "composition update should is not detected")
    }


    @Test
    fun onImeActionPerformedTest() {
        var lastPerformedImeAction: ImeAction? = null

        val backingTextArea = BackingTextArea(
            imeOptions = ImeOptions(
                singleLine = true,
                imeAction = ImeAction.Done,
            ),
            onImeActionPerformed = { action ->
                lastPerformedImeAction = action
            },
            onEditCommand = { },
            processKeyboardEvent = {}
        )

        backingTextArea.register()

        val textArea = document.querySelector("textarea")!!
        textArea.dispatchEvent(InputEvent("input", InputEventInit(inputType = "insertLineBreak", data = "")))

        assertEquals(lastPerformedImeAction, ImeAction.Done)
    }

}