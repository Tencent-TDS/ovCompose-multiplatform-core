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

package androidx.xr.compose.unit

import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.scenecore.Dimensions
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DpVolumeSizeTest {
    @Test
    fun dpVolumeSize_isCreated() {
        val dpVolumeSize = DpVolumeSize(0.dp, 0.dp, 0.dp)

        assertNotNull(dpVolumeSize)
    }

    @Test
    fun dpVolumeSize_toString_returnsString() {
        val dpVolumeSize = DpVolumeSize(0.dp, 0.dp, 0.dp)

        val toString = dpVolumeSize.toString()

        assertThat(toString).isEqualTo("DpVolumeSize(width=0.0.dp, height=0.0.dp, depth=0.0.dp)")
    }

    @Test
    fun toDimensionsInMeter_returnsCorrectDimensions() {
        val dpVolumeSize = DpVolumeSize(1151.856f.dp, 1151.856f.dp, 1151.856f.dp)

        val dimensions = dpVolumeSize.toDimensionsInMeters()

        assertThat(dimensions).isEqualTo(Dimensions(1f, 1f, 1f))
    }

    @Test
    fun dpVolumeSize_fromMeters_returnsCorrectDpVolumeSize() {
        val dpVolumeSize = Dimensions(1f, 1f, 1f).toDpVolumeSize()

        assertThat(dpVolumeSize).isEqualTo(DpVolumeSize(1151.856f.dp, 1151.856f.dp, 1151.856f.dp))
    }

    @Test
    fun dpVolumeSize_zero_returnsCorrectDpVolumeSize() {
        val zero = DpVolumeSize.Zero

        assertThat(zero).isEqualTo(DpVolumeSize(0f.dp, 0f.dp, 0f.dp))
    }

    @Test
    fun toDimensionsInMeters_andFromMeters_returnsCorrectDpVolumeSize() {
        val testDpVolumeSize = DpVolumeSize(1111.11f.dp, 1111.11f.dp, 1111.11f.dp)

        val dimensions = testDpVolumeSize.toDimensionsInMeters()
        val fromMetersDpVolumeSize = dimensions.toDpVolumeSize()

        assertThat(fromMetersDpVolumeSize)
            .isEqualTo(DpVolumeSize(1111.11f.dp, 1111.11f.dp, 1111.11f.dp))
    }
}

/**
 * Converts this [DpVolumeSize] to a [Dimensions] object in meters.
 *
 * @return a [Dimensions] object representing the volume size in meters
 */
internal fun DpVolumeSize.toDimensionsInMeters(): Dimensions =
    Dimensions(width.toMeter().value, height.toMeter().value, depth.toMeter().value)

/**
 * Creates a [DpVolumeSize] from a [Dimensions] object in meters.
 *
 * @param dimensions the [Dimensions] object in meters.
 * @return a [DpVolumeSize] object representing the same volume size in Dp.
 */
internal fun Dimensions.toDpVolumeSize(): DpVolumeSize =
    DpVolumeSize(Meter(width).toDp(), Meter(height).toDp(), Meter(depth).toDp())
