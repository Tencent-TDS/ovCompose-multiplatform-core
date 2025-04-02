/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.collection.mutableScatterMapOf
import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.internal.checkPrecondition
import androidx.compose.foundation.internal.requirePrecondition
import androidx.compose.foundation.internal.requirePreconditionNotNull
import androidx.compose.foundation.lazy.layout.LazyLayoutPrefetchState.LazyLayoutPrefetchResultScope
import androidx.compose.foundation.lazy.layout.LazyLayoutPrefetchState.PrefetchHandle
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayoutState
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.TraversableNode.Companion.TraverseDescendantsAction
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.trace
import androidx.compose.ui.util.traceValue
import kotlin.time.TimeSource.Monotonic.markNow

/**
 * State for lazy items prefetching, used by lazy layouts to instruct the prefetcher.
 *
 * Note: this class is a part of [LazyLayout] harness that allows for building custom lazy layouts.
 * LazyLayout and all corresponding APIs are still under development and are subject to change.
 *
 * @param prefetchScheduler the [PrefetchScheduler] implementation to use to execute prefetch
 *   requests. If null is provided, the default [PrefetchScheduler] for the platform will be used.
 * @param onNestedPrefetch a callback which will be invoked when this LazyLayout is prefetched in
 *   context of a parent LazyLayout, giving a chance to recursively prefetch its own children. See
 *   [NestedPrefetchScope].
 */
@ExperimentalFoundationApi
@Stable
class LazyLayoutPrefetchState(
    internal val prefetchScheduler: PrefetchScheduler? = null,
    private val onNestedPrefetch: (NestedPrefetchScope.() -> Unit)? = null
) {

    private val prefetchMetrics: PrefetchMetrics = PrefetchMetrics()
    internal var prefetchHandleProvider: PrefetchHandleProvider? = null

    /**
     * Schedules precomposition for the new item. If you also want to premeasure the item please use
     * a second overload accepting a [Constraints] param.
     *
     * @param index item index to prefetch.
     */
    @Deprecated(
        "Please use schedulePrecomposition(index) instead",
        level = DeprecationLevel.WARNING
    )
    fun schedulePrefetch(index: Int): PrefetchHandle {
        return prefetchHandleProvider?.schedulePrecomposition(
            index,
            prefetchMetrics,
        ) ?: DummyHandle
    }

    /**
     * Schedules precomposition for the new item. If you also want to premeasure the item please use
     * [schedulePrecompositionAndPremeasure] instead. This function should only be called once per
     * item. If the item has already been composed at the time this request executes, either from a
     * previous call to this function or because the item is already visible, this request should
     * have no meaningful effect.
     *
     * @param index item index to prefetch.
     */
    fun schedulePrecomposition(index: Int): PrefetchHandle {
        return prefetchHandleProvider?.schedulePrecomposition(
            index,
            prefetchMetrics,
        ) ?: DummyHandle
    }

    /**
     * Schedules precomposition and premeasure for the new item.
     *
     * @param index item index to prefetch.
     * @param constraints [Constraints] to use for premeasuring.
     */
    @Deprecated(
        "Please use schedulePremeasure(index, constraints) instead",
        level = DeprecationLevel.WARNING
    )
    fun schedulePrefetch(index: Int, constraints: Constraints): PrefetchHandle =
        schedulePrecompositionAndPremeasure(index, constraints, null)

    /**
     * Schedules precomposition and premeasure for the new item. This should be used instead of
     * [schedulePrecomposition] if you also want to premeasure the item. This function should only
     * be called once per item. If the item has already been composed / measured at the time this
     * request executes, either from a previous call to this function or because the item is already
     * visible, this request should have no meaningful effect.
     *
     * @param index item index to prefetch.
     * @param constraints [Constraints] to use for premeasuring.
     * @param onItemPremeasured This callback is called when the item premeasuring is finished. If
     *   the request is canceled or no measuring is performed this callback won't be called. Use
     *   [LazyLayoutPrefetchResultScope.getSize] to get the item's size.
     */
    fun schedulePrecompositionAndPremeasure(
        index: Int,
        constraints: Constraints,
        onItemPremeasured: (LazyLayoutPrefetchResultScope.() -> Unit)? = null
    ): PrefetchHandle {
        return prefetchHandleProvider?.schedulePremeasure(
            index,
            constraints,
            prefetchMetrics,
            onItemPremeasured
        ) ?: DummyHandle
    }

    internal fun collectNestedPrefetchRequests(): List<PrefetchRequest> {
        val onNestedPrefetch = onNestedPrefetch ?: return emptyList()

        return NestedPrefetchScopeImpl().run {
            onNestedPrefetch()
            requests
        }
    }

    sealed interface PrefetchHandle {
        /**
         * Notifies the prefetcher that previously scheduled item is no longer needed. If the item
         * was precomposed already it will be disposed.
         */
        fun cancel()

        /**
         * Marks this prefetch request as urgent, which is a way to communicate that the requested
         * item is expected to be needed during the next frame.
         *
         * For urgent requests we can proceed with doing the prefetch even if the available time in
         * the frame is less than we spend on similar prefetch requests on average.
         */
        fun markAsUrgent()
    }

    /**
     * A scope for [schedulePrefetch] callbacks. The scope provides additional information about a
     * prefetched item.
     */
    interface LazyLayoutPrefetchResultScope {

        /** The amount of placeables composed into this item. */
        val placeablesCount: Int

        /** The index of the prefetched item. */
        val index: Int

        /** Retrieves the latest measured size for a given placeable [placeableIndex] in pixels. */
        fun getSize(placeableIndex: Int): IntSize
    }

    private inner class NestedPrefetchScopeImpl : NestedPrefetchScope {

        val requests: List<PrefetchRequest>
            get() = _requests

        private val _requests: MutableList<PrefetchRequest> = mutableListOf()

        override fun schedulePrecomposition(index: Int) {
            val prefetchHandleProvider = prefetchHandleProvider ?: return
            _requests.add(
                prefetchHandleProvider.createNestedPrefetchRequest(index, prefetchMetrics)
            )
        }

        override fun schedulePrecompositionAndPremeasure(index: Int, constraints: Constraints) {
            val prefetchHandleProvider = prefetchHandleProvider ?: return
            _requests.add(
                prefetchHandleProvider.createNestedPrefetchRequest(
                    index,
                    constraints,
                    prefetchMetrics
                )
            )
        }
    }
}

