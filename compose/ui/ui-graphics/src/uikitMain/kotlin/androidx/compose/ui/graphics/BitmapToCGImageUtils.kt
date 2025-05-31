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

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import platform.CoreGraphics.CGBitmapInfo
import platform.CoreGraphics.CGBlendMode
import platform.CoreGraphics.CGColorRenderingIntent
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGColorSpaceRelease
import platform.CoreGraphics.CGContextDrawImage
import platform.CoreGraphics.CGContextScaleCTM
import platform.CoreGraphics.CGContextSetBlendMode
import platform.CoreGraphics.CGContextTranslateCTM
import platform.CoreGraphics.CGDataProviderCreateWithData
import platform.CoreGraphics.CGDataProviderRelease
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGImageCreate
import platform.CoreGraphics.CGImageRelease
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.CoreGraphics.kCGBitmapByteOrder16Little
import platform.CoreGraphics.kCGBitmapByteOrder32Big
import platform.CoreGraphics.kCGBitmapByteOrder32Little
import platform.Foundation.NSData
import platform.UIKit.UIGraphicsBeginImageContext
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetCurrentContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.posix.CHAR_BIT
import platform.posix.size_t
import kotlin.experimental.xor
import platform.posix.memcpy

private fun computeCgAlphaInfoRgba(at: ColorAlphaType): CGBitmapInfo {
    val info: CGBitmapInfo = kCGBitmapByteOrder32Big
    return when (at) {
        ColorAlphaType.UNKNOWN -> info
        ColorAlphaType.OPAQUE -> info or CGImageAlphaInfo.kCGImageAlphaNoneSkipLast.value
        ColorAlphaType.PREMUL -> info or CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value
        ColorAlphaType.UNPREMUL -> info or CGImageAlphaInfo.kCGImageAlphaLast.value
    }
}

private fun computeCgAlphaInfoBgra(at: ColorAlphaType): CGBitmapInfo {
    val info: CGBitmapInfo = kCGBitmapByteOrder32Little
    return when (at) {
        ColorAlphaType.UNKNOWN -> info
        ColorAlphaType.OPAQUE -> info or CGImageAlphaInfo.kCGImageAlphaNoneSkipFirst.value
        ColorAlphaType.PREMUL -> info or CGImageAlphaInfo.kCGImageAlphaPremultipliedFirst.value
        ColorAlphaType.UNPREMUL -> info or CGImageAlphaInfo.kCGImageAlphaFirst.value
    }
}

private fun computeCgAlphaInfo4444(at: ColorAlphaType): CGBitmapInfo {
    val info: CGBitmapInfo = kCGBitmapByteOrder16Little
    return when (at) {
        ColorAlphaType.OPAQUE -> info or CGImageAlphaInfo.kCGImageAlphaNoneSkipLast.value
        else -> info or CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value
    }
}

