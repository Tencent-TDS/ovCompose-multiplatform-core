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

import static java.lang.annotation.RetentionPolicy.SOURCE;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;
import androidx.xr.extensions.ExperimentalExtensionApi;

import java.lang.annotation.Retention;

/**
 * Triggers when there is a change to spatial visibility. For example, user looks away from the
 * activity.
 *
 * @deprecated Not used anymore.
 */
@Deprecated
@ExperimentalExtensionApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public final class VisibilityChangeEvent extends SpatialStateEvent {
    /** Possible visibility values for an activity. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @IntDef(value = {UNKNOWN, HIDDEN, PARTIALLY_VISIBLE, VISIBLE})
    @Retention(SOURCE)
    public @interface SpatialVisibility {}

    /** The visibility of this activity is not known. */
    public static final int UNKNOWN = 0;

    /** This activity is hidden outside of the user's Field of View. */
    public static final int HIDDEN = 1;

    /** Some, but not all, of the activity is within the user's Field of View. */
    public static final int PARTIALLY_VISIBLE = 2;

    /** The entirety of the activity is within the user's Field of View. */
    public static final int VISIBLE = 3;

    public @SpatialVisibility int visibility;

    public VisibilityChangeEvent(@SpatialVisibility int visibility) {
        this.visibility = visibility;
    }
}
