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

import androidx.compose.ui.events.createMouseEvent
import androidx.compose.ui.sendFromScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest

class MinimalChannelTest {

    @Test
    fun pureChannelTest() = runTest {
        val minimalChannel = Channel<Int>(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

        window.setTimeout({
            println("[pureChannelTest] sending from scope")
            minimalChannel.sendFromScope(42)
            null
        }, 3000)

        assertEquals(42, minimalChannel.receive())
    }

    @Test
    fun domEventChannelTest() = runTest {
        val minimalChannel = Channel<Int>(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

        val el = document.createElement("div")
        el.addEventListener("mousedown", {
            println("[domEventChannelTest] sending on mouseDown")
            minimalChannel.sendFromScope(42)
        })

        el.dispatchEvent(createMouseEvent("mousedown"))

        assertEquals(42, minimalChannel.receive())
    }
}