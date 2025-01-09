/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.uikit

import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.asDpRect
import androidx.compose.ui.unit.roundToIntRect
import androidx.compose.ui.unit.toRect
import kotlinx.cinterop.CValue
import kotlinx.cinterop.refTo
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ImageInfo
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGColorSpaceRelease
import platform.CoreGraphics.CGContextRef
import platform.CoreGraphics.CGContextRelease
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.kCGImageByteOrder32Little
import platform.UIKit.UIView

internal class UIViewBitmapRecorder(
    val view: UIView
) {
    private var size: IntSize? = null
    private var bitmap: Bitmap? = null
    private var data: ByteArray? = null

    private var context: CGContextRef? = null

    val viewImageBitmap get() = bitmap?.asComposeImageBitmap()

    fun dispose() {
        this.size = null
        releaseContext()
        closeBitmap()
    }

    private fun initializeBitmap(size: IntSize) {
        val imageInfo = ImageInfo.makeS32(size.width, size.height, ColorAlphaType.PREMUL)
        bitmap = Bitmap().apply { allocPixelsFlags(imageInfo, zeroPixels = false) }

        val bytesPerPixel = 4
        data = ByteArray(size.width * size.height * bytesPerPixel)
    }

    private fun closeBitmap() {
        bitmap?.close()
        bitmap = null
        data = null
    }

    private fun initializeContext(size: IntSize) {
        val colorSpace = CGColorSpaceCreateDeviceRGB()
        val bitmapInfo =
            CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value or kCGImageByteOrder32Little
        val bytesPerPixel = 4
        context = CGBitmapContextCreate(
            data = data!!.refTo(0),
            width = size.width.toULong(),
            height = size.height.toULong(),
            bitsPerComponent = 8U,
            bytesPerRow = (size.width * bytesPerPixel).toULong(),
            space = colorSpace,
            bitmapInfo = bitmapInfo
        )
        CGColorSpaceRelease(colorSpace)
    }

    private fun releaseContext() {
        CGContextRelease(context)
        context = null
    }

    fun drawRect(rect: CValue<CGRect>) {
        val density = view.density
        val size = rect.asDpRect().toRect(density).roundToIntRect().size
        if (this.size != size) {
            this.size = size
            releaseContext()
            closeBitmap()

            initializeBitmap(size)
            initializeContext(size)
        }

        // TODO: Clear? Scale? Translate?
        view.layer.renderInContext(context)
        bitmap?.installPixels(data)
    }
}
