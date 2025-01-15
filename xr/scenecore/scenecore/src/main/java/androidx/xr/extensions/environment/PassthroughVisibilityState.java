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

package androidx.xr.extensions.environment;

import static androidx.xr.extensions.XrExtensions.IMAGE_TOO_OLD;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;

/** Visibility states of passthrough. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface PassthroughVisibilityState {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @IntDef(
            value = {
                DISABLED, HOME, APP, SYSTEM,
            })
    @Retention(SOURCE)
    @interface State {}

    /** Passthrough is not shown. */
    int DISABLED = 0;

    /** Home environment Passthrough is shown with greater than 0 opacity. */
    int HOME = 1;

    /** App set Passthrough is shown with greater than 0 opacity. */
    int APP = 2;

    /** System set Passthrough is shown with greater than 0 opacity. */
    int SYSTEM = 3;

    /** Returns the current passthrough visibility state */
    default @PassthroughVisibilityState.State int getCurrentState() {
        throw new UnsupportedOperationException(IMAGE_TOO_OLD);
    }

    /** Returns the current passthrough opacity */
    default float getOpacity() {
        throw new UnsupportedOperationException(IMAGE_TOO_OLD);
    }
}
