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

import androidx.compose.runtime.ComposeTabService
import androidx.compose.runtime.tooling.ConvertParameters
import androidx.compose.ui.uikit.utils.CMPHoverGestureHandler
import androidx.compose.ui.uikit.utils.CMPPanGestureRecognizer
import androidx.compose.ui.uikit.utils.TMMInteropBaseView
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.asDpOffset
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.readValue
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.OSVersion
import org.jetbrains.skiko.SkikoInputModifiers
import org.jetbrains.skiko.SkikoKey
import org.jetbrains.skiko.SkikoKeyboardEvent
import org.jetbrains.skiko.SkikoKeyboardEventKind
import org.jetbrains.skiko.available
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSSelectorFromString
import platform.UIKit.UIEvent
import platform.UIKit.UIGestureRecognizerStateBegan
import platform.UIKit.UIGestureRecognizerStateCancelled
import platform.UIKit.UIGestureRecognizerStateChanged
import platform.UIKit.UIGestureRecognizerStateEnded
import platform.UIKit.UIGestureRecognizerStateFailed
import platform.UIKit.UIKeyModifierAlternate
import platform.UIKit.UIKeyModifierCommand
import platform.UIKit.UIKeyModifierControl
import platform.UIKit.UIKeyModifierShift
import platform.UIKit.UIPanGestureRecognizer
import platform.UIKit.UIPress
import platform.UIKit.UIPressesEvent
import platform.UIKit.UIScrollTypeMaskAll
import platform.UIKit.UIView
import platform.UIKit.setState

// region Tencent Code
enum class UITouchesEventPhase {
    BEGAN, MOVED, ENDED, CANCELLED,REDIRECTED
}

enum class HitTestViewType {
    NONE, NATIVEVIEW, COMPOSEVIEW
}

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

private class ScrollGestureRecognizer(
    private var onScrollEvent: (position: DpOffset, delta: DpOffset, event: UIEvent?, eventKind: TouchesEventKind) -> Unit,
    private var onCancelScroll: () -> Unit
) : CMPPanGestureRecognizer(target = null, action = null) {

    init {
        setDelaysTouchesBegan(false)
        setDelaysTouchesEnded(false)
        setCancelsTouchesInView(false)
        setAllowedScrollTypesMask(UIScrollTypeMaskAll)
        addTarget(this, NSSelectorFromString(::onPan.name + ":"))
    }

    private var cursorPosition: DpOffset? = null
    private var previousPosition: DpOffset? = null
    private var event: UIEvent? = null

    @ObjCAction
    fun onPan(gestureRecognizer: UIPanGestureRecognizer) {
        val position = gestureRecognizer.locationInView(view).asDpOffset()

        when (gestureRecognizer.state) {
            UIGestureRecognizerStateBegan -> {
                onScrollEvent(position, DpOffset.Zero, event, TouchesEventKind.BEGAN)
                cursorPosition = position
                previousPosition = position
            }

            UIGestureRecognizerStateChanged -> {
                val delta = (previousPosition ?: position) - position
                onScrollEvent(cursorPosition ?: position, delta, event, TouchesEventKind.MOVED)
                previousPosition = position
            }

            UIGestureRecognizerStateEnded -> {
                val delta = (previousPosition ?: position) - position
                onScrollEvent(cursorPosition ?: position, delta, event, TouchesEventKind.ENDED)
                cursorPosition = null
                previousPosition = null
                event = null
            }

            UIGestureRecognizerStateCancelled, UIGestureRecognizerStateFailed -> {
                onCancelScroll()
                cursorPosition = null
                previousPosition = null
                event = null
            }

            else -> {}
        }
    }

    override fun shouldReceiveEvent(event: UIEvent): Boolean {
        this.event = event
        return super.shouldReceiveEvent(event)
    }

    fun dispose() {
        removeTarget(this, null)
        onScrollEvent = { _, _, _, _  -> }
        onCancelScroll = {}
    }

    override fun touchesBegan(touches: Set<*>, withEvent: UIEvent) {
        // Gesture recognizer only works with the trackpad. All touches should be cancelled.
        setState(UIGestureRecognizerStateFailed)
    }

    override fun touchesMoved(touches: Set<*>, withEvent: UIEvent) {
        // Do nothing. No need to handle touches for scroll gesture
    }

    override fun touchesEnded(touches: Set<*>, withEvent: UIEvent) {
        // Do nothing. No need to handle touches for scroll gesture
    }

    override fun touchesCancelled(touches: Set<*>, withEvent: UIEvent) {
        // Do nothing. No need to handle touches for scroll gesture
    }
}

