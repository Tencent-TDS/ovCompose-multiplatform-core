package androidx.compose.ui.platform.nativefoundation

import androidx.compose.common.interop.LogPrintUtil
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.NativeShaderFactory
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_createNativeImageShader
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_createNativeLinearGradientShader
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_createNativeRadialGradientShader
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_createNativeSweepGradientShader
import kotlinx.cinterop.CValues
import kotlinx.cinterop.UIntVar
import kotlinx.cinterop.toCValues
import platform.arkui.OH_Drawing_TileMode
import platform.native.OH_Drawing_ImageCreate

private inline fun TileMode.asNativeEnum(): OH_Drawing_TileMode {
    return when (this) {
        TileMode.Clamp -> OH_Drawing_TileMode.CLAMP
        TileMode.Mirror -> OH_Drawing_TileMode.MIRROR
        TileMode.Repeated -> OH_Drawing_TileMode.REPEAT
        TileMode.Decal -> OH_Drawing_TileMode.DECAL
        else -> throw RuntimeException("暂不支持")
    }
}

internal object NativeShaderFactoryImpl : NativeShaderFactory {
    override fun makeLinearGradientShader(
        from: Offset,
        to: Offset,
        colors: List<Color>,
        colorStops: List<Float>?,
        tileMode: TileMode
    ): Any {
        val colorsArray = toCValuesColors(colors)
        val posArray = colorStops?.toFloatArray()?.toCValues()
        return androidx_compose_ui_arkui_utils_createNativeLinearGradientShader(
            startX = from.x,
            startY = from.y,
            endX = to.x,
            endY = to.y,
            colors = colorsArray,
            colorPositions = posArray,
            colorCount = colors.size.toUInt(),
            tileMode = tileMode.asNativeEnum().value,
        ) as Any
    }

    override fun makeRadialGradientShader(
        center: Offset,
        radius: Float,
        colors: List<Color>,
        colorStops: List<Float>?,
        tileMode: TileMode
    ): Any {
        val posArray = colorStops?.toFloatArray()?.toCValues()
        val colorsArray = toCValuesColors(colors)
        return androidx_compose_ui_arkui_utils_createNativeRadialGradientShader(
            centerX = center.x,
            centerY = center.y,
            radius = radius,
            colors = colorsArray,
            colorPositions = posArray,
            colorCount = colors.size.toUInt(),
            tileMode = tileMode.asNativeEnum().value
        ) as Any
    }

    override fun makeSweepGradientShader(
        center: Offset,
        colors: List<Color>,
        colorStops: List<Float>?
    ): Any {
        return androidx_compose_ui_arkui_utils_createNativeSweepGradientShader(
            centerX = center.x,
            centerY = center.y,
            colors = toCValuesColors(colors),
            colorPositions = colorStops?.toFloatArray()?.toCValues(),
            colorCount = colors.size.toUInt(),
        ) as Any
    }

    override fun makeImageShader(
        image: ImageBitmap,
        tileModeX: TileMode,
        tileModeY: TileMode
    ): Any {
        return androidx_compose_ui_arkui_utils_createNativeImageShader(
            // TODO 此处涉及到 Compose的ImageBitmap到原生Bitmap的转换
            // TODO 当前先创建一个空的image，编译通过，后续再修改
            image = OH_Drawing_ImageCreate(),
            tileModeX = tileModeX.asNativeEnum().value,
            tileModeY = tileModeY.asNativeEnum().value,
        ) as Any
    }

    private fun toCValuesColors(colors: List<Color>): CValues<UIntVar> {
        val colorsArray = colors.map {
            // 转成ARGB
            val a = (it.alpha * 255.0f).toInt() and 0xFF
            val r = (it.red * 255.0f).toInt() and 0xFF
            val g = (it.green * 255.0f).toInt() and 0xFF
            val b = (it.blue * 255.0f).toInt() and 0xFF
            ((a shl 24) or (r shl 16) or (g shl 8) or b).toUInt()
        }.toUIntArray().toCValues()
        return colorsArray
    }
}
