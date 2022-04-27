/*
 * Copyright 2022 The Android Open Source Project
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

@file:OptIn(ExperimentalComposeUiApi::class)

package androidx.compose.foundation

import androidx.compose.foundation.gestures.GestureCancellationException
import androidx.compose.foundation.gestures.PressGestureScope
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.isOutOfBounds
import androidx.compose.ui.unit.Density
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

internal class ClicksHandlerScope(
    pointerInputScope: PointerInputScope,
    val interactionSource: MutableInteractionSource,
    val pressedInteraction: MutableState<PressInteraction.Press?>,
    val filterState: State<(PointerEvent) -> Boolean>,
    val onDoubleClick: State<(() -> Unit)?>,
    val onLongClick: State<(() -> Unit)?>,
    val onClick: State<() -> Unit>
) : PointerInputScope by pointerInputScope {

    private val pressScope = PressGestureScopeImpl(this)

    private val longPressTimeout: Long
        get() = if (onLongClick.value != null) {
            viewConfiguration.longPressTimeoutMillis
        } else {
            Long.MAX_VALUE / 2
        }

    private fun CoroutineScope.handlePressInteraction(pressOffset: Offset) {
        launch {
            pressScope.handlePressInteraction(
                pressPoint = pressOffset,
                interactionSource = interactionSource,
                pressedInteraction = pressedInteraction,
                delayPressInteraction = mutableStateOf({ false })
            )
        }
    }

    suspend fun awaitEvents() {
        coroutineScope {
            awaitPointerEventScope {
                pressScope.reset()

                val firstPress = awaitPress().apply {
                    changes.fastForEach { it.consume() }
                }

                handlePressInteraction(firstPress.changes[0].position)

                var cancelled = false

                // `firstRelease` will be null if either event is cancelled or it's timed out
                // use `cancelled` flag to distinguish between two cases

                val firstRelease = withTimeoutOrNull(longPressTimeout) {
                    awaitReleaseOrCancelled().apply {
                        this?.changes?.fastForEach { it.consume() }
                        cancelled = this == null
                    }
                }

                if (cancelled) {
                    pressScope.cancel()
                } else if (firstRelease != null) {
                    pressScope.release()
                }

                if (firstRelease == null) {
                    if (onLongClick.value != null && !cancelled) {
                        onLongClick.value!!.invoke()
                        awaitReleaseOrCancelled()
                        pressScope.release()
                    }
                } else if (onDoubleClick.value == null) {
                    onClick.value()
                } else {
                    processSecondClickIfAny(firstRelease, this@coroutineScope)
                }
            }
        }
    }

    private suspend fun AwaitPointerEventScope.processSecondClickIfAny(
        firstRelease: PointerEvent,
        coroutineScope: CoroutineScope,
    ) {
        pressScope.reset()

        val secondPress = awaitSecondPressUnconsumed(
            firstRelease.changes[0]
        )?.apply {
            changes.fastForEach { it.consume() }
        }

        if (secondPress == null) {
            onClick.value()
        } else {
            coroutineScope.handlePressInteraction(secondPress.changes[0].position)

            var cancelled = false

            val secondRelease = withTimeoutOrNull(longPressTimeout) {
                awaitReleaseOrCancelled().apply {
                    this?.changes?.fastForEach { it.consume() }
                    cancelled = this == null
                }
            }

            if (cancelled) {
                pressScope.cancel()
            } else if (secondRelease != null) {
                pressScope.release()
            }

            if (secondRelease == null) {
                if (onLongClick.value != null && !cancelled) {
                    onLongClick.value!!.invoke()
                    awaitReleaseOrCancelled()
                    pressScope.release()
                }
            } else if (!cancelled) {
                onDoubleClick.value?.invoke()
            }
        }
    }

    private suspend fun AwaitPointerEventScope.awaitPress(): PointerEvent {
        return awaitPress(filterState.value)
    }

    private suspend fun AwaitPointerEventScope.awaitSecondPressUnconsumed(
        firstUp: PointerInputChange
    ): PointerEvent? = withTimeoutOrNull(viewConfiguration.doubleTapTimeoutMillis) {
        val minUptime = firstUp.uptimeMillis + viewConfiguration.doubleTapMinTimeMillis
        var event: PointerEvent
        var change: PointerInputChange
        // The second tap doesn't count if it happens before DoubleTapMinTime of the first tap
        do {
            event = awaitPress()
            change = event.changes[0]
        } while (change.uptimeMillis < minUptime)
        event
    }

    private suspend fun AwaitPointerEventScope.awaitReleaseOrCancelled(): PointerEvent? {
        var event: PointerEvent? = null

        while (event == null) {
            event = awaitPointerEvent()

            val cancelled = event.changes.fastAny {
                it.isOutOfBounds(size, Size.Zero)
            }

            if (cancelled) return null

            event = event.takeIf {
                it.isAllPressedUp(requireUnconsumed = true) &&
                    filterState.value(it)
            }

            // Check for cancel by position consumption. We can look on the Final pass of the
            // existing pointer event because it comes after the Main pass we checked above.
            val consumeCheck = awaitPointerEvent(PointerEventPass.Final)
            if (consumeCheck.changes.fastAny { it.isConsumed }) {
                return null
            }
        }

        return event
    }

    private class PressGestureScopeImpl(
        density: Density
    ) : PressGestureScope, Density by density {
        private var isReleased = false
        private var isCanceled = false
        private val mutex = Mutex(locked = false)

        /**
         * Called when a gesture has been canceled.
         */
        fun cancel() {
            isCanceled = true
            mutex.unlock()
        }

        /**
         * Called when all pointers are up.
         */
        fun release() {
            isReleased = true
            mutex.unlock()
        }

        /**
         * Called when a new gesture has started.
         */
        fun reset() {
            mutex.tryLock() // If tryAwaitRelease wasn't called, this will be unlocked.
            isReleased = false
            isCanceled = false
        }

        override suspend fun awaitRelease() {
            if (!tryAwaitRelease()) {
                throw GestureCancellationException("The press gesture was canceled.")
            }
        }

        override suspend fun tryAwaitRelease(): Boolean {
            if (!isReleased && !isCanceled) {
                mutex.lock()
            }
            return isReleased
        }
    }
}

internal suspend fun AwaitPointerEventScope.awaitPress(
    filterPressEvent: (PointerEvent) -> Boolean,
    requireUnconsumed: Boolean = true
): PointerEvent {
    var event: PointerEvent? = null

    while (event == null) {
        event = awaitPointerEvent().takeIf {
            it.isAllPressedDown(requireUnconsumed = requireUnconsumed) &&
            filterPressEvent(it)
        }
    }

    return event
}

private fun PointerEvent.isAllPressedDown(requireUnconsumed: Boolean = true) =
    type == PointerEventType.Press &&
        changes.fastAll { it.type == PointerType.Mouse && (!requireUnconsumed || !it.isConsumed) } ||
        changes.fastAll { if (requireUnconsumed) it.changedToDown() else it.changedToDownIgnoreConsumed() }

private fun PointerEvent.isAllPressedUp(requireUnconsumed: Boolean = true) =
    type == PointerEventType.Release &&
        changes.fastAll { it.type == PointerType.Mouse && (!requireUnconsumed || !it.isConsumed) } ||
        changes.fastAll { if (requireUnconsumed) it.changedToUp() else it.changedToUpIgnoreConsumed() }
