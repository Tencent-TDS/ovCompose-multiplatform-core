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

package com.android.extensions.xr.passthrough;

import androidx.annotation.RestrictTo;

/** Allows to configure the passthrough when the application is in full-space mode. */
@SuppressWarnings({"unchecked", "deprecation", "all"})
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class PassthroughState {

    PassthroughState() {
        throw new RuntimeException("Stub!");
    }

    /** Node maximizes the opacity of the final passthrough state. */
    public static final int PASSTHROUGH_MODE_MAX = 1; // 0x1

    /** Node minimizes the opacity of the final passthrough state. */
    public static final int PASSTHROUGH_MODE_MIN = 2; // 0x2

    /** Node does not contribute to the opacity of the final passthrough state. */
    public static final int PASSTHROUGH_MODE_OFF = 0; // 0x0
}
