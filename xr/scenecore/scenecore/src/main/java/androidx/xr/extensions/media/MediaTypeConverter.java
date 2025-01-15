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
import androidx.annotation.RestrictTo;

/** This class is able to convert library types into platform types. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class MediaTypeConverter {

    private MediaTypeConverter() {}

    /**
     * Converts a {@link com.android.extensions.xr.media.XrSpatialAudioExtensions} to a library
     * type.
     *
     * @param extensions The {@link com.android.extensions.xr.media.XrSpatialAudioExtensions} to
     *     convert.
     * @return The library type of the {@link XrSpatialAudioExtensions}.
     */
    @NonNull
    public static XrSpatialAudioExtensions toLibrary(
            @NonNull com.android.extensions.xr.media.XrSpatialAudioExtensions extensions) {
        requireNonNull(extensions);

        return new XrSpatialAudioExtensionsImpl(extensions);
    }
}
