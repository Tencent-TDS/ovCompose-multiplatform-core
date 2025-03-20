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

package com.android.extensions.xr.space;


/**
 * Triggers when the spatial capability set has changed.
 *
 * @deprecated Use SpatialState instead.
 */
@SuppressWarnings({"unchecked", "deprecation", "all"})
@Deprecated
public final class SpatialCapabilityChangeEvent
        implements com.android.extensions.xr.space.SpatialStateEvent {

    @Deprecated
    public SpatialCapabilityChangeEvent(
            com.android.extensions.xr.space.SpatialCapabilities capabilities) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Gets the enabled capabailities.
     *
     * @deprecated Use SpatialState instead.
     */
    @Deprecated
    public com.android.extensions.xr.space.SpatialCapabilities getCurrentCapabilities() {
        throw new RuntimeException("Stub!");
    }
}