fun bitmapToCGImage(bitmap: Bitmap): UIImage? {
    if (bitmap.width == 0 || bitmap.height == 0 || bitmap.drawsNothing()) {
        return null
    }
    val bitsPerComponent: size_t = if (bitmap.colorType == ColorType.ARGB_4444) 4u else 8u
    val bitmapInfo = bitmap.toCGBitmapInfo() ?: return null
    val bitmapResult = if (bitmap.colorType == ColorType.RGB_565) {
        val copyBitmap = Bitmap()
        copyBitmap.allocPixels(bitmap.imageInfo.withColorType(ColorType.RGBA_8888))
        bitmap.readPixels(copyBitmap.imageInfo, copyBitmap.rowBytes, 0, 0)
        copyBitmap
    } else {
        bitmap
    }

    val finalPixels = getPixelArray(bitmapResult)
    finalPixels?.usePinned { pinned ->
        val dataRef = CGDataProviderCreateWithData(
            null,
            pinned.addressOf(0),
            finalPixels.size.toULong(),
            null
        )

        val width = bitmapResult.width
        val height = bitmapResult.height
        // 由于iOS不支持Alpha8类型的图，所以会将Alpha8的图转成RGBA8888，每个像素占的字节也要从1byte转换成4byte
        val alpha8Ratio = if (bitmap.colorType == ColorType.ALPHA_8) 4u else 1u
        val bytesPerPixel = (bitmapResult.imageInfo.bytesPerPixel * CHAR_BIT).toULong() * alpha8Ratio
        val colorSpaceRef = CGColorSpaceCreateDeviceRGB()
        val imageRef = CGImageCreate(
            width.toULong(),
            height.toULong(),
            bitsPerComponent,
            bytesPerPixel,
            bitmapResult.rowBytes.toULong() * alpha8Ratio,
            colorSpaceRef,
            bitmapInfo,
            dataRef,
            null,
            false,
            CGColorRenderingIntent.kCGRenderingIntentDefault
        )
        UIGraphicsBeginImageContext(CGSizeMake(width.toDouble(), height.toDouble()))
        val cgContext = UIGraphicsGetCurrentContext()
        CGContextTranslateCTM(cgContext, 0.0, height.toDouble())
        CGContextScaleCTM(cgContext, 1.0, -1.0)
        CGContextSetBlendMode(cgContext, CGBlendMode.kCGBlendModeCopy)
        CGContextDrawImage(
            cgContext,
            CGRectMake(0.0, 0.0, width.toDouble(), height.toDouble()),
            imageRef
        )

        val image = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        CGColorSpaceRelease(colorSpaceRef)
        CGDataProviderRelease(dataRef)
        CGImageRelease(imageRef)
        return image
    }
    return null
}

private fun getPixelArray(bitmap: Bitmap): ByteArray? {
    val pixels = bitmap.readPixels(bitmap.imageInfo, bitmap.rowBytes, 0, 0)
    if (pixels != null && bitmap.colorType == ColorType.ALPHA_8) {
        // 转换字节数组，将Alpha8转换成ARGB8888的数组
        val finalPixels = ByteArray(pixels.size * 4)
        for (i in pixels.indices) {
            finalPixels[i * 4] = 0 // R
            finalPixels[i * 4 + 1] = 0 // G
            finalPixels[i * 4 + 2] = 0 // B
            finalPixels[i * 4 + 3] = pixels[i] // A
        }
        // TODO: releaseData
        return finalPixels
    } else {
        // TODO: releaseData
        return pixels
    }
}

fun Bitmap.toCGBitmapInfo(): CGBitmapInfo? {
    return when (this.colorType) {
        ColorType.RGB_565 -> computeCgAlphaInfoRgba(this.alphaType)
        ColorType.RGBA_8888 -> computeCgAlphaInfoRgba(this.alphaType)
        ColorType.BGRA_8888 -> computeCgAlphaInfoBgra(this.alphaType)
        ColorType.ARGB_4444 -> computeCgAlphaInfo4444(this.alphaType)
        // 如果是Alpha_8类型，则不需要处理Alpha
        ColorType.ALPHA_8 -> computeCgAlphaInfoRgba(this.alphaType)
        else -> null
    }
}

private fun NSData.tmmToBytearray(): ByteArray {
    val result = ByteArray(length.toInt())
    if (result.isEmpty()) return result
    result.usePinned {
        memcpy(it.addressOf(0), bytes, length)
    }
    return result
}
fun NSData.tmmToImageBitmapFromDecoded(width: Int, height: Int): ImageBitmap? {
    if (this.length.toInt() != width * height * 4) return null
    tmmToBytearray().let { byteArray ->
        // iOS传过来的是 RGBA_8888，4byte 为一像素
        // 需要转成 ARGB_8888，实际颜色值是 BGRA
        val size = byteArray.size / 4
        var i = 0
        while (i < size) {
            // RGBA => BGRA
            byteArray[i * 4] = byteArray[i * 4] xor byteArray[i * 4 + 2]
            byteArray[i * 4 + 2] = byteArray[i * 4] xor byteArray[i * 4 + 2]
            byteArray[i * 4] = byteArray[i * 4] xor byteArray[i * 4 + 2]
            i++
        }
        // 创建指定大小的 ImageBitmap
        return ImageBitmap(
            width = width,
            height = height,
            config = ImageBitmapConfig.Argb8888,
        ).apply {
            // 更新像素值
            asSkiaBitmap().installPixels(byteArray)
        }
    }
}