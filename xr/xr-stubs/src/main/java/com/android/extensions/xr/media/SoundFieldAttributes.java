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
 * {@link com.android.extensions.xr.media.SoundFieldAttributes SoundFieldAttributes} is used to
 * configure ambisonic sound sources.
 */
@SuppressWarnings({"unchecked", "deprecation", "all"})
public class SoundFieldAttributes {

    SoundFieldAttributes() {
        throw new RuntimeException("Stub!");
    }

    /**
     * @return The {@link com.android.extensions.xr.media.SpatializerExtensions.AmbisonicsOrder
     *     SpatializerExtensions.AmbisonicsOrder} of this sound source.
     */
    public int getAmbisonicsOrder() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Builder class for {@link com.android.extensions.xr.media.SoundFieldAttributes
     * SoundFieldAttributes}
     */
    @SuppressWarnings({"unchecked", "deprecation", "all"})
    public static final class Builder {

        public Builder() {
            throw new RuntimeException("Stub!");
        }

        /**
         * @param ambisonicsOrder Sets the {@link
         *     com.android.extensions.xr.media.SpatializerExtensions.AmbisonicsOrder
         *     SpatializerExtensions.AmbisonicsOrder} of this sound source.
         * @return The Builder instance.
         */
        public com.android.extensions.xr.media.SoundFieldAttributes.Builder setAmbisonicsOrder(
                int ambisonicsOrder) {
            throw new RuntimeException("Stub!");
        }

        /**
         * Creates a new {@link com.android.extensions.xr.media.PointSourceAttributes
         * PointSourceAttributes} to be used. If no {@link Node} is provided, this will create a new
         * {@link Node} that must be parented to a node in the current scene.
         *
         * @return A new {@link com.android.extensions.xr.media.PointSourceAttributes
         *     PointSourceAttributes} object.
         */
        public com.android.extensions.xr.media.SoundFieldAttributes build()
                throws java.lang.UnsupportedOperationException {
            throw new RuntimeException("Stub!");
        }
    }
}
