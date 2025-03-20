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

package com.android.extensions.xr.space;


/**
 * Hit test result.
 *
 * @see androidx.xr.extensions.XrExtensions#hitTest
 */
@SuppressWarnings({"unchecked", "deprecation", "all"})
public class HitTestResult {

    HitTestResult() {
        throw new RuntimeException("Stub!");
    }

    /** Distance from the ray origin to the hit position. */
    public float getDistance() {
        throw new RuntimeException("Stub!");
    }

    /** The hit position in task coordinates. */
    public com.android.extensions.xr.node.Vec3 getHitPosition() {
        throw new RuntimeException("Stub!");
    }

    /** Normal of the surface at the collision point, if known. */
    public com.android.extensions.xr.node.Vec3 getSurfaceNormal() {
        throw new RuntimeException("Stub!");
    }

    /** The type of surface that was hit. */
    public int getSurfaceType() {
        throw new RuntimeException("Stub!");
    }

    /** Whether or not the virtual background environment is visible. */
    public boolean getVirtualEnvironmentIsVisible() {
        throw new RuntimeException("Stub!");
    }

    /** The ray has hit a 3D object */
    public static final int SURFACE_3D_OBJECT = 2; // 0x2

    /** The ray has hit a 2D panel */
    public static final int SURFACE_PANEL = 1; // 0x1

    /** The ray has hit something unknown or nothing at all */
    public static final int SURFACE_UNKNOWN = 0; // 0x0

    @SuppressWarnings({"unchecked", "deprecation", "all"})
    public static final class Builder {

        public Builder(
                float distance,
                com.android.extensions.xr.node.Vec3 hitPosition,
                boolean virtualEnvironmentIsVisible,
                int surfaceType) {
            throw new RuntimeException("Stub!");
        }

        /** Sets the surface vector. */
        public com.android.extensions.xr.space.HitTestResult.Builder setSurfaceNormal(
                com.android.extensions.xr.node.Vec3 surfaceNormal) {
            throw new RuntimeException("Stub!");
        }

        /** Builds the HitTestResult. */
        public com.android.extensions.xr.space.HitTestResult build() {
            throw new RuntimeException("Stub!");
        }
    }
}
