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

package androidx.compose.ui.scene

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest

class BaseComposeSceneTest {

    @Test
    fun `test move events consumption`() = runTest(StandardTestDispatcher()) {
        val scenes: List<ComposeScene> = listOf(
            PlatformLayersComposeScene(size = IntSize(100, 100)),
            CanvasLayersComposeScene(size = IntSize(100, 100))
        )

        scenes.forEach { scene ->
            var consumeAll = false
            scene.setContent {
                Box(modifier = Modifier.fillMaxSize().pointerInput(PointerEventPass.Initial) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (consumeAll) {
                                event.changes.forEach {
                                    if ((it.previousPosition - it.position) != Offset.Zero) it.consume()
                                }
                            }
                        }
                    }
                })
            }
            scene.sendPointerEvent(PointerEventType.Press, Offset(10f, 10f))
            assertFalse(
                scene.sendPointerEvent(PointerEventType.Move, Offset(11f, 10f))
                    .anyMovementConsumed
            )
            assertFalse(
                scene.sendPointerEvent(PointerEventType.Release, Offset(12f, 10f))
                    .anyMovementConsumed
            )

            consumeAll = true

            scene.sendPointerEvent(PointerEventType.Press, Offset(10f, 10f))
            assertTrue(
                scene.sendPointerEvent(PointerEventType.Move, Offset(11f, 10f))
                    .anyMovementConsumed
            )
            assertTrue(
                scene.sendPointerEvent(PointerEventType.Release, Offset(12f, 10f))
                    .anyMovementConsumed
            )
        }
    }
}
