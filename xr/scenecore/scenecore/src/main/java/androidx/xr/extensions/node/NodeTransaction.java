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

import static androidx.xr.extensions.XrExtensions.IMAGE_TOO_OLD;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.os.IBinder;
import android.view.SurfaceControl;
import android.view.SurfaceControlViewHost.SurfacePackage;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.xr.extensions.asset.EnvironmentToken;
import androidx.xr.extensions.asset.GltfAnimation;
import androidx.xr.extensions.asset.GltfModelToken;
import androidx.xr.extensions.asset.SceneToken;
import androidx.xr.extensions.passthrough.PassthroughState;
import androidx.xr.extensions.subspace.Subspace;

import java.io.Closeable;
import java.lang.annotation.Retention;

/**
 * An atomic set of changes to apply to a set of {@link Node}s.
 *
 * <p>Note that {@link Node} uses a right-hand coordinate system, i.e. +X points to the right, +Y
 * up, and +Z points towards the camera.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface NodeTransaction extends Closeable {
    /**
     * Sets a name for the node that is used to it in `adb dumpsys cpm` output log.
     *
     * <p>While the name does not have to be globally unique, it is recommended to set a unique name
     * for each node for ease of debugging.
     *
     * @param node The node to be updated.
     * @param name The debug name of the node.
     * @return The reference to this {@link NodeTransaction} object that is currently being updated.
     */
    default @NonNull NodeTransaction setName(@NonNull Node node, @NonNull String name) {
        throw new UnsupportedOperationException(IMAGE_TOO_OLD);
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
     * @return The reference to this {@link NodeTransaction} object that is currently being updated.
     */
    default @NonNull NodeTransaction setParent(@NonNull Node node, @Nullable Node parent) {
        throw new UnsupportedOperationException(IMAGE_TOO_OLD);
    }

    /**
     * Sets the position of the node in the local coordinate space (parent space).
     *
     * @param node The node to be updated.
     * @param x The 'x' distance in meters from parent's origin.
     * @param y The 'y' distance in meters from parent's origin.
     * @param z The 'z' distance in meters from parent's origin.
     * @return The reference to this {@link NodeTransaction} object that is currently being updated.
     */
    default @NonNull NodeTransaction setPosition(@NonNull Node node, float x, float y, float z) {
        throw new UnsupportedOperationException(IMAGE_TOO_OLD);
    }

    /**
     * Rotates the node by the quaternion specified by x, y, z, and w components in the local
     * coordinate space.
     *
     * @param node The node to be updated.
     * @param x The x component of the quaternion.
     * @param y The y component of the quaternion.
     * @param z The z component of the quaternion.
     * @param w The w component of the quaternion.
     * @return The reference to this {@link NodeTransaction} object that is currently being updated.
     */
    default @NonNull NodeTransaction setOrientation(
            @NonNull Node node, float x, float y, float z, float w) {
        throw new UnsupportedOperationException(IMAGE_TOO_OLD);
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
     * @return The reference to this {@link NodeTransaction} object that is currently being updated.
     */
    default @NonNull NodeTransaction setScale(@NonNull Node node, float sx, float sy, float sz) {
        throw new UnsupportedOperationException(IMAGE_TOO_OLD);
    }

    /**
     * Sets the opacity of the node's content to a value between [0..1].
     *
     * @param node The node to be updated.
     * @param value The new opacity amount in range of [0..1].
     * @return The reference to this {@link NodeTransaction} object that is currently being updated.
     */
    default @NonNull NodeTransaction setAlpha(@NonNull Node node, float value) {
        throw new UnsupportedOperationException(IMAGE_TOO_OLD);
    }

    /**
     * Changes the visibility of the node and its content.
     *
     * @param node The node to be updated.
     * @param isVisible Whether the node is visible.
     * @return The reference to this {@link NodeTransaction} object that is currently being updated.
     */
    default @NonNull NodeTransaction setVisibility(@NonNull Node node, boolean isVisible) {
        throw new UnsupportedOperationException(IMAGE_TOO_OLD);
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
     * @return The reference to this {@link NodeTransaction} object that is currently being updated.
     */
    default @NonNull NodeTransaction setSurfaceControl(
            @Nullable Node node, @NonNull SurfaceControl surfaceControl) {
        throw new UnsupportedOperationException(IMAGE_TOO_OLD);
    }

    /**
     * Configures the node to host and control the given surface data.
     *
     * <p>This method is similar to {@link #setSurfaceControl(Node, SurfaceControl)} and is provided
     * for convenience.
     *
     * @param node The node to be updated.
     * @param surfacePackage The package that contains the {@link SurfaceControl}, or 'null' to
     *     disassociate the currently hosted surface from the node.
     * @return The reference to this {@link NodeTransaction} object that is currently being updated.
     */
    default @NonNull NodeTransaction setSurfacePackage(
            @Nullable Node node, @NonNull SurfacePackage surfacePackage) {
        throw new UnsupportedOperationException(IMAGE_TOO_OLD);
    }

    /**
     * Crops the 2D buffer of the Surface hosted by this node to match the given bounds in pixels.
     *
     * <p>This method only applies to nodes that host a {@link SurfaceControl} set by {@link
     * #setSurfaceControl}.
     *
     * @param surfaceControl The on-screen surface.
     * @param widthPx The width of the surface in pixels.
     * @param heightPx The height of the surface in pixels.
     * @return The reference to this {@link NodeTransaction} object that is currently being updated.
     */
    default @NonNull NodeTransaction setWindowBounds(
            @NonNull SurfaceControl surfaceControl, int widthPx, int heightPx) {
        throw new UnsupportedOperationException(IMAGE_TOO_OLD);
    }

    /**
     * Crops the 2D buffer of the Surface hosted by this node to match the given bounds in pixels.
     *
     * <p>This method is similar to {@link #setWindowBounds(SurfaceControl, int, int)} and is
     * provided for convenience.
     *
     * @param surfacePackage The package that contains the {@link SurfaceControl}.
     * @param widthPx The width of the surface in pixels.
     * @param heightPx The height of the surface in pixels.
     * @return The reference to this {@link NodeTransaction} object that is currently being updated.
     */
    @NonNull
    default NodeTransaction setWindowBounds(
            @NonNull SurfacePackage surfacePackage, int widthPx, int heightPx) {
        throw new UnsupportedOperationException(IMAGE_TOO_OLD);
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
     * @return The reference to this {@link NodeTransaction} object that is currently being updated.
     * @deprecated Use Split Engine to create a curved panel.
     */
    @Deprecated
    default @NonNull NodeTransaction setCurvature(@NonNull Node node, float curvature) {
        throw new UnsupportedOperationException(IMAGE_TOO_OLD);
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
     * @return The reference to this {@link NodeTransaction} object that is currently being updated.
     */
    default @NonNull NodeTransaction setPixelResolution(@NonNull Node node, float pixelsPerMeter) {
        throw new UnsupportedOperationException(IMAGE_TOO_OLD);
    }

    // clang-format off
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @IntDef(
            flag = true,
            value = {
                X_POSITION_IN_PIXELS,
                Y_POSITION_IN_PIXELS,
                Z_POSITION_IN_PIXELS,
                POSITION_FROM_PARENT_TOP_LEFT,
            })
    @Retention(SOURCE)
    public @interface PixelPositionFlags {}

    // clang-format on

    int X_POSITION_IN_PIXELS = 0x01;
    int Y_POSITION_IN_PIXELS = 0x02;
    int Z_POSITION_IN_PIXELS = 0x04;
    // POSITION_FROM_PARENT_TOP_LEFT makes it so the node's position is relative to the top left
    // corner of the parent node, instead of the center. Only relevant if the parent node has a size
    // (currently this is only true for surface tracking nodes).
    int POSITION_FROM_PARENT_TOP_LEFT = 0x40;

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
     * @return The reference to this {@link NodeTransaction} object that is currently being updated.
     */
    default @NonNull NodeTransaction setPixelPositioning(
            @NonNull Node node, @PixelPositionFlags int pixelPositionFlags) {
        throw new UnsupportedOperationException(IMAGE_TOO_OLD);
    }

    /**
     * Renders a previously loaded glTF model.
     *
     * <p>The token must belong to a previously loaded glTF model that is currently cached in the
     * SpaceFlinger.
     *
     * @param node The node to be updated.
     * @param gltfModelToken The token of a glTF model that was previously loaded.
     * @return The reference to this {@link NodeTransaction} object that is currently being updated.
     * @deprecated JXR Core doesn't need this anymore as it does the same with Split Engine.
     */
    @Deprecated
    default @NonNull NodeTransaction setGltfModel(
            @NonNull Node node, @NonNull GltfModelToken gltfModelToken) {
        throw new UnsupportedOperationException(IMAGE_TOO_OLD);
    }

    /**
     * Renders a previously loaded environment.
     *
     * <p>The token must belong to a previously loaded environment that is currently cached in the
     * SpaceFlinger.
     *
     * @param node The node to be updated.
     * @param environmentToken The token of an environment that was previously loaded.
     * @return The reference to this {@link NodeTransaction} object that is currently being updated.
     * @deprecated JXR Core doesn't need this anymore as it does the same with Split Engine.
     */
    @Deprecated
    default @NonNull NodeTransaction setEnvironment(
            @NonNull Node node, @NonNull EnvironmentToken environmentToken) {
        throw new UnsupportedOperationException(IMAGE_TOO_OLD);
    }

    /**
     * Renders a previously loaded Impress scene.
     *
     * <p>The token must belong to a previously loaded Impress scene that is currently cached in the
     * SpaceFlinger.
     *
     * @param node The node to be updated.
     * @param sceneToken The token of an Impress scene that was previously loaded.
     * @return The reference to this {@link NodeTransaction} object that is currently being updated.
     * @deprecated JXR Core doesn't need this anymore as it does the same with Split Engine.
     */
    @Deprecated
    default @NonNull NodeTransaction setImpressScene(
            @NonNull Node node, @NonNull SceneToken sceneToken) {
        throw new UnsupportedOperationException(IMAGE_TOO_OLD);
    }

    /**
     * Animates a previously loaded glTF model.
     *
     * @param node The node to be updated.
     * @param gltfAnimationName The name of the glTF animation.
     * @param gltfAnimationState The {@link GltfAnimation.State} state of the glTF animation.
     * @return The reference to this {@link NodeTransaction} object that is currently being updated.
     * @deprecated JXR Core doesn't need this anymore as it does the same with Split Engine.
     */
    @Deprecated
    default @NonNull NodeTransaction setGltfAnimation(
            @NonNull Node node,
            @NonNull String gltfAnimationName,
            @NonNull GltfAnimation.State gltfAnimationState) {
        throw new UnsupportedOperationException(IMAGE_TOO_OLD);
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
     * @return The reference to this {@link NodeTransaction} object that is currently being updated.
     */
    default @NonNull NodeTransaction setAnchorId(@NonNull Node node, @Nullable IBinder anchorId) {
        throw new UnsupportedOperationException(IMAGE_TOO_OLD);
    }

    /**
     * Sets a subspace to be used.
     *
     * @param node The node to be updated.
     * @param subspace The previously created subspace to be associated with the node.
     * @return The reference to this {@link NodeTransaction} object that is currently being updated.
     */
    default @NonNull NodeTransaction setSubspace(@NonNull Node node, @NonNull Subspace subspace) {
        throw new UnsupportedOperationException(IMAGE_TOO_OLD);
    }

    /**
     * Updates the passthrough state.
     *
     * @param node The node to be updated.
     * @param passthroughOpacity The opacity of the passthrough layer where 0.0 means no passthrough
     *     and 1.0 means full passthrough.
     * @param passthroughMode The {@link PassthroughState.Mode} mode that the passthrough will use.
     * @return The reference to this {@link NodeTransaction} object that is currently being updated.
     */
    default @NonNull NodeTransaction setPassthroughState(
            @NonNull Node node,
            float passthroughOpacity,
            @PassthroughState.Mode int passthroughMode) {
        throw new UnsupportedOperationException(IMAGE_TOO_OLD);
    }

    /**
     * Enables reform UX for a node.
     *
     * @param node The node to be updated.
     * @param options Configuration options for the reform UX.
     * @return The reference to this {@link NodeTransaction} object that is currently being updated.
     */
    default @NonNull NodeTransaction enableReform(
            @NonNull Node node, @NonNull ReformOptions options) {
        throw new UnsupportedOperationException(IMAGE_TOO_OLD);
    }

    /**
     * Updates the size of the reform UX.
     *
     * @param node The node to be updated.
     * @param reformSize The new size in meters that should be used to lay out the reform UX.
     * @return The reference to this {@link NodeTransaction} object that is currently being updated.
     */
    default @NonNull NodeTransaction setReformSize(@NonNull Node node, @NonNull Vec3 reformSize) {
        throw new UnsupportedOperationException(IMAGE_TOO_OLD);
    }

    /**
     * Disables reform UX for a node.
     *
     * @param node The node to be updated.
     * @return The reference to this {@link NodeTransaction} object that is currently being updated.
     */
    default @NonNull NodeTransaction disableReform(@NonNull Node node) {
        throw new UnsupportedOperationException(IMAGE_TOO_OLD);
    }

    /**
     * Sets the corner radius for 2D surfaces under this node.
     *
     * <p>The corner radius is propagated to child nodes.
     *
     * @param node The node to be updated.
     * @param cornerRadius The corner radius for 2D surfaces under this node, in meters.
     * @return The reference to this {@link NodeTransaction} object that is currently being updated.
     */
    default @NonNull NodeTransaction setCornerRadius(@NonNull Node node, float cornerRadius) {
        throw new UnsupportedOperationException(IMAGE_TOO_OLD);
    }

    /**
     * Removes the corner radius from this node.
     *
     * @param node The node to be updated.
     * @return The reference to this {@link NodeTransaction} object that is currently being updated.
     */
    default @NonNull NodeTransaction removeCornerRadius(@NonNull Node node) {
        throw new UnsupportedOperationException(IMAGE_TOO_OLD);
    }

    /**
     * Merges the given transaction into this one so that they can be submitted together to the
     * system. All of the changes in the other transaction are moved into this one; the other
     * transaction is left in an empty state.
     *
     * @return The reference to this {@link NodeTransaction} object that is currently being updated.
     */
    default @NonNull NodeTransaction merge(@NonNull NodeTransaction other) {
        throw new UnsupportedOperationException(IMAGE_TOO_OLD);
    }

    /**
     * Submits the queued transactions to backend.
     *
     * <p>This method will clear the existing transaction state so the same transaction object can
     * be used for the next set of updates.
     */
    default void apply() {
        throw new UnsupportedOperationException(IMAGE_TOO_OLD);
    }

    /**
     * Closes and releases the native transaction object without applying it.
     *
     * <p>Note that a closed transaction cannot be used again.
     */
    @Override
    default void close() {
        throw new UnsupportedOperationException(IMAGE_TOO_OLD);
    }
}
