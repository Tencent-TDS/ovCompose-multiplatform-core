package androidx.compose.ui.platform

import androidx.compose.runtime.ComposeTabService
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.NativeCanvas
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.platform.v2.nativefoundation.IOSNativeCanvas
import org.jetbrains.skia.Picture
import org.jetbrains.skia.PictureRecorder

private class TextPictureImpl(private val picture:Picture) : TextPicture {
    override fun drawNativeCanvas(nativeCanvas: NativeCanvas) {
        nativeCanvas.drawPicture(picture)
    }

    override fun close() {
        picture.close()
    }
}

private class TextPictureRecorderImpl() : TextPictureRecorder {
    private val skRecorder = PictureRecorder()

    override fun beginRecording(width: Float, height: Float): Canvas {
        val rect = org.jetbrains.skia.Rect.makeWH(width, height)
        return skRecorder.beginRecording(rect).asComposeCanvas()
    }

    override fun finishRecordingAsPicture(): TextPicture {
        val picture = skRecorder.finishRecordingAsPicture()
        return TextPictureImpl(picture)
    }

    override fun close() {
        skRecorder.close()
    }
}

/** IOS平台的文本节点 */
class IOSTextNode : PlatformTextNode {
    override fun createTextPictureRecorder(): TextPictureRecorder? {
        val recorder = TextPictureRecorderImpl()
        return recorder
    }

    override fun needRedrawText(
        nativeCanvas: Canvas,
        paragraphHashKey: Int,
        width: Int,
        height: Int,
    ): Boolean {
        if (nativeCanvas is IOSNativeCanvas) {
            return nativeCanvas.needRedrawImageWithHashCode(
                paragraphHashCode = paragraphHashKey,
                width = width,
                height = height
            )
        }
        return false
    }

    // width, height暂时保留
    override fun renderTextImage(
        imageBitmap: ImageBitmap?,
        width: Int,
        height: Int,
        paragraphHashCode: Int,
        nativeCanvas: Canvas
    ) {
        imageBitmap?.let {
            if (nativeCanvas is IOSNativeCanvas) {
                nativeCanvas.drawParagraphImage(
                    image = it,
                    paragraphHashCode = paragraphHashCode,
                    width = width,
                    height = height
                )
            }
        }
    }

    override fun drawNullText(
        nativeCanvas: Canvas,
        paragraphHashKey: Int,
        width: Int,
        height: Int
    ) {
        if (nativeCanvas is IOSNativeCanvas) {
            nativeCanvas.drawNullTextImage(
                paragraphHashCode = paragraphHashKey,
                width = width,
                height = height
            )
        }
    }

    override fun imageFromImageBitmap(
        nativeCanvas: Canvas,
        paragraphHashCode: Int,
        imageBitmap: ImageBitmap
    ): Long {
        if (nativeCanvas is IOSNativeCanvas) {
            return nativeCanvas.imageFromImageBitmap(paragraphHashCode, imageBitmap)
        }
        throw RuntimeException("nativeCanvas is not IOSNativeCanvas")
    }

    override fun asyncDrawIntoCanvas(
        nativeCanvas: Canvas,
        asyncTask: () -> Long,
        paragraphHashCode: Int,
        width: Int,
        height: Int,
        drawCompleted: () -> Unit
    ) {
        if (nativeCanvas is IOSNativeCanvas) {
            nativeCanvas.asyncDrawIntoCanvas(asyncTask, paragraphHashCode, width, height, drawCompleted)
        }
    }

    override fun enableTextAsyncPaint(nativeCanvas: Canvas): Boolean {
        // 全局关闭文本异步 Paint
        if (ComposeTabService.closeAsyncPaintEnable) {
            return false
        }
        if (nativeCanvas is IOSNativeCanvas) {
            val config = nativeCanvas.experimentalConfig() ?: return false
            return config.enableTextAsyncPaint
        }
        return false
    }
}
