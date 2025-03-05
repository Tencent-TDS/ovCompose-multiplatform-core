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

import static java.util.Objects.requireNonNull;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/** This class is able to convert library types into platform types. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class SubspaceTypeConverter {

    private SubspaceTypeConverter() {}

    /**
     * Converts a {@link com.android.extensions.xr.subspace.Subspace} to a library type.
     *
     * @param subspace The {@link com.android.extensions.xr.subspace.Subspace} to convert.
     * @return The library type of the {@link Subspace}.
     */
    @NonNull
    public static Subspace toLibrary(
            @NonNull com.android.extensions.xr.subspace.Subspace subspace) {
        requireNonNull(subspace);

        return new SubspaceImpl(subspace);
    }

    /**
     * Converts a {@link Subspace} to a framework type.
     *
     * @param subspace The {@link Subspace} to convert.
     * @return The framework type of the {@link com.android.extensions.xr.subspace.Subspace}.
     */
    @NonNull
    public static com.android.extensions.xr.subspace.Subspace toFramework(
            @NonNull Subspace subspace) {
        requireNonNull(subspace);

        return ((SubspaceImpl) subspace).mSubspace;
    }
}
