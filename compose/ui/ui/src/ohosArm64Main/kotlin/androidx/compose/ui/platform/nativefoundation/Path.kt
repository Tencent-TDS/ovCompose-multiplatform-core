package androidx.compose.ui.platform.nativefoundation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.PathType
import androidx.compose.common.interop.LogPrintUtil
import androidx.compose.ui.arkui.utils.OHPath_addArc
import androidx.compose.ui.arkui.utils.OHPath_addOval
import androidx.compose.ui.arkui.utils.OHPath_addPath
import androidx.compose.ui.arkui.utils.OHPath_addRect
import androidx.compose.ui.arkui.utils.OHPath_arcTo
import androidx.compose.ui.arkui.utils.OHPath_close
import androidx.compose.ui.arkui.utils.OHPath_create
import androidx.compose.ui.arkui.utils.OHPath_cubicTo
import androidx.compose.ui.arkui.utils.OHPath_destroy
import androidx.compose.ui.arkui.utils.OHPath_getBounds
import androidx.compose.ui.arkui.utils.OHPath_getFillType
import androidx.compose.ui.arkui.utils.OHPath_isConvex
import androidx.compose.ui.arkui.utils.OHPath_isEmpty
import androidx.compose.ui.arkui.utils.OHPath_lineTo
import androidx.compose.ui.arkui.utils.OHPath_moveTo
import androidx.compose.ui.arkui.utils.OHPath_quadTo
import androidx.compose.ui.arkui.utils.OHPath_rCubicTo
import androidx.compose.ui.arkui.utils.OHPath_rLineTo
import androidx.compose.ui.arkui.utils.OHPath_rMoveTo
import androidx.compose.ui.arkui.utils.OHPath_rQuadTo
import androidx.compose.ui.arkui.utils.OHPath_reset
import androidx.compose.ui.arkui.utils.OHPath_setFillType
import androidx.compose.ui.arkui.utils.OHPath_translate
import androidx.compose.ui.arkui.utils.OH_Drawing_Path_Handle
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.FloatVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlin.math.PI

// PathFillType conversion
private inline fun PathFillType.asOHDrawingPathFillType(): UInt {
    return when (this) {
        PathFillType.NonZero -> 0u // PATH_FILL_TYPE_WINDING
        PathFillType.EvenOdd -> 1u // PATH_FILL_TYPE_EVEN_ODD
        else -> 0u
    }
}

private inline fun UInt.asPathFillType(): PathFillType {
    return when (this) {
        0u -> PathFillType.NonZero // PATH_FILL_TYPE_WINDING
        1u -> PathFillType.EvenOdd // PATH_FILL_TYPE_EVEN_ODD
        else -> PathFillType.NonZero
    }
}

// PathOperation conversion (for op method)
private inline fun PathOperation.asOHDrawingPathOpMode(): UInt {
    return when (this) {
        PathOperation.Difference -> 0u // PATH_OP_MODE_DIFFERENCE
        PathOperation.Intersect -> 1u // PATH_OP_MODE_INTERSECT
        PathOperation.Union -> 2u // PATH_OP_MODE_UNION
        PathOperation.Xor -> 3u // PATH_OP_MODE_XOR
        PathOperation.ReverseDifference -> 4u // PATH_OP_MODE_REVERSE_DIFFERENCE
        else -> throw RuntimeException("Unsupported PathOperation: $this")
    }
}

@OptIn(ExperimentalForeignApi::class)
internal class NativePathImpl : Path {
    internal val nativeRef: OH_Drawing_Path_Handle? = OHPath_create()

    /**
     * 使用组合方式管理资源生命周期
     * 由于NativePathImpl需要实现Path接口，且nativeRef在类内部创建，
     * 所以使用组合而非继承的方式使用NativeResourceHolder
     */
    private class PathResourceHolder(handle: OH_Drawing_Path_Handle?)
        : NativeResourceHolder<OH_Drawing_Path_Handle>(handle, ::OHPath_destroy)

    private val resourceHolder = PathResourceHolder(nativeRef)

    init {
        if (nativeRef == null) {
            LogPrintUtil.verbose { "NativePathImpl: Failed to create OH_Drawing_Path" }
        }
    }

