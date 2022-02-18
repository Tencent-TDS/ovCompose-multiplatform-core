/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.input.mouse

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.use
import androidx.compose.ui.window.density
import androidx.compose.ui.window.runApplicationTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.awt.Dimension
import kotlin.random.Random

@OptIn(ExperimentalComposeUiApi::class)
@RunWith(JUnit4::class)
class MouseHoverFilterTest {
    @Test
    fun `inside window`() = ImageComposeScene(
        width = 100,
        height = 100,
        density = Density(2f)
    ).use { scene ->
        var moveCount = 0
        var enterCount = 0
        var exitCount = 0

        scene.setContent {
            Box(
                modifier = Modifier
                    .pointerMove(
                        onMove = {
                            moveCount++
                        },
                        onEnter = {
                            enterCount++
                        },
                        onExit = {
                            exitCount++
                        }
                    )
                    .size(10.dp, 20.dp)
            )
        }

        scene.sendPointerEvent(PointerEventType.Enter, Offset(0f, 0f))
        scene.sendPointerEvent(PointerEventType.Move, Offset(10f, 20f))
        assertThat(enterCount).isEqualTo(1)
        assertThat(exitCount).isEqualTo(0)
        assertThat(moveCount).isEqualTo(1)

        scene.sendPointerEvent(PointerEventType.Move, Offset(10f, 15f))
        assertThat(enterCount).isEqualTo(1)
        assertThat(exitCount).isEqualTo(0)
        assertThat(moveCount).isEqualTo(2)

        scene.sendPointerEvent(PointerEventType.Move, Offset(30f, 30f))
        assertThat(enterCount).isEqualTo(1)
        assertThat(exitCount).isEqualTo(1)
        assertThat(moveCount).isEqualTo(2)
    }

    @Test
    fun `window enter`() = ImageComposeScene(
        width = 100,
        height = 100,
        density = Density(2f)
    ).use { scene ->
        var moveCount = 0
        var enterCount = 0
        var exitCount = 0

        scene.setContent {
            Box(
                modifier = Modifier
                    .pointerMove(
                        onMove = {
                            moveCount++
                        },
                        onEnter = {
                            enterCount++
                        },
                        onExit = {
                            exitCount++
                        }
                    )
                    .size(10.dp, 20.dp)
            )
        }

        scene.sendPointerEvent(PointerEventType.Enter, Offset(10f, 20f))
        assertThat(enterCount).isEqualTo(1)
        assertThat(exitCount).isEqualTo(0)
        assertThat(moveCount).isEqualTo(0)

        scene.sendPointerEvent(PointerEventType.Exit, Offset(-1f, -1f))
        assertThat(enterCount).isEqualTo(1)
        assertThat(exitCount).isEqualTo(1)
        assertThat(moveCount).isEqualTo(0)
    }

