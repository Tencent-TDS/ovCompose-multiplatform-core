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

import androidx.compose.ui.backhandler.UIKitBackGestureRecognizer
import androidx.compose.ui.scene.PointerEventResult
import androidx.compose.ui.uikit.utils.CMPGestureRecognizer
import androidx.compose.ui.viewinterop.InteropView
import androidx.compose.ui.viewinterop.InteropWrappingView
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import kotlin.math.abs
import kotlinx.cinterop.CValue
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGRectZero
import platform.UIKit.UIEvent
import platform.UIKit.UIGestureRecognizer
import platform.UIKit.UIGestureRecognizerState
import platform.UIKit.UIGestureRecognizerStateBegan
import platform.UIKit.UIGestureRecognizerStateCancelled
import platform.UIKit.UIGestureRecognizerStateChanged
import platform.UIKit.UIGestureRecognizerStateEnded
import platform.UIKit.UIGestureRecognizerStatePossible
import platform.UIKit.UIPanGestureRecognizer
import platform.UIKit.UIPress
import platform.UIKit.UIPressesEvent
import platform.UIKit.UIScreenEdgePanGestureRecognizer
import platform.UIKit.UIScrollView
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
    ENDED
}

private val UIGestureRecognizerState.isOngoing: Boolean
    get() =
        when (this) {
            UIGestureRecognizerStateBegan, UIGestureRecognizerStateChanged -> true
            else -> false
        }

/**
 * Implementation of [UIGestureRecognizer] that handles touch events and forwards
 * them. The main difference from the original [UIView] touches based is that it's built on top of
 * [UIGestureRecognizer], which play differently with UIKit touches processing and are required
 * for the correct handling of the touch events in interop scenarios, because they rely on
 * [UIGestureRecognizer] failure requirements and touches interception, which is an exclusive way
 * to control touches delivery to [UIView]s and their [UIGestureRecognizer]s in a fine-grain manner.
 */
