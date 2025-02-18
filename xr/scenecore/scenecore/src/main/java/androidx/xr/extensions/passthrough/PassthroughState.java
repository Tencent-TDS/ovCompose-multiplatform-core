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

package androidx.xr.extensions.passthrough;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;

/** Allows to configure the passthrough when the application is in full-space mode. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class PassthroughState {
    /** Passthrough mode to be used. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @IntDef(
            value = {
                PASSTHROUGH_MODE_OFF,
                PASSTHROUGH_MODE_MAX,
                PASSTHROUGH_MODE_MIN,
            })
    @Retention(SOURCE)
    public @interface Mode {}

    /** Node does not contribute to the opacity of the final passthrough state. */
    public static final int PASSTHROUGH_MODE_OFF = 0;

    /** Node maximizes the opacity of the final passthrough state. */
    public static final int PASSTHROUGH_MODE_MAX = 1;

    /** Node minimizes the opacity of the final passthrough state. */
    public static final int PASSTHROUGH_MODE_MIN = 2;
}
