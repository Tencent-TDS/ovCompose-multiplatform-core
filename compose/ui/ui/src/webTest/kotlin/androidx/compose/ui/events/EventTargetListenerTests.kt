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

package androidx.compose.ui.events

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.browser.document
import kotlinx.browser.window

class EventTargetListenerTests {

    @Test
    fun eventIsNotAttachedGlobally() {
        val el = document.createElement("div")
        val log = mutableListOf<Int>()
        val listener = EventTargetListener(el)
        listener.addDisposableEvent("mousedown") {
            log.add(1)
        }

        // EventTargetListeners for a different DOM elements should not affect each other
        EventTargetListener(document.createElement("div")).addDisposableEvent("mousedown") {
            log.add(2)
        }

        assertEquals(emptyList(), log, "log initially should be empty")

        window.dispatchEvent(MouseEvent("mousedown"))

        assertEquals(emptyList(), log, "dispatching to window should not affect log")

        document.body!!.appendChild(el)

        el.dispatchEvent(MouseEvent("mousedown"))
        assertEquals(listOf(1), log,"log after one event")

        el.dispatchEvent(MouseEvent("mousedown"))
        assertEquals(listOf(1, 1), log,"log after two events")

        el.remove()

        el.dispatchEvent(MouseEvent("mousedown"))
        assertEquals(listOf(1, 1, 1), log, "removing from document should not affect log")

//TODO: this won't work as supposed because lambda passed on the javascript side won't be disposed

//        listener.dispose()
//
//        el.dispatchEvent(MouseEvent("mousedown"))
//        assertEquals(listOf(1, 1, 1), log, "after dispose log should not be updated")
    }
}