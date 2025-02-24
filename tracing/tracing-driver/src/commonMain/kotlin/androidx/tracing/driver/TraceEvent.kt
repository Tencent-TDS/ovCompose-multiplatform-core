/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.tracing.driver

internal const val TRACE_EVENT_TYPE_UNDEFINED: Int = 0
internal const val TRACE_EVENT_TYPE_BEGIN: Int = 1
internal const val TRACE_EVENT_TYPE_END: Int = 2
internal const val TRACE_EVENT_TYPE_INSTANT: Int = 3
internal const val TRACE_EVENT_TYPE_COUNTER: Int = 4

/**
 * Mutable in-memory only representation a trace event, such as a slice start, slice end, or counter
 * update.
 *
 * This structure is optimized for performance, with the expectation that these are created by a
 * trace event, and passed to a background thread to be serialized. Mutability (and thus reuse) is
 * an important component of this.
 *
 * Code outside of tracing-driver implementation should only ever consume these objects, not produce
 * them.
 */
@Suppress("NOTHING_TO_INLINE")
internal class TraceEvent(
    /**
     * Must be one of [TRACE_EVENT_TYPE_UNDEFINED], [TRACE_EVENT_TYPE_INSTANT],
     * [TRACE_EVENT_TYPE_BEGIN], [TRACE_EVENT_TYPE_END], [TRACE_EVENT_TYPE_COUNTER]
     */
    @JvmField var type: Int,
    @JvmField var trackUuid: Long,
    @JvmField var timestamp: Long,
    @JvmField var name: String?,
    @JvmField var counterDoubleValue: Double?,
    @JvmField var counterLongValue: Long?,
    @JvmField var flowIds: List<Long>,
    @JvmField var trackDescriptor: TrackDescriptor?,
) {
    internal constructor() :
        this(
            type = INVALID_INT,
            trackUuid = INVALID_LONG,
            timestamp = INVALID_LONG,
            name = null,
            counterDoubleValue = null,
            counterLongValue = null,
            flowIds = emptyList(),
            trackDescriptor = null
        )

    internal inline fun setPreamble(trackDescriptor: TrackDescriptor) {
        this.trackDescriptor = trackDescriptor
        this.timestamp = nanoTime()
    }

    internal inline fun setBeginSection(trackUuid: Long, name: String) {
        type = TRACE_EVENT_TYPE_BEGIN
        this.trackUuid = trackUuid
        timestamp = nanoTime()
        this.name = name
    }

    internal inline fun setBeginSectionWithFlows(
        trackUuid: Long,
        name: String,
        flowIds: List<Long>
    ) {
        type = TRACE_EVENT_TYPE_BEGIN
        this.trackUuid = trackUuid
        timestamp = nanoTime()
        this.flowIds = flowIds
        this.name = name
    }

    internal inline fun setEndSection(trackUuid: Long) {
        type = TRACE_EVENT_TYPE_END
        this.trackUuid = trackUuid
        timestamp = nanoTime()
    }

    internal inline fun setInstant(
        trackUuid: Long,
        name: String,
    ) {
        type = TRACE_EVENT_TYPE_END
        this.trackUuid = trackUuid
        timestamp = nanoTime()
        this.name = name
    }

    internal inline fun setCounterLong(trackUuid: Long, value: Long) {
        type = TRACE_EVENT_TYPE_COUNTER
        this.trackUuid = trackUuid
        timestamp = nanoTime()
        counterLongValue = value
    }

    internal inline fun setCounterDouble(trackUuid: Long, value: Double) {
        type = TRACE_EVENT_TYPE_COUNTER
        this.trackUuid = trackUuid
        timestamp = nanoTime()
        counterDoubleValue = value
    }

    fun reset() {
        type = INVALID_INT
        trackUuid = INVALID_LONG
        timestamp = INVALID_LONG
        name = null
        counterDoubleValue = null
        counterLongValue = null
        flowIds = emptyList()
        trackDescriptor = null
    }
}
