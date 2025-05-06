package androidx.compose.ui.platform.v2.nativefoundation

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.UikitImageBitmap
import androidx.compose.ui.graphics.Vertices
import androidx.compose.ui.graphics.asCGImage
import androidx.compose.ui.graphics.skBitmapPtr
import androidx.compose.ui.uikit.utils.ITMMCanvasViewProxyProtocol
import androidx.compose.ui.uikit.utils.TMMCanvasViewProxyV2
import androidx.compose.ui.uikit.utils.TMMComposeNativePaint
import androidx.compose.ui.uikit.utils.TMMNativeComposeHasTextImageCache
import androidx.compose.ui.uikit.utils.TMMNativeDrawClipOp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import platform.QuartzCore.CALayer
import kotlin.experimental.ExperimentalObjCRefinement

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

private inline fun ClipOp.asNativeEnum(): TMMNativeDrawClipOp {
    return when (this) {
        ClipOp.Difference -> TMMNativeDrawClipOp.TMMNativeDrawClipOpDifference
        ClipOp.Intersect -> TMMNativeDrawClipOp.TMMNativeDrawClipOpIntersect
        else -> throw RuntimeException("暂不支持")
    }
}

private inline fun CornerRadius.greaterThen(rhs: CornerRadius): Boolean {
    return x > rhs.x && y > rhs.y
}

/**
 * 从[canvas]中获取绘制的[UikitImageBitmap]
 */
fun getImageBitmapFromCanvas(canvas: Canvas?): ImageBitmap? {
    if (canvas is AdaptiveCanvas) {
        return UikitImageBitmap(canvas.viewProxy.getSnapshotImage())
    }
    return null
}

fun getImageBitmapFromCanvas(canvas: Canvas?, width:Int, height: Int): ImageBitmap? {
    if (canvas is AdaptiveCanvas) {
        return UikitImageBitmap(canvas.viewProxy.getSnapshotImageWithWidth(width, height))
    }
    return null
}

@OptIn(ExperimentalObjCRefinement::class)
@HiddenFromObjC
internal class AdaptiveCanvas(private val nativeReusePool: Long = 0) : IOSNativeCanvas {

    val viewProxy: ITMMCanvasViewProxyProtocol = TMMCanvasViewProxyV2()

    private val nativePaint = viewProxy.paint()

    override fun save() {
        viewProxy.save()
    }

    override fun restore() {
        viewProxy.restore()
    }

    override fun clearClip() {
        viewProxy.clearClip()
    }

    override fun saveLayer(bounds: Rect, paint: Paint) = Unit

    override fun translate(dx: Float, dy: Float) {
        viewProxy.translate(dx, dy)
    }

    override fun scale(sx: Float, sy: Float) {
        viewProxy.scale(sx, sy)
    }

    override fun rotate(degrees: Float) {
        viewProxy.rotate(degrees)
    }

    override fun skew(sx: Float, sy: Float) {
        viewProxy.skew(sx = sx, sy = sy)
    }

    override fun concat(matrix: Matrix) {
    }

    override fun clipRect(left: Float, top: Float, right: Float, bottom: Float, clipOp: ClipOp) {
        viewProxy.clipRect(
            left = left,
            top = top,
            right = right,
            bottom = bottom,
            clipOp = clipOp.asNativeEnum()
        )
    }

    override fun onPreDraw() {
        viewProxy.beginDraw()
    }

    override fun drawLayer(layer: CALayer) {
        viewProxy.drawLayer(layer)
    }

    override fun drawLayerWithNativeCanvas(nativeCanvas: IOSNativeCanvas) {
        viewProxy.drawLayerWithSubproxy((nativeCanvas as AdaptiveCanvas).viewProxy)
    }

    override fun onPostDraw() {
        viewProxy.finishDraw()
    }

    override fun clipRoundRect(rect: RoundRect) {
        if (rect.topLeftCornerRadius.greaterThen(CornerRadius.Zero) ||
            rect.topRightCornerRadius.greaterThen(CornerRadius.Zero) ||
            rect.bottomLeftCornerRadius.greaterThen(CornerRadius.Zero) ||
            rect.bottomRightCornerRadius.greaterThen(CornerRadius.Zero)
        ) {
            viewProxy.clipRoundRect(
                left = rect.left,
                top = rect.top,
                right = rect.right,
                bottom = rect.bottom,
                topLeftCornerRadiusX = rect.topLeftCornerRadius.x,
                topLeftCornerRadiusY = rect.topLeftCornerRadius.y,
                topRightCornerRadiusX = rect.topRightCornerRadius.x,
                topRightCornerRadiusY = rect.topRightCornerRadius.y,
                bottomLeftCornerRadiusX = rect.bottomLeftCornerRadius.x,
                bottomLeftCornerRadiusY = rect.bottomLeftCornerRadius.y,
                bottomRightCornerRadiusX = rect.bottomRightCornerRadius.x,
                bottomRightCornerRadiusY = rect.bottomRightCornerRadius.y,
            )
        }
    }

