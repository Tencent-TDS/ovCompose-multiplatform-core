/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

@ExperimentalTestApi
class LayoutTest {

    @Test
    // Issue: https://github.com/JetBrains/compose-jb/issues/2696
    fun layoutInMovableContent() = runSkikoComposeUiTest {
        var lastLayoutOffset = -1
        var layoutCount = 0

        val layoutCallback: (Int) -> Unit = { newOffsetY ->
            lastLayoutOffset = newOffsetY
            layoutCount++
        }

        mainClock.autoAdvance = false // to manually control when layout happens

        var boxIx by mutableStateOf(0)
        val childOffsetY = mutableStateOf(0)

        setContent {
            val movableChildContent: @Composable () -> Unit = remember {
                movableContentOf { Sample(childOffsetY, layoutCallback) }
            }

            Column(modifier = Modifier.fillMaxSize()) {
                if (boxIx == 0) {
                    Box(modifier = Modifier.size(200.dp).background(color = Color.Gray)) {
                        movableChildContent()
                    }
                } else if (boxIx == 1) {
                    Box(modifier = Modifier.size(200.dp).background(color = Color.Magenta)) {
                       movableChildContent()
                    }
                }
            }
        }

        runOnIdle {
            assertEquals(1, layoutCount)
            assertEquals(0, lastLayoutOffset)
        }


        repeat(5) {
            childOffsetY.value += 10
            mainClock.advanceTimeByFrame()

            runOnIdle {
                assertEquals(2 + it, layoutCount)
                assertEquals(childOffsetY.value, lastLayoutOffset)
            }
        }

        boxIx = 1 // move the child content to another Box

        runOnIdle {
            assertEquals(7, layoutCount)
            assertEquals(50, lastLayoutOffset)
        }

        repeat(5) {
            childOffsetY.value += 10
            mainClock.advanceTimeByFrame()

            runOnIdle {
                assertEquals(8 + it, layoutCount)
                assertEquals(childOffsetY.value, lastLayoutOffset)
            }
        }
    }
}



@Composable
fun Sample(offsetYState: State<Int>, onLayoutCallback: (newOffsetY: Int) -> Unit) {
    Layout(
        content = { Text("Text") }
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints) }

        layout(constraints.maxWidth, constraints.maxHeight) {
            onLayoutCallback(offsetYState.value)
            placeables.forEach {
                it.placeRelative(x = 0, y = offsetYState.value)
            }
        }
    }
}