package androidx.compose.ui.platform.v2.nativefoundation

import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlendModeColorFilter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorMatrixColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.LightingColorFilter
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.uikit.utils.TMMComposeNativeColorFilter
import androidx.compose.ui.uikit.utils.TMMComposeNativePaint
import androidx.compose.ui.uikit.utils.TMMNativeColorFilterType
import androidx.compose.ui.uikit.utils.TMMNativeDrawBlendMode
import androidx.compose.ui.uikit.utils.TMMNativeDrawFilterQuality
import androidx.compose.ui.uikit.utils.TMMNativeDrawPaintingStyle
import androidx.compose.ui.uikit.utils.TMMNativeDrawStrokeCap
import androidx.compose.ui.uikit.utils.TMMNativeDrawStrokeJoin
import kotlinx.cinterop.set

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

private inline fun PaintingStyle.asNativePaintStyle(): TMMNativeDrawPaintingStyle {
    return when (this) {
        PaintingStyle.Fill -> TMMNativeDrawPaintingStyle.TMMNativeDrawPaintingStyleFill
        PaintingStyle.Stroke -> TMMNativeDrawPaintingStyle.TMMNativeDrawPaintingStyleStroke
        else -> throw RuntimeException("暂不支持")
    }
}

private inline fun StrokeCap.asNativeStrokeCap(): TMMNativeDrawStrokeCap {
    return when (this) {
        StrokeCap.Butt -> TMMNativeDrawStrokeCap.TMMNativeDrawStrokeCapButt
        StrokeCap.Round -> TMMNativeDrawStrokeCap.TMMNativeDrawStrokeCapRound
        StrokeCap.Square -> TMMNativeDrawStrokeCap.TMMNativeDrawStrokeCapSquare
        else -> throw RuntimeException("暂不支持")
    }
}

private inline fun StrokeJoin.asNativeStrokeJoin(): TMMNativeDrawStrokeJoin {
    return when (this) {
        StrokeJoin.Bevel -> TMMNativeDrawStrokeJoin.TMMNativeDrawStrokeJoinBevel
        StrokeJoin.Miter -> TMMNativeDrawStrokeJoin.TMMNativeDrawStrokeJoinMiter
        StrokeJoin.Round -> TMMNativeDrawStrokeJoin.TMMNativeDrawStrokeJoinRound
        else -> throw RuntimeException("暂不支持")
    }
}

private inline fun FilterQuality.asNativeFilterQuality(): TMMNativeDrawFilterQuality {
    return when (this) {
        FilterQuality.Low -> TMMNativeDrawFilterQuality.TMMNativeDrawFilterQualityLow
        FilterQuality.High -> TMMNativeDrawFilterQuality.TMMNativeDrawFilterQualityHigh
        FilterQuality.Medium -> TMMNativeDrawFilterQuality.TMMNativeDrawFilterQualityMedium
        FilterQuality.None -> TMMNativeDrawFilterQuality.TMMNativeDrawFilterQualityNone
        else -> throw RuntimeException("暂不支持")
    }
}

internal fun BlendMode.asNativeBlendMode(): TMMNativeDrawBlendMode {
    return when (this) {
        BlendMode.Clear -> TMMNativeDrawBlendMode.TMMNativeDrawBlendModeClear
        BlendMode.Src -> TMMNativeDrawBlendMode.TMMNativeDrawBlendModeSrc
        BlendMode.Dst -> TMMNativeDrawBlendMode.TMMNativeDrawBlendModeDst
        BlendMode.SrcOver -> TMMNativeDrawBlendMode.TMMNativeDrawBlendModeSrcOver
        BlendMode.DstOver -> TMMNativeDrawBlendMode.TMMNativeDrawBlendModeDstOver
        BlendMode.SrcIn -> TMMNativeDrawBlendMode.TMMNativeDrawBlendModeSrcIn
        BlendMode.DstIn -> TMMNativeDrawBlendMode.TMMNativeDrawBlendModeDstIn
        BlendMode.SrcOut -> TMMNativeDrawBlendMode.TMMNativeDrawBlendModeSrcOut
        BlendMode.DstOut -> TMMNativeDrawBlendMode.TMMNativeDrawBlendModeDstOut
        BlendMode.DstAtop -> TMMNativeDrawBlendMode.TMMNativeDrawBlendModeDstAtop
        BlendMode.Xor -> TMMNativeDrawBlendMode.TMMNativeDrawBlendModeXor
        BlendMode.Plus -> TMMNativeDrawBlendMode.TMMNativeDrawBlendModePlus
        BlendMode.Modulate -> TMMNativeDrawBlendMode.TMMNativeDrawBlendModeModulate
        BlendMode.Screen -> TMMNativeDrawBlendMode.TMMNativeDrawBlendModeScreen
        BlendMode.Overlay -> TMMNativeDrawBlendMode.TMMNativeDrawBlendModeOverlay
        BlendMode.Darken -> TMMNativeDrawBlendMode.TMMNativeDrawBlendModeDarken
        BlendMode.Lighten -> TMMNativeDrawBlendMode.TMMNativeDrawBlendModeLighten
        BlendMode.ColorDodge -> TMMNativeDrawBlendMode.TMMNativeDrawBlendModeColorDodge
        BlendMode.ColorBurn -> TMMNativeDrawBlendMode.TMMNativeDrawBlendModeColorBurn
        BlendMode.Hardlight -> TMMNativeDrawBlendMode.TMMNativeDrawBlendModeHardlight
        BlendMode.Softlight -> TMMNativeDrawBlendMode.TMMNativeDrawBlendModeSoftlight
        BlendMode.Difference -> TMMNativeDrawBlendMode.TMMNativeDrawBlendModeDifference
        BlendMode.Exclusion -> TMMNativeDrawBlendMode.TMMNativeDrawBlendModeExclusion
        BlendMode.Multiply -> TMMNativeDrawBlendMode.TMMNativeDrawBlendModeMultiply
        BlendMode.Hue -> TMMNativeDrawBlendMode.TMMNativeDrawBlendModeHue
        BlendMode.Saturation -> TMMNativeDrawBlendMode.TMMNativeDrawBlendModeSaturation
        BlendMode.Color -> TMMNativeDrawBlendMode.TMMNativeDrawBlendModeColor
        BlendMode.Luminosity -> TMMNativeDrawBlendMode.TMMNativeDrawBlendModeLuminosity
        else -> TMMNativeDrawBlendMode.TMMNativeDrawBlendModeUnknown
    }
}

