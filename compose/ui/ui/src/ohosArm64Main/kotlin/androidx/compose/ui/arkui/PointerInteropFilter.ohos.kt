/*
 * Tencent is pleased to support the open source community by making ovCompose available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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

package androidx.compose.ui.arkui

import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputFilter
import androidx.compose.ui.input.pointer.PointerInputModifier
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.interop.arkc.InteropWrapper
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach

/**
 * A special PointerInputModifier that provides access to the underlying [TouchEvent]s originally
 * dispatched to Compose. Prefer [pointerInput] and use this only for interoperation with
 * existing code that consumes [TouchEvent]s.
 *
 * While the main intent of this Modifier is to allow arbitrary code to access the original
 * [TouchEvent] dispatched to Compose, for completeness, analogs are provided to allow arbitrary
 * code to interact with the system as if it were an Android View.
 *
 * This includes 2 APIs,
 *
 * 1. [onTouchEvent] has a Boolean return type which is akin to the return type of
 * [View.onTouchEvent]. If the provided [onTouchEvent] returns true, it will continue to receive
 * the event stream (unless the event stream has been intercepted) and if it returns false, it will
 * not.
 *
 * 2. [requestDisallowInterceptTouchEvent] is a lambda that you can optionally provide so that
 * you can later call it (yes, in this case, you call the lambda that you provided) which is akin
 * to calling [ViewParent.requestDisallowInterceptTouchEvent]. When this is called, any
 * associated ancestors in the tree that abide by the contract will act accordingly and will not
 * intercept the even stream.
 *
 * @see [View.onTouchEvent]
 * @see [ViewParent.requestDisallowInterceptTouchEvent]
 */
@ExperimentalComposeUiApi
fun Modifier.pointerInteropFilter(
    requestDisallowInterceptTouchEvent: (RequestDisallowInterceptTouchEvent)? = null,
    onTouchEvent: (TouchEvent) -> Boolean
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "pointerInteropFilter"
        properties["requestDisallowInterceptTouchEvent"] = requestDisallowInterceptTouchEvent
        properties["onTouchEvent"] = onTouchEvent
    }
) {
    val filter = remember { PointerInteropFilter() }
    filter.onTouchEvent = onTouchEvent
    filter.requestDisallowInterceptTouchEvent = requestDisallowInterceptTouchEvent
    filter
}

/**
 * Function that can be passed to [pointerInteropFilter] and then later invoked which provides an
 * analog to [ViewParent.requestDisallowInterceptTouchEvent].
 */
@ExperimentalComposeUiApi
class RequestDisallowInterceptTouchEvent : (Boolean) -> Unit {
    internal var pointerInteropFilter: PointerInteropFilter? = null

    override fun invoke(disallowIntercept: Boolean) {
        pointerInteropFilter?.disallowIntercept = disallowIntercept
    }
}

/**
 * Similar to the 2 argument overload of [pointerInteropFilter], but connects
 * directly to an [AndroidViewHolder] for more seamless interop with Android.
 */
@ExperimentalComposeUiApi
internal fun Modifier.pointerInteropFilter(view: ArkUIViewContainer): Modifier {
    val filter = PointerInteropFilter()

    val requestDisallowInterceptTouchEvent = RequestDisallowInterceptTouchEvent()
    filter.requestDisallowInterceptTouchEvent = requestDisallowInterceptTouchEvent

    filter.onTouchEvent = { touchEvent ->
        view.dispatchTouchEvent(touchEvent)
    }
    view.onRequestDisallowInterceptTouchEvent = requestDisallowInterceptTouchEvent
    return this.then(filter)
}

/** For interop placeholder, just consume the down event.*/
private val PointerInteropPlaceholderDispatcher = { touchEvent: TouchEvent ->
    touchEvent.type == TouchEvent.ACTION_DOWN
}

@ExperimentalComposeUiApi
internal fun Modifier.pointerInteropPlaceholderFilter(view: ArkUIViewContainer): Modifier {
    val filter = PointerInteropFilter()

    filter.onTouchEvent = PointerInteropPlaceholderDispatcher

    val requestDisallowInterceptTouchEvent = RequestDisallowInterceptTouchEvent()
    filter.requestDisallowInterceptTouchEvent = requestDisallowInterceptTouchEvent
    view.onRequestDisallowInterceptTouchEvent = requestDisallowInterceptTouchEvent
    return this.then(filter)
}

