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

package androidx.compose.ui.graphics

import androidx.compose.ui.graphics.colorspace.ColorSpace
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import org.jetbrains.skia.impl.getPtr
import platform.CoreGraphics.CGColorSpaceGetModel
import platform.CoreGraphics.CGImageGetBitmapInfo
import platform.CoreGraphics.CGImageGetBitsPerComponent
import platform.CoreGraphics.CGImageGetBitsPerPixel
import platform.CoreGraphics.CGImageGetColorSpace
import platform.CoreGraphics.CGImageGetHeight
import platform.CoreGraphics.CGImageGetWidth
import platform.CoreGraphics.CGImageRef
import platform.CoreGraphics.kCGBitmapAlphaInfoMask
import platform.CoreGraphics.kCGColorSpaceModelMonochrome
import platform.CoreGraphics.kCGColorSpaceModelRGB
import platform.UIKit.UIImage
import androidx.compose.ui.uikit.utils.tmm_decodedData

class UikitImageBitmap(val uiImage: UIImage ?= null, val skiaBackedImageBitmap: ImageBitmap? = null) : ImageBitmap {
    constructor(image: UIImage) : this(image, null) {
    }
    constructor(skiaBackedImageBitmap: ImageBitmap) : this(null, skiaBackedImageBitmap) {
    }

    override val width: Int
        get() = if (uiImage != null) {
            CGImageGetWidth(uiImage.CGImage).toInt()
        } else skiaBackedImageBitmap?.width ?: 0
    override val height: Int
        get() = if (uiImage != null) {CGImageGetHeight(uiImage.CGImage).toInt()} else skiaBackedImageBitmap?.height ?: 0
    override val config: ImageBitmapConfig
        get() = if (uiImage != null) imageFormatFromCGImage(uiImage.CGImage) else skiaBackedImageBitmap?.config ?: ImageBitmapConfig.Unknown

    // TODO, 获取真实的ColorSpace
    override val colorSpace: ColorSpace
        get() = ColorSpaces.Srgb
    override val hasAlpha: Boolean
        get() = (CGImageGetBitmapInfo(uiImage?.CGImage).toInt() and kCGBitmapAlphaInfoMask.toInt()) != 0

    override fun readPixels(
        buffer: IntArray,
        startX: Int,
        startY: Int,
        width: Int,
        height: Int,
        bufferOffset: Int,
        stride: Int
    ) {
        // TODO 暂时不需要
    }

    override fun prepareToDraw() {
        // TODO 暂时不需要
    }


    private fun imageFormatFromCGImage(imageRef: CGImageRef?): ImageBitmapConfig {
        val colorSpace = CGImageGetColorSpace(imageRef)
        val colorSpaceModel = CGColorSpaceGetModel(colorSpace)
        val bitsPerComponent = CGImageGetBitsPerComponent(imageRef).toInt()
        val bitsPerPixel = CGImageGetBitsPerPixel(imageRef).toInt()
        val bitmapInfo = CGImageGetBitmapInfo(imageRef)
        val hasAlpha = (bitmapInfo.toInt() and kCGBitmapAlphaInfoMask.toInt()) != 0

        if (colorSpaceModel == kCGColorSpaceModelRGB) {
            if (bitsPerComponent == 8 && bitsPerPixel == 32 && hasAlpha) {
                return ImageBitmapConfig.Argb8888
            } else if (bitsPerComponent == 5 && bitsPerPixel == 16 && !hasAlpha) {
                return ImageBitmapConfig.Rgb565
            }
        } else if (colorSpaceModel == kCGColorSpaceModelMonochrome) {
            if (bitsPerComponent == 8 && bitsPerPixel == 8 && hasAlpha) {
                return ImageBitmapConfig.Alpha8
            }
        }

        return ImageBitmapConfig.Unknown
    }

    override fun toSkiaBackedImageBitmap(): ImageBitmap?
            = skiaBackedImageBitmap ?: uiImage?.tmm_decodedData()?.tmmToImageBitmapFromDecoded(this.width, this.height)
}

fun ImageBitmap.skBitmapPtr(): Long = if (this is SkiaBackedImageBitmap) getPtr(this.bitmap).toLong() else 0L

fun ImageBitmap.asCGImage(): CGImageRef? =
    when (this) {
        is UikitImageBitmap -> uiImage?.CGImage()
        is SkiaBackedImageBitmap -> bitmapToCGImage(bitmap)?.CGImage
        else -> throw UnsupportedOperationException("Unable to obtain CGImageRef")
    }