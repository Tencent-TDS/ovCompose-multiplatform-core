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

import org.w3c.dom.AddEventListenerOptions
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventTarget

private external interface AbortSignal
private external class AbortController {
    val signal: AbortSignal
    fun abort()
}


private fun withSignal(signal: AbortSignal): AddEventListenerOptions = js("({signal: signal})")

internal class EventTargetListener(private val eventTarget: EventTarget) {
    private val abortController = AbortController()

    fun addDisposableEvent(eventName: String, handler: (Event) -> Unit) {
        eventTarget.addEventListener(eventName, handler, withSignal(abortController.signal))
    }

    fun dispose() {
        abortController.abort()
    }
}

