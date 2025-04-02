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
 * An atomic set of changes to apply to a set of {@link com.android.extensions.xr.node.Node Node}s.
 *
 * <p>Note that {@link com.android.extensions.xr.node.Node Node} uses a right-hand coordinate
 * system, i.e. +X points to the right, +Y up, and +Z points towards the camera.
 */
@SuppressWarnings({"unchecked", "deprecation", "all"})
public class NodeTransaction implements java.io.Closeable {

    NodeTransaction() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Sets a name for the node that is used to it in `adb dumpsys cpm` output log.
     *
     * <p>While the name does not have to be globally unique, it is recommended to set a unique name
     * for each node for ease of debugging.
     *
     * @param node The node to be updated.
     * @param name The debug name of the node.
     * @return The reference to this {@link com.android.extensions.xr.node.NodeTransaction
     *     NodeTransaction} object that is currently being updated.
     */
    public com.android.extensions.xr.node.NodeTransaction setName(
            com.android.extensions.xr.node.Node node, java.lang.String name) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Sets the parent of this node to the given node.
     *
     * <p>This method detaches the node from its current branch and moves into the new parent's
     * hierarchy (if any). If parent parameter is `null`, the node will be orphaned and removed from
     * the rendering tree until it is reattached to another node that is in the root hierarchy.
     *
     * @param node The node to be updated.
     * @param parent The new parent of the node or `null` if the node is to be removed from the
     *     rendering tree.
     * @return The reference to this {@link com.android.extensions.xr.node.NodeTransaction
     *     NodeTransaction} object that is currently being updated.
     */
    public com.android.extensions.xr.node.NodeTransaction setParent(
            com.android.extensions.xr.node.Node node, com.android.extensions.xr.node.Node parent) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Sets the position of the node in the local coordinate space (parent space).
     *
     * @param node The node to be updated.
     * @param x The 'x' distance in meters from parent's origin.
     * @param y The 'y' distance in meters from parent's origin.
     * @param z The 'z' distance in meters from parent's origin.
     * @return The reference to this {@link com.android.extensions.xr.node.NodeTransaction
     *     NodeTransaction} object that is currently being updated.
     */
    public com.android.extensions.xr.node.NodeTransaction setPosition(
            com.android.extensions.xr.node.Node node, float x, float y, float z) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Rotates the node by the quaternion specified by x, y, z, and w components in the local
     * coordinate space.
     *
     * @param node The node to be updated.
     * @return The reference to this {@link com.android.extensions.xr.node.NodeTransaction
     *     NodeTransaction} object that is currently being updated.
     */
    public com.android.extensions.xr.node.NodeTransaction setOrientation(
            com.android.extensions.xr.node.Node node, float x, float y, float z, float w) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Scales the node along the x, y, and z axis in the local coordinate space.
     *
     * <p>For 2D panels, this method scales the panel in the world, increasing its visual size
     * without changing the buffer size. It will not trigger a relayout and will not affect its
     * enclosing view's layout configuration.
     *
     * @param node The node to be updated.
     * @param sx The scaling factor along the x-axis.
     * @param sy The scaling factor along the y-axis.
     * @param sz The scaling factor along the z-axis.
     * @return The reference to this {@link com.android.extensions.xr.node.NodeTransaction
     *     NodeTransaction} object that is currently being updated.
     */
    public com.android.extensions.xr.node.NodeTransaction setScale(
            com.android.extensions.xr.node.Node node, float sx, float sy, float sz) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Sets the opacity of the node's content to a value between [0..1].
     *
     * @param value The new opacity amount in range of [0..1].
     * @return The reference to this {@link com.android.extensions.xr.node.NodeTransaction
     *     NodeTransaction} object that is currently being updated.
     */
    public com.android.extensions.xr.node.NodeTransaction setAlpha(
            com.android.extensions.xr.node.Node node, float value) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Changes the visibility of the node and its content.
     *
     * @param isVisible Whether the node is visible.
     * @return The reference to this {@link com.android.extensions.xr.node.NodeTransaction
     *     NodeTransaction} object that is currently being updated.
     */
    public com.android.extensions.xr.node.NodeTransaction setVisibility(
            com.android.extensions.xr.node.Node node, boolean isVisible) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Configures the node to host and control the given surface data.
     *
     * <p>Passing a 'null' for surfaceControl parameter will disassociate it from the node, so the
     * same node can be used to host another surface or volume data.
     *
     * @param node The node to be updated.
     * @param surfaceControl Handle to an on-screen surface managed by the system compositor, or
     *     'null' to disassociate the currently hosted surface from the node.
     * @return The reference to this {@link com.android.extensions.xr.node.NodeTransaction
     *     NodeTransaction} object that is currently being updated.
     */
    public com.android.extensions.xr.node.NodeTransaction setSurfaceControl(
            com.android.extensions.xr.node.Node node, android.view.SurfaceControl surfaceControl) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Configures the node to host and control the given surface data.
     *
     * <p>This method is similar to {@link
     * #setSurfaceControl(com.android.extensions.xr.node.Node,android.view.SurfaceControl)} and is
     * provided for convenience.
     *
     * @param node The node to be updated.
     * @param surfacePackage The package that contains the {@link android.view.SurfaceControl
     *     SurfaceControl}, or 'null' to disassociate the currently hosted surface from the node.
     * @return The reference to this {@link com.android.extensions.xr.node.NodeTransaction
     *     NodeTransaction} object that is currently being updated.
     */
    public com.android.extensions.xr.node.NodeTransaction setSurfacePackage(
            com.android.extensions.xr.node.Node node,
            android.view.SurfaceControlViewHost.SurfacePackage surfacePackage) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Crops the 2D buffer of the Surface hosted by this node to match the given bounds in pixels.
     *
     * <p>This method only applies to nodes that host a {@link android.view.SurfaceControl
     * SurfaceControl} set by {@link #setSurfaceControl}.
     *
     * @param surfaceControl The on-screen surface.
     * @param widthPx The width of the surface in pixels.
     * @param heightPx The height of the surface in pixels.
     * @return The reference to this {@link com.android.extensions.xr.node.NodeTransaction
     *     NodeTransaction} object that is currently being updated.
     */
    public com.android.extensions.xr.node.NodeTransaction setWindowBounds(
            android.view.SurfaceControl surfaceControl, int widthPx, int heightPx) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Crops the 2D buffer of the Surface hosted by this node to match the given bounds in pixels.
     *
     * <p>This method is similar to {@link #setWindowBounds(android.view.SurfaceControl,int,int)}
     * and is provided for convenience.
     *
     * @param surfacePackage The package that contains the {@link android.view.SurfaceControl
     *     SurfaceControl}.
     * @param widthPx The width of the surface in pixels.
     * @param heightPx The height of the surface in pixels.
     * @return The reference to this {@link com.android.extensions.xr.node.NodeTransaction
     *     NodeTransaction} object that is currently being updated.
     */
    public com.android.extensions.xr.node.NodeTransaction setWindowBounds(
            android.view.SurfaceControlViewHost.SurfacePackage surfacePackage,
            int widthPx,
            int heightPx) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Curves the XY plane of the node around the y-axis and towards the positive z-axis.
     *
     * <p>This method essentially curves the x-axis of the node, moving and rotating its children to
     * align with the new x-axis shape. It will also curve the children's x-axes in a similar
     * manner.
     *
     * <p>If this node is hosting a 2D panel, setting a curvature will bend the panel along the Y
     * axis, projecting it onto a cylinder defined by the given radius.
     *
     * <p>To remove the curvature, set the radius to 0.
     *
     * @param node The node to be updated.
     * @param curvature A positive value equivalent to 1/radius, where 'radius' represents the
     *     radial distance of the polar coordinate system that is used to curve the x-axis. Setting
     *     this value to 0 will straighten the axis and remove its curvature.
     * @return The reference to this {@link com.android.extensions.xr.node.NodeTransaction
     *     NodeTransaction} object that is currently being updated.
     * @deprecated Use Split Engine to create a curved panel.
     */
    @Deprecated
    public com.android.extensions.xr.node.NodeTransaction setCurvature(
            com.android.extensions.xr.node.Node node, float curvature) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Sets the resolution of 2D surfaces under this node.
     *
     * <p>The sizes of 2D surfaces under this node will be set according to their 2D pixel
     * dimensions and the pixelsPerMeter value. The pixelsPerMeter value is propagated to child
     * nodes.
     *
     * @param node The node to be updated.
     * @param pixelsPerMeter The number of pixels per meter to use when sizing 2D surfaces.
     * @return The reference to this {@link com.android.extensions.xr.node.NodeTransaction
     *     NodeTransaction} object that is currently being updated.
     */
    public com.android.extensions.xr.node.NodeTransaction setPixelResolution(
            com.android.extensions.xr.node.Node node, float pixelsPerMeter) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Sets whether position is interpreted in meters or in pixels for each dimension.
     *
     * <p>The sizes of 2D surfaces under this node will be set according to their 2D pixel
     * dimensions and the pixelsPerMeter value. The pixelsPerMeter value is propagated to child
     * nodes.
     *
     * @param node The node to be updated.
     * @param pixelPositionFlags Flags indicating which dimensins of the local position of the node
     *     should be interpreted as pixel values (as opposed to the default meters).
     * @return The reference to this {@link com.android.extensions.xr.node.NodeTransaction
     *     NodeTransaction} object that is currently being updated.
     */
    public com.android.extensions.xr.node.NodeTransaction setPixelPositioning(
            com.android.extensions.xr.node.Node node, int pixelPositionFlags) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Renders a previously loaded glTF model.
     *
     * <p>The token must belong to a previously loaded glTF model that is currently cached in the
     * SpaceFlinger.
     *
     * @param node The node to be updated.
     * @param gltfModelToken The token of a glTF model that was previously loaded.
     * @return The reference to this {@link com.android.extensions.xr.node.NodeTransaction
     *     NodeTransaction} object that is currently being updated.
     * @deprecated JXR Core doesn't need this anymore as it does the same with Split Engine.
     */
    @Deprecated
    public com.android.extensions.xr.node.NodeTransaction setGltfModel(
            com.android.extensions.xr.node.Node node,
            com.android.extensions.xr.asset.GltfModelToken gltfModelToken) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Renders a previously loaded environment.
     *
     * <p>The token must belong to a previously loaded environment that is currently cached in the
     * SpaceFlinger.
     *
     * @param node The node to be updated.
     * @param environmentToken The token of an environment that was previously loaded.
     * @return The reference to this {@link com.android.extensions.xr.node.NodeTransaction
     *     NodeTransaction} object that is currently being updated.
     * @deprecated JXR Core doesn't need this anymore as it does the same with Split Engine.
     */
    @Deprecated
    public com.android.extensions.xr.node.NodeTransaction setEnvironment(
            com.android.extensions.xr.node.Node node,
            com.android.extensions.xr.asset.EnvironmentToken environmentToken) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Renders a previously loaded Impress scene.
     *
     * <p>The token must belong to a previously loaded Impress scene that is currently cached in the
     * SpaceFlinger.
     *
     * @param node The node to be updated.
     * @param sceneToken The token of an Impress scene that was previously loaded.
     * @return The reference to this {@link com.android.extensions.xr.node.NodeTransaction
     *     NodeTransaction} object that is currently being updated.
     * @deprecated JXR Core doesn't need this anymore as it does the same with Split Engine.
     */
    @Deprecated
    public com.android.extensions.xr.node.NodeTransaction setImpressScene(
            com.android.extensions.xr.node.Node node,
            com.android.extensions.xr.asset.SceneToken sceneToken) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Animates a previously loaded glTF model.
     *
     * @param node The node to be updated.
     * @param gltfAnimationName The name of the glTF animation.
     * @param gltfAnimationState The {@link com.android.extensions.xr.asset.GltfAnimation.State
     *     GltfAnimation.State} state of the glTF animation.
     * @return The reference to this {@link com.android.extensions.xr.node.NodeTransaction
     *     NodeTransaction} object that is currently being updated.
     * @deprecated JXR Core doesn't need this anymore as it does the same with Split Engine.
     */
    @Deprecated
    public com.android.extensions.xr.node.NodeTransaction setGltfAnimation(
            com.android.extensions.xr.node.Node node,
            java.lang.String gltfAnimationName,
            com.android.extensions.xr.asset.GltfAnimation.State gltfAnimationState) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Sets the transform of the node on a per-frame basis from a previously created anchor.
     *
     * <p>The client who created the anchor and provided the ID will always remain the owner of the
     * anchor.
     *
     * <p>Modifying the transform of the node will only be applied if or when the anchor is no
     * longer linked to the node, or if the anchor is no longer locatable.
     *
     * <p>A node can be unlinked from an anchor by setting the ID to null. Note that this does not
     * destroy the actual anchor.
     *
     * @param node The node to be updated.
     * @param anchorId The ID of a previously created anchor.
     * @return The reference to this {@link com.android.extensions.xr.node.NodeTransaction
     *     NodeTransaction} object that is currently being updated.
     */
    public com.android.extensions.xr.node.NodeTransaction setAnchorId(
            com.android.extensions.xr.node.Node node, android.os.IBinder anchorId) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Sets a subspace to be used.
     *
     * @param node The node to be updated.
     * @param subspace The previously created subspace to be associated with the node.
     * @return The reference to this {@link com.android.extensions.xr.node.NodeTransaction
     *     NodeTransaction} object that is currently being updated.
     */
    public com.android.extensions.xr.node.NodeTransaction setSubspace(
            com.android.extensions.xr.node.Node node,
            com.android.extensions.xr.subspace.Subspace subspace) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Updates the passthrough state.
     *
     * @param node The node to be updated.
     * @param passthroughOpacity The opacity of the passthrough layer where 0.0 means no passthrough
     *     and 1.0 means full passthrough.
     * @param passthroughMode The {@link com.android.extensions.xr.passthrough.PassthroughState.Mode
     *     PassthroughState.Mode} mode that the passthrough will use.
     * @return The reference to this {@link com.android.extensions.xr.node.NodeTransaction
     *     NodeTransaction} object that is currently being updated.
     */
    public com.android.extensions.xr.node.NodeTransaction setPassthroughState(
            com.android.extensions.xr.node.Node node,
            float passthroughOpacity,
            int passthroughMode) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Enables reform UX for a node.
     *
     * @param node The node to be updated.
     * @param options Configuration options for the reform UX.
     * @return The reference to this {@link com.android.extensions.xr.node.NodeTransaction
     *     NodeTransaction} object that is currently being updated.
     */
    public com.android.extensions.xr.node.NodeTransaction enableReform(
            com.android.extensions.xr.node.Node node,
            com.android.extensions.xr.node.ReformOptions options) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Updates the size of the reform UX.
     *
     * @param node The node to be updated.
     * @param reformSize The new size in meters that should be used to lay out the reform UX.
     * @return The reference to this {@link com.android.extensions.xr.node.NodeTransaction
     *     NodeTransaction} object that is currently being updated.
     */
    public com.android.extensions.xr.node.NodeTransaction setReformSize(
            com.android.extensions.xr.node.Node node,
            com.android.extensions.xr.node.Vec3 reformSize) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Disables reform UX for a node.
     *
     * @param node The node to be updated.
     * @return The reference to this {@link com.android.extensions.xr.node.NodeTransaction
     *     NodeTransaction} object that is currently being updated.
     */
    public com.android.extensions.xr.node.NodeTransaction disableReform(
            com.android.extensions.xr.node.Node node) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Sets the corner radius for 2D surfaces under this node.
     *
     * <p>The corner radius is propagated to child nodes.
     *
     * @param node The node to be updated.
     * @param cornerRadius The corner radius for 2D surfaces under this node, in meters.
     * @return The reference to this {@link com.android.extensions.xr.node.NodeTransaction
     *     NodeTransaction} object that is currently being updated.
     */
    public com.android.extensions.xr.node.NodeTransaction setCornerRadius(
            com.android.extensions.xr.node.Node node, float cornerRadius) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Removes the corner radius from this node.
     *
     * @param node The node to be updated.
     * @return The reference to this {@link com.android.extensions.xr.node.NodeTransaction
     *     NodeTransaction} object that is currently being updated.
     */
    public com.android.extensions.xr.node.NodeTransaction removeCornerRadius(
            com.android.extensions.xr.node.Node node) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Merges the given transaction into this one so that they can be submitted together to the
     * system. All of the changes in the other transaction are moved into this one; the other
     * transaction is left in an empty state.
     *
     * @return The reference to this {@link com.android.extensions.xr.node.NodeTransaction
     *     NodeTransaction} object that is currently being updated.
     */
    public com.android.extensions.xr.node.NodeTransaction merge(
            com.android.extensions.xr.node.NodeTransaction other) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Submits the queued transactions to backend.
     *
     * <p>This method will clear the existing transaction state so the same transaction object can
     * be used for the next set of updates.
     */
    public void apply() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Closes and releases the native transaction object without applying it.
     *
     * <p>Note that a closed transaction cannot be used again.
     */
    public void close() {
        throw new RuntimeException("Stub!");
    }

    public static final int POSITION_FROM_PARENT_TOP_LEFT = 64; // 0x40

    public static final int X_POSITION_IN_PIXELS = 1; // 0x1

    public static final int Y_POSITION_IN_PIXELS = 2; // 0x2

    public static final int Z_POSITION_IN_PIXELS = 4; // 0x4
}
