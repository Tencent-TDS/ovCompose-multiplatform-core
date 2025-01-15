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

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/** {@link SoundFieldAttributes} is used to configure ambisonic sound sources. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class SoundFieldAttributes {
    private int mAmbisonicsOrder;

    private SoundFieldAttributes(int ambisonicsOrder) {
        mAmbisonicsOrder = ambisonicsOrder;
    }

    /**
     * @return The {@link SpatializerExtensions.AmbisonicsOrder} of this sound source.
     */
    @SpatializerExtensions.AmbisonicsOrder
    public int getAmbisonicsOrder() {
        return mAmbisonicsOrder;
    }

    /** Builder class for {@link SoundFieldAttributes} */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public static final class Builder {
        private int mAmbisonicsOrder = SpatializerExtensions.AMBISONICS_ORDER_FIRST_ORDER;

        public Builder() {}

        /**
         * @param ambisonicsOrder Sets the {@link SpatializerExtensions.AmbisonicsOrder} of this
         *     sound source.
         * @return The Builder instance.
         */
        public @NonNull Builder setAmbisonicsOrder(
                @SpatializerExtensions.AmbisonicsOrder int ambisonicsOrder) {
            mAmbisonicsOrder = ambisonicsOrder;
            return this;
        }

        /**
         * Creates a new {@link PointSourceAttributes} to be used. If no {@link Node} is provided,
         * this will create a new {@link Node} that must be parented to a node in the current scene.
         *
         * @return A new {@link PointSourceAttributes} object.
         */
        @NonNull
        public SoundFieldAttributes build() throws UnsupportedOperationException {
            return new SoundFieldAttributes(mAmbisonicsOrder);
        }
    }
}
