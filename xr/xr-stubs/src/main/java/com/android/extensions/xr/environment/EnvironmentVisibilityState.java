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

package com.android.extensions.xr.environment;

import androidx.annotation.RestrictTo;

/** Visibility states of an environment. */
@SuppressWarnings({"unchecked", "deprecation", "all"})
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class EnvironmentVisibilityState {

    EnvironmentVisibilityState() {
        throw new RuntimeException("Stub!");
    }

    /** Returns the current environment visibility state */
    public int getCurrentState() {
        throw new RuntimeException("Stub!");
    }

    /** equals() */
    public boolean equals(java.lang.Object other) {
        throw new RuntimeException("Stub!");
    }

    /** hashCode() */
    public int hashCode() {
        throw new RuntimeException("Stub!");
    }

    /** toString() */
    public java.lang.String toString() {
        throw new RuntimeException("Stub!");
    }

    /** App environment is shown. Passthrough might be on but its opacity is less than 100%. */
    public static final int APP_VISIBLE = 2; // 0x2

    /** Home environment is shown. Passthrough might be on but its opacity is less than 100%. */
    public static final int HOME_VISIBLE = 1; // 0x1

    /**
     * No environment is shown. This could mean Passthrough is on with 100% opacity or the
     * home environment app has crashed.
     */
    public static final int INVISIBLE = 0; // 0x0
}
