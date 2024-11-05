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

import androidx.compose.ui.platform.CUPERTINO_TOUCH_SLOP
import androidx.compose.ui.uikit.utils.CMPGestureRecognizer
import androidx.compose.ui.uikit.utils.CMPGestureRecognizerDelegateProxy
import androidx.compose.ui.viewinterop.InteropView
import androidx.compose.ui.viewinterop.InteropWrappingView
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlinx.cinterop.CValue
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectZero
import platform.UIKit.UIEvent
import platform.UIKit.UIGestureRecognizer
import platform.UIKit.UIGestureRecognizerDelegateProtocol
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
import platform.UIKit.UIView
import platform.UIKit.setState

/**
 * A reason for why touches are sent to Compose
 */
internal enum class TouchesEventKind {
    /**
     * [UIEvent] when `touchesBegan`
     */
    BEGAN,

    /**
     * [UIEvent] when `touchesMoved`
     */
    MOVED,

    /**
     * [UIEvent] when `touchesEnded`
     */
    ENDED,

    /**
     * [UIEvent] when `touchesCancelled`
     */
    CANCELLED,

    /**
     * Compose withdraws from processing touches. They are now processed by an interop view.
     */
    REDIRECTED
}

/**
 * An event of gesture lifecycle change.
 */
internal enum class GestureEvent {
    /**
     * First touch in the sequence just happened.
     */
    BEGAN,

    /**
     * No more touches are present.
     */
    ENDED
}

private val UIGestureRecognizerState.isOngoing: Boolean
    get() =
        when (this) {
            UIGestureRecognizerStateBegan, UIGestureRecognizerStateChanged -> true
            else -> false
        }

/**
 * Enum class representing the possible hit test result of [UserInputViewHitTestResult].
 * This enum is used solely to determine the strategy of touch event delivery and
 * doesn't require any additional information about the hit-tested view itself.
 */
private sealed interface UserInputViewHitTestResult {
    data object Self : UserInputViewHitTestResult

    data object NonCooperativeChildView : UserInputViewHitTestResult

    /**
     * Hit test result is Cooperative child view, that allows a delay of [delayMillis] milliseconds.
     */
    class CooperativeChildView(
        val delayMillis: Int
    ) : UserInputViewHitTestResult {
        val delaySeconds: Double
            get() = delayMillis.toDouble() / 1000.0
    }
}

/**
 * An implementation of [UIGestureRecognizerDelegateProtocol] methods exposed by
 * [CMPGestureRecognizerDelegateProxy].
 */
private class UserInputGestureRecognizerDelegateProxy : CMPGestureRecognizerDelegateProxy() {
    override fun gestureRecognizerShouldRecognizeSimultaneouslyWithGestureRecognizer(
        gestureRecognizer: UIGestureRecognizer,
        otherGestureRecognizer: UIGestureRecognizer
    ): Boolean {
        // We should recognize simultaneously only with the gesture recognizers
        // belonging to itself or to the views up in the hierarchy.

        // Can't check if either view is null
        val view = gestureRecognizer.view ?: return false
        val otherView = otherGestureRecognizer.view ?: return false

        val otherIsAscendant = !otherView.isDescendantOfView(view)

        // Only allow simultaneous recognition if the other gesture recognizer is attached to the same view
        // or to a view up in the hierarchy
        return otherView == view || otherIsAscendant
    }

    override fun gestureRecognizerShouldRequireFailureOfGestureRecognizer(
        gestureRecognizer: UIGestureRecognizer,
        otherGestureRecognizer: UIGestureRecognizer
    ): Boolean {
        // We don't require other gesture recognizers to fail.
        // Assumption is that we recognize
        // simultaneously with the gesture recognizers of the views up in the hierarchy.
        // And gesture recognizers down the hierarchy require to failure us.
        return false
    }

    override fun gestureRecognizerShouldBeRequiredToFailByGestureRecognizer(
        gestureRecognizer: UIGestureRecognizer,
        otherGestureRecognizer: UIGestureRecognizer
    ): Boolean {
        // Other gesture recognizers,
        // except the case where it belongs to the same view,
        // are required to wait until we fail.
        // In practice, it can only happen when other gesture recognizers are attached to the
        // descendant views (aka interop views).
        // In other cases, it's allowed to recognize simultaneously, so this method will not be
        // called
        return gestureRecognizer.view != otherGestureRecognizer.view
    }
}

