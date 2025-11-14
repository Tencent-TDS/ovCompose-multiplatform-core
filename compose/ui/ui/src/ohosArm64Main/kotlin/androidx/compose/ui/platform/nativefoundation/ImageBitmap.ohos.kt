package androidx.compose.ui.platform.nativefoundation

import androidx.compose.common.interop.LogPrintUtil
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_createNativePixelMapFromPixels
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.posix.uint8_tVar

/**
 * Extension function to convert ImageBitmap to NativePixelMap handle.
 *
 * Reads pixel data from ImageBitmap (ARGB format) and converts it to BGRA format,
 * then creates NativePixelMap using C++ helper function.
 *
 * @return NativePixelMap handle (COpaquePointer) or null if conversion fails
 */
@OptIn(ExperimentalForeignApi::class)
internal fun ImageBitmap.asNativePixelMap(): COpaquePointer? {
    try {
        // 读取像素数据（ARGB格式，每个Int代表一个像素）
        val pixelCount = width * height
        if (pixelCount <= 0) {
            println("ImageBitmap.asNativePixelMap: invalid image size: ${width}x${height}")
            return null
        }
        val argbPixels = IntArray(pixelCount)
        readPixels(
            buffer = argbPixels,
            startX = 0,
            startY = 0,
            width = width,
            height = height
        )

        // 将ARGB IntArray转换为BGRA ByteArray
        // ARGB格式：每个Int = (A << 24) | (R << 16) | (G << 8) | B
        // BGRA格式：每个像素4字节 = [B, G, R, A]
        val bgraData = ByteArray(pixelCount * 4)
        for (i in argbPixels.indices) {
            val argb = argbPixels[i]
            val a = (argb shr 24) and 0xFF
            val r = (argb shr 16) and 0xFF
            val g = (argb shr 8) and 0xFF
            val b = argb and 0xFF

            // 转换为BGRA格式
            val offset = i * 4
            bgraData[offset] = b.toByte()     // B
            bgraData[offset + 1] = g.toByte() // G
            bgraData[offset + 2] = r.toByte() // R
            bgraData[offset + 3] = a.toByte() // A
        }

        // 调用C++函数创建NativePixelMap
        // usePinned确保ByteArray在C++函数调用期间保持pinned状态
        val result = bgraData.usePinned { pinned ->
            val pixelDataPtr = pinned.addressOf(0).reinterpret<uint8_tVar>()
            // 注意：OH_PixelmapNative_CreatePixelmap会复制数据，所以usePinned结束后unpin是安全的
            androidx_compose_ui_arkui_utils_createNativePixelMapFromPixels(
                pixelData = pixelDataPtr,
                dataLength = bgraData.size.toULong(),
                width = width,
                height = height,
                hasAlpha = hasAlpha
            )
        }
        if (result == null) {
            LogPrintUtil.verbose {
                "ImageBitmap.asNativePixelMap: failed to create NativePixelMap from pixels, " +
                        "image: ${width}x${height}, hasAlpha: $hasAlpha, dataLength: ${bgraData.size}"
            }
        }
        return result
    } catch (e: Exception) {
        // 转换失败时返回null
        LogPrintUtil.verbose { "ImageBitmap.asNativePixelMap: exception during conversion: ${e.message}" }
        e.printStackTrace()
        return null
    }
}

