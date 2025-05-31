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

package androidx.compose.ui.input.pointer

import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach

/**
 * Modifier to catch pointer above platform interop view, like UIKitView.
 */
internal class InteropViewCatchPointerModifier : PointerInputModifier {

    var onTouchEvent: ((Any?) -> Boolean) = { _ -> false }

    /**
     * The 3 possible states
     */
    private enum class DispatchToViewState {
        /**
         * We have yet to dispatch a new event stream to the child UIKit View.
         */
        Unknown,

        /**
         * We have dispatched to the child UIKit View and it wants to continue to receive
         * events for the current event stream.
         */
        Dispatching,

        /**
         * We intercepted the event stream, or the UIKit View no longer wanted to receive
         * events for the current event stream.
         */
        NotDispatching
    }

    override val pointerInputFilter =
        object : PointerInputFilter() {

            private var currentPointerEvent: PointerEvent? = null

            private var state = DispatchToViewState.Unknown

            override fun onPointerEvent(
                pointerEvent: PointerEvent,
                pass: PointerEventPass,
                bounds: IntSize
            ) {
                /*
                 * If the event was a down or up event, we dispatch to platform as early as possible.
                 * If the event is a move event, and we can still intercept, we dispatch to platform after
                 * we have a chance to intercept due to movement.
                 *
                 * See Android's PointerInteropFilter as original source for this logic.
                 */
                val dispatchDuringInitialTunnel = pointerEvent.changes.fastAny {
                    it.changedToDownIgnoreConsumed() || it.changedToUpIgnoreConsumed()
                }
                if (pass == PointerEventPass.Initial && dispatchDuringInitialTunnel) {
                    dispatchToView(pointerEvent)
                }
                if (pass == PointerEventPass.Final && !dispatchDuringInitialTunnel) {
                    dispatchToView(pointerEvent)
                }
            }

            override fun onCancel() {
                // If we are still dispatching to the Android View, we have to send them a
                // cancel event, otherwise, we should not.
                if (state === DispatchToViewState.Dispatching) {
                    onTouchEvent(currentPointerEvent?.nativeEvent)
                    reset()
                }
            }

            /**
             * Resets all of our state to be ready for a "new event stream".
             */
            private fun reset() {
                state = DispatchToViewState.Unknown
            }

            /**
             * Dispatches to the Android View.
             *
             * Also consumes aspects of [pointerEvent] and updates our [state] accordingly.
             *
             * Will dispatch ACTION_CANCEL if any aspect of [pointerEvent] has been consumed and
             * update our [state] accordingly.
             *
             * @param pointerEvent The change to dispatch.
             * @return The resulting changes (fully consumed or untouched).
             */
            private fun dispatchToView(pointerEvent: PointerEvent) {

                val changes = pointerEvent.changes

                if (changes.fastAny { it.isConsumed }) {
                    // We should no longer dispatch to the Android View.
                    if (state === DispatchToViewState.Dispatching) {
                        // If we were dispatching, send ACTION_CANCEL.
                        onTouchEvent(pointerEvent.nativeEvent)
                    }
                    state = DispatchToViewState.NotDispatching
                } else {
                    // Dispatch and update our state with the result.
                    if (pointerEvent.type == PointerEventType.Press) {
                        // If the action is ACTION_DOWN, we care about the return value of
                        // onTouchEvent and use it to set our initial dispatching state.
                        state = if (onTouchEvent(pointerEvent.nativeEvent)) {
                            DispatchToViewState.Dispatching
                        } else {
                            DispatchToViewState.NotDispatching
                        }
                    } else {
                        // Otherwise, we don't care about the return value. This is intended
                        // to be in accordance with how the Android View system works.
                        onTouchEvent(pointerEvent.nativeEvent)
                    }
                    if (state === DispatchToViewState.Dispatching) {
                        // If the Android View claimed the event, consume all changes.
                        changes.fastForEach {
                            it.consume()
                        }
                    }
                }
            }
        }
}
