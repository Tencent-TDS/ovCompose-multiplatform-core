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

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.interop.UIKitInteropAction
import androidx.compose.ui.interop.UIKitInteropTransaction
import androidx.compose.ui.platform.IOSSkikoInput
import androidx.compose.ui.platform.SkikoUITextInputTraits
import androidx.compose.ui.platform.TextActions
import kotlinx.cinterop.*
import org.jetbrains.skia.Rect
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.Metal.MTLCreateSystemDefaultDevice
import platform.Metal.MTLDeviceProtocol
import platform.Metal.MTLPixelFormatBGRA8Unorm
import platform.QuartzCore.CAMetalLayer
import platform.UIKit.*
import platform.darwin.NSInteger
import org.jetbrains.skia.Surface
import org.jetbrains.skia.Canvas
import org.jetbrains.skiko.SkikoInputModifiers
import org.jetbrains.skiko.SkikoKey
import org.jetbrains.skiko.SkikoKeyboardEvent
import org.jetbrains.skiko.SkikoKeyboardEventKind
import org.jetbrains.skiko.SkikoPointer
import org.jetbrains.skiko.SkikoPointerDevice
import org.jetbrains.skiko.SkikoPointerEvent
import org.jetbrains.skiko.SkikoPointerEventKind


internal interface SkikoUIViewDelegate {

    fun pointInside(point: CValue<CGPoint>, event: UIEvent?): Boolean

    fun onTouchesEvent(view: UIView, event: UIEvent, phase: UITouchesEventPhase)

    fun retrieveInteropTransaction(): UIKitInteropTransaction

    fun render(canvas: Canvas, targetTimestamp: NSTimeInterval)

    /**
     * A callback invoked when [UIView.didMoveToWindow] receives non null window
     */
    fun onAttachedToWindow() {}
}

internal enum class UITouchesEventPhase {
    BEGAN, MOVED, ENDED, CANCELLED
}

