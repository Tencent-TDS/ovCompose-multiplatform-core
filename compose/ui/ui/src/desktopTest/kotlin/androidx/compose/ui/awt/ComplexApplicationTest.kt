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
package androidx.compose.ui.awt

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDecoration.Companion.Underline
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import com.google.common.truth.Truth
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.jetbrains.skiko.MainUIDispatcher
import org.junit.Ignore
import org.junit.Test

@Suppress("ConstPropertyName")
private const val title = "Desktop Compose Elements"

private val isCtrlPressed = mutableStateOf(false)

@Composable
private fun FrameWindowScope.App() {
    val uriHandler = LocalUriHandler.current
    MaterialTheme {
        Scaffold(
            topBar = {
                WindowDraggableArea {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painterResource("androidx/compose/ui/res/star-size-100.svg"),
                                    contentDescription = "Star"
                                )
                                Text(title)
                            }
                        }
                    )
                }
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    text = { Text("Open URL") },
                    onClick = {
                        uriHandler.openUri("https://google.com")
                    }
                )
            },
            isFloatingActionButtonDocked = true,
            bottomBar = {
                BottomAppBar(cutoutShape = CircleShape) {
                    IconButton(
                        onClick = {}
                    ) {
                        Icon(Icons.Filled.Menu, "Menu", Modifier.size(ButtonDefaults.IconSize))
                    }
                }
            },
            content = { innerPadding ->
                Row(Modifier.padding(innerPadding)) {
                    LeftColumn(Modifier.weight(1f))
                    RightColumn(Modifier.width(200.dp))
                }
            }
        )
    }
}

