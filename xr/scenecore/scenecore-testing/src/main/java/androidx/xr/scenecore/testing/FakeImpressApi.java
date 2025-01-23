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

package androidx.xr.scenecore.testing;

import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.concurrent.futures.ResolvableFuture;

import com.google.ar.imp.apibindings.ImpressApi;
import com.google.ar.imp.view.View;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fake implementation of the JNI API for communicating with the Impress Split Engine instance for
 * testing purposes.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class FakeImpressApi implements ImpressApi {

    // ResolvableFuture is marked as RestrictTo(LIBRARY_GROUP_PREFIX), which is intended for classes
    // within AndroidX. We're in the process of migrating to AndroidX. Without suppressing this
    // warning, however, we get a build error - go/bugpattern/RestrictTo.
    @SuppressWarnings("RestrictTo")
    static class AnimationInProgress {
        public String name;
        public ResolvableFuture<Void> fireOnDone;
    }

    // Map of model tokens to the list of impress nodes that are instances of that model.
    private final Map<Long, List<Integer>> mGltfModels = new HashMap<>();
    // Map of impress nodes to their parent impress nodes.
    private final Map<Integer, Integer> mImpressNodes = new HashMap<>();

    // Map of impress nodes and animations that are currently playing (non looping)
    final Map<Integer, AnimationInProgress> mImpressAnimatedNodes = new HashMap<>();

    // Map of impress nodes and animations that are currently playing (looping)
    final Map<Integer, AnimationInProgress> mImpressLoopAnimatedNodes = new HashMap<>();

    private int mNextModelId = 1;
    private int mNextNodeId = 1;

    @Override
    public void setup(@NonNull View view) {}

    @Override
    public void onResume() {}

    @Override
    public void onPause() {}

    @Override
    @NonNull
    @SuppressWarnings({"RestrictTo", "AsyncSuffixFuture"})
    public ListenableFuture<Long> loadGltfModel(@NonNull String name) {
        long modelToken = mNextModelId++;
        mGltfModels.put(modelToken, new ArrayList<>());
        // TODO(b/352827267): Enforce minSDK API strategy - go/androidx-api-guidelines#compat-newapi
        ResolvableFuture<Long> ret = ResolvableFuture.create();
        ret.set(modelToken);

        return ret;
    }

    @Override
    public void releaseGltfModel(long modelToken) {
        if (!mGltfModels.containsKey(modelToken)) {
            throw new IllegalArgumentException("Model token not found");
        }
        mGltfModels.remove(modelToken);
    }

    @Override
    public int instanceGltfModel(long modelToken) {
        return instanceGltfModel(modelToken, true);
    }

    @Override
    public int instanceGltfModel(long modelToken, boolean enableCollider) {
        if (!mGltfModels.containsKey(modelToken)) {
            throw new IllegalArgumentException("Model token not found");
        }
        int entityId = mNextNodeId++;
        mGltfModels.get(modelToken).add(entityId);
        mImpressNodes.put(entityId, null);
        return entityId;
    }

    @Override
    public void setGltfModelColliderEnabled(int impressNode, boolean enableCollider) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    @NonNull
    @SuppressWarnings({"RestrictTo", "AsyncSuffixFuture"})
    public ListenableFuture<Void> animateGltfModel(
            int impressNode, @Nullable String animationName, boolean loop) {
        ResolvableFuture<Void> future = ResolvableFuture.create();
        if (mImpressNodes.get(impressNode) == null) {
            future.setException(new IllegalArgumentException("Impress node not found"));
            return future;
        }

        AnimationInProgress animationInProgress = new AnimationInProgress();
        animationInProgress.name = animationName;
        animationInProgress.fireOnDone = future;
        if (loop) {
            mImpressLoopAnimatedNodes.put(impressNode, animationInProgress);
        } else {
            mImpressAnimatedNodes.put(impressNode, animationInProgress);
        }
        return future;
    }

    @Override
    public void stopGltfModelAnimation(int impressNode) {
        if (mImpressNodes.get(impressNode) == null) {
            throw new IllegalArgumentException("Impress node not found");
        } else if (!mImpressAnimatedNodes.containsKey(impressNode)
                && !mImpressLoopAnimatedNodes.containsKey(impressNode)) {
            throw new IllegalArgumentException("Impress node is not animating");
        } else if (mImpressAnimatedNodes.containsKey(impressNode)) {
            mImpressAnimatedNodes.remove(impressNode);
        } else if (mImpressLoopAnimatedNodes.containsKey(impressNode)) {
            mImpressLoopAnimatedNodes.remove(impressNode);
        }
    }

    @Override
    public int createImpressNode() {
        int entityId = mNextNodeId++;
        mImpressNodes.put(entityId, null);
        return entityId;
    }

    @Override
    public void destroyImpressNode(int impressNode) {
        if (!mImpressNodes.containsKey(impressNode)) {
            throw new IllegalArgumentException("Impress node not found");
        }
        for (Map.Entry<Long, List<Integer>> pair : mGltfModels.entrySet()) {
            if (pair.getValue().contains(impressNode)) {
                pair.getValue().remove(Integer.valueOf(impressNode));
            }
        }
        for (Map.Entry<Integer, Integer> pair : mImpressNodes.entrySet()) {
            if (pair.getValue() != null && pair.getValue().equals((Integer) impressNode)) {
                pair.setValue(null);
            }
        }
        mImpressNodes.remove(impressNode);
    }

    @Override
    public void setImpressNodeParent(int impressNodeChild, int impressNodeParent) {
        if (!mImpressNodes.containsKey(impressNodeChild)
                || !mImpressNodes.containsKey(impressNodeParent)) {
            throw new IllegalArgumentException("Impress node(s) not found");
        }
        mImpressNodes.put(impressNodeChild, impressNodeParent);
    }

    /** Gets the impress nodes for glTF models that match the given token. */
    @NonNull
    public List<Integer> getImpressNodesForToken(long modelToken) {
        return mGltfModels.get(modelToken);
    }

    /** Returns true if the given impress node has a parent. */
    public boolean impressNodeHasParent(int impressNode) {
        return mImpressNodes.containsKey(impressNode) && mImpressNodes.get(impressNode) != null;
    }

    /** Returns the number of impress nodes that are currently animating. */
    public int impressNodeAnimatingSize() {
        return mImpressAnimatedNodes.size();
    }

    /** Returns the number of impress nodes that looping animations. */
    public int impressNodeLoopAnimatingSize() {
        return mImpressLoopAnimatedNodes.size();
    }

    @Override
    public int createStereoSurface(@StereoMode int mode) {
        return mNextNodeId++;
    }

    @Override
    @NonNull
    public Surface getSurfaceFromStereoSurface(int panelImpressNode) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public void setStereoModeForStereoSurface(int panelImpressNode, @StereoMode int mode) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public void setCanvasDimensionsForStereoSurface(
            int panelImpressNode, float width, float height) {
        throw new IllegalArgumentException("not implemented");
    }
}
