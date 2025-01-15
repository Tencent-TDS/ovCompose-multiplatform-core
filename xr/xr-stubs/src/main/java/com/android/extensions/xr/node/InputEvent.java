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

import androidx.annotation.RestrictTo;

/** A single 6DOF pointer event. */
@SuppressWarnings({"unchecked", "deprecation", "all"})
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public final class InputEvent {

    InputEvent() {
        throw new RuntimeException("Stub!");
    }

    /** Returns the source of this event. */
    public int getSource() {
        throw new RuntimeException("Stub!");
    }

    /** Returns the pointer type of this event. */
    public int getPointerType() {
        throw new RuntimeException("Stub!");
    }

    /** The time this event occurred, in the android.os.SystemClock#uptimeMillis time base. */
    public long getTimestamp() {
        throw new RuntimeException("Stub!");
    }

    /** The origin of the ray, in the receiver's task coordinate space. */
    public com.android.extensions.xr.node.Vec3 getOrigin() {
        throw new RuntimeException("Stub!");
    }

    /**
     * The direction the ray is pointing in, in the receiver's task coordinate space. Any point
     * along the ray can be represented as origin + d * direction, where d is non-negative.
     */
    public com.android.extensions.xr.node.Vec3 getDirection() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Info about the first scene node (closest to the ray origin) that was hit by the input ray, if
     * any. This will be null if no node was hit. Note that the hit node remains the same during an
     * ongoing DOWN -> MOVE -> UP action, even if the pointer stops hitting the node during the
     * action.
     */
    public com.android.extensions.xr.node.InputEvent.HitInfo getHitInfo() {
        throw new RuntimeException("Stub!");
    }

    /** Info about the second scene node from the same task that was hit, if any. */
    public com.android.extensions.xr.node.InputEvent.HitInfo getSecondaryHitInfo() {
        throw new RuntimeException("Stub!");
    }

    /** Gets the dispatch flags. */
    public int getDispatchFlags() {
        throw new RuntimeException("Stub!");
    }

    /** Returns the current action associated with this input event. */
    public int getAction() {
        throw new RuntimeException("Stub!");
    }

    /**
     * While the primary action button or gesture was held, the pointer was disabled. This happens
     * if you are using controllers and the battery runs out, or if you are using a source that
     * transitions to a new pointer type, eg SOURCE_GAZE_AND_GESTURE.
     */
    public static final int ACTION_CANCEL = 3; // 0x3

    /** The primary action button or gesture was just pressed / started. */
    public static final int ACTION_DOWN = 0; // 0x0

    /**
     * The primary action button or gesture is not pressed, and the pointer ray started to hit a new
     * node. The hit info represents the node that is being hit (may be null if pointer capture is
     * enabled).
     *
     * <p>Hover input events are never provided for sensitive source types.
     */
    public static final int ACTION_HOVER_ENTER = 5; // 0x5

    /**
     * The primary action button or gesture is not pressed, and the pointer ray stopped hitting the
     * node that it was previously hitting. The hit info represents the node that was being hit (may
     * be null if pointer capture is enabled).
     *
     * <p>Hover input events are never provided for sensitive source types.
     */
    public static final int ACTION_HOVER_EXIT = 6; // 0x6

    /**
     * The primary action button or gesture is not pressed, and the pointer ray continued to hit the
     * same node. The hit info represents the node that was hit (may be null if pointer capture is
     * enabled).
     *
     * <p>Hover input events are never provided for sensitive source types.
     */
    public static final int ACTION_HOVER_MOVE = 4; // 0x4

    /**
     * The primary action button or gesture was pressed/active in the previous event, and is still
     * pressed/active. The hit info represents the node that was originally hit (ie, as provided in
     * the ACTION_DOWN event). The hit position may be null if the pointer is no longer hitting that
     * node.
     */
    public static final int ACTION_MOVE = 2; // 0x2

    /**
     * The primary action button or gesture was just released / stopped. The hit info represents the
     * node that was originally hit (ie, as provided in the ACTION_DOWN event).
     */
    public static final int ACTION_UP = 1; // 0x1

    /** This event was also dispatched as a 2D Android input event. */
    public static final int DISPATCH_FLAG_2D = 2; // 0x2

    /** This event was dispatched to this receiver only because pointer capture was enabled. */
    public static final int DISPATCH_FLAG_CAPTURED_POINTER = 1; // 0x1

    /** Normal dispatch. */
    public static final int DISPATCH_FLAG_NONE = 0; // 0x0

    /**
     * Default pointer type for the source (no handedness). Occurs for SOURCE_UNKNOWN, SOURCE_HEAD,
     * SOURCE_MOUSE, and SOURCE_GAZE_AND_GESTURE.
     */
    public static final int POINTER_TYPE_DEFAULT = 0; // 0x0

    /**
     * Left hand / controller pointer.. Occurs for SOURCE_CONTROLLER, SOURCE_HANDS, and
     * SOURCE_GAZE_AND_GESTURE.
     */
    public static final int POINTER_TYPE_LEFT = 1; // 0x1

    /**
     * Right hand / controller pointer.. Occurs for SOURCE_CONTROLLER, SOURCE_HANDS, and
     * SOURCE_GAZE_AND_GESTURE.
     */
    public static final int POINTER_TYPE_RIGHT = 2; // 0x2

    /**
     * Event is based on (one of) the user's controller(s). Ray origin and direction are for a
     * controller aim pose as defined by OpenXR.
     * (https://registry.khronos.org/OpenXR/specs/1.1/html/xrspec.html#semantic-paths-standard-pose-identifiers)
     * Action state is based on the primary button on the controller, usually the bottom-most face
     * button.
     */
    public static final int SOURCE_CONTROLLER = 2; // 0x2

    /**
     * Event is based on a mix of the head, eyes, and hands. Ray origin is at average between eyes
     * and points in direction based on a mix of eye gaze direction and hand motion. During a
     * two-handed zoom/rotate gesture, left/right pointer events will be issued; otherwise, default
     * events are issued based on the gaze ray. Action state is based on if the user has done a
     * pinch gesture or not.
     *
     * <p>Events from this device type are considered sensitive and hover events are never sent.
     */
    public static final int SOURCE_GAZE_AND_GESTURE = 5; // 0x5

    /**
     * Event is based on one of the user's hands. Ray is a hand aim pose, with origin between thumb
     * and forefinger and points in direction based on hand orientation. Action state is based on a
     * pinch gesture.
     */
    public static final int SOURCE_HANDS = 3; // 0x3

    /**
     * Event is based on the user's head. Ray origin is at average between eyes, pushed out to the
     * near clipping plane for both eyes and points in direction head is facing. Action state is
     * based on volume up button being depressed.
     *
     * <p>Events from this device type are considered sensitive and hover events are never sent.
     */
    public static final int SOURCE_HEAD = 1; // 0x1

    /**
     * Event is based on a 2D mouse pointing device. Ray origin behaves the same as for
     * DEVICE_TYPE_HEAD and points in direction based on mouse movement. During a drag, the ray
     * origin moves approximating hand motion. The scrollwheel moves the ray away from / towards the
     * user. Action state is based on the primary mouse button.
     */
    public static final int SOURCE_MOUSE = 4; // 0x4

    public static final int SOURCE_UNKNOWN = 0; // 0x0

    /** Info about a single ray hit. */
    @SuppressWarnings({"unchecked", "deprecation", "all"})
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public static class HitInfo {

        public HitInfo(
                int subspaceImpressNodeId,
                com.android.extensions.xr.node.Node inputNode,
                com.android.extensions.xr.node.Mat4f transform,
                com.android.extensions.xr.node.Vec3 hitPosition) {
            throw new RuntimeException("Stub!");
        }

        /**
         * ID of the front-end Impress node within the subspace that was hit. Used by Split-Engine
         * to create a handle to the node with the same entity ID. In case the node doesn't belong
         * to a subspace the value will be 0, i.e.,
         * utils::Entity::import(subspaceImpressNodeId).IsNull() == true.
         *
         * <p>ACTION_MOVE, ACTION_UP, and ACTION_CANCEL events will report the same node id as was
         * hit during the initial ACTION_DOWN.
         */
        public int getSubspaceImpressNodeId() {
            throw new RuntimeException("Stub!");
        }

        /**
         * The CPM node that was hit.
         *
         * <p>ACTION_MOVE, ACTION_UP, and ACTION_CANCEL events will report the same node as was hit
         * during the initial ACTION_DOWN.
         */
        public com.android.extensions.xr.node.Node getInputNode() {
            throw new RuntimeException("Stub!");
        }

        /**
         * The ray hit position, in the receiver's task coordinate space.
         *
         * <p>All events may report the current ray's hit position. This can be null if there no
         * longer is a collision between the ray and the input node (eg, during a drag event).
         */
        public com.android.extensions.xr.node.Vec3 getHitPosition() {
            throw new RuntimeException("Stub!");
        }

        /** The matrix transforming task node coordinates into the hit CPM node's coordinates. */
        public com.android.extensions.xr.node.Mat4f getTransform() {
            throw new RuntimeException("Stub!");
        }
    }
}