@Composable
private fun FrameWindowScope.LeftColumn(modifier: Modifier) = Box(modifier.fillMaxSize()) {
    val state = rememberScrollState()
    ScrollableContent(state)

    VerticalScrollbar(
        rememberScrollbarAdapter(state),
        Modifier.align(Alignment.CenterEnd).fillMaxHeight()
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun FrameWindowScope.ScrollableContent(scrollState: ScrollState) {
    val amount = remember { mutableStateOf(0f) }
    val animation = remember { mutableStateOf(true) }
    Column(Modifier.fillMaxSize().verticalScroll(scrollState)) {
        val info = "${window.renderApi} (${window.windowHandle})"
        Text(
            text = "Привет! 你好! Desktop Compose use $info: ${amount.value}",
            color = Color.Black,
            modifier = Modifier
                .background(Color.Blue)
                .height(56.dp)
                .wrapContentSize(Alignment.Center)
        )

        val inlineIndicatorId = "indicator"

        Text(
            text = buildAnnotatedString {
                append("The quick ")
                if (animation.value) {
                    appendInlineContent(inlineIndicatorId)
                }
                pushStyle(
                    SpanStyle(
                        color = Color(0xff964B00),
                        shadow = Shadow(Color.Green, offset = Offset(1f, 1f))
                    )
                )
                append("brown fox")
                pop()
                pushStyle(SpanStyle(background = Color.Yellow))
                append(" 🦊 ate a ")
                pop()
                pushStyle(SpanStyle(fontSize = 30.sp, textDecoration = Underline))
                append("zesty hamburgerfons")
                pop()
                append(" 🍔.\nThe 👩‍👩‍👧‍👧 laughed.")
                addStyle(SpanStyle(color = Color.Green), 25, 35)
            },
            color = Color.Black,
            inlineContent = mapOf(
                inlineIndicatorId to InlineTextContent(
                    Placeholder(
                        width = 1.em,
                        height = 1.em,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.AboveBaseline
                    )
                ) {
                    CircularProgressIndicator(Modifier.padding(end = 3.dp))
                }
            )
        )

        val loremColors = listOf(
            Color.Black,
            Color.Yellow,
            Color.Green,
            Color.Blue
        )
        var loremColor by remember { mutableStateOf(0) }

        @Suppress("RemoveRedundantQualifierName")
        val loremDecorations = listOf(
            TextDecoration.None,
            TextDecoration.Underline,
            TextDecoration.LineThrough
        )
        val lorem = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do" +
            " eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad" +
            " minim veniam, quis nostrud exercitation ullamco laboris nisi ut" +
            " aliquipex ea commodo consequat. Duis aute irure dolor in reprehenderit" +
            " in voluptate velit esse cillum dolore eu fugiat nulla pariatur." +
            " Excepteur" +
            " sint occaecat cupidatat non proident, sunt in culpa qui officia" +
            " deserunt mollit anim id est laborum."
        var loremDecoration by remember { mutableStateOf(0) }
        Text(
            text = lorem,
            color = loremColors[loremColor],
            textAlign = TextAlign.Center,
            textDecoration = loremDecorations[loremDecoration],
            modifier = Modifier.clickable {
                if (loremColor < loremColors.size - 1) {
                    loremColor += 1
                } else {
                    loremColor = 0
                }

                if (loremDecoration < loremDecorations.size - 1) {
                    loremDecoration += 1
                } else {
                    loremDecoration = 0
                }
            }
        )

        Text(
            text = lorem,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                "Default",
            )

            Text(
                "SansSerif",
                fontFamily = FontFamily.SansSerif
            )

            Text(
                "Serif",
                fontFamily = FontFamily.Serif
            )

            Text(
                "Monospace",
                fontFamily = FontFamily.Monospace
            )

            Text(
                "Cursive",
                fontFamily = FontFamily.Cursive
            )
        }

        var overText by remember { mutableStateOf("Move mouse over text:") }
        Text(overText, style = TextStyle(letterSpacing = 10.sp))

        SelectionContainer {
            Text(
                text = "fun <T : Comparable<T>> List<T>.quickSort(): List<T> = when {\n" +
                    "  size < 2 -> this\n" +
                    "  else -> {\n" +
                    "    val pivot = first()\n" +
                    "    val (smaller, greater) = drop(1).partition { it <= pivot }\n" +
                    "    smaller.quickSort() + pivot + greater.quickSort()\n" +
                    "   }\n" +
                    "}",
                modifier = Modifier
                    .padding(10.dp)
                    .onPointerEvent(PointerEventType.Move) {
                        val position = it.changes.first().position
                        overText = "Move position: $position"
                    }
                    .onPointerEvent(PointerEventType.Enter) {
                        overText = "Over enter"
                    }
                    .onPointerEvent(PointerEventType.Exit) {
                        overText = "Over exit"
                    }
            )
        }
        Text(
            text = buildAnnotatedString {
                append("resolved: NotoSans-Regular.ttf ")
                pushStyle(
                    SpanStyle(
                        fontStyle = FontStyle.Italic
                    )
                )
                append("NotoSans-italic.ttf.")
            },
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                modifier = Modifier.padding(4.dp).pointerHoverIcon(PointerIcon.Hand),
                onClick = {
                    amount.value++
                }
            ) {
                Text("Base")
            }

            var clickableText by remember { mutableStateOf("Click me!") }
            @OptIn(ExperimentalFoundationApi::class)
            Text(
                modifier = Modifier.mouseClickable(
                    onClick = {
                        clickableText = buildString {
                            append("Buttons pressed:\n")
                            append("primary: ${buttons.isPrimaryPressed}\t")
                            append("secondary: ${buttons.isSecondaryPressed}\t")
                            append("tertiary: ${buttons.isTertiaryPressed}\t")
                            append("primary: ${buttons.isPrimaryPressed}\t")
                            append("back: ${buttons.isBackPressed}\t")
                            append("forward: ${buttons.isForwardPressed}\t")

                            append("\n\nKeyboard modifiers pressed:\n")

                            append("alt: ${keyboardModifiers.isAltPressed}\t")
                            append("ctrl: ${keyboardModifiers.isCtrlPressed}\t")
                            append("meta: ${keyboardModifiers.isMetaPressed}\t")
                            append("shift: ${keyboardModifiers.isShiftPressed}\t")
                        }
                    }
                ),
                text = clickableText
            )
        }

        Row(
            modifier = Modifier.padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row {
                Column {
                    Switch(
                        animation.value,
                        onCheckedChange = {
                            animation.value = it
                        }
                    )
                    Row(
                        modifier = Modifier.padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            animation.value,
                            onCheckedChange = {
                                animation.value = it
                            }
                        )
                        Text("Animation")
                    }
                }

                Button(
                    modifier = Modifier.padding(4.dp),
                    onClick = {
                        @OptIn(DelicateCoroutinesApi::class)
                        GlobalScope.launchApplication {
                            Window(
                                onCloseRequest = ::exitApplication,
                                state = rememberWindowState(size = DpSize(400.dp, 200.dp)),
                                onPreviewKeyEvent = {
                                    if (it.key == Key.Escape) {
                                        exitApplication()
                                        true
                                    } else {
                                        false
                                    }
                                }
                            ) {
                                Animations(isCircularEnabled = animation.value)
                            }
                        }
                    }
                ) {
                    Text("Window")
                }
            }

            Animations(isCircularEnabled = animation.value)
        }

        Slider(
            value = amount.value / 100f,
            onValueChange = { amount.value = (it * 100) }
        )
        val dropDownMenuExpanded = remember { mutableStateOf(false) }
        Button(onClick = { dropDownMenuExpanded.value = true }) {
            Text("Expand Menu")
        }
        DropdownMenu(
            expanded = dropDownMenuExpanded.value,
            onDismissRequest = {
                dropDownMenuExpanded.value = false
                println("OnDismissRequest")
            }
        ) {
            DropdownMenuItem(modifier = Modifier, onClick = {
                println("Item 1 clicked")
            }) {
                Text("Item 1")
            }
            DropdownMenuItem(modifier = Modifier, onClick = {
                println("Item 2 clicked")
            }) {
                Text("Item 2")
            }
            DropdownMenuItem(modifier = Modifier, onClick = {
                println("Item 3 clicked")
            }) {
                Text("Item 3")
            }
        }
        TextField(
            value = amount.value.toString(),
            onValueChange = { amount.value = it.toFloatOrNull() ?: 42f },
            label = { Text(text = "Input1") }
        )

        val (focusItem1, focusItem2) = FocusRequester.createRefs()
        val text = remember {
            mutableStateOf("Hello \uD83E\uDDD1\uD83C\uDFFF\u200D\uD83E\uDDB0")
        }
        ContextMenuDataProvider(
            items = {
                listOf(ContextMenuItem("Clear") { text.value = ""; focusItem1.requestFocus() })
            }
        ) {
            TextField(
                value = text.value,
                onValueChange = { text.value = it },
                label = { Text(text = "Input2") },
                placeholder = {
                    Text(text = "Important input")
                },
                maxLines = 1,
                modifier = Modifier.onPreviewKeyEvent {
                    when {
                        (it.isMetaPressed && it.key == Key.Enter) -> {
                            if (it.isShiftPressed) {
                                text.value = "Cleared with shift!"
                            } else {
                                text.value = "Cleared!"
                            }
                            true
                        }
                        else -> false
                    }
                }.focusRequester(focusItem1)
                    .focusProperties {
                        next = focusItem2
                    }
            )
        }

        var text2 by remember {
            val initText = buildString {
                (1..1000).forEach {
                    append("$it\n")
                }
            }
            mutableStateOf(initText)
        }
        TextField(
            text2,
            modifier = Modifier
                .height(200.dp)
                .focusRequester(focusItem2)
                .focusProperties {
                    previous = focusItem1
                },
            onValueChange = { text2 = it }
        )

        Row {
            Image(
                painterResource("androidx/compose/ui/res/test.png"),
                "Localized description",
                Modifier.size(200.dp)
            )
        }

        Box(
            modifier = Modifier.size(150.dp).background(Color.Gray).pointerHoverIcon(
                if (isCtrlPressed.value) PointerIcon.Hand else PointerIcon.Default
            )
        ) {
            Box(
                modifier = Modifier.offset(20.dp, 20.dp).size(100.dp).background(Color.Blue).pointerHoverIcon(
                    if (isCtrlPressed.value) PointerIcon.Crosshair else PointerIcon.Text,
                )
            ) {
                Text("pointerHoverIcon test with Ctrl")
            }
        }
    }
}

