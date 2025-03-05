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

package androidx.xr.scenecore.impl;

import android.util.Log;
import android.view.SurfaceControlViewHost;
import android.view.SurfaceControlViewHost.SurfacePackage;

import androidx.xr.extensions.XrExtensions;
import androidx.xr.extensions.node.Node;
import androidx.xr.extensions.node.NodeTransaction;
import androidx.xr.scenecore.JxrPlatformAdapter.PanelEntity;
import androidx.xr.scenecore.JxrPlatformAdapter.PixelDimensions;

import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Implementation of PanelEntity.
 *
 * <p>(Requires API Level 30)
 *
 * <p>This entity shows 2D view on spatial panel.
 */
final class PanelEntityImpl extends BasePanelEntity implements PanelEntity {
    private static final String TAG = "PanelEntity";
    private final SurfaceControlViewHost mSurfaceControlViewHost;

    // TODO(b/352630140): Create a static factory method for PanelEntityImpl and move the Extensions
    //                    init there (out of JxrPlatformAdapterAxr)

    PanelEntityImpl(
            Node node,
            XrExtensions extensions,
            EntityManager entityManager,
            SurfaceControlViewHost surfaceControlViewHost,
            PixelDimensions windowBoundsPx,
            ScheduledExecutorService executor) {
        super(node, extensions, entityManager, executor);
        // We need to manually inform our base class of the pixelDimensions, even though the
        // Extensions
        // are initialized in the factory method. (ext.setWindowBounds, etc)
        super.setPixelDimensions(windowBoundsPx);
        mSurfaceControlViewHost = surfaceControlViewHost;
    }

    // TODO(b/352827267): Enforce minSDK API strategy - go/androidx-api-guidelines#compat-newapi
    @Override
    public void setPixelDimensions(PixelDimensions dimensions) {
        super.setPixelDimensions(dimensions);

        SurfacePackage surfacePackage =
                Objects.requireNonNull(mSurfaceControlViewHost.getSurfacePackage());

        mSurfaceControlViewHost.relayout(dimensions.width, dimensions.height);
        try (NodeTransaction transaction = mExtensions.createNodeTransaction()) {
            transaction
                    .setWindowBounds(surfacePackage, dimensions.width, dimensions.height)
                    .apply();
        }
        surfacePackage.release();
    }

    @SuppressWarnings("ObjectToString")
    @Override
    public void dispose() {
        Log.i(TAG, "Disposing " + this);
        mSurfaceControlViewHost.release();
        super.dispose();
    }
}
