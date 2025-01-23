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

import android.media.MediaPlayer;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/** Provides spatial audio extensions on the framework {@link MediaPlayer} class. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface MediaPlayerExtensions {

    /**
     * @param mediaPlayer The {@link MediaPlayer} on which to set the attributes.
     * @param attributes The source attributes to be set.
     * @return The same {@link MediaPlayer} instance provided.
     */
    @NonNull
    default MediaPlayer setPointSourceAttributes(
            @NonNull MediaPlayer mediaPlayer, @NonNull PointSourceAttributes attributes) {
        throw new UnsupportedOperationException(IMAGE_TOO_OLD);
    }

    /**
     * Sets the {@link SoundFieldAttributes} on the provided {@link MediaPlayer}.
     *
     * @param mediaPlayer The {@link MediaPlayer} on which to set the attributes.
     * @param attributes The source attributes to be set.
     * @return The same {@link MediaPlayer} instance provided.
     */
    @NonNull
    default MediaPlayer setSoundFieldAttributes(
            @NonNull MediaPlayer mediaPlayer, @NonNull SoundFieldAttributes attributes) {
        throw new UnsupportedOperationException(IMAGE_TOO_OLD);
    }
}
