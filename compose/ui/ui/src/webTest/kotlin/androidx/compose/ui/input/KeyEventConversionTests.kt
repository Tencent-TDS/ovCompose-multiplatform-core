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

import androidx.compose.ui.events.keyEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.toComposeEvent
import androidx.compose.ui.input.key.utf16CodePoint
import kotlin.test.Test
import kotlin.test.assertEquals
import org.w3c.dom.events.KeyboardEvent

private fun KeyboardEvent.assertEquivalence(key: Key, codePoint: Int = key.keyCode.toInt()) {
    val keyEvent = toComposeEvent()
    assertEquals(actual = keyEvent.key, expected = key, message = "key doesn't match for ${this.key} / ${this.code}")
    assertEquals(actual = keyEvent.utf16CodePoint, expected = codePoint, message = "utf16CodePoint doesn't match for ${this.key} / ${this.code}")
}

class KeyEventConversionTests {

    @Test
    fun standardKeyboardLayout() {
        keyEvent("Escape", code = "Escape", keyCode = Key.Escape.keyCode.toInt()).assertEquivalence(key = Key.Escape)

        keyEvent("CapsLock", code = "CapsLock", keyCode = Key.CapsLock.keyCode.toInt()).assertEquivalence(key = Key.CapsLock)
        keyEvent("Tab", code = "Tab", keyCode = Key.Tab.keyCode.toInt()).assertEquivalence(key = Key.Tab)
        keyEvent("Enter", code = "Enter", keyCode = Key.Enter.keyCode.toInt()).assertEquivalence(key = Key.Enter)

        keyEvent("a", code = "KeyA").assertEquivalence(key = Key.A, codePoint = 97)
        keyEvent("b", code = "KeyB").assertEquivalence(key = Key.B, codePoint = 98)
        keyEvent("c", code = "KeyC").assertEquivalence(key = Key.C, codePoint = 99)
        keyEvent("d", code = "KeyD").assertEquivalence(key = Key.D, codePoint = 100)
        keyEvent("e", code = "KeyE").assertEquivalence(key = Key.E, codePoint = 101)
        keyEvent("f", code = "KeyF").assertEquivalence(key = Key.F, codePoint = 102)
        keyEvent("g", code = "KeyG").assertEquivalence(key = Key.G, codePoint = 103)
        keyEvent("h", code = "KeyH").assertEquivalence(key = Key.H, codePoint = 104)
        keyEvent("i", code = "KeyI").assertEquivalence(key = Key.I, codePoint = 105)
        keyEvent("j", code = "KeyJ").assertEquivalence(key = Key.J, codePoint = 106)
        keyEvent("k", code = "KeyK").assertEquivalence(key = Key.K, codePoint = 107)
        keyEvent("l", code = "KeyL").assertEquivalence(key = Key.L, codePoint = 108)
        keyEvent("m", code = "KeyM").assertEquivalence(key = Key.M, codePoint = 109)
        keyEvent("n", code = "KeyN").assertEquivalence(key = Key.N, codePoint = 110)
        keyEvent("o", code = "KeyO").assertEquivalence(key = Key.O, codePoint = 111)
        keyEvent("p", code = "KeyP").assertEquivalence(key = Key.P, codePoint = 112)
        keyEvent("q", code = "KeyQ").assertEquivalence(key = Key.Q, codePoint = 113)
        keyEvent("r", code = "KeyR").assertEquivalence(key = Key.R, codePoint = 114)
        keyEvent("s", code = "KeyS").assertEquivalence(key = Key.S, codePoint = 115)
        keyEvent("t", code = "KeyT").assertEquivalence(key = Key.T, codePoint = 116)
        keyEvent("u", code = "KeyU").assertEquivalence(key = Key.U, codePoint = 117)
        keyEvent("v", code = "KeyV").assertEquivalence(key = Key.V, codePoint = 118)
        keyEvent("w", code = "KeyW").assertEquivalence(key = Key.W, codePoint = 119)
        keyEvent("x", code = "KeyX").assertEquivalence(key = Key.X, codePoint = 120)
        keyEvent("y", code = "KeyY").assertEquivalence(key = Key.Y, codePoint = 121)
        keyEvent("z", code = "KeyZ").assertEquivalence(key = Key.Z, codePoint = 122)

        keyEvent("`", code = "Backquote", keyCode = Key.Grave.keyCode.toInt()).assertEquivalence(key = Key.Grave, codePoint = 96)
        keyEvent("0", code = "Digit0").assertEquivalence(key = Key.Zero)
        keyEvent("1", code = "Digit1").assertEquivalence(key = Key.One)
        keyEvent("2", code = "Digit2").assertEquivalence(key = Key.Two)
        keyEvent("3", code = "Digit3").assertEquivalence(key = Key.Three)
        keyEvent("4", code = "Digit4").assertEquivalence(key = Key.Four)
        keyEvent("5", code = "Digit5").assertEquivalence(key = Key.Five)
        keyEvent("6", code = "Digit6").assertEquivalence(key = Key.Six)
        keyEvent("7", code = "Digit7").assertEquivalence(key = Key.Seven)
        keyEvent("8", code = "Digit8").assertEquivalence(key = Key.Eight)
        keyEvent("9", code = "Digit9").assertEquivalence(key = Key.Nine)

        keyEvent("0", code = "Numpad0").assertEquivalence(key = Key.NumPad0, codePoint = 48)
        keyEvent("1", code = "Numpad1").assertEquivalence(key = Key.NumPad1, codePoint = 49)
        keyEvent("2", code = "Numpad2").assertEquivalence(key = Key.NumPad2, codePoint = 50)
        keyEvent("3", code = "Numpad3").assertEquivalence(key = Key.NumPad3, codePoint = 51)
        keyEvent("4", code = "Numpad4").assertEquivalence(key = Key.NumPad4, codePoint = 52)
        keyEvent("5", code = "Numpad5").assertEquivalence(key = Key.NumPad5, codePoint = 53)
        keyEvent("6", code = "Numpad6").assertEquivalence(key = Key.NumPad6, codePoint = 54)
        keyEvent("7", code = "Numpad7").assertEquivalence(key = Key.NumPad7, codePoint = 55)
        keyEvent("8", code = "Numpad8").assertEquivalence(key = Key.NumPad8, codePoint = 56)
        keyEvent("9", code = "Numpad9").assertEquivalence(key = Key.NumPad9, codePoint = 57)

        keyEvent("=", code = "NumpadEqual", keyCode = Key.NumPadEquals.keyCode.toInt()).assertEquivalence(key = Key.NumPadEquals, codePoint = 61)
        keyEvent("/", code = "NumpadDivide", keyCode = Key.NumPadDivide.keyCode.toInt()).assertEquivalence(key = Key.NumPadDivide, codePoint = 47)
        keyEvent("*", code = "NumpadMultiply", keyCode = Key.NumPadMultiply.keyCode.toInt()).assertEquivalence(key = Key.NumPadMultiply, codePoint = 42)
        keyEvent("-", code = "NumpadSubtract", keyCode = Key.NumPadSubtract.keyCode.toInt()).assertEquivalence(key = Key.NumPadSubtract, codePoint = 45)
        keyEvent("+", code = "NumpadAdd", keyCode = Key.NumPadAdd.keyCode.toInt()).assertEquivalence(key = Key.NumPadAdd, codePoint = 43)

        //TODO: Situation with NumpadEnter is not clear so far keyDownEvent("Enter", code = "NumpadEnter").assertEquivalence(key = Key.NumPadEnter, codePoint = 13)
        keyEvent(".", code = "NumpadDecimal", keyCode = Key.NumPadDot.keyCode.toInt()).assertEquivalence(key = Key.NumPadDot, codePoint = 46)

        keyEvent("Backspace", code = "Backspace", keyCode = Key.Backspace.keyCode.toInt()).assertEquivalence(key = Key.Backspace)
        keyEvent("Delete", code = "Delete", keyCode = Key.Delete.keyCode.toInt()).assertEquivalence(key = Key.Delete)

        keyEvent("[", code = "BracketLeft", keyCode = Key.LeftBracket.keyCode.toInt()).assertEquivalence(key = Key.LeftBracket, codePoint = 91)
        keyEvent("]", code = "BracketRight", keyCode = Key.RightBracket.keyCode.toInt()).assertEquivalence(key = Key.RightBracket, codePoint = 93)
        keyEvent("\\", code = "Backslash",  keyCode = Key.Backslash.keyCode.toInt()).assertEquivalence(key = Key.Backslash, codePoint = 92)

        keyEvent("Home", code = "Home", keyCode = Key.MoveHome.keyCode.toInt()).assertEquivalence(key = Key.MoveHome)
        keyEvent("End", code = "End", keyCode = Key.MoveEnd.keyCode.toInt()).assertEquivalence(key = Key.MoveEnd)
        keyEvent("PageUp", code = "PageUp", keyCode = Key.PageUp.keyCode.toInt()).assertEquivalence(key = Key.PageUp)
        keyEvent("PageDown", code = "PageDown", keyCode = Key.PageDown.keyCode.toInt()).assertEquivalence(key = Key.PageDown)


        keyEvent("ShiftLeft", code = "ShiftLeft", keyCode = Key.ShiftLeft.keyCode.toInt()).assertEquivalence(key = Key.ShiftLeft)
        keyEvent("ShiftRight", code = "ShiftRight", keyCode = Key.ShiftRight.keyCode.toInt()).assertEquivalence(key = Key.ShiftRight)

        keyEvent("Control", code = "ControlLeft", keyCode = Key.CtrlLeft.keyCode.toInt()).assertEquivalence(key = Key.CtrlLeft)
        keyEvent("Control", code = "ControlRight", keyCode = Key.CtrlRight.keyCode.toInt()).assertEquivalence(key = Key.CtrlRight)

        keyEvent("Meta", code = "MetaLeft", keyCode = Key.MetaLeft.keyCode.toInt()).assertEquivalence(key = Key.MetaLeft)
        keyEvent("Meta", code = "MetaRight", keyCode = Key.MetaRight.keyCode.toInt()).assertEquivalence(key = Key.MetaRight)

        keyEvent("-", code = "Minus", keyCode = Key.Minus.keyCode.toInt()).assertEquivalence(key = Key.Minus, codePoint = 45)
        keyEvent("=", code = "Equal", keyCode = Key.Equals.keyCode.toInt()).assertEquivalence(key = Key.Equals, codePoint = 61)
        keyEvent(";", code = "Semicolon", keyCode = Key.Semicolon.keyCode.toInt()).assertEquivalence(key = Key.Semicolon, codePoint = 59)

        keyEvent(",", code = "Comma", keyCode = Key.Comma.keyCode.toInt()).assertEquivalence(key = Key.Comma, codePoint = 44)
        keyEvent(".", code = "Period", keyCode = Key.Period.keyCode.toInt()).assertEquivalence(key = Key.Period, codePoint = 46)
        keyEvent("/", code = "Slash", keyCode = Key.Slash.keyCode.toInt()).assertEquivalence(key = Key.Slash, codePoint = 47)

        keyEvent("Alt", code = "AltLeft", keyCode = Key.AltLeft.keyCode.toInt()).assertEquivalence(key = Key.AltLeft)
        keyEvent("Alt", code = "AltRight", keyCode = Key.AltRight.keyCode.toInt()).assertEquivalence(key = Key.AltRight)

        keyEvent("ArrowUp", code = "ArrowUp", keyCode = Key.DirectionUp.keyCode.toInt()).assertEquivalence(key = Key.DirectionUp)
        keyEvent("ArrowRight", code = "ArrowRight", keyCode = Key.DirectionRight.keyCode.toInt()).assertEquivalence(key = Key.DirectionRight)
        keyEvent("ArrowDown", code = "ArrowDown", keyCode = Key.DirectionDown.keyCode.toInt()).assertEquivalence(key = Key.DirectionDown)
        keyEvent("ArrowLeft", code = "ArrowLeft", keyCode = Key.DirectionLeft.keyCode.toInt()).assertEquivalence(key = Key.DirectionLeft)

        keyEvent("CapsLock", code = "CapsLock", keyCode = Key.CapsLock.keyCode.toInt()).assertEquivalence(key = Key.CapsLock)

        keyEvent("F1", code = "F1", keyCode = Key.F1.keyCode.toInt()).assertEquivalence(key = Key.F1)
        keyEvent("F2", code = "F2", keyCode = Key.F2.keyCode.toInt()).assertEquivalence(key = Key.F2)
        keyEvent("F3", code = "F3", keyCode = Key.F3.keyCode.toInt()).assertEquivalence(key = Key.F3)
        keyEvent("F4", code = "F4", keyCode = Key.F4.keyCode.toInt()).assertEquivalence(key = Key.F4)
        keyEvent("F5", code = "F5", keyCode = Key.F5.keyCode.toInt()).assertEquivalence(key = Key.F5)
        keyEvent("F6", code = "F6", keyCode = Key.F6.keyCode.toInt()).assertEquivalence(key = Key.F6)
        keyEvent("F7", code = "F7", keyCode = Key.F7.keyCode.toInt()).assertEquivalence(key = Key.F7)
        keyEvent("F8", code = "F8", keyCode = Key.F8.keyCode.toInt()).assertEquivalence(key = Key.F8)
        keyEvent("F9", code = "F9", keyCode = Key.F9.keyCode.toInt()).assertEquivalence(key = Key.F9)
        keyEvent("F10", code = "F10", keyCode = Key.F10.keyCode.toInt()).assertEquivalence(key = Key.F10)
        keyEvent("F11", code = "F11", keyCode = Key.F11.keyCode.toInt()).assertEquivalence(key = Key.F11)
        keyEvent("F12", code = "F12", keyCode = Key.F12.keyCode.toInt()).assertEquivalence(key = Key.F12)

        keyEvent("", code = "Space", keyCode = Key.Spacebar.keyCode.toInt()).assertEquivalence(key = Key.Spacebar)
    }

