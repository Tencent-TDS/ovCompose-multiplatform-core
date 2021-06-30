/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.*
import androidx.compose.ui.util.fastForEach

// import org.jetbrains.skiko.skia.native.RRect as SkiaRRect
import org.jetbrains.skiko.skia.native.SkRect as SkiaNativeRect
import org.jetbrains.skiko.skia.native.*
import org.jetbrains.skiko.skia.native.SkCanvas__SrcRectConstraint.* // TODO: Do something with cpp nesting.


internal actual fun ActualCanvas(image: ImageBitmap): Canvas {
    val skiaBitmap = image.asSkiaBitmap()
    require(!skiaBitmap.isImmutable()) {
        "Cannot draw on immutable ImageBitmap"
    }
    return NativeCanvas(org.jetbrains.skiko.skia.native.Canvas(skiaBitmap))
}


actual val Canvas.nativeCanvas: PlatformCanvas get() = (this as NativeCanvas).skia

actual typealias PlatformCanvas = org.jetbrains.skiko.skia.native.Canvas

class NativeCanvas(val skia: org.jetbrains.skiko.skia.native.Canvas) : Canvas {
    private val Paint.skia
        get() = (this as NativePaint).skia

    override fun save() {
        skia.save()
    }

    override fun restore() {
        skia.restore()
    }

    override fun saveLayer(bounds: Rect, paint: Paint) {
        skia.saveLayer(
            bounds.toSkiaNativeRect(),
            paint.skia
        )
    }

    override fun translate(dx: Float, dy: Float) {
        skia.translate(dx, dy)
    }

    override fun scale(sx: Float, sy: Float) {
        skia.scale(sx, sy)
    }

    override fun rotate(degrees: Float) {
        skia.rotate(degrees)
    }

    override fun skew(sx: Float, sy: Float) {
        skia.skew(sx, sy)
    }

    override fun concat(matrix: Matrix) {
        if (!matrix.isIdentity()) {
            TODO("implement native concat(matrix: Matrix)")
            // skia.concat(matrix.toSkiaNative())
        }
    }

    override fun clipRect(left: Float, top: Float, right: Float, bottom: Float, clipOp: ClipOp) {
        val antiAlias = true
        skia.clipRect(makeLTRB(left, top, right, bottom), clipOp.toSkiaNative(), antiAlias)
    }

    fun clipRoundRect(rect: RoundRect, clipOp: ClipOp = ClipOp.Intersect) {
        val antiAlias = true
        skia.clipRRect(rect.toSkiaNativeRRect(), clipOp.toSkiaNative(), antiAlias)
    }

    override fun clipPath(path: Path, clipOp: ClipOp) {
        val antiAlias = true
        skia.clipPath(path.asSkiaPath(), clipOp.toSkiaNative(), antiAlias)
    }

    override fun drawLine(p1: Offset, p2: Offset, paint: Paint) {
        skia.drawLine(p1.x, p1.y, p2.x, p2.y, paint.skia)
    }

    override fun drawRect(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
        skia.drawRect(makeLTRB(left, top, right, bottom), paint.skia)
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
        skia.drawRRect(
            makeComplexLTRB(
                left,
                top,
                right,
                bottom,
                radiusX,
                radiusY
            ),
            paint.skia
        )
    }

    override fun drawOval(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
        skia.drawOval(makeLTRB(left, top, right, bottom), paint.skia)
    }

    override fun drawCircle(center: Offset, radius: Float, paint: Paint) {
        skia.drawCircle(center.x, center.y, radius, paint.skia)
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
        skia.drawArc(
            makeLTRB(left, top, right, bottom),
            startAngle,
            sweepAngle,
            useCenter,
            paint.skia
        )
    }

    override fun drawPath(path: Path, paint: Paint) {
        skia.drawPath(path.asSkiaPath(), paint.skia)
    }

    override fun drawImage(image: ImageBitmap, topLeftOffset: Offset, paint: Paint) {
        val size = Size(image.width.toFloat(), image.height.toFloat())
        drawImageRect(image, Offset.Zero, size, topLeftOffset, size, paint)
    }

    override fun drawImageRect(
        image: ImageBitmap,
        srcOffset: IntOffset,
        srcSize: IntSize,
        dstOffset: IntOffset,
        dstSize: IntSize,
        paint: Paint
    ) {
        drawImageRect(
            image,
            Offset(srcOffset.x.toFloat(), srcOffset.y.toFloat()),
            Size(srcSize.width.toFloat(), srcSize.height.toFloat()),
            Offset(dstOffset.x.toFloat(), dstOffset.y.toFloat()),
            Size(dstSize.width.toFloat(), dstSize.height.toFloat()),
            paint
        )
    }


    // TODO(demin): probably this method should be in the common Canvas
    private fun drawImageRect(
        image: ImageBitmap,
        srcOffset: Offset,
        srcSize: Size,
        dstOffset: Offset,
        dstSize: Size,
        paint: Paint
    ) {
        val bitmap = image.asSkiaBitmap()
        // TODO: with skija this is .use, not .let .
        Image.MakeFromBitmap(bitmap).let { skiaImage ->
            skia.drawImageRect(
                skiaImage,
                makeXYWH(
                    srcOffset.x,
                    srcOffset.y,
                    srcSize.width,
                    srcSize.height
                ),
                makeXYWH(
                    dstOffset.x,
                    dstOffset.y,
                    dstSize.width,
                    dstSize.height
                ),
                paint.filterQuality.toSkia(),
                paint.skia,
                kFast_SrcRectConstraint
            )
        }
    }


