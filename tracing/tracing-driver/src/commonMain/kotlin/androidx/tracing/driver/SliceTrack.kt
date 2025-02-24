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

import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.withContext

/** Entities that we can attach traces to to be visualized on the timeline with slices & flows. */
public abstract class SliceTrack(
    /** The [TraceContext] instance. */
    context: TraceContext,
    /** The uuid for the track descriptor. */
    uuid: Long
) : Track(context = context, uuid = uuid) {
    public open fun beginSection(name: String, flowIds: List<Long>) {
        if (context.isEnabled) {
            emitTraceEvent { event -> event.setBeginSectionWithFlows(uuid, name, flowIds) }
        }
    }

    public open fun beginSection(name: String) {
        if (context.isEnabled) {
            emitTraceEvent { event -> event.setBeginSection(uuid, name) }
        }
    }

    public open fun endSection() {
        if (context.isEnabled) {
            emitTraceEvent { event -> event.setEndSection(uuid) }
        }
    }

    public open fun instant(name: String) {
        if (context.isEnabled) {
            emitTraceEvent { event -> event.setInstant(uuid, name) }
        }
    }

    public inline fun <T> trace(name: String, crossinline block: () -> T): T {
        beginSection(name)
        try {
            return block()
        } finally {
            endSection()
        }
    }

    /** [Track] scoped async trace slices. */
    public suspend fun <T> traceFlow(
        name: String,
        flowId: Long = monotonicId(),
        block: suspend () -> T
    ): T {
        return if (!context.isEnabled) {
            block()
        } else {
            traceFlow(name = name, flowIds = listOf(flowId), block = block)
        }
    }

    /** [Track] scoped async trace slices. */
    private suspend fun <T, R : Track> R.traceFlow(
        name: String,
        flowIds: List<Long>,
        block: suspend () -> T
    ): T {
        val element = obtainFlowContext()
        val newFlowIds =
            if (element == null) {
                flowIds
            } else {
                element.flowIds + flowIds
            }
        val newElement = FlowContextElement(flowIds = newFlowIds)
        return withContext(coroutineContext + newElement) {
            beginSection(name = name, flowIds = newFlowIds)
            try {
                block()
            } finally {
                endSection()
            }
        }
    }
}
