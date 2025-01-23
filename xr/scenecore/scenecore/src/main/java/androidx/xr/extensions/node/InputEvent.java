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

import static java.lang.annotation.RetentionPolicy.SOURCE;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;

/** A single 6DOF pointer event. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface InputEvent {
    // clang-format off
    /** The type of the source of this event. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @IntDef(
            value = {
                SOURCE_UNKNOWN,
                SOURCE_HEAD,
                SOURCE_CONTROLLER,
                SOURCE_HANDS,
                SOURCE_MOUSE,
                SOURCE_GAZE_AND_GESTURE,
            })
    @Retention(SOURCE)
    @interface Source {}

    // clang-format on

    // Unknown source.
    int SOURCE_UNKNOWN = 0;

    /**
     * Event is based on the user's head. Ray origin is at average between eyes, pushed out to the
     * near clipping plane for both eyes and points in direction head is facing. Action state is
     * based on volume up button being depressed.
     *
     * <p>Events from this device type are considered sensitive and hover events are never sent.
     */
    int SOURCE_HEAD = 1;

    /**
     * Event is based on (one of) the user's controller(s). Ray origin and direction are for a
     * controller aim pose as defined by OpenXR.
     * (https://registry.khronos.org/OpenXR/specs/1.1/html/xrspec.html#semantic-paths-standard-pose-identifiers)
     * Action state is based on the primary button on the controller, usually the bottom-most face
     * button.
     */
    int SOURCE_CONTROLLER = 2;

    /**
     * Event is based on one of the user's hands. Ray is a hand aim pose, with origin between thumb
     * and forefinger and points in direction based on hand orientation. Action state is based on a
     * pinch gesture.
     */
    int SOURCE_HANDS = 3;

    /**
     * Event is based on a 2D mouse pointing device. Ray origin behaves the same as for
     * DEVICE_TYPE_HEAD and points in direction based on mouse movement. During a drag, the ray
     * origin moves approximating hand motion. The scrollwheel moves the ray away from / towards the
     * user. Action state is based on the primary mouse button.
     */
    int SOURCE_MOUSE = 4;

    /**
     * Event is based on a mix of the head, eyes, and hands. Ray origin is at average between eyes
     * and points in direction based on a mix of eye gaze direction and hand motion. During a
     * two-handed zoom/rotate gesture, left/right pointer events will be issued; otherwise, default
     * events are issued based on the gaze ray. Action state is based on if the user has done a
     * pinch gesture or not.
     *
     * <p>Events from this device type are considered sensitive and hover events are never sent.
     */
    int SOURCE_GAZE_AND_GESTURE = 5;

    /** Returns the source of this event. */
    @Source
    int getSource();

    /** The type of the individual pointer. */
    // clang-format off
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @IntDef(
            value = {
                POINTER_TYPE_DEFAULT,
                POINTER_TYPE_LEFT,
                POINTER_TYPE_RIGHT,
            })
    @Retention(SOURCE)
    @interface PointerType {}

    // clang-format on

    /**
     * Default pointer type for the source (no handedness). Occurs for SOURCE_UNKNOWN, SOURCE_HEAD,
     * SOURCE_MOUSE, and SOURCE_GAZE_AND_GESTURE.
     */
    int POINTER_TYPE_DEFAULT = 0;

    /**
     * Left hand / controller pointer.. Occurs for SOURCE_CONTROLLER, SOURCE_HANDS, and
     * SOURCE_GAZE_AND_GESTURE.
     */
    int POINTER_TYPE_LEFT = 1;

    /**
     * Right hand / controller pointer.. Occurs for SOURCE_CONTROLLER, SOURCE_HANDS, and
     * SOURCE_GAZE_AND_GESTURE.
     */
    int POINTER_TYPE_RIGHT = 2;

    /** Returns the pointer type of this event. */
    @PointerType
    int getPointerType();

    /** The time this event occurred, in the android.os.SystemClock#uptimeMillis time base. */
    long getTimestamp();

    /** The origin of the ray, in the receiver's task coordinate space. */
    @NonNull
    Vec3 getOrigin();

    /**
     * The direction the ray is pointing in, in the receiver's task coordinate space. Any point
     * along the ray can be represented as origin + d * direction, where d is non-negative.
     */
    @NonNull
    Vec3 getDirection();

    /** Info about a single ray hit. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    interface HitInfo {
        /**
         * ID of the front-end Impress node within the subspace that was hit. Used by Split-Engine
         * to create a handle to the node with the same entity ID. In case the node doesn't belong
         * to a subspace the value will be 0, i.e.,
         * utils::Entity::import(subspaceImpressNodeId).IsNull() == true.
         *
         * <p>ACTION_MOVE, ACTION_UP, and ACTION_CANCEL events will report the same node id as was
         * hit during the initial ACTION_DOWN.
         */
        int getSubspaceImpressNodeId();

        /**
         * The CPM node that was hit.
         *
         * <p>ACTION_MOVE, ACTION_UP, and ACTION_CANCEL events will report the same node as was hit
         * during the initial ACTION_DOWN.
         */
        @NonNull
        Node getInputNode();

        /**
         * The ray hit position, in the receiver's task coordinate space.
         *
         * <p>All events may report the current ray's hit position. This can be null if there no
         * longer is a collision between the ray and the input node (eg, during a drag event).
         */
        @Nullable
        Vec3 getHitPosition();

        /** The matrix transforming task node coordinates into the hit CPM node's coordinates. */
        @NonNull
        Mat4f getTransform();
    }

    /**
     * Info about the first scene node (closest to the ray origin) that was hit by the input ray, if
     * any. This will be null if no node was hit. Note that the hit node remains the same during an
     * ongoing DOWN -> MOVE -> UP action, even if the pointer stops hitting the node during the
     * action.
     */
    @Nullable
    HitInfo getHitInfo();

    /** Info about the second scene node from the same task that was hit, if any. */
    @Nullable
    HitInfo getSecondaryHitInfo();

    /** Event dispatch flags. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @IntDef(value = {DISPATCH_FLAG_NONE, DISPATCH_FLAG_CAPTURED_POINTER, DISPATCH_FLAG_2D})
    @Retention(SOURCE)
    @interface DispatchFlag {}

    // Normal dispatch.
    int DISPATCH_FLAG_NONE = 0;
    // This event was dispatched to this receiver only because pointer capture was enabled.
    int DISPATCH_FLAG_CAPTURED_POINTER = 1;
    // This event was also dispatched as a 2D Android input event.
    int DISPATCH_FLAG_2D = 2;

    /** Returns the dispatch flags for this event. */
    @DispatchFlag
    int getDispatchFlags();

    // clang-format off
    /**
     * Actions similar to Android's MotionEvent actions:
     * https://developer.android.com/reference/android/view/MotionEvent for keeping track of a
     * sequence of events on the same target, e.g., * HOVER_ENTER -> HOVER_MOVE -> HOVER_EXIT * DOWN
     * -> MOVE -> UP
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @IntDef(
            value = {
                ACTION_DOWN,
                ACTION_UP,
                ACTION_MOVE,
                ACTION_CANCEL,
                ACTION_HOVER_MOVE,
                ACTION_HOVER_ENTER,
                ACTION_HOVER_EXIT,
            })
    @Retention(SOURCE)
    @interface Action {}

    // clang-format on

    /** The primary action button or gesture was just pressed / started. */
    int ACTION_DOWN = 0;

    /**
     * The primary action button or gesture was just released / stopped. The hit info represents the
     * node that was originally hit (ie, as provided in the ACTION_DOWN event).
     */
    int ACTION_UP = 1;

    /**
     * The primary action button or gesture was pressed/active in the previous event, and is still
     * pressed/active. The hit info represents the node that was originally hit (ie, as provided in
     * the ACTION_DOWN event). The hit position may be null if the pointer is no longer hitting that
     * node.
     */
    int ACTION_MOVE = 2;

    /**
     * While the primary action button or gesture was held, the pointer was disabled. This happens
     * if you are using controllers and the battery runs out, or if you are using a source that
     * transitions to a new pointer type, eg SOURCE_GAZE_AND_GESTURE.
     */
    int ACTION_CANCEL = 3;

    /**
     * The primary action button or gesture is not pressed, and the pointer ray continued to hit the
     * same node. The hit info represents the node that was hit (may be null if pointer capture is
     * enabled).
     *
     * <p>Hover input events are never provided for sensitive source types.
     */
    int ACTION_HOVER_MOVE = 4;

    /**
     * The primary action button or gesture is not pressed, and the pointer ray started to hit a new
     * node. The hit info represents the node that is being hit (may be null if pointer capture is
     * enabled).
     *
     * <p>Hover input events are never provided for sensitive source types.
     */
    int ACTION_HOVER_ENTER = 5;

    /**
     * The primary action button or gesture is not pressed, and the pointer ray stopped hitting the
     * node that it was previously hitting. The hit info represents the node that was being hit (may
     * be null if pointer capture is enabled).
     *
     * <p>Hover input events are never provided for sensitive source types.
     */
    int ACTION_HOVER_EXIT = 6;

    /** Returns the current action associated with this input event. */
    @Action
    int getAction();
}
