package androidx.compose.ui.platform.v2.nativefoundation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.PathType
import androidx.compose.ui.uikit.utils.TMMComposeCoreNativeCreateDefaultRoundRect
import androidx.compose.ui.uikit.utils.TMMComposeCoreNativeCreateNativePath
import androidx.compose.ui.uikit.utils.TMMNativeDrawPathFillType
import androidx.compose.ui.uikit.utils.TMMNativeDrawPathOperation
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents

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

private inline fun PathOperation.asNativeEnum(): TMMNativeDrawPathOperation {
    return when (this) {
        PathOperation.Difference -> TMMNativeDrawPathOperation.TMMNativeDrawPathOperationDifference
        PathOperation.Intersect -> TMMNativeDrawPathOperation.TMMNativeDrawPathOperationIntersect
        PathOperation.Union -> TMMNativeDrawPathOperation.TMMNativeDrawPathOperationUnion
        PathOperation.Xor -> TMMNativeDrawPathOperation.TMMNativeDrawPathOperationXor
        PathOperation.ReverseDifference -> TMMNativeDrawPathOperation.TMMNativeDrawPathOperationReverseDifference
        else -> throw RuntimeException("暂不支持")
    }
}

private inline fun PathFillType.asNativeEnum(): TMMNativeDrawPathFillType {
    return when (this) {
        PathFillType.NonZero -> TMMNativeDrawPathFillType.TMMNativeDrawPathFillTypeNonZero
        PathFillType.EvenOdd -> TMMNativeDrawPathFillType.TMMNativeDrawPathFillTypeEvenOdd
        else -> throw RuntimeException("暂不支持")
    }
}

private inline fun TMMNativeDrawPathFillType.asPathFillType(): PathFillType {
    return when (this) {
        TMMNativeDrawPathFillType.TMMNativeDrawPathFillTypeEvenOdd ->  PathFillType.EvenOdd
        TMMNativeDrawPathFillType.TMMNativeDrawPathFillTypeNonZero -> PathFillType.NonZero
        else -> throw RuntimeException("暂不支持")
    }
}

@OptIn(ExperimentalForeignApi::class)
internal class NativePathImpl : Path {
    internal val nativeRef = TMMComposeCoreNativeCreateNativePath()

    override var pathType = PathType.Native
    override val currentPath: Path
        get() = this

    override var fillType: PathFillType
        get() = nativeRef.pathFillType.asPathFillType()
        set(value) {
            nativeRef.pathFillType = value.asNativeEnum()
        }

    override val isConvex: Boolean
        get() = nativeRef.isConvex

    override val isEmpty: Boolean
        get() = nativeRef.isEmpty

    override fun moveTo(x: Float, y: Float) {
        nativeRef.moveTo(x, y)
    }

    override fun relativeMoveTo(dx: Float, dy: Float) {
        nativeRef.relativeMoveTo(dx, dy)
    }

    override fun lineTo(x: Float, y: Float) {
        nativeRef.lineTo(x, y)
    }

    override fun relativeLineTo(dx: Float, dy: Float) {
        nativeRef.relativeLineTo(dx, dy)
    }

    override fun quadraticBezierTo(x1: Float, y1: Float, x2: Float, y2: Float) {
        nativeRef.quadraticBezierTo(x1, y1, x2, y2)
    }

    override fun relativeQuadraticBezierTo(dx1: Float, dy1: Float, dx2: Float, dy2: Float) {
        nativeRef.relativeQuadraticBezierTo(dx1, dy1, dx2, dy2)
    }

    override fun cubicTo(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
        nativeRef.cubicTo(x1, y1, x2, y2, x3, y3)
    }

    override fun relativeCubicTo(
        dx1: Float,
        dy1: Float,
        dx2: Float,
        dy2: Float,
        dx3: Float,
        dy3: Float
    ) {
        nativeRef.relativeCubicTo(dx1, dy1, dx2, dy2, dx3, dy3)
    }