    @Test
    fun `move from one component to another`() = ImageComposeScene(
        width = 100,
        height = 100,
        density = Density(1f)
    ).use { scene ->
        var enterCount1 = 0
        var exitCount1 = 0
        var enterCount2 = 0
        var exitCount2 = 0

        scene.setContent {
            Column {
                Box(
                    modifier = Modifier
                        .pointerMove(
                            onEnter = { enterCount1++ },
                            onExit = { exitCount1++ }
                        )
                        .size(10.dp, 10.dp)
                )
                Box(
                    modifier = Modifier
                        .pointerMove(
                            onEnter = { enterCount2++ },
                            onExit = { exitCount2++ }
                        )
                        .size(10.dp, 10.dp)
                )
            }
        }

        scene.sendPointerEvent(PointerEventType.Enter, Offset(5f, 5f))
        assertThat(enterCount1).isEqualTo(1)
        assertThat(exitCount1).isEqualTo(0)
        assertThat(enterCount2).isEqualTo(0)
        assertThat(exitCount2).isEqualTo(0)

        scene.sendPointerEvent(PointerEventType.Move, Offset(5f, 15f))
        assertThat(enterCount1).isEqualTo(1)
        assertThat(exitCount1).isEqualTo(1)
        assertThat(enterCount2).isEqualTo(1)
        assertThat(exitCount2).isEqualTo(0)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `send multiple move events with paused dispatcher`() {
        var moveCount = 0
        var enterCount = 0
        var exitCount = 0

        val dispatcher = TestCoroutineDispatcher().apply {
            pauseDispatcher()
        }

        ImageComposeScene(
            width = 100,
            height = 100,
            density = Density(1f),
            coroutineContext = dispatcher
        ).use { scene ->
            scene.setContent {
                Box(
                    modifier = Modifier
                        .pointerMove(
                            onMove = {
                                moveCount++
                            },
                            onEnter = {
                                enterCount++
                            },
                            onExit = {
                                exitCount++
                            }
                        )
                        .size(20.dp, 20.dp)
                )
            }
            dispatcher.advanceUntilIdle()

            scene.sendPointerEvent(PointerEventType.Enter, Offset(5f, 10f))
            scene.sendPointerEvent(PointerEventType.Move, Offset(6f, 10f))
            scene.sendPointerEvent(PointerEventType.Move, Offset(7f, 10f))
            scene.sendPointerEvent(PointerEventType.Press, Offset(7f, 10f))
            dispatcher.advanceUntilIdle()

            assertThat(enterCount).isEqualTo(1)
            assertThat(exitCount).isEqualTo(0)
            assertThat(moveCount).isEqualTo(2)

            scene.sendPointerEvent(PointerEventType.Move, Offset(100f, 10f))
            dispatcher.advanceUntilIdle()

            assertThat(enterCount).isEqualTo(1)
            assertThat(exitCount).isEqualTo(1)
            assertThat(moveCount).isEqualTo(2)
        }
    }

    @Test
    fun `hover on scroll`() = ImageComposeScene(
        width = 100,
        height = 100,
        density = Density(1f)
    ).use { scene ->
        val boxes = (1..6).map { HoverBox() }

        scene.setContent {
            val state = rememberScrollState()
            Column(
                Modifier
                    .requiredSize(20.dp, 30.dp)
                    .onPointerEvent(PointerEventType.Scroll) {
                        runBlocking {
                            state.scrollBy(it.changes.first().scrollDelta.y * size.height.toFloat())
                        }
                    }
                    .verticalScroll(state, enabled = false)
            ) {
                boxes.forEachIndexed { index, box ->
                    key(System.identityHashCode(box)) {
                        Box(
                            modifier = Modifier
                                .onPointerEvent(PointerEventType.Enter) { box.onEnter() }
                                .onPointerEvent(PointerEventType.Exit) { box.onExit() }
                                .size(20.dp, 10.dp)
                        )
                    }
                }
            }
        }

        scene.sendPointerEvent(PointerEventType.Enter, Offset(0f, 0f))
        assertThat(boxes.map { it.isHovered }).isEqualTo(listOf(true, false, false, false, false, false))

        scene.sendPointerEvent(PointerEventType.Scroll, Offset(15f, 15f), scrollDelta = Offset(0f, 3.0f / 3.0f))
        scene.render() // we update hover state only during next render
        assertThat(boxes.map { it.isHovered }).isEqualTo(listOf(false, false, false, false, true, false))

        scene.sendPointerEvent(PointerEventType.Scroll, Offset(15f, 15f), scrollDelta = Offset(0f, -3.0f / 3.0f))
        scene.render()
        assertThat(boxes.map { it.isHovered }).isEqualTo(listOf(false, true, false, false, false, false))

        scene.sendPointerEvent(PointerEventType.Scroll, Offset(15f, 15f), scrollDelta = Offset(0f, 2.0f / 3.0f))
        scene.render()
        assertThat(boxes.map { it.isHovered }).isEqualTo(listOf(false, false, false, true, false, false))

        scene.sendPointerEvent(PointerEventType.Exit, Offset(-1f, -1f))
        assertThat(boxes.map { it.isHovered }).isEqualTo(listOf(false, false, false, false, false, false))
    }

    @Test
    fun `hover on random moves`() = ImageComposeScene(
        width = 100,
        height = 100,
        density = Density(1f)
    ).use { scene ->
        repeat(200) {
            val boxes = (1..4).map { HoverBox.random(100) }

            scene.setContent {
                for (box in boxes) {
                    key(box) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .offset { box.position }
                                .wrapContentSize(align = Alignment.TopStart, unbounded = true)
                        ) {
                            Box(
                                Modifier
                                    .requiredSize(with(LocalDensity.current) { box.size.toDp() })
                                    .background(Color.Red)
                                    .border(1.dp, Color.Green)
                                    .onPointerEvent(PointerEventType.Enter) { box.onEnter() }
                                    .onPointerEvent(PointerEventType.Exit) { box.onExit() }
                            )
                        }
                    }
                }
            }

            val randomEvents = RandomEvents(scene)
            repeat(10) {
                repeat(Random.nextInt(0, 4)) {
                    randomEvents.randomMove()
                }
                fun isHoveredByMouse(box: HoverBox) =
                    box.rect.contains(randomEvents.position) && randomEvents.isInsideScene
                val hoveredBoxes = listOfNotNull(boxes.findLast(::isHoveredByMouse))
                val notHoveredBoxes = boxes - hoveredBoxes

                assertThat(hoveredBoxes.all { it.isHovered }).isTrue()
                assertThat(notHoveredBoxes.all { !it.isHovered }).isTrue()
            }
        }
    }

    @Test
    fun `hover on random moves and scrolls`() = ImageComposeScene(
        width = 100,
        height = 100,
        density = Density(1f)
    ).use { scene ->
        val windowSize = scene.constraints.maxWidth
        var scrollOffset by mutableStateOf(IntOffset.Zero)

        repeat(100) {
            val boxes = (1..4).map { HoverBox.random(windowSize) }

            scene.setContent {
                Box(
                    Modifier.fillMaxSize().onPointerEvent(PointerEventType.Scroll) {
                        val pxPerScroll = windowSize / 4f
                        val delta = (it.changes.first().scrollDelta * pxPerScroll).round()
                        scrollOffset = (scrollOffset + delta).coerceIn(-windowSize, windowSize)
                    }
                ) {
                    for (box in boxes) {
                        key(box) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .offset { box.position + scrollOffset }
                                    .wrapContentSize(align = Alignment.TopStart, unbounded = true)
                            ) {
                                Box(
                                    Modifier
                                        .requiredSize(with(LocalDensity.current) { box.size.toDp() })
                                        .background(Color.Red)
                                        .border(1.dp, Color.Green)
                                        .onPointerEvent(PointerEventType.Enter) { box.onEnter() }
                                        .onPointerEvent(PointerEventType.Exit) { box.onExit() }
                                )
                            }
                        }
                    }
                }
            }

            val randomEvents = RandomEvents(scene)
            repeat(10) {
                repeat(Random.nextInt(0, 4)) {
                    if (Random.nextBoolean()) {
                        randomEvents.randomMove()
                    } else {
                        randomEvents.randomScroll(maxCount = 4f)
                    }
                }
                scene.render()

                fun isHoveredByMouse(box: HoverBox) =
                    box.rect.translate(scrollOffset).contains(randomEvents.position) && randomEvents.isInsideScene
                val hoveredBoxes = listOfNotNull(boxes.findLast(::isHoveredByMouse))
                val notHoveredBoxes = boxes - hoveredBoxes

                assertThat(hoveredBoxes.all { it.isHovered }).isTrue()
                assertThat(notHoveredBoxes.all { !it.isHovered }).isTrue()
            }
        }
    }
}

