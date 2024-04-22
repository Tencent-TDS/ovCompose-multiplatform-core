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

import androidx.compose.foundation.text.isTypedEvent
import androidx.compose.ui.input.key.toComposeEvent
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.w3c.dom.events.KeyboardEvent

class IsTypedEventTests {
    private fun KeyboardEvent.assertIsTyped(message: String? = null) {
        val composeEvent = toComposeEvent()
        assertTrue(composeEvent.isTypedEvent, message ?: "event ${composeEvent} supposed to be typed but actually is not")
    }

    private fun KeyboardEvent.assertIsNotTyped(message: String? = null) {
        val composeEvent = toComposeEvent()
        assertFalse(composeEvent.isTypedEvent, message ?: "event ${composeEvent} not supposed to be typed but actually is")
    }

    @Test
    fun standardLatin() {
        val latin = listOf(
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
            'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'
        )
        latin.forEach { keyDownEvent(it).assertIsTyped() }
    }
}