package androidx.compose.ui.platform.nativefoundation

import androidx.compose.common.interop.TraceUtil
import androidx.compose.ui.arkui.utils.NativeBasicShader_Handle
import androidx.compose.ui.arkui.utils.OHComposeNativePaint_Handle
import androidx.compose.ui.arkui.utils.OHComposeNativeColorFilter_Handle
import androidx.compose.ui.arkui.utils.OH_Native_Draw_FilterQuality
import androidx.compose.ui.arkui.utils.OH_Native_Draw_PaintingStyle
import androidx.compose.ui.arkui.utils.OH_Native_Draw_StrokeCap
import androidx.compose.ui.arkui.utils.OH_Native_Draw_StrokeJoin
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_DisposeOHComposeNativePaint
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHComposeNativePaint_syncAll
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_createNativeTintColorFilter
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_createNativeColorMatrixColorFilter
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_createNativeLightingColorFilter
import androidx.compose.ui.graphics.*
import kotlinx.cinterop.toCValues
import platform.arkui.OH_Drawing_BlendMode
import platform.native.OH_Drawing_PointMode

internal inline fun BlendMode.asNativeBlendMode(): OH_Drawing_BlendMode {
    return when (this) {
        BlendMode.Clear -> OH_Drawing_BlendMode.BLEND_MODE_CLEAR
        BlendMode.Src -> OH_Drawing_BlendMode.BLEND_MODE_SRC
        BlendMode.Dst -> OH_Drawing_BlendMode.BLEND_MODE_DST
        BlendMode.SrcOver -> OH_Drawing_BlendMode.BLEND_MODE_SRC_OVER
        BlendMode.DstOver -> OH_Drawing_BlendMode.BLEND_MODE_DST_OVER
        BlendMode.SrcIn -> OH_Drawing_BlendMode.BLEND_MODE_SRC_IN
        BlendMode.DstIn -> OH_Drawing_BlendMode.BLEND_MODE_DST_IN
        BlendMode.SrcOut -> OH_Drawing_BlendMode.BLEND_MODE_SRC_OUT
        BlendMode.SrcAtop -> OH_Drawing_BlendMode.BLEND_MODE_SRC_ATOP
        BlendMode.DstOut -> OH_Drawing_BlendMode.BLEND_MODE_DST_OUT
        BlendMode.DstAtop -> OH_Drawing_BlendMode.BLEND_MODE_DST_ATOP
        BlendMode.Xor -> OH_Drawing_BlendMode.BLEND_MODE_XOR
        BlendMode.Plus -> OH_Drawing_BlendMode.BLEND_MODE_PLUS
        BlendMode.Modulate -> OH_Drawing_BlendMode.BLEND_MODE_MODULATE
        BlendMode.Screen -> OH_Drawing_BlendMode.BLEND_MODE_SCREEN
        BlendMode.Overlay -> OH_Drawing_BlendMode.BLEND_MODE_OVERLAY
        BlendMode.Darken -> OH_Drawing_BlendMode.BLEND_MODE_DARKEN
        BlendMode.Lighten -> OH_Drawing_BlendMode.BLEND_MODE_LIGHTEN
        BlendMode.ColorDodge -> OH_Drawing_BlendMode.BLEND_MODE_COLOR_DODGE
        BlendMode.ColorBurn -> OH_Drawing_BlendMode.BLEND_MODE_COLOR_BURN
        BlendMode.Hardlight -> OH_Drawing_BlendMode.BLEND_MODE_HARD_LIGHT
        BlendMode.Softlight -> OH_Drawing_BlendMode.BLEND_MODE_SOFT_LIGHT
        BlendMode.Difference -> OH_Drawing_BlendMode.BLEND_MODE_DIFFERENCE
        BlendMode.Exclusion -> OH_Drawing_BlendMode.BLEND_MODE_EXCLUSION
        BlendMode.Multiply -> OH_Drawing_BlendMode.BLEND_MODE_MULTIPLY
        BlendMode.Hue -> OH_Drawing_BlendMode.BLEND_MODE_HUE
        BlendMode.Saturation -> OH_Drawing_BlendMode.BLEND_MODE_SATURATION
        BlendMode.Color -> OH_Drawing_BlendMode.BLEND_MODE_COLOR
        BlendMode.Luminosity -> OH_Drawing_BlendMode.BLEND_MODE_LUMINOSITY
        else -> throw RuntimeException("Unsupported BlendMode: $this")
    }
}

