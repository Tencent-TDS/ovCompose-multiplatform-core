/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.window.window

import androidx.compose.ui.sendInputEvent
import androidx.compose.ui.sendKeyEvent
import androidx.compose.ui.sendKeyTypedEvent
import androidx.compose.ui.text.TextRange
import java.awt.event.KeyEvent.KEY_PRESSED
import java.awt.event.KeyEvent.KEY_RELEASED
import org.junit.experimental.theories.Theories
import org.junit.experimental.theories.Theory
import org.junit.runner.RunWith

/**
 * Tests for emulate input to the native window on various systems.
 *
 * Events were captured on each system via logging.
 * All tests can run on all OSes.
 * The OS names in test names just represent a unique order of input events on these OSes.
 */
@RunWith(Theories::class)
class WindowTypeTest : BaseWindowTextFieldTest() {
    @Theory
    internal fun `q, w, space, backspace 4x (English)`(
        textFieldKind: TextFieldKind<*>
    ) = runTextFieldTest(textFieldKind, "English") {
        // q
        window.sendKeyEvent(81, 'q', KEY_PRESSED)
        window.sendKeyTypedEvent('q')
        window.sendKeyEvent(81, 'q', KEY_RELEASED)
        assertStateEquals("q", selection = TextRange(1), composition = null)

        // w
        window.sendKeyEvent(87, 'w', KEY_PRESSED)
        window.sendKeyTypedEvent('w')
        window.sendKeyEvent(87, 'w', KEY_RELEASED)
        assertStateEquals("qw", selection = TextRange(2), composition = null)

        // space
        window.sendKeyEvent(32, ' ', KEY_PRESSED)
        window.sendKeyTypedEvent(' ')
        window.sendKeyEvent(32, ' ', KEY_RELEASED)
        assertStateEquals("qw ", selection = TextRange(3), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("qw", selection = TextRange(2), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("q", selection = TextRange(1), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)
    }

    @Theory
    internal fun `q, w, space, backspace 4x (Russian)`(
        textFieldKind: TextFieldKind<*>
    ) = runTextFieldTest(textFieldKind, "Russian") {
        // q
        window.sendKeyEvent(81, 'й', KEY_PRESSED)
        window.sendKeyTypedEvent('й')
        window.sendKeyEvent(81, 'й', KEY_RELEASED)
        assertStateEquals("й", selection = TextRange(1), composition = null)

        // w
        window.sendKeyEvent(87, 'ц', KEY_PRESSED)
        window.sendKeyTypedEvent('ц')
        window.sendKeyEvent(87, 'ц', KEY_RELEASED)
        assertStateEquals("йц", selection = TextRange(2), composition = null)

        // space
        window.sendKeyEvent(32, ' ', KEY_PRESSED)
        window.sendKeyTypedEvent(' ')
        window.sendKeyEvent(32, ' ', KEY_RELEASED)
        assertStateEquals("йц ", selection = TextRange(3), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("йц", selection = TextRange(2), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("й", selection = TextRange(1), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)
    }

    @Theory
    internal fun `f, g, space, backspace 4x (Arabic)`(
        textFieldKind: TextFieldKind<*>
    ) = runTextFieldTest(textFieldKind, "Arabic") {
        // q
        window.sendKeyEvent(70, 'ب', KEY_PRESSED)
        window.sendKeyTypedEvent('ب')
        window.sendKeyEvent(70, 'ب', KEY_RELEASED)
        assertStateEquals("ب", selection = TextRange(1), composition = null)

        // w
        window.sendKeyEvent(71, 'ل', KEY_PRESSED)
        window.sendKeyTypedEvent('ل')
        window.sendKeyEvent(71, 'ل', KEY_RELEASED)
        assertStateEquals("بل", selection = TextRange(2), composition = null)

        // space
        window.sendKeyEvent(32, ' ', KEY_PRESSED)
        window.sendKeyTypedEvent(' ')
        window.sendKeyEvent(32, ' ', KEY_RELEASED)
        assertStateEquals("بل ", selection = TextRange(3), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("بل", selection = TextRange(2), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("ب", selection = TextRange(1), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)
    }

    @Theory
    internal fun `q, w, space, backspace 4x (Korean, Windows)`(
        textFieldKind: TextFieldKind<*>
    ) = runTextFieldTest(textFieldKind, "Korean, Windows") {
        // q
        window.sendInputEvent("ㅂ", 0)
        window.sendKeyEvent(81, 'q', KEY_RELEASED)
        assertStateEquals("ㅂ", selection = TextRange(1), composition = TextRange(0, 1))

        // w
        window.sendInputEvent("ㅂ", 1)
        window.sendInputEvent("ㅈ", 0)
        window.sendKeyEvent(87, 'w', KEY_RELEASED)
        assertStateEquals("ㅂㅈ", selection = TextRange(2), composition = TextRange(1, 2))

        // space
        window.sendInputEvent(null, 0)
        window.sendKeyTypedEvent('ㅈ')
        window.sendKeyEvent(32, ' ', KEY_PRESSED)
        window.sendKeyTypedEvent(' ')
        window.sendKeyEvent(32, ' ', KEY_RELEASED)
        assertStateEquals("ㅂㅈ ", selection = TextRange(3), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("ㅂㅈ", selection = TextRange(2), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("ㅂ", selection = TextRange(1), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)
    }

    @Theory
    internal fun `q, w, backspace 3x (Korean, Windows)`(
        textFieldKind: TextFieldKind<*>
    ) = runTextFieldTest(textFieldKind, "Korean, Windows") {
        // q
        window.sendInputEvent("ㅂ", 0)
        window.sendKeyEvent(81, 'q', KEY_RELEASED)
        assertStateEquals("ㅂ", selection = TextRange(1), composition = TextRange(0, 1))

        // w
        window.sendInputEvent("ㅂ", 1)
        window.sendInputEvent("ㅈ", 0)
        window.sendKeyEvent(87, 'w', KEY_RELEASED)
        assertStateEquals("ㅂㅈ", selection = TextRange(2), composition = TextRange(1, 2))

        // backspace
        window.sendInputEvent(null, 0)
        window.sendInputEvent(null, 0)
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("ㅂ", selection = TextRange(1), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)
    }

    @Theory
    internal fun `f, g, space, backspace 3x (Korean, Windows)`(
        textFieldKind: TextFieldKind<*>
    ) = runTextFieldTest(textFieldKind, "Korean, Windows") {
        // f
        window.sendInputEvent("ㄹ", 0)
        window.sendKeyEvent(81, 'f', KEY_RELEASED)
        assertStateEquals("ㄹ", selection = TextRange(1), composition = TextRange(0, 1))

        // g
        window.sendInputEvent("ㅀ", 0)
        window.sendKeyEvent(87, 'g', KEY_RELEASED)
        assertStateEquals("ㅀ", selection = TextRange(1), composition = TextRange(0, 1))

        // space
        window.sendInputEvent(null, 0)
        window.sendKeyTypedEvent('ㅀ')
        window.sendKeyEvent(32, ' ', KEY_PRESSED)
        window.sendKeyTypedEvent(' ')
        window.sendKeyEvent(32, ' ', KEY_RELEASED)
        assertStateEquals("ㅀ ", selection = TextRange(2), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("ㅀ", selection = TextRange(1), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)
    }

    @Theory
    internal fun `f, g, backspace 2x (Korean, Windows)`(
        textFieldKind: TextFieldKind<*>
    ) = runTextFieldTest(textFieldKind, "Korean, Windows") {
        // f
        window.sendInputEvent("ㄹ", 0)
        window.sendKeyEvent(81, 'f', KEY_RELEASED)
        assertStateEquals("ㄹ", selection = TextRange(1), composition = TextRange(0, 1))

        // g
        window.sendInputEvent("ㅀ", 0)
        window.sendKeyEvent(87, 'g', KEY_RELEASED)
        assertStateEquals("ㅀ", selection = TextRange(1), composition = TextRange(0, 1))

        // backspace
        window.sendInputEvent("ㄹ", 0)
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("ㄹ", selection = TextRange(1), composition = TextRange(0, 1))

        // backspace
        window.sendInputEvent(null, 0)
        window.sendInputEvent(null, 0)
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)
    }

    @Theory
    internal fun `q, w, space, backspace 4x (Korean, macOS)`(
        textFieldKind: TextFieldKind<*>
    ) = runTextFieldTest(textFieldKind, "Korean, macOS") {
        // q
        window.sendInputEvent("ㅂ", 0)
        window.sendKeyEvent(81, 'ㅂ', KEY_RELEASED)
        assertStateEquals("ㅂ", selection = TextRange(1), composition = TextRange(0, 1))

        // w
        window.sendInputEvent("ㅂ", 0)
        window.sendInputEvent("ㅂ", 1)
        window.sendInputEvent("ㅈ", 0)
        window.sendKeyEvent(87, 'ㅈ', KEY_RELEASED)
        assertStateEquals("ㅂㅈ", selection = TextRange(2), composition = TextRange(1, 2))

        // space
        window.sendInputEvent("ㅈ ", 0)
        window.sendInputEvent("ㅈ ", 2)
        window.sendKeyEvent(32, ' ', KEY_RELEASED)
        assertStateEquals("ㅂㅈ ", selection = TextRange(3), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("ㅂㅈ", selection = TextRange(2), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("ㅂ", selection = TextRange(1), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)
    }

    @Theory
    internal fun `q, w, backspace 3x (Korean, macOS)`(
        textFieldKind: TextFieldKind<*>
    ) = runTextFieldTest(textFieldKind, "Korean, macOS") {
        // q
        window.sendInputEvent("ㅂ", 0)
        window.sendKeyEvent(81, 'ㅂ', KEY_RELEASED)
        assertStateEquals("ㅂ", selection = TextRange(1), composition = TextRange(0, 1))

        // w
        window.sendInputEvent("ㅂ", 0)
        window.sendInputEvent("ㅂ", 1)
        window.sendInputEvent("ㅈ", 0)
        window.sendKeyEvent(87, 'ㅈ', KEY_RELEASED)
        assertStateEquals("ㅂㅈ", selection = TextRange(2), composition = TextRange(1, 2))

        // backspace
        window.sendInputEvent("ㅈ", 0)
        window.sendInputEvent("ㅈ", 1)
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("ㅂ", selection = TextRange(1), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)
    }

    // f, g on macOS prints 2 separate symbols (comparing to Windows), so we test t + y
    @Theory
    internal fun `t, y, space, backspace 3x (Korean, macOS)`(
        textFieldKind: TextFieldKind<*>
    ) = runTextFieldTest(textFieldKind, "Korean, macOS") {
        // t
        window.sendInputEvent("ㅅ", 0)
        window.sendKeyEvent(84, 'ㅅ', KEY_RELEASED)
        assertStateEquals("ㅅ", selection = TextRange(1), composition = TextRange(0, 1))

        // y
        window.sendInputEvent("쇼", 0)
        window.sendKeyEvent(89, 'ㅛ', KEY_RELEASED)
        assertStateEquals("쇼", selection = TextRange(1), composition = TextRange(0, 1))

        // space
        window.sendInputEvent("쇼 ", 0)
        window.sendInputEvent("쇼 ", 2)
        window.sendKeyEvent(32, ' ', KEY_RELEASED)
        assertStateEquals("쇼 ", selection = TextRange(2), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("쇼", selection = TextRange(1), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)
    }

    @Theory
    internal fun `t, y, backspace 2x (Korean, macOS)`(
        textFieldKind: TextFieldKind<*>
    ) = runTextFieldTest(textFieldKind, "Korean, macOS") {
        // t
        window.sendInputEvent("ㅅ", 0)
        window.sendKeyEvent(84, 'ㅅ', KEY_RELEASED)
        assertStateEquals("ㅅ", selection = TextRange(1), composition = TextRange(0, 1))

        // y
        window.sendInputEvent("쇼", 0)
        window.sendKeyEvent(89, 'ㅛ', KEY_RELEASED)
        assertStateEquals("쇼", selection = TextRange(1), composition = TextRange(0, 1))

        // backspace
        window.sendInputEvent("ㅅ", 0)
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("ㅅ", selection = TextRange(1), composition = TextRange(0, 1))

        // backspace
        window.sendInputEvent("ㅅ", 0)
        window.sendInputEvent("ㅅ", 1)
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)
    }

    @Theory
    internal fun `q, w, space, backspace 4x (Korean, Linux)`(
        textFieldKind: TextFieldKind<*>
    ) = runTextFieldTest(textFieldKind, "Korean, Linux") {
        // q
        window.sendInputEvent("ㅂ", 0)
        window.sendKeyEvent(0, 'ㅂ', KEY_RELEASED)
        assertStateEquals("ㅂ", selection = TextRange(1), composition = TextRange(0, 1))

        // w
        window.sendInputEvent(null, 0)
        window.sendInputEvent("ㅂ", 1)
        window.sendInputEvent("ㅈ", 0)
        window.sendKeyEvent(0, 'ㅈ', KEY_RELEASED)
        assertStateEquals("ㅂㅈ", selection = TextRange(2), composition = TextRange(1, 2))

        // space
        window.sendInputEvent(null, 0)
        window.sendInputEvent("ㅈ", 1)
        window.sendKeyEvent(32, ' ', KEY_PRESSED)
        window.sendKeyTypedEvent(' ')
        window.sendKeyEvent(32, ' ', KEY_RELEASED)
        assertStateEquals("ㅂㅈ ", selection = TextRange(3), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("ㅂㅈ", selection = TextRange(2), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("ㅂ", selection = TextRange(1), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)
    }

    @Theory
    internal fun `q, w, space, backspace 3x (Chinese, Windows)`(
        textFieldKind: TextFieldKind<*>
    ) = runTextFieldTest(textFieldKind, "Chinese, Windows") {
        // q
        window.sendInputEvent("q", 0)
        window.sendKeyEvent(81, 'q', KEY_RELEASED)
        assertStateEquals("q", selection = TextRange(1), composition = TextRange(0, 1))

        // w
        window.sendInputEvent("q'w", 0)
        window.sendKeyEvent(87, 'w', KEY_RELEASED)
        assertStateEquals("q'w", selection = TextRange(3), composition = TextRange(0, 3))

        // space
        window.sendInputEvent("請問", 2)
        window.sendInputEvent(null, 0)
        window.sendKeyEvent(32, ' ', KEY_RELEASED)
        assertStateEquals("請問", selection = TextRange(2), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("請", selection = TextRange(1), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)
    }

    @Theory
    internal fun `q, w, backspace 3x (Chinese, Windows)`(
        textFieldKind: TextFieldKind<*>
    ) = runTextFieldTest(textFieldKind, "Chinese, Windows") {
        // q
        window.sendInputEvent("q", 0)
        window.sendKeyEvent(81, 'q', KEY_RELEASED)
        assertStateEquals("q", selection = TextRange(1), composition = TextRange(0, 1))

        // w
        window.sendInputEvent("q'w", 0)
        window.sendKeyEvent(87, 'w', KEY_RELEASED)
        assertStateEquals("q'w", selection = TextRange(3), composition = TextRange(0, 3))

        // backspace
        window.sendInputEvent("q", 0)
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("q", selection = TextRange(1), composition = TextRange(0, 1))

        // backspace
        window.sendInputEvent(null, 0)
        window.sendInputEvent(null, 0)
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)
    }

    @Theory
    internal fun `q, w, space, backspace 3x (Chinese, macOS)`(
        textFieldKind: TextFieldKind<*>
    ) = runTextFieldTest(textFieldKind, "Chinese, macOS") {
        // q
        window.sendInputEvent("q", 0)
        window.sendKeyEvent(81, 'q', KEY_RELEASED)
        assertStateEquals("q", selection = TextRange(1), composition = TextRange(0, 1))

        // w
        window.sendInputEvent("q w", 0)
        window.sendKeyEvent(87, 'w', KEY_RELEASED)
        assertStateEquals("q w", selection = TextRange(3), composition = TextRange(0, 3))

        // space
        window.sendInputEvent("请问", 2)
        window.sendKeyEvent(32, ' ', KEY_RELEASED)
        assertStateEquals("请问", selection = TextRange(2), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("请", selection = TextRange(1), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)
    }

    @Theory
    internal fun `q, w, backspace 3x (Chinese, macOS)`(
        textFieldKind: TextFieldKind<*>
    ) = runTextFieldTest(textFieldKind, "Chinese, macOS") {
        // q
        window.sendInputEvent("q", 0)
        window.sendKeyEvent(81, 'q', KEY_RELEASED)
        assertStateEquals("q", selection = TextRange(1), composition = TextRange(0, 1))

        // w
        window.sendInputEvent("q w", 0)
        window.sendKeyEvent(87, 'w', KEY_RELEASED)
        assertStateEquals("q w", selection = TextRange(3), composition = TextRange(0, 3))

        // backspace
        window.sendInputEvent("q", 0)
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("q", selection = TextRange(1), composition = TextRange(0, 1))

        // backspace
        window.sendInputEvent("", 0)
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)

        // backspace
        window.sendKeyEvent(8, Char(8), KEY_PRESSED)
        window.sendKeyTypedEvent(Char(8))
        window.sendKeyEvent(8, Char(8), KEY_RELEASED)
        assertStateEquals("", selection = TextRange(0), composition = null)
    }

//    @Test
//    fun secureTextFieldWorksWithInputMethods() = runTextFieldTest(
//        textFieldKind = SecureTextField,
//        name = "OutputTransform, Chinese, macOS"
//    ) {
//        // c
//        window.sendInputEvent("c", 0)
//        window.sendKeyEvent(67, 'c', KEY_RELEASED)
//        assertStateEquals("c", selection = TextRange(1), composition = TextRange(0, 1))
//
//        // space
//        window.sendInputEvent("才", 1)
//        window.sendKeyEvent(32, ' ', KEY_RELEASED)
//        assertStateEquals("才", selection = TextRange(1), composition = null)
//
//        // c
//        window.sendInputEvent("c", 0)
//        window.sendKeyEvent(67, 'c', KEY_RELEASED)
//        assertStateEquals("才c", selection = TextRange(2), composition = TextRange(1, 2))
//
//        // space
//        window.sendInputEvent("才", 1)
//        window.sendKeyEvent(32, ' ', KEY_RELEASED)
//        assertStateEquals("才才", selection = TextRange(2), composition = null)
//    }
}
