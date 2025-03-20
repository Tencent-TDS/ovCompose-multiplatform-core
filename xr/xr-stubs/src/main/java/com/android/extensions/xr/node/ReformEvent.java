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


/** A reform (move / resize) event. */
@SuppressWarnings({"unchecked", "deprecation", "all"})
public class ReformEvent {

    ReformEvent() {
        throw new RuntimeException("Stub!");
    }

    /** Gets the event type. */
    public int getType() {
        throw new RuntimeException("Stub!");
    }

    /** Gets the event state. */
    public int getState() {
        throw new RuntimeException("Stub!");
    }

    /** An identifier for this reform action. */
    public int getId() {
        throw new RuntimeException("Stub!");
    }

    /** The initial ray origin and direction, in task space. */
    public com.android.extensions.xr.node.Vec3 getInitialRayOrigin() {
        throw new RuntimeException("Stub!");
    }

    /** The initial ray direction, in task space. */
    public com.android.extensions.xr.node.Vec3 getInitialRayDirection() {
        throw new RuntimeException("Stub!");
    }

    /** The current ray origin and direction, in task space. */
    public com.android.extensions.xr.node.Vec3 getCurrentRayOrigin() {
        throw new RuntimeException("Stub!");
    }

    /** The current ray direction, in task space. */
    public com.android.extensions.xr.node.Vec3 getCurrentRayDirection() {
        throw new RuntimeException("Stub!");
    }

    /**
     * For a move event, the proposed pose of the node, in task space (or relative to the parent
     * node, if FLAG_POSE_RELATIVE_TO_PARENT was specified in the ReformOptions).
     */
    public com.android.extensions.xr.node.Vec3 getProposedPosition() {
        throw new RuntimeException("Stub!");
    }

    /** For a move event, the proposed orientation of the node, in task space. */
    public com.android.extensions.xr.node.Quatf getProposedOrientation() {
        throw new RuntimeException("Stub!");
    }

    /** Scale will change with distance if ReformOptions.FLAG_SCALE_WITH_DISTANCE is set. */
    public com.android.extensions.xr.node.Vec3 getProposedScale() {
        throw new RuntimeException("Stub!");
    }

    /**
     * For a resize event, the proposed new size in meters. Note that in the initial implementation,
     * the Z size may not be modified.
     */
    public com.android.extensions.xr.node.Vec3 getProposedSize() {
        throw new RuntimeException("Stub!");
    }

    public static final int REFORM_STATE_END = 3; // 0x3

    public static final int REFORM_STATE_ONGOING = 2; // 0x2

    public static final int REFORM_STATE_START = 1; // 0x1

    public static final int REFORM_STATE_UNKNOWN = 0; // 0x0

    public static final int REFORM_TYPE_MOVE = 1; // 0x1

    public static final int REFORM_TYPE_RESIZE = 2; // 0x2

    public static final int REFORM_TYPE_UNKNOWN = 0; // 0x0
}