internal class InteractionUIView(
    private var keyboardEventHandler: KeyboardEventHandler,
    private var touchesDelegate: Delegate,
    private var updateTouchesCount: (count: Int) -> Unit,
    onScrollEvent: (position: DpOffset, delta: DpOffset, event: UIEvent?, eventKind: TouchesEventKind) -> Unit,
    onCancelScroll: () -> Unit,
    private var onHoverEvent: (position: DpOffset, event: UIEvent?, eventKind: TouchesEventKind) -> Unit,
    private var checkBounds: (point: DpOffset) -> Boolean,
    private var becomeFirstResponder: Boolean = true,
    private var drawInSkia: Boolean = false,
) : TMMInteropBaseView(CGRectZero.readValue()) {

    interface Delegate {
        fun pointInside(point: CValue<CGPoint>, event: UIEvent?): HitTestViewType
        fun onTouchesEvent(view: UIView, event: UIEvent, phase: UITouchesEventPhase)
    }
// endregion

    /**
     * When there at least one tracked touch, we need notify redrawer about it. It should schedule CADisplayLink which
     * affects frequency of polling UITouch events on high frequency display and forces it to match display refresh rate.
     */
    private var _touchesCount = 0
        set(value) {
            field = value
            updateTouchesCount(value)
        }

    private val scrollGestureRecognizer by lazy {
        if (available(OS.Ios to OSVersion(major = 13, minor = 4))) {
            ScrollGestureRecognizer(
                onScrollEvent = onScrollEvent,
                onCancelScroll = onCancelScroll
            )
        } else {
            null
        }
    }

    private val hoverGestureHandler by lazy {
        CMPHoverGestureHandler(this, NSSelectorFromString(::onHover.name + ":"))
    }

    init {
        multipleTouchEnabled = true
        userInteractionEnabled = true

        scrollGestureRecognizer?.let {
            addGestureRecognizer(it)
        }
        hoverGestureHandler.attachToView(this)
    }

    // region Tencent Code
    override fun canBecomeFirstResponder() = becomeFirstResponder
    // endregion

    override fun pressesBegan(presses: Set<*>, withEvent: UIPressesEvent?) {
        handleUIViewPressesBegan(keyboardEventHandler, presses, withEvent)
        super.pressesBegan(presses, withEvent)
    }

    override fun pressesEnded(presses: Set<*>, withEvent: UIPressesEvent?) {
        handleUIViewPressesEnded(keyboardEventHandler, presses, withEvent)
        super.pressesEnded(presses, withEvent)
    }

    // region Tencent Code
    override fun hitTest(point: CValue<CGPoint>, withEvent: UIEvent?): UIView? {
        if (!pointInside(point, withEvent)) return null
        var view: UIView? = null
        val hitTestViewType = touchesDelegate.pointInside(point, withEvent)
        view = when (hitTestViewType) {
            HitTestViewType.NATIVEVIEW -> super.hitTest(point, withEvent)
            HitTestViewType.COMPOSEVIEW -> this
            HitTestViewType.NONE -> null
        }

        return view
    }

    // region Tencent Code
    private var isTouchesConsuming: Boolean = false

    /**
     * Wrap onTouchesEvent in this function to avoid calling it recursively.
     */
    private inline fun consumeTouchesOnce(block: () -> Unit) {
        if (isTouchesConsuming) return
        isTouchesConsuming = true
        block()
        isTouchesConsuming = false
    }
    // endregion

    override fun touchesBegan(touches: Set<*>, withEvent: UIEvent?) {
        if (!ComposeTabService.composeGestureEnable) {
            return originalTouchesBegan(touches, withEvent)
        }

        super.touchesBegan(touches, withEvent)
        if (this.disableTouch.boolValue) return
        consumeTouchesOnce {
            _touchesCount += touches.size
            withEvent?.let { event ->
                touchesDelegate.onTouchesEvent(this, event, UITouchesEventPhase.BEGAN)
            }
        }
    }

    override fun touchesEnded(touches: Set<*>, withEvent: UIEvent?) {
        if (!ComposeTabService.composeGestureEnable) {
            return originalTouchesEnded(touches, withEvent)
        }
        super.touchesEnded(touches, withEvent)
        if (this.disableTouch.boolValue) return
        consumeTouchesOnce {
            _touchesCount -= touches.size
            withEvent?.let { event ->
                touchesDelegate.onTouchesEvent(this, event, UITouchesEventPhase.ENDED)
            }
        }
    }

    override fun touchesMoved(touches: Set<*>, withEvent: UIEvent?) {
        if (!ComposeTabService.composeGestureEnable) {
            return originalTouchesMoved(touches, withEvent)
        }
        super.touchesMoved(touches, withEvent)
        if (this.disableTouch.boolValue) return
        consumeTouchesOnce {
            withEvent?.let { event ->
                touchesDelegate.onTouchesEvent(this, event, UITouchesEventPhase.MOVED)
            }
        }
    }

    override fun touchesCancelled(touches: Set<*>, withEvent: UIEvent?) {
        if (!ComposeTabService.composeGestureEnable) {
            return originalTouchesCancelled(touches, withEvent)
        }
        if (!this.disableTouch.boolValue) {
            super.touchesCancelled(touches, withEvent)
        }
        consumeTouchesOnce {
            _touchesCount -= touches.size
            withEvent?.let { event ->
                touchesDelegate.onTouchesEvent(this, event, UITouchesEventPhase.CANCELLED)
            }
        }
    }

    private fun originalTouchesBegan(touches: Set<*>, withEvent: UIEvent?) {
        super.touchesBegan(touches, withEvent)
        _touchesCount += touches.size
        withEvent?.let { event ->
            touchesDelegate.onTouchesEvent(this, event, UITouchesEventPhase.BEGAN)
        }
    }
    private fun originalTouchesEnded(touches: Set<*>, withEvent: UIEvent?) {
        super.touchesEnded(touches, withEvent)
        _touchesCount -= touches.size
        withEvent?.let { event ->
            touchesDelegate.onTouchesEvent(this, event, UITouchesEventPhase.ENDED)
        }
    }
    private fun originalTouchesMoved(touches: Set<*>, withEvent: UIEvent?) {
        super.touchesMoved(touches, withEvent)
        withEvent?.let { event ->
            touchesDelegate.onTouchesEvent(this, event, UITouchesEventPhase.MOVED)
        }
    }

    private fun originalTouchesCancelled(touches: Set<*>, withEvent: UIEvent?) {
        super.touchesCancelled(touches, withEvent)
        _touchesCount -= touches.size
        withEvent?.let { event ->
            touchesDelegate.onTouchesEvent(this, event, UITouchesEventPhase.CANCELLED)
        }
    }

    private var lastHoverPosition: DpOffset? = null
    @ObjCAction
    fun onHover(gestureRecognizer: UIPanGestureRecognizer) {
        val position = gestureRecognizer.locationInView(this).asDpOffset()
        val lastEvent = hoverGestureHandler.lastHandledEvent
        when (gestureRecognizer.state) {
            UIGestureRecognizerStateBegan ->
                onHoverEvent(position, lastEvent, TouchesEventKind.BEGAN)

            UIGestureRecognizerStateChanged ->
                if (lastHoverPosition != position && _touchesCount <= 0) {
                    onHoverEvent(position, lastEvent, TouchesEventKind.MOVED)
                }

            UIGestureRecognizerStateEnded ->
                onHoverEvent(position, lastEvent, TouchesEventKind.ENDED)

            UIGestureRecognizerStateCancelled,
            UIGestureRecognizerStateFailed ->
                onHoverEvent(lastHoverPosition ?: position, lastEvent, TouchesEventKind.ENDED)

            else -> {}
        }
        lastHoverPosition = position
    }

    /**
     * Intentionally clean up all dependencies of InteractionUIView to prevent retain cycles that
     * can be caused by implicit capture of the view by UIKit objects (such as UIEvent).
     */
    fun dispose() {
        touchesDelegate = object : Delegate {
            override fun pointInside(point: CValue<CGPoint>, event: UIEvent?): HitTestViewType = HitTestViewType.NONE
            override fun onTouchesEvent(view: UIView, event: UIEvent, phase: UITouchesEventPhase) {}
        }
        updateTouchesCount = {}
        scrollGestureRecognizer?.let {
            removeGestureRecognizer(it)
            it.dispose()
        }
        hoverGestureHandler.detachFromViewAndDispose(this)
        onHoverEvent = { _, _, _ -> }
        checkBounds = { false }
        keyboardEventHandler = object: KeyboardEventHandler {
            override fun onKeyboardEvent(event: SkikoKeyboardEvent) {}
        }
        setNodeBlock(null)
    }
    // endregion
}