/**
 * Implementation of [CMPGestureRecognizer] that handles touch events and forwards
 * them. The main difference from the original [UIView] touches based is that it's built on top of
 * [CMPGestureRecognizer], which play differently with UIKit touches processing and are required
 * for the correct handling of the touch events in interop scenarios, because they rely on
 * [UIGestureRecognizer] failure requirements and touches interception, which is an exclusive way
 * to control touches delivery to [UIView]s and their [UIGestureRecognizer]s in a fine-grain manner.
 */
private class UserInputGestureRecognizer(
    private var onTouchesEvent: (view: UIView, touches: Set<*>, event: UIEvent?, phase: TouchesEventKind) -> Unit,
    private var onGestureEvent: (GestureEvent) -> Unit
) : CMPGestureRecognizer(target = null, action = null) {
    private val delegateProxy = UserInputGestureRecognizerDelegateProxy()

    /**
     * The actual view that was hit-tested by the first touch in the sequence.
     * It could be an interop view, for example.
     * If there are tracked touches, the assignment is ignored.
     */
    var hitTestResult: UserInputViewHitTestResult? = null
        set(value) {
            /**
             * Only remember the first hit-tested view in the sequence.
             */
            if (initialLocation == null) {
                field = value
            }
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
     * Scheduled job for the gesture recognizer failure.
     */
    private var failureJob: Job? = null

    init {
        // Assign delegateProxy to be the delegate of this gesture recognizer.
        // Delegates are weakly referenced in UIKit,
        // so we need to keep a reference to it as a property.
        delegate = delegateProxy

        // When recognized, immediately cancel all touches in the subviews.
        // This scenario shouldn't happen due to `delaysTouchesBegan`, so it's
        // more of a defensive line.
        cancelsTouchesInView = true

        // Delays touches reception by underlying views until the gesture recognizer is explicitly
        // stated as failed (aka, the touch sequence is targeted to the interop view).
        delaysTouchesBegan = true
    }

    /**
     * Checks whether the centroid location of [trackedTouches] has exceeded the scrolling slop
     * relative to [initialLocation]
     */
    private val isLocationDeltaAboveSlop: Boolean
        get() {
            val initialLocation = initialLocation ?: return false
            val centroidLocation = trackedTouchesCentroidLocation ?: return false

            val slop = CUPERTINO_TOUCH_SLOP.toDouble()

            val dx = centroidLocation.useContents { x - initialLocation.useContents { x } }
            val dy = centroidLocation.useContents { y - initialLocation.useContents { y } }

            return dx * dx + dy * dy > slop * slop
        }

    /**
     * Calculates the centroid of the tracked touches.
     */
    private val trackedTouchesCentroidLocation: CValue<CGPoint>?
        get() = view?.let {
            trackedTouches.centroidLocationInView(it)
        }

    /**
     * There are following scenarios:
     *
     * 1. Those are first touches in the sequence, [UserInputView] is hit-tested.
     * In this case we should start the gesture recognizer immediately
     * and start forwarding touches to the Compose runtime.
     *
     * 2. Those are first touches in the sequence, an interop view is hit-tested.
     * In this case we intercept touches from interop views until the gesture recognizer is
     * explicitly failed.
     * See [UIGestureRecognizer.delaysTouchesBegan].
     * In the same time we schedule a failure in
     * [scheduleFailure], which will pass intercepted touches to the interop views
     * if the gesture is not recognized within a time frame allowed by hit tested cooperative
     * child view.
     * The similar approach is used by [UIScrollView](https://developer.apple.com/documentation/uikit/uiscrollview)
     *
     * 3. Those are not the first touches in the sequence.
     * A gesture is recognized.
     * We should continue with scenario (1), we don't yet support multitouch sequence in
     * compose and interop view simultaneously (e.g. scrolling native horizontal
     * scroll and compose horizontal scroll with different fingers)
     *
     * 4. Those are not the first touches in the sequence.
     * A gesture is not recognized.
     * See if centroid of the tracked touches has moved enough to recognize the gesture.
     *
     * TODO (https://youtrack.jetbrains.com/issue/CMP-5877/iOS-non-eager-touch-interception):
     *  An improvement to the current implementation would be to remove the delay if hitTest
     *  on ComposeScene didn't go through a node, that has a PointerFilter attached
     *  (e.g. a scrollable)
     */
    override fun touchesBegan(touches: Set<*>, withEvent: UIEvent) {
        if (hitTestResult == UserInputViewHitTestResult.NonCooperativeChildView) {
            // If the child view opts out of the delay logic, the gesture should fail immediately.
            // Consequently, touches will be forwarded straight to the interop child view,
            // bypassing Compose.
            // Compose will not receive any touches until all fingers are lifted.
            setState(UIGestureRecognizerStateFailed)
            return
        }

        val areTouchesInitial = startTrackingTouches(touches)

        onTouchesEvent(trackedTouches, withEvent, TouchesEventKind.BEGAN)

        if (state.isOngoing || hitTestResult == UserInputViewHitTestResult.Self) {
            // Golden path, immediately start/continue the gesture recognizer if possible and pass touches.
            when (state) {
                UIGestureRecognizerStatePossible -> {
                    setState(UIGestureRecognizerStateBegan)
                }

                UIGestureRecognizerStateBegan, UIGestureRecognizerStateChanged -> {
                    setState(UIGestureRecognizerStateChanged)
                }
            }
        } else {
            if (areTouchesInitial) {
                // We are in the scenario (2), we should schedule failure and pass touches to the
                // interop view.
                when (val cooperativeChildView = hitTestResult) {
                    is UserInputViewHitTestResult.CooperativeChildView ->
                        scheduleFailure(cooperativeChildView.delaySeconds)
                    else -> {}
                }
            } else {
                // We are in the scenario (4), check if the gesture recognizer should be recognized.
                checkPanIntent()
            }
        }
    }

    /**
     * There are the following scenarios:
     *
     * 1. The [UserInputView] is hit-tested, or a gesture is already recognized.
     * In this case, we should just forward the touches.
     *
     * 2. An interop view is hit-tested. In this case we should check if the pan intent is met.
     */
    override fun touchesMoved(touches: Set<*>, withEvent: UIEvent) {
        onTouchesEvent(trackedTouches, withEvent, TouchesEventKind.MOVED)

        if (state.isOngoing || hitTestResult == UserInputViewHitTestResult.Self) {
            // Golden path, just update the gesture recognizer state and pass touches to
            // the Compose runtime.

            when (state) {
                UIGestureRecognizerStateBegan, UIGestureRecognizerStateChanged -> {
                    setState(UIGestureRecognizerStateChanged)
                }
            }
        } else {
            checkPanIntent()
        }
    }

    /**
     * There are the following scenarios:
     *
     * 1. The [UserInputView] is hit-tested, or a gesture is already recognized.
     * Update the gesture recognizer state and forward touches to the Compose runtime.
     *
     * 2. An interop view is hit-tested.
     * In this case if there are no tracked touches left, we need to allow all the touches to be
     * passed to the interop view by failing explicitly.
     */
    override fun touchesEnded(touches: Set<*>, withEvent: UIEvent) {
        onTouchesEvent(trackedTouches, withEvent, TouchesEventKind.ENDED)

        stopTrackingTouches(touches)

        if (state.isOngoing || hitTestResult == UserInputViewHitTestResult.Self) {
            // Golden path, just update the gesture recognizer state and pass touches to
            // the Compose runtime.

            if (state.isOngoing) {

                setState(
                    if (trackedTouches.isEmpty()) {
                        UIGestureRecognizerStateEnded
                    } else {
                        UIGestureRecognizerStateChanged
                    }
                )
            }
        } else {
            if (trackedTouches.isEmpty()) {
                // Explicitly fail the gesture, cancelling a scheduled failure
                cancelScheduledFailure()

                setState(UIGestureRecognizerStateFailed)
            }
        }
    }

    /**
     * There are the following scenarios:
     *
     * 1. The [UserInputView] is hit-tested, or a gesture is already recognized.
     * Update the gesture recognizer state and pass touches to the Compose runtime.
     *
     * 2. An interop view is hit-tested. In this case if there are no tracked touches left -
     * we need to allow all the touches to be passed to the interop view by failing explicitly.
     */
    override fun touchesCancelled(touches: Set<*>, withEvent: UIEvent) {
        onTouchesEvent(trackedTouches, withEvent, TouchesEventKind.CANCELLED)

        stopTrackingTouches(touches)

        if (hitTestResult == UserInputViewHitTestResult.Self) {
            // Golden path, just update the gesture recognizer state.

            if (state.isOngoing) {
                setState(
                    if (trackedTouches.isEmpty()) {
                        UIGestureRecognizerStateCancelled
                    } else {
                        UIGestureRecognizerStateChanged
                    }
                )
            }
        } else {
            if (trackedTouches.isEmpty()) {
                // Those were the last touches in the sequence
                // Explicitly fail the gesture, cancelling a scheduled failure
                cancelScheduledFailure()

                // If touches were withheld, give it a chance to be passed to the interop view
                setState(UIGestureRecognizerStateFailed)
            }
        }
    }

    /**
     * Fail the gesture recognizer explicitly.
     *
     * It means we need to pass all the tracked touches to the runtime as canceled and set failed
     * state on the gesture recognizer.
     *
     * Intercepted touches will be passed to the interop views by UIKit due to
     * [UIGestureRecognizer.delaysTouchesBegan]
     */
    fun fail() {
        // Allow withheld touches to be passed to the interop view
        setState(UIGestureRecognizerStateFailed)

        // We won't receive other touches events until all fingers are lifted, so we can't rely
        // on touchesEnded/touchesCancelled to reset the state.  We need to immediately notify
        // Compose about the redirected touches and reset the state manually.
        onTouchesEvent(trackedTouches, null, TouchesEventKind.REDIRECTED)
        stopTrackingAllTouches()
    }

    /**
     * Intentionally clean up all dependencies to prevent retain cycles that
     * can be caused by implicit capture of the view by UIKit objects (such as [UIEvent]) in
     * some rare scenarios.
     */
    fun dispose() {
        cancelScheduledFailure()
        fail()
        onTouchesEvent = { _, _, _, _ -> }
        onGestureEvent = {}
        trackedTouches.clear()
    }

    /**
     * Schedule the gesture recognizer failure after [failureDelaySeconds].
     *
     * We still pass the touches to the interop view
     * until the gesture recognizer is explicitly failed.
     *
     * But when failure happens,
     * all tracked touches are forwarded to runtime as
     * and stop receiving touches from the system.
     *
     * This only happens if the hitTest is not the [UserInputView] itself.
     *
     * @see [fail]
     * @see [cancelScheduledFailure]
     */
    private fun scheduleFailure(failureDelaySeconds: Double) {
        failureJob?.cancel()

        failureJob = CoroutineScope(Dispatchers.Main).launch {
            delay(failureDelaySeconds.toDuration(DurationUnit.SECONDS))

            fail()
        }
    }

    private fun cancelScheduledFailure() {
        failureJob?.cancel()
        failureJob = null
    }

    /**
     * Starts tracking the given touches.
     * Remembers the [initialLocation] if those are the first touches in the sequence.
     * @return `true` if the touches are initial, `false` otherwise.
     */
    private fun startTrackingTouches(touches: Set<*>): Boolean {
        val areTouchesInitial = trackedTouches.isEmpty()

        for (touch in touches) {
            trackedTouches.add(touch as UITouch)
        }

        if (areTouchesInitial) {
            onGestureEvent(GestureEvent.BEGAN)
            initialLocation = trackedTouchesCentroidLocation
        }

        return areTouchesInitial
    }

    /**
     * Check if the centroid of tracked touches has moved past the scroll slop.
     * If so, cancel the scheduled failure and recognize the gesture.
     * Touches intercepted from child views will be discarded due to [delaysTouchesBegan].
     *
     * Otherwise, do nothing.
     */
    private fun checkPanIntent() {
        if (isLocationDeltaAboveSlop) {
            cancelScheduledFailure()

            // When the state changes to UIGestureRecognizerStateBegan,
            // iOS simply discards the touches
            // intercepted due to [delaysTouchesBegan]
            // and prevents them from being sent
            // to the interop view.
            setState(UIGestureRecognizerStateBegan)
        }
    }

    /**
     * Stops tracking the given touches associated with [UIEvent]. If those are the last touches,
     * end the gesture and reset the internal state.
     */
    private fun stopTrackingTouches(touches: Set<*>) {
        for (touch in touches) {
            trackedTouches.remove(touch as UITouch)
        }

        if (trackedTouches.isEmpty()) {
            onGestureEnded()
        }
    }

    /**
     * Stops tracking all [trackedTouches]. End the gesture and reset the internal state.
     */
    private fun stopTrackingAllTouches() {
        trackedTouches.clear()

        onGestureEnded()
    }

    /**
     * Process the moment when all tracked touches are lifted (or canceled due to system events).
     */
    private fun onGestureEnded() {
        initialLocation = null
        onGestureEvent(GestureEvent.ENDED)
    }

    private fun onTouchesEvent(
        touches: Set<*>,
        event: UIEvent?,
        phase: TouchesEventKind
    ) {
        val view = view ?: return

        onTouchesEvent(view, touches, event, phase)
    }
}

/**
 * [UIView] subclass that handles touches and keyboard presses events and forwards them
 * to the Compose runtime.
 *
 * @param hitTestInteropView A callback to find an [InteropView] at the given point.
 * @param onTouchesEvent A callback to notify the Compose runtime about touch events.
 * @param onGestureEvent A callback to notify that touches sequence state has began or ended.
 * @param isPointInsideInteractionBounds A callback to check if the given point is within the interaction
 * bounds as defined by the owning implementation.
 * @param onKeyboardPresses A callback to notify the Compose runtime about keyboard presses.
 * The parameter is a [Set] of [UIPress] objects. Erasure happens due to K/N not supporting Obj-C
 * lightweight generics.
 */
internal class UserInputView(
    private var hitTestInteropView: (point: CValue<CGPoint>, event: UIEvent?) -> UIView?,
    onTouchesEvent: (view: UIView, touches: Set<*>, event: UIEvent?, phase: TouchesEventKind) -> Unit,
    onGestureEvent: (GestureEvent) -> Unit,
    private var isPointInsideInteractionBounds: (CValue<CGPoint>) -> Boolean,
    private var onKeyboardPresses: (Set<*>) -> Unit,
) : UIView(CGRectZero.readValue()) {
    /**
     * Gesture recognizer responsible for processing touches
     * and sending them to the Compose runtime.
     *
     * Also involved in the decision-making process of whether the touch sequence should be
     * passed to the Compose runtime or to the interop view.
     */
    private val gestureRecognizer = UserInputGestureRecognizer(
        onTouchesEvent = onTouchesEvent,
        onGestureEvent = onGestureEvent
    )

    init {
        multipleTouchEnabled = true
        userInteractionEnabled = true

        addGestureRecognizer(gestureRecognizer)
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

    override fun hitTest(point: CValue<CGPoint>, withEvent: UIEvent?): UIView? =
        savingHitTestResult {
            if (!isPointInsideInteractionBounds(point)) {
                null
            } else {
                // Check if a scene contains an [InteropView] in the given point.
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
        removeGestureRecognizer(gestureRecognizer)
        gestureRecognizer.dispose()

        hitTestInteropView = { _, _ -> null }

        isPointInsideInteractionBounds = { false }
        onKeyboardPresses = {}
    }

    /**
     * Execute the given [hitTestBlock] and save the result to the gesture recognizer handler, so
     * that it can be used later to determine if the gesture recognizer should be recognized
     * or failed.
     */
    private fun savingHitTestResult(hitTestBlock: () -> UIView?): UIView? {
        val result = hitTestBlock()
        gestureRecognizer.hitTestResult = if (result == null) {
            null
        } else {
            if (result == this) {
                UserInputViewHitTestResult.Self
            } else {
                // All views beneath are considered to be interop views.
                // If the hit-tested view is not a descendant of [InteropWrappingView], then it
                // should be considered as a view that doesn't want to cooperate with Compose.

                val interactionMode = result.findAncestorInteropWrappingView()?.interactionMode

                when (interactionMode) {
                    is UIKitInteropInteractionMode.Cooperative -> {
                        UserInputViewHitTestResult.CooperativeChildView(
                            delayMillis = interactionMode.delayMillis
                        )
                    }

                    is UIKitInteropInteractionMode.NonCooperative -> {
                        UserInputViewHitTestResult.NonCooperativeChildView
                    }

                    null -> UserInputViewHitTestResult.Self
                }
            }
        }
        return result
    }
}

/**
 * There is no way
 * to associate [InteropWrappingView.interactionMode] with a given [UIView.hitTest] query.
 * This extension method allows finding the nearest [InteropWrappingView] up the view hierarchy
 * and request the value retroactively.
 */
private fun UIView.findAncestorInteropWrappingView(): InteropWrappingView? {
    var view: UIView? = this
    while (view != null) {
        if (view is InteropWrappingView) {
            return view
        }
        view = view.superview
    }
    return null
}

/**
 * Calculate the centroid location of the touches in the given collection.
 *
 * @param view The view in which coordinate space calculation is performed.
 *
 * @return The centroid location of the touches in [this] collection in the coordinate space
 * of the given [view]. Or `null` if [this] is empty.
 */
internal fun Collection<UITouch>.centroidLocationInView(view: UIView): CValue<CGPoint>? {
    if (isEmpty()) {
        return null
    }

    var centroidX = 0.0
    var centroidY = 0.0

    for (touch in this) {
        val location = touch.locationInView(view)
        location.useContents {
            centroidX += x
            centroidY += y
        }
    }

    return CGPointMake(
        x = centroidX / size.toDouble(),
        y = centroidY / size.toDouble()
    )
}