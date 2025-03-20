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


/**
 * Handle to a node in the SpaceFlinger scene graph that can also host a 2D Panel or 3D subspace.
 *
 * <p>A Node by itself does not have any visual representation. It merely defines a local space in
 * its parent space. However, a node can also host a single 2D panel or 3D subspace. Once an element
 * is hosted, the node must be attached to the rest of scene graph hierarchy for the element become
 * visible and appear on-screen.
 *
 * <p>Note that {@link com.android.extensions.xr.node.Node Node} uses a right-hand coordinate
 * system, i.e. +X points to the right, +Y up, and +Z points towards the camera.
 */
@SuppressWarnings({"unchecked", "deprecation", "all"})
public class Node implements android.os.Parcelable {

    Node() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Begins listening for 6DOF input events on this Node, and any descendant Nodes that do not
     * have their own event listener set. The event listener is called on the provided Executor.
     * Calling this method replaces any existing event listener for this node.
     */
    public void listenForInput(
            com.android.extensions.xr.function.Consumer<com.android.extensions.xr.node.InputEvent>
                    listener,
            java.util.concurrent.Executor executor) {
        throw new RuntimeException("Stub!");
    }

    /** Removes the listener for 6DOF input events from this Node. */
    public void stopListeningForInput() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Sets the focus target for non-pointer input (eg, keyboard events) when this Node is clicked.
     * The new target is the focusTarget's underlying View Root.
     */
    public void setNonPointerFocusTarget(android.view.AttachedSurfaceControl focusTarget) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Requests pointer capture. All XR input events that hit this node or any of its children are
     * delivered as normal; any other input events that would otherwise be dispatched elsewhere will
     * instead be delivered to the input queue of this node (without hit info).
     *
     * <p>The stateCallback is called immediately with the current state of this pointer capture.
     * Whenever this node is visible and a descendant of a task that is not bounded (is in FSM or
     * overlay space), pointer capture will be active; otherwise it will be paused.
     *
     * <p>If pointer capture is explicitly stopped by a new call to requestPointerCapture() on the
     * same node, or by a call to stopPointerCapture(), POINTER_CAPTURE_STATE_STOPPED is passed (and
     * the stateCallback will not be called subsequently; also, the app can be sure that no more
     * captured pointer events will be delivered based on that request). This also occurs if the
     * node is destroyed without explicitly stopping pointer capture, or if a new call to
     * requestPointerCapture() is made on the same node without stopping the previous request.
     *
     * <p>If there are multiple pointer capture requests (eg from other apps) that could be active
     * at the same time, the most recently requested one is activated; all other requests stay
     * paused.
     *
     * <p>There can only be a single request per Node. If a new requestPointerCapture() call is made
     * on the same node without stopping the previous pointer capture request, the previous request
     * is automatically stopped.
     *
     * @param stateCallback a callback that will be called when pointer capture state changes.
     * @param executor the executor the callback will be called on.
     */
    public void requestPointerCapture(
            com.android.extensions.xr.function.Consumer<java.lang.Integer> stateCallback,
            java.util.concurrent.Executor executor) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Disables previously-requested pointer capture on this node. The stateCallback callback will
     * be called with POINTER_CAPTURE_STOPPED.
     */
    public void stopPointerCapture() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Subscribes to the transform of this node, relative to the OpenXR reference space used as
     * world space for the shared scene. See {@code XrExtensions.getOpenXrWorldSpaceType()}. The
     * provided matrix transforms a point in this node's local coordinate system into a point in
     * world space coordinates. For example, {@code NodeTransform.getTransform()} * (0, 0, 0, 1) is
     * the position of this node in world space. The first non-null transform will be returned
     * immediately after the subscription set-up is complete. Note that the returned closeable must
     * be closed by calling {@code close()} to prevent wasting system resources associated with the
     * subscription.
     *
     * @param transformCallback a callback that will be called when this node's transform changes.
     * @param executor the executor the callback will be called on.
     * @return a Closeable that must be used to cancel the subscription by calling {@code close()}.
     */
    public java.io.Closeable subscribeToTransform(
            com.android.extensions.xr.function.Consumer<
                            com.android.extensions.xr.node.NodeTransform>
                    transformCallback,
            java.util.concurrent.Executor executor) {
        throw new RuntimeException("Stub!");
    }

    /** A no-op override. */
    public int describeContents() {
        throw new RuntimeException("Stub!");
    }

    /** Writes the Node to a Parcel. */
    public void writeToParcel(android.os.Parcel out, int flags) {
        throw new RuntimeException("Stub!");
    }

    /** toString() */
    public java.lang.String toString() {
        throw new RuntimeException("Stub!");
    }

    /** equals() */
    public boolean equals(java.lang.Object object) {
        throw new RuntimeException("Stub!");
    }

    /** hashCode() */
    public int hashCode() {
        throw new RuntimeException("Stub!");
    }

    public static final int POINTER_CAPTURE_STATE_ACTIVE = 1; // 0x1

    public static final int POINTER_CAPTURE_STATE_PAUSED = 0; // 0x0

    public static final int POINTER_CAPTURE_STATE_STOPPED = 2; // 0x2
}
