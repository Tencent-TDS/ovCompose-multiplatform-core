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

package androidx.xr.extensions.space;

import androidx.annotation.NonNull;

final class SpatialCapabilitiesImpl implements SpatialCapabilities {
    @NonNull final com.android.extensions.xr.space.SpatialCapabilities mCapabilities;

    SpatialCapabilitiesImpl() {
        mCapabilities = new com.android.extensions.xr.space.SpatialCapabilities();
    }

    SpatialCapabilitiesImpl(
            @NonNull com.android.extensions.xr.space.SpatialCapabilities capabilities) {
        mCapabilities = capabilities;
    }

    @Override
    public boolean get(@CapabilityType int capability) {
        return mCapabilities.get(capability);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        SpatialCapabilitiesImpl impl = (SpatialCapabilitiesImpl) other;
        return mCapabilities.equals(impl.mCapabilities);
    }

    @Override
    public int hashCode() {
        return mCapabilities.hashCode();
    }

    @Override
    public String toString() {
        return mCapabilities.toString();
    }
}
