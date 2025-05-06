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

package androidx.compose.foundation.lazy.layout

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.layout.SubcomposeLayoutState
import androidx.compose.ui.platform.ChoreographerManager
import androidx.compose.ui.platform.FrameCallback
import androidx.compose.ui.platform.LocalArkUIViewController
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.util.trace
import kotlinx.coroutines.Runnable

val LocalOpenPrefetch = staticCompositionLocalOf {
    false
}


@ExperimentalFoundationApi
@Composable
internal actual fun LazyLayoutPreFetcherProxy(
    prefetchState: LazyLayoutPrefetchState,
    itemContentFactory: LazyLayoutItemContentFactory,
    subcomposeLayoutState: SubcomposeLayoutState
) {
    if (!LocalOpenPrefetch.current) {
        return
    }
    val choreographer = LocalChoreographer.current
    val getDrawingTimeNs: () -> Long =
        if (choreographer === AbsentChoreographer) {
            val collector = LocalArkUIViewController.current
            remember(choreographer, collector) { { collector.drawingTime } }
        } else {
            remember(choreographer) { choreographer::getDrawingTimeNs }
        }
    remember(prefetchState, subcomposeLayoutState, itemContentFactory, getDrawingTimeNs) {
        LazyLayoutPrefetcher(
            prefetchState,
            subcomposeLayoutState,
            itemContentFactory,
            getDrawingTimeNs
        )
    }
}

/**
 * copy from LazyLayoutPrefetcher.android
 * */
