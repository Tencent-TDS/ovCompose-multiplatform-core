package androidx.compose.ui.platform.nativefoundation

import androidx.compose.common.interop.LogPrintUtil
import androidx.compose.common.interop.TraceUtil
import androidx.compose.ui.arkui.utils.BaseRenderNode_Handle
import androidx.compose.ui.arkui.utils.OHComposeNativePaint_Handle
import androidx.compose.ui.arkui.utils.OHNativeCanvasProxy_Handle
import androidx.compose.ui.arkui.utils.OH_Native_Draw_ClipOp
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_Paint
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_createOHNativeCanvasProxy
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CanvasType
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathType
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.Vertices
import androidx.compose.ui.node.LayerSourceType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.cinterop.COpaquePointer

private inline fun ClipOp.asNativeEnum(): OH_Native_Draw_ClipOp {
    return when (this) {
        ClipOp.Difference -> OH_Native_Draw_ClipOp.Difference
        ClipOp.Intersect -> OH_Native_Draw_ClipOp.Intersect
        else -> throw RuntimeException("暂不支持")
    }
}

private inline fun CornerRadius.greaterThen(rhs: CornerRadius): Boolean {
    return x > rhs.x && y > rhs.y
}

/**
 * Adaptive canvas implementation for HarmonyOS platform.
 * Provides a native canvas wrapper that delegates drawing operations to the underlying
 * HarmonyOS native graphics API through OHNativeCanvasProxy.
 *
 * @param factory A native pointer to the canvas factory used to create the native canvas proxy
 */
