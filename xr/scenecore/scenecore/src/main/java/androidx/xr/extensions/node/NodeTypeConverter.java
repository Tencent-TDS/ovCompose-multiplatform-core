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

package androidx.xr.extensions.node;

import static java.util.Objects.requireNonNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

/** This class is able to convert library types into platform types. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class NodeTypeConverter {

    private NodeTypeConverter() {}

    /**
     * Converts a {@link Node} to a framework type.
     *
     * @param node The {@link Node} to convert.
     * @return The framework type of the {@link com.android.extensions.xr.node.Node}.
     */
    @Nullable
    public static com.android.extensions.xr.node.Node toFramework(@Nullable Node node) {
        if (node == null) {
            return null;
        }

        return ((NodeImpl) node).mNode;
    }

    /**
     * Converts a {@link com.android.extensions.xr.node.Node} to a library type.
     *
     * @param node The {@link com.android.extensions.xr.node.Node} to convert.
     * @return The library type of the {@link Node}.
     */
    @Nullable
    public static Node toLibrary(@Nullable com.android.extensions.xr.node.Node node) {
        if (node == null) {
            return null;
        }

        return new NodeImpl(node);
    }

    /**
     * Converts a {@link Vec3} to a framework type.
     *
     * @param vec The {@link Vec3} to convert.
     * @return The framework type of the {@link com.android.extensions.xr.node.Vec3}.
     */
    @NonNull
    public static com.android.extensions.xr.node.Vec3 toFramework(@NonNull Vec3 vec) {
        requireNonNull(vec);
        return new com.android.extensions.xr.node.Vec3(vec.x, vec.y, vec.z);
    }

    /**
     * Converts a {@link com.android.extensions.xr.node.Vec3} to a library type.
     *
     * @param vec The {@link com.android.extensions.xr.node.Vec3} to convert.
     * @return The library type of the {@link Vec3}.
     */
    @NonNull
    public static Vec3 toLibrary(@NonNull com.android.extensions.xr.node.Vec3 vec) {
        requireNonNull(vec);
        return new Vec3(vec.x, vec.y, vec.z);
    }

    /**
     * Converts a {@link com.android.extensions.xr.node.NodeTransaction} to a library type.
     *
     * @param transform The {@link com.android.extensions.xr.node.NodeTransaction} to convert.
     * @return The framework type of the {@link NodeTransaction}.
     */
    @NonNull
    public static NodeTransaction toLibrary(
            @NonNull com.android.extensions.xr.node.NodeTransaction transform) {
        requireNonNull(transform);
        return new NodeTransactionImpl(transform);
    }

    /**
     * Converts a {@link Quatf} to a framework type.
     *
     * @param value The {@link Quatf} to convert.
     * @return The framework type of the {@link com.android.extensions.xr.node.Quatf}.
     */
    @NonNull
    public static Quatf toLibrary(@NonNull com.android.extensions.xr.node.Quatf value) {
        requireNonNull(value);
        return new Quatf(value.x, value.y, value.z, value.w);
    }

    /**
     * Converts a {@link ReformOptions} to a framework type.
     *
     * @param options The {@link ReformOptions} to convert.
     * @return The framework type of the {@link com.android.extensions.xr.node.ReformOptions}.
     */
    @NonNull
    public static com.android.extensions.xr.node.ReformOptions toFramework(
            @NonNull ReformOptions options) {
        requireNonNull(options);
        return ((ReformOptionsImpl) options).mOptions;
    }

    /**
     * Converts a {@link com.android.extensions.xr.node.ReformOptions} to a library type.
     *
     * @param options The {@link com.android.extensions.xr.node.ReformOptions} to convert.
     * @return The library type of the {@link ReformOptions}.
     */
    @NonNull
    public static ReformOptions toLibrary(
            @NonNull com.android.extensions.xr.node.ReformOptions options) {
        requireNonNull(options);
        return new ReformOptionsImpl(options);
    }

    /**
     * Converts a {@link com.android.extensions.xr.node.ReformEvent} to a library type.
     *
     * @param event The {@linkcom.android.extensions.xr.node.ReformEvent} to convert.
     * @return The library type of the {@link ReformEvent}.
     */
    @NonNull
    public static ReformEvent toLibrary(@NonNull com.android.extensions.xr.node.ReformEvent event) {
        requireNonNull(event);
        return new ReformOptionsImpl.ReformEventImpl(event);
    }

    /**
     * Converts a {@link ReformEvent} to a framework type.
     *
     * @param event The {@link ReformEvent} to convert.
     * @return The framework type of the {@link com.android.extensions.xr.node.ReformEvent}.
     */
    @NonNull
    public static com.android.extensions.xr.node.ReformEvent toFramework(
            @NonNull ReformEvent event) {
        requireNonNull(event);
        return ((ReformOptionsImpl.ReformEventImpl) event).mEvent;
    }
}
