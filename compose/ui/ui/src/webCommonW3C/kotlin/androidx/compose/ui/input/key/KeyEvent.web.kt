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

import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import org.w3c.dom.events.KeyboardEvent

private fun KeyboardEvent.toInputModifiers(): PointerKeyboardModifiers {
    return PointerKeyboardModifiers(
        isAltPressed = altKey,
        isShiftPressed = shiftKey,
        isCtrlPressed = ctrlKey,
        isMetaPressed = metaKey
    )
}

internal fun KeyboardEvent.toComposeEvent(): KeyEvent {
    val composeKey = toKey()
    return KeyEvent(
        nativeKeyEvent = InternalKeyEvent(
            key = composeKey,
            type = when (type) {
                "keydown" -> KeyEventType.KeyDown
                "keyup" -> KeyEventType.KeyUp
                else -> KeyEventType.Unknown
            },
            codePoint = if (key.firstOrNull()?.toString() == key) key.codePointAt(0) else composeKey.keyCode.toInt(),
            modifiers = toInputModifiers(),
            nativeEvent = this
        )
    )
}


// TODO Remove once it's available in common stdlib https://youtrack.jetbrains.com/issue/KT-23251
internal typealias CodePoint = Int

/**
 * Converts a surrogate pair to a unicode code point.
 */
private fun Char.Companion.toCodePoint(high: Char, low: Char): CodePoint =
    (((high - MIN_HIGH_SURROGATE) shl 10) or (low - MIN_LOW_SURROGATE)) + 0x10000

/**
 * Returns the character (Unicode code point) at the specified index.
 */
internal fun String.codePointAt(index: Int): CodePoint {
    val high = this[index]
    if (high.isHighSurrogate() && index + 1 < this.length) {
        val low = this[index + 1]
        if (low.isLowSurrogate()) {
            return Char.toCodePoint(high, low)
        }
    }
    return high.code
}

private fun KeyboardEvent.toKey(): Key {
    return when(code) {
        "KeyA" -> Key.A
        "KeyB" -> Key.B
        "KeyC" -> Key.C
        "KeyD" -> Key.D
        "KeyE" -> Key.E
        "KeyF" -> Key.F
        "KeyG" -> Key.G
        "KeyH" -> Key.H
        "KeyI" -> Key.I
        "KeyJ" -> Key.J
        "KeyK" -> Key.K
        "KeyL" -> Key.L
        "KeyM" -> Key.M
        "KeyN" -> Key.N
        "KeyO" -> Key.O
        "KeyP" -> Key.P
        "KeyQ" -> Key.Q
        "KeyR" -> Key.R
        "KeyS" -> Key.S
        "KeyT" -> Key.T
        "KeyU" -> Key.U
        "KeyV" -> Key.V
        "KeyW" -> Key.W
        "KeyX" -> Key.X
        "KeyY" -> Key.Y
        "KeyZ" -> Key.Z

        "Digit0" -> Key.Zero
        "Digit1" -> Key.One
        "Digit2" -> Key.Two
        "Digit3" -> Key.Three
        "Digit4" -> Key.Four
        "Digit5" -> Key.Five
        "Digit6" -> Key.Six
        "Digit7" -> Key.Seven
        "Digit8" -> Key.Eight
        "Digit9" -> Key.Nine

        "Numpad0" -> Key.NumPad0
        "Numpad1" -> Key.NumPad1
        "Numpad2" -> Key.NumPad2
        "Numpad3" -> Key.NumPad3
        "Numpad4" -> Key.NumPad4
        "Numpad5" -> Key.NumPad5
        "Numpad6" -> Key.NumPad6
        "Numpad7" -> Key.NumPad7
        "Numpad8" -> Key.NumPad8
        "Numpad9" -> Key.NumPad9

        "NumpadDivide" -> Key.NumPadDivide
        "NumpadMultiply" -> Key.NumPadMultiply
        "NumpadSubtract" -> Key.NumPadSubtract
        "NumpadAdd" -> Key.NumPadAdd
        "NumpadEnter" -> Key.NumPadEnter
        "NumpadEqual" -> Key.NumPadEquals
        "NumpadDecimal" -> Key.NumPadDot

        "NumLock" -> Key.NumLock

        "Minus" -> Key.Minus
        "Equal" -> Key.Equals
        "Backspace" -> Key.Backspace
        "BracketLeft" -> Key.LeftBracket
        "BracketRight" -> Key.RightBracket
        "Backslash" -> Key.Backslash
        "Semicolon" -> Key.Semicolon
        "Enter" -> Key.Enter
        "Comma" -> Key.Comma
        "Period" -> Key.Period
        "Slash" -> Key.Slash

        "ArrowLeft" -> Key.DirectionLeft
        "ArrowUp" -> Key.DirectionUp
        "ArrowRight" -> Key.DirectionRight
        "ArrowDown" -> Key.DirectionDown

        "Home" -> Key.MoveHome
        "PageUp" -> Key.PageUp
        "PageDown" -> Key.PageDown
        "Delete" -> Key.Delete
        "End" -> Key.MoveEnd

        "Escape" -> Key.Escape

        "Backquote" -> Key.Grave
        "Tab" -> Key.Tab
        "CapsLock" -> Key.CapsLock

        "ShiftLeft" -> Key.ShiftLeft
        "ControlLeft" -> Key.CtrlLeft
        "AltLeft" -> Key.AltLeft
        "MetaLeft" -> Key.MetaLeft

        "ShiftRight" -> Key.ShiftRight
        "ControlRight" -> Key.CtrlRight
        "AltRight" -> Key.AltRight
        "MetaRight" -> Key.MetaRight
        "Insert" -> Key.Insert

        "F1" -> Key.F1
        "F2" -> Key.F2
        "F3" -> Key.F3
        "F4" -> Key.F4
        "F5" -> Key.F5
        "F6" -> Key.F6
        "F7" -> Key.F7
        "F8" -> Key.F8
        "F9" -> Key.F9
        "F10" -> Key.F10
        "F11" -> Key.F11
        "F12" -> Key.F12

        else -> Key.Unknown
    }
}
