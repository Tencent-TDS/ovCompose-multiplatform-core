/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.RootMeasurePolicy.measure
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.renderingTest
import androidx.compose.ui.test.InternalTestApi
import androidx.compose.ui.test.junit4.DesktopScreenshotTestRule
import androidx.compose.ui.test.junit4.ScreenshotTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyPress
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.runApplicationTest
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Surface
import org.junit.Assert.assertFalse
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

@OptIn(InternalTestApi::class, ExperimentalComposeUiApi::class)
class ComposeSceneTest {
    @get:Rule
    val screenshotRule = DesktopScreenshotTestRule("compose/ui/ui-desktop")

    @get:Rule
    val composeRule = createComposeRule()

    private fun ScreenshotTestRule.snap(surface: Surface, idSuffix: String? = null) {
        assertImageAgainstGolden(surface.makeImageSnapshot(), idSuffix)
    }

    @Test(timeout = 5000)
    fun `rendering of Box state change`() = renderingTest(width = 40, height = 40) {
        var size by mutableStateOf(20.dp)
        setContent {
            Box(Modifier.size(size).background(Color.Blue))
        }
        awaitNextRender()
        screenshotRule.snap(surface, "frame1_initial_size_20")
        assertFalse(hasRenders())

        size = 10.dp
        awaitNextRender()
        screenshotRule.snap(surface, "frame2_change_size_to_10")
        assertFalse(hasRenders())

        size = 5.dp
        awaitNextRender()
        screenshotRule.snap(surface, "frame3_change_size_to_5")
        assertFalse(hasRenders())

        size = 10.dp
        size = 20.dp
        awaitNextRender()
        screenshotRule.snap(surface, "frame4_change_size_to_10_and_20")
        assertFalse(hasRenders())

        size = 20.dp
        assertFalse(hasRenders())
    }

    @Test(timeout = 5000)
    fun `rendering of Canvas state change`() = renderingTest(width = 40, height = 40) {
        var x by mutableStateOf(0f)
        var clipToBounds by mutableStateOf(false)
        setContent {
            val modifier = if (clipToBounds) {
                Modifier.size(20.dp).clipToBounds()
            } else {
                Modifier.size(20.dp)
            }
            Canvas(modifier) {
                drawRect(
                    color = Color.Red,
                    topLeft = Offset(x, 0f),
                    size = Size(10f, 10f)
                )
            }
        }

        awaitNextRender()
        screenshotRule.snap(surface, "frame1_initial")
        assertFalse(hasRenders())

        x = 15f
        awaitNextRender()
        screenshotRule.snap(surface, "frame2_translate")
        assertFalse(hasRenders())

        clipToBounds = true
        awaitNextRender()
        screenshotRule.snap(surface, "frame3_clipToBounds")
        assertFalse(hasRenders())
    }

    // https://github.com/JetBrains/compose-multiplatform/issues/3137
    @Test
    fun `rendering of Text state change`() = renderingTest(width = 400, height = 200) {
        var text by mutableStateOf("before")
        setContent {
            Text(text)
        }
        awaitNextRender()
        val before = surface.makeImageSnapshot().toComposeImageBitmap().toPixelMap().buffer

        text = "after"
        awaitNextRender()
        val after = surface.makeImageSnapshot().toComposeImageBitmap().toPixelMap().buffer
        assertThat(after).isNotEqualTo(before)
    }

    // Verify that when snapshot state changes in one of the render phases, the change is applied
    // before the next phase, and is visible there.
    // Note that it tests the same thing as `rendering of Text state change` is trying to, but at a
    // lower-level, and without depending on the implementation of Text.
    @Suppress("UNUSED_EXPRESSION")
    @Test
    fun stateChangesAppliedBetweenRenderPhases() = renderingTest(width = 400, height = 200) {
        var value by mutableStateOf(0)
        var compositionCount = 0
        var layoutCount = 0
        var drawCount = 0

        var layoutScopeInvalidation by mutableStateOf(Unit, neverEqualPolicy())
        var drawScopeInvalidation by mutableStateOf(Unit, neverEqualPolicy())

        setContent {
            value
            compositionCount += 1
            layoutScopeInvalidation = Unit
            Layout(
                modifier = remember {
                    Modifier.graphicsLayer().drawBehind {
                        drawScopeInvalidation
                        drawCount += 1
                    }
                },
                measurePolicy = remember {
                    MeasurePolicy { measurables, constraints ->
                        layoutScopeInvalidation
                        drawScopeInvalidation = Unit
                        layoutCount += 1
                        measure(measurables, constraints)
                    }
                }
            )
        }

        awaitNextRender()
        assertEquals(compositionCount, layoutCount)
        assertEquals(layoutCount, drawCount)

        value = 1
        awaitNextRender()
        assertTrue(compositionCount >= 2)
        assertTrue(layoutCount >= compositionCount, "Layout was performed less times than composition")
        assertTrue(drawCount >= layoutCount, "Draw was performed less times than layout")
    }

