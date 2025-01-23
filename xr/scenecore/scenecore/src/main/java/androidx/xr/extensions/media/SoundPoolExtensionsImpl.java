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

import static java.util.Objects.requireNonNull;

import android.media.SoundPool;

import androidx.annotation.NonNull;

/** Wraps a {@link com.android.extensions.xr.media.SoundPoolExtensions}. */
class SoundPoolExtensionsImpl implements SoundPoolExtensions {
    @NonNull final com.android.extensions.xr.media.SoundPoolExtensions mSoundPool;

    SoundPoolExtensionsImpl(
            @NonNull com.android.extensions.xr.media.SoundPoolExtensions soundPool) {
        requireNonNull(soundPool);
        mSoundPool = soundPool;
    }

    @Override
    public int playAsPointSource(
            @NonNull SoundPool soundPool,
            int soundID,
            @NonNull PointSourceAttributes attributes,
            float volume,
            int priority,
            int loop,
            float rate) {
        com.android.extensions.xr.media.PointSourceAttributes internal =
                PointSourceAttributesHelper.convertToFramework(attributes);

        return mSoundPool.playAsPointSource(
                soundPool, soundID, internal, volume, priority, loop, rate);
    }

    @Override
    public int playAsSoundField(
            @NonNull SoundPool soundPool,
            int soundID,
            @NonNull SoundFieldAttributes attributes,
            float volume,
            int priority,
            int loop,
            float rate) {
        com.android.extensions.xr.media.SoundFieldAttributes internal =
                SoundFieldAttributesHelper.convertToFramework(attributes);

        return mSoundPool.playAsSoundField(
                soundPool, soundID, internal, volume, priority, loop, rate);
    }

    @Override
    @SpatializerExtensions.SourceType
    public int getSpatialSourceType(@NonNull SoundPool soundPool, int streamID) {
        return mSoundPool.getSpatialSourceType(soundPool, streamID);
    }
}
