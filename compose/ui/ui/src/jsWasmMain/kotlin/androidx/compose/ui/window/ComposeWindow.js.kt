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

package androidx.compose.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.createSkiaLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.native.ComposeLayer
import androidx.compose.ui.platform.JSTextInputService
import androidx.compose.ui.platform.Platform
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.isActive
import org.w3c.dom.HTMLCanvasElement
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.delay

internal actual class ComposeWindow(val canvasId: String)  {

    actual constructor(): this(defaultCanvasElementId)

    private val density: Density = Density(
        density = window.devicePixelRatio.toFloat(),
        fontScale = 1f
    )

    private val jsTextInputService = JSTextInputService()
    val platform = object : Platform by Platform.Empty {
        override val textInputService = jsTextInputService
        override val viewConfiguration = object : ViewConfiguration {
            override val longPressTimeoutMillis: Long = 500
            override val doubleTapTimeoutMillis: Long = 300
            override val doubleTapMinTimeMillis: Long = 40
            override val touchSlop: Float get() = with(density) { 18.dp.toPx() }
        }
    }
    private val layer = ComposeLayer(
        layer = createSkiaLayer(),
        platform = platform,
        getTopLeftOffset = { Offset.Zero },
        input = jsTextInputService.input
    )

    val canvas = document.getElementById(canvasId) as HTMLCanvasElement

    init {
        layer.layer.attachTo(canvas)
        canvas.setAttribute("tabindex", "0")
        layer.layer.needRedraw()

        layer.setSize(canvas.width, canvas.height)
    }

    fun resize(newSize: IntSize) {
        canvas.width = newSize.width
        canvas.height = newSize.height
        layer.layer.attachTo(canvas)
        layer.setSize(canvas.width, canvas.height)
        layer.layer.needRedraw()
    }

    /**
     * Sets Compose content of the ComposeWindow.
     *
     * @param content Composable content of the ComposeWindow.
     */
    actual fun setContent(
        content: @Composable () -> Unit
    ) {
        layer.setDensity(density)
        layer.setContent(
            content = content
        )
    }

    // TODO: need to call .dispose() on window close.
    actual fun dispose() {
        layer.dispose()
    }
}

private val defaultCanvasElementId = "ComposeTarget"

@ExperimentalComposeUiApi
/**
 * EXPERIMENTAL! Might be deleted or changed in the future!
 *
 * Initializes the composition in HTML canvas identified by [canvasElementId].
 *
 * It can be resized by providing [requestResize].
 * By default it will listen to the window resize events.
 */
fun CanvasBasedWindow(
    title: String = "JetpackNativeWindow",
    canvasElementId: String = defaultCanvasElementId,
    requestResize: suspend () -> IntSize = ::defaultResizeBehavior,
    content: @Composable () -> Unit = { }
) {

    if (requestResize == ::defaultResizeBehavior) {
        (document.getElementById(canvasElementId) as? HTMLCanvasElement)?.let {
            it.width = document.documentElement?.clientWidth ?: 0
            it.height = document.documentElement?.clientHeight ?: 0
        }
    }

    ComposeWindow(canvasId = canvasElementId).apply {
        val composeWindow = this
        setContent {
            content()
            LaunchedEffect(Unit) {
                while (isActive) {
                    val newSize = requestResize()
                    composeWindow.resize(newSize)
                    delay(100) // throttle
                }
            }
        }
    }
}

private suspend fun defaultResizeBehavior(): IntSize {
    val channel = Channel<IntSize>(capacity = CONFLATED)
    window.addEventListener("resize", { _ ->
        val w = document.documentElement?.clientWidth ?: 0
        val h = document.documentElement?.clientHeight ?: 0
        channel.trySend(IntSize(w, h))
    })

    return channel.receive()
}
