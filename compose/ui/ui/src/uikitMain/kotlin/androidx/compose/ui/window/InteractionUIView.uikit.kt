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
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGRectZero
import platform.UIKit.UIEvent
import platform.UIKit.UIPressesEvent
import platform.UIKit.UIView
import platform.UIKit.UITouchPhase
import platform.UIKit.UIGestureRecognizer
import platform.darwin.NSObject

/**
 * Subset of [UITouchPhase] reflecting immediate phase when event is received by the [UIView] or
 * [UIGestureRecognizer].
 */
internal enum class CupertinoTouchesPhase {
    BEGAN, MOVED, ENDED, CANCELLED
}

internal class GestureRecognizerHandlerImpl(
    private var onTouchesEvent: (view: UIView, event: UIEvent, phase: CupertinoTouchesPhase) -> Unit,
    private var view: UIView?,
    private val onTouchesCountChanged: (by: Int) -> Unit

): NSObject(), CMPGestureRecognizerHandlerProtocol {
    override fun touchesBegan(touches: Set<*>, withEvent: UIEvent?) {
        onTouchesCountChanged(touches.size)

        val view = view ?: return
        val event = withEvent ?: return

        onTouchesEvent(view, event, CupertinoTouchesPhase.BEGAN)
    }

    override fun touchesMoved(touches: Set<*>, withEvent: UIEvent?) {
        val view = view ?: return
        val event = withEvent ?: return

        onTouchesEvent(view, event, CupertinoTouchesPhase.MOVED)
    }

    override fun touchesEnded(touches: Set<*>, withEvent: UIEvent?) {
        onTouchesCountChanged(-touches.size)

        val view = view ?: return
        val event = withEvent ?: return

        onTouchesEvent(view, event, CupertinoTouchesPhase.ENDED)
    }

    override fun touchesCancelled(touches: Set<*>, withEvent: UIEvent?) {
        onTouchesCountChanged(-touches.size)

        val view = view ?: return
        val event = withEvent ?: return

        onTouchesEvent(view, event, CupertinoTouchesPhase.CANCELLED)
    }

    override fun shouldRecognizeSimultaneously(first: UIGestureRecognizer, withOther: UIGestureRecognizer): Boolean {
        return true
    }

    fun dispose() {
        onTouchesEvent = { _, _, _ -> }
    }
}

internal class InteractionUIView(
    private var hitTestInteropView: (point: CValue<CGPoint>, event: UIEvent?) -> InteropView?,
    onTouchesEvent: (view: UIView, event: UIEvent, phase: CupertinoTouchesPhase) -> Unit,
    private var onTouchesCountChange: (count: Int) -> Unit,
    private var inBounds: (CValue<CGPoint>) -> Boolean,
    private var onPresses: (Set<*>) -> Unit,
) : UIView(CGRectZero.readValue()) {
    private val gestureRecognizerHandler = GestureRecognizerHandlerImpl(
        view = this,
        onTouchesEvent = onTouchesEvent,
        onTouchesCountChanged = { _touchesCount += it }
    )

    private val gestureRecognizer = CMPGestureRecognizer()

    /**
     * When there at least one tracked touch, we need notify redrawer about it. It should schedule CADisplayLink which
     * affects frequency of polling UITouch events on high frequency display and forces it to match display refresh rate.
     */
    private var _touchesCount = 0
        set(value) {
            field = value
            onTouchesCountChange(value)
        }

    init {
        multipleTouchEnabled = true
        userInteractionEnabled = true

        addGestureRecognizer(gestureRecognizer)
        gestureRecognizer.handler = gestureRecognizerHandler
    }

    override fun canBecomeFirstResponder() = true

    override fun pressesBegan(presses: Set<*>, withEvent: UIPressesEvent?) {
        onPresses(presses)
        super.pressesBegan(presses, withEvent)
    }

    override fun pressesEnded(presses: Set<*>, withEvent: UIPressesEvent?) {
        onPresses(presses)
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

    override fun hitTest(point: CValue<CGPoint>, withEvent: UIEvent?): UIView? {
        if (!inBounds(point)) {
            return null
        }

        val interopView =
            hitTestInteropView(point, withEvent)
            ?.hitTest(point, withEvent, this)

        // hitTest happens after `pointInside` is checked. If hitTest inside ComposeScene didn't
        // find any interop view, then we should return the view itself.
        return interopView ?: this
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
        inBounds = { false }
        onPresses = {}
    }
}
