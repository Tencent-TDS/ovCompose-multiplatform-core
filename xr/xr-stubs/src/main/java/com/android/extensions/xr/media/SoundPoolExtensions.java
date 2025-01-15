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

package com.android.extensions.xr.media;

import androidx.annotation.RestrictTo;

/**
 * Provides spatial audio extensions on the framework {@link android.media.SoundPool SoundPool}
 * class.
 */
@SuppressWarnings({"unchecked", "deprecation", "all"})
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class SoundPoolExtensions {

    SoundPoolExtensions() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Plays a spatialized sound effect emitted relative {@link Node} in the {@link
     * com.android.extensions.xr.media.PointSourceAttributes PointSourceAttributes}.
     *
     * @param soundPool The {@link android.media.SoundPool SoundPool} to use to the play the sound.
     * @param soundID a soundId returned by the load() function.
     * @param attributes attributes to specify sound source. {@link
     *     com.android.extensions.xr.media.PointSourceAttributes PointSourceAttributes}
     * @param volume volume value (range = 0.0 to 1.0)
     * @param priority stream priority (0 = lowest priority)
     * @param loop loop mode (0 = no loop, -1 = loop forever)
     * @param rate playback rate (1.0 = normal playback, range 0.5 to 2.0)
     * @return non-zero streamID if successful, zero if failed
     */
    public int playAsPointSource(
            android.media.SoundPool soundPool,
            int soundID,
            com.android.extensions.xr.media.PointSourceAttributes attributes,
            float volume,
            int priority,
            int loop,
            float rate) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Plays a spatialized sound effect as a sound field.
     *
     * @param soundPool The {@link android.media.SoundPool SoundPool} to use to the play the sound.
     * @param soundID a soundId returned by the load() function.
     * @param attributes attributes to specify sound source. {@link
     *     com.android.extensions.xr.media.SoundFieldAttributes SoundFieldAttributes}
     * @param volume volume value (range = 0.0 to 1.0)
     * @param priority stream priority (0 = lowest priority)
     * @param loop loop mode (0 = no loop, -1 = loop forever)
     * @param rate playback rate (1.0 = normal playback, range 0.5 to 2.0)
     * @return non-zero streamID if successful, zero if failed
     */
    public int playAsSoundField(
            android.media.SoundPool soundPool,
            int soundID,
            com.android.extensions.xr.media.SoundFieldAttributes attributes,
            float volume,
            int priority,
            int loop,
            float rate) {
        throw new RuntimeException("Stub!");
    }

    /**
     * @param soundPool The {@link android.media.SoundPool SoundPool} to use to get its SourceType.
     * @param streamID a streamID returned by the play(), playAsPointSource(), or
     *     playAsSoundField().
     * @return The {@link com.android.extensions.xr.media.SpatializerExtensions.SourceType
     *     SpatializerExtensions.SourceType} for the given streamID.
     */
    public int getSpatialSourceType(android.media.SoundPool soundPool, int streamID) {
        throw new RuntimeException("Stub!");
    }
}
