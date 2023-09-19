/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.gestures

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.util.fastAll

internal suspend fun PointerInputScope.detectRepeatingTapsGestures(
    onTap: ((Offset) -> Unit)? = null,
    onDoubleTap: ((Offset) -> Unit)? = null,
    onTripleTap: ((Offset) -> Unit)? = null,
) {
    awaitEachGesture {
        val touchesCounter = TouchCounter(viewConfiguration)
        while (true) {
            val down = awaitTouchEventDown()
            touchesCounter.update(down)
            val downChange = down.changes[0]
            when (touchesCounter.touches) {
                1 -> {
                    if (onTap != null) {
                        onTap(downChange.position)
                        downChange.consume()
                    }
                }

                2 -> {
                    if (onDoubleTap != null) {
                        onDoubleTap(downChange.position)
                        downChange.consume()
                    }
                }

                else -> {
                    if (onTripleTap != null) {
                        onTripleTap(downChange.position)
                        downChange.consume()
                    }
                }
            }
        }
    }
}

// Distance in pixels between consecutive click positions to be considered them as clicks sequence
private const val TouchesSlop = 100.0

private class TouchCounter(
    private val viewConfiguration: ViewConfiguration
) {
    var touches = 0
    var prevTouch: PointerInputChange? = null
    fun update(event: PointerEvent) {
        val currentPrevTouch = prevTouch
        val newTouch = event.changes[0]
        if (currentPrevTouch != null &&
            timeIsTolerable(currentPrevTouch, newTouch) &&
            positionIsTolerable(currentPrevTouch, newTouch)
        ) {
            touches += 1
        } else {
            touches = 1
        }
        prevTouch = newTouch
    }

    fun timeIsTolerable(prevTouch: PointerInputChange, newTouch: PointerInputChange): Boolean {
        val diff = newTouch.uptimeMillis - prevTouch.uptimeMillis
        return diff < viewConfiguration.doubleTapTimeoutMillis
    }

    fun positionIsTolerable(prevTouch: PointerInputChange, newTouch: PointerInputChange): Boolean {
        val diff = newTouch.position - prevTouch.position
        return diff.getDistance() < TouchesSlop
    }
}

private suspend fun AwaitPointerEventScope.awaitTouchEventDown(): PointerEvent {
    var event: PointerEvent
    do {
        event = awaitPointerEvent(PointerEventPass.Main)
    } while (
        !(event.isTouchedDown())
    )
    return event
}

private fun PointerEvent.isTouchedDown(): Boolean {
    return this.changes.fastAll { (it.type == PointerType.Touch) && it.changedToDown() }
}