    @Test(timeout = 5000)
    fun `rendering of Layout state change`() = renderingTest(width = 40, height = 40) {
        var width by mutableStateOf(10)
        var height by mutableStateOf(20)
        var x by mutableStateOf(0)
        setContent {
            Row(Modifier.height(height.dp)) {
                Layout({
                    Box(Modifier.fillMaxSize().background(Color.Green))
                }) { measurables, constraints ->
                    val placeables = measurables.map { it.measure(constraints) }
                    layout(width, constraints.maxHeight) {
                        placeables.forEach { it.place(x, 0) }
                    }
                }

                Box(Modifier.background(Color.Red).size(10.dp))
            }
        }

        awaitNextRender()
        screenshotRule.snap(surface, "frame1_initial")
        assertFalse(hasRenders())

        width = 20
        awaitNextRender()
        screenshotRule.snap(surface, "frame2_change_width")
        assertFalse(hasRenders())

        x = 10
        awaitNextRender()
        screenshotRule.snap(surface, "frame3_change_x")
        assertFalse(hasRenders())

        height = 10
        awaitNextRender()
        screenshotRule.snap(surface, "frame4_change_height")
        assertFalse(hasRenders())

        width = 10
        height = 20
        x = 0
        awaitNextRender()
        screenshotRule.snap(surface, "frame5_change_all")
        assertFalse(hasRenders())

        width = 10
        height = 20
        x = 0
        assertFalse(hasRenders())
    }

    @Test(timeout = 5000)
    fun `rendering of layer offset`() = renderingTest(width = 40, height = 40) {
        var translationX by mutableStateOf(10f)
        var offsetX by mutableStateOf(10.dp)
        setContent {
            Box(Modifier.offset(x = offsetX).graphicsLayer(translationX = translationX)) {
                Box(Modifier.background(Color.Green).size(10.dp))
            }
        }

        awaitNextRender()
        screenshotRule.snap(surface, "frame1_initial")
        assertFalse(hasRenders())

        offsetX -= 10.dp
        awaitNextRender()
        screenshotRule.snap(surface, "frame2_offset")
        assertFalse(hasRenders())

        translationX -= 10f
        awaitNextRender()
        screenshotRule.snap(surface, "frame3_translation")
        assertFalse(hasRenders())

        offsetX += 10.dp
        translationX += 10f
        awaitNextRender()
        screenshotRule.snap(surface, "frame4_offset_and_translation")
        assertFalse(hasRenders())
    }

    @Test(timeout = 5000)
    fun `rendering of transition`() = renderingTest(width = 40, height = 40) {
        val startValue = 10f
        var targetValue by mutableStateOf(startValue)
        var lastComposedValue = Float.MIN_VALUE

        setContent {
            val value by animateFloatAsState(
                targetValue,
                animationSpec = TweenSpec(durationMillis = 30, easing = LinearEasing)
            )
            Box(Modifier.size(value.dp).background(Color.Blue))
            lastComposedValue = value
        }

        awaitNextRender()
        screenshotRule.snap(surface, "frame1_initial")

        targetValue = 40f
        awaitNextRender()
        screenshotRule.snap(surface, "frame2_target40_0ms")

        // animation can start not immediately, but on the second/third frame
        // so wait when the animation will change the animating value
        while (lastComposedValue == startValue) {
            currentTimeMillis += 10
            awaitNextRender()
        }

        screenshotRule.snap(surface, "frame3_target40_10ms")

        currentTimeMillis += 10
        awaitNextRender()
        screenshotRule.snap(surface, "frame4_target40_20ms")

        currentTimeMillis += 10
        awaitNextRender()
        screenshotRule.snap(surface, "frame5_target40_30ms")

        assertFalse(hasRenders())
    }

