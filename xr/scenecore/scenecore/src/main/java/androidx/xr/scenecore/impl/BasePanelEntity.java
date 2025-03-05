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

import android.content.res.Resources;

import androidx.xr.extensions.XrExtensions;
import androidx.xr.extensions.node.Node;
import androidx.xr.extensions.node.NodeTransaction;
import androidx.xr.runtime.math.Vector3;
import androidx.xr.scenecore.JxrPlatformAdapter.Dimensions;
import androidx.xr.scenecore.JxrPlatformAdapter.PanelEntity;
import androidx.xr.scenecore.JxrPlatformAdapter.PixelDimensions;

import java.util.concurrent.ScheduledExecutorService;

/** BasePanelEntity provides implementations of capabilities common to PanelEntities. */
@SuppressWarnings("deprecation") // TODO(b/373435470): Remove
abstract class BasePanelEntity extends AndroidXrEntity implements PanelEntity {
    protected PixelDimensions mPixelDimensions;
    private float mCornerRadius;

    BasePanelEntity(
            Node node,
            XrExtensions extensions,
            EntityManager entityManager,
            ScheduledExecutorService executor) {
        super(node, extensions, entityManager, executor);
    }

    private float getDefaultPixelDensity() {
        return mExtensions
                .getConfig()
                .defaultPixelsPerMeter(Resources.getSystem().getDisplayMetrics().density);
    }

    @Override
    public Vector3 getPixelDensity() {
        Vector3 scale = getWorldSpaceScale();
        float defaultPixelDensity = getDefaultPixelDensity();
        return new Vector3(
                defaultPixelDensity / scale.getX(),
                defaultPixelDensity / scale.getY(),
                defaultPixelDensity / scale.getZ());
    }

    @Override
    public Dimensions getSize() {
        Vector3 pixelDensity = getPixelDensity();
        return new Dimensions(
                mPixelDimensions.width / pixelDensity.getX(),
                mPixelDimensions.height / pixelDensity.getY(),
                0);
    }

    @Override
    public void setSize(Dimensions dimensions) {
        // TODO(b/352630025): remove this method.
        setPixelDimensions(new PixelDimensions((int) dimensions.width, (int) dimensions.height));
    }

    @Override
    public PixelDimensions getPixelDimensions() {
        return mPixelDimensions;
    }

    @Override
    public void setPixelDimensions(PixelDimensions dimensions) {
        mPixelDimensions = dimensions;
    }

    @Override
    public void setCornerRadius(float value) {
        if (value < 0.0f) {
            throw new IllegalArgumentException("Corner radius can't be negative: " + value);
        }
        try (NodeTransaction transaction = mExtensions.createNodeTransaction()) {
            transaction.setCornerRadius(mNode, value).apply();
            mCornerRadius = value;
        }
    }

    @Override
    public float getCornerRadius() {
        return mCornerRadius;
    }
}
