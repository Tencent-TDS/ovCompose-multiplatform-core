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

package androidx.compose.ui.graphics

import androidx.compose.ui.geometry.Offset

class PlatformShader(
    val skiaShader: org.jetbrains.skia.Shader? = null,
    val nativeShader: Any? = null
)

interface NativeShaderFactory {

    fun makeLinearGradientShader(
        from: Offset,
        to: Offset,
        colors: List<Color>,
        colorStops: List<Float>?,
        tileMode: TileMode
    ): Any

    fun makeRadialGradientShader(
        center: Offset,
        radius: Float,
        colors: List<Color>,
        colorStops: List<Float>?,
        tileMode: TileMode
    ): Any

    fun makeSweepGradientShader(
        center: Offset,
        colors: List<Color>,
        colorStops: List<Float>?
    ): Any

    fun makeImageShader(
        image: ImageBitmap,
        tileModeX: TileMode,
        tileModeY: TileMode
    ): Any
}

internal var factory: NativeShaderFactory? = null

internal var nativeShaderFactoryImpl: NativeShaderFactory? = null

fun setNativeShaderFactory(factory: NativeShaderFactory) {
    nativeShaderFactoryImpl = factory
}

internal fun NativeShaderFactory(): NativeShaderFactory? =
    nativeShaderFactoryImpl