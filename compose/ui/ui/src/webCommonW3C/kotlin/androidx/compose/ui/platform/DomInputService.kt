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

package androidx.compose.ui.platform

import org.w3c.dom.events.CompositionEvent
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventTarget
import org.w3c.dom.events.KeyboardEvent

internal interface DomInputListener {
    // This has nothing to do with compositions in Compose sense - it's about DOM composition events
    fun onCompositionStart(evt: CompositionEvent) {}
    fun onCompositionUpdate(evt: CompositionEvent) {}
    fun onCompositionEnd(evt: CompositionEvent) {}

    fun onKeyDown(evt: KeyboardEvent) {}
    fun onKeyUp(evt: KeyboardEvent) {}

    fun onInputInsertLineBreak(evt: InputEvent) {}
    fun onInputInsertCompositionText(evt: InputEvent) {}
    fun onInputInsertText(evt: InputEvent) {}
    fun onInputDeleteContentBackward(evt: InputEvent) {}

    // we end up here only if inputType was unprocessed by any of onInput events above
    fun onInputUnprocessed(evt: InputEvent) {}
}

internal class DomInputService(private val backingElement: EventTarget, private val inputListener: DomInputListener)  {
    init {
        initEvents()
    }

    private fun initEvents() {
        backingElement.addEventListener("keydown", { inputListener.onKeyDown(it as KeyboardEvent) })
        backingElement.addEventListener("keyup", { inputListener.onKeyUp(it as KeyboardEvent) })

        backingElement.addEventListener("compositionstart", { inputListener.onCompositionStart(it as CompositionEvent) })
        backingElement.addEventListener("compositionupdate", { inputListener.onCompositionUpdate(it as CompositionEvent) })
        backingElement.addEventListener("compositionend", { inputListener.onCompositionEnd(it as CompositionEvent) })

        backingElement.addEventListener("input", { evt ->
            evt as InputEvent

            when (evt.inputType) {
                "insertLineBreak" -> inputListener.onInputInsertLineBreak(evt)
                "insertCompositionText" -> inputListener.onInputInsertCompositionText(evt)
                "insertText" -> inputListener.onInputInsertText(evt)
                "deleteContentBackward" -> inputListener.onInputDeleteContentBackward(evt)
                else -> inputListener.onInputUnprocessed(evt)
            }

        })
    }
}

internal external class InputEvent : Event {
    val inputType: String
    val data: String?
}