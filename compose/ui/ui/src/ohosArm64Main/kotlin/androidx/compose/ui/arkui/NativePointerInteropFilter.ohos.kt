package androidx.compose.ui.arkui

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputFilter
import androidx.compose.ui.input.pointer.PointerInputModifier
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.interop.arkc.ArkUIInputEvent
import androidx.compose.ui.interop.arkc.InteropWrapper
import androidx.compose.ui.interop.arkc.action
import androidx.compose.ui.interop.arkc.postClonedEvent
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import platform.arkui.ARKUI_ERROR_CODE_NO_ERROR
import platform.arkui.UI_TOUCH_EVENT_ACTION_DOWN

@ExperimentalComposeUiApi
class NativeRequestDisallowInterceptTouchEvent : (Boolean) -> Unit {
    internal var pointerInteropFilter: NativePointerInteropFilter? = null

    override fun invoke(disallowIntercept: Boolean) {
        pointerInteropFilter?.disallowIntercept = disallowIntercept
    }
}

@ExperimentalComposeUiApi
internal fun Modifier.pointerInteropFilter(view: InteropWrapper): Modifier {
    val filter = NativePointerInteropFilter()
    filter.onNativeTouchEvent = { event ->
        val result = view.handle.postClonedEvent(event)
        val consumed = result == ARKUI_ERROR_CODE_NO_ERROR.toInt()
        consumed
    }
    val requestDisallowInterceptTouchEvent = NativeRequestDisallowInterceptTouchEvent()
    filter.requestDisallowInterceptTouchEvent = requestDisallowInterceptTouchEvent
    view.onRequestDisallowInterceptTouchEvent = requestDisallowInterceptTouchEvent
    return this.then(filter)
}

@ExperimentalComposeUiApi
internal class NativePointerInteropFilter : PointerInputModifier {

    lateinit var onNativeTouchEvent: (ArkUIInputEvent) -> Boolean

    var requestDisallowInterceptTouchEvent: NativeRequestDisallowInterceptTouchEvent? = null
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
                                pointerEvent.toCancelNativeTouchEventScope(
                                    this.layoutCoordinates?.localToRoot(Offset.Zero)
                                        ?: error("layoutCoordinates not set")
                                ) { nativeEvent ->
                                    onNativeTouchEvent(nativeEvent)
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
                        pointerEvent.toCancelNativeTouchEventScope(
                            this.layoutCoordinates?.localToRoot(Offset.Zero)
                                ?: error("layoutCoordinates not set")
                        ) { nativeEvent ->
                            onNativeTouchEvent(nativeEvent)
                        }
                    }
                    state = DispatchToViewState.NotDispatching
                } else {
                    // Dispatch and update our state with the result.
                    pointerEvent.toNativeTouchEventScope(
                        this.layoutCoordinates?.localToRoot(Offset.Zero)
                            ?: error("layoutCoordinates not set")
                    ) { nativeEvent ->
                        if (nativeEvent.action.toUInt() == UI_TOUCH_EVENT_ACTION_DOWN) {
                            // If the action is ACTION_DOWN, we care about the return value of
                            // onTouchEvent and use it to set our initial dispatching state.
                            state = if (onNativeTouchEvent(nativeEvent)) {
                                consumeChanges = true
                                DispatchToViewState.Dispatching
                            } else {
                                DispatchToViewState.NotDispatching
                            }
                        } else {
                            // Otherwise, we don't care about the return value. This is intended
                            // to be in accordance with how the Android View system works.
                            onNativeTouchEvent(nativeEvent)
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
