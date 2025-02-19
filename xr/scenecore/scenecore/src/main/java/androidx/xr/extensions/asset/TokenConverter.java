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

package androidx.xr.extensions.asset;

import static java.util.Objects.requireNonNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

/**
 * This class is able to convert library versions of {@link AssetToken}s into platform types.
 *
 * @deprecated This will be removed once all clients are migrated.
 */
@Deprecated
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class TokenConverter {

    private TokenConverter() {}

    /**
     * Converts a {@link GltfModelToken} to a framework type.
     *
     * @param token The {@link GltfModelToken} to convert.
     * @return The framework type of the {@link GltfModelToken}.
     * @deprecated This will be removed once all clients are migrated.
     */
    @Deprecated
    @NonNull
    public static com.android.extensions.xr.asset.GltfModelToken toFramework(
            @NonNull GltfModelToken token) {
        requireNonNull(token);

        return ((GltfModelTokenImpl) token).mToken;
    }

    /**
     * Converts a {@link com.android.extensions.xr.asset.GltfModelToken} to a library type.
     *
     * @param token The {@link com.android.extensions.xr.asset.GltfModelToken} to convert.
     * @return The library type of the {@link GltfModelToken}.
     */
    @Nullable
    public static GltfModelToken toLibrary(
            @Nullable com.android.extensions.xr.asset.GltfModelToken token) {
        if (token == null) {
            return null;
        }

        return new GltfModelTokenImpl((com.android.extensions.xr.asset.GltfModelToken) token);
    }

    /**
     * Converts a {@link EnvironmentToken} to a framework type.
     *
     * @param token The {@link EnvironmentToken} to convert.
     * @return The framework type of the {@link com.android.extensions.xr.asset.EnvironmentToken}.
     */
    @NonNull
    public static com.android.extensions.xr.asset.EnvironmentToken toFramework(
            @NonNull EnvironmentToken token) {
        requireNonNull(token);

        return ((EnvironmentTokenImpl) token).mToken;
    }

    /**
     * Converts a {@link com.android.extensions.xr.asset.EnvironmentToken} to a library type.
     *
     * @param token The {@link com.android.extensions.xr.asset.EnvironmentToken} to convert.
     * @return The library type of the {@link EnvironmentToken}.
     */
    @Nullable
    public static EnvironmentToken toLibrary(
            @Nullable com.android.extensions.xr.asset.EnvironmentToken token) {
        if (token == null) {
            return null;
        }

        return new EnvironmentTokenImpl((com.android.extensions.xr.asset.EnvironmentToken) token);
    }

    /**
     * Converts a {@link SceneToken} to a framework type.
     *
     * @param token The {@link SceneToken} to convert.
     * @return The framework type of the {@link com.android.extensions.xr.asset.SceneToken}.
     */
    @NonNull
    public static com.android.extensions.xr.asset.SceneToken toFramework(
            @NonNull SceneToken token) {
        requireNonNull(token);

        return ((SceneTokenImpl) token).mToken;
    }

    /**
     * Converts a {@link com.android.extensions.xr.asset.SceneToken} to a library type.
     *
     * @param token The {@link com.android.extensions.xr.asset.SceneToken} to convert.
     * @return The library type of the {@link SceneToken}.
     */
    @Nullable
    public static SceneToken toLibrary(@Nullable com.android.extensions.xr.asset.SceneToken token) {
        if (token == null) {
            return null;
        }

        return new SceneTokenImpl((com.android.extensions.xr.asset.SceneToken) token);
    }

    /**
     * Converts a {@link GltfAnimation.State} to a framework type.
     *
     * @param token The {@link GltfAnimation.State} to convert.
     * @return The framework type {@link com.android.extensions.xr.asset.GltfAnimation.State}.
     */
    public static com.android.extensions.xr.asset.GltfAnimation.State toFramework(
            GltfAnimation.State token) {

        switch (token) {
            case STOP:
                return com.android.extensions.xr.asset.GltfAnimation.State.STOP;
            case PLAY:
                return com.android.extensions.xr.asset.GltfAnimation.State.PLAY;
            case LOOP:
                return com.android.extensions.xr.asset.GltfAnimation.State.LOOP;
            default:
                throw new IllegalArgumentException("Should not happen");
        }
    }
}
