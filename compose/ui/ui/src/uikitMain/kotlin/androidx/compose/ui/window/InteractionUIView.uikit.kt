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

package androidx.compose.ui.window

import androidx.compose.ui.uikit.utils.CMPGestureRecognizer
import androidx.compose.ui.uikit.utils.CMPGestureRecognizerHandlerProtocol
import androidx.compose.ui.viewinterop.InteropView
import kotlinx.cinterop.CValue
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectZero
import platform.UIKit.UIEvent
import platform.UIKit.UIGestureRecognizer
import platform.UIKit.UIGestureRecognizerState
import platform.UIKit.UIGestureRecognizerStateBegan
import platform.UIKit.UIGestureRecognizerStateCancelled
import platform.UIKit.UIGestureRecognizerStateChanged
import platform.UIKit.UIGestureRecognizerStateEnded
import platform.UIKit.UIGestureRecognizerStateFailed
import platform.UIKit.UIGestureRecognizerStatePossible
import platform.UIKit.UIPress
import platform.UIKit.UIPressesEvent
import platform.UIKit.UITouch
import platform.UIKit.UITouchPhase
import platform.UIKit.UIView
import platform.UIKit.setState
import platform.darwin.NSObject

/**
 * Subset of [UITouchPhase] reflecting immediate phase when event is received by the [UIView] or
 * [UIGestureRecognizer].
 */
internal enum class CupertinoTouchesPhase {
    BEGAN, MOVED, ENDED, CANCELLED
}

fun dbgLog(msg: String) {
    println("DBG: $msg")
}

private val UIGestureRecognizerState.isOngoing: Boolean
    get() =
        when (this) {
            UIGestureRecognizerStateBegan, UIGestureRecognizerStateChanged -> true
            else -> false
        }

