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

import androidx.annotation.RestrictTo;

/** Extensions of the existing {@link Spatializer} class. */
@SuppressWarnings({"unchecked", "deprecation", "all"})
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class SpatializerExtensions {

    SpatializerExtensions() {
        throw new RuntimeException("Stub!");
    }

    /** Specifies spatial rendering using First Order Ambisonics */
    public static final int AMBISONICS_ORDER_FIRST_ORDER = 0; // 0x0

    /** Specifies spatial rendering using Second Order Ambisonics */
    public static final int AMBISONICS_ORDER_SECOND_ORDER = 1; // 0x1

    /** Specifies spatial rendering using Third Order Ambisonics */
    public static final int AMBISONICS_ORDER_THIRD_ORDER = 2; // 0x2

    /** The sound source has not been spatialized with the Spatial Audio SDK. */
    public static final int SOURCE_TYPE_BYPASS = 0; // 0x0

    /** The sound source has been spatialized as a 3D point source. */
    public static final int SOURCE_TYPE_POINT_SOURCE = 1; // 0x1

    /** The sound source is an ambisonics sound field. */
    public static final int SOURCE_TYPE_SOUND_FIELD = 2; // 0x2
}
