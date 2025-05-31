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

package androidx.compose.ui.scene

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.asComposeCanvas
import org.jetbrains.skia.BackendRenderTarget
import org.jetbrains.skia.Color
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.DirectContext
import org.jetbrains.skia.PictureRecorder
import org.jetbrains.skia.PixelGeometry
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface
import org.jetbrains.skia.SurfaceColorFormat
import org.jetbrains.skia.SurfaceOrigin
import org.jetbrains.skia.SurfaceProps

class ComposeSceneRender(
    val onDraw: (canvas: Canvas, timestamp: Long) -> Unit
) {
    private var directContext: DirectContext? = null
    private var renderTarget: BackendRenderTarget? = null
    private var surface: Surface? = null
    private var surfaceCanvas: Canvas? = null
    private var pictureRecorder: PictureRecorder? = null
    private var renderRect = Rect(0f, 0f, 0f, 0f)
    var width: Int = 0
    var height: Int = 0

    fun setSize(width: Int, height: Int) {
        if (this.width != width || this.height != height) {
            this.width = width
            this.height = height
            this.renderRect = Rect(0f, 0f, width.toFloat(), height.toFloat())
            clearSurface()
            clearRecorder()
        }
    }

    fun draw(timestamp: Long) {
        ensureSurface()
        onDraw(surfaceCanvas!!, timestamp)
        flush()
    }

    fun drawByPictureRecorder(timestamp: Long) {
        ensureRecorder()
        val recorderCanvas = pictureRecorder!!.beginRecording(renderRect)
        recorderCanvas.clear(Color.TRANSPARENT)
        recorderCanvas.resetMatrix()
        onDraw(recorderCanvas.asComposeCanvas(), timestamp)

        ensureSurface()
        surface?.canvas?.drawPicture(pictureRecorder!!.finishRecordingAsPicture())
        flush()
    }

    fun close() {
        clearSurface()
        clearRecorder()
        disposeDirectContext()
    }

    private fun ensureSurface() {
        if (directContext == null) {
            directContext = DirectContext.makeGL()
        }
        if (renderTarget == null) {
            renderTarget = BackendRenderTarget.makeGL(width, height, 1, 8, 0, 0x8058)
        }
        if (surface == null) {
            surface = Surface.makeFromBackendRenderTarget(
                directContext!!, // Safe: only accessed from main thread
                renderTarget!!,
                SurfaceOrigin.TOP_LEFT,
                SurfaceColorFormat.RGBA_8888,
                ColorSpace.sRGB,
                SurfaceProps(pixelGeometry = PixelGeometry.UNKNOWN)
            )
            surfaceCanvas = surface?.canvas?.asComposeCanvas()
        }
    }

    private fun disposeDirectContext() {
        directContext?.abandon()
        directContext = null
    }

    private fun clearSurface() {
        renderTarget?.close()
        renderTarget = null
        surface?.close()
        surface = null
    }

    private fun ensureRecorder() {
        if (pictureRecorder == null) {
            pictureRecorder = PictureRecorder()
        }
    }

    private fun clearRecorder() {
        pictureRecorder?.close()
        pictureRecorder = null
    }

    private fun flush() {
        directContext?.flush()
    }
}