/**
 * A scope which allows nested prefetches to be requested for the precomposition of a LazyLayout.
 */
@ExperimentalFoundationApi
sealed interface NestedPrefetchScope {

    /**
     * Requests a child index to be prefetched as part of the prefetch of a parent LazyLayout.
     *
     * The prefetch will only do the precomposition for the new item. If you also want to premeasure
     * please use a second overload accepting a [Constraints] param.
     *
     * @param index item index to prefetch.
     */
    @Deprecated(
        "Please use schedulePrecomposition(index) instead",
        level = DeprecationLevel.WARNING
    )
    fun schedulePrefetch(index: Int) = schedulePrecomposition(index)

    /**
     * Requests a child index to be precomposed as part of the prefetch of a parent LazyLayout.
     *
     * The prefetch will only do the precomposition for the new item. If you also want to premeasure
     * please use [schedulePrecompositionAndPremeasure].
     *
     * @param index item index to prefetch.
     */
    fun schedulePrecomposition(index: Int)

    /**
     * Requests a child index to be prefetched as part of the prefetch of a parent LazyLayout.
     *
     * @param index the index of the child to prefetch.
     * @param constraints [Constraints] to use for premeasuring.
     */
    @Deprecated(
        "Please use schedulePremeasure(index, constraints) instead",
        level = DeprecationLevel.WARNING
    )
    fun schedulePrefetch(index: Int, constraints: Constraints) =
        schedulePrecompositionAndPremeasure(index, constraints)

    /**
     * Requests a child index to be precomposed and premeasured as part of the prefetch of a parent
     * LazyLayout. If you just want to precompose an item use [schedulePrecomposition] instead.
     *
     * @param index the index of the child to prefetch.
     * @param constraints [Constraints] to use for premeasuring.
     */
    fun schedulePrecompositionAndPremeasure(index: Int, constraints: Constraints)
}

/**
 * [PrefetchMetrics] tracks timings for subcompositions so that they can be used to estimate whether
 * we can fit prefetch work into idle time without delaying the start of the next frame.
 */
