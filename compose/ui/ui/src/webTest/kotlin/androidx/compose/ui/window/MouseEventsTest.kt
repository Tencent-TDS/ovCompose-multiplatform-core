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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.onClick
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.OnCanvasTests
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.events.MouseEventInit

class MouseEventsTest : OnCanvasTests {

    @Test
    fun createMouseEvent() = runTest {
        createComposeWindow {  }

        var offsetX = Double.MIN_VALUE
        var offsetY = Double.MIN_VALUE

        getCanvas().addEventListener("mouseenter", { event ->
            event as MouseEvent
            offsetX = event.offsetX
            offsetY = event.offsetY
        })

        dispatchEvents(MouseEvent("mouseenter", MouseEventInit(100, 100)))

        // We see that screenX/screenY are ignored
        assertEquals(0.0, offsetX)
        assertEquals(0.0, offsetY)


        dispatchEvents(MouseEvent("mouseenter", MouseEventInit(clientX = 100, clientY = 100)))

        // We see that clientX/clientY are not ignored
        assertEquals(100.0, offsetX)
        assertEquals(100.0, offsetY)
    }

    @Test
    fun testPointerEvents() = runTest {
        val pointerEvents = mutableListOf<PointerEvent>()

        createComposeWindow {
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

        dispatchEvents(
            MouseEvent("mouseenter", MouseEventInit(clientX = 100, clientY = 100)),
            MouseEvent("mousedown", MouseEventInit(clientX = 100, clientY = 100, button = 0, buttons = 1)),
            MouseEvent("mouseup", MouseEventInit(clientX = 100, clientY = 100, button = 0, buttons = 0))
        )

        assertEquals(3, pointerEvents.size)
        assertEquals(PointerEventType.Enter, pointerEvents[0].type)

        // Check for primary button
        assertEquals(PointerEventType.Press, pointerEvents[1].type)
        assertEquals(PointerButton.Primary, pointerEvents[1].button)
        assertEquals(PointerEventType.Release, pointerEvents[2].type)
        assertEquals(PointerButton.Primary, pointerEvents[2].button)

        dispatchEvents(
            MouseEvent("mousedown", MouseEventInit(clientX = 100, clientY = 100, button = 2, buttons = 2)),
            MouseEvent("mouseup", MouseEventInit(clientX = 100, clientY = 100, button = 2, buttons = 0))
        )

        assertEquals(5, pointerEvents.size)

        // Check for secondary button
        assertEquals(PointerEventType.Press, pointerEvents[3].type)
        assertEquals(PointerButton.Secondary, pointerEvents[3].button)
        assertEquals(PointerEventType.Release, pointerEvents[4].type)
        assertEquals(PointerButton.Secondary, pointerEvents[4].button)
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun testOnClickWithPointerMatchers() = runTest {
        var primaryClickedCounter = 0
        var secondaryClickedCounter = 0

        createComposeWindow {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onClick(matcher = PointerMatcher.Primary) { primaryClickedCounter++ }
                    .onClick(matcher = PointerMatcher.mouse(PointerButton.Secondary)) { secondaryClickedCounter++ }
            ) {}
        }

        dispatchEvents(
            MouseEvent("mouseenter", MouseEventInit(clientX = 100, clientY = 100)),
            MouseEvent("mousedown", MouseEventInit(clientX = 100, clientY = 100, button = 0, buttons = 1)),
            MouseEvent("mouseup", MouseEventInit(clientX = 100, clientY = 100, button = 0, buttons = 0))
        )

        assertEquals(1, primaryClickedCounter)
        assertEquals(0, secondaryClickedCounter)

        dispatchEvents(
            MouseEvent("mousedown", MouseEventInit(clientX = 100, clientY = 100, button = 2, buttons = 2)),
            MouseEvent("mouseup", MouseEventInit(clientX = 100, clientY = 100, button = 2, buttons = 0))
        )

        assertEquals(1, primaryClickedCounter)
        assertEquals(1, secondaryClickedCounter)
    }

    @Test
    fun testPointerButtonIsNullForNoClickEvents() = runTest {
        var event: PointerEvent? = null

        createComposeWindow {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (isActive) {
                                event = awaitPointerEvent()
                            }
                        }
                    }
            ) {}
        }

        assertEquals(null, event)

        dispatchEvents(MouseEvent("mouseenter", MouseEventInit(clientX = 100, clientY = 100)))
        assertEquals(PointerEventType.Enter, event!!.type)
        assertEquals(null, event!!.button)

        dispatchEvents(MouseEvent("mousemove", MouseEventInit(clientX = 101, clientY = 101)))
        assertEquals(PointerEventType.Move, event!!.type)
        assertEquals(null, event!!.button)

        dispatchEvents(MouseEvent("mouseleave", MouseEventInit(clientX = 0, clientY = 0)))
        assertEquals(PointerEventType.Exit, event!!.type)
        assertEquals(null, event!!.button)
    }
}