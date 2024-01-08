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
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.text.selection.ClicksCounter
import androidx.compose.foundation.text.selection.SelectionAdjustment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.CancellationException

internal interface CupertinoTouchSelectionObserver {
    // if returns true event will be consumed
    fun onStart(downPosition: Offset, adjustment: SelectionAdjustment): Boolean
    fun onDrag(dragPosition: Offset, adjustment: SelectionAdjustment): Boolean
    fun onStop()
    fun onCancel()
}

internal suspend fun PointerInputScope.cupertinoTouchSelectionDetector(
    observer: CupertinoTouchSelectionObserver
) {
    awaitEachGesture {
        try {
            val clicksCounter = ClicksCounter(viewConfiguration, clicksSlop = 50.dp.toPx())
            while (true) {
                val down = awaitFirstDown(requireUnconsumed = false)
                val drag = awaitLongPressOrCancellation(down.id)
                clicksCounter.update(down)
                when (clicksCounter.clicks) {
                    1 -> { /* Should be ignored without drag */ }
                    2 -> {
                        observer.onStart(down.position, SelectionAdjustment.Word)
                        observer.onStop()
                    }
                    else -> {
                        observer.onStart(down.position, SelectionAdjustment.Paragraph)
                        observer.onStop()
                    }
                }

                if (drag != null) {
                    observer.onStart(down.position, SelectionAdjustment.Word)
                    if (
                        drag(drag.id) {
                            if (observer.onDrag(it.position, SelectionAdjustment.CharacterWithWordAccelerate)) {
                                it.consume()
                            }
                        }
                    ) {
                        currentEvent.changes.fastForEach {
                            if (it.changedToUp()) { it.consume() }
                        }
                        observer.onStop()
                    } else {
                        observer.onCancel()
                    }
                }
            }
        } catch (c: CancellationException) {
            observer.onCancel()
            throw c
        }
    }
}
