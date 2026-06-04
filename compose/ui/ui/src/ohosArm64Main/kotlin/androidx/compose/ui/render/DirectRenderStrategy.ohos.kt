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
import androidx.compose.ui.interop.OhosTrace
import androidx.compose.ui.scene.getInteropActions
import androidx.compose.ui.scene.process

/**
 * 直接绘制到XComponent的Surface上，当前业务正在使用的。
 */
internal class RenderStrategyDirect : RenderStrategy() {
    override fun prepareRender() {
        OhosTrace.traceSync(TRACE_PREPARE_DRAW) {
            prepareDraw()
        }
    }

    override fun render(timestamp: Long, onDraw: (canvas: Canvas, timestamp: Long) -> Unit) {
        logger.log { "RenderStrategyDirect render" }

        OhosTrace.traceSync(TRACE_DRAW) {
            ensureSurface()
            surfaceCanvas?.run {
                onDraw(this, timestamp)
            }
        }
        OhosTrace.traceSync(TRACE_FLUSH) {
            flush()
        }
        OhosTrace.traceSync(TRACE_PROCESS_INTEROP_ACTIONS) {
            config.interopContext?.getInteropActions()?.process()
        }
        dumpSkia(timestamp)
    }

    override fun finishRender() {
        OhosTrace.traceSync(TRACE_FINISH_DRAW) {
            finishDraw()
        }
    }
}