@ExperimentalFoundationApi
internal class LazyLayoutPrefetcher(
    private val prefetchState: LazyLayoutPrefetchState,
    private val subcomposeLayoutState: SubcomposeLayoutState,
    private val itemContentFactory: LazyLayoutItemContentFactory,
    private val getDrawingTimeNs: () -> Long
) : RememberObserver,
    LazyLayoutPrefetchState.Prefetcher,
    Runnable, FrameCallback {

    /**
     * The list of currently not processed prefetch requests. The requests will be processed one by
     * during subsequent [run]s.
     */
    private val prefetchRequests = mutableVectorOf<PrefetchRequest>()

    /**
     * Average time the prefetching operations takes. Keeping it allows us to not start the work
     * if in this frame we are most likely not going to finish the work in time to not delay the
     * next frame.
     */
    private var averagePrecomposeTimeNs: Long = 0
    private var averagePremeasureTimeNs: Long = 0

    private var prefetchScheduled = false

    /** Is true when LazyList was composed and not yet disposed. */
    private var isActive = false

    /**
     * Callback to be executed when the prefetching is needed.
     * [prefetchRequests] will be used as an input.
     */
    override fun run() {
        if (prefetchRequests.isEmpty() || !prefetchScheduled || !isActive) {
            // incorrect input. ignore
            prefetchScheduled = false
            return
        }
        val currentTimeNs = nanoTime()
        val latestFrameVsyncNs = getDrawingTimeNs()
        val nextFrameNs = latestFrameVsyncNs + getFrameIntervalNs()
        var oneOverTimeTaskAllowed = currentTimeNs > nextFrameNs && latestFrameVsyncNs != ChoreographerManager.getCurFrameTimeNs()
        var scheduleForNextFrame = false

        while (prefetchRequests.isNotEmpty() && !scheduleForNextFrame) {
            val request = prefetchRequests[0]
            val itemProvider = itemContentFactory.itemProvider()
            if (request.canceled || request.index !in 0 until itemProvider.itemCount) {
                prefetchRequests.removeAt(0)
            } else if (request.precomposeHandle == null) {
                trace("compose:lazylist:prefetch:compose") {
                    val beforeTimeNs = nanoTime()
                    // check if there is enough time left in this frame. otherwise, we schedule
                    // a next frame callback in which we will post the message in the handler again.
                    if (enoughTimeLeft(beforeTimeNs, nextFrameNs, averagePrecomposeTimeNs) || oneOverTimeTaskAllowed
                    ) {
                        oneOverTimeTaskAllowed = false
                        val key = itemProvider.getKey(request.index)
                        val contentType = itemProvider.getContentType(request.index)
                        val content = itemContentFactory.getContent(request.index, key, contentType)
                        request.precomposeHandle = subcomposeLayoutState.precompose(key, content)
                        averagePrecomposeTimeNs = calculateAverageTime(
                            nanoTime() - beforeTimeNs,
                            averagePrecomposeTimeNs
                        )
                    } else {
                        scheduleForNextFrame = true
                    }
                }
            } else {
                check(!request.measured) { "request already measured" }
                trace("compose:lazylist:prefetch:measure") {
                    val beforeTimeNs = nanoTime()
                    if (enoughTimeLeft(beforeTimeNs, nextFrameNs, averagePremeasureTimeNs) ||
                        oneOverTimeTaskAllowed
                    ) {
                        oneOverTimeTaskAllowed = false
                        val handle = request.precomposeHandle!!
                        repeat(handle.placeablesCount) { placeableIndex ->
                            handle.premeasure(
                                placeableIndex,
                                request.constraints
                            )
                        }
                        averagePremeasureTimeNs =
                            calculateAverageTime(nanoTime() - beforeTimeNs, averagePremeasureTimeNs)
                        // we finished this request
                        prefetchRequests.removeAt(0)
                    } else {
                        scheduleForNextFrame = true
                    }
                }
            }
        }

        if (scheduleForNextFrame) {
            // there is not enough time left in this frame. we schedule a next frame callback
            // in which we are going to post the message in the handler again.
            ChoreographerManager.postFrameCallback(this)
        } else {
            prefetchScheduled = false
        }
    }

    private fun enoughTimeLeft(now: Long, nextFrame: Long, average: Long) =
        now + average < nextFrame

    /**
     * Choreographer frame callback. It will be called when during the previous frame we didn't
     * have enough time left. We will post a new message in the handler in order to try to
     * prefetch again after this frame.
     */
    override fun doFrame(frameTimeNanos: Long) {
        if (isActive) {
            LazyUtils.post(this)
        }
    }

    private fun calculateAverageTime(new: Long, current: Long): Long {
        // Calculate a weighted moving average of time taken to compose an item. We use weighted
        // moving average to bias toward more recent measurements, and to minimize storage /
        // computation cost. (the idea is taken from RecycledViewPool)
        return if (current == 0L) {
            new
        } else {
            // dividing first to avoid a potential overflow
            current / 4 * 3 + new / 4
        }
    }

    private fun nanoTime(): Long = kotlin.system.getTimeNanos()

    /**
     * 获取一帧时间间隔,单位ns
     * */
    private fun getFrameIntervalNs(): Long = 1000L * 1000000 / 120

    override fun schedulePrefetch(
        index: Int,
        constraints: Constraints
    ): LazyLayoutPrefetchState.PrefetchHandle {
        val request = PrefetchRequest(index, constraints)
        prefetchRequests.add(request)
        if (!prefetchScheduled) {
            prefetchScheduled = true
            // schedule the prefetching
            LazyUtils.post(this)
        }
        return request
    }

    override fun onRemembered() {
        prefetchState.prefetcher = this
        isActive = true
    }

    override fun onForgotten() {
        isActive = false
        prefetchState.prefetcher = null
        LazyUtils.remove(this)
        ChoreographerManager.removeFrameCallback(this)
    }

    override fun onAbandoned() {}

    private class PrefetchRequest(
        val index: Int,
        val constraints: Constraints
    ) : @Suppress("SEALED_INHERITOR_IN_DIFFERENT_MODULE")
    LazyLayoutPrefetchState.PrefetchHandle {

        var precomposeHandle: SubcomposeLayoutState.PrecomposedSlotHandle? = null
        var canceled = false
        var measured = false

        override fun cancel() {
            if (!canceled) {
                canceled = true
                precomposeHandle?.dispose()
                precomposeHandle = null
            }
        }
    }
}