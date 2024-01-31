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

package androidx.compose.ui.platform

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.runApplicationTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.launch

@OptIn(ExperimentalTestApi::class)
class SceneRenderTest {
    // Test that we're draining the effect dispatcher completely before starting the draw phase
    @Test
    fun allEffectsRunBeforeDraw() = runApplicationTest {
        var flag by mutableStateOf(false)
        val events = mutableListOf<String>()
        launchTestApplication {
            Window(onCloseRequest = {}) {
                LaunchedEffect(flag) {
                    if (flag) {
                        launch {
                            launch {
                                launch {
                                    launch {
                                        launch {
                                            launch {
                                                events.add("effect")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Canvas(Modifier.size(100.dp)) {
                    if (flag) {
                        events.add("draw")
                    }
                }
            }
        }
        awaitIdle()

        flag = true
        awaitIdle()
        assertEquals(listOf("effect", "draw"), events)
    }

    // Test that effects are run before synthetic events are delivered.
    // This is important because the effects may be registering to receive the events. For example
    // when a new element appears under the mouse pointer and registers to mouse-enter events.
    @Test
    fun effectsRunBeforeSyntheticEvents() = runComposeUiTest {
        var flag by mutableStateOf(false)
        val events = mutableListOf<String>()

        setContent {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .testTag("container"),
                contentAlignment = Alignment.Center
            ) {
                if (flag) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .onPointerEvent(eventType = PointerEventType.Enter) {
                                events.add("mouse-enter")
                            }
                    )

                    LaunchedEffect(Unit) {
                        events.add("effect")
                    }
                }
            }
        }

        onNodeWithTag("container").performMouseInput {
            moveTo(Offset(50f, 50f))
        }
        flag = true
        waitForIdle()

        assertEquals(listOf("effect", "mouse-enter"), events)
    }
}
