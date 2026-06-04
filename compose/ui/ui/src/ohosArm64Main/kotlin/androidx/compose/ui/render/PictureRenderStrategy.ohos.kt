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

package androidx.compose.ui.render

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.interop.OhosTrace
import androidx.compose.ui.scene.getInteropActions
import androidx.compose.ui.scene.process
import org.jetbrains.skia.Color
import org.jetbrains.skia.Picture
import org.jetbrains.skia.PictureRecorder

/**
 * 先使用PictureRecorder记性记录，再绘制到XComponent的Surface上。
 * 官方采用的方式。
 */
internal class RenderStrategyPicture : RenderStrategy() {
    override fun prepareRender() {
        OhosTrace.traceSync(TRACE_PREPARE_DRAW) {
            prepareDraw()
        }
    }

    override fun render(timestamp: Long, onDraw: (canvas: Canvas, timestamp: Long) -> Unit) {
        val recorder = ensureRecorder()
        logger.log { "RenderStrategyPicture render" }

        val picture = renderPicture(recorder, onDraw, timestamp)

        OhosTrace.traceSync(TRACE_DRAW_PICTURE) {
            ensureSurface()
            surface?.canvas?.drawPicture(picture)
            picture.close()
        }

        OhosTrace.traceSync(TRACE_FLUSH) {
            flush()
        }

        OhosTrace.traceSync(TRACE_PROCESS_INTEROP_ACTIONS) {
            config.interopContext?.getInteropActions()?.process()
        }
    }

    private fun renderPicture(
        recorder: PictureRecorder,
        onDraw: (canvas: Canvas, timestamp: Long) -> Unit,
        timestamp: Long
    ): Picture {
        return OhosTrace.traceSync(TRACE_RENDER_PICTURE) {
            val recorderCanvas = recorder.beginRecording(renderRect)
            recorderCanvas.clear(Color.TRANSPARENT)
            recorderCanvas.resetMatrix()
            onDraw(recorderCanvas.asComposeCanvas(), timestamp)

            recorder.finishRecordingAsPicture()
        }
    }

    override fun finishRender() {
        OhosTrace.traceSync(TRACE_FINISH_DRAW) {
            finishDraw()
        }
    }
}