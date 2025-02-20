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

package androidx.xr.extensions.space;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.xr.extensions.node.Node;

/**
 * Defines the panel in XR scene to support embedding activities within a host activity.
 *
 * <p>When the host activity is destroyed, all the activities in its embedded {@link ActivityPanel}
 * will also be destroyed.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface ActivityPanel {
    /**
     * Launches an activity into this panel.
     *
     * @param intent the {@link Intent} to start.
     * @param options additional options for how the Activity should be started.
     */
    void launchActivity(@NonNull Intent intent, @Nullable Bundle options);

    /**
     * Moves an existing activity into this panel.
     *
     * @param activity the {@link Activity} to move.
     */
    void moveActivity(@NonNull Activity activity);

    /**
     * Gets the node associated with this {@link ActivityPanel}.
     *
     * <p>The {@link ActivityPanel} can only be shown to the user after this node is attached to the
     * host activity's scene.
     *
     * @see androidx.xr.extensions.XrExtensions#attachSpatialScene
     */
    @NonNull
    Node getNode();

    /**
     * Updates the 2D window bounds of this {@link ActivityPanel}.
     *
     * <p>If the new bounds are smaller that the minimum dimensions of the activity embedded in this
     * ActivityPanel, the ActivityPanel bounds will be reset to match the host Activity bounds.
     *
     * @param windowBounds the new 2D window bounds in the host container window coordinates.
     */
    void setWindowBounds(@NonNull Rect windowBounds);

    /**
     * Deletes the activity panel. All the activities in this {@link ActivityPanel} will also be
     * destroyed.
     */
    void delete();
}
