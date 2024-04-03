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

package androidx.compose.ui.input.key

actual class NativeKeyEvent(
    val key: Key,
    val value: String?,
    val modifiers: InputModifiers = InputModifiers.EMPTY,
    val kind: KeyEventType,
    val timestamp: Long = 0
)

value class InputModifiers(val value: Int) {
    companion object {
        val EMPTY = InputModifiers(0)
        val META = InputModifiers(1)
        val CONTROL = InputModifiers(2)
        val ALT = InputModifiers(4)
        val SHIFT = InputModifiers(8)
    }

    fun has(value: InputModifiers): Boolean {
        return value.value and this.value != 0
    }

    override fun toString(): String {
        val result = mutableListOf<String>().apply {
            if (has(META)) {
                add("META")
            }
            if (has(CONTROL)) {
                add("CONTROL")
            }
            if (has(ALT)) {
                add("ALT")
            }
            if (has(SHIFT)) {
                add("SHIFT")
            }
        }

        return if (result.isNotEmpty()) result.toString() else ""
    }
}