    @Test(timeout = 5000)
    @Ignore("b/271123970 Fails in AOSP. Will be fixed after upstreaming Compose for Desktop")
    fun `rendering of clickable`() = renderingTest(width = 40, height = 40) {
        setContent {
            val interactionSource = remember { MutableInteractionSource() }
            Box(
                Modifier
                    .size(20.dp)
                    .background(Color.Blue)
                    .clickable(indication = PressIndicationNodeFactory, interactionSource = interactionSource) {}
            )
        }
        awaitNextRender()
        screenshotRule.snap(surface, "frame1_initial")
        assertFalse(hasRenders())

        scene.sendPointerEvent(PointerEventType.Enter, Offset(2f, 2f))
        scene.sendPointerEvent(PointerEventType.Move, Offset(2f, 2f))

        scene.sendPointerEvent(PointerEventType.Press, Offset(2f, 2f))
        awaitNextRender()
        screenshotRule.snap(surface, "frame2_onMousePressed")
        assertFalse(hasRenders())

        scene.sendPointerEvent(PointerEventType.Move, Offset(1f, 1f))
        assertFalse(hasRenders())

        scene.sendPointerEvent(PointerEventType.Release, Offset(1f, 1f))

        scene.sendPointerEvent(PointerEventType.Move, Offset(-1f, -1f))
        scene.sendPointerEvent(PointerEventType.Exit, Offset(-1f, -1f))
        awaitNextRender()
        // TODO(https://github.com/JetBrains/compose-multiplatform/issues/2970)
        //  fix one-frame lag after a Release
        awaitNextRender()
        screenshotRule.snap(surface, "frame3_onMouseReleased")

        scene.sendPointerEvent(PointerEventType.Enter, Offset(1f, 1f))
        scene.sendPointerEvent(PointerEventType.Move, Offset(1f, 1f))

        scene.sendPointerEvent(PointerEventType.Press, Offset(3f, 3f))
        awaitNextRender()
        screenshotRule.snap(surface, "frame4_onMouseMoved_onMousePressed")
        assertFalse(hasRenders())
    }

    @Test(timeout = 5000)
    fun `rendering of LazyColumn`() = renderingTest(
        width = 40,
        height = 40
    ) {
        var itemHeight by mutableStateOf(10.dp)
        val padding = 10
        val columnHeight = this.height - padding * 2
        val state = LazyListState()
        setContent {
            Box(Modifier.padding(padding.dp)) {
                LazyColumn(state = state) {
                    items(
                        listOf(Color.Red, Color.Green, Color.Blue, Color.Black, Color.Gray)
                    ) { color ->
                        Box(Modifier.size(width = 30.dp, height = itemHeight).background(color))
                    }
                }
            }
        }

        awaitNextRender()
        screenshotRule.snap(surface, "frame1_initial")
        assertFalse(hasRenders())

        state.scroll {
            scrollBy(columnHeight.toFloat())
        }
        awaitNextRender()
        screenshotRule.snap(surface, "frame2_onMouseScroll")
        assertFalse(hasRenders())

        state.scroll {
            scrollBy(10 * columnHeight.toFloat())
        }
        awaitNextRender()
        screenshotRule.snap(surface, "frame3_onMouseScroll")
        assertFalse(hasRenders())

        itemHeight = 5.dp
        awaitNextRender()
        screenshotRule.snap(surface, "frame4_change_height")

        // see https://github.com/JetBrains/compose-jb/issues/2171, we have extra rendered frames here
        skipRenders()

        assertFalse(hasRenders())
    }

    @Test(timeout = 5000)
    fun `rendering, change state before first onRender`() = renderingTest(
        width = 40,
        height = 40
    ) {
        var size by mutableStateOf(20.dp)
        setContent {
            Box(Modifier.size(size).background(Color.Blue))
        }

        size = 10.dp
        awaitNextRender()
        screenshotRule.snap(surface, "frame1_initial")
        assertFalse(hasRenders())
    }

    @Test(timeout = 5000)
    fun `launch effect`() = renderingTest(width = 40, height = 40) {
        var effectIsLaunched = false

        setContent {
            LaunchedEffect(Unit) {
                effectIsLaunched = true
            }
        }

        awaitNextRender()
        assertThat(effectIsLaunched).isTrue()
    }

