/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.collection.mutableScatterMapOf
import perfetto.protos.MutableProcessDescriptor
import perfetto.protos.MutableTrackDescriptor

/** Represents a track for a process in a perfetto trace. */
public open class ProcessTrack(
    /** The tracing context. */
    context: TraceContext,
    /** The process id */
    internal val id: Int,
    /** The name of the process. */
    internal val name: String,
) : EventTrack(context = context, uuid = monotonicId()) {
    internal val packetLock = Any()
    internal val threads = mutableScatterMapOf<String, ThreadTrack>()
    internal val counters = mutableScatterMapOf<String, CounterTrack>()

    init {
        emitPacket(immediateDispatch = true) { packet ->
            synchronized(packetLock) {
                packet.setPreamble(
                    this,
                    MutableTrackDescriptor(
                        uuid = uuid,
                        process = MutableProcessDescriptor(id, process_name = name)
                    )
                )
            }
        }
    }

    public override fun beginSection(name: String, flowIds: List<Long>) {
        if (context.isEnabled) {
            synchronized(packetLock) {
                emitPacket { packet ->
                    packet.setBeginSectionWithFlows(uuid, sequenceId, name, flowIds)
                }
            }
        }
    }

    public override fun beginSection(name: String) {
        if (context.isEnabled) {
            synchronized(packetLock) {
                emitPacket { packet -> packet.setBeginSection(uuid, sequenceId, name) }
            }
        }
    }

    public override fun endSection() {
        if (context.isEnabled) {
            synchronized(packetLock) {
                emitPacket { packet -> packet.setEndSection(uuid, sequenceId) }
            }
        }
    }

    public override fun instant() {
        if (context.isEnabled) {
            synchronized(packetLock) {
                emitPacket { packet -> packet.setInstantEvent(uuid, sequenceId) }
            }
        }
    }

    /**
     * @return A [ThreadTrack] for a given [ProcessTrack] using the unique thread [id] and a thread
     *   [name].
     */
    public open fun getOrCreateThreadTrack(id: Int, name: String): ThreadTrack {
        // Thread ids are only unique for lifetime of the thread and can be potentially reused.
        // Therefore we end up combining the `name` of the thread and its `id` as a key.
        val key = "$id/$name"
        return threads[key]
            ?: synchronized(threads) {
                val track =
                    threads.getOrPut(key) { ThreadTrack(id = id, name = name, process = this) }
                check(track.name == name)
                track
            }
    }

    /** @return A [CounterTrack] for a given [ProcessTrack] and the provided counter [name]. */
    public open fun getOrCreateCounterTrack(name: String): CounterTrack {
        return counters[name]
            ?: synchronized(counters) {
                counters.getOrPut(name) { CounterTrack(name = name, parent = this) }
            }
    }
}

// An empty process track when tracing is disabled.

private const val EMPTY_PROCESS_ID = -1
private const val EMPTY_PROCESS_NAME = "Empty Process"

internal class EmptyProcessTrack(context: EmptyTraceContext) :
    ProcessTrack(
        context = context,
        id = EMPTY_PROCESS_ID,
        name = EMPTY_PROCESS_NAME,
    ) {

    private val emptyContext: EmptyTraceContext = context

    override fun getOrCreateThreadTrack(id: Int, name: String): ThreadTrack = emptyContext.thread

    override fun getOrCreateCounterTrack(name: String): CounterTrack = emptyContext.counter
}
