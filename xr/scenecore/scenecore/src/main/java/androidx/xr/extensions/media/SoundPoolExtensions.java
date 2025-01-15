/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.xr.extensions.media;

import static androidx.xr.extensions.XrExtensions.IMAGE_TOO_OLD;

import android.media.SoundPool;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/** Provides spatial audio extensions on the framework {@link SoundPool} class. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface SoundPoolExtensions {
    /**
     * Plays a spatialized sound effect emitted relative {@link Node} in the {@link
     * PointSourceAttributes}.
     *
     * @param soundPool The {@link SoundPool} to use to the play the sound.
     * @param soundID a soundId returned by the load() function.
     * @param attributes attributes to specify sound source. {@link PointSourceAttributes}
     * @param volume volume value (range = 0.0 to 1.0)
     * @param priority stream priority (0 = lowest priority)
     * @param loop loop mode (0 = no loop, -1 = loop forever)
     * @param rate playback rate (1.0 = normal playback, range 0.5 to 2.0)
     * @return non-zero streamID if successful, zero if failed
     */
    default int playAsPointSource(
            @NonNull SoundPool soundPool,
            int soundID,
            @NonNull PointSourceAttributes attributes,
            float volume,
            int priority,
            int loop,
            float rate) {
        throw new UnsupportedOperationException(IMAGE_TOO_OLD);
    }

    /**
     * Plays a spatialized sound effect as a sound field.
     *
     * @param soundPool The {@link SoundPool} to use to the play the sound.
     * @param soundID a soundId returned by the load() function.
     * @param attributes attributes to specify sound source. {@link SoundFieldAttributes}
     * @param volume volume value (range = 0.0 to 1.0)
     * @param priority stream priority (0 = lowest priority)
     * @param loop loop mode (0 = no loop, -1 = loop forever)
     * @param rate playback rate (1.0 = normal playback, range 0.5 to 2.0)
     * @return non-zero streamID if successful, zero if failed
     */
    default int playAsSoundField(
            @NonNull SoundPool soundPool,
            int soundID,
            @NonNull SoundFieldAttributes attributes,
            float volume,
            int priority,
            int loop,
            float rate) {
        throw new UnsupportedOperationException(IMAGE_TOO_OLD);
    }

    /**
     * @param soundPool The {@link SoundPool} to use to get its SourceType.
     * @param streamID a streamID returned by the play(), playAsPointSource(), or
     *     playAsSoundField().
     * @return The {@link SpatializerExtensions.SourceType} for the given streamID.
     */
    default @SpatializerExtensions.SourceType int getSpatialSourceType(
            @NonNull SoundPool soundPool, int streamID) {
        throw new UnsupportedOperationException(IMAGE_TOO_OLD);
    }
}
