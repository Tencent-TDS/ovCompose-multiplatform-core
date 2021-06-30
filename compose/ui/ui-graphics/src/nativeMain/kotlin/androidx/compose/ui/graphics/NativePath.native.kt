/*
 * Copyright 2020 The Android Open Source Project
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
import org.jetbrains.skiko.skia.native.*
import org.jetbrains.skiko.skia.native.SkPathFillType.*
import org.jetbrains.skiko.skia.native.SkPathDirection.*
import org.jetbrains.skiko.skia.native.SkPath__AddPathMode.*
import kotlinx.cinterop.*

actual fun Path(): Path = NativePath()

@Suppress("NOTHING_TO_INLINE")
inline fun Path.asSkiaPath(): org.jetbrains.skiko.skia.native.Path =
    if (this is NativePath) {
        internalPath
    } else {
        error("Unable to obtain org.jetbrains.skija.Path")
    }

class NativePath(
    internalPath: org.jetbrains.skiko.skia.native.Path = org.jetbrains.skiko.skia.native.Path()
) : Path {
    var internalPath = internalPath
        private set

    override var fillType: PathFillType
        get() {
            if (internalPath.getFillType() == kEvenOdd) {
                return PathFillType.EvenOdd
            } else {
                return PathFillType.NonZero
            }
        }

        set(value) {
            internalPath.setFillType(
                if (value == PathFillType.EvenOdd) {
                    kEvenOdd
                } else {
                    kWinding
                }
            )
        }

    override fun moveTo(x: Float, y: Float) {
        internalPath.moveTo(x, y)
    }

    override fun relativeMoveTo(dx: Float, dy: Float) {
        internalPath.rMoveTo(dx, dy)
    }

    override fun lineTo(x: Float, y: Float) {
        internalPath.lineTo(x, y)
    }

    override fun relativeLineTo(dx: Float, dy: Float) {
        internalPath.rLineTo(dx, dy)
    }

    override fun quadraticBezierTo(x1: Float, y1: Float, x2: Float, y2: Float) {
        internalPath.quadTo(x1, y1, x2, y2)
    }

    override fun relativeQuadraticBezierTo(dx1: Float, dy1: Float, dx2: Float, dy2: Float) {
        internalPath.rQuadTo(dx1, dy1, dx2, dy2)
    }

    override fun cubicTo(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
        internalPath.cubicTo(
            x1, y1,
            x2, y2,
            x3, y3
        )
    }

    override fun relativeCubicTo(
        dx1: Float,
        dy1: Float,
        dx2: Float,
        dy2: Float,
        dx3: Float,
        dy3: Float
    ) {
        internalPath.rCubicTo(
            dx1, dy1,
            dx2, dy2,
            dx3, dy3
        )
    }

    override fun arcTo(
        rect: Rect,
        startAngleDegrees: Float,
        sweepAngleDegrees: Float,
        forceMoveTo: Boolean
    ) {
        internalPath.arcTo(
            rect.toSkiaNativeRect(),
            startAngleDegrees,
            sweepAngleDegrees,
            forceMoveTo
        )
    }

    override fun addRect(rect: Rect) {
        internalPath.addRect(rect.toSkiaNativeRect(), kCCW)
    }

    override fun addOval(oval: Rect) {
        internalPath.addOval(oval.toSkiaNativeRect(), kCCW)
    }

    override fun addArcRad(oval: Rect, startAngleRadians: Float, sweepAngleRadians: Float) {
        addArc(oval, degrees(startAngleRadians), degrees(sweepAngleRadians))
    }

    override fun addArc(oval: Rect, startAngleDegrees: Float, sweepAngleDegrees: Float) {
        internalPath.addArc(oval.toSkiaNativeRect(), startAngleDegrees, sweepAngleDegrees)
    }

    override fun addRoundRect(roundRect: RoundRect) {
        internalPath.addRRect(roundRect.toSkiaNativeRRect(), kCCW)
    }

    override fun addPath(path: Path, offset: Offset) {
        internalPath.addPath(path.asSkiaPath(), offset.x, offset.y, kAppend_AddPathMode)
    }

    override fun close() {
        internalPath.close()
    }

    override fun reset() {
        // preserve fillType to match the Android behavior
        // see https://cs.android.com/android/_/android/platform/frameworks/base/+/d0f379c1976c600313f1f4c39f2587a649e3a4fc
        val fillType = this.fillType
        internalPath.reset()
        this.fillType = fillType
    }

    override fun translate(offset: Offset) {
        TODO("figure out Pat.translate()")
        // internalPath.transform(Matrix33.makeTranslate(offset.x, offset.y))
    }

    override fun getBounds(): Rect {
        val bounds = internalPath.getBounds()!!.pointed
        return Rect(
            bounds.fLeft,
            bounds.fTop,
            bounds.fRight,
            bounds.fBottom
        )
    }

    override fun op(
        path1: Path,
        path2: Path,
        operation: PathOperation
    ): Boolean {
        TODO("implement Path.op")
        /*
        val path = org.jetbrains.skija.Path.makeCombining(
            path1.asDesktopPath(),
            path2.asDesktopPath(),
            operation.toSkiaOperation()
        )

        internalPath = path ?: internalPath
        return path != null

         */
    }
/*
    private fun PathOperation.toSkiaOperation() = when (this) {
        PathOperation.difference -> PathOp.DIFFERENCE
        PathOperation.intersect -> PathOp.INTERSECT
        PathOperation.union -> PathOp.UNION
        PathOperation.xor -> PathOp.XOR
        PathOperation.reverseDifference -> PathOp.REVERSE_DIFFERENCE
    }
*/
    override val isConvex: Boolean get() = internalPath.isConvex()

    override val isEmpty: Boolean get() = internalPath.isEmpty()
}
