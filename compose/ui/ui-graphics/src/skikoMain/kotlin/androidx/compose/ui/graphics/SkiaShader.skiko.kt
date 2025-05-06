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

import androidx.compose.runtime.EnableIosRenderLayerV2
import androidx.compose.ui.geometry.Offset
import org.jetbrains.skia.GradientStyle

actual typealias Shader = PlatformShader
// actual typealias Shader = org.jetbrains.skia.Shader

// region Tencent Code
internal actual fun ActualLinearGradientShader(
    from: Offset,
    to: Offset,
    colors: List<Color>,
    colorStops: List<Float>?,
    tileMode: TileMode
): Shader = ActualLinearGradientShader(from, to, colors, colorStops, tileMode, false)

internal actual fun ActualLinearGradientShader(
    from: Offset,
    to: Offset,
    colors: List<Color>,
    colorStops: List<Float>?,
    tileMode: TileMode,
    forceUseSkia: Boolean
): Shader {
    validateColorStops(colors, colorStops)
    return if (!EnableIosRenderLayerV2 || forceUseSkia) {
        PlatformShader(
            skiaShader = org.jetbrains.skia.Shader.makeLinearGradient(
                from.x, from.y, to.x, to.y, colors.toIntArray(), colorStops?.toFloatArray(),
                GradientStyle(tileMode.toSkiaTileMode(), true, identityMatrix33())
            )
        )
    } else {
        PlatformShader(
            nativeShader = NativeShaderFactory().makeLinearGradientShader(
                from,
                to,
                colors,
                colorStops,
                tileMode
            )
        )
    }
}
// endregion

internal actual fun ActualRadialGradientShader(
    center: Offset,
    radius: Float,
    colors: List<Color>,
    colorStops: List<Float>?,
    tileMode: TileMode
): Shader {
    validateColorStops(colors, colorStops)
    return if (EnableIosRenderLayerV2) {
        PlatformShader(
            nativeShader = NativeShaderFactory().makeRadialGradientShader(
                center,
                radius,
                colors,
                colorStops,
                tileMode
            )
        )
    } else {
        PlatformShader(
            skiaShader = org.jetbrains.skia.Shader.makeRadialGradient(
                center.x,
                center.y,
                radius,
                colors.toIntArray(),
                colorStops?.toFloatArray(),
                GradientStyle(tileMode.toSkiaTileMode(), true, identityMatrix33())
            )
        )
    }
}

internal actual fun ActualSweepGradientShader(
    center: Offset,
    colors: List<Color>,
    colorStops: List<Float>?
): Shader {
    validateColorStops(colors, colorStops)

    return if (EnableIosRenderLayerV2) {
        PlatformShader(
            nativeShader = NativeShaderFactory().makeSweepGradientShader(
                center,
                colors,
                colorStops
            )
        )
    } else {
        PlatformShader(
            skiaShader = org.jetbrains.skia.Shader.makeSweepGradient(
                center.x,
                center.y,
                colors.toIntArray(),
                colorStops?.toFloatArray()
            )
        )
    }
}

internal actual fun ActualImageShader(
    image: ImageBitmap,
    tileModeX: TileMode,
    tileModeY: TileMode
): Shader {
    // 现在缺少了ImageShader，先解决crash问题，后续补充能力
    return if (EnableIosRenderLayerV2) {
        PlatformShader(
            nativeShader = NativeShaderFactory().makeImageShader(
                image,
                tileModeX,
                tileModeY
            )
        )
    } else {
        PlatformShader(
            skiaShader = image.asSkiaBitmap().makeShader(
                tileModeX.toSkiaTileMode(),
                tileModeY.toSkiaTileMode()
            )
        )
    }
}

private fun List<Color>.toIntArray(): IntArray =
    IntArray(size) { i -> this[i].toArgb() }

private fun validateColorStops(colors: List<Color>, colorStops: List<Float>?) {
    if (colorStops == null) {
        if (colors.size < 2) {
            throw IllegalArgumentException(
                "colors must have length of at least 2 if colorStops " +
                        "is omitted."
            )
        }
    } else if (colors.size != colorStops.size) {
        throw IllegalArgumentException(
            "colors and colorStops arguments must have" +
                    " equal length."
        )
    }
}