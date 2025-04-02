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

package androidx.xr.scenecore.impl;

import android.media.SoundPool;

import androidx.xr.scenecore.JxrPlatformAdapter.PointSourceAttributes;
import androidx.xr.scenecore.JxrPlatformAdapter.SoundFieldAttributes;
import androidx.xr.scenecore.JxrPlatformAdapter.SoundPoolExtensionsWrapper;

import com.android.extensions.xr.media.SoundPoolExtensions;

/** Implementation of {@link SoundPoolExtensionsWrapper}. */
final class SoundPoolExtensionsWrapperImpl implements SoundPoolExtensionsWrapper {

    private final SoundPoolExtensions mExtensions;

    SoundPoolExtensionsWrapperImpl(SoundPoolExtensions extensions) {
        mExtensions = extensions;
    }

    @Override
    public int play(
            SoundPool soundPool,
            int soundId,
            PointSourceAttributes attributes,
            float volume,
            int priority,
            int loop,
            float rate) {
        com.android.extensions.xr.media.PointSourceAttributes extAttributes =
                MediaUtils.convertPointSourceAttributesToExtensions(attributes);
        return mExtensions.playAsPointSource(
                soundPool, soundId, extAttributes, volume, priority, loop, rate);
    }

    @Override
    public int play(
            SoundPool soundPool,
            int soundId,
            SoundFieldAttributes attributes,
            float volume,
            int priority,
            int loop,
            float rate) {
        com.android.extensions.xr.media.SoundFieldAttributes extAttributes =
                MediaUtils.convertSoundFieldAttributesToExtensions(attributes);

        return mExtensions.playAsSoundField(
                soundPool, soundId, extAttributes, volume, priority, loop, rate);
    }

    @Override
    public int getSpatialSourceType(SoundPool soundPool, int streamId) {
        return MediaUtils.convertExtensionsToSourceType(
                mExtensions.getSpatialSourceType(soundPool, streamId));
    }
}