internal fun handleUIViewPressesBegan(
    keyboardEventHandler: KeyboardEventHandler,
    presses: Set<*>,
    withEvent: UIPressesEvent?
) {
    if (withEvent != null) {
        for (press in withEvent.allPresses) {
            if (press is UIPress) {
                keyboardEventHandler.onKeyboardEvent(
                    toSkikoKeyboardEvent(press, SkikoKeyboardEventKind.DOWN)
                )
            }
        }
    }
}

internal fun handleUIViewPressesEnded(
    keyboardEventHandler: KeyboardEventHandler,
    presses: Set<*>,
    withEvent: UIPressesEvent?
) {
    if (withEvent != null) {
        for (press in withEvent.allPresses) {
            if (press is UIPress) {
                keyboardEventHandler.onKeyboardEvent(
                    toSkikoKeyboardEvent(press, SkikoKeyboardEventKind.UP)
                )
            }
        }
    }
}

private fun toSkikoKeyboardEvent(
    event: UIPress,
    kind: SkikoKeyboardEventKind
): SkikoKeyboardEvent {
    val timestamp = (event.timestamp * 1_000).toLong()
    return SkikoKeyboardEvent(
        // region Tencent Code Modify
        /*SkikoKey.valueOf(event.key!!.keyCode),*/
        event.key?.keyCode?.let { SkikoKey.valueOf(it) } ?: SkikoKey.KEY_UNKNOWN,
        // endregion
        toSkikoModifiers(event),
        kind,
        timestamp,
        event
    )
}

private fun toSkikoModifiers(event: UIPress): SkikoInputModifiers {
    var result = 0
    // region Tencent Code
    if (event.key == null) {
        return SkikoInputModifiers(result)
    }
    // endregion
    val modifiers = event.key!!.modifierFlags
    if (modifiers and UIKeyModifierAlternate != 0L) {
        result = result.or(SkikoInputModifiers.ALT.value)
    }
    if (modifiers and UIKeyModifierShift != 0L) {
        result = result.or(SkikoInputModifiers.SHIFT.value)
    }
    if (modifiers and UIKeyModifierControl != 0L) {
        result = result.or(SkikoInputModifiers.CONTROL.value)
    }
    if (modifiers and UIKeyModifierCommand != 0L) {
        result = result.or(SkikoInputModifiers.META.value)
    }
    return SkikoInputModifiers(result)
}
