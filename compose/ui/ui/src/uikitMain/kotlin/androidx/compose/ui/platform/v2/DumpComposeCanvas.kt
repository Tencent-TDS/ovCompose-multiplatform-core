package androidx.compose.ui.platform.v2

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.Vertices
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize


internal class DumpComposeCanvas : Canvas {
    override fun save() {
    }

    override fun restore() {
    }

    override fun saveLayer(bounds: androidx.compose.ui.geometry.Rect, paint: Paint) {
    }

    override fun translate(dx: Float, dy: Float) {
    }

    override fun scale(sx: Float, sy: Float) {
    }

    override fun rotate(degrees: Float) {
    }

    override fun skew(sx: Float, sy: Float) {
    }

    override fun concat(matrix: Matrix) {
    }

    override fun clipRect(left: Float, top: Float, right: Float, bottom: Float, clipOp: ClipOp) {
    }

    override fun clipPath(path: Path, clipOp: ClipOp) {
    }

    override fun drawLine(p1: Offset, p2: Offset, paint: Paint) {
    }

    override fun drawRect(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
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
    }

    override fun drawOval(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {

    }

    override fun drawCircle(center: Offset, radius: Float, paint: Paint) {

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
    }

    override fun drawPath(path: Path, paint: Paint) {
    }

    override fun drawImage(image: ImageBitmap, topLeftOffset: Offset, paint: Paint) {
    }

    override fun drawImageRect(
        image: ImageBitmap,
        srcOffset: IntOffset,
        srcSize: IntSize,
        dstOffset: IntOffset,
        dstSize: IntSize,
        paint: Paint
    ) {
    }

    override fun drawPoints(pointMode: PointMode, points: List<Offset>, paint: Paint) {
    }

    override fun drawRawPoints(pointMode: PointMode, points: FloatArray, paint: Paint) {
    }

    override fun drawVertices(vertices: Vertices, blendMode: BlendMode, paint: Paint) {
    }

    override fun enableZ() {
    }

    override fun disableZ() {

    }
}