    /**
     * 显式释放方法：在明确生命周期结束且当前线程为主线程时调用
     */
    fun dispose() {
        resourceHolder.dispose()
    }

    override var pathType = PathType.Native
    override val currentPath: Path
        get() = this

    override var fillType: PathFillType
        get() {
            if (nativeRef == null) return PathFillType.NonZero
            val fillTypeValue = OHPath_getFillType(nativeRef)
            return fillTypeValue.asPathFillType()
        }
        set(value) {
            if (nativeRef == null) return
            OHPath_setFillType(
                nativeRef,
                value.asOHDrawingPathFillType()
            )
        }

    override val isConvex: Boolean
        get() {
            if (nativeRef == null) return false
            return OHPath_isConvex(nativeRef)
        }

    override val isEmpty: Boolean
        get() {
            if (nativeRef == null) return true
            return OHPath_isEmpty(nativeRef)
        }

    override fun moveTo(x: Float, y: Float) {
        if (nativeRef == null) return
        OHPath_moveTo(nativeRef, x, y)
    }

    override fun relativeMoveTo(dx: Float, dy: Float) {
        if (nativeRef == null) return
        OHPath_rMoveTo(nativeRef, dx, dy)
    }

    override fun lineTo(x: Float, y: Float) {
        if (nativeRef == null) return
        OHPath_lineTo(nativeRef, x, y)
    }

    override fun relativeLineTo(dx: Float, dy: Float) {
        if (nativeRef == null) return
        OHPath_rLineTo(nativeRef, dx, dy)
    }

    override fun quadraticBezierTo(x1: Float, y1: Float, x2: Float, y2: Float) {
        if (nativeRef == null) return
        OHPath_quadTo(nativeRef, x1, y1, x2, y2)
    }

    override fun relativeQuadraticBezierTo(dx1: Float, dy1: Float, dx2: Float, dy2: Float) {
        if (nativeRef == null) return
        OHPath_rQuadTo(nativeRef, dx1, dy1, dx2, dy2)
    }

    override fun cubicTo(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
        if (nativeRef == null) return
        OHPath_cubicTo(nativeRef, x1, y1, x2, y2, x3, y3)
    }

    override fun relativeCubicTo(
        dx1: Float,
        dy1: Float,
        dx2: Float,
        dy2: Float,
        dx3: Float,
        dy3: Float
    ) {
        if (nativeRef == null) return
        OHPath_rCubicTo(
            nativeRef,
            dx1,
            dy1,
            dx2,
            dy2,
            dx3,
            dy3
        )
    }

    override fun arcTo(
        rect: Rect,
        startAngleDegrees: Float,
        sweepAngleDegrees: Float,
        forceMoveTo: Boolean
    ) {
        if (nativeRef == null) return
        // Note: OHOS arcTo doesn't support forceMoveTo directly
        // We need to handle it by checking if we need to moveTo first
        if (forceMoveTo) {
            // Calculate start point of arc and moveTo it
            // For simplicity, we'll just call arcTo and let OHOS handle it
            // This may not be 100% accurate but should work for most cases
        }
        OHPath_arcTo(
            nativeRef,
            rect.left,
            rect.top,
            rect.right,
            rect.bottom,
            startAngleDegrees,
            sweepAngleDegrees
        )
    }

    override fun addRect(rect: Rect) {
        if (nativeRef == null) return
        OHPath_addRect(
            nativeRef,
            rect.left,
            rect.top,
            rect.right,
            rect.bottom
        )
    }

    override fun addOval(oval: Rect) {
        if (nativeRef == null) return
        OHPath_addOval(
            nativeRef,
            oval.left,
            oval.top,
            oval.right,
            oval.bottom
        )
    }

    override fun addArcRad(
        oval: Rect,
        startAngleRadians: Float,
        sweepAngleRadians: Float
    ) {
        if (nativeRef == null) return
        val startAngleDegrees = (startAngleRadians * 180.0 / PI).toFloat()
        val sweepAngleDegrees = (sweepAngleRadians * 180.0 / PI).toFloat()
        OHPath_addArc(
            nativeRef,
            oval.left,
            oval.top,
            oval.right,
            oval.bottom,
            startAngleDegrees,
            sweepAngleDegrees
        )
    }

