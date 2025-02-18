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

package androidx.xr.extensions.media;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.xr.extensions.XrExtensionsProvider;
import androidx.xr.extensions.node.Node;

/** {@link PointSourceAttributes} is used to configure a sound be spatialized as a 3D point. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class PointSourceAttributes {
    private Node mNode;

    private PointSourceAttributes(@NonNull Node node) {
        mNode = node;
    }

    /**
     * The {@link Node} to which this sound source is attached. The sound source will use the 3D
     * transform of the Node. The node returned from this method must be parented to a node in the
     * scene.
     *
     * @return The {@link Node} to which the sound source is attached.
     */
    public @NonNull Node getNode() {
        return mNode;
    }

    /** Builder class for {@link PointSourceAttributes} */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public static final class Builder {
        private Node mNode;

        public Builder() {}

        /**
         * @param node The {@link Node} to use to position the sound source.
         * @return The Builder instance.
         */
        public @NonNull Builder setNode(@NonNull Node node) {
            mNode = node;
            return this;
        }

        /**
         * Creates a new {@link PointSourceAttributes} to be used. If no {@link Node} is provided,
         * this will create a new {@link Node} that must be parented to a node in the current scene.
         *
         * @return A new {@link PointSourceAttributes} object.
         */
        @NonNull
        public PointSourceAttributes build() throws UnsupportedOperationException {
            if (mNode == null) {
                mNode = XrExtensionsProvider.getXrExtensions().createNode();
            }

            return new PointSourceAttributes(mNode);
        }
    }
}
