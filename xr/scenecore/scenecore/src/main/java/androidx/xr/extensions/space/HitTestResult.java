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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.xr.extensions.node.Vec3;

import java.lang.annotation.Retention;

/**
 * Hit test result.
 *
 * @see androidx.xr.extensions.XrExtensions#hitTest
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class HitTestResult {
    /** Distance from the ray origin to the hit position. */
    public float distance;

    /** The hit position in task coordinates. */
    public @NonNull Vec3 hitPosition;

    /** Normal of the surface at the collision point, if known. */
    public @Nullable Vec3 surfaceNormal;

    /** Hit surface types. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @IntDef(value = {SURFACE_UNKNOWN, SURFACE_PANEL, SURFACE_3D_OBJECT})
    @Retention(SOURCE)
    public @interface SurfaceType {}

    public static final int SURFACE_UNKNOWN = 0;
    public static final int SURFACE_PANEL = 1;
    public static final int SURFACE_3D_OBJECT = 2;

    /** The type of surface that was hit. */
    public @SurfaceType int surfaceType;

    /** Whether or not the virtual background environment is visible. */
    public boolean virtualEnvironmentIsVisible;
}