    @Test
    fun standardKeyboardLayoutUpper() {
        keyEvent("A", code = "KeyA").assertEquivalence(key = Key.A)
        keyEvent("B", code = "KeyB").assertEquivalence(key = Key.B)
        keyEvent("C", code = "KeyC").assertEquivalence(key = Key.C)
        keyEvent("D", code = "KeyD").assertEquivalence(key = Key.D)
        keyEvent("E", code = "KeyE").assertEquivalence(key = Key.E)
        keyEvent("F", code = "KeyF").assertEquivalence(key = Key.F)
        keyEvent("G", code = "KeyG").assertEquivalence(key = Key.G)
        keyEvent("H", code = "KeyH").assertEquivalence(key = Key.H)
        keyEvent("I", code = "KeyI").assertEquivalence(key = Key.I)
        keyEvent("J", code = "KeyJ").assertEquivalence(key = Key.J)
        keyEvent("K", code = "KeyK").assertEquivalence(key = Key.K)
        keyEvent("L", code = "KeyL").assertEquivalence(key = Key.L)
        keyEvent("M", code = "KeyM").assertEquivalence(key = Key.M)
        keyEvent("N", code = "KeyN").assertEquivalence(key = Key.N)
        keyEvent("O", code = "KeyO").assertEquivalence(key = Key.O)
        keyEvent("P", code = "KeyP").assertEquivalence(key = Key.P)
        keyEvent("Q", code = "KeyQ").assertEquivalence(key = Key.Q)
        keyEvent("R", code = "KeyR").assertEquivalence(key = Key.R)
        keyEvent("S", code = "KeyS").assertEquivalence(key = Key.S)
        keyEvent("T", code = "KeyT").assertEquivalence(key = Key.T)
        keyEvent("U", code = "KeyU").assertEquivalence(key = Key.U)
        keyEvent("V", code = "KeyV").assertEquivalence(key = Key.V)
        keyEvent("W", code = "KeyW").assertEquivalence(key = Key.W)
        keyEvent("X", code = "KeyX").assertEquivalence(key = Key.X)
        keyEvent("Y", code = "KeyY").assertEquivalence(key = Key.Y)
        keyEvent("Z", code = "KeyZ").assertEquivalence(key = Key.Z)

        keyEvent("~", code = "Backquote", keyCode = Key.Grave.keyCode.toInt()).assertEquivalence(key = Key.Grave, codePoint = 126)
        keyEvent(")", code = "Digit0", keyCode = Key.Zero.keyCode.toInt()).assertEquivalence(key = Key.Zero, codePoint = 41)
        keyEvent("!", code = "Digit1", keyCode = Key.One.keyCode.toInt()).assertEquivalence(key = Key.One, codePoint = 33)
        keyEvent("@", code = "Digit2", keyCode = Key.Two.keyCode.toInt()).assertEquivalence(key = Key.Two, codePoint = 64)
        keyEvent("#", code = "Digit3", keyCode = Key.Three.keyCode.toInt()).assertEquivalence(key = Key.Three, codePoint = 35)
        keyEvent("$", code = "Digit4", keyCode = Key.Four.keyCode.toInt()).assertEquivalence(key = Key.Four, codePoint = 36)
        keyEvent("%", code = "Digit5", keyCode = Key.Five.keyCode.toInt()).assertEquivalence(key = Key.Five, codePoint = 37)
        keyEvent("^", code = "Digit6", keyCode = Key.Six.keyCode.toInt()).assertEquivalence(key = Key.Six, codePoint = 94)
        keyEvent("&", code = "Digit7", keyCode = Key.Seven.keyCode.toInt()).assertEquivalence(key = Key.Seven, codePoint = 38)
        keyEvent("*", code = "Digit8", keyCode = Key.Eight.keyCode.toInt()).assertEquivalence(key = Key.Eight, codePoint = 42)
        keyEvent("(", code = "Digit9", keyCode = Key.Nine.keyCode.toInt()).assertEquivalence(key = Key.Nine, codePoint = 40)
        keyEvent("_", code = "Minus", keyCode = Key.Minus.keyCode.toInt()).assertEquivalence(key = Key.Minus, codePoint = 95)
        keyEvent("+", code = "Equal", keyCode = Key.Equals.keyCode.toInt()).assertEquivalence(key = Key.Equals, codePoint = 43)
    }

    @Test
    fun standardVirtualKeyboardLayout() {
        // Virtual keyboard generates actual keyboard events for some of the keys pressed
        // This keyboard events, however, actually differ - the code is always "" while key contains the value that we need
        keyEvent("ArrowRight", code = "", keyCode = Key.DirectionRight.keyCode.toInt()).assertEquivalence(key = Key.DirectionRight)
        keyEvent("ArrowLeft", code = "", keyCode = Key.DirectionLeft.keyCode.toInt()).assertEquivalence(key = Key.DirectionLeft)
        keyEvent("Delete", code = "", keyCode = Key.Delete.keyCode.toInt()).assertEquivalence(key = Key.Delete)
        keyEvent("Backspace", code = "", keyCode = Key.Backspace.keyCode.toInt()).assertEquivalence(key = Key.Backspace)
        keyEvent("Enter", code = "", keyCode = Key.Enter.keyCode.toInt()).assertEquivalence(key = Key.Enter)
    }

}
