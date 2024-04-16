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

package androidx.compose.ui.window

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.browser.document
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.runTest
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.events.MouseEventInit

class MouseEventsTest {

    private val canvasId = "canvas1"

    @AfterTest
    fun cleanup() {
        document.getElementById(canvasId)?.remove()
    }

    @Test
    fun testPointerEvents() = runTest {
        if (isHeadlessBrowser()) return@runTest
        val canvasElement = document.createElement("canvas") as HTMLCanvasElement
        canvasElement.setAttribute("id", canvasId)
        document.body!!.appendChild(canvasElement)

        val pointerEvents = mutableListOf<PointerEvent>()

        CanvasBasedWindow(canvasElementId = canvasId) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (isActive) {
                                pointerEvents.add(awaitPointerEvent())
                            }
                        }
                    }
            ) {}
        }


        canvasElement.dispatchEvent(MouseEvent("mouseenter", MouseEventInit(100, 100)))
        canvasElement.dispatchEvent(MouseEvent("mousedown", MouseEventInit(100, 100, button = 0, buttons = 1)))
        canvasElement.dispatchEvent(MouseEvent("mouseup", MouseEventInit(100, 100, button = 0, buttons = 0)))

        assertEquals(3, pointerEvents.size)
        assertEquals(PointerEventType.Enter, pointerEvents[0].type)

        // Check for primary button
        assertEquals(PointerEventType.Press, pointerEvents[1].type)
        assertEquals(PointerButton.Primary, pointerEvents[1].button)
        assertEquals(PointerEventType.Release, pointerEvents[2].type)
        assertEquals(PointerButton.Primary, pointerEvents[2].button)

        canvasElement.dispatchEvent(MouseEvent("mousedown", MouseEventInit(100, 100, button = 2, buttons = 2)))
        canvasElement.dispatchEvent(MouseEvent("mouseup", MouseEventInit(100, 100, button = 2, buttons = 0)))
        assertEquals(5, pointerEvents.size)

        // Check for secondary button
        assertEquals(PointerEventType.Press, pointerEvents[3].type)
        assertEquals(PointerButton.Secondary, pointerEvents[3].button)
        assertEquals(PointerEventType.Release, pointerEvents[4].type)
        assertEquals(PointerButton.Secondary, pointerEvents[4].button)
    }
}