internal class PrefetchMetrics {

    /**
     * We keep the overall average numbers and averages for each content type separately. the idea
     * is once we encounter a new content type we don't want to start with no averages, instead we
     * use the overall averages initially until we collected more data.
     */
    fun getAverage(contentType: Any?): Averages {
        val lastUsedAverage = this@PrefetchMetrics.lastUsedAverage
        return if (lastUsedContentType === contentType && lastUsedAverage != null) {
            lastUsedAverage
        } else {
            averagesByContentType
                .getOrPut(contentType) { Averages() }
                .also {
                    this.lastUsedContentType = contentType
                    this.lastUsedAverage = it
                }
        }
    }

    private val averagesByContentType = mutableScatterMapOf<Any?, Averages>()

    private var lastUsedContentType: Any? = null
    private var lastUsedAverage: Averages? = null
}

internal class Averages {
    /** Average time the full composition phase has taken. */
    var compositionTimeNanos: Long = 0L
    /** Average time needed to resume the pausable composition until the next interruption. */
    var resumeTimeNanos: Long = 0L
    /** Average time needed to pause the pausable composition. */
    var pauseTimeNanos: Long = 0L
    /** Average time the apply phase has taken. */
    var applyTimeNanos: Long = 0L
    /** Average time the measure phase has taken. */
    var measureTimeNanos: Long = 0L

    fun saveCompositionTimeNanos(timeNanos: Long) {
        compositionTimeNanos = calculateAverageTime(timeNanos, compositionTimeNanos)
    }

    fun saveResumeTimeNanos(timeNanos: Long) {
        resumeTimeNanos = calculateAverageTime(timeNanos, resumeTimeNanos)
    }

    fun savePauseTimeNanos(timeNanos: Long) {
        pauseTimeNanos = calculateAverageTime(timeNanos, pauseTimeNanos)
    }

    fun saveApplyTimeNanos(timeNanos: Long) {
        applyTimeNanos = calculateAverageTime(timeNanos, applyTimeNanos)
    }

    fun saveMeasureTimeNanos(timeNanos: Long) {
        measureTimeNanos = calculateAverageTime(timeNanos, measureTimeNanos)
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
}

@ExperimentalFoundationApi
private object DummyHandle : PrefetchHandle {
    override fun cancel() {}

