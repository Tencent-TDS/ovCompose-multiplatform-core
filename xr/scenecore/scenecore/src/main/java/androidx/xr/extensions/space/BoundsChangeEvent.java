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

import androidx.annotation.RestrictTo;

/**
 * Triggers when there is a bounds change. For example, resize the panel in home space, or
 * enter/exit FSM.
 *
 * @deprecated Use SpatialState instead.
 */
@Deprecated
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public final class BoundsChangeEvent extends SpatialStateEvent {
    /** Width of the bounds in meters. */
    public float width;

    /** Height of the bounds in meters. */
    public float height;

    /** Depth of the bounds in meters. */
    public float depth;

    /** Bounds in meters. */
    public Bounds bounds;

    public BoundsChangeEvent(Bounds bounds) {
        this.bounds = bounds;
        this.width = bounds.width;
        this.height = bounds.height;
        this.depth = bounds.depth;
    }
}