private class GestureRecognizerHandlerImpl(
    private var onTouchesEvent: (view: UIView, touches: Set<*>, event: UIEvent, phase: CupertinoTouchesPhase) -> Unit,
    private var view: UIView?,
    private val onTouchesCountChanged: (by: Int) -> Unit,
): NSObject(), CMPGestureRecognizerHandlerProtocol {
    /**
     * The actual view that was hit-tested by the first touch in the sequence.
     * It could be interop view, for example. If there are tracked touches assignment is ignored.
     */
    var hitTestView: UIView? = null
        set(value) {
            /**
             * Only remember the first hit-tested view in the sequence.
             */
            if (initialLocation == null) {
                field = value
            }
        }

    /**
     * [CMPGestureRecognizer] that is associated with this handler.
     */
    var gestureRecognizer: CMPGestureRecognizer? = null

    private var state: UIGestureRecognizerState
        get() = gestureRecognizer?.state ?: UIGestureRecognizerStateFailed
        set(value) {
            gestureRecognizer?.setState(value)
        }

    /**
     * Initial centroid location in the sequence to measure the motion slop and to determine whether the gesture
     * should be recognized or failed and pass touches to interop views.
     */
    private var initialLocation: CValue<CGPoint>? = null

    /**
     * Touches that are currently tracked by the gesture recognizer.
     */
    private val trackedTouches: MutableSet<UITouch> = mutableSetOf()

    /**
     * Checks whether the centroid location of [trackedTouches] has exceeded the scrolling slop
     * relative to [initialLocation]
     */
    private val isLocationDeltaAboveSlope: Boolean
        get() {
            val initialLocation = initialLocation ?: return false
            val centroidLocation = trackedTouchesCentroidLocation ?: return false

            val slop = 10.0

            val dx = centroidLocation.useContents { x - initialLocation.useContents { x } }
            val dy = centroidLocation.useContents { y - initialLocation.useContents { y } }

            return dx * dx + dy * dy > slop * slop
        }

    /**
     * Calculates the centroid of the tracked touches.
     */
    private val trackedTouchesCentroidLocation: CValue<CGPoint>?
        get() {
            if (trackedTouches.isEmpty()) {
                return null
            }

            var centroidX = 0.0
            var centroidY = 0.0

            for (touch in trackedTouches) {
                val location = touch.locationInView(view)
                location.useContents {
                    centroidX += x
                    centroidY += y
                }
            }

            return CGPointMake(
                x = centroidX / trackedTouches.size.toDouble(),
                y = centroidY / trackedTouches.size.toDouble()
            )
        }

    /**
     * Implementation of [CMPGestureRecognizerHandlerProtocol] that handles touchesBegan event and
     * forwards it here.
     *
     * There are following scenarios:
     * 1. Those are first touches in the sequence, the interaction view is hit-tested. In this case, we
     * should start the gesture recognizer immediately and start passing touches to the Compose
     * runtime.
     *
     * 2. Those are first touches in the sequence, an interop view is hit-tested. In this case we
     * intecept touches from interop views until the gesture recognizer is explicitly failed.
     * See [UIGestureRecognizer.delaysTouchesBegan]. In the same time we schedule a failure in
     * [CMPGestureRecognizer.scheduleFailure], which will pass intercepted touches to the interop
     * views if the gesture recognizer is not recognized within a certain time frame
     * (UIScrollView reverse-engineered 150ms is used).
     * The similar approach is used by [UIScrollView](https://developer.apple.com/documentation/uikit/uiscrollview?language=objc)
     *
     * 3. Those are not the first touches in the sequence. A gesture is recognized.
     * We should continue with scenario (1), we don't yet support multitouch sequence in
     * compose and interop view simultaneously (e.g. scrolling native horizontal
     * scroll and compose horizontal scroll with different fingers)
     *
     * 4. Those are not the first touches in the sequence. A gesture is not recognized.
     * See if centroid of the tracked touches has moved enough to recognize the gesture.
     *
     */
    override fun touchesBegan(touches: Set<*>, withEvent: UIEvent?) {
        val areTouchesInitial = startTrackingTouches(touches)

        val onTouchesEvent = onTouchesEventCallbackForPhase(touches, withEvent, CupertinoTouchesPhase.BEGAN)

        if (state.isOngoing || hitTestView == view) {
            // Golden path, immediately start/continue the gesture recognizer if possible and pass touches.
            when (state) {
                UIGestureRecognizerStatePossible -> {
                    state = UIGestureRecognizerStateBegan

                    onTouchesEvent()
                }

                UIGestureRecognizerStateBegan, UIGestureRecognizerStateChanged -> {
                    state = UIGestureRecognizerStateChanged

                    onTouchesEvent()
                }
            }
        } else {
            if (areTouchesInitial) {
                // We are in the scenario (2), we should schedule failure and pass touches to the
                // interop view.
                gestureRecognizer?.scheduleFailure()

                onTouchesEvent()
            } else {
                // We are in the scenario (4), check if the gesture recognizer should be recognized.
                checkPanIntent()

                onTouchesEvent()
            }
        }
    }

    /**
     * Implementation of [CMPGestureRecognizerHandlerProtocol] that handles touchesMoved event and
     * forwards it here.
     *
     * There are following scenarios:
     * 1. The interaction view is hit-tested, or a gesture is recognized.
     * In this case, we should just forward the touches.
     *
     * 2. An interop view is hit-tested. In this case we should check if the pan intent is met.
     */
    override fun touchesMoved(touches: Set<*>, withEvent: UIEvent?) {
        val onTouchesEvent = onTouchesEventCallbackForPhase(touches, withEvent, CupertinoTouchesPhase.MOVED)

        if (state.isOngoing || hitTestView == view) {
            // Golden path, just update the gesture recognizer state and pass touches to
            // the Compose runtime.

            when (state) {
                UIGestureRecognizerStateBegan, UIGestureRecognizerStateChanged -> {
                    state = UIGestureRecognizerStateChanged
                    onTouchesEvent()
                }
            }
        } else {
            checkPanIntent()

            onTouchesEvent()
        }
    }

    /**
     * Implementation of [CMPGestureRecognizerHandlerProtocol] that handles touchesEnded event and
     * forwards it here.
     *
     * There are following scenarios:
     * 1. The interaction view is hit-tested, or a gesture is recognized. Just update the gesture
     * recognizer state and pass touches to the Compose runtime.
     *
     * 2. An interop view is hit-tested. In this case if there are no tracked touches left -
     * we need to allow all the touches to be passed to the interop view by failing explicitly.
     */
    override fun touchesEnded(touches: Set<*>, withEvent: UIEvent?) {
        stopTrackingTouches(touches)

        val onTouchesEvent = onTouchesEventCallbackForPhase(touches, withEvent, CupertinoTouchesPhase.ENDED)

        if (state.isOngoing || hitTestView == view) {
            // Golden path, just update the gesture recognizer state and pass touches to
            // the Compose runtime.

            when (state) {
                UIGestureRecognizerStateBegan, UIGestureRecognizerStateChanged -> {
                    if (trackedTouches.isEmpty()) {
                        state = UIGestureRecognizerStateEnded
                        onTouchesEvent()
                    } else {
                        state = UIGestureRecognizerStateChanged
                        onTouchesEvent()
                    }
                }
            }
        } else {
            if (trackedTouches.isEmpty()) {
                // Those were the last touches in the sequence
                // Explicitly pass them as cancelled to Compose
                onTouchesEventCallbackForPhase(
                    touches,
                    withEvent,
                    CupertinoTouchesPhase.CANCELLED
                ).invoke()

                // Explicitly fail the gesture, cancelling a scheduled failure
                gestureRecognizer?.cancelFailure()

                state = UIGestureRecognizerStateFailed
            } else {
                onTouchesEvent()
            }
        }
    }

    override fun touchesCancelled(touches: Set<*>, withEvent: UIEvent?) {
        stopTrackingTouches(touches)

        val onTouchesEvent = onTouchesEventCallbackForPhase(touches, withEvent, CupertinoTouchesPhase.CANCELLED)

        if (hitTestView == view) {
            // Golden path, just update the gesture recognizer state and pass touches to
            // the Compose runtime.

            when (state) {
                UIGestureRecognizerStateBegan, UIGestureRecognizerStateChanged -> {
                    if (trackedTouches.isEmpty()) {
                        state = UIGestureRecognizerStateCancelled
                        onTouchesEvent()
                    } else {
                        state = UIGestureRecognizerStateChanged
                        onTouchesEvent()
                    }
                }
            }
        } else {
            onTouchesEvent()

            if (trackedTouches.isEmpty()) {
                // Those were the last touches in the sequence
                // Explicitly fail the gesture, cancelling a scheduled failure
                gestureRecognizer?.cancelFailure()

                state = UIGestureRecognizerStateFailed
            }
        }
    }

    /**
     * Implementation of [CMPGestureRecognizerHandlerProtocol] that handles the failure of
     * the gesture if it's not recognized within the certain time frame.
     *
     * It means we need to pass all the tracked touches to the runtime as cancelled and set failed
     * state on the gesture recognizer.
     *
     * Intercepted touches will be passed to the interop views by UIKit due to
     * [UIGestureRecognizer.delaysTouchesBegan]
     */
    override fun onFailure() {
        state = UIGestureRecognizerStateFailed

        // We won't receive other touches events until all fingers are lifted, so we can't rely
        // on touchesEnded/touchesCancelled to reset the state.  We need to immediately notify
        // the runtime about the cancelled touches and reset the state manually
        onTouchesEventCallbackForPhase(trackedTouches, null, CupertinoTouchesPhase.CANCELLED).invoke()
        stopTrackingTouches(trackedTouches)
    }

    override fun shouldRecognizeSimultaneously(first: UIGestureRecognizer, withOther: UIGestureRecognizer): Boolean {
        val gestureRecognizer = gestureRecognizer ?: return false

        return when {
            first == gestureRecognizer -> {
                shouldRecognizeSimultaneously(
                    first = gestureRecognizer,
                    second = withOther
                )
            }
            withOther == gestureRecognizer -> {
                shouldRecognizeSimultaneously(
                    first = gestureRecognizer,
                    second = first
                )
            }
            else -> false
        }
    }

    /**
     * Intentionally clean up all dependencies of GestureRecognizerHandlerImpl to prevent retain cycles that
     * can be caused by implicit capture of the view by UIKit objects (such as UIEvent).
     */
    fun dispose() {
        onTouchesEvent = { _, _, _, _ -> }
        gestureRecognizer = null
        trackedTouches.clear()
    }

    /**
     * Determines if the Compose [CMPGestureRecognizer] should recognize simultaneously with the other [UIGestureRecognizer].
     * @param first The Compose gesture recognizer.
     * @param second The other UIKit gesture recognizer.
     * @return `true` if the gesture recognizers should recognize simultaneously, `false` otherwise.
     * @note
     */
    private fun shouldRecognizeSimultaneously(first: CMPGestureRecognizer, second: UIGestureRecognizer): Boolean {
        val firstView = first.view ?: return false
        val secondView = second.view ?: return false

        return if (secondView.isDescendantOfView(firstView)) {
            // If other gesture recognizer belongs to interop view, then it means that it should
            // intercept all the touches
            false
        } else {
            // As of now, we unconditionally allow simultaneous recognition with UIKit gesture
            // recognizers from super views.
            true
        }
    }

    /**
     * Starts tracking the given touches. Remember initial location if those are the first touches
     * in the sequence.
     * @return `true` if the touches are initial, `false` otherwise.
     */
    private fun startTrackingTouches(touches: Set<*>): Boolean {
        onTouchesCountChanged(touches.size)

        val areTouchesInitial = trackedTouches.isEmpty()

        for (touch in touches) {
            trackedTouches.add(touch as UITouch)
        }

        if (areTouchesInitial) {
            initialLocation = trackedTouchesCentroidLocation
        }

        return areTouchesInitial
    }

    /**
     * Check if the tracked touches have moved enough to recognize the gesture.
     */
    private fun checkPanIntent() {
        if (isLocationDeltaAboveSlope) {
            gestureRecognizer?.cancelFailure()
            state = UIGestureRecognizerStateBegan
        }
    }

    /**
     * Stops tracking the given touches. If there are no tracked touches left, reset the initial
     * location to null.
     */
    private fun stopTrackingTouches(touches: Set<*>) {
        onTouchesCountChanged(-touches.size)

        for (touch in touches) {
            trackedTouches.remove(touch as UITouch)
        }

        if (trackedTouches.isEmpty()) {
            initialLocation = null
        }
    }

    /**
     * Curry the [onTouchesEvent] callback with the given [touches], [event], and [phase].
     */
    private fun onTouchesEventCallbackForPhase(
        touches: Set<*>,
        event: UIEvent?,
        phase: CupertinoTouchesPhase
    ): () -> Unit =
        block@{
            val view = view ?: return@block
            val nonNullEvent = event ?: return@block

            onTouchesEvent(view, touches, nonNullEvent, phase)
        }
}