    override fun arcTo(
        rect: androidx.compose.ui.geometry.Rect,
        startAngleDegrees: Float,
        sweepAngleDegrees: Float,
        forceMoveTo: Boolean
    ) {
        nativeRef.arcTo(
            left = rect.left,
            top = rect.top,
            right = rect.right,
            bottom = rect.bottom,
            startAngleDegrees = startAngleDegrees,
            sweepAngleDegrees = sweepAngleDegrees,
            forceMoveTo = forceMoveTo
        )
    }

    override fun addRect(rect: androidx.compose.ui.geometry.Rect) {
        nativeRef.addRect(
            left = rect.left,
            top = rect.top,
            right = rect.right,
            bottom = rect.bottom
        )
    }

    override fun addOval(oval: androidx.compose.ui.geometry.Rect) {
        nativeRef.addOval(
            left = oval.left,
            top = oval.top,
            right = oval.right,
            bottom = oval.bottom
        )
    }

    override fun addArcRad(
        oval: androidx.compose.ui.geometry.Rect,
        startAngleRadians: Float,
        sweepAngleRadians: Float
    ) {
        nativeRef.addArcRad(
            left = oval.left,
            top = oval.top,
            right = oval.right,
            bottom = oval.bottom,
            startAngleRadians = startAngleRadians,
            sweepAngleRadians = sweepAngleRadians
        )
    }

    override fun addArc(
        oval: androidx.compose.ui.geometry.Rect,
        startAngleDegrees: Float,
        sweepAngleDegrees: Float
    ) {
        nativeRef.addArc(
            left = oval.left,
            top = oval.top,
            right = oval.right,
            bottom = oval.bottom,
            startAngleDegrees = startAngleDegrees,
            sweepAngleDegrees = sweepAngleDegrees
        )
    }

    override fun addRoundRect(roundRect: RoundRect) {
        val nativeRoundRectRef = TMMComposeCoreNativeCreateDefaultRoundRect()
        with(nativeRoundRectRef) {
            left = roundRect.left
            top = roundRect.top
            right = roundRect.right
            bottom = roundRect.bottom

            topLeftCornerRadiusX = roundRect.topLeftCornerRadius.x
            topLeftCornerRadiusY = roundRect.topLeftCornerRadius.y

            topRightCornerRadiusX = roundRect.topRightCornerRadius.x
            topRightCornerRadiusY = roundRect.topRightCornerRadius.y

            bottomRightCornerRadiusX = roundRect.bottomRightCornerRadius.x
            bottomRightCornerRadiusY = roundRect.bottomRightCornerRadius.y

            bottomLeftCornerRadiusX = roundRect.bottomLeftCornerRadius.x
            bottomLeftCornerRadiusY = roundRect.bottomLeftCornerRadius.y
        }

        nativeRef.addRoundRect(nativeRoundRectRef)
    }

    override fun addPath(path: Path, offset: Offset) {
        path.pathType = PathType.Native
        val realPath = path.currentPath
        if (realPath !is NativePathImpl) return
        nativeRef.addPath(
            path = realPath.nativeRef,
            offsetX = offset.x,
            offsetY = offset.y,
        )
    }

    override fun translate(offset: Offset) {
        nativeRef.translate(
            offsetX = offset.x,
            offsetY = offset.y
        )
    }

    override fun getBounds(): androidx.compose.ui.geometry.Rect {
        return nativeRef.getBounds().useContents {
            androidx.compose.ui.geometry.Rect(
                left = origin.x.toFloat(),
                top = origin.y.toFloat(),
                right = origin.x.toFloat(),
                bottom = origin.x.toFloat()
            )
        }
    }

    override fun op(
        path1: Path,
        path2: Path,
        operation: PathOperation
    ): Boolean {
        path1.pathType = PathType.Native
        path2.pathType = PathType.Native
        val realPath1 = path1.currentPath
        val realPath2 = path2.currentPath
        if (realPath1 !is NativePathImpl || realPath2 !is NativePathImpl) return false
        return nativeRef.op(
            path1 = realPath1.nativeRef,
            path2 = realPath2.nativeRef,
            pathOperation = operation.asNativeEnum()
        )
    }

    override fun close() {
        nativeRef.close()
    }

    override fun reset() {
        nativeRef.reset()
    }

    override fun rewind() {
        reset()
    }
}