@Suppress("CONFLICTING_OVERLOADS")//todo in all places redundant?
internal class SkikoUIView(
    private val focusable: Boolean,
    private val keyboardEventHandler: KeyboardEventHandler,
    private val delegate: SkikoUIViewDelegate,
) : UIView(frame = CGRectZero.readValue()) {

    companion object : UIViewMeta() {//todo redundant?
        override fun layerClass() = CAMetalLayer
    }

    var onAttachedToWindow: (() -> Unit)? = null
    private val _isReadyToShowContent: MutableState<Boolean> = mutableStateOf(false)
    val isReadyToShowContent: State<Boolean> = _isReadyToShowContent

    private val _device: MTLDeviceProtocol =
        MTLCreateSystemDefaultDevice() ?: throw IllegalStateException("Metal is not supported on this system")
    private val _metalLayer: CAMetalLayer get() = layer as CAMetalLayer
    private val _redrawer: MetalRedrawer = MetalRedrawer(
        _metalLayer,
        callbacks = object : MetalRedrawerCallbacks {
            override fun render(canvas: Canvas, targetTimestamp: NSTimeInterval) {
                delegate.render(canvas, targetTimestamp)
            }

            override fun retrieveInteropTransaction(): UIKitInteropTransaction =
                delegate.retrieveInteropTransaction()

        }
    )

    /*
     * When there at least one tracked touch, we need notify redrawer about it. It should schedule CADisplayLink which
     * affects frequency of polling UITouch events on high frequency display and forces it to match display refresh rate.
     */
    private var _touchesCount = 0
        set(value) {
            field = value

            val needHighFrequencyPolling = value > 0

            _redrawer.needsProactiveDisplayLink = needHighFrequencyPolling
        }

    init {
        multipleTouchEnabled = true
        translatesAutoresizingMaskIntoConstraints = false

        _metalLayer.also {
            // Workaround for KN compiler bug
            // Type mismatch: inferred type is platform.Metal.MTLDeviceProtocol but objcnames.protocols.MTLDeviceProtocol? was expected
            @Suppress("USELESS_CAST")
            it.device = _device as objcnames.protocols.MTLDeviceProtocol?

            it.pixelFormat = MTLPixelFormatBGRA8Unorm
            doubleArrayOf(0.0, 0.0, 0.0, 0.0).usePinned { pinned ->
                it.backgroundColor = CGColorCreate(CGColorSpaceCreateDeviceRGB(), pinned.addressOf(0))
            }
            it.setOpaque(true)
            it.framebufferOnly = false
        }
    }

    override fun isUserInteractionEnabled(): Boolean = focusable

    fun needRedraw() = _redrawer.needRedraw()

    var isForcedToPresentWithTransactionEveryFrame by _redrawer::isForcedToPresentWithTransactionEveryFrame

    fun dispose() {
        _redrawer.dispose()
        removeFromSuperview()
    }

    override fun didMoveToWindow() {
        super.didMoveToWindow()

        window?.screen?.let {
            contentScaleFactor = it.scale
            _redrawer.maximumFramesPerSecond = it.maximumFramesPerSecond
        }
        if (window != null) {
            onAttachedToWindow?.invoke()
            delegate.onAttachedToWindow()
            _isReadyToShowContent.value = true
        }
    }

    override fun layoutSubviews() {
        super.layoutSubviews()

        val scaledSize = bounds.useContents {
            CGSizeMake(size.width * contentScaleFactor, size.height * contentScaleFactor)
        }

        // If drawableSize is zero in any dimension it means that it's a first layout
        // we need to synchronously dispatch first draw and block until it's presented
        // so user doesn't have a flicker
        val needsSynchronousDraw = _metalLayer.drawableSize.useContents {
            width == 0.0 || height == 0.0
        }

        _metalLayer.drawableSize = scaledSize

        if (needsSynchronousDraw) {
            _redrawer.drawSynchronously()
        }
    }

    override fun canBecomeFirstResponder() = true

    override fun pressesBegan(presses: Set<*>, withEvent: UIPressesEvent?) {//todo duplicate
        if (withEvent != null) {
            for (press in withEvent.allPresses) {
                val uiPress = press as? UIPress
                if (uiPress != null) {
                    keyboardEventHandler.onKeyboardEvent(
                        toSkikoKeyboardEvent(press, SkikoKeyboardEventKind.DOWN)
                    )
                }
            }
        }
        super.pressesBegan(presses, withEvent)
    }

    override fun pressesEnded(presses: Set<*>, withEvent: UIPressesEvent?) {//todo duplicate
        if (withEvent != null) {
            for (press in withEvent.allPresses) {
                val uiPress = press as? UIPress
                if (uiPress != null) {
                    keyboardEventHandler.onKeyboardEvent(
                        toSkikoKeyboardEvent(press, SkikoKeyboardEventKind.UP)
                    )
                }
            }
        }
        super.pressesEnded(presses, withEvent)
    }

    /**
     * https://developer.apple.com/documentation/uikit/uiview/1622533-point
     */
    override fun pointInside(point: CValue<CGPoint>, withEvent: UIEvent?): Boolean =
        delegate.pointInside(point, withEvent) ?: super.pointInside(point, withEvent)


    override fun touchesBegan(touches: Set<*>, withEvent: UIEvent?) {
        super.touchesBegan(touches, withEvent)

        _touchesCount += touches.size

        withEvent?.let { event ->
            delegate.onTouchesEvent(this, event, UITouchesEventPhase.BEGAN)
        }
    }

    override fun touchesEnded(touches: Set<*>, withEvent: UIEvent?) {
        super.touchesEnded(touches, withEvent)

        _touchesCount -= touches.size

        withEvent?.let { event ->
            delegate.onTouchesEvent(this, event, UITouchesEventPhase.ENDED)
        }
    }

    override fun touchesMoved(touches: Set<*>, withEvent: UIEvent?) {
        super.touchesMoved(touches, withEvent)

        withEvent?.let { event ->
            delegate.onTouchesEvent(this, event, UITouchesEventPhase.MOVED)
        }
    }

    override fun touchesCancelled(touches: Set<*>, withEvent: UIEvent?) {
        super.touchesCancelled(touches, withEvent)

        _touchesCount -= touches.size

        withEvent?.let { event ->
            delegate.onTouchesEvent(this, event, UITouchesEventPhase.CANCELLED)
        }
    }

}
