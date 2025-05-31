package androidx.compose.ui.platform.v2.nativefoundation

import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RenderEffect
import platform.QuartzCore.CALayer

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

interface IOSNativeCanvas : Canvas {

    fun onPreDraw()

    fun drawLayer(layer: CALayer)

    fun drawLayerWithNativeCanvas(nativeCanvas: IOSNativeCanvas)

    fun onPostDraw()

    fun clipRoundRect(rect: RoundRect)

    fun applyTransformMatrix(
        rotationX: Float,
        rotationY: Float,
        rotationZ: Float,
        scaleX: Float,
        scaleY: Float,
        translationX: Float,
        translationY: Float,
        m34Transform: Double
    )

    fun drawParagraphImage(image: ImageBitmap, width: Int, height: Int, paragraphHashCode: Int)

    fun needRedrawImageWithHashCode(paragraphHashCode: Int, width: Int, height: Int): Boolean

    fun asyncDrawIntoCanvas(globalTask: () -> Long, paragraphHashCode: Int, width: Int, height: Int)

    fun imageFromImageBitmap(paragraphHashCode: Int, imageBitmap: ImageBitmap): Long

    fun applyRenderEffect(renderEffect: RenderEffect?)

    fun clearClip()
}