internal inline fun PaintingStyle.asNativePaintStyle(): OH_Native_Draw_PaintingStyle {
    return when (this) {
        PaintingStyle.Fill -> OH_Native_Draw_PaintingStyle.Fill
        PaintingStyle.Stroke -> OH_Native_Draw_PaintingStyle.Stroke
        else -> throw RuntimeException("Unsupported PaintingStyle: $this")
    }

}

private inline fun StrokeCap.asNativeStrokeCap(): OH_Native_Draw_StrokeCap {
    return when (this) {
        StrokeCap.Butt -> OH_Native_Draw_StrokeCap.StrokeCapButt
        StrokeCap.Round -> OH_Native_Draw_StrokeCap.StrokeCapRound
        StrokeCap.Square -> OH_Native_Draw_StrokeCap.StrokeCapSquare
        else -> throw RuntimeException("Unsupported StrokeCap: $this")
    }
}

private inline fun StrokeJoin.asNativeStrokeJoin(): OH_Native_Draw_StrokeJoin {
    return when (this) {
        StrokeJoin.Bevel -> OH_Native_Draw_StrokeJoin.StrokeJoinBevel
        StrokeJoin.Miter -> OH_Native_Draw_StrokeJoin.StrokeJoinMitter
        StrokeJoin.Round -> OH_Native_Draw_StrokeJoin.StrokeJoinRound
        else -> throw RuntimeException("Unsupported StrokeJoin: $this")
    }
}

private inline fun FilterQuality.asNativeFilterQuality(): OH_Native_Draw_FilterQuality {
    return when (this) {
        FilterQuality.Low -> OH_Native_Draw_FilterQuality.Low
        FilterQuality.High -> OH_Native_Draw_FilterQuality.High
        FilterQuality.Medium -> OH_Native_Draw_FilterQuality.Medium
        FilterQuality.None -> OH_Native_Draw_FilterQuality.None
        else -> throw RuntimeException("Unsupported FilterQuality: $this")
    }
}

internal inline fun PointMode.asNativePointMode(): OH_Drawing_PointMode {
    return when (this) {
        PointMode.Points -> OH_Drawing_PointMode.POINT_MODE_POINTS
        PointMode.Lines -> OH_Drawing_PointMode.POINT_MODE_LINES
        PointMode.Polygon -> OH_Drawing_PointMode.POINT_MODE_POLYGON
        else -> throw RuntimeException("Unsupported PointMode: $this")
    }
}

fun Paint.toReadableString(): String {
    return "Paint(alpha=$alpha, " +
            "isAntiAlias=$isAntiAlias, " +
            "color=$color, " +
            "blendMode=$blendMode, " +
            "style=$style, " +
            "strokeWidth=$strokeWidth, " +
            "strokeCap=$strokeCap, " +
            "strokeJoin=$strokeJoin, " +
            "strokeMiterLimit=$strokeMiterLimit, " +
            "filterQuality=$filterQuality, " +
            "shader=$shader, " +
            "colorFilter=$colorFilter)"
}