    override fun addArc(
        oval: Rect,
        startAngleDegrees: Float,
        sweepAngleDegrees: Float
    ) {
        if (nativeRef == null) return
        OHPath_addArc(
            nativeRef,
            oval.left,
            oval.top,
            oval.right,
            oval.bottom,
            startAngleDegrees,
            sweepAngleDegrees
        )
    }

    override fun addRoundRect(roundRect: RoundRect) {
        if (nativeRef == null) return
        // Note: OHOS doesn't have direct RoundRect support
        // We need to build the path manually using arcs and lines
        // For now, we'll approximate by creating a path with rounded corners
        // This is a simplified implementation - a full implementation would
        // need to handle all corner radius combinations
        val left = roundRect.left
        val top = roundRect.top
        val right = roundRect.right
        val bottom = roundRect.bottom

        // Use the minimum corner radius for all corners as a simple approximation
        val radius = minOf(
            roundRect.topLeftCornerRadius.x,
            roundRect.topLeftCornerRadius.y,
            roundRect.topRightCornerRadius.x,
            roundRect.topRightCornerRadius.y,
            roundRect.bottomLeftCornerRadius.x,
            roundRect.bottomLeftCornerRadius.y,
            roundRect.bottomRightCornerRadius.x,
            roundRect.bottomRightCornerRadius.y
        )

        // Create rounded rectangle path manually
        moveTo(left + radius, top)
        lineTo(right - radius, top)
        // Top-right corner arc
        arcTo(
            Rect(right - 2 * radius, top, right, top + 2 * radius),
            270f,
            90f,
            false
        )
        lineTo(right, bottom - radius)
        // Bottom-right corner arc
        arcTo(
            Rect(right - 2 * radius, bottom - 2 * radius, right, bottom),
            0f,
            90f,
            false
        )
        lineTo(left + radius, bottom)
        // Bottom-left corner arc
        arcTo(
            Rect(left, bottom - 2 * radius, left + 2 * radius, bottom),
            90f,
            90f,
            false
        )
        lineTo(left, top + radius)
        // Top-left corner arc
        arcTo(
            Rect(left, top, left + 2 * radius, top + 2 * radius),
            180f,
            90f,
            false
        )
        close()
    }

    override fun addPath(path: Path, offset: Offset) {
        if (nativeRef == null) return
        path.pathType = PathType.Native
        val realPath = path.currentPath
        if (realPath !is NativePathImpl || realPath.nativeRef == null) return
        OHPath_addPath(
            nativeRef,
            realPath.nativeRef,
            offset.x,
            offset.y
        )
    }

    override fun translate(offset: Offset) {
        if (nativeRef == null) return
        OHPath_translate(nativeRef, offset.x, offset.y)
    }

    override fun getBounds(): Rect {
        if (nativeRef == null) return Rect.Zero
        return memScoped {
            val leftPtr = alloc<FloatVar>()
            val topPtr = alloc<FloatVar>()
            val rightPtr = alloc<FloatVar>()
            val bottomPtr = alloc<FloatVar>()
            OHPath_getBounds(
                nativeRef,
                leftPtr.ptr,
                topPtr.ptr,
                rightPtr.ptr,
                bottomPtr.ptr
            )
            Rect(
                left = leftPtr.value,
                top = topPtr.value,
                right = rightPtr.value,
                bottom = bottomPtr.value
            )
        }
    }

    override fun op(
        path1: Path,
        path2: Path,
        operation: PathOperation
    ): Boolean {
        if (nativeRef == null) return false
        path1.pathType = PathType.Native
        path2.pathType = PathType.Native
        val realPath1 = path1.currentPath
        val realPath2 = path2.currentPath
        if (realPath1 !is NativePathImpl || realPath2 !is NativePathImpl) return false
        if (realPath1.nativeRef == null || realPath2.nativeRef == null) return false

        // Note: OHOS PathOp requires both paths to be valid
        // We need to use OH_Drawing_PathOp which is not yet exposed
        // For now, return false as a placeholder
        // TODO: Implement PathOp when OH_Drawing_PathOp is available
        return false
    }

    override fun close() {
        if (nativeRef == null) return
        OHPath_close(nativeRef)
    }

    override fun reset() {
        if (nativeRef == null) return
        OHPath_reset(nativeRef)
    }

    override fun rewind() {
        reset()
    }
}

