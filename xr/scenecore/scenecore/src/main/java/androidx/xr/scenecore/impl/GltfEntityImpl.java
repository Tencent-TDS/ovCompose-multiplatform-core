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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.xr.scenecore.JxrPlatformAdapter.Entity;
import androidx.xr.scenecore.JxrPlatformAdapter.GltfEntity;
import androidx.xr.scenecore.JxrPlatformAdapter.MaterialResource;

import com.android.extensions.xr.XrExtensions;
import com.android.extensions.xr.node.NodeTransaction;

import java.util.concurrent.ScheduledExecutorService;

/**
 * Implementation of a RealityCore GltfEntity.
 *
 * <p>This is used to create an entity that contains a glTF object.
 */
// TODO: b/321782625 - Add tests when the Extensions can be faked.
@SuppressWarnings("deprecation") // TODO(b/373435470): Remove
final class GltfEntityImpl extends AndroidXrEntity implements GltfEntity {
    GltfEntityImpl(
            GltfModelResourceImpl gltfModelResource,
            Entity parentEntity,
            XrExtensions extensions,
            EntityManager entityManager,
            ScheduledExecutorService executor) {
        super(extensions.createNode(), extensions, entityManager, executor);
        setParent(parentEntity);

        try (NodeTransaction transaction = extensions.createNodeTransaction()) {
            transaction.setGltfModel(mNode, gltfModelResource.getExtensionModelToken()).apply();
        }
    }

    @Override
    public void startAnimation(boolean looping, @Nullable String animationName) {
        // Implement this for the non-Split Engine path or ignore until the Split
        // Engine path becomes the default.
        Log.e("GltfEntityImpl: ", "GLTF Animation is only supported when using SplitEngine.");
    }

    @Override
    public void stopAnimation() {
        // Implement this for the non-Split Engine path or ignore until the Split
        // Engine path becomes the default.
        Log.e("GltfEntityImpl: ", "GLTF Animation is only supported when using SplitEngine.");
    }

    @Override
    @AnimationState
    public int getAnimationState() {
        // Implement this for the non-Split Engine path or ignore until the Split
        // Engine path becomes the default.
        Log.e("GltfEntityImpl: ", "GLTF Animation is only supported when using SplitEngine.");
        return AnimationState.STOPPED;
    }

    @Override
    public void setMaterialOverride(@NonNull MaterialResource material, @NonNull String meshName) {
        // Implement this for the non-Split Engine path or ignore until the Split
        // Engine path becomes the default.
        Log.e(
                "GltfEntityImpl: ",
                "GLTF Material Override is only supported when using SplitEngine.");
    }

    @SuppressWarnings("ObjectToString")
    @Override
    public void dispose() {
        Log.i("GltfEntityImpl", "Disposing " + this);
        super.dispose();
    }
}