class OHComposeNativePaint(handle: OHComposeNativePaint_Handle?) :
    NativeResourceHolder<OHComposeNativePaint_Handle>(
        handle,
        ::androidx_compose_ui_arkui_utils_DisposeOHComposeNativePaint
    ) {

    fun sync(paint: Paint) {
        // 同步 ColorFilter（参考iOS实现）
        val colorFilter = paint.colorFilter
        val nativeColorFilter = if (colorFilter != null) {
            createNativeColorFilter(colorFilter)
        } else {
            null
        }
        // 使用批量同步函数，将11次FFI调用合并为1次，大幅提升性能
        androidx_compose_ui_arkui_utils_OHComposeNativePaint_syncAll(
            handle,
            paint.alpha,
            paint.isAntiAlias,
            paint.color.value,
            paint.strokeWidth,
            paint.blendMode.asNativeBlendMode().value,
            paint.style.asNativePaintStyle().value,
            paint.strokeCap.asNativeStrokeCap().value,
            paint.strokeJoin.asNativeStrokeJoin().value,
            paint.filterQuality.asNativeFilterQuality().value,
            paint.strokeMiterLimit,
            paint.shader?.nativeShader as NativeBasicShader_Handle?,
            nativeColorFilter
        )
    }
}

/**
 * 将Compose ColorFilter转换为Native ColorFilter
 * 参考iOS的fillWithSkiaFilter实现
 */
private fun createNativeColorFilter(colorFilter: ColorFilter): OHComposeNativeColorFilter_Handle? {
    return when (colorFilter) {
        is BlendModeColorFilter -> {
            // Tint类型的ColorFilter
            val argb = colorFilter.color.toArgbULong()
            val blendModeValue = colorFilter.blendMode.asNativeBlendMode().value

            androidx_compose_ui_arkui_utils_createNativeTintColorFilter(
                colorValue = argb,
                blendMode = blendModeValue.toInt()
            )
        }

        is ColorMatrixColorFilter -> {
            // ColorMatrix类型的ColorFilter
            val matrix = ColorMatrix()
            colorFilter.copyColorMatrix(matrix)

            // OHOS Drawing API使用0-255范围的offset值
            // Compose使用0-1范围，需要转换
            val remappedValues = matrix.values.copyOf()
            remappedValues[4] /= 255f   // Red offset
            remappedValues[9] /= 255f   // Green offset
            remappedValues[14] /= 255f  // Blue offset
            remappedValues[19] /= 255f  // Alpha offset

            val matrixArray = remappedValues.toCValues()

            androidx_compose_ui_arkui_utils_createNativeColorMatrixColorFilter(
                matrix = matrixArray,
                matrixSize = remappedValues.size.toUInt()
            )
        }

        is LightingColorFilter -> {
            // Lighting类型的ColorFilter
            val multiply = colorFilter.multiply
            val add = colorFilter.add

            // 检查是否可以优化为简单的Blend模式（iOS的优化逻辑）
            if (isRGBZero(add)) {
                // 如果add为0，可以使用Modulate blend模式
                val color = Color(multiply.toArgb() or Color.Black.toArgb())
                val argb = color.toArgbULong()

                androidx_compose_ui_arkui_utils_createNativeTintColorFilter(
                    colorValue = argb,
                    blendMode = 12 // BlendModeModulate
                )
            } else {
                // 完整的lighting效果，使用ColorMatrix实现
                val multiplyArgb = multiply.toArgbULong()
                val addArgb = add.toArgbULong()

                androidx_compose_ui_arkui_utils_createNativeLightingColorFilter(
                    multiplyColor = multiplyArgb,
                    addColor = addArgb
                )
            }
        }

        else -> null
    }
}

/**
 * 将Color转换为ARGB格式的ULong
 */
private fun Color.toArgbULong(): ULong {
    val a = (alpha * 255.0f).toInt() and 0xFF
    val r = (red * 255.0f).toInt() and 0xFF
    val g = (green * 255.0f).toInt() and 0xFF
    val b = (blue * 255.0f).toInt() and 0xFF
    return ((a shl 24) or (r shl 16) or (g shl 8) or b).toULong()
}

/**
 * 检查Color的RGB是否为0（参考iOS实现）
 */
private fun isRGBZero(color: Color): Boolean {
    return color.toArgb() and Color.Black.toArgb().inv() == 0
}