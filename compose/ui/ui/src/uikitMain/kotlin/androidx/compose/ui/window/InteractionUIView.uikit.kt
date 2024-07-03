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

import androidx.compose.ui.viewinterop.InteropView
import kotlinx.cinterop.CValue
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGRectZero
import platform.UIKit.UIEvent
import platform.UIKit.UIPressesEvent
import platform.UIKit.UIView
import platform.UIKit.UITouchPhase
import platform.UIKit.UIGestureRecognizer
import platform.UIKit.UIPress

/**
 * Subset of [UITouchPhase] reflecting immediate phase when event is received by the [UIView] or
 * [UIGestureRecognizer].
 */
internal enum class CupertinoTouchesPhase {
    BEGAN, MOVED, ENDED, CANCELLED
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
    private var onTouchesEvent: (view: UIView, event: UIEvent, phase: CupertinoTouchesPhase) -> Unit,
    private var onTouchesCountChange: (count: Int) -> Unit,
    private var inInteractionBounds: (CValue<CGPoint>) -> Boolean,
    private var onKeyboardPresses: (Set<*>) -> Unit,
) : UIView(CGRectZero.readValue()) {
    /**
     * When there at least one tracked touch, we need notify redrawer about it. It should schedule
     * CADisplayLink which affects frequency of polling UITouch events on high frequency display
     * and forces it to match display refresh rate.
     */
    private var _touchesCount = 0
        set(value) {
            field = value
            onTouchesCountChange(value)
        }

    init {
        multipleTouchEnabled = true
        userInteractionEnabled = true
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

    override fun touchesBegan(touches: Set<*>, withEvent: UIEvent?) {
        _touchesCount += touches.size

        val event = withEvent ?: return

        onTouchesEvent(this, event, CupertinoTouchesPhase.BEGAN)
    }

    override fun touchesMoved(touches: Set<*>, withEvent: UIEvent?) {
        val event = withEvent ?: return

        onTouchesEvent(this, event, CupertinoTouchesPhase.MOVED)
    }

    override fun touchesEnded(touches: Set<*>, withEvent: UIEvent?) {
        _touchesCount -= touches.size

        val event = withEvent ?: return

        onTouchesEvent(this, event, CupertinoTouchesPhase.ENDED)
    }

    override fun touchesCancelled(touches: Set<*>, withEvent: UIEvent?) {
        _touchesCount -= touches.size

        val event = withEvent ?: return

        onTouchesEvent(this, event, CupertinoTouchesPhase.CANCELLED)
    }

    override fun hitTest(point: CValue<CGPoint>, withEvent: UIEvent?): UIView? {
        if (!inInteractionBounds(point)) {
            return null
        }

        // Find if a scene contains a node [InteropViewModifier] at the given point.
        // Native [hitTest] happens after [pointInside] is checked. If hit testing
        // inside ComposeScene didn't yield any interop view, then we should return [this]
        val interopView = hitTestInteropView(point, withEvent) ?: return this

        // Transform the point to the interop view's coordinate system.
        // And perform native [hitTest] on the interop view.
        val hitTestView = interopView.hitTest(
            point = convertPoint(point, toView = interopView),
            withEvent = withEvent)

        return hitTestView ?: this
    }

    /**
     * Intentionally clean up all dependencies of InteractionUIView to prevent retain cycles that
     * can be caused by implicit capture of the view by UIKit objects (such as UIEvent).
     */
    fun dispose() {
        hitTestInteropView = { _, _ -> null }
        onTouchesEvent = { _, _, _ -> }

        onTouchesCountChange = {}
        inInteractionBounds = { false }
        onKeyboardPresses = {}
    }
}
