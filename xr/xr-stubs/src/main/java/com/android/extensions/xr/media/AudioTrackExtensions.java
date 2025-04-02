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
 * Provides spatial audio extensions on the framework {@link android.media.AudioTrack AudioTrack}
 * class.
 */
@SuppressWarnings({"unchecked", "deprecation", "all"})
public class AudioTrackExtensions {

    AudioTrackExtensions() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Gets the {@link com.android.extensions.xr.media.PointSourceAttributes PointSourceAttributes}
     * of the provided {@link android.media.AudioTrack AudioTrack}.
     *
     * @param track The {@link android.media.AudioTrack AudioTrack} from which to get the {@link
     *     com.android.extensions.xr.media.PointSourceAttributes PointSourceAttributes}.
     * @return The {@link com.android.extensions.xr.media.PointSourceAttributes
     *     PointSourceAttributes} of the provided track, null if not set.
     */
    public com.android.extensions.xr.media.PointSourceAttributes getPointSourceAttributes(
            android.media.AudioTrack track) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Gets the {@link com.android.extensions.xr.media.SoundFieldAttributes SoundFieldAttributes} of
     * the provided {@link android.media.AudioTrack AudioTrack}.
     *
     * @param track The {@link android.media.AudioTrack AudioTrack} from which to get the {@link
     *     com.android.extensions.xr.media.SoundFieldAttributes SoundFieldAttributes}.
     * @return The {@link com.android.extensions.xr.media.SoundFieldAttributes SoundFieldAttributes}
     *     of the provided track, null if not set.
     */
    public com.android.extensions.xr.media.SoundFieldAttributes getSoundFieldAttributes(
            android.media.AudioTrack track) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Gets the {@link SourceType} of the provided {@link android.media.AudioTrack AudioTrack}. This
     * value is implicitly set depending one which type of attributes was used to configure the
     * builder. Will return {@link SOURCE_TYPE_BYPASS} for tracks that didn't use spatial audio
     * attributes.
     *
     * @param track The {@link android.media.AudioTrack AudioTrack} from which to get the {@link
     *     SourceType}.
     * @return The {@link SourceType} of the provided track.
     */
    public int getSpatialSourceType(android.media.AudioTrack track) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Sets the {@link com.android.extensions.xr.media.PointSourceAttributes PointSourceAttributes}
     * on the provided {@link android.media.AudioTrack.Builder AudioTrack.Builder}.
     *
     * @param builder The Builder on which to set the attributes.
     * @param attributes The source attributes to be set.
     * @return The same {AudioTrack.Builder} instance provided.
     */
    public android.media.AudioTrack.Builder setPointSourceAttributes(
            android.media.AudioTrack.Builder builder,
            com.android.extensions.xr.media.PointSourceAttributes attributes) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Sets the {@link com.android.extensions.xr.media.SoundFieldAttributes SoundFieldAttributes} on
     * the provided {@link android.media.AudioTrack.Builder AudioTrack.Builder}.
     *
     * @param builder The Builder on which to set the attributes.
     * @param attributes The sound field attributes to be set.
     * @return The same {AudioTrack.Builder} instance provided.
     */
    public android.media.AudioTrack.Builder setSoundFieldAttributes(
            android.media.AudioTrack.Builder builder,
            com.android.extensions.xr.media.SoundFieldAttributes attributes) {
        throw new RuntimeException("Stub!");
    }
}
