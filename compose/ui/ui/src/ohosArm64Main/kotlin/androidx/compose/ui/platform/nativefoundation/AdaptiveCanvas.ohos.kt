package androidx.compose.ui.platform.nativefoundation

import androidx.compose.common.interop.LogPrintUtil
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
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.Vertices
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.cinterop.COpaquePointer
import kotlin.experimental.ExperimentalObjCRefinement


private inline fun ClipOp.asNativeEnum(): OH_Native_Draw_ClipOp {
    return when (this) {
        ClipOp.Difference -> OH_Native_Draw_ClipOp.OH_NATIVE_CLIPOP_DIFFERENCE
        ClipOp.Intersect -> OH_Native_Draw_ClipOp.OH_NATIVE_CLIPOP_INTERSECT
        else -> throw RuntimeException("暂不支持")
    }
}

private inline fun CornerRadius.greaterThen(rhs: CornerRadius): Boolean {
    return x > rhs.x && y > rhs.y
}

/**
 * 从[canvas]中获取绘制的[UikitImageBitmap]
 */
//fun getImageBitmapFromCanvas(canvas: Canvas?): ImageBitmap? {
//    if (canvas is AdaptiveCanvas) {
//        return UikitImageBitmap(canvas.viewProxy.getSnapshotImage())
//    }
//    return null
//}

//fun getImageBitmapFromCanvas(canvas: Canvas?, width:Int, height: Int): ImageBitmap? {
//    if (canvas is AdaptiveCanvas) {
//        return UikitImageBitmap(canvas.viewProxy.getSnapshotImageWithWidth(width, height))
//    }
//    return null
//}

@OptIn(ExperimentalObjCRefinement::class)
@HiddenFromObjC
internal class AdaptiveCanvas(factory: COpaquePointer) : OHOSNativeCanvas {
    override val canvasType: CanvasType get() = CanvasType.Native

    // 封装的Kotlin对象，可以像普通Kotlin对象一样使用
    val nativeCanvasProxy: OHNativeCanvasProxy
    val nativePaint: OHComposeNativePaint

    init {
        val rawCanvasProxyHandle: OHNativeCanvasProxy_Handle? =
            androidx_compose_ui_arkui_utils_createOHNativeCanvasProxy(factory)
        val rawNativePaintHandle: OHComposeNativePaint_Handle? =
            androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_Paint(rawCanvasProxyHandle)
        nativeCanvasProxy = OHNativeCanvasProxy(rawCanvasProxyHandle)
        nativePaint = OHComposeNativePaint(rawNativePaintHandle)
    }


    override fun onPreDraw() {
        nativeCanvasProxy.beginDraw()
        LogPrintUtil.verbose("AdaptiveCanvas::beginDraw")
    }

    override fun drawLayer(renderNodeHandle: BaseRenderNode_Handle) {
        nativeCanvasProxy.drawLayer(renderNodeHandle)
        LogPrintUtil.verbose("AdaptiveCanvas::drawLayer")
    }

    override fun drawLayerWithNativeCanvas(nativeCanvas: OHOSNativeCanvas) {
        nativeCanvasProxy.drawLayerWithSubproxy((nativeCanvas as AdaptiveCanvas).nativeCanvasProxy)
        LogPrintUtil.verbose("AdaptiveCanvas::drawLayerWithNativeCanvas")
    }

    override fun onPostDraw() {
        nativeCanvasProxy.finishDraw()
        LogPrintUtil.verbose("AdaptiveCanvas::clipRoundRect")
    }

    override fun clipRoundRect(rect: RoundRect) {
        // TODO("Not yet implemented")
        LogPrintUtil.verbose("AdaptiveCanvas::clipRoundRect")
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
        LogPrintUtil.verbose("AdaptiveCanvas::applyTransformMatrix")
    }

    override fun drawParagraphImage(
        image: ImageBitmap,
        width: Int,
        height: Int,
        paragraphHashCode: Int
    ) {
        // TODO("Not yet implemented")
        LogPrintUtil.verbose("AdaptiveCanvas::drawParagraphImage")
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
        // TODO("Not yet implemented")
        LogPrintUtil.verbose("AdaptiveCanvas::asyncDrawIntoCanvas")
    }

    override fun imageFromImageBitmap(paragraphHashCode: Int, imageBitmap: ImageBitmap): Long {
        // TODO("Not yet implemented")
        LogPrintUtil.verbose("AdaptiveCanvas::imageFromImageBitMap")
        return 0L
    }

    override fun applyRenderEffect(renderEffect: RenderEffect?) {
        // TODO("Not yet implemented")
        LogPrintUtil.verbose("AdaptiveCanvas::applyRenderEffect")
    }

    override fun clearClip() {
        // TODO("Not yet implemented")
        LogPrintUtil.verbose("AdaptiveCanvas::clearClip")
    }

    override fun save() {
        nativeCanvasProxy.save()
        LogPrintUtil.verbose("AdaptiveCanvas::save")
    }

    override fun restore() {
        nativeCanvasProxy.restore()
        LogPrintUtil.verbose("AdaptiveCanvas::restore")
    }

    override fun saveLayer(bounds: Rect, paint: Paint) {
        // TODO("Not yet implemented")
        LogPrintUtil.verbose("AdaptiveCanvas::saveLayer")
    }

    override fun translate(dx: Float, dy: Float) {
        nativeCanvasProxy.translate(dx, dy)
        LogPrintUtil.verbose("AdaptiveCanvas::translate, dx: $dx, dy: $dy")
    }

    override fun scale(sx: Float, sy: Float) {
        // TODO("Not yet implemented")
        LogPrintUtil.verbose("AdaptiveCanvas::scale")
    }

