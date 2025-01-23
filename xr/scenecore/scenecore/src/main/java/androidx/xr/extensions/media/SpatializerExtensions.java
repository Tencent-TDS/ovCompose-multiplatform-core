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

import static java.lang.annotation.RetentionPolicy.SOURCE;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;

/** Extensions of the existing {@link Spatializer} class. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface SpatializerExtensions {
    /** Used to set the Ambisonics order of a {@link SoundFieldAttributes} */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @IntDef(
            value = {
                AMBISONICS_ORDER_FIRST_ORDER,
                AMBISONICS_ORDER_SECOND_ORDER,
                AMBISONICS_ORDER_THIRD_ORDER,
            })
    @Retention(SOURCE)
    @interface AmbisonicsOrder {}

    /** Specifies spatial rendering using First Order Ambisonics */
    int AMBISONICS_ORDER_FIRST_ORDER = 0;

    /** Specifies spatial rendering using Second Order Ambisonics */
    int AMBISONICS_ORDER_SECOND_ORDER = 1;

    /** Specifies spatial rendering using Third Order Ambisonics */
    int AMBISONICS_ORDER_THIRD_ORDER = 2;

    /** Represents the type of spatialization for an audio source */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @IntDef(
            value = {
                SOURCE_TYPE_BYPASS,
                SOURCE_TYPE_POINT_SOURCE,
                SOURCE_TYPE_SOUND_FIELD,
            })
    @Retention(SOURCE)
    @interface SourceType {}

    /** The sound source has not been spatialized with the Spatial Audio SDK. */
    int SOURCE_TYPE_BYPASS = 0;

    /** The sound source has been spatialized as a 3D point source. */
    int SOURCE_TYPE_POINT_SOURCE = 1;

    /** The sound source is an ambisonics sound field. */
    int SOURCE_TYPE_SOUND_FIELD = 2;
}