internal class AdaptiveCanvas(
    factory: COpaquePointer,
    private val sourceType: LayerSourceType = LayerSourceType.REGULAR
) : OHOSNativeCanvas {
    override val canvasType: CanvasType get() = CanvasType.Native

    val nativeCanvasProxy: OHNativeCanvasProxy
    private val nativePaint: OHComposeNativePaint

    init {
        val rawCanvasProxyHandle: OHNativeCanvasProxy_Handle? =
            androidx_compose_ui_arkui_utils_createOHNativeCanvasProxy(factory)
        val rawNativePaintHandle: OHComposeNativePaint_Handle? =
            androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_Paint(rawCanvasProxyHandle)
        nativeCanvasProxy = OHNativeCanvasProxy(rawCanvasProxyHandle)
        nativePaint = OHComposeNativePaint(rawNativePaintHandle)
    }


    override fun onPreDraw() {
        TraceUtil.traceSync("AdaptiveCanvas:onPreDraw") {
            nativeCanvasProxy.beginDraw()
        }
    }

    override fun drawLayer(renderNodeHandle: BaseRenderNode_Handle) {
        TraceUtil.traceSync("AdaptiveCanvas:drawLayer") {
            nativeCanvasProxy.drawLayer(renderNodeHandle)
        }
    }

    override fun drawParagraph(paragraph: BaseRenderNode_Handle) {
        TraceUtil.traceSync("AdaptiveCanvas:drawParagraph") {
            nativeCanvasProxy.drawParagraph(paragraph)
        }
    }

    override fun drawLayerWithNativeCanvas(nativeCanvas: OHOSNativeCanvas) {
        TraceUtil.traceSync("AdaptiveCanvas:drawLayerWithNativeCanvas") {
            nativeCanvasProxy.drawLayerWithSubproxy((nativeCanvas as AdaptiveCanvas).nativeCanvasProxy)
        }
    }

    override fun onPostDraw() {
        TraceUtil.traceSync("AdaptiveCanvas:onPostDraw") {
            nativeCanvasProxy.finishDraw().also {
                if (sourceType == LayerSourceType.LAZY_LIST_ITEM) {
                    nativeCanvasProxy.markSelfAsNodeGroup()
                }
            }
        }
    }

    override fun clipRoundRect(rect: RoundRect) {
        TraceUtil.traceSync("AdaptiveCanvas:clipRoundRect") {
            // 使用默认的 ClipOp.Intersect
            nativeCanvasProxy.clipRoundRect(
                rect.left, rect.top, rect.right, rect.bottom,
                rect.topLeftCornerRadius.x, rect.topLeftCornerRadius.y,
                OH_Native_Draw_ClipOp.Intersect.value
            )
        }
    }

    override fun applyTransformMatrix(
        rotationX: Float,
        rotationY: Float,
        rotationZ: Float,
        scaleX: Float,
        scaleY: Float,
        translationX: Float,
        translationY: Float,
        m34Transform: Double
    ) {
        nativeCanvasProxy.applyTransformMatrix(
            rotationX,
            rotationY,
            rotationZ,
            scaleX,
            scaleY,
            translationX,
            translationY,
            m34Transform
        )
    }

    override fun drawParagraphImage(
        image: ImageBitmap,
        width: Int,
        height: Int,
        paragraphHashCode: Int
    ) {
        TraceUtil.traceSync("AdaptiveCanvas:drawParagraphImage") {
            // 将 ImageBitmap 转换为 NativePixelMap
            val pixelMap = image.asNativePixelMap() ?: return

            // 调用 nativeCanvasProxy 绘制文本图像
            nativeCanvasProxy.drawTextPixelMap(
                pixelMap = pixelMap,
                cacheKey = paragraphHashCode,
                width = width,
                height = height
            )
        }
    }

    override fun needRedrawImageWithHashCode(
        paragraphHashCode: Int,
        width: Int,
        height: Int
    ): Boolean {
        return nativeCanvasProxy.needRedrawImageWithHashCode(paragraphHashCode, width, height)
    }

    override fun asyncDrawIntoCanvas(
        globalTask: () -> Long,
        paragraphHashCode: Int,
        width: Int,
        height: Int
    ) {
        TraceUtil.traceSync("AdaptiveCanvas:asyncDrawIntoCanvas") {
            // 执行 globalTask 获取图像指针
            val imagePtr = globalTask()
            if (imagePtr != 0L) {
                // 使用获取到的图像指针绘制
                nativeCanvasProxy.drawTextPixelMapWithPtr(
                    pixelMapPtr = imagePtr,
                    width = width,
                    height = height
                )
            }
        }
    }

    override fun imageFromImageBitmap(paragraphHashCode: Int, imageBitmap: ImageBitmap): Long {
        TraceUtil.traceSync("AdaptiveCanvas:imageFromImageBitmap") {
            // 将 ImageBitmap 转换为 NativePixelMap
            val pixelMap = imageBitmap.asNativePixelMap() ?: return 0L

            // 调用 nativeCanvasProxy 创建并缓存 PixelMap，返回图像指针
            val imagePtr = nativeCanvasProxy.imageFromImageBitmap(
                pixelMap = pixelMap,
                paragraphHashCode = paragraphHashCode
            )
            return imagePtr
        }
    }

    override fun applyRenderEffect(renderEffect: RenderEffect?) {
        // TODO("Not yet implemented")
        LogPrintUtil.verbose { "AdaptiveCanvas::applyRenderEffect" }
    }

    override fun clearClip() {
        TraceUtil.traceSync("AdaptiveCanvas:clearClip") {
            nativeCanvasProxy.clearClip()
        }
    }

    override fun save() {
        nativeCanvasProxy.save()
        LogPrintUtil.verbose { "AdaptiveCanvas::save" }
    }

    override fun restore() {
        nativeCanvasProxy.restore()
        LogPrintUtil.verbose { "AdaptiveCanvas::restore" }
    }

    override fun saveLayer(bounds: Rect, paint: Paint) {
        TraceUtil.traceSync("AdaptiveCanvas:saveLayer") {
            nativePaint.sync(paint)
            nativeCanvasProxy.saveLayer(bounds.left, bounds.top, bounds.right, bounds.bottom, nativePaint)
        }
    }

    override fun translate(dx: Float, dy: Float) {
        nativeCanvasProxy.translate(dx, dy)
    }

    override fun scale(sx: Float, sy: Float) {
        nativeCanvasProxy.scale(sx, sy)
    }

    override fun rotate(degrees: Float) {
        nativeCanvasProxy.rotate(degrees)
    }

    override fun skew(sx: Float, sy: Float) {
        nativeCanvasProxy.skew(sx, sy)
    }

    override fun concat(matrix: Matrix) {
        // Convert Compose Matrix to FloatArray (16 elements)
        val matrixArray = FloatArray(16)
        // Compose Matrix is column-major, same as Transform3D
        // [ m0, m4, m8, m12 ]
        // [ m1, m5, m9, m13 ]
        // [ m2, m6, m10, m14 ]
        // [ m3, m7, m11, m15 ]
        // We can copy directly if the internal storage matches, but let's be safe and copy element by element
        // Matrix[row, col]
        for (i in 0..15) {
            // Matrix stores values in a float array in column-major order
            // values[0] is m11 (row 0, col 0)
            // values[1] is m21 (row 1, col 0)
            // ...
            // values[4] is m12 (row 0, col 1)
            // So we can just copy the values array if it's accessible, or use index
            // Since we don't have direct access to values array in common code easily without knowing implementation details,
            // we use the operator get(row, col) or just assume the order.
            // Actually, Matrix.values is public in some versions, but let's use the values property if available or copy.
            // Looking at Matrix.kt in common:
            // val values: FloatArray
            // It is column major.
            matrixArray[i] = matrix.values[i]
        }
        nativeCanvasProxy.concat(matrixArray)
    }

    override fun clipRect(left: Float, top: Float, right: Float, bottom: Float, clipOp: ClipOp) {
        nativeCanvasProxy.clipRect(left, top, right, bottom, clipOp.asNativeEnum().value)
    }

    override fun clipPath(path: Path, clipOp: ClipOp) {
        TraceUtil.traceSync("AdaptiveCanvas:clipPath") {
            // Try to use NativePathImpl first (similar to iOS implementation)
            path.pathType = PathType.Native
            val currentPath = path.currentPath
            if (currentPath is NativePathImpl && currentPath.handle != null) {
                nativeCanvasProxy.clipPath(currentPath.handle, clipOp.asNativeEnum().value)
            }
        }
    }

    override fun drawLine(p1: Offset, p2: Offset, paint: Paint) {
        nativePaint.sync(paint)
        nativeCanvasProxy.drawLine(p1.x, p1.y, p2.x, p2.y, nativePaint)
    }

    override fun drawRect(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
        TraceUtil.traceSync("AdaptiveCanvas:drawRect") {
            nativePaint.sync(paint)
            nativeCanvasProxy.drawRect(left, top, right, bottom, nativePaint)
        }
    }

    override fun drawRoundRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        radiusX: Float,
        radiusY: Float,
        paint: Paint
    ) {
        TraceUtil.traceSync("AdaptiveCanvas:drawRoundRect") {
            nativePaint.sync(paint)
            nativeCanvasProxy.drawRoundRect(left, top, right, bottom, radiusX, radiusY, nativePaint)
        }
    }

    override fun drawOval(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
        TraceUtil.traceSync("AdaptiveCanvas:drawOval") {
            nativePaint.sync(paint)
            nativeCanvasProxy.drawOval(left, top, right, bottom, nativePaint)
        }
    }

    override fun drawCircle(center: Offset, radius: Float, paint: Paint) {
        TraceUtil.traceSync("AdaptiveCanvas:drawCircle") {
            nativePaint.sync(paint)
            nativeCanvasProxy.drawCircle(center.x, center.y, radius, nativePaint)
        }
    }

    override fun drawArc(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        startAngle: Float,
        sweepAngle: Float,
        useCenter: Boolean,
        paint: Paint
    ) {
        TraceUtil.traceSync("AdaptiveCanvas:drawArc") {
            nativePaint.sync(paint)
            nativeCanvasProxy.drawArc(
                left,
                top,
                right,
                bottom,
                startAngle,
                sweepAngle,
                useCenter,
                nativePaint
            )
        }
    }

    override fun drawPath(path: Path, paint: Paint) {
        TraceUtil.traceSync("AdaptiveCanvas:drawPath") {
            nativePaint.sync(paint)
            // Try to use NativePathImpl first (similar to iOS implementation)
            path.pathType = PathType.Native
            val currentPath = path.currentPath
            if (currentPath is NativePathImpl && currentPath.handle != null) {
                nativeCanvasProxy.drawPath(currentPath.handle, nativePaint)
            } else {
                return
            }
        }
    }

    override fun drawImage(image: ImageBitmap, topLeftOffset: Offset, paint: Paint) {
        TraceUtil.traceSync("AdaptiveCanvas:drawImage") {
            nativePaint.sync(paint)
            // 参考iOS实现：drawImage简化为调用drawImageRect
            // srcRect = (0, 0, image.width, image.height)
            // dstRect = (topLeftOffset.x, topLeftOffset.y, image.width, image.height)
            val pixelMap = image.asNativePixelMap() ?: return
            nativeCanvasProxy.drawImageRect(
                pixelMap = pixelMap,
                srcX = 0,
                srcY = 0,
                srcWidth = image.width,
                srcHeight = image.height,
                dstX = topLeftOffset.x.toInt(),
                dstY = topLeftOffset.y.toInt(),
                dstWidth = image.width,
                dstHeight = image.height,
                nativePaint
            )
        }
    }

    override fun drawImageRect(
        image: ImageBitmap,
        srcOffset: IntOffset,
        srcSize: IntSize,
        dstOffset: IntOffset,
        dstSize: IntSize,
        paint: Paint
    ) {
        TraceUtil.traceSync("AdaptiveCanvas:drawImageRect") {
            nativePaint.sync(paint)
            val pixelMap = image.asNativePixelMap() ?: return
            nativeCanvasProxy.drawImageRect(
                pixelMap = pixelMap,
                srcX = srcOffset.x,
                srcY = srcOffset.y,
                srcWidth = srcSize.width,
                srcHeight = srcSize.height,
                dstX = dstOffset.x,
                dstY = dstOffset.y,
                dstWidth = dstSize.width,
                dstHeight = dstSize.height,
                nativePaint
            )
        }
    }

    override fun drawPoints(pointMode: PointMode, points: List<Offset>, paint: Paint) {
        TraceUtil.traceSync("AdaptiveCanvas:drawPoints") {
            nativePaint.sync(paint)
            // 转换为 FloatArray: [x1, y1, x2, y2, ...]
            val floatArray = FloatArray(points.size * 2) { i ->
                if (i % 2 == 0) points[i / 2].x else points[i / 2].y
            }
            nativeCanvasProxy.drawPoints(
                pointMode = pointMode.asNativePointMode(),
                points = floatArray,
                nativePaint = nativePaint
            )
        }
    }

    override fun drawRawPoints(pointMode: PointMode, points: FloatArray, paint: Paint) {
        TraceUtil.traceSync("AdaptiveCanvas:drawRawPoints") {
            if (points.size % 2 != 0) {
                throw IllegalArgumentException("points must have an even number of values")
            }
            nativePaint.sync(paint)
            nativeCanvasProxy.drawPoints(
                pointMode = pointMode.asNativePointMode(),
                points = points,
                nativePaint = nativePaint
            )
        }
    }

    override fun drawVertices(vertices: Vertices, blendMode: BlendMode, paint: Paint) {
        // TODO("Not yet implemented")
        LogPrintUtil.verbose { "AdaptiveCanvas::drawRawVertices, vertices: $vertices, blendMode: $blendMode, paint: $paint" }
    }

    override fun enableZ() {
        TraceUtil.traceSync("AdaptiveCanvas:enableZ") {
            // TODO: ArkUI RenderNode 目前没有直接的 Z 轴 API
            // 可以使用 SetShadowElevation 来实现部分功能，但完整的 Z 轴支持需要确认 ArkUI API
            nativeCanvasProxy.enableZ()
        }
    }

    override fun disableZ() {
        TraceUtil.traceSync("AdaptiveCanvas:disableZ") {
            // TODO: ArkUI RenderNode 目前没有直接的 Z 轴 API
            nativeCanvasProxy.disableZ()
        }
    }

    fun destroy() {
        TraceUtil.traceSync("AdaptiveCanvas:destroy") {
            // 1. 先移除 canvasNode 从父节点，避免父节点持有已释放节点的引用
            nativeCanvasProxy.removeCanvasNodeFromParent()
        }
    }
}
