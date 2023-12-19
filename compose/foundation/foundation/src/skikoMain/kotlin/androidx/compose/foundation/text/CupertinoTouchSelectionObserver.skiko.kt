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

package androidx.compose.foundation.text

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.text.selection.ClicksCounter
import androidx.compose.foundation.text.selection.SelectionAdjustment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAll

internal interface CupertinoTouchSelectionObserver {
    // if returns true event will be consumed
    fun onStart(downPosition: Offset, adjustment: SelectionAdjustment): Boolean
    fun onDrag(dragPosition: Offset, adjustment: SelectionAdjustment): Boolean
}

internal suspend fun PointerInputScope.cupertinoTouchSelectionDetector(
    observer: CupertinoTouchSelectionObserver
) {
    awaitEachGesture {
        val clicksCounter = ClicksCounter(viewConfiguration, clicksSlop = 50.dp.toPx())
        while (true) {
            val down = awaitTouchEventDown()
            val downChange = down.changes[0]
            clicksCounter.update(downChange)
            val selectionMode = when (clicksCounter.clicks) {
                1 -> SelectionAdjustment.None
                2 -> SelectionAdjustment.Word
                else -> SelectionAdjustment.Paragraph
            }
            val started = observer.onStart(downChange.position, selectionMode)
            if (started) {
                downChange.consume()
                drag(downChange.id) {
                    if (observer.onDrag(it.position, selectionMode)) {
                        it.consume()
                    }
                }
            }
        }
    }
}

private suspend fun AwaitPointerEventScope.awaitTouchEventDown(): PointerEvent {
    var event: PointerEvent
    do {
        event = awaitPointerEvent(PointerEventPass.Main)
    } while (
        !(
            /*event.buttons.isPrimaryPressed &&*/ event.changes.fastAll {
                it.type == PointerType.Touch /*&& it.changedToDown()*/
            }
            )
    )
    return event
}
