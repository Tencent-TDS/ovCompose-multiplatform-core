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

import android.media.AudioTrack;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Wraps a {@link com.android.extensions.xr.media.AudioTrackExtensions}. */
class AudioTrackExtensionsImpl implements AudioTrackExtensions {
    @NonNull final com.android.extensions.xr.media.AudioTrackExtensions mAudioTrack;

    AudioTrackExtensionsImpl(
            @NonNull com.android.extensions.xr.media.AudioTrackExtensions audioTrack) {
        requireNonNull(audioTrack);
        mAudioTrack = audioTrack;
    }

    @Override
    @Nullable
    public PointSourceAttributes getPointSourceAttributes(@NonNull AudioTrack track) {
        return PointSourceAttributesHelper.convertToExtensions(
                mAudioTrack.getPointSourceAttributes(track));
    }

    @Override
    @Nullable
    public SoundFieldAttributes getSoundFieldAttributes(@NonNull AudioTrack track) {
        return SoundFieldAttributesHelper.convertToExtensions(
                mAudioTrack.getSoundFieldAttributes(track));
    }

    @Override
    public @SpatializerExtensions.SourceType int getSpatialSourceType(@NonNull AudioTrack track) {
        return mAudioTrack.getSpatialSourceType(track);
    }

    @Override
    @NonNull
    public AudioTrack.Builder setPointSourceAttributes(
            @NonNull AudioTrack.Builder builder, @NonNull PointSourceAttributes attributes) {
        return mAudioTrack.setPointSourceAttributes(
                builder, PointSourceAttributesHelper.convertToFramework(attributes));
    }

    @Override
    @NonNull
    public AudioTrack.Builder setSoundFieldAttributes(
            @NonNull AudioTrack.Builder builder, @NonNull SoundFieldAttributes attributes) {
        return mAudioTrack.setSoundFieldAttributes(
                builder, SoundFieldAttributesHelper.convertToFramework(attributes));
    }
}
