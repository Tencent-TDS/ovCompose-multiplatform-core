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

package androidx.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.ComposeViewport
import kotlin.test.BeforeTest
import kotlinx.browser.document
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.events.Event

/**
 * An interface with helper functions to initialise the tests
 */

private const val containerId: String = "canvasApp"

private external interface CanReplaceChildren {
    // this is a standard method for (among other things) emptying DOM element content
    // https://developer.mozilla.org/en-US/docs/Web/API/Element/replaceChildren
    // TODO: add this to our kotlin web external definitions
    fun replaceChildren()
}

internal interface OnCanvasTests {

    @BeforeTest
    fun beforeTest() {
        /** TODO: [kotlin.test.AfterTest] is fixed only in kotlin 2.0
        see https://youtrack.jetbrains.com/issue/KT-61888
         */
        resetCanvas()
    }

    private fun resetCanvas() {
        (getCanvasContainer() as CanReplaceChildren).replaceChildren()
    }

    private fun getCanvasContainer() = document.getElementById(containerId) ?: error("failed to get canvas with id ${containerId}")

    fun getCanvas(): HTMLCanvasElement {
        val canvas = (getCanvasContainer().querySelector("canvas") as? HTMLCanvasElement) ?: error("failed to get canvas")
        return canvas
    }

    fun createComposeWindow(content: @Composable () -> Unit) {
        ComposeViewport(containerId, content = content)
    }

    fun dispatchEvents(vararg events: Any) {
        val canvas = getCanvas()
        for (event in events) {
            canvas.dispatchEvent(event as Event)
        }
    }
}

internal fun <T> Channel<T>.sendFromScope(value: T, scope: CoroutineScope = GlobalScope) {
    scope.launch(Dispatchers.Unconfined) { send(value) }
}