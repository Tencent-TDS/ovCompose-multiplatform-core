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
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch

/**
 * copy from AndroidUiDispatcher
 *
 * 主线程Dispatcher,在dispatch时同时使用Handler.post和
 * Choreographer.postFrameCallback分发事件,onFrame和run哪个先回调就在哪里处理事件，然后把事件移除
 *
 * */
class OhosUiDispatcher private constructor(
    val choreographer: Choreographer,
    private val handler: Handler
) : CoroutineDispatcher() {

    // Guards all properties in this class
    private val lock = platformReentrantLockObject()

    private val toRunTrampolined = ArrayDeque<Runnable>()
    private var toRunOnFrame = mutableListOf<FrameCallback>()
    private var spareToRunOnFrame = mutableListOf<FrameCallback>()
    private var scheduledTrampolineDispatch = false
    private var scheduledFrameDispatch = false

    private val dispatchCallback = object : FrameCallback, Runnable {
        override fun run() {
            performTrampolineDispatch()
            synchronized(lock) {
                if (toRunOnFrame.isEmpty()) {
                    choreographer.removeFrameCallback(this)
                    scheduledFrameDispatch = false
                }
            }
        }

        override fun doFrame(frameTimeNanos: Long) {
            synchronized(lock) {
                handler.removeCallbacks(this)
            }
            performTrampolineDispatch()
            performFrameDispatch(frameTimeNanos)
        }
    }

    private fun nextTask(): Runnable? = synchronized(lock) {
        return toRunTrampolined.removeFirstOrNull()
    }

    private fun performTrampolineDispatch() {
        do {
            var task = nextTask()
            while (task != null) {
                task.run()
                task = nextTask()
            }
        } while (
            synchronized(lock) {
                if (toRunTrampolined.isEmpty()) {
                    scheduledTrampolineDispatch = false
                    false
                } else true
            }
        )
    }

    private fun performFrameDispatch(frameTimeNanos: Long) {
        val toRun = synchronized(lock) {
            if (!scheduledFrameDispatch) return
            scheduledFrameDispatch = false
            val result = toRunOnFrame
            toRunOnFrame = spareToRunOnFrame
            spareToRunOnFrame = result
            result
        }
        for (i in 0 until toRun.size) {
            // This callback will not and must not throw, see AndroidUiFrameClock
            toRun[i].doFrame(frameTimeNanos)
        }
        toRun.clear()
    }

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        synchronized(lock) {
            toRunTrampolined.addLast(block)
            if (!scheduledTrampolineDispatch) {
                scheduledTrampolineDispatch = true
                handler.post(dispatchCallback)
                if (!scheduledFrameDispatch) {
                    scheduledFrameDispatch = true
                    choreographer.postFrameCallback(dispatchCallback)
                }
            }
        }
    }

    companion object {
        val Main = OhosUiDispatcher(Choreographer(), Handler())
    }
}

class Choreographer {

    fun removeFrameCallback(callback: FrameCallback) {
        ChoreographerManager.removeFrameCallback(callback)
    }

    fun postFrameCallback(callback: FrameCallback) {
        ChoreographerManager.postFrameCallback(callback)
    }
}

class Handler {

    private val jobs = mutableMapOf<Runnable, Job>()

    fun removeCallbacks(action: Runnable) {
        jobs.remove(action)?.cancel(DefaultException.exception)
    }

    fun post(action: Runnable) {
        jobs[action] = CoroutineScope(Dispatchers.Main).launch {
            action.run()
            jobs.remove(action)
        }
    }
}