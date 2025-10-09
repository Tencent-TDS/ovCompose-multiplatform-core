package androidx.compose.ui.platform.nativefoundation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.NativeShaderFactory
import androidx.compose.ui.graphics.TileMode
import kotlinx.cinterop.CValues
import kotlinx.cinterop.UIntVar
import kotlinx.cinterop.toCValues
import platform.arkui.OH_Drawing_ShaderEffectCreateLinearGradient
import platform.arkui.OH_Drawing_ShaderEffectCreateRadialGradient
import platform.arkui.OH_Drawing_TileMode
import platform.arkui.OH_Drawing_PointCreate
import platform.arkui.OH_Drawing_ShaderEffectCreateImageShader
import platform.arkui.OH_Drawing_ShaderEffectCreateSweepGradient
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
        val startPt = OH_Drawing_PointCreate(from.x, from.y)
        val endPt = OH_Drawing_PointCreate(to.x, to.y)
        val colorsArray = toCValuesColors(colors)
        val posArray = colorStops?.toFloatArray()?.toCValues()
        return OH_Drawing_ShaderEffectCreateLinearGradient(
            startPt = startPt,
            endPt = endPt,
            colors = colorsArray,
            pos = posArray,
            size = colors.size.toUInt(),
            tileMode = tileMode.asNativeEnum()
        ) as Any
    }

    override fun makeRadialGradientShader(
        center: Offset,
        radius: Float,
        colors: List<Color>,
        colorStops: List<Float>?,
        tileMode: TileMode
    ): Any {
        val centerPt = OH_Drawing_PointCreate(center.x, center.y)
        val posArray = colorStops?.toFloatArray()?.toCValues()
        val colorsArray = toCValuesColors(colors)
        return OH_Drawing_ShaderEffectCreateRadialGradient(
            centerPt = centerPt,
            radius = radius,
            colors = colorsArray,
            pos = posArray,
            size = colors.size.toUInt(),
            tileMode = tileMode.asNativeEnum()
        ) as Any
    }


    override fun makeSweepGradientShader(
        center: Offset,
        colors: List<Color>,
        colorStops: List<Float>?
    ): Any {
        return OH_Drawing_ShaderEffectCreateSweepGradient(
            centerPt = OH_Drawing_PointCreate(center.x, center.y),
            colors = toCValuesColors(colors),
            pos = colorStops?.toFloatArray()?.toCValues(),
            size = colors.size.toUInt(),
            // Using CLAMP as the default tileMode since Compose's API doesn't require this parameter
            // but the OH native API does require
            tileMode = OH_Drawing_TileMode.CLAMP
        ) as Any
    }

    override fun makeImageShader(
        image: ImageBitmap,
        tileModeX: TileMode,
        tileModeY: TileMode
    ): Any {
        return OH_Drawing_ShaderEffectCreateImageShader(
            // TODO 此处涉及到 Compose的ImageBitmap到原生Bitmap的转换
            // TODO 当前先创建一个空的image，编译通过，后续再修改
            image = OH_Drawing_ImageCreate(),
            tileX = tileModeX.asNativeEnum(),
            tileY = tileModeY.asNativeEnum(),
            samplingOptions = null,
            matrix = null
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
