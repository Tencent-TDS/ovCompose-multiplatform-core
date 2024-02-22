import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.TextField
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import kotlin.test.Test
import kotlinx.browser.document
import org.w3c.dom.HTMLCanvasElement
import androidx.compose.ui.window.*
import kotlin.test.AfterTest
import kotlin.test.assertEquals
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.w3c.dom.events.KeyboardEvent

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

class CanvasBasedWindowTests {

    private val canvasId = "canvas1"
    
    @AfterTest
    fun cleanup() {
        document.getElementById(canvasId)?.remove()
    }
    
    @Test
    fun canCreate() {
        val canvasElement = document.createElement("canvas") as HTMLCanvasElement
        canvasElement.setAttribute("id", canvasId)
        document.body!!.appendChild(canvasElement)
        CanvasBasedWindow(canvasElementId = canvasId) {  }
    }

    @Test
    fun testPreventDefault() = runTest {
        val canvasElement = document.createElement("canvas") as HTMLCanvasElement
        canvasElement.setAttribute("id", canvasId)
        document.body!!.appendChild(canvasElement)

        val fr = FocusRequester()
        var changedValue = ""
        CanvasBasedWindow(canvasElementId = canvasId) {
            TextField(
                value = "",
                onValueChange = { changedValue = it },
                modifier = Modifier.fillMaxSize().focusRequester(fr)
            )
            SideEffect {
                fr.requestFocus()
            }
        }

        val stack = mutableListOf<Boolean>()
        canvasElement.addEventListener("keydown", { event ->
            stack.add(event.defaultPrevented)
        })

        canvasElement.dispatchEvent(createCopyKeyboardEvent())
        delay(100)

        assertEquals(1, stack.size)
        assertEquals(true, stack.last())

        canvasElement.dispatchEvent(createTypedEvent())
        delay(100)

        assertEquals(2, stack.size)
        assertEquals(true, stack.last())
        assertEquals("c", changedValue)

        canvasElement.dispatchEvent(createEventShouldNotBePrevented())
        delay(100)

        assertEquals(3, stack.size)
        assertEquals(false, stack.last())
    }
}

private fun createCopyKeyboardEvent(): KeyboardEvent =
    js("new KeyboardEvent('keydown', {key: 'c', code: 'KeyC', keyCode: 67, ctrlKey: true, metaKey: true, cancelable: true})")

private fun createTypedEvent(): KeyboardEvent =
    js("new KeyboardEvent('keydown', {key: 'c', code: 'KeyC', keyCode: 67, cancelable: true})")

private fun createEventShouldNotBePrevented(): KeyboardEvent =
    js("new KeyboardEvent('keydown', {ctrlKey: true, cancelable: true})")