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

package androidx.health.connect.client.testing

import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.Instant.EPOCH
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

/** Unit tests for [MetadataTestHelper]. */
class MetadataTestHelperTest {
    companion object {
        private const val testId = "test_uuid"
        private val testDataOrigin = DataOrigin("test.package")
        private val testLastModifiedTime = Instant.ofEpochMilli(123456)
    }

    @Test
    fun populatedWithTestValues_allMetadataFieldsSet_copyValues() = runTest {
        val metadata =
            Metadata.activelyRecorded(
                clientRecordId = "clientId",
                clientRecordVersion = 321,
                device =
                    Device(
                        manufacturer = "some company",
                        model = "best model",
                        type = Device.TYPE_PHONE,
                    )
            )
        val updatedMetadata =
            metadata.populatedWithTestValues(
                id = testId,
                dataOrigin = testDataOrigin,
                lastModifiedTime = testLastModifiedTime,
            )

        assertThat(updatedMetadata.id).isEqualTo(testId)
        assertThat(updatedMetadata.dataOrigin).isEqualTo(testDataOrigin)
        assertThat(updatedMetadata.lastModifiedTime).isEqualTo(testLastModifiedTime)

        assertThat(updatedMetadata.recordingMethod).isEqualTo(metadata.recordingMethod)
        assertThat(updatedMetadata.clientRecordId).isEqualTo(metadata.clientRecordId)
        assertThat(updatedMetadata.clientRecordVersion).isEqualTo(metadata.clientRecordVersion)
        assertThat(updatedMetadata.device).isEqualTo(metadata.device)
    }

    @Test
    fun populatedWithTestValues_noMetadataFieldsSet_defaultValues() = runTest {
        val metadata = Metadata.unknownRecordingMethod()
        val updatedMetadata = metadata.populatedWithTestValues()

        assertThat(updatedMetadata.id).isEqualTo("")
        assertThat(updatedMetadata.dataOrigin).isEqualTo(DataOrigin(""))
        assertThat(updatedMetadata.lastModifiedTime).isEqualTo(EPOCH)

        assertThat(updatedMetadata.recordingMethod).isEqualTo(metadata.recordingMethod)
        assertThat(updatedMetadata.clientRecordId).isNull()
        assertThat(updatedMetadata.clientRecordVersion).isEqualTo(0)
        assertThat(updatedMetadata.device).isNull()
    }
}