/**
 * [UIView] subclass that handles touches and keyboard presses events and forwards them
 * to the Compose runtime.
 *
 * @param hitTestInteropView A callback to find an [InteropView] at the given point.
 * @param onTouchesEvent A callback to notify the Compose runtime about touch events.
 * @param onTouchesCountChange A callback to notify the Compose runtime about the number of tracked
 * touches.
 * @param inInteractionBounds A callback to check if the given point is within the interaction
 * bounds as defined by the owning implementation.
 * @param onKeyboardPresses A callback to notify the Compose runtime about keyboard presses.
 * The parameter is a [Set] of [UIPress] objects. Erasure happens due to K/N not supporting Obj-C
 * lightweight generics.
 */
internal class InteractionUIView(
    private var hitTestInteropView: (point: CValue<CGPoint>, event: UIEvent?) -> InteropView?,
    onTouchesEvent: (view: UIView, touches: Set<*>, event: UIEvent?, phase: CupertinoTouchesPhase) -> Unit,
    private var onTouchesCountChange: (count: Int) -> Unit,
    private var inInteractionBounds: (CValue<CGPoint>) -> Boolean,
    private var onKeyboardPresses: (Set<*>) -> Unit,
) : UIView(CGRectZero.readValue()) {
    private val gestureRecognizerHandler = GestureRecognizerHandlerImpl(
        view = this,
        onTouchesEvent = onTouchesEvent,
        onTouchesCountChanged = { touchesCount += it }
    )

    private val gestureRecognizer = CMPGestureRecognizer()

    /**
     * When there at least one tracked touch, we need notify redrawer about it. It should schedule
     * CADisplayLink which affects frequency of polling UITouch events on high frequency display
     * and forces it to match display refresh rate.
     */
    private var touchesCount = 0
        set(value) {
            field = value
            onTouchesCountChange(value)
        }

    init {
        multipleTouchEnabled = true
        userInteractionEnabled = true

        // When CMPGestureRecognizer is recognized, immediately cancel all touches in the subviews.
        gestureRecognizer.cancelsTouchesInView = true

        // Delays touches reception by underlying views until the gesture recognizer is explicitly
        // stated as failed (aka, the touch sequence is targeted to the interop view).
        gestureRecognizer.delaysTouchesBegan = true

        addGestureRecognizer(gestureRecognizer)
        gestureRecognizer.handler = gestureRecognizerHandler
        gestureRecognizerHandler.gestureRecognizer = gestureRecognizer
    }

    override fun canBecomeFirstResponder() = true

    override fun pressesBegan(presses: Set<*>, withEvent: UIPressesEvent?) {
        onKeyboardPresses(presses)
        super.pressesBegan(presses, withEvent)
    }

    override fun pressesEnded(presses: Set<*>, withEvent: UIPressesEvent?) {
        onKeyboardPresses(presses)
        super.pressesEnded(presses, withEvent)
    }

    // TODO: inspect if touches should be forwarded further up the responder chain
    //  via super call or they considered to be consumed by this view

    override fun touchesBegan(touches: Set<*>, withEvent: UIEvent?) {
        super.touchesBegan(touches, withEvent)
    }

    override fun touchesMoved(touches: Set<*>, withEvent: UIEvent?) {
        super.touchesMoved(touches, withEvent)
    }

    override fun touchesEnded(touches: Set<*>, withEvent: UIEvent?) {
        super.touchesEnded(touches, withEvent)
    }

    override fun touchesCancelled(touches: Set<*>, withEvent: UIEvent?) {
        super.touchesCancelled(touches, withEvent)
    }

    override fun hitTest(point: CValue<CGPoint>, withEvent: UIEvent?): UIView? = rememberHitTestResult {
        if (!inInteractionBounds(point)) {
            null
        } else {
            // Find if a scene contains a [InteropViewAnchorModifierNode] at the given point.
            val interopView = hitTestInteropView(point, withEvent)

            if (interopView == null) {
                // Native [hitTest] happens after [pointInside] is checked. If hit testing
                // inside ComposeScene didn't yield any interop view, then we should return [this]
                this
            } else {
                // Transform the point to the interop view's coordinate system.
                // And perform native [hitTest] on the interop view.
                // Return this view if the interop view doesn't handle the hit test.
                interopView.hitTest(
                    point = convertPoint(point, toView = interopView),
                    withEvent = withEvent
                ) ?: this
            }
        }
    }

    /**
     * Intentionally clean up all dependencies of InteractionUIView to prevent retain cycles that
     * can be caused by implicit capture of the view by UIKit objects (such as UIEvent).
     */
    fun dispose() {
        gestureRecognizerHandler.dispose()
        gestureRecognizer.handler = null
        removeGestureRecognizer(gestureRecognizer)

        hitTestInteropView = { _, _ -> null }

        onTouchesCountChange = {}
        inInteractionBounds = { false }
        onKeyboardPresses = {}
    }

    /**
     * Execute the given [hitTestBlock] and save the result to the gesture recognizer handler, so
     * that it can be used later to determine if the gesture recognizer should be recognized
     * or failed.
     */
    private fun rememberHitTestResult(hitTestBlock: () -> UIView?): UIView? {
        val result = hitTestBlock()
        gestureRecognizerHandler.hitTestView = result

        dbgLog("hitTestView: $result")
        return result
    }
}
