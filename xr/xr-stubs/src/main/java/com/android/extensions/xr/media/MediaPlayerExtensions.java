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


/**
 * Provides spatial audio extensions on the framework {@link android.media.MediaPlayer MediaPlayer}
 * class.
 */
@SuppressWarnings({"unchecked", "deprecation", "all"})
public class MediaPlayerExtensions {

    MediaPlayerExtensions() {
        throw new RuntimeException("Stub!");
    }

    /**
     * @param mediaPlayer The {@link android.media.MediaPlayer MediaPlayer} on which to set the
     *     attributes.
     * @param attributes The source attributes to be set.
     * @return The same {@link android.media.MediaPlayer MediaPlayer} instance provided.
     */
    public android.media.MediaPlayer setPointSourceAttributes(
            android.media.MediaPlayer mediaPlayer,
            com.android.extensions.xr.media.PointSourceAttributes attributes) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Sets the {@link com.android.extensions.xr.media.SoundFieldAttributes SoundFieldAttributes} on
     * the provided {@link android.media.MediaPlayer MediaPlayer}.
     *
     * @param mediaPlayer The {@link android.media.MediaPlayer MediaPlayer} on which to set the
     *     attributes.
     * @param attributes The source attributes to be set.
     * @return The same {@link android.media.MediaPlayer MediaPlayer} instance provided.
     */
    public android.media.MediaPlayer setSoundFieldAttributes(
            android.media.MediaPlayer mediaPlayer,
            com.android.extensions.xr.media.SoundFieldAttributes attributes) {
        throw new RuntimeException("Stub!");
    }
}
