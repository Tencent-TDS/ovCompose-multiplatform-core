package androidx.compose.ui.platform

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.v2.nativefoundation.IOSNativeCanvas

/** IOS平台的文本节点 */
class IOSTextNode : PlatformTextNode {

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
        height: Int
    ) {
        if (nativeCanvas is IOSNativeCanvas) {
            nativeCanvas.asyncDrawIntoCanvas(asyncTask, paragraphHashCode, width, height)
        }
    }
}
