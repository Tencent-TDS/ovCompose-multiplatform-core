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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.OnCanvasTests
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.sendFromScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import org.w3c.dom.events.WheelEvent
import org.w3c.dom.events.WheelEventInit

class ScrollTests : OnCanvasTests {
    @Test
    fun scrollTillEnd() = runTest {
        val firstRowScrollPositionResolved = Channel<Boolean>(
            1, onBufferOverflow = BufferOverflow.DROP_OLDEST
        )

        val lastRowScrollPositionResolved = Channel<Boolean>(
            1, onBufferOverflow = BufferOverflow.DROP_OLDEST
        )

        val composeWindow = createComposeWindow {
            // Setting the density to 2 so the test works correctly on all displays
            CompositionLocalProvider(LocalDensity provides Density(2f)) {
                Column(
                    modifier = Modifier.fillMaxWidth().height(300.dp)
                        .wrapContentSize(Alignment.Center)
                        .verticalScroll(
                            rememberScrollState()
                        )
                ) {
                    Box(
                        modifier = Modifier.height(100.dp).fillMaxWidth()
                            .background(Color(237, 40, 57))
                            .onGloballyPositioned { coordinates ->
                                val screenPosition = coordinates.positionOnScreen()
                                if (screenPosition == Offset(0f, -200f)) {
                                    firstRowScrollPositionResolved.sendFromScope(true)
                                }
                            }
                    )
                    Box(
                        modifier = Modifier.height(100.dp).fillMaxWidth()
                            .background(Color(255, 88, 0))
                    )
                    Box(
                        modifier = Modifier.height(100.dp).fillMaxWidth()
                            .background(Color(255, 211, 0))
                    )
                    Box(
                        modifier = Modifier.height(100.dp).fillMaxWidth()
                            .background(Color(0, 173, 131))
                    )
                    Box(
                        modifier = Modifier.height(100.dp).fillMaxWidth()
                            .background(Color(190, 219, 237))
                    )
                    Box(
                        modifier = Modifier.height(100.dp).fillMaxWidth()
                            .background(Color(31, 117, 254))
                    )
                    Box(
                        modifier = Modifier.height(100.dp).fillMaxWidth()
                            .background(Color(143, 0, 255)).onGloballyPositioned { coordinates ->
                            val screenPosition = coordinates.positionOnScreen()
                            if (screenPosition == Offset(0f, 1000f)) {
                                lastRowScrollPositionResolved.sendFromScope(true)
                            }
                        }
                    )
                }
            }
        }

        dispatchEvents(createWheelEvent(clientX = 100, clientY = 100, deltaX = 0.0, deltaY = 200.0))

        assertTrue(firstRowScrollPositionResolved.receive(), "first row scroll position is not resolved")
        assertTrue(lastRowScrollPositionResolved.receive(), "last row scroll position is not resolved")
    }
}


private fun createWheelEvent(
    deltaX: Double,
    deltaY: Double,
    clientX: Int,
    clientY: Int
): WheelEvent {
    return WheelEvent(
        "wheel", WheelEventInit(
            deltaX = deltaX,
            deltaY = deltaY,
            clientX = clientX,
            clientY = clientY
        )
    )
}
