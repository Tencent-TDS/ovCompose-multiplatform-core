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

package androidx.compose.mpp.demo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import org.jetbrains.skiko.currentNanoTime

@Composable
fun PointerInputDebug() {
    val events = remember { mutableStateListOf<Pair<Long, String>>() }

    Box(Modifier
        .fillMaxSize()
        .pointerInput(Unit) {
            coroutineScope {
                try {
                    awaitPointerEventScope {
                        while(isActive) {
                            val event = awaitPointerEvent()

                            val name = when (event.type) {
                                PointerEventType.Press -> "Press"
                                PointerEventType.Release -> "Release"
                                PointerEventType.Move -> "Move"
                                else -> "Unknown"
                            }

                            val time = currentNanoTime() / 1_000_000
                            val formatted = "$name ${event.changes.first().pressed} ${event.changes.first().id.value}"

                            if (events.lastOrNull()?.second != formatted) {
                                events.add(time to formatted)

                                println("pointerInput: $name ${event.changes.first().pressed} ${event.changes.first().id.value}")
                            }

                            val maxElements = 15
                            if (events.size > maxElements) {
                                events.removeRange(0, events.size - maxElements)
                            }
                        }
                    }
                } catch (e: CancellationException) {
                    if (!isActive) {
                        throw e
                    }
                }
            }
        }
    ) {
        Column(Modifier.fillMaxSize()) {
            for (event in events) {
                Text("${event.first} ${event.second}")
            }
        }
    }
}