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

package androidx.compose.ui.window

import androidx.compose.runtime.staticCompositionLocalOf

val LocalChoreographer = staticCompositionLocalOf<Choreographer> { AbsentChoreographer }

/**
 * 简单版单线程的Choreographer,只在主线程使用,未实现线程安全
 * */
open class Choreographer {

    /**
     * 绘制时间戳
     * */
    private var targetDrawingTimeNs = 0L

    private var durationNs = 0L

    private var lastFrameTimeNs = 0L

    /**
     * 获取绘制时间戳,单位ns
     * */
    open fun getTargetDrawingTimeNs(): Long = targetDrawingTimeNs

    open fun getDurationTimeNs(): Long = durationNs

    open fun getLastFrameTimeNs(): Long = lastFrameTimeNs
    /**
     * 触发Draw的时间
     * */
    open fun onDraw(timestamp: Long, duration: Long, lastFrameTime: Long) {
        targetDrawingTimeNs = timestamp
        durationNs = duration
        lastFrameTimeNs = lastFrameTime
    }
}

internal object AbsentChoreographer : Choreographer() {
    override fun getTargetDrawingTimeNs(): Long =
        error("CompositionLocal LocalChoreographer not provided")

    override fun onDraw(timestamp: Long, duration: Long, lastFrameTime: Long) =
        error("CompositionLocal LocalChoreographer not provided")
}