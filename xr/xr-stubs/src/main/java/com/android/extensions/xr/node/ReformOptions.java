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
 * Configuration options for reform (move/resize) UX. To create a ReformOptions instance, call
 * {@code XrExtensions.createReformOptions()}.
 */
@SuppressWarnings({"unchecked", "deprecation", "all"})
public class ReformOptions {

    ReformOptions() {
        throw new RuntimeException("Stub!");
    }

    /** Which reform actions are enabled. */
    public int getEnabledReform() {
        throw new RuntimeException("Stub!");
    }

    /** By default, only ALLOW_MOVE is enabled. */
    public com.android.extensions.xr.node.ReformOptions setEnabledReform(int enabledReform) {
        throw new RuntimeException("Stub!");
    }

    /** Behaviour flags. */
    public int getFlags() {
        throw new RuntimeException("Stub!");
    }

    /** By default, the flags are set to 0. */
    public com.android.extensions.xr.node.ReformOptions setFlags(int flags) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Current size of the content, in meters. This is the local size (does not include any scale
     * factors)
     */
    public com.android.extensions.xr.node.Vec3 getCurrentSize() {
        throw new RuntimeException("Stub!");
    }

    /** By default, the current size is set to (1, 1, 1). */
    public com.android.extensions.xr.node.ReformOptions setCurrentSize(
            com.android.extensions.xr.node.Vec3 currentSize) {
        throw new RuntimeException("Stub!");
    }

    /** Minimum size of the content, in meters. This is a local size. */
    public com.android.extensions.xr.node.Vec3 getMinimumSize() {
        throw new RuntimeException("Stub!");
    }

    /** By default, the minimum size is set to (1, 1, 1). */
    public com.android.extensions.xr.node.ReformOptions setMinimumSize(
            com.android.extensions.xr.node.Vec3 minimumSize) {
        throw new RuntimeException("Stub!");
    }

    /** Maximum size of the content, in meters. This is a local size. */
    public com.android.extensions.xr.node.Vec3 getMaximumSize() {
        throw new RuntimeException("Stub!");
    }

    /** By default, the maximum size is set to (1, 1, 1). */
    public com.android.extensions.xr.node.ReformOptions setMaximumSize(
            com.android.extensions.xr.node.Vec3 maximumSize) {
        throw new RuntimeException("Stub!");
    }

    /** The aspect ratio of the content on resizing. <= 0.0f when there are no preferences. */
    public float getFixedAspectRatio() {
        throw new RuntimeException("Stub!");
    }

    /**
     * The aspect ratio determined by taking the panel's width over its height. An aspect ratio
     * value less than 0 will be ignored. A value <= 0.0f means there are no preferences.
     */
    public com.android.extensions.xr.node.ReformOptions setFixedAspectRatio(
            float fixedAspectRatio) {
        throw new RuntimeException("Stub!");
    }

    /** Returns the current value of forceShowResizeOverlay. */
    public boolean getForceShowResizeOverlay() {
        throw new RuntimeException("Stub!");
    }

    /**
     * If forceShowResizeOverlay is set to true, the resize overlay will always be show (until
     * forceShowResizeOverlay is changed to false). This can be used by apps to implement their own
     * resize affordances.
     */
    public com.android.extensions.xr.node.ReformOptions setForceShowResizeOverlay(
            boolean forceShowResizeOverlay) {
        throw new RuntimeException("Stub!");
    }

    /** Returns the callback that will receive reform events. */
    public com.android.extensions.xr.function.Consumer<com.android.extensions.xr.node.ReformEvent>
            getEventCallback() {
        throw new RuntimeException("Stub!");
    }

    /** Sets the callback that will receive reform events. */
    public com.android.extensions.xr.node.ReformOptions setEventCallback(
            com.android.extensions.xr.function.Consumer<com.android.extensions.xr.node.ReformEvent>
                    callback) {
        throw new RuntimeException("Stub!");
    }

    /** Returns the executor that events will be handled on. */
    public java.util.concurrent.Executor getEventExecutor() {
        throw new RuntimeException("Stub!");
    }

    /** Sets the executor that events will be handled on. */
    public com.android.extensions.xr.node.ReformOptions setEventExecutor(
            java.util.concurrent.Executor executor) {
        throw new RuntimeException("Stub!");
    }

    /** Returns the current value of scaleWithDistanceMode. */
    public int getScaleWithDistanceMode() {
        throw new RuntimeException("Stub!");
    }

    /**
     * If scaleWithDistanceMode is set, and FLAG_SCALE_WITH_DISTANCE is also in use, the scale the
     * system suggests (or automatically applies when FLAG_ALLOW_SYSTEM_MOVEMENT is also in use)
     * follows scaleWithDistanceMode:
     *
     * <p>SCALE_WITH_DISTANCE_MODE_DEFAULT: The panel scales in the same way as home space mode.
     * SCALE_WITH_DISTANCE_MODE_DMM: The panel scales in a way that the user-perceived panel size
     * never changes.
     *
     * <p>When FLAG_SCALE_WITH_DISTANCE is not in use, scaleWithDistanceMode is ignored.
     */
    public com.android.extensions.xr.node.ReformOptions setScaleWithDistanceMode(
            int scaleWithDistanceMode) {
        throw new RuntimeException("Stub!");
    }

    public static final int ALLOW_MOVE = 1; // 0x1

    public static final int ALLOW_RESIZE = 2; // 0x2

    public static final int FLAG_ALLOW_SYSTEM_MOVEMENT = 2; // 0x2

    public static final int FLAG_POSE_RELATIVE_TO_PARENT = 4; // 0x4

    public static final int FLAG_SCALE_WITH_DISTANCE = 1; // 0x1

    public static final int SCALE_WITH_DISTANCE_MODE_DEFAULT = 3; // 0x3

    public static final int SCALE_WITH_DISTANCE_MODE_DMM = 2; // 0x2
}