    @Test(timeout = 5000)
    fun `change density`() = renderingTest(width = 40, height = 40) {
        @Composable
        fun redRect() {
            Box(Modifier.size(4.dp).background(Color.Red))
        }

        @Composable
        fun greenRectOnCanvas() {
            Canvas(Modifier.size(100.dp)) {
                drawRect(
                    Color.Green,
                    topLeft = Offset(4f * density, 4f * density),
                    size = Size(4f * density, 4f * density)
                )
            }
        }

        @Composable
        fun blueRectInRoundedLayer() {
            Box(
                Modifier
                    .offset(8.dp, 8.dp)
                    .graphicsLayer(shape = RoundedCornerShape(2.dp), clip = true)
            ) {
                Box(
                    Modifier
                        .size(4.dp)
                        .background(Color.Blue)
                )
            }
        }

        @Composable
        fun elevation() {
            Box(
                Modifier
                    .offset(8.dp, 0.dp)
            ) {
                Surface(
                    modifier = Modifier.size(4.dp),
                    elevation = 2.dp
                ) {
                }
            }
        }

        setContent {
            redRect()
            greenRectOnCanvas()
            blueRectInRoundedLayer()
            elevation()
        }

        density = 2f
        awaitNextRender()
        screenshotRule.snap(surface, "frame1_density2")

        density = 3f
        awaitNextRender()
        screenshotRule.snap(surface, "frame2_density3")

        assertFalse(hasRenders())
    }

    @Test(timeout = 5000)
    fun `receive buttons`() = renderingTest(
        width = 40,
        height = 40,
        context = Dispatchers.Unconfined
    ) {
        val receivedButtons = mutableListOf<PointerButtons>()

        setContent {
            Box(
                Modifier.size(40.dp).onPointerEvent(PointerEventType.Press) {
                    receivedButtons.add(it.buttons)
                }
            )
        }

        var buttons = PointerButtons(isSecondaryPressed = true, isBackPressed = true)
        scene.sendPointerEvent(
            PointerEventType.Press,
            Offset(0f, 0f),
            buttons = buttons
        )
        assertThat(receivedButtons.size).isEqualTo(1)
        assertThat(receivedButtons.last()).isEqualTo(buttons)

        buttons = PointerButtons(
            isPrimaryPressed = true,
            isTertiaryPressed = true,
            isForwardPressed = true
        )
        scene.sendPointerEvent(
            PointerEventType.Press,
            Offset(0f, 0f),
            buttons = buttons
        )
        assertThat(receivedButtons.size).isEqualTo(2)
        assertThat(receivedButtons.last()).isEqualTo(buttons)
    }

    @Test(timeout = 5000)
    fun `receive modifiers`() = renderingTest(
        width = 40,
        height = 40,
        context = Dispatchers.Unconfined
    ) {
        val receivedKeyboardModifiers = mutableListOf<PointerKeyboardModifiers>()

        setContent {
            Box(
                Modifier.size(40.dp).onPointerEvent(PointerEventType.Press) {
                    receivedKeyboardModifiers.add(it.keyboardModifiers)
                }
            )
        }

        var keyboardModifiers = PointerKeyboardModifiers(isAltPressed = true)
        scene.sendPointerEvent(
            PointerEventType.Press,
            Offset(0f, 0f),
            keyboardModifiers = keyboardModifiers
        )
        assertThat(receivedKeyboardModifiers.size).isEqualTo(1)
        assertThat(receivedKeyboardModifiers.last()).isEqualTo(keyboardModifiers)

        keyboardModifiers = PointerKeyboardModifiers(
            isCtrlPressed = true,
            isMetaPressed = true,
            isAltPressed = false,
            isShiftPressed = true,
            isAltGraphPressed = true,
            isSymPressed = true,
            isFunctionPressed = true,
            isCapsLockOn = true,
            isScrollLockOn = true,
            isNumLockOn = true,
        )
        scene.sendPointerEvent(
            PointerEventType.Press,
            Offset(0f, 0f),
            keyboardModifiers = keyboardModifiers
        )
        assertThat(receivedKeyboardModifiers.size).isEqualTo(2)
        assertThat(receivedKeyboardModifiers.last()).isEqualTo(keyboardModifiers)
    }