@Composable
fun Animations(isCircularEnabled: Boolean) = Row {
    if (isCircularEnabled) {
        CircularProgressIndicator(Modifier.padding(10.dp))
    }

    val enabled = remember { mutableStateOf(true) }
    val color by animateColorAsState(
        if (enabled.value) Color.Green else Color.Red,
        animationSpec = TweenSpec(durationMillis = 2000)
    )

    MaterialTheme {
        Box(
            Modifier
                .size(70.dp)
                .clickable { enabled.value = !enabled.value }
                .background(color)
        )
    }
}

@Composable
private fun RightColumn(modifier: Modifier) = Box {
    val state = rememberLazyListState()
    val itemCount = 100000
    val heights = remember {
        val random = Random(24)
        (0 until itemCount).map { random.nextFloat() }
    }

    LazyColumn(modifier.graphicsLayer(alpha = 0.5f), state = state) {
        items((0 until itemCount).toList()) { i ->
            val itemHeight = 20.dp + 20.dp * heights[i]
            Text(i.toString(), Modifier.graphicsLayer(alpha = 0.5f).height(itemHeight))
        }
    }

    VerticalScrollbar(
        rememberScrollbarAdapter(state),
        Modifier.align(Alignment.CenterEnd)
    )
}

@Composable
fun AppWindow() {
    Window(
        onCloseRequest = {},
        title = title,
        state = rememberWindowState(width = 1024.dp, height = 850.dp),
        onPreviewKeyEvent = {
            isCtrlPressed.value = it.isCtrlPressed
            false
        }
    ) {
        CompositionLocalProvider(LocalDensity provides Density(1f)) {
            App()
        }
    }
}