    override fun markAsUrgent() {}
}

/**
 * PrefetchHandleProvider is used to connect the [LazyLayoutPrefetchState], which provides the API
 * to schedule prefetches, to a [LazyLayoutItemContentFactory] which resolves key and content from
 * an index, [SubcomposeLayoutState] which knows how to precompose/premeasure, and a specific
 * [PrefetchScheduler] used to execute a request.
 */
@ExperimentalFoundationApi
internal class PrefetchHandleProvider(
    private val itemContentFactory: LazyLayoutItemContentFactory,
    private val subcomposeLayoutState: SubcomposeLayoutState,
    private val executor: PrefetchScheduler,
) {
    fun schedulePrecomposition(
        index: Int,
        prefetchMetrics: PrefetchMetrics,
    ): PrefetchHandle =
        HandleAndRequestImpl(index, prefetchMetrics, null).also {
            executor.schedulePrefetch(it)
            traceValue("compose:lazy:schedule_prefetch:index", index.toLong())
        }

    fun schedulePremeasure(
        index: Int,
        constraints: Constraints,
        prefetchMetrics: PrefetchMetrics,
        onItemPremeasured: (LazyLayoutPrefetchResultScope.() -> Unit)?
    ): PrefetchHandle =
        HandleAndRequestImpl(index, constraints, prefetchMetrics, onItemPremeasured).also {
            executor.schedulePrefetch(it)
            traceValue("compose:lazy:schedule_prefetch:index", index.toLong())
        }

    fun createNestedPrefetchRequest(
        index: Int,
        constraints: Constraints,
        prefetchMetrics: PrefetchMetrics,
    ): PrefetchRequest =
        HandleAndRequestImpl(index, constraints = constraints, prefetchMetrics, null)

    fun createNestedPrefetchRequest(
        index: Int,
        prefetchMetrics: PrefetchMetrics,
    ): PrefetchRequest = HandleAndRequestImpl(index, prefetchMetrics, null)

    @ExperimentalFoundationApi
    private inner class HandleAndRequestImpl(
        override val index: Int,
        private val prefetchMetrics: PrefetchMetrics,
        private val onItemPremeasured: (LazyLayoutPrefetchResultScope.() -> Unit)?
    ) : PrefetchHandle, PrefetchRequest, LazyLayoutPrefetchResultScope {

        constructor(
            index: Int,
            constraints: Constraints,
            prefetchMetrics: PrefetchMetrics,
            onItemPremeasured: (LazyLayoutPrefetchResultScope.() -> Unit)?
        ) : this(index, prefetchMetrics, onItemPremeasured) {
            premeasureConstraints = constraints
        }

        private var premeasureConstraints: Constraints? = null
        private var precomposeHandle: SubcomposeLayoutState.PrecomposedSlotHandle? = null
        private var pausedPrecomposition: SubcomposeLayoutState.PausedPrecomposition? = null
        private var isMeasured = false
        private var isCanceled = false
        private var isComposed = false

        private var hasResolvedNestedPrefetches = false
        private var nestedPrefetchController: NestedPrefetchController? = null
        private var isUrgent = false

        override fun cancel() {
            if (!isCanceled) {
                isCanceled = true
                precomposeHandle?.dispose()
                precomposeHandle = null
            }
        }

        override fun markAsUrgent() {
            isUrgent = true
        }

        override val placeablesCount: Int
            get() = (precomposeHandle?.placeablesCount ?: 0)

        override fun getSize(placeableIndex: Int): IntSize {
            return (precomposeHandle?.getSize(placeableIndex) ?: IntSize.Zero)
        }

        private fun shouldExecute(available: Long, average: Long): Boolean {
            // even for urgent request we only do the work if we have time available, as otherwise
            // it is better to just return early to allow the next frame to start and do the work.
            return (isUrgent && available > 0) || average < available
        }

        private var availableTimeNanos = 0L
        private var elapsedTimeNanos = 0L
        private var startTime = markNow()

        private fun resetAvailableTimeTo(availableTimeNanos: Long) {
            this.availableTimeNanos = availableTimeNanos
            startTime = markNow()
            elapsedTimeNanos = 0L
            traceValue("compose:lazy:prefetch:available_time_nanos", availableTimeNanos)
        }

        private fun updateElapsedAndAvailableTime() {
            val now = markNow()
            elapsedTimeNanos = (now - startTime).inWholeNanoseconds
            availableTimeNanos -= elapsedTimeNanos
            startTime = now
            traceValue("compose:lazy:prefetch:available_time_nanos", availableTimeNanos)
        }

        override fun PrefetchRequestScope.execute(): Boolean {
            return if (isUrgent) {
                    trace("compose:lazy:prefetch:execute:urgent") { executeRequest() }
                } else {
                    executeRequest()
                }
                .also {
                    // execution for this item finished, reset the trace value
                    traceValue("compose:lazy:prefetch:execute:item", -1)
                }
        }

        private fun PrefetchRequestScope.executeRequest(): Boolean {
            traceValue("compose:lazy:prefetch:execute:item", index.toLong())
            val itemProvider = itemContentFactory.itemProvider()
            val isValid = !isCanceled && index in 0 until itemProvider.itemCount
            if (!isValid) {
                return false
            }

            val contentType = itemProvider.getContentType(index)
            val average = prefetchMetrics.getAverage(contentType)

            // we save the value we get from availableTimeNanos() into a local variable once
            // and manually update it later by calling updateElapsedAndAvailableTime()
            resetAvailableTimeTo(availableTimeNanos())
            if (!isComposed) {
                if (ComposeFoundationFlags.isPausableCompositionInPrefetchEnabled) {
                    if (
                        shouldExecute(
                            availableTimeNanos,
                            average.resumeTimeNanos + average.pauseTimeNanos
                        )
                    ) {
                        trace("compose:lazy:prefetch:compose") {
                            performPausableComposition(itemProvider, contentType, average)
                        }
                    }
                } else {
                    if (shouldExecute(availableTimeNanos, average.compositionTimeNanos)) {
                        trace("compose:lazy:prefetch:compose") {
                            performFullComposition(itemProvider, contentType)
                        }
                        updateElapsedAndAvailableTime()
                        average.saveCompositionTimeNanos(elapsedTimeNanos)
                    }
                }
                if (!isComposed) {
                    return true
                }
            }

            if (pausedPrecomposition != null) {
                if (shouldExecute(availableTimeNanos, average.applyTimeNanos)) {
                    trace("compose:lazy:prefetch:apply") { performApply() }
                    updateElapsedAndAvailableTime()
                    average.saveApplyTimeNanos(elapsedTimeNanos)
                } else {
                    return true
                }
            }

            // if the request is urgent we better proceed with the measuring straight away instead
            // of spending time trying to split the work more via nested prefetch. nested prefetch
            // is always an estimation and it could potentially do work we will not need in the end,
            // but the measuring will only do exactly the needed work (including composing nested
            // lazy layouts)
            if (!isUrgent) {
                // Nested prefetch logic is best-effort: if nested LazyLayout children are
                // added/removed/updated after we've resolved nested prefetch states here or
                // resolved
                // nestedPrefetchRequests below, those changes won't be taken into account.
                if (!hasResolvedNestedPrefetches) {
                    if (availableTimeNanos > 0) {
                        trace("compose:lazy:prefetch:resolve-nested") {
                            nestedPrefetchController = resolveNestedPrefetchStates()
                            hasResolvedNestedPrefetches = true
                        }
                    } else {
                        return true
                    }
                }
                val hasMoreWork =
                    nestedPrefetchController?.run { executeNestedPrefetches() } ?: false
                if (hasMoreWork) {
                    return true
                }
                updateElapsedAndAvailableTime()
                // set the item value again since it will have changed in the nested block.
                traceValue("compose:lazy:prefetch:execute:item", index.toLong())
            }

            val constraints = premeasureConstraints
            if (!isMeasured && constraints != null) {
                if (shouldExecute(availableTimeNanos, average.measureTimeNanos)) {
                    trace("compose:lazy:prefetch:measure") { performMeasure(constraints) }
                    updateElapsedAndAvailableTime()
                    average.saveMeasureTimeNanos(elapsedTimeNanos)
                    onItemPremeasured?.invoke(this@HandleAndRequestImpl)
                } else {
                    return true
                }
            }

            // All our work is done.
            return false
        }

        private var pauseRequested = false

        private fun performPausableComposition(
            itemProvider: LazyLayoutItemProvider,
            contentType: Any?,
            averages: Averages
        ) {
            val composition =
                pausedPrecomposition
                    ?: run {
                        val key = itemProvider.getKey(index)
                        val content = itemContentFactory.getContent(index, key, contentType)
                        subcomposeLayoutState.createPausedPrecomposition(key, content).also {
                            pausedPrecomposition = it
                        }
                    }

            pauseRequested = false

            composition.resume {
                if (!pauseRequested) {
                    updateElapsedAndAvailableTime()
                    averages.saveResumeTimeNanos(elapsedTimeNanos)
                    pauseRequested =
                        !shouldExecute(
                            availableTimeNanos,
                            averages.resumeTimeNanos + averages.pauseTimeNanos
                        )
                }
                pauseRequested
            }

            updateElapsedAndAvailableTime()
            if (pauseRequested) {
                averages.savePauseTimeNanos(elapsedTimeNanos)
            } else {
                averages.saveResumeTimeNanos(elapsedTimeNanos)
            }

            isComposed = composition.isComplete
        }

        private fun performFullComposition(
            itemProvider: LazyLayoutItemProvider,
            contentType: Any?
        ) {
            requirePrecondition(precomposeHandle == null) { "Request was already composed!" }
            val key = itemProvider.getKey(index)
            val content = itemContentFactory.getContent(index, key, contentType)
            precomposeHandle = subcomposeLayoutState.precompose(key, content)
            isComposed = true
        }

        private fun performApply() {
            val precomposition = requireNotNull(pausedPrecomposition) { "Nothing to apply!" }
            precomposeHandle = precomposition.apply()
            pausedPrecomposition = null
        }

        private fun performMeasure(constraints: Constraints) {
            requirePrecondition(!isCanceled) {
                "Callers should check whether the request is still valid before calling " +
                    "performMeasure()"
            }
            requirePrecondition(!isMeasured) { "Request was already measured!" }
            isMeasured = true
            val handle =
                requirePreconditionNotNull(precomposeHandle) {
                    "performComposition() must be called before performMeasure()"
                }
            repeat(handle.placeablesCount) { placeableIndex ->
                handle.premeasure(placeableIndex, constraints)
            }
        }

        private fun resolveNestedPrefetchStates(): NestedPrefetchController? {
            val precomposedSlotHandle =
                requirePreconditionNotNull(precomposeHandle) {
                    "Should precompose before resolving nested prefetch states"
                }

            var nestedStates: MutableList<LazyLayoutPrefetchState>? = null
            precomposedSlotHandle.traverseDescendants(TraversablePrefetchStateNodeKey) {
                val prefetchState = (it as TraversablePrefetchStateNode).prefetchState
                nestedStates =
                    nestedStates?.apply { add(prefetchState) } ?: mutableListOf(prefetchState)
                TraverseDescendantsAction.SkipSubtreeAndContinueTraversal
            }
            return nestedStates?.let { NestedPrefetchController(it) }
        }

        override fun toString(): String =
            "HandleAndRequestImpl { index = $index, constraints = $premeasureConstraints, " +
                "isComposed = $isComposed, isMeasured = $isMeasured, isCanceled = $isCanceled }"

        private inner class NestedPrefetchController(
            private val states: List<LazyLayoutPrefetchState>
        ) {

            // This array is parallel to nestedPrefetchStates, so index 0 in nestedPrefetchStates
            // corresponds to index 0 in this array, etc.
            private val requestsByState: Array<List<PrefetchRequest>?> = arrayOfNulls(states.size)
            private var stateIndex: Int = 0
            private var requestIndex: Int = 0

            init {
                requirePrecondition(states.isNotEmpty()) {
                    "NestedPrefetchController shouldn't be created with no states"
                }
            }

            fun PrefetchRequestScope.executeNestedPrefetches(): Boolean {
                if (stateIndex >= states.size) {
                    return false
                }
                checkPrecondition(!isCanceled) {
                    "Should not execute nested prefetch on canceled request"
                }

                trace("compose:lazy:prefetch:nested") {
                    while (stateIndex < states.size) {
                        if (requestsByState[stateIndex] == null) {
                            if (availableTimeNanos() <= 0) {
                                // When we have time again, we'll resolve nested requests for this
                                // state
                                return true
                            }

                            requestsByState[stateIndex] =
                                states[stateIndex].collectNestedPrefetchRequests()
                        }

                        val nestedRequests = requestsByState[stateIndex]!!
                        while (requestIndex < nestedRequests.size) {
                            val hasMoreWork = with(nestedRequests[requestIndex]) { execute() }
                            if (hasMoreWork) {
                                return true
                            } else {
                                requestIndex++
                            }
                        }

                        requestIndex = 0
                        stateIndex++
                    }
                }

                return false
            }
        }
    }
}

private const val TraversablePrefetchStateNodeKey =
    "androidx.compose.foundation.lazy.layout.TraversablePrefetchStateNode"

/**
 * A modifier which lets the [LazyLayoutPrefetchState] for a [LazyLayout] to be discoverable via
 * [TraversableNode] traversal.
 */
@ExperimentalFoundationApi
internal fun Modifier.traversablePrefetchState(
    lazyLayoutPrefetchState: LazyLayoutPrefetchState?
): Modifier {
    return lazyLayoutPrefetchState?.let { this then TraversablePrefetchStateModifierElement(it) }
        ?: this
}

@ExperimentalFoundationApi
private class TraversablePrefetchStateNode(
    var prefetchState: LazyLayoutPrefetchState,
) : Modifier.Node(), TraversableNode {

    override val traverseKey: String = TraversablePrefetchStateNodeKey
}

@ExperimentalFoundationApi
private data class TraversablePrefetchStateModifierElement(
    private val prefetchState: LazyLayoutPrefetchState,
) : ModifierNodeElement<TraversablePrefetchStateNode>() {
    override fun create() = TraversablePrefetchStateNode(prefetchState)

    override fun update(node: TraversablePrefetchStateNode) {
        node.prefetchState = prefetchState
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "traversablePrefetchState"
        value = prefetchState
    }
}

private val ZeroConstraints = Constraints(maxWidth = 0, maxHeight = 0)
