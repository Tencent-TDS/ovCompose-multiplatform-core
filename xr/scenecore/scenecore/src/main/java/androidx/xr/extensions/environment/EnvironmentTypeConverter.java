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

package androidx.xr.extensions.environment;

import static java.util.Objects.requireNonNull;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/** This class is able to convert library types into platform types. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class EnvironmentTypeConverter {

    private EnvironmentTypeConverter() {}

    /**
     * Converts a {@link PassthroughVisibilityState} to a framework type.
     *
     * @param state The {@link PassthroughVisibilityState} to convert.
     * @return The framework type of the {@link
     *     com.android.extensions.xr.environment.PassthroughVisibilityState}.
     */
    @NonNull
    public static com.android.extensions.xr.environment.PassthroughVisibilityState toFramework(
            @NonNull PassthroughVisibilityState state) {
        requireNonNull(state);

        return ((PassthroughVisibilityStateImpl) state).mState;
    }

    /**
     * Converts a {@link com.android.extensions.xr.environment.PassthroughVisibilityState} to a
     * library type.
     *
     * @param state The {@link com.android.extensions.xr.environment.PassthroughVisibilityState} to
     *     convert.
     * @return The library type of the {@link PassthroughVisibilityState}.
     */
    @NonNull
    public static PassthroughVisibilityState toLibrary(
            @NonNull com.android.extensions.xr.environment.PassthroughVisibilityState state) {
        requireNonNull(state);

        return new PassthroughVisibilityStateImpl(state);
    }

    /**
     * Converts a {@link EnvironmentVisibilityState} to a framework type.
     *
     * @param state The {@link EnvironmentVisibilityState} to convert.
     * @return The framework type of the {@link
     *     com.android.extensions.xr.environment.EnvironmentVisibilityState}.
     */
    @NonNull
    public static com.android.extensions.xr.environment.EnvironmentVisibilityState toFramework(
            @NonNull EnvironmentVisibilityState state) {
        requireNonNull(state);

        return ((EnvironmentVisibilityStateImpl) state).mState;
    }

    /**
     * Converts a {@link com.android.extensions.xr.environment.EnvironmentVisibilityState} to a
     * library type.
     *
     * @param state The {@link com.android.extensions.xr.environment.EnvironmentVisibilityState} to
     *     convert.
     * @return The library type of the {@link EnvironmentVisibilityState}.
     */
    @NonNull
    public static EnvironmentVisibilityState toLibrary(
            @NonNull com.android.extensions.xr.environment.EnvironmentVisibilityState state) {
        requireNonNull(state);

        return new EnvironmentVisibilityStateImpl(state);
    }
}