private suspend fun performGC() {
    repeat(10) {
        System.gc()
        System.runFinalization()
        delay(100)
    }
    delay(5000)
}

private val usedMemory get() = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()

class ComplexApplicationTest {

    @Test(timeout = 10 * 60 * 1000)
    fun `no memory leak when open window multiple times`() = runBlocking(MainUIDispatcher) {
        System.err.println("${Date()} Running `no memory leak when open window multiple times`")
        repeat(10) {
            awaitApplication {
                AppWindow()
                LaunchedEffect(Unit) {
                    delay(1000)
                    exitApplication()
                    awaitEDT()
                }
            }
        }

        performGC()
        val oldMemory = usedMemory

        System.err.println("Total memory before: ${Runtime.getRuntime().totalMemory()}")

        repeat(10) {
            awaitApplication {
                AppWindow()
                LaunchedEffect(Unit) {
                    delay(1000)
                    exitApplication()
                    awaitEDT()
                }
            }
        }

        performGC()
        val newMemory = usedMemory

        System.err.println("Total memory after: ${Runtime.getRuntime().totalMemory()}")

        System.err.println("Used memory before: $oldMemory, after: $newMemory, ratio: ${newMemory.toDouble()/oldMemory}")

        Truth
            .assertWithMessage("Memory is increased more than 15% after opening multiple windows")
            .that(newMemory < 1.15 * oldMemory)
            .isTrue()

        assertTrue(false)
    }

    @Ignore
    @Test
    fun `no memory leak when wait 3 minutes`() = runApplicationTest(
        timeoutMillis = 10 * 60 * 1000
    ) {
        launchTestApplication {
            AppWindow()
        }

        delay(30 * 1000)

        performGC()
        val oldMemory = usedMemory

        delay(3 * 60 * 1000)

        performGC()
        val newMemory = usedMemory

        Truth
            .assertWithMessage("Memory is increased more than 15% after waiting a few minutes")
            .that(newMemory < 1.15 * oldMemory)
            .isTrue()
    }
}