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


/** Provides new functionality of existing framework APIs needed to Spatialize audio sources. */
@SuppressWarnings({"unchecked", "deprecation", "all"})
public class XrSpatialAudioExtensions {

    XrSpatialAudioExtensions() {
        throw new RuntimeException("Stub!");
    }

    /**
     * @return {@link com.android.extensions.xr.media.SoundPoolExtensions SoundPoolExtensions}
     *     instance to control spatial audio from a {@link SoundPool}.
     */
    public com.android.extensions.xr.media.SoundPoolExtensions getSoundPoolExtensions() {
        throw new RuntimeException("Stub!");
    }

    /**
     * @return {@link com.android.extensions.xr.media.AudioTrackExtensions AudioTrackExtensions}
     *     instance to control spatial audio from an {@link AudioTrack}.
     */
    public com.android.extensions.xr.media.AudioTrackExtensions getAudioTrackExtensions() {
        throw new RuntimeException("Stub!");
    }

    /**
     * @return {@link com.android.extensions.xr.media.AudioManagerExtensions AudioManagerExtensions}
     *     instance to control spatial audio from an {@link AudioManager}.
     */
    public com.android.extensions.xr.media.AudioManagerExtensions getAudioManagerExtensions() {
        throw new RuntimeException("Stub!");
    }

    /**
     * @return {@link com.android.extensions.xr.media.MediaPlayerExtensions MediaPlayerExtensions}
     *     instance to control spatial audio from a {@link MediaPlayer}.
     */
    public com.android.extensions.xr.media.MediaPlayerExtensions getMediaPlayerExtensions() {
        throw new RuntimeException("Stub!");
    }
}