    @Test(expected = TestException::class)
    fun `catch exception in LaunchedEffect`() {
        runBlocking(Dispatchers.Main) {
            composeRule.setContent {
                LaunchedEffect(Unit) {
                    throw TestException()
                }
            }
            composeRule.awaitIdle()
        }
    }

    @Test
    fun stateChangeFromNonUiThreadDoesntCrash() = runApplicationTest {
        // https://github.com/JetBrains/compose-multiplatform/issues/4546
        var value by mutableStateOf(0)
        val done = CompletableDeferred<Unit>()
        var exceptionThrown: Throwable? = null

        launchTestApplication {
            Window(onCloseRequest = {}) {
                Canvas(Modifier.size(100.dp)) {
                    @Suppress("UNUSED_EXPRESSION")
                    value
                }
                LaunchedEffect(Unit) {
                    withContext(Dispatchers.IO) {
                        try {
                            for (i in 1..50) {
                                value = i
                                Snapshot.sendApplyNotifications()
                                delay(1)
                            }
                        } catch (e: Throwable) {
                            exceptionThrown = e
                        }
                        done.complete(Unit)
                    }
                }
            }
        }

        done.await()
        assertNull(exceptionThrown, "Exception thrown setting snapshot state from non-UI thread")
    }

    private class TestException : RuntimeException()

    @ExperimentalComposeUiApi
    @Test
    fun `focus management by keys`() {
        var field1FocusState: FocusState? = null
        var field2FocusState: FocusState? = null
        val (focusItem1, focusItem2) = FocusRequester.createRefs()
        composeRule.setContent {
            var text by remember { mutableStateOf("") }
            Row {
                TextField(
                    text,
                    onValueChange = { text = it },
                    maxLines = 1,
                    modifier = Modifier
                        .onFocusChanged { field1FocusState = it }
                        .focusRequester(focusItem1)
                        .focusProperties {
                            next = focusItem2
                        }
                )
                TextField(
                    text,
                    onValueChange = { text = it },
                    maxLines = 1,
                    modifier = Modifier
                        .onFocusChanged { field2FocusState = it }
                        .focusRequester(focusItem2)
                        .focusProperties {
                            previous = focusItem1
                        }
                )
            }
        }
        composeRule.runOnIdle { focusItem1.requestFocus() }

        composeRule.runOnIdle {
            assertThat(field1FocusState!!.isFocused).isTrue()
            assertThat(field2FocusState!!.isFocused).isFalse()
        }

        composeRule.onRoot().performKeyPress(KeyEvent(Key.Tab, KeyEventType.KeyDown))

        composeRule.runOnIdle {
            assertThat(field1FocusState!!.isFocused).isFalse()
            assertThat(field2FocusState!!.isFocused).isTrue()
        }

        composeRule.onRoot().performKeyPress(
            KeyEvent(Key.Tab, KeyEventType.KeyDown, isShiftPressed = true)
        )

        composeRule.runOnIdle {
            assertThat(field1FocusState!!.isFocused).isTrue()
            assertThat(field2FocusState!!.isFocused).isFalse()
        }
    }
}

private object PressIndicationNodeFactory: IndicationNodeFactory {

    override fun create(interactionSource: InteractionSource): DelegatableNode =
        PressIndicationInstance(interactionSource)

    override fun hashCode() = super.hashCode()

    override fun equals(other: Any?) = super.equals(other)

    private class PressIndicationInstance(private val interactionSource: InteractionSource) :
        Modifier.Node(), DrawModifierNode {
        private var isPressed = false
        override fun onAttach() {
            coroutineScope.launch {
                var pressCount = 0
                interactionSource.interactions.collect { interaction ->
                    when (interaction) {
                        is PressInteraction.Press -> pressCount++
                        is PressInteraction.Release -> pressCount--
                        is PressInteraction.Cancel -> pressCount--
                    }
                    val pressed = pressCount > 0
                    var invalidateNeeded = false
                    if (isPressed != pressed) {
                        isPressed = pressed
                        invalidateNeeded = true
                    }
                    if (invalidateNeeded) invalidateDraw()
                }
            }
        }

        override fun ContentDrawScope.draw() {
            drawContent()
            if (isPressed) {
                drawRect(color = Color.Black.copy(alpha = 0.3f), size = size)
            }
        }
    }
}