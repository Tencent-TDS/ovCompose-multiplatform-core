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

package com.android.extensions.xr.media;


/**
 * {@link com.android.extensions.xr.media.PointSourceAttributes PointSourceAttributes} is used to
 * configure a sound be spatialized as a 3D point.
 */
@SuppressWarnings({"unchecked", "deprecation", "all"})
public class PointSourceAttributes {

    PointSourceAttributes() {
        throw new RuntimeException("Stub!");
    }

    /**
     * The {@link com.android.extensions.xr.node.Node Node} to which this sound source is attached.
     * The sound source will use the 3D transform of the Node. The node returned from this method
     * must be parented to a node in the scene.
     *
     * @return The {@link com.android.extensions.xr.node.Node Node} to which the sound source is
     *     attached.
     */
    public com.android.extensions.xr.node.Node getNode() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Builder class for {@link com.android.extensions.xr.media.PointSourceAttributes
     * PointSourceAttributes}
     */
    @SuppressWarnings({"unchecked", "deprecation", "all"})
    public static final class Builder {

        public Builder() {
            throw new RuntimeException("Stub!");
        }

        /**
         * @param node The {@link com.android.extensions.xr.node.Node Node} to use to position the
         *     sound source.
         * @return The Builder instance.
         */
        public com.android.extensions.xr.media.PointSourceAttributes.Builder setNode(
                com.android.extensions.xr.node.Node node) {
            throw new RuntimeException("Stub!");
        }

        /**
         * Creates a new {@link com.android.extensions.xr.media.PointSourceAttributes
         * PointSourceAttributes} to be used. If no {@link com.android.extensions.xr.node.Node Node}
         * is provided, this will create a new {@link com.android.extensions.xr.node.Node Node} that
         * must be parented to a node in the current scene.
         *
         * @return A new {@link com.android.extensions.xr.media.PointSourceAttributes
         *     PointSourceAttributes} object.
         */
        public com.android.extensions.xr.media.PointSourceAttributes build()
                throws java.lang.UnsupportedOperationException {
            throw new RuntimeException("Stub!");
        }
    }
}
