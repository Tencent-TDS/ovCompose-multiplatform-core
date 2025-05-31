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

package androidx.compose.ui.platform

import androidx.compose.runtime.platformReentrantLockObject
import androidx.compose.runtime.synchronized

/**
 * 只支持主线程的Choreographer单例管理类，vsync回调来自XComponent
 * */
object ChoreographerManager {

    private var curFrameTimeNs = 0L
    private val backingCallbacks1 = ArrayList<FrameCallback>()
    private val backingCallbacks2 = ArrayList<FrameCallback>()
    private var callbacks = backingCallbacks1
    private val lock = platformReentrantLockObject()

    /**
     * 运行在主、子线程
     * */
    fun postFrameCallback(callback: FrameCallback) {
        synchronized(lock) {
            callbacks.add(callback)
        }
    }

    /**
     * 运行在主线程
     * */
    fun removeFrameCallback(callback: FrameCallback) {
        synchronized(lock) {
            callbacks.remove(callback)
        }
    }

    /**
     * 运行在主线程
     * */
    fun onVsync(timestamp: Long) {
        if (curFrameTimeNs == timestamp) {
            return
        }
        curFrameTimeNs = timestamp
        var list: ArrayList<FrameCallback>? = null
        synchronized(lock) {
            list = callbacks
            callbacks = if (callbacks === backingCallbacks1) {
                backingCallbacks2
            } else {
                backingCallbacks1
            }
        }
        list?.forEach { it.doFrame(timestamp) }
        list?.clear()
    }

    /**
     * 获取当前帧时间戳,单位ns
     * */
    fun getCurFrameTimeNs(): Long = curFrameTimeNs
}