package androidx.compose.ui.platform.nativefoundation

import androidx.compose.ui.arkui.utils.NativeBasicShader_Handle
import androidx.compose.ui.arkui.utils.OHComposeNativePaint_Handle
import androidx.compose.ui.arkui.utils.OH_Native_Draw_FilterQuality
import androidx.compose.ui.arkui.utils.OH_Native_Draw_PaintingStyle
import androidx.compose.ui.arkui.utils.OH_Native_Draw_StrokeCap
import androidx.compose.ui.arkui.utils.OH_Native_Draw_StrokeJoin
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHComposeNativePaint_setBlendMode
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHComposeNativePaint_setAlpha
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHComposeNativePaint_setColor
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHComposeNativePaint_setFilterQuality
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHComposeNativePaint_setIsAntiAlias
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHComposeNativePaint_setShader
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHComposeNativePaint_setStrokeCap
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHComposeNativePaint_setStrokeJoin
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHComposeNativePaint_setStrokeMiterLimit
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHComposeNativePaint_setStrokeWidth
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHComposeNativePaint_setStyle
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import platform.arkui.OH_Drawing_BlendMode

internal fun BlendMode.asNativeBlendMode(): OH_Drawing_BlendMode {
    return when (this) {
        BlendMode.Clear -> OH_Drawing_BlendMode.BLEND_MODE_CLEAR
        BlendMode.Src -> OH_Drawing_BlendMode.BLEND_MODE_SRC
        BlendMode.Dst -> OH_Drawing_BlendMode.BLEND_MODE_DST
        BlendMode.SrcOver -> OH_Drawing_BlendMode.BLEND_MODE_SRC_OVER
        BlendMode.DstOver -> OH_Drawing_BlendMode.BLEND_MODE_DST_OVER
        BlendMode.SrcIn -> OH_Drawing_BlendMode.BLEND_MODE_SRC_IN
        BlendMode.DstIn -> OH_Drawing_BlendMode.BLEND_MODE_DST_IN
        BlendMode.SrcOut -> OH_Drawing_BlendMode.BLEND_MODE_SRC_OUT
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

internal fun PaintingStyle.asNativePaintStyle(): OH_Native_Draw_PaintingStyle {
    return when (this) {
        PaintingStyle.Fill -> OH_Native_Draw_PaintingStyle.OH_NATIVE_PAINTING_STYLE_FILL
        PaintingStyle.Stroke -> OH_Native_Draw_PaintingStyle.OH_NATIVE_PAINTING_STYLE_STROKE
        else -> throw RuntimeException("Unsupported PaintingStyle: $this")
    }

}

private inline fun StrokeCap.asNativeStrokeCap(): OH_Native_Draw_StrokeCap {
    return when (this) {
        StrokeCap.Butt -> OH_Native_Draw_StrokeCap.OH_NATIVE_STROKE_CAP_BUTT
        StrokeCap.Round -> OH_Native_Draw_StrokeCap.OH_NATIVE_STROKE_CAP_ROUND
        StrokeCap.Square -> OH_Native_Draw_StrokeCap.OH_NATIVE_STROKE_CAP_SQUARE
        else -> throw RuntimeException("Unsupported StrokeCap: $this")
    }
}

private inline fun StrokeJoin.asNativeStrokeJoin(): OH_Native_Draw_StrokeJoin {
    return when (this) {
        StrokeJoin.Bevel -> OH_Native_Draw_StrokeJoin.OH_NATIVE_STROKE_JOIN_BEVEL
        StrokeJoin.Miter -> OH_Native_Draw_StrokeJoin.OH_NATIVE_STROKE_JOIN_MITER
        StrokeJoin.Round -> OH_Native_Draw_StrokeJoin.OH_NATIVE_STROKE_JOIN_ROUND
        else -> throw RuntimeException("Unsupported StrokeJoin: $this")
    }
}

private inline fun FilterQuality.asNativeFilterQuality(): OH_Native_Draw_FilterQuality {
    return when (this) {
        FilterQuality.Low -> OH_Native_Draw_FilterQuality.OH_NATIVE_FILTER_QUALITY_LOW
        FilterQuality.High -> OH_Native_Draw_FilterQuality.OH_NATIVE_FILTER_QUALITY_HIGH
        FilterQuality.Medium -> OH_Native_Draw_FilterQuality.OH_NATIVE_FILTER_QUALITY_MEDIUM
        FilterQuality.None -> OH_Native_Draw_FilterQuality.OH_NATIVE_FILTER_QUALITY_NONE
        else -> throw RuntimeException("Unsupported FilterQuality: $this")
    }
}

class OHComposeNativePaint(val handle: OHComposeNativePaint_Handle?) {
    fun sync(paint: Paint) {
        // 实现与 OHComposeNativePaint 的同步逻辑
        androidx_compose_ui_arkui_utils_OHComposeNativePaint_setAlpha(handle, paint.alpha)
        androidx_compose_ui_arkui_utils_OHComposeNativePaint_setIsAntiAlias(handle, paint.isAntiAlias)
        androidx_compose_ui_arkui_utils_OHComposeNativePaint_setColor(handle, paint.color.value)
        androidx_compose_ui_arkui_utils_OHComposeNativePaint_setStrokeWidth(handle, paint.strokeWidth)
        androidx_compose_ui_arkui_utils_OHComposeNativePaint_setBlendMode(handle, paint.blendMode.asNativeBlendMode().value)
        androidx_compose_ui_arkui_utils_OHComposeNativePaint_setStyle(handle, paint.style.asNativePaintStyle().value)
        androidx_compose_ui_arkui_utils_OHComposeNativePaint_setStrokeCap(handle, paint.strokeCap.asNativeStrokeCap().value)
        androidx_compose_ui_arkui_utils_OHComposeNativePaint_setStrokeJoin(handle, paint.strokeJoin.asNativeStrokeJoin().value)
        androidx_compose_ui_arkui_utils_OHComposeNativePaint_setFilterQuality(handle, paint.filterQuality.asNativeFilterQuality().value)
        androidx_compose_ui_arkui_utils_OHComposeNativePaint_setStrokeMiterLimit(handle, paint.strokeMiterLimit)
        androidx_compose_ui_arkui_utils_OHComposeNativePaint_setShader(handle, paint.shader?.nativeShader as NativeBasicShader_Handle?)
        // TODO setColorFilter
        // androidx_compose_ui_arkui_utils_OHComposeNativePaint_setColorFilter(handle, paint.colorFilter?.nativeColorFilter)
    }
}


