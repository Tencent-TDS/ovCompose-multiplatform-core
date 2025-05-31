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

package androidx.compose.ui.window

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.interop.UIKitInteropTransaction
import androidx.compose.ui.platform.v2.UIViewRedrawer
import androidx.compose.ui.uikit.FirstFrameRenderConfig
import androidx.compose.ui.uikit.RenderBackend
import platform.Foundation.NSTimeInterval
import platform.QuartzCore.CAMetalLayer
import platform.UIKit.UIView

@Suppress("LeakingThis")
internal sealed class RenderingComponent<T : UIView> {

    interface Delegate {
        fun retrieveInteropTransaction(): UIKitInteropTransaction
        fun render(canvas: Canvas, targetTimestamp: NSTimeInterval)
    }

    var onAttachedToWindow: (() -> Unit)? = null

    private val _isReadyToShowContent: MutableState<Boolean> = mutableStateOf(false)
    val isReadyToShowContent: State<Boolean> = _isReadyToShowContent

    val view: T = createView()
    abstract val redrawer: Redrawer

    var isForcedToPresentWithTransactionEveryFrame: Boolean
        get() {
            return redrawer.isForcedToPresentWithTransactionEveryFrame
        }

        set(value) {
            redrawer.isForcedToPresentWithTransactionEveryFrame = value
        }

    protected abstract fun createView(): T

    open fun needRedrawSynchronously() = Unit

    fun needRedraw() = redrawer.needRedraw()

    fun didMoveToWindow() {
        val window = view.window ?: return

        val screen = window.screen
        view.contentScaleFactor = screen.scale
        redrawer.maximumFramesPerSecond = screen.maximumFramesPerSecond
        onAttachedToWindow?.invoke()
        _isReadyToShowContent.value = true
    }

    fun dispose() {
        redrawer.dispose()
    }
}

internal class RenderingComponentForSkia(renderDelegate: Delegate) :
    RenderingComponent<RenderingUIView>() {

    override val redrawer: MetalRedrawer = MetalRedrawer(
        view.layer as CAMetalLayer,
        callbacks = object : RedrawerCallbacks {
            override fun render(canvas: Canvas, targetTimestamp: NSTimeInterval) {
                renderDelegate.render(canvas, targetTimestamp)
            }

            override fun retrieveInteropTransaction(): UIKitInteropTransaction =
                renderDelegate.retrieveInteropTransaction()
        }
    )

    override fun createView(): RenderingUIView =
        RenderingUIView(this)
}

internal class RenderingComponentForUIView(
    renderDelegate: Delegate,
    firstFrameRenderConfig: FirstFrameRenderConfig? = null
) : RenderingComponent<RenderingUIViewForUIViewBackend>() {

    override val redrawer: UIViewRedrawer = UIViewRedrawer(object : RedrawerCallbacks {
        private var firstFrameExecuted = false

        override fun render(canvas: Canvas, targetTimestamp: NSTimeInterval) {
            renderDelegate.render(canvas, targetTimestamp)
            val config = firstFrameRenderConfig ?: return
            if (config.firstFrameReady && !firstFrameExecuted) {
                firstFrameExecuted = true
                config.firstFrameRenderCallback?.invoke()
            }
        }

        override fun retrieveInteropTransaction(): UIKitInteropTransaction =
            renderDelegate.retrieveInteropTransaction()
    })

    override fun createView(): RenderingUIViewForUIViewBackend =
        RenderingUIViewForUIViewBackend(this)

    override fun needRedrawSynchronously() {
        view.drawSynchronouslyOnLayout = true
    }
}

internal fun RenderingComponent(
    backend: RenderBackend,
    renderDelegate: RenderingComponent.Delegate,
    firstFrameRenderConfig: FirstFrameRenderConfig? = null
): RenderingComponent<*> {
    return when (backend) {
        RenderBackend.Skia -> RenderingComponentForSkia(renderDelegate)
        RenderBackend.UIView -> RenderingComponentForUIView(renderDelegate, firstFrameRenderConfig)
    }
}