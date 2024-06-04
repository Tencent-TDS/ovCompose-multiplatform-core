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
import platform.UIKit.UIKeyModifierAlternate
import platform.UIKit.UIKeyModifierCommand
import platform.UIKit.UIKeyModifierControl
import platform.UIKit.UIKeyModifierShift
import platform.UIKit.UIPress
import platform.UIKit.UIPressPhase.UIPressPhaseBegan
import platform.UIKit.UIPressPhase.UIPressPhaseEnded
import platform.UIKit.UIPressTypeDownArrow
import platform.UIKit.UIPressTypeLeftArrow
import platform.UIKit.UIPressTypePageDown
import platform.UIKit.UIPressTypePageUp
import platform.UIKit.UIPressTypeRightArrow
import platform.UIKit.UIPressTypeUpArrow

internal fun UIPress.toComposeEvent(): KeyEvent {
    val pressKey = key

    val keyEventType = when (phase) {
        UIPressPhaseBegan -> KeyEventType.KeyDown
        UIPressPhaseEnded -> KeyEventType.KeyUp
        else -> KeyEventType.Unknown
    }

    // UIPress has special types for arrow keys and page up/down and has null `key`
    // For other keys the `type` returns an int value that doesn't match any UIPressType enum case.
    val specialTypeKey = when (type) {
        UIPressTypeUpArrow -> Key.DirectionUp
        UIPressTypeDownArrow -> Key.DirectionDown
        UIPressTypeLeftArrow -> Key.DirectionLeft
        UIPressTypeRightArrow -> Key.DirectionRight
        UIPressTypePageDown -> Key.PageDown
        UIPressTypePageUp -> Key.PageUp
        else -> null
    }

    val key = specialTypeKey ?: pressKey?.keyCode?.let { Key(it) } ?: Key.Unknown
    val codePoint = pressKey?.characters?.firstOrNull()?.code ?: 0

    val modifiers = pressKey?.let {
        PointerKeyboardModifiers(
            isCtrlPressed = it.modifierFlags and UIKeyModifierControl != 0L,
            isMetaPressed = it.modifierFlags and UIKeyModifierCommand != 0L,
            isAltPressed = it.modifierFlags and UIKeyModifierAlternate != 0L,
            isShiftPressed = it.modifierFlags and UIKeyModifierShift != 0L,
        )
    } ?: PointerKeyboardModifiers()

    return KeyEvent(
        nativeKeyEvent = InternalKeyEvent(
            key = key,
            type = keyEventType,
            codePoint = codePoint,
            modifiers = modifiers,
            nativeEvent = this
        )
    )
}
