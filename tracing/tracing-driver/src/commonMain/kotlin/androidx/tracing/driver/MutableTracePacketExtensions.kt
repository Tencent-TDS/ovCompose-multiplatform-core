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

/** We inline these extension methods as they're only expected */
@file:Suppress("NOTHING_TO_INLINE")

package androidx.tracing.driver

import perfetto.protos.MutableTracePacket
import perfetto.protos.MutableTrackDescriptor
import perfetto.protos.MutableTrackEvent

internal inline fun MutableTracePacket.reset() {
    if (track_event == null) {
        // This can only be null when a preamble packet is used
        track_event = MutableTrackEvent(track_uuid = INVALID_LONG)
    } else {
        // reset existing track event
        val event = track_event!!
        event.type = null
        event.track_uuid = INVALID_LONG
        event.name = null
        event.counter_value = null
        event.double_counter_value = null
        // Wire creates an `immutableCopyOf(...) the incoming list.
        // Only set the flowIds if it needs to be cleared
        if (event.flow_ids.isNotEmpty()) {
            event.flow_ids = emptyList()
        }
    }
    track_descriptor = null
    timestamp = 0L
    trusted_packet_sequence_id = 0
}

internal inline fun MutableTracePacket.setPreamble(
    track: Track,
    trackDescriptor: MutableTrackDescriptor
) {
    track_descriptor = trackDescriptor
    track_event = null // this is bad, but recovers in reset
    timestamp = nanoTime()
    trusted_packet_sequence_id = track.context.sequenceId
}

internal inline fun MutableTracePacket.setBeginSectionWithFlows(
    trackUuid: Long,
    sequenceId: Int,
    name: String,
    flowIds: List<Long>
) {
    val event = track_event!!
    event.type = MutableTrackEvent.Type.TYPE_SLICE_BEGIN
    event.track_uuid = trackUuid
    event.name = name
    // Wire creates an `immutableCopyOf(...) the incoming list.
    // Only set the flowIds if they exist, or non-empty (and thus may need updating).
    if (flowIds.isNotEmpty()) {
        event.flow_ids = flowIds
    }
    timestamp = nanoTime()
    trusted_packet_sequence_id = sequenceId
}

internal inline fun MutableTracePacket.setBeginSection(
    trackUuid: Long,
    sequenceId: Int,
    name: String
) {
    val event = track_event!!
    event.type = MutableTrackEvent.Type.TYPE_SLICE_BEGIN
    event.track_uuid = trackUuid
    event.name = name
    timestamp = nanoTime()
    trusted_packet_sequence_id = sequenceId
}

internal inline fun MutableTracePacket.setEndSection(trackUuid: Long, sequenceId: Int) {
    val event = track_event!!
    event.type = MutableTrackEvent.Type.TYPE_SLICE_END
    event.track_uuid = trackUuid
    timestamp = nanoTime()
    trusted_packet_sequence_id = sequenceId
}

internal inline fun MutableTracePacket.setInstantEvent(trackUuid: Long, sequenceId: Int) {
    val event = track_event!!
    event.type = MutableTrackEvent.Type.TYPE_INSTANT
    event.track_uuid = trackUuid
    timestamp = nanoTime()
    trusted_packet_sequence_id = sequenceId
}

internal inline fun MutableTracePacket.setLongCounter(
    trackUuid: Long,
    sequenceId: Int,
    value: Long
) {
    val event = track_event!!
    event.type = MutableTrackEvent.Type.TYPE_COUNTER
    event.track_uuid = trackUuid
    event.counter_value = value
    event.name = null
    timestamp = nanoTime()
    trusted_packet_sequence_id = sequenceId
}

internal inline fun MutableTracePacket.setDoubleCounter(
    trackUuid: Long,
    sequenceId: Int,
    value: Double
) {
    val event = track_event!!
    event.type = MutableTrackEvent.Type.TYPE_COUNTER
    event.track_uuid = trackUuid
    event.double_counter_value = value
    event.name = null
    timestamp = nanoTime()
    trusted_packet_sequence_id = sequenceId
}