    override fun drawPoints(pointMode: PointMode, points: List<Offset>, paint: Paint) {
        when (pointMode) {
            // Draw a line between each pair of points, each point has at most one line
            // If the number of points is odd, then the last point is ignored.
            PointMode.Lines -> drawLines(points, paint, 2)

            // Connect each adjacent point with a line
            PointMode.Polygon -> drawLines(points, paint, 1)

            // Draw a point at each provided coordinate
            PointMode.Points -> drawPoints(points, paint)
        }
    }

    override fun enableZ() = Unit

    override fun disableZ() = Unit

    private fun drawPoints(points: List<Offset>, paint: Paint) {
        points.fastForEach { point ->
            skia.drawPoint(
                point.x,
                point.y,
                paint.skia
            )
        }
    }

    /**
     * Draw lines connecting points based on the corresponding step.
     *
     * ex. 3 points with a step of 1 would draw 2 lines between the first and second points
     * and another between the second and third
     *
     * ex. 4 points with a step of 2 would draw 2 lines between the first and second and another
     * between the third and fourth. If there is an odd number of points, the last point is
     * ignored
     *
     * @see drawRawLines
     */
    private fun drawLines(points: List<Offset>, paint: Paint, stepBy: Int) {
        if (points.size >= 2) {
            for (i in 0 until points.size - 1 step stepBy) {
                val p1 = points[i]
                val p2 = points[i + 1]
                skia.drawLine(
                    p1.x,
                    p1.y,
                    p2.x,
                    p2.y,
                    paint.skia
                )
            }
        }
    }

    /**
     * @throws IllegalArgumentException if a non even number of points is provided
     */
    override fun drawRawPoints(pointMode: PointMode, points: FloatArray, paint: Paint) {
        if (points.size % 2 != 0) {
            throw IllegalArgumentException("points must have an even number of values")
        }
        when (pointMode) {
            PointMode.Lines -> drawRawLines(points, paint, 2)
            PointMode.Polygon -> drawRawLines(points, paint, 1)
            PointMode.Points -> drawRawPoints(points, paint, 2)
        }
    }

    private fun drawRawPoints(points: FloatArray, paint: Paint, stepBy: Int) {
        if (points.size % 2 == 0) {
            for (i in 0 until points.size - 1 step stepBy) {
                val x = points[i]
                val y = points[i + 1]
                skia.drawPoint(x, y, paint.skia)
            }
        }
    }

    /**
     * Draw lines connecting points based on the corresponding step. The points are interpreted
     * as x, y coordinate pairs in alternating index positions
     *
     * ex. 3 points with a step of 1 would draw 2 lines between the first and second points
     * and another between the second and third
     *
     * ex. 4 points with a step of 2 would draw 2 lines between the first and second and another
     * between the third and fourth. If there is an odd number of points, the last point is
     * ignored
     *
     * @see drawLines
     */
    private fun drawRawLines(points: FloatArray, paint: Paint, stepBy: Int) {
        // Float array is treated as alternative set of x and y coordinates
        // x1, y1, x2, y2, x3, y3, ... etc.
        if (points.size >= 4 && points.size % 2 == 0) {
            for (i in 0 until points.size - 3 step stepBy * 2) {
                val x1 = points[i]
                val y1 = points[i + 1]
                val x2 = points[i + 2]
                val y2 = points[i + 3]
                skia.drawLine(
                    x1,
                    y1,
                    x2,
                    y2,
                    paint.skia
                )
            }
        }
    }

    override fun drawVertices(vertices: Vertices, blendMode: BlendMode, paint: Paint) {
        TODO("implement NativeCanvas.drawVertices")
        /*
        org.jetbrains.skiko.skia.native.Canvas._nDrawVertices(
            skia._ptr,
            vertices.vertexMode.toSkiaNativeMode(),
            vertices.positions,
            vertices.colors,
            vertices.textureCoordinates,
            vertices.indices,
            blendMode.toSkiaNative().ordinal,
            Native.getPtr(paint.asFrameworkPaint())
        )
        */
    }

    private fun VertexMode.toSkiaNativeMode() = when (this) {
        VertexMode.Triangles -> 0
        VertexMode.TriangleStrip -> 1
        VertexMode.TriangleFan -> 2
        else -> 0
    }

    private fun ClipOp.toSkiaNative() = when (this) {
        ClipOp.Difference -> kDifference_
        ClipOp.Intersect -> kIntersect
        else -> kIntersect
    }

    private fun Matrix.toSkiaNative() = M44(
        this[0, 0],
        this[1, 0],
        this[2, 0],
        this[3, 0],

        this[0, 1],
        this[1, 1],
        this[2, 1],
        this[3, 1],

        this[0, 2],
        this[1, 2],
        this[2, 2],
        this[3, 2],

        this[0, 3],
        this[1, 3],
        this[2, 3],
        this[3, 3]
    )

    private fun FilterQuality.toSkia() = null
    /* : SamplingMode = when (this) {
        FilterQuality.Low -> FilterMipmap(FilterMode.LINEAR, MipmapMode.NONE)
        FilterQuality.Medium -> FilterMipmap(FilterMode.LINEAR, MipmapMode.NEAREST)
        FilterQuality.High -> CubicResampler(1 / 3.0f, 1 / 3.0f)
        else -> FilterMipmap(FilterMode.NEAREST, MipmapMode.NONE)
    }
    */
}
