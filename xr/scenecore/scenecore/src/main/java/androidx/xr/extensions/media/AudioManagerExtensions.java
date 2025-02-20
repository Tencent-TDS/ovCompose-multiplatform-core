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

import android.media.AudioManager;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/** Provides spatial audio extensions on the framework {@link AudioManagerExtensions} class. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface AudioManagerExtensions {

    /**
     * Play a spatialized sound effect for sound sources that will be rendered in 3D space as a
     * point source.
     *
     * @param audioManager The {@link AudioManager} to use to play the sound effect.
     * @param effectType The type of sound effect.
     * @param attributes attributes to specify sound source in 3D. {@link PointSourceAttributes}.
     */
    default void playSoundEffectAsPointSource(
            @NonNull AudioManager audioManager,
            int effectType,
            @NonNull PointSourceAttributes attributes) {
        throw new UnsupportedOperationException(IMAGE_TOO_OLD);
    }
}
