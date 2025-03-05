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
import androidx.xr.extensions.environment.PassthroughVisibilityState;

/**
 * For all resumed top activities with this spatialstate callback set, this is called whenever the
 * Passthrough state changes. This is also called when an activity becomes top resumed.
 * TODO(runping): Ask the privacy team if it's okay to send this event in HSM.
 *
 * @deprecated Use SpatialState instead.
 */
@Deprecated
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public final class PassthroughVisibilityChangeEvent extends SpatialStateEvent {
    /** Visibility state of Passthrough. */
    public @PassthroughVisibilityState.State int passthroughState;

    public PassthroughVisibilityChangeEvent(@PassthroughVisibilityState.State int state) {
        this.passthroughState = state;
    }
}
