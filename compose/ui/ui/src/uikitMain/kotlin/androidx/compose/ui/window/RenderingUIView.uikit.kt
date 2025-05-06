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

import androidx.compose.ui.graphics.traceAction
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGColorCreate
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGRectIsEmpty
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Metal.MTLCreateSystemDefaultDevice
import platform.Metal.MTLDeviceProtocol
import platform.Metal.MTLPixelFormatBGRA8Unorm
import platform.QuartzCore.CAMetalLayer
import platform.UIKit.UIView
import platform.UIKit.UIViewMeta

// region Tencent Code
internal class RenderingUIView(
    private val component: RenderingComponentForSkia
) : UIView(
    frame = CGRectMake(
        x = 0.0,
        y = 0.0,
        width = 1.0, // TODO: Non-zero size need to first render with ComposeSceneLayer
        height = 1.0
    )
) {

    companion object : UIViewMeta() {
        override fun layerClass() = CAMetalLayer
    }

    private val device: MTLDeviceProtocol =
        MTLCreateSystemDefaultDevice()
            ?: throw IllegalStateException("Metal is not supported on this system")
    private val metalLayer: CAMetalLayer get() = layer as CAMetalLayer

    override fun setOpaque(opaque: Boolean) {
        super.setOpaque(opaque)

        component.redrawer.opaque = opaque
    }

    init {
        userInteractionEnabled = false
        metalLayer.also {
            // Workaround for KN compiler bug
            // Type mismatch: inferred type is platform.Metal.MTLDeviceProtocol but objcnames.protocols.MTLDeviceProtocol? was expected
            @Suppress("USELESS_CAST")
            it.device = device as objcnames.protocols.MTLDeviceProtocol?

            it.pixelFormat = MTLPixelFormatBGRA8Unorm
            doubleArrayOf(0.0, 0.0, 0.0, 0.0).usePinned { pinned ->
                it.backgroundColor =
                    CGColorCreate(CGColorSpaceCreateDeviceRGB(), pinned.addressOf(0))
            }
            it.framebufferOnly = false
        }
    }

    fun dispose() {
        component.dispose()
    }

    override fun didMoveToWindow() {
        super.didMoveToWindow()

        component.didMoveToWindow()
        updateMetalLayerSize()
    }

    override fun layoutSubviews() {
        // region Tencent Code
        traceAction("RenderingUIView layoutSubviews") {
        // endregion
            super.layoutSubviews()
            updateMetalLayerSize()
        }
    }

    private fun updateMetalLayerSize() {
        if (window == null || CGRectIsEmpty(bounds)) {
            return
        }
        val scaledSize = bounds.useContents {
            CGSizeMake(size.width * contentScaleFactor, size.height * contentScaleFactor)
        }

        // If drawableSize is zero in any dimension it means that it's a first layout
        // we need to synchronously dispatch first draw and block until it's presented
        // so user doesn't have a flicker
        val needsSynchronousDraw = metalLayer.drawableSize.useContents {
            width == 0.0 || height == 0.0
        }

        metalLayer.drawableSize = scaledSize

        if (needsSynchronousDraw) {
            component.redrawer.drawSynchronously()
        }
    }

    override fun canBecomeFirstResponder() = false

}
// endregion
