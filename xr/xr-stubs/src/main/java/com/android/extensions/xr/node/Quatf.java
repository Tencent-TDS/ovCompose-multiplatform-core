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

package com.android.extensions.xr.node;

import androidx.annotation.RestrictTo;

/** Quaternion. q = w + xi + yj + zk */
@SuppressWarnings({"unchecked", "deprecation", "all"})
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public final class Quatf {

    public Quatf(float x, float y, float z, float w) {
        throw new RuntimeException("Stub!");
    }

    public final float w;

    {
        w = 0;
    }

    public final float x;

    {
        x = 0;
    }

    public final float y;

    {
        y = 0;
    }

    public final float z;

    {
        z = 0;
    }
}
