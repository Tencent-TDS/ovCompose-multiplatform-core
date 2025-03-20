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

package com.android.extensions.xr.node;


/** interface containing the Node transform */
@SuppressWarnings({"unchecked", "deprecation", "all"})
public class NodeTransform {

    NodeTransform() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Get the transformation matrix associated with the node.
     *
     * <p>The provided matrix transforms a point in this node's local coordinate system into a point
     * in world space coordinates. For example, {@code NodeTransform.getTransform()} * (0, 0, 0, 1)
     * is the position of this node in world space. The first non-null transform will be returned
     * immediately after the subscription set-up is complete.
     *
     * @return A transformation matrix {@link com.android.extensions.xr.node.Mat4f Mat4f} containing
     *     the current transformation matrix of this node.
     */
    public com.android.extensions.xr.node.Mat4f getTransform() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Get the timestamp at which the transformation matrix was recorded.
     *
     * <p>The time the record happened, in the android.os.SystemClock#uptimeNanos time base.
     *
     * @return A timestamp at which the transformation matrix was recorded.
     */
    public long getTimestamp() {
        throw new RuntimeException("Stub!");
    }
}
