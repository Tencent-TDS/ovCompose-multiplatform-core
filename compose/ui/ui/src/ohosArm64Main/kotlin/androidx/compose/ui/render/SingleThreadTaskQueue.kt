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
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import kotlin.time.measureTime

internal class SingleThreadTaskQueue(val name: String) {

    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    private val singleThreadContext = newSingleThreadContext(name)
    private val tasks = Channel<suspend () -> Unit>(Channel.UNLIMITED)
    private val taskCounter: kotlinx.atomicfu.AtomicInt = atomic(0)

    init {
        CoroutineScope(singleThreadContext).launch {
            for (task in tasks) {
                try {
                    // 执行任务
                    measureTimeInternal {
                        task()
                    }
                } catch (e: Throwable) {
                    logger.log { "run task error: ${e.message}" }
                } finally {
                    val count = taskCounter.addAndGet(-1)
                    logger.log { "execute finish task count=$count" }
                }
            }
        }
    }

    /**
     * 提交任务到队列,如果期望在主线程执行，并且队列以经执行完成，则直接执行任务。
     * @param async 是否异步运行
     * @param task 任务逻辑
     */
    @UiThread
    fun submit(async: Boolean = false, task: () -> Unit) {
        // 如果队列中无任务，当前任务需要在主线程中执行，则直接运行来提升效率。
        if (taskCounter.value == 0 && !async) {
            logger.log { "submit and direct run in main thread" }
            task()
            return
        }

        val counter = taskCounter.addAndGet(1)
        logger.log { "submit async=$async, counter=$counter" }
        tasks.trySend {
            if (async) {
                // 在单线程上下文中直接执行任务
                task()
            } else {
                // 切换到主线程并等待任务完成
                withContext(Dispatchers.Main) {
                    task()
                }
            }
        }
    }

    /**
     * 关闭任务队列
     */
    @UiThread
    fun close() {
        tasks.close()
        singleThreadContext.close()
    }

    /**
     * 测量任务执行时间
     */
    private inline fun measureTimeInternal(block: () -> Unit) {
        if (RenderLog.DEBUG) {
            val time = measureTime(block)
            logger.log { "run block time=$time" }
            return
        }
        block()
    }

    companion object {
        val logger = RenderLog.getLogger("SingleThreadTaskQueue")
    }
}