@ExperimentalComposeUiApi
internal fun Modifier.pointerInteropPlaceholderFilter(view: InteropWrapper): Modifier {
    val filter = PointerInteropFilter()
    filter.onTouchEvent = {
        when (it.type) {
            TouchEvent.ACTION_DOWN -> view.enabled = true
            TouchEvent.ACTION_UP, TouchEvent.ACTION_CANCEL -> view.enabled = false
        }
        PointerInteropPlaceholderDispatcher(it)
    }

    val requestDisallowInterceptTouchEvent = RequestDisallowInterceptTouchEvent()
    filter.requestDisallowInterceptTouchEvent = requestDisallowInterceptTouchEvent
    view.onRequestDisallowInterceptTouchEvent = requestDisallowInterceptTouchEvent
    return this.then(filter)
}

/**
 * The stateful part of pointerInteropFilter that manages the interop with Android.
 *
 * The intent of this PointerInputModifier is to allow Android Views and PointerInputModifiers to
 * interact seamlessly despite the differences in the 2 systems. Below is a detailed explanation
 * for how the interop is accomplished.
 *
 * When the type of event is not a movement event, we dispatch to the Android View as soon as
 * possible (during [PointerEventPass.Initial]) so that the Android View can react to down
 * and up events before Compose PointerInputModifiers normally would.
 *
 * When the type of event is a movement event, we dispatch to the Android View during
 * [PointerEventPass.Final] to allow Compose PointerInputModifiers to react to movement first,
 * which mimics a parent [ViewGroup] intercepting the event stream.
 *
 * Whenever we are about to call [onTouchEvent], we check to see if anything in Compose
 * consumed any aspect of the pointer input changes, and if they did, we intercept the stream and
 * dispatch ACTION_CANCEL to the Android View if they have already returned true for a call to
 * View#dispatchTouchEvent(...).
 *
 * If we do call [onTouchEvent], and it returns true, we consume all of the changes so that
 * nothing in Compose also responds.
 *
 * If the [requestDisallowInterceptTouchEvent] is provided and called with true, we simply dispatch move
 * events during [PointerEventPass.Initial] so that normal PointerInputModifiers don't get a
 * chance to consume first.  Note:  This does mean that it is possible for a Compose
 * PointerInputModifier to "intercept" even after requestDisallowInterceptTouchEvent has been
 * called because consumption can occur during [PointerEventPass.Initial].  This may seem
 * like a flaw, but in reality, any PointerInputModifier that consumes that aggressively would
 * likely only do so after some consumption already occurred on a later pass, and this ability to
 * do so is on par with a [ViewGroup]'s ability to override [ViewGroup.dispatchTouchEvent]
 * instead of overriding the more usual [ViewGroup.onTouchEvent] and [ViewGroup
 * .onInterceptTouchEvent].
 *
 * If [requestDisallowInterceptTouchEvent] is later called with false (the Android equivalent of
 * calling [ViewParent.requestDisallowInterceptTouchEvent] is exceedingly rare), we revert back to
 * the normal behavior.
 *
 * If all pointers go up on the pointer interop filter, parents will be set to be allowed to
 * intercept when new pointers go down. [requestDisallowInterceptTouchEvent] must be called again to
 * change that state.
 */
@ExperimentalComposeUiApi
internal class PointerInteropFilter : PointerInputModifier {

    lateinit var onTouchEvent: (TouchEvent) -> Boolean

    var requestDisallowInterceptTouchEvent: RequestDisallowInterceptTouchEvent? = null
        set(value) {
            field?.pointerInteropFilter = null
            field = value
            field?.pointerInteropFilter = this
        }
    internal var disallowIntercept = false

    /**
     * The 3 possible states
     */
    private enum class DispatchToViewState {
        /**
         * We have yet to dispatch a new event stream to the child Android View.
         */
        Unknown,

        /**
         * We have dispatched to the child Android View and it wants to continue to receive
         * events for the current event stream.
         */
        Dispatching,