    override fun rotate(degrees: Float) {
        // TODO("Not yet implemented")
        LogPrintUtil.verbose("AdaptiveCanvas::rotate")
    }

    override fun skew(sx: Float, sy: Float) {
        // TODO("Not yet implemented")
        LogPrintUtil.verbose("AdaptiveCanvas::skew")
    }

    override fun concat(matrix: Matrix) {
        // TODO("Not yet implemented")
        LogPrintUtil.verbose("AdaptiveCanvas::concat, matrix: $matrix")
    }

    override fun clipRect(left: Float, top: Float, right: Float, bottom: Float, clipOp: ClipOp) {
        nativeCanvasProxy.clipRect(left, top, right, bottom, clipOp.asNativeEnum().value)
        LogPrintUtil.verbose(
            "AdaptiveCanvas::clipRect, " +
                    "left: $left, top: $top, right: $right, bottom: $bottom, clipOp: $clipOp"
        )
    }

    override fun clipPath(path: Path, clipOp: ClipOp) {
        // TODO("Not yet implemented")
        LogPrintUtil.verbose("AdaptiveCanvas::clipPath, path: $path, clipOp: $clipOp")
    }

    override fun drawLine(p1: Offset, p2: Offset, paint: Paint) {
        nativePaint.sync(paint)
        nativeCanvasProxy.drawLine(p1.x, p1.y, p2.x, p2.y, nativePaint)
        LogPrintUtil.verbose("AdaptiveCanvas::drawLine, p1: $p1, p2: $p2, paint: $paint")
    }

    override fun drawRect(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
        nativePaint.sync(paint)
        nativeCanvasProxy.drawRect(left, top, right, bottom, nativePaint)
        LogPrintUtil.verbose(
            "AdaptiveCanvas::drawRect, " +
                    "left: $left, top: $top, right: $right, bottom: $bottom, paint: $paint"
        )
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
        nativePaint.sync(paint)
        nativeCanvasProxy.drawRoundRect(left, top, right, bottom, radiusX, radiusY, nativePaint)
        LogPrintUtil.verbose(
            "AdaptiveCanvas::drawRoundRect, " +
                    "left: $left, top: $top, right: $right, bottom: $bottom, " +
                    "radiusX: $radiusX, radiusY: $radiusY, paint: $paint"
        )
    }

    override fun drawOval(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
        // TODO("Not yet implemented")
        LogPrintUtil.verbose(
            "AdaptiveCanvas::drawRoundRect, " +
                    "left: $left, top: $top, right: $right, bottom: $bottom, paint: $paint"
        )
    }

    override fun drawCircle(center: Offset, radius: Float, paint: Paint) {
        // TODO("Not yet implemented")
        LogPrintUtil.verbose("AdaptiveCanvas::drawCircle, center: $center, radius: $radius, paint: $paint")
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
        // TODO("Not yet implemented")
        LogPrintUtil.verbose(
            "AdaptiveCanvas::drawArc, " +
                    "left: $left, top: $top, right: $right, bottom: $bottom, " +
                    "startAngle: $startAngle, sweepAngle: $sweepAngle, " +
                    "useCenter: $useCenter, paint: $paint"
        )
    }

    override fun drawPath(path: Path, paint: Paint) {
        // TODO("Not yet implemented")
        LogPrintUtil.verbose("AdaptiveCanvas::drawPath, path: $path, paint: $paint")
    }

    override fun drawImage(image: ImageBitmap, topLeftOffset: Offset, paint: Paint) {
        // TODO("Not yet implemented")
        LogPrintUtil.verbose("AdaptiveCanvas::drawImage, image: $image, topLeftOffset: $topLeftOffset, paint: $paint")
    }

    override fun drawImageRect(
        image: ImageBitmap,
        srcOffset: IntOffset,
        srcSize: IntSize,
        dstOffset: IntOffset,
        dstSize: IntSize,
        paint: Paint
    ) {
        // TODO("Not yet implemented")
        LogPrintUtil.verbose(
            "AdaptiveCanvas::drawImageRect, " +
                    "image: $image, srcOffset: $srcOffset, srcSize: $srcSize" +
                    "dstOffset: $dstOffset, dstSize: $dstSize, paint: $paint"
        )
    }

    override fun drawPoints(pointMode: PointMode, points: List<Offset>, paint: Paint) {
        // TODO("Not yet implemented")
        LogPrintUtil.verbose("AdaptiveCanvas::drawPoints, pointMode: $pointMode, points: $points, paint: $paint")

    }

    override fun drawRawPoints(pointMode: PointMode, points: FloatArray, paint: Paint) {
        // TODO("Not yet implemented")
        LogPrintUtil.verbose("AdaptiveCanvas::drawRawPoints, pointMode: $pointMode, points: $points, paint: $paint")
    }

    override fun drawVertices(vertices: Vertices, blendMode: BlendMode, paint: Paint) {
        // TODO("Not yet implemented")
        LogPrintUtil.verbose("AdaptiveCanvas::drawRawVertices, vertices: $vertices, blendMode: $blendMode, paint: $paint")
    }

    override fun enableZ() {
        // TODO("Not yet implemented")
        LogPrintUtil.verbose("AdaptiveCanvas::enableZ")
    }

    override fun disableZ() {
        // TODO("Not yet implemented")
        LogPrintUtil.verbose("AdaptiveCanvas::disableZ")
    }

    fun destroy() {
        // TODO: destroy some resources
        LogPrintUtil.verbose("AdaptiveCanvas::destroy")
    }
}