    override fun clipPath(path: Path, clipOp: ClipOp) {
        val nativePath = path as? NativePathImpl ?: return
        viewProxy.clipPath(
            path = nativePath.nativeRef,
            clipOp = clipOp.asNativeEnum()
        )
    }

    override fun drawLine(p1: Offset, p2: Offset, paint: Paint) {
        nativePaint.sync(paint)
        viewProxy.drawLine(
            pointX1 = p1.x,
            pointY1 = p1.y,
            pointX2 = p2.x,
            pointY2 = p2.y,
            paint = nativePaint
        )
    }

    override fun drawRect(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
        nativePaint.sync(paint)
        viewProxy.drawRect(
            left = left,
            top = top,
            right = right,
            bottom = bottom,
            paint = nativePaint
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
        viewProxy.drawRoundRect(
            left = left,
            top = top,
            right = right,
            bottom = bottom,
            radiusX = radiusX,
            radiusY = radiusY,
            paint = nativePaint
        )
    }

    override fun drawOval(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
    }

    override fun drawCircle(center: Offset, radius: Float, paint: Paint) {
        nativePaint.sync(paint)
        viewProxy.drawCircle(center.x, center.y, radius, nativePaint)
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
        nativePaint.sync(paint)
        viewProxy.drawArc(
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

    override fun drawPath(path: Path, paint: Paint) {
        nativePaint.sync(paint)
        val nativePath = path as? NativePathImpl ?: return
        viewProxy.drawPath(nativePath.nativeRef, nativePaint)
    }

    override fun drawImage(image: ImageBitmap, topLeftOffset: Offset, paint: Paint) {
        nativePaint.sync(paint)
        drawImageRectImpl(
            image = image,
            srcOffsetX = 0f,
            srcOffsetY = 0f,
            srcSizeWidth = image.width,
            srcSizeHeight = image.height,
            dstOffsetX = topLeftOffset.x,
            dstOffsetY = topLeftOffset.y,
            dstSizeWidth = image.width,
            dstSizeHeight = image.height,
            nativePaint
        )
    }

    override fun drawParagraphImage(
        image: ImageBitmap,
        width: Int,
        height: Int,
        paragraphHashCode: Int
    ) {
        viewProxy.drawTextSkBitmap(
            skBitmap = image.skBitmapPtr(),
            cacheKey = paragraphHashCode,
            width = width,
            height = height
        )
    }

    override fun needRedrawImageWithHashCode(
        paragraphHashCode: Int,
        width: Int,
        height: Int
    ): Boolean {
        val imagePtr = TMMNativeComposeHasTextImageCache(paragraphHashCode)
        if (imagePtr != 0L) {
            viewProxy.drawTextSkBitmapWithUIImagePtr(
                imagePtr = imagePtr,
                width = width,
                height = height
            )
            return false
        }
        return true
    }

    override fun asyncDrawIntoCanvas(
        globalTask: () -> Long,
        paragraphHashCode: Int,
        width: Int,
        height: Int
    ) {
        viewProxy.asyncDrawIntoCanvas(globalTask, paragraphHashCode, width, height)
    }

    override fun imageFromImageBitmap(paragraphHashCode: Int, imageBitmap: ImageBitmap): Long {
        return viewProxy.imageFromImageBitmap(imageBitmap.skBitmapPtr(), paragraphHashCode)
    }

    override fun applyRenderEffect(renderEffect: RenderEffect?) {
        val blurEffect = renderEffect as? BlurEffect
        viewProxy.blur(
            radiusX = blurEffect?.radiusX ?: 0f,
            radiusY = blurEffect?.radiusY ?: 0f
        )
    }

    override fun drawImageRect(
        image: ImageBitmap,
        srcOffset: IntOffset,
        srcSize: IntSize,
        dstOffset: IntOffset,
        dstSize: IntSize,
        paint: Paint
    ) {
        nativePaint.sync(paint)
        drawImageRectImpl(
            image = image,
            srcOffsetX = srcOffset.x.toFloat(),
            srcOffsetY = srcOffset.y.toFloat(),
            srcSizeWidth = srcSize.width,
            srcSizeHeight = srcSize.height,
            dstOffsetX = dstOffset.x.toFloat(),
            dstOffsetY = dstOffset.y.toFloat(),
            dstSizeWidth = dstSize.width,
            dstSizeHeight = dstSize.height,
            nativePaint
        )
    }

    private fun drawImageRectImpl(
        image: ImageBitmap,
        srcOffsetX: Float,
        srcOffsetY: Float,
        srcSizeWidth: Int,
        srcSizeHeight: Int,
        dstOffsetX: Float,
        dstOffsetY: Float,
        dstSizeWidth: Int,
        dstSizeHeight: Int,
        paint: TMMComposeNativePaint
    ) {
        val imageRef = image.asCGImage()
        if (imageRef == null) {
            return
        }
        viewProxy.drawImageRect(
            imagePointer = imageRef,
            srcOffsetX = srcOffsetX,
            srcOffsetY = srcOffsetY,
            srcSizeWidth = srcSizeWidth,
            srcSizeHeight = srcSizeHeight,
            dstOffsetX = dstOffsetX,
            dstOffsetY = dstOffsetY,
            dstSizeWidth = dstSizeWidth,
            dstSizeHeight = dstSizeHeight,
            paint
        )
    }

    override fun drawPoints(pointMode: PointMode, points: List<Offset>, paint: Paint) {
        nativePaint.sync(paint)
        when (pointMode) {
            // Draw a line between each pair of points, each point has at most one line
            // If the number of points is odd, then the last point is ignored.
            PointMode.Lines -> drawLinesImpl(points, nativePaint, 2)

            // Connect each adjacent point with a line
            PointMode.Polygon -> drawLinesImpl(points, nativePaint, 1)

            // Draw a point at each provided coordinate
            PointMode.Points -> drawPointsImpl(points, nativePaint)
        }
    }

    override fun drawRawPoints(pointMode: PointMode, points: FloatArray, paint: Paint) {
        nativePaint.sync(paint)
        if (points.size % 2 != 0) {
            throw IllegalArgumentException("points must have an even number of values")
        }
        when (pointMode) {
            PointMode.Lines -> drawRawLinesImpl(points, nativePaint, 2)
            PointMode.Polygon -> drawRawLinesImpl(points, nativePaint, 1)
            PointMode.Points -> drawRawPointsImpl(points, nativePaint, 2)
        }
    }

    private fun drawLinesImpl(points: List<Offset>, paint: TMMComposeNativePaint, stepBy: Int) {
        if (points.size >= 2) {
            for (i in 0 until points.size - 1 step stepBy) {
                val p1 = points[i]
                val p2 = points[i + 1]

                viewProxy.drawLine(
                    pointX1 = p1.x,
                    pointY1 = p1.y,
                    pointX2 = p2.x,
                    pointY2 = p2.y,
                    paint = paint
                )
            }
        }
    }

    private fun drawRawLinesImpl(points: FloatArray, paint: TMMComposeNativePaint, stepBy: Int) {
        // Float array is treated as alternative set of x and y coordinates
        // x1, y1, x2, y2, x3, y3, ... etc.
        if (points.size >= 4 && points.size % 2 == 0) {
            for (i in 0 until points.size - 3 step stepBy * 2) {
                val x1 = points[i]
                val y1 = points[i + 1]
                val x2 = points[i + 2]
                val y2 = points[i + 3]
                viewProxy.drawLine(
                    pointX1 = x1,
                    pointY1 = y1,
                    pointX2 = x2,
                    pointY2 = y2,
                    paint = paint
                )
            }
        }
    }

    private fun drawPointsImpl(pointsOffset: List<Offset>, paint: TMMComposeNativePaint) {
        val floatList: List<Float> = pointsOffset.flatMap {
            listOf(it.x, it.y)
        }

        viewProxy.drawRawPoints(
            points = floatList,
            paint = paint
        )
    }

    private fun drawRawPointsImpl(points: FloatArray, paint: TMMComposeNativePaint, stepBy: Int) {
        if (points.size % 2 == 0) {
            viewProxy.drawRawPoints(
                points = points.toList(),
                paint = paint
            )
        }
    }

    override fun drawVertices(vertices: Vertices, blendMode: BlendMode, paint: Paint) {
    }

    override fun enableZ() {
        viewProxy.enableZ()
    }

    override fun disableZ() {
        viewProxy.disableZ()
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
        viewProxy.applyTransformMatrix(
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

    fun destroy() {
        viewProxy.removeFromSuperView()
//        if (ComposeTabService.viewProxyReuseEnable) {
//            TMMCanvasViewProxyEnqueToReusePool(viewProxy, nativeReusePool)
//        }
    }
}