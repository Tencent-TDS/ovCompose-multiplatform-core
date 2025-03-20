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

package com.android.extensions.xr.space;


/**
 * Defines the panel in XR scene to support embedding activities within a host activity.
 *
 * <p>When the host activity is destroyed, all the activities in its embedded {@link
 * com.android.extensions.xr.space.ActivityPanel ActivityPanel} will also be destroyed.
 */
@SuppressWarnings({"unchecked", "deprecation", "all"})
public class ActivityPanel {

    ActivityPanel() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Launches an activity into this panel.
     *
     * @param intent the {@link android.content.Intent Intent} to start.
     * @param options additional options for how the Activity should be started.
     */
    public void launchActivity(android.content.Intent intent, android.os.Bundle options) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Moves an existing activity into this panel.
     *
     * @param activity the {@link android.app.Activity Activity} to move.
     */
    public void moveActivity(android.app.Activity activity) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Gets the node associated with this {@link com.android.extensions.xr.space.ActivityPanel
     * ActivityPanel}.
     *
     * <p>The {@link com.android.extensions.xr.space.ActivityPanel ActivityPanel} can only be shown
     * to the user after this node is attached to the host activity's scene.
     *
     * @see androidx.xr.extensions.XrExtensions#attachSpatialScene
     */
    public com.android.extensions.xr.node.Node getNode() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Updates the 2D window bounds of this {@link com.android.extensions.xr.space.ActivityPanel
     * ActivityPanel}.
     *
     * <p>If the new bounds are smaller that the minimum dimensions of the activity embedded in this
     * ActivityPanel, the ActivityPanel bounds will be reset to match the host Activity bounds.
     *
     * @param windowBounds the new 2D window bounds in the host container window coordinates.
     */
    public void setWindowBounds(android.graphics.Rect windowBounds) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Deletes the activity panel. All the activities in this {@link
     * com.android.extensions.xr.space.ActivityPanel ActivityPanel} will also be destroyed.
     */
    public void delete() {
        throw new RuntimeException("Stub!");
    }
}
