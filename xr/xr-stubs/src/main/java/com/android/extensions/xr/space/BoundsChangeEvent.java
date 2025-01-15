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

import androidx.annotation.RestrictTo;

/**
 * Triggers when there is a bounds change. For example, resize the panel in home space, or
 * enter/exit FSM.
 *
 * @deprecated Use SpatialState instead.
 */
@SuppressWarnings({"unchecked", "deprecation", "all"})
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Deprecated
public final class BoundsChangeEvent implements com.android.extensions.xr.space.SpatialStateEvent {

    @Deprecated
    public BoundsChangeEvent(com.android.extensions.xr.space.Bounds bounds) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Width of the bounds in meters.
     *
     * @deprecated Use SpatialState instead.
     */
    @Deprecated
    public float getWidth() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Height of the bounds in meters.
     *
     * @deprecated Use SpatialState instead.
     */
    @Deprecated
    public float getHeight() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Depth of the bounds in meters.
     *
     * @deprecated Use SpatialState instead.
     */
    @Deprecated
    public float getDepth() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Bounds in meters.
     *
     * @deprecated Use SpatialState instead.
     */
    @Deprecated
    public com.android.extensions.xr.space.Bounds getBounds() {
        throw new RuntimeException("Stub!");
    }
}
