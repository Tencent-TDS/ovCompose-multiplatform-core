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

package androidx.xr.compose.spatial

import androidx.annotation.RestrictTo
import androidx.compose.ui.unit.dp
import androidx.xr.compose.unit.toMeter

/**
 * Represents the resting elevation level for spatial elements.
 *
 * Elevation levels range from `Level0` (no elevation) to `Level5` (highest allowed elevation).
 *
 * NOTE: Level0 is not visually distinguishable from base-level content but is present to support
 * smooth transitioning between elevation levels.
 *
 * Values are expressed in meters for consistency with spatial positioning.
 */
@JvmInline
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public value class SpatialElevationLevel internal constructor(public val level: Float) {
    public companion object {
        internal val ActivityDefault = SpatialElevationLevel(0f)
        public val Level0: SpatialElevationLevel = SpatialElevationLevel(0.1.dp.toMeter().value)
        public val Level1: SpatialElevationLevel = SpatialElevationLevel(15.dp.toMeter().value)
        public val Level2: SpatialElevationLevel = SpatialElevationLevel(30.dp.toMeter().value)
        public val Level3: SpatialElevationLevel = SpatialElevationLevel(60.dp.toMeter().value)
        public val Level4: SpatialElevationLevel = SpatialElevationLevel(90.dp.toMeter().value)
        public val Level5: SpatialElevationLevel = SpatialElevationLevel(120.dp.toMeter().value)
        internal val DialogDefault = SpatialElevationLevel(125.dp.toMeter().value)
    }
}
