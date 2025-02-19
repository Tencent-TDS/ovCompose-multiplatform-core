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

/** Visibility states of an environment. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface EnvironmentVisibilityState {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @IntDef(
            value = {
                INVISIBLE,
                HOME_VISIBLE,
                APP_VISIBLE,
            })
    @Retention(SOURCE)
    @interface State {}

    /*
     * No environment is shown. This could mean Passthrough is on with 100% opacity or the
     * home environment app has crashed.
     */
    int INVISIBLE = 0;

    /** Home environment is shown. Passthrough might be on but its opacity is less than 100%. */
    int HOME_VISIBLE = 1;

    /** App environment is shown. Passthrough might be on but its opacity is less than 100%. */
    int APP_VISIBLE = 2;

    /** Returns the current environment visibility state */
    default @EnvironmentVisibilityState.State int getCurrentState() {
        throw new UnsupportedOperationException(IMAGE_TOO_OLD);
    }
}
