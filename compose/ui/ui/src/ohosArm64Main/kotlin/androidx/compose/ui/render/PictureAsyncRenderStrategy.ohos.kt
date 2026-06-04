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

import androidx.annotation.UiThread
import androidx.compose.runtime.platformSynchronizedObject
import androidx.compose.runtime.synchronized
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.interop.ArkUIInteropAction
import androidx.compose.ui.interop.OhosTrace
import androidx.compose.ui.scene.getInteropActions
import androidx.compose.ui.scene.process
import kotlinx.cinterop.COpaquePointer
import org.jetbrains.skia.Color
import org.jetbrains.skia.Picture
import platform.devices.OH_QoS_SetThreadQoS
import platform.devices.QOS_USER_INTERACTIVE

/**
 * 异步绘制模式，主线程进行PictureRecorder进行录制，子线程进行上屏。
 * 充分利用多线程提升性能，当前存在一些问题待优化。
 */
internal class RenderStrategyPictureAsync : RenderStrategy() {

    private val taskQueue = SingleThreadTaskQueue("RenderStrategyPictureAsync")
    private val lock = platformSynchronizedObject()
    private var lastRenderInfo: RenderInfo? = null

    init {
        taskQueue.submit(async = true) {
            // 提升线程优先级
            OH_QoS_SetThreadQoS(QOS_USER_INTERACTIVE)
        }
    }

    override var render: COpaquePointer?
        get() = super.render
        set(value) {
            synchronized(lock) {
                // 加锁，避免在异步渲染的时候，XComponent被释放
                super.render = value
            }
        }

    @UiThread
    override fun prepareRender() {
        // do nothing
    }

    override fun onSystemVsync(timestamp: Long) {
        lastRenderInfo?.let { renderInfo ->
            lastRenderInfo = null
            renderAsync(renderInfo.async, renderInfo.picture, renderInfo.actions)
        }
    }

    @UiThread
    override fun render(timestamp: Long, onDraw: (canvas: Canvas, timestamp: Long) -> Unit) {
        lastRenderInfo?.picture?.close()

        val picture = renderPicture(onDraw, timestamp)
        val actions = config.interopContext?.getInteropActions()
        val renderAsync = canAsync(actions)
        logger.log { "RenderStrategyPictureAsync render, canAsync=${renderAsync}" }

        lastRenderInfo = RenderInfo(
            async = renderAsync,
            picture = picture,
            actions = actions
        )
    }

    private fun renderAsync(
        renderAsync: Boolean,
        picture: Picture,
        actions: List<ArkUIInteropAction>?
    ) {
        runBlock(renderAsync) {
            OhosTrace.traceSync(TRACE_DRAW_PICTURE_ASYNC) {
                drawPicture(picture)
                picture.close()
            }

            // 异步绘制 actions 一定为空，同步绘制时进行处理
            OhosTrace.traceSync(TRACE_PROCESS_INTEROP_ACTIONS) {
                if (!renderAsync) {
                    actions?.process()
                }
            }

            OhosTrace.traceSync(TRACE_FINISH_DRAW) {
                synchronized(lock) {
                    finishDraw()
                }
                makeCurrentNull()
            }
        }
    }

    @UiThread
    override fun finishRender() {
        // do nothing
    }

    override fun close() {
        super.close()
        taskQueue.close()
    }

    private fun drawPicture(picture: Picture) {
        synchronized(lock) {
            if (!prepareDraw()) {
                logger.log { "RenderStrategyPictureAsync prepareDraw failed" }
                return
            }

            ensureSurface()
            surface?.canvas?.drawPicture(picture)

            OhosTrace.traceSync(TRACE_FLUSH) {
                flush()
            }
        }
    }

    private fun renderPicture(
        onDraw: (canvas: Canvas, timestamp: Long) -> Unit,
        timestamp: Long
    ): Picture {
        return OhosTrace.traceSync(TRACE_RENDER_PICTURE) {
            val recorder = ensureRecorder()
            val recorderCanvas = recorder.beginRecording(renderRect)
            recorderCanvas.clear(Color.TRANSPARENT)
            recorderCanvas.resetMatrix()
            onDraw(recorderCanvas.asComposeCanvas(), timestamp)

            recorder.finishRecordingAsPicture()
        }
    }

    private inline fun canAsync(actions: List<ArkUIInteropAction>?): Boolean {
        return actions?.isEmpty() ?: false
                && !RenderStrategyFactory.forceMainRender
    }

    private fun runBlock(async: Boolean, task: () -> Unit) {
        taskQueue.submit(async, task)
    }

    data class RenderInfo(
        val async: Boolean,
        val picture: Picture,
        val actions: List<ArkUIInteropAction>?,
    )
}