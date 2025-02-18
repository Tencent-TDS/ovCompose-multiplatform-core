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

import java.util.Objects;

/**
 * Bounds values in meters.
 *
 * @see androidx.xr.extensions.XrExtensions#getSpatialState
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class Bounds {
    public final float width;
    public final float height;
    public final float depth;

    public Bounds(float width, float height, float depth) {
        this.width = width;
        this.height = height;
        this.depth = depth;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || !(other instanceof Bounds)) {
            return false;
        }
        Bounds impl = (Bounds) other;
        return width == impl.width && height == impl.height && depth == impl.depth;
    }

    @Override
    public int hashCode() {
        return Objects.hash(width, height, depth);
    }

    @Override
    public String toString() {
        return "{width=" + width + ", height=" + height + ", depth=" + depth + "}";
    }
}
