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

import androidx.annotation.NonNull;

/** Provides new functionality of existing framework APIs needed to spatialize audio sources. */
class XrSpatialAudioExtensionsImpl implements XrSpatialAudioExtensions {
    @NonNull final com.android.extensions.xr.media.XrSpatialAudioExtensions mExtensions;

    @NonNull private final SoundPoolExtensionsImpl mSoundPoolExtensions;
    @NonNull private final AudioTrackExtensionsImpl mAudioTrackExtensions;
    @NonNull private final AudioManagerExtensionsImpl mAudioManagerExtensions;
    @NonNull private final MediaPlayerExtensionsImpl mMediaPlayerExtensions;

    XrSpatialAudioExtensionsImpl(
            @NonNull com.android.extensions.xr.media.XrSpatialAudioExtensions extensions) {
        requireNonNull(extensions);
        mExtensions = extensions;

        mSoundPoolExtensions = new SoundPoolExtensionsImpl(mExtensions.getSoundPoolExtensions());
        mAudioTrackExtensions = new AudioTrackExtensionsImpl(mExtensions.getAudioTrackExtensions());
        mAudioManagerExtensions =
                new AudioManagerExtensionsImpl(mExtensions.getAudioManagerExtensions());
        mMediaPlayerExtensions =
                new MediaPlayerExtensionsImpl(mExtensions.getMediaPlayerExtensions());
    }

    @Override
    @NonNull
    public SoundPoolExtensions getSoundPoolExtensions() {
        return mSoundPoolExtensions;
    }

    @Override
    @NonNull
    public AudioTrackExtensions getAudioTrackExtensions() {
        return mAudioTrackExtensions;
    }

    @Override
    @NonNull
    public AudioManagerExtensions getAudioManagerExtensions() {
        return mAudioManagerExtensions;
    }

    @Override
    @NonNull
    public MediaPlayerExtensions getMediaPlayerExtensions() {
        return mMediaPlayerExtensions;
    }
}