        /**
         * We intercepted the event stream, or the Android View no longer wanted to receive
         * events for the current event stream.
         */
        NotDispatching
    }

    override val pointerInputFilter =
        object : PointerInputFilter() {

            private var state = DispatchToViewState.Unknown

            override val shareWithSiblings
                get() = true

            override fun onPointerEvent(
                pointerEvent: PointerEvent,
                pass: PointerEventPass,
                bounds: IntSize
            ) {
                val changes = pointerEvent.changes

                // If we were told to disallow intercept, or if the event was a down or up event,
                // we dispatch to Android as early as possible.  If the event is a move event and
                // we can still intercept, we dispatch to Android after we have a chance to
                // intercept due to movement.
                val dispatchDuringInitialTunnel = disallowIntercept ||
                        changes.fastAny {
                            it.changedToDownIgnoreConsumed() || it.changedToUpIgnoreConsumed()
                        }

                if (state !== DispatchToViewState.NotDispatching) {
                    if (pass == PointerEventPass.Initial && dispatchDuringInitialTunnel) {
                        dispatchToView(pointerEvent)
                    }
                    // Dispatch to view in main pass to be able to respond before outer dispatchers.
                    if (pass == PointerEventPass.Main && !dispatchDuringInitialTunnel) {
                        dispatchToView(pointerEvent)
                    }
                    // We also need to check other dispatching results in final pass
                    // to avoid conflicts with outer dispatchers.
                    if (pass == PointerEventPass.Final && !dispatchDuringInitialTunnel) {
                        if (changes.fastAny { it.isConsumed }) {
                            // We should no longer dispatch to the Android View.
                            if (state === DispatchToViewState.Dispatching) {
                                // If we were dispatching, send ACTION_CANCEL.
                                pointerEvent.toCancelTouchEventScope { touchEvent ->
                                    onTouchEvent(touchEvent)
                                }
                            }
                            state = DispatchToViewState.NotDispatching
                        }
                    }
                }
                if (pass == PointerEventPass.Final) {
                    // If all of the changes were up changes, then the "event stream" has ended
                    // and we reset.
                    if (changes.fastAll { it.changedToUpIgnoreConsumed() }) {
                        reset()
                    }
                }
            }

            override fun onCancel() {
                // If we are still dispatching to the Android View, we have to send them a
                // cancel event, otherwise, we should not.
                if (state === DispatchToViewState.Dispatching) {
                    // TODO(Cancel): dispatch cancel event to view.
                    reset()
                }
            }

            /**
             * Resets all of our state to be ready for a "new event stream".
             */
            private fun reset() {
                state = DispatchToViewState.Unknown
                disallowIntercept = false
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
                var consumeChanges = false
                val changes = pointerEvent.changes

                if (changes.fastAny { it.isConsumed }) {
                    // We should no longer dispatch to the Android View.
                    if (state === DispatchToViewState.Dispatching) {
                        // If we were dispatching, send ACTION_CANCEL.
                        pointerEvent.toCancelTouchEventScope { touchEvent ->
                            onTouchEvent(touchEvent)
                        }
                    }
                    state = DispatchToViewState.NotDispatching
                } else {
                    // Dispatch and update our state with the result.
                    pointerEvent.toTouchEventScope { touchEvent ->
                        if (touchEvent.type == TouchEvent.ACTION_DOWN) {
                            // If the action is ACTION_DOWN, we care about the return value of
                            // onTouchEvent and use it to set our initial dispatching state.
                            state = if (onTouchEvent(touchEvent)) {
                                consumeChanges = true
                                DispatchToViewState.Dispatching
                            } else {
                                DispatchToViewState.NotDispatching
                            }
                        } else {
                            // Otherwise, we don't care about the return value. This is intended
                            // to be in accordance with how the Android View system works.
                            onTouchEvent(touchEvent)
                        }
                    }
                    consumeChanges = consumeChanges || disallowIntercept
                    if (consumeChanges) {
                        // If the View claimed the event, consume all changes.
                        changes.fastForEach {
                            it.consume()
                        }
                    }
                }
            }
        }
}
