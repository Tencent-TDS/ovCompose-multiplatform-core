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

/** Visibility states of passthrough. */
@SuppressWarnings({"unchecked", "deprecation", "all"})
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class PassthroughVisibilityState {

    PassthroughVisibilityState() {
        throw new RuntimeException("Stub!");
    }

    /** Returns the current passthrough visibility state */
    public int getCurrentState() {
        throw new RuntimeException("Stub!");
    }

    /** Returns the current passthrough opacity */
    public float getOpacity() {
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

    /** App set Passthrough is shown with greater than 0 opacity. */
    public static final int APP = 2; // 0x2

    /** Passthrough is not shown. */
    public static final int DISABLED = 0; // 0x0

    /** Home environment Passthrough is shown with greater than 0 opacity. */
    public static final int HOME = 1; // 0x1

    /** System set Passthrough is shown with greater than 0 opacity. */
    public static final int SYSTEM = 3; // 0x3
}
