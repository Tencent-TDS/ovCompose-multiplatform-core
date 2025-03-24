/*
 * Copyright 2025 The Android Open Source Project
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

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.OnCanvasTests
import androidx.compose.ui.events.TouchEvent
import androidx.compose.ui.events.TouchEventInit
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.runTest
import org.w3c.dom.events.EventTarget

class GesturesTest : OnCanvasTests {

    @Test
    fun pan() = runTest {
        var currentDensity = Density(1f)

        val pans = mutableListOf<Offset>()

        createComposeWindow {
            currentDensity = LocalDensity.current
            Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                detectTransformGestures { _, pan, _, _ -> pans.add(pan) }
            })
        }

        dispatchEvents(
            TouchEvent("touchstart", touchEventInit(createTouch(0, getCanvas()))),
            // first move to exceed the touch slop
            TouchEvent("touchmove", touchEventInit(createTouch(0, getCanvas(), clientX = 10.0, clientY = 10.0))),
            TouchEvent("touchmove", touchEventInit(createTouch(0, getCanvas(), clientX = 10.0, clientY = 20.0))),
            TouchEvent("touchmove", touchEventInit(createTouch(0, getCanvas(), clientX = 20.0, clientY = 20.0))),
        )

        val actualPan = 10f * currentDensity.density
        assertEquals(2, pans.size)
        assertEquals(Offset(0f, actualPan), pans[0])
        assertEquals(Offset(actualPan, 0f), pans[1])
    }

    @Test
    fun zoomGestureTest() = runTest {
        val zooms = mutableListOf<Float>()

        createComposeWindow {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            zooms.add(zoom)
                        }
                    }
            )
        }

        dispatchEvents(
            // Simulate two touch points starting fairly close together
            TouchEvent(
                "touchstart",
                touchEventInit(
                    createTouch(0, getCanvas(), clientX = 50.0, clientY = 50.0),
                    createTouch(1, getCanvas(), clientX = 60.0, clientY = 50.0)
                )
            ),
            // first move to exceed the touch slop
            TouchEvent(
                "touchmove",
                touchEventInit(
                    createTouch(0, getCanvas(), clientX = 45.0, clientY = 60.0),
                    createTouch(1, getCanvas(), clientX = 65.0, clientY = 50.0)
                )
            ),
            // Zoom in, zoom > 1
            TouchEvent(
                "touchmove",
                touchEventInit(
                    createTouch(0, getCanvas(), clientX = 40.0, clientY = 50.0),
                    createTouch(1, getCanvas(), clientX = 70.0, clientY = 50.0)
                )
            ),
            TouchEvent(
                "touchmove",
                touchEventInit(
                    createTouch(0, getCanvas(), clientX = 30.0, clientY = 50.0),
                    createTouch(1, getCanvas(), clientX = 80.0, clientY = 50.0)
                )
            ),
            // and now zoom out, zoom < 1
            TouchEvent(
                "touchmove",
                touchEventInit(
                    createTouch(0, getCanvas(), clientX = 35.0, clientY = 50.0),
                    createTouch(1, getCanvas(), clientX = 75.0, clientY = 50.0)
                )
            ),
            TouchEvent(
                "touchmove",
                touchEventInit(
                    createTouch(0, getCanvas(), clientX = 37.0, clientY = 50.0),
                    createTouch(1, getCanvas(), clientX = 73.0, clientY = 50.0)
                )
            ),
        )

        // Verify that at least one zoom value greater than 1.0 was recorded.
        assertEquals(4, zooms.size)
        println(zooms.joinToString(","))
        assertTrue(zooms[0] > 1 && zooms[0] < zooms[1]) // according to the Offset change
        assertTrue(zooms[2] < 1 && zooms[2] < zooms[3]) // according to the Offset change
    }

    @Test
    // test that both TouchEvent.changedTouches and TouchEvent.targetTouches are handled
    fun canReceiveTouchEvents() = runTest {
        var lastPointerEvent: PointerEvent? = null

        createComposeWindow {
            Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                awaitPointerEventScope {
                    while (coroutineContext.isActive) {
                        lastPointerEvent = awaitPointerEvent()
                    }
                }
            })
        }

        assertEquals(null, lastPointerEvent)

        dispatchEvents(
            TouchEvent("touchstart", touchEventInit(createTouch(0, getCanvas(), clientX = 50.0, clientY = 50.0))),
            TouchEvent("touchmove", touchEventInit(createTouch(0, getCanvas(), clientX = 60.0, clientY = 60.0)))
        )

        assertNotEquals(null, lastPointerEvent)
        assertEquals(1, lastPointerEvent!!.changes.size)
        assertEquals(PointerEventType.Move, lastPointerEvent!!.type)
        lastPointerEvent = null

        dispatchEvents(
            TouchEvent("touchstart", touchEventWithTargetTouchesInit(createTouch(1, getCanvas(), clientX = 10.0, clientY = 10.0))),
            TouchEvent("touchmove", touchEventWithTargetTouchesInit(createTouch(1, getCanvas(), clientX = 20.0, clientY = 20.0)))
        )
        assertNotEquals(null, lastPointerEvent)
        assertEquals(1, lastPointerEvent!!.changes.size)
        assertEquals(PointerEventType.Move, lastPointerEvent!!.type)
    }

}

external interface Touch


private fun createTouch(
    identifier: Int = 0,
    target: EventTarget,
    clientX: Double = 0.0,
    clientY: Double = 0.0,
    pageX: Double = 0.0,
    pageY: Double = 0.0
): Touch = js(
    """
    new Touch({
        identifier: identifier,
        target: target,
        clientX: clientX,
        clientY: clientY,
        pageX: pageX,
        pageY: pageY
    })
    """
)

private fun touchEventInit(vararg touches: Touch): TouchEventInit = js(
    """
    ({
        bubbles: true,
        cancelable: true,
        composed: true,
        changedTouches: touches,
        targetTouches: [],
        touches: []
    })
    """
)

private fun touchEventWithTargetTouchesInit(vararg touches: Touch): TouchEventInit = js(
    """
    ({
        bubbles: true,
        cancelable: true,
        composed: true,
        changedTouches: [],
        targetTouches: touches,
        touches: []
    })
    """
)