internal class UserInputGestureRecognizer(
    private var onTouchesEvent: (touches: Set<*>, event: UIEvent?, phase: TouchesEventKind) -> PointerEventResult,
    private var onCancelAllTouches: (touches: Set<*>) -> Unit,
    private var canIgnoreDragGesture: (UIGestureRecognizer) -> Boolean
) : CMPGestureRecognizer(target = null, action = null) {
    /**
     * Touches that are currently tracked by the gesture recognizer.
     */
    private val trackedTouches: MutableMap<UITouch, UIView?> = mutableMapOf()

    /**
     * Scheduled job for the gesture recognizer failure.
     */
    private var failureJob: Job? = null

    init {
        // When recognized, immediately cancel all touches in the subviews.
        // This scenario shouldn't happen due to `delaysTouchesBegan`, so it's
        // more of a defensive line.
        cancelsTouchesInView = true

        // Delays touches reception by underlying views until the gesture recognizer is explicitly
        // stated as failed (aka, the touch sequence is targeted to the interop view).
        delaysTouchesBegan = true
    }

    override fun touchesBegan(touches: Set<*>, withEvent: UIEvent) {
        super.touchesBegan(touches, withEvent)

        val touchesToInteractionMode = touches.map { touch ->
            touch as UITouch
            val point = touch.locationInView(view)
            val hitTestResult = view?.hitTest(point, withEvent)?.takeIf { it != view }
            touch to hitTestResult
        }.toMap()

        fun startTouchesEvent() {
            val isInitialTouches = trackedTouches.isEmpty()
            trackedTouches.putAll(touchesToInteractionMode)
            onTouchesEvent(trackedTouches.keys, withEvent, TouchesEventKind.BEGAN)
            if (isInitialTouches) {
                setState(UIGestureRecognizerStatePossible)
            } else if (state.isOngoing) {
                setState(UIGestureRecognizerStateChanged)
            }
        }

        val interactionMode = touchesToInteractionMode.values.map {
            it?.findAncestorInteropWrappingView()?.interactionMode
        }.findMostRestrictedInteractionMode()
        when (interactionMode) {
            is UIKitInteropInteractionMode.Cooperative -> {
                startTouchesEvent()
                scheduleTouchesFailureIfNeeded(interactionMode.delayMillis)
            }

            UIKitInteropInteractionMode.NonCooperative -> {
                cancelAllTrackedTouches()
            }

            null -> {
                startTouchesEvent()
            }
        }
    }

    override fun touchesMoved(touches: Set<*>, withEvent: UIEvent) {
        super.touchesMoved(touches, withEvent)

        fun processGesture() {
            if (trackedTouches.isEmpty()) {
                return
            }
            val result = onTouchesEvent(trackedTouches.keys, withEvent, TouchesEventKind.MOVED)
            if (result.anyMovementConsumed) {
                if (!state.isOngoing) {
                    setState(UIGestureRecognizerStateBegan)
                    cancelTouchesFailure()
                }
            }
        }

        // The UserInputGestureRecognizer receives touches earlier than its interop scroll views,
        // if any. If an interop scroll view is involved in tracking touches, we let it capture
        // the pan gesture first in order to prioritise the scrolling gesture of the child scroll
        // view.
        val postponeGesture = state == UIGestureRecognizerStatePossible &&
            touches.any { trackedTouches[it].hasTrackingUIScrollView() }
        if (postponeGesture) {
            CoroutineScope(Dispatchers.Main).launch { processGesture() }
        } else {
            processGesture()
        }
    }

    override fun touchesEnded(touches: Set<*>, withEvent: UIEvent) {
        super.touchesEnded(touches, withEvent)

        fun endTouchesEvent() {
            onTouchesEvent(trackedTouches.keys, withEvent, TouchesEventKind.ENDED)
            stopTrackingTouches(touches)
            if (trackedTouches.isEmpty()) {
                setState(UIGestureRecognizerStateEnded)
            }
        }

        if (state.isOngoing) {
            endTouchesEvent()
        } else {
            val hasHitTestResult = touches.firstNotNullOfOrNull { trackedTouches[it] } != null
            if (hasHitTestResult) {
                cancelAllTrackedTouches()
            } else {
                endTouchesEvent()
            }
        }
    }

    override fun touchesCancelled(touches: Set<*>, withEvent: UIEvent) {
        super.touchesCancelled(touches, withEvent)

        cancelAllTrackedTouches()
    }

    private fun cancelAllTrackedTouches() {
        setState(UIGestureRecognizerStateCancelled)
        onCancelAllTouches(trackedTouches.keys)
        trackedTouches.clear()
        cancelTouchesFailure()
    }

    private fun Collection<UIKitInteropInteractionMode?>.findMostRestrictedInteractionMode() =
        minBy {
            when (it) {
                UIKitInteropInteractionMode.NonCooperative -> 0
                is UIKitInteropInteractionMode.Cooperative -> it.delayMillis
                null -> Int.MAX_VALUE
            }
        }

    override fun canBePreventedByGestureRecognizer(
        preventingGestureRecognizer: UIGestureRecognizer
    ): Boolean {
        return if (preventingGestureRecognizer is UIKitBackGestureRecognizer) {
            cancelAllTrackedTouches()
            true
        } else if (canIgnoreDragGesture(preventingGestureRecognizer)) {
            false
        } else if (preventingGestureRecognizer is UserInputGestureRecognizer
            && preventingGestureRecognizer.state.isOngoing) {
            cancelAllTrackedTouches()
            true
        } else if (isInChildHierarchy(preventingGestureRecognizer.view)) {
            if ((state == UIGestureRecognizerStatePossible || state.isOngoing) &&
                isScrollViewAtTheEndOfScrollableContent(preventingGestureRecognizer)
            ) {
                false
            } else {
                cancelAllTrackedTouches()
                true
            }
        } else {
            if (state.isOngoing || !preventingGestureRecognizer.state.isOngoing) {
                false
            } else {
                cancelAllTrackedTouches()
                true
            }
        }
    }

    override fun canPreventGestureRecognizer(
        preventedGestureRecognizer: UIGestureRecognizer
    ): Boolean {
        return if (isInChildHierarchy(preventedGestureRecognizer.view)) {
            super.canPreventGestureRecognizer(preventedGestureRecognizer)
        } else if (preventedGestureRecognizer is UIScreenEdgePanGestureRecognizer) {
            false
        } else {
            state == UIGestureRecognizerStatePossible || state.isOngoing
        }
    }

    /**
     * Checks if compose can get priority over interop view with UIScrollView on it.
     *
     * @return return true if UIScrollView can no longer scroll content in the direction of the user
     * gesture that UIScrollView detected.
     */
    private fun isScrollViewAtTheEndOfScrollableContent(recognizer: UIGestureRecognizer): Boolean {
        val pan = recognizer as? UIPanGestureRecognizer ?: return false
        val scrollView = recognizer.view as? UIScrollView ?: return false

        val (diffX, diffY) = pan.translationInView(scrollView).useContents { x to y }
        val (offsetX, offsetY) = scrollView.contentOffset.useContents { x to y }
        val (contentWidth, contentHeight) = scrollView.contentSize.useContents { width to height }
        val (scrollWidth, scrollHeight) = scrollView.bounds.useContents { size.width to size.height }
        val insets = scrollView.contentInset.useContents { this }

        val endOfHorizontal = (diffX >= 0 && offsetX.equalWithinPixelTolerance(-insets.left)) ||
            (diffX <= 0 &&
                offsetX.equalWithinPixelTolerance(contentWidth - scrollWidth + insets.right))

        val endOfVertical = (diffY >= 0 && offsetY.equalWithinPixelTolerance(-insets.top)) ||
            (diffY <= 0 &&
                offsetY.equalWithinPixelTolerance(contentHeight - scrollHeight + insets.bottom))

        return endOfHorizontal && endOfVertical
    }

    private fun isInChildHierarchy(child: UIView?): Boolean {
        val view = view ?: return false
        var iteratingView = child
        while (iteratingView != null) {
            if (view == iteratingView) {
                return true
            }
            iteratingView = iteratingView.superview
        }
        return false
    }

    /**
     * Intentionally clean up all dependencies to prevent retain cycles that
     * can be caused by implicit capture of the view by UIKit objects (such as [UIEvent]) in
     * some rare scenarios.
     */
    fun dispose() {
        cancelTouchesFailure()
        onTouchesEvent = { _, _, _ -> PointerEventResult(anyMovementConsumed = false) }
        onCancelAllTouches = {}
        canIgnoreDragGesture = { false }
        trackedTouches.clear()
    }

    /**
     * Schedule the gesture recognizer failure after [delayMills].
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
     * @see [cancelTouchesFailure]
     */
    private fun scheduleTouchesFailureIfNeeded(delayMills: Int) {
        failureJob?.cancel()

        if (delayMills != Int.MAX_VALUE) {
            failureJob = CoroutineScope(Dispatchers.Main).launch {
                delay(delayMills.toLong())

                cancelAllTrackedTouches()
            }
        }
    }

    private fun cancelTouchesFailure() {
        failureJob?.cancel()
        failureJob = null
    }

    /**
     * Stops tracking the given touches associated with [UIEvent]. If those are the last touches,
     * end the gesture and reset the internal state.
     */
    private fun stopTrackingTouches(touches: Set<*>) {
        for (touch in touches) {
            trackedTouches.remove(touch as UITouch)
        }
    }
}

