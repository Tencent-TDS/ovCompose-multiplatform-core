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

import android.media.AudioTrack;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

/** Provides spatial audio extensions on the framework {@link AudioTrack} class. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface AudioTrackExtensions {
    /**
     * Gets the {@link PointSourceAttributes} of the provided {@link AudioTrack}.
     *
     * @param track The {@link AudioTrack} from which to get the {@link PointSourceAttributes}.
     * @return The {@link PointSourceAttributes} of the provided track, null if not set.
     */
    @Nullable
    default PointSourceAttributes getPointSourceAttributes(@NonNull AudioTrack track) {
        throw new UnsupportedOperationException(IMAGE_TOO_OLD);
    }

    /**
     * Gets the {@link SoundFieldAttributes} of the provided {@link AudioTrack}.
     *
     * @param track The {@link AudioTrack} from which to get the {@link SoundFieldAttributes}.
     * @return The {@link SoundFieldAttributes} of the provided track, null if not set.
     */
    @Nullable
    default SoundFieldAttributes getSoundFieldAttributes(@NonNull AudioTrack track) {
        throw new UnsupportedOperationException(IMAGE_TOO_OLD);
    }

    /**
     * Gets the {@link SourceType} of the provided {@link AudioTrack}. This value is implicitly set
     * depending one which type of attributes was used to configure the builder. Will return {@link
     * SOURCE_TYPE_BYPASS} for tracks that didn't use spatial audio attributes.
     *
     * @param track The {@link AudioTrack} from which to get the {@link SourceType}.
     * @return The {@link SourceType} of the provided track.
     */
    default @SpatializerExtensions.SourceType int getSpatialSourceType(@NonNull AudioTrack track) {
        throw new UnsupportedOperationException(IMAGE_TOO_OLD);
    }

    /**
     * Sets the {@link PointSourceAttributes} on the provided {@link AudioTrack.Builder}.
     *
     * @param builder The Builder on which to set the attributes.
     * @param attributes The source attributes to be set.
     * @return The same {AudioTrack.Builder} instance provided.
     */
    @NonNull
    default AudioTrack.Builder setPointSourceAttributes(
            @NonNull AudioTrack.Builder builder, @NonNull PointSourceAttributes attributes) {
        throw new UnsupportedOperationException(IMAGE_TOO_OLD);
    }

    /**
     * Sets the {@link SoundFieldAttributes} on the provided {@link AudioTrack.Builder}.
     *
     * @param builder The Builder on which to set the attributes.
     * @param attributes The sound field attributes to be set.
     * @return The same {AudioTrack.Builder} instance provided.
     */
    @NonNull
    default AudioTrack.Builder setSoundFieldAttributes(
            @NonNull AudioTrack.Builder builder, @NonNull SoundFieldAttributes attributes) {
        throw new UnsupportedOperationException(IMAGE_TOO_OLD);
    }
}
