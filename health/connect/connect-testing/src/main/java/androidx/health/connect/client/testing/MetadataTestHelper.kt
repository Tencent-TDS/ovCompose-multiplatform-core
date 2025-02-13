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
@file:JvmName("MetadataTestHelper")

package androidx.health.connect.client.testing

import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.records.metadata.Metadata
import java.time.Instant

/**
 * Creates a new [Metadata] object by copying existing fields and overriding [id], [dataOrigin], and
 * [lastModifiedTime] for testing purposes.
 *
 * This simulates the behavior of the Health Connect implementation, which automatically populates
 * these values during record insertion.
 *
 * @param id The ID to assign to the new [Metadata]. Defaults to an empty string.
 * @param dataOrigin The [DataOrigin] to assign to the new [Metadata]. Defaults to a [DataOrigin]
 *   with an empty package name.
 * @param lastModifiedTime The [Instant] representing the last modified time. Defaults to
 *   [Instant.EPOCH].
 */
@JvmOverloads
public fun Metadata.populatedWithTestValues(
    id: String = "",
    dataOrigin: DataOrigin = DataOrigin(""),
    lastModifiedTime: Instant = Instant.EPOCH,
): Metadata =
    @Suppress("DEPRECATION") // The constructor will become internal
    Metadata(
        recordingMethod = this.recordingMethod,
        id = id,
        dataOrigin = dataOrigin,
        lastModifiedTime = lastModifiedTime,
        clientRecordId = this.clientRecordId,
        clientRecordVersion = this.clientRecordVersion,
        device = this.device,
    )