internal inline fun TMMComposeNativePaint.sync(paint: Paint) {
    alpha = paint.alpha
    colorValue = paint.color.value
    strokeWidth = paint.strokeWidth
    blendMode = paint.blendMode.asNativeBlendMode()
    style = paint.style.asNativePaintStyle()
    strokeCap = paint.strokeCap.asNativeStrokeCap()
    strokeJoin = paint.strokeJoin.asNativeStrokeJoin()
    filterQuality = paint.filterQuality.asNativeFilterQuality()
    strokeMiterLimit = paint.strokeMiterLimit
    shader = paint.shader?.nativeShader

    val colorFilter = paint.colorFilter
    if (colorFilter == null) {
        this.colorFilter = null
    } else {
        ensureColorFilter().fillWithSkiaFilter(colorFilter)
    }
}

internal inline fun TMMComposeNativePaint.ensureColorFilter(): TMMComposeNativeColorFilter {
    val filter = colorFilter as? TMMComposeNativeColorFilter ?: TMMComposeNativeColorFilter()
    colorFilter = filter
    return filter
}

internal inline fun TMMComposeNativeColorFilter.fillWithSkiaFilter(skiaFilter: ColorFilter?) {
    when (skiaFilter) {
        is BlendModeColorFilter -> {
            type = TMMNativeColorFilterType.TMMNativeColorFilterTypeBlend
            blendMode = skiaFilter.blendMode.asNativeBlendMode()
            colorValue = skiaFilter.color.value
        }

        is ColorMatrixColorFilter -> {
            type = TMMNativeColorFilterType.TMMNativeColorFilterTypeMatrix
            // 这里总共三次拷贝，目前看耗时不高，先这样吧。
            // 如果未来PictureRecorder机制还在，最多可以优化到一次拷贝。
            // 如果采用同步绘制，未来可以不拷贝。
            ColorMatrix().apply {
                // 第一次拷贝，将skia的matrix复制出来。
                skiaFilter.copyColorMatrix(this)
                // 第二次拷贝，将复制出来的matrix设置给通用Paint中的TMMComposeNativeColorFilter
                // 第三次拷贝，将通用Paint中的TMMComposeNativeColorFilter的内容赋值给图片的
                // TMMComposeNativeColorFilter，代码在TMMComposeNativeColorFilter.setColorFilterInfo
                for (i in values.indices) {
                    // TODO 做一个20大小的保护，并且减少拷贝次数
                    matrix?.set(i, values[i].toDouble())
                }
            }
        }

        is LightingColorFilter -> {
            val sMultiply = skiaFilter.multiply
            val sAdd = skiaFilter.add
            ColorMatrix().apply {
                if (isRGBZero(sAdd)) {
                    type = TMMNativeColorFilterType.TMMNativeColorFilterTypeBlend
                    blendMode = TMMNativeDrawBlendMode.TMMNativeDrawBlendModeModulate
                    colorValue = Color(sMultiply.toArgb() or Color.Black.toArgb()).value
                } else {
                    type = TMMNativeColorFilterType.TMMNativeColorFilterTypeLighting
                    skiaScale(sMultiply.red, sMultiply.green, sMultiply.blue, 1f)
                    skiaTranslate(sAdd.red.to255(), sAdd.green.to255(), sAdd.blue.to255(), 0f)
                    for (i in values.indices) {
                        // TODO 做一个20大小的保护，并且减少拷贝次数
                        matrix?.set(i, values[i].toDouble())
                    }
                }
            }
        }
    }
}

private fun Float.to255(): Float {
    return this * 255
}

private fun isRGBZero(color: Color): Boolean {
    return color.toArgb() and Color.Black.toArgb().inv() == 0
}

fun ColorMatrix.skiaScale(
    redScale: Float,
    greenScale: Float,
    blueScale: Float,
    alphaScale: Float
) {
    values.fill(0f)
    this[0, 0] = redScale
    this[1, 1] = greenScale
    this[2, 2] = blueScale
    this[3, 3] = alphaScale
}

fun ColorMatrix.skiaTranslate(
    dr: Float,
    dg: Float,
    db: Float,
    da: Float
) {
    this[0, 4] += dr
    this[1, 4] += dg
    this[2, 4] += db
    this[3, 4] += da
}