private data class HoverBox(val size: Int = 0, val x: Int = 0, val y: Int = 0) {
    var isHovered by mutableStateOf(false)
        private set

    val position = IntOffset(x, y)
    val rect = IntRect(x, y, x + size, y + size)

    fun onEnter() {
        check(!isHovered) { "HoverBox is already hovered" }
        isHovered = true
    }

    fun onExit() {
        check(isHovered) { "HoverBox isn't hovered" }
        isHovered = false
    }

    companion object {
        fun random(windowSize: Int) = centered(
            size = (1 + windowSize * Random.nextFloat() * 2f).toInt(),
            centerX = (windowSize * Random.nextFloat()).toInt(),
            centerY = (windowSize * Random.nextFloat()).toInt()
        )

        fun centered(size: Int, centerX: Int, centerY: Int) =
            HoverBox(size, centerX - size / 2, centerY - size / 2)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private class RandomEvents(val scene: ImageComposeScene) {
    var isInsideScene = false
    var position = Offset(-1f, -1f)

    fun randomMove() {
        val width = scene.constraints.maxWidth
        val height = scene.constraints.maxHeight
        val sceneRect = Rect(0f, 0f, width.toFloat(), height.toFloat())
        position = Offset(
            Random.nextInt(-10, width + 10).toFloat(),
            Random.nextInt(-10, height + 10).toFloat(),
        )

        val isInsideWindow = sceneRect.contains(position)
        scene.sendPointerEvent(
            eventType = when {
                isInsideWindow && !this.isInsideScene -> PointerEventType.Enter
                !isInsideWindow && this.isInsideScene -> PointerEventType.Exit
                else -> PointerEventType.Move
            },
            position = position
        )
        this.isInsideScene = isInsideWindow
    }

    fun randomScroll(maxCount: Float) {
        val wheelRotation = Random.nextFloat(-maxCount, maxCount)
        scene.sendPointerEvent(PointerEventType.Scroll, position = position, scrollDelta = Offset(0f, wheelRotation))
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private fun Modifier.pointerMove(
    onMove: () -> Unit = {},
    onExit: () -> Unit = {},
    onEnter: () -> Unit = {},
): Modifier = this
    .onPointerEvent(PointerEventType.Move) { onMove() }
    .onPointerEvent(PointerEventType.Exit) { onExit() }
    .onPointerEvent(PointerEventType.Enter) { onEnter() }

private operator fun IntRect.contains(offset: Offset): Boolean {
    return offset.x >= left && offset.x < right && offset.y >= top && offset.y < bottom
}

private operator fun DpOffset.times(other: Float) = DpOffset(x * other, y * other)

private fun IntOffset.coerceIn(min: Int, max: Int) =
    IntOffset(x.coerceIn(min, max), y.coerceIn(min, max))

private fun Random.Default.nextFloat(from: Float, until: Float): Float = from + (until - from) * Random.nextFloat()