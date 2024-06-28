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
import androidx.compose.ui.uikit.utils.CMPGestureRecognizerProxyProtocol
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

internal class GestureRecognizerProxyImpl(
    private var view: UIView?,
    private val touchesDelegate: InteractionUIView.Delegate,
    private val onTouchesCountChanged: (by: Int) -> Unit

): NSObject(), CMPGestureRecognizerProxyProtocol {
    override fun touchesBegan(touches: Set<*>, withEvent: UIEvent?) {
        onTouchesCountChanged(touches.size)

        val view = view ?: return
        val event = withEvent ?: return

        touchesDelegate.onTouchesEvent(view, event, CupertinoTouchesPhase.BEGAN)
    }

    override fun touchesMoved(touches: Set<*>, withEvent: UIEvent?) {
        val view = view ?: return
        val event = withEvent ?: return

        touchesDelegate.onTouchesEvent(view, event, CupertinoTouchesPhase.MOVED)
    }

    override fun touchesEnded(touches: Set<*>, withEvent: UIEvent?) {
        onTouchesCountChanged(-touches.size)

        val view = view ?: return
        val event = withEvent ?: return

        touchesDelegate.onTouchesEvent(view, event, CupertinoTouchesPhase.ENDED)
    }

    override fun touchesCancelled(touches: Set<*>, withEvent: UIEvent?) {
        onTouchesCountChanged(-touches.size)

        val view = view ?: return
        val event = withEvent ?: return

        touchesDelegate.onTouchesEvent(view, event, CupertinoTouchesPhase.CANCELLED)
    }
}

internal class InteractionUIView(
    private var keyboardEventHandler: KeyboardEventHandler,
    private var touchesDelegate: Delegate,
    private var updateTouchesCount: (count: Int) -> Unit,
    private var inBounds: (CValue<CGPoint>) -> Boolean,
) : UIView(CGRectZero.readValue()) {
    private val gestureRecognizerProxy = GestureRecognizerProxyImpl(
        view = this,
        touchesDelegate = touchesDelegate,
        onTouchesCountChanged = { _touchesCount += it }
    )

    private val gestureRecognizer = CMPGestureRecognizer()

    interface Delegate {
        fun hitTestInteropView(point: CValue<CGPoint>, event: UIEvent?): InteropView?
        fun onTouchesEvent(view: UIView, event: UIEvent, phase: CupertinoTouchesPhase)
    }

    /**
     * When there at least one tracked touch, we need notify redrawer about it. It should schedule CADisplayLink which
     * affects frequency of polling UITouch events on high frequency display and forces it to match display refresh rate.
     */
    private var _touchesCount = 0
        set(value) {
            field = value
            updateTouchesCount(value)
        }

    init {
        multipleTouchEnabled = true
        userInteractionEnabled = true

        addGestureRecognizer(gestureRecognizer)
        gestureRecognizer.proxy = gestureRecognizerProxy
    }

    override fun canBecomeFirstResponder() = true

    override fun pressesBegan(presses: Set<*>, withEvent: UIPressesEvent?) {
        keyboardEventHandler.pressesBegan(presses, withEvent)
        super.pressesBegan(presses, withEvent)
    }

    override fun pressesEnded(presses: Set<*>, withEvent: UIPressesEvent?) {
        keyboardEventHandler.pressesEnded(presses, withEvent)
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

        val interopView = touchesDelegate
            .hitTestInteropView(point, withEvent)
            ?.hitTest(point, withEvent)

        return interopView ?: super.hitTest(point, withEvent)
    }

    /**
     * Intentionally clean up all dependencies of InteractionUIView to prevent retain cycles that
     * can be caused by implicit capture of the view by UIKit objects (such as UIEvent).
     */
    fun dispose() {
        gestureRecognizer.proxy = null
        removeGestureRecognizer(gestureRecognizer)

        touchesDelegate = object : Delegate {
            override fun hitTestInteropView(point: CValue<CGPoint>, event: UIEvent?): InteropView? = null
            override fun onTouchesEvent(view: UIView, event: UIEvent, phase: CupertinoTouchesPhase) {}
        }

        updateTouchesCount = {}
        inBounds = { false }
        keyboardEventHandler = KeyboardEventHandler.Empty
    }
}
