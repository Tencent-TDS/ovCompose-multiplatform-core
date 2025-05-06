package androidx.compose.ui.platform.v2.nativefoundation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.NativeShaderFactory
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.UikitImageBitmap
import androidx.compose.ui.uikit.utils.TMMNativeImageShader
import androidx.compose.ui.uikit.utils.TMMNativeLinearGradientShader
import androidx.compose.ui.uikit.utils.TMMNativeRadialGradientShader
import androidx.compose.ui.uikit.utils.TMMNativeSweepGradientShader
import androidx.compose.ui.uikit.utils.TMMNativeTileMode

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

private inline fun TileMode.asNativeEnum(): TMMNativeTileMode {
    return when (this) {
        TileMode.Clamp -> TMMNativeTileMode.TMMNativeTileModeClamp
        TileMode.Mirror -> TMMNativeTileMode.TMMNativeTileModeMirror
        TileMode.Repeated -> TMMNativeTileMode.TMMNativeTileModeRepeated
        TileMode.Decal -> TMMNativeTileMode.TMMNativeTileModeDecal
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
        return TMMNativeLinearGradientShader.shader().apply {
            setFrom(from.x, from.y)
            setTo(to.x, to.y)
            setTileMode(tileMode.asNativeEnum())
            // 用最简单的，速度最快
            for (i in 0 until colors.size) {
                val color = colors[i]
                addColor(
                    colorRed = color.red,
                    colorBlue = color.blue,
                    colorGreen = color.green,
                    colorAlpha = color.alpha
                )
            }

            if (colorStops != null) {
                for (i in 0 until colorStops.size) {
                    val colorStop = colorStops[i]
                    addColorStops(colorStop)
                }
            }
        }
    }

    override fun makeRadialGradientShader(
        center: Offset,
        radius: Float,
        colors: List<Color>,
        colorStops: List<Float>?,
        tileMode: TileMode
    ): Any {
        return TMMNativeRadialGradientShader.shader().apply {
            setCenter(center.x, center.y)
            setRadius(radius)
            setTileMode(tileMode.asNativeEnum())
            // 用最简单的，速度最快
            for (i in 0 until colors.size) {
                val color = colors[i]
                addColor(
                    colorRed = color.red,
                    colorBlue = color.blue,
                    colorGreen = color.green,
                    colorAlpha = color.alpha
                )
            }

            if (colorStops != null) {
                for (i in 0 until colorStops.size) {
                    val colorStop = colorStops[i]
                    addColorStops(colorStop)
                }
            }
        }
    }

    override fun makeSweepGradientShader(
        center: Offset,
        colors: List<Color>,
        colorStops: List<Float>?
    ): Any {
        return TMMNativeSweepGradientShader.shader().apply {
            setCenter(center.x, center.y)

            // 用最简单的，速度最快
            for (i in 0 until colors.size) {
                val color = colors[i]
                addColor(
                    colorRed = color.red,
                    colorBlue = color.blue,
                    colorGreen = color.green,
                    colorAlpha = color.alpha
                )
            }

            if (colorStops != null) {
                for (i in 0 until colorStops.size) {
                    val colorStop = colorStops[i]
                    addColorStops(colorStop)
                }
            }
        }
    }

    override fun makeImageShader(
        image: ImageBitmap,
        tileModeX: TileMode,
        tileModeY: TileMode
    ): Any {
        return TMMNativeImageShader.shader().apply {
            if (image is UikitImageBitmap) {
                setImage(image.uiImage)
            }
            setTileModeX(tileModeX.asNativeEnum())
            setTileModeY(tileModeY.asNativeEnum())
        }
    }
}