/**
 * [UIView] subclass that handles touches and keyboard presses events and forwards them
 * to the Compose runtime.
 *
 * @param hitTestInteropView A callback to find an [InteropView] at the given point.
 * @param onTouchesEvent A callback to notify the Compose runtime about touch events.
 * @param isPointInsideInteractionBounds A callback to check if the given point is within the interaction
 * bounds as defined by the owning implementation.
 * @param onKeyboardPresses A callback to notify the Compose runtime about keyboard presses.
 * The parameter is a [Set] of [UIPress] objects. Erasure happens due to K/N not supporting Obj-C
 * lightweight generics.
 */
internal class UserInputView(
    private var hitTestInteropView: (point: CValue<CGPoint>) -> UIView?,
    private var isPointInsideInteractionBounds: (CValue<CGPoint>) -> Boolean,
    onTouchesEvent: (touches: Set<*>, event: UIEvent?, phase: TouchesEventKind) -> PointerEventResult,
    onCancelAllTouches: (touches: Set<*>) -> Unit,
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
        onCancelAllTouches = onCancelAllTouches,
        canIgnoreDragGesture = { canIgnoreDragGesture(it) }
    )

    // See [UIKitDragAndDropManager] for more context
    var canIgnoreDragGesture: (UIGestureRecognizer) -> Boolean = { false }

    init {
        multipleTouchEnabled = true

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
        if (isPointInsideInteractionBounds(point)) {
            hitTestInteropView(point)?.let { interopView ->
                interopView.hitTest(
                    point = convertPoint(point, toView = interopView),
                    withEvent = withEvent
                )
            } ?: this
        } else {
            null
        }

    /**
     * Intentionally clean up all dependencies of InteractionUIView to prevent retain cycles that
     * can be caused by implicit capture of the view by UIKit objects (such as UIEvent).
     */
    fun dispose() {
        removeGestureRecognizer(gestureRecognizer)
        gestureRecognizer.dispose()

        hitTestInteropView = { null }
        isPointInsideInteractionBounds = { false }
        canIgnoreDragGesture = { false }
        onKeyboardPresses = {}
    }
}

/**
 * There is no way to associate [InteropWrappingView.interactionMode] with a given [UIView.hitTest]
 * query. This extension method allows finding the nearest [InteropWrappingView] up the view
 * hierarchy and request the value retroactively.
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

private fun Double.equalWithinPixelTolerance(other: Double): Boolean {
    return abs(other - this) < 0.1 // Any number smaller than a pixel size is sufficient here
}

private fun UIView?.hasTrackingUIScrollView(): Boolean {
    var view: UIView? = this
    while (view != null) {
        if (view is InteropWrappingView) {
            return false
        }
        if (view is UIScrollView &&
            view.userInteractionEnabled &&
            view.panGestureRecognizer.isEnabled()) {
            if ((view.panGestureRecognizer.state == UIGestureRecognizerStatePossible ||
                    view.panGestureRecognizer.state == UIGestureRecognizerStateBegan) &&
                view.isTracking()
            ) {
                return true
            }
        }
        view = view.superview
    }
    return false
}
