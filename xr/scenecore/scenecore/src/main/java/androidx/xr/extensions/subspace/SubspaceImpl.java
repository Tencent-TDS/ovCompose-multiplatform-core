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

package androidx.xr.extensions.subspace;

import androidx.annotation.NonNull;

/**
 * Handle to a subspace in the system scene graph.
 *
 * <p>A subspace by itself does not have any visual representation. It merely defines a local space
 * in its parent space. Once created, 3D content can be rendered in the hierarchy of that subspace.
 *
 * <p>Note that {@link Subspace} uses a right-hand coordinate system, i.e. +X points to the right,
 * +Y up, and +Z points towards the camera.
 */
class SubspaceImpl implements Subspace {
    final com.android.extensions.xr.subspace.Subspace mSubspace;

    /** Creates a new empty subspace. */
    SubspaceImpl(@NonNull com.android.extensions.xr.subspace.Subspace subspace) {
        mSubspace = subspace;
    }
}
