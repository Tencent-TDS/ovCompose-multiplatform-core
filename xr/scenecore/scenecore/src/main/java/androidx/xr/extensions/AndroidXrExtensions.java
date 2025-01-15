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

package androidx.xr.extensions;

import static java.util.Objects.requireNonNull;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.xr.extensions.asset.EnvironmentToken;
import androidx.xr.extensions.asset.GltfModelToken;
import androidx.xr.extensions.asset.SceneToken;
import androidx.xr.extensions.asset.TokenConverter;
import androidx.xr.extensions.media.MediaTypeConverter;
import androidx.xr.extensions.media.XrSpatialAudioExtensions;
import androidx.xr.extensions.node.Node;
import androidx.xr.extensions.node.NodeTransaction;
import androidx.xr.extensions.node.NodeTypeConverter;
import androidx.xr.extensions.node.ReformEvent;
import androidx.xr.extensions.node.ReformOptions;
import androidx.xr.extensions.node.Vec3;
import androidx.xr.extensions.space.ActivityPanel;
import androidx.xr.extensions.space.ActivityPanelLaunchParameters;
import androidx.xr.extensions.space.Bounds;
import androidx.xr.extensions.space.HitTestResult;
import androidx.xr.extensions.space.SpaceTypeConverter;
import androidx.xr.extensions.space.SpatialCapabilities;
import androidx.xr.extensions.space.SpatialState;
import androidx.xr.extensions.space.SpatialStateEvent;
import androidx.xr.extensions.splitengine.SplitEngineBridge;
import androidx.xr.extensions.splitengine.SplitEngineTypeConverter;
import androidx.xr.extensions.subspace.Subspace;
import androidx.xr.extensions.subspace.SubspaceTypeConverter;

import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * The main extensions class that creates or provides instances of various XR Extensions components.
 *
 * <p>This class wraps the com.android.extensions.xr.XrExtensions class.
 */
class AndroidXrExtensions implements XrExtensions {
    @NonNull private final com.android.extensions.xr.XrExtensions mExtensions;

    AndroidXrExtensions(@NonNull com.android.extensions.xr.XrExtensions extensions) {
        requireNonNull(extensions);
        mExtensions = extensions;
    }

    @Override
    public int getApiVersion() {
        return mExtensions.getApiVersion();
    }

    @Override
    public @NonNull Node createNode() {
        return NodeTypeConverter.toLibrary(mExtensions.createNode());
    }

    @Override
    public @NonNull NodeTransaction createNodeTransaction() {
        return NodeTypeConverter.toLibrary(mExtensions.createNodeTransaction());
    }

    @Override
    public @NonNull Subspace createSubspace(
            @NonNull SplitEngineBridge splitEngineBridge, int subspaceId) {
        com.android.extensions.xr.splitengine.SplitEngineBridge bridge =
                SplitEngineTypeConverter.toFramework(splitEngineBridge);

        return SubspaceTypeConverter.toLibrary(mExtensions.createSubspace(bridge, subspaceId));
    }

    @Override
    @Deprecated
    public @NonNull CompletableFuture</* @Nullable */ GltfModelToken> loadGltfModel(
            InputStream asset, int regionSizeBytes, int regionOffsetBytes, String url) {
        return mExtensions
                .loadGltfModel(asset, regionSizeBytes, regionOffsetBytes, url)
                .thenApply(token -> TokenConverter.toLibrary(token));
    }

    @Override
    @Deprecated
    public @NonNull CompletableFuture</* @Nullable */ SceneViewerResult> displayGltfModel(
            Activity activity, GltfModelToken gltfModel) {
        return mExtensions
                .displayGltfModel(activity, TokenConverter.toFramework(gltfModel))
                .thenApply(result -> (result == null) ? null : new SceneViewerResult());
    }

    @Override
    @Deprecated
    public @NonNull CompletableFuture</* @Nullable */ EnvironmentToken> loadEnvironment(
            InputStream asset, int regionSizeBytes, int regionOffsetBytes, String url) {
        // This method has been deprecated on the platform side. Hard  code width and height to 256.
        return loadEnvironment(
                asset,
                regionSizeBytes,
                regionOffsetBytes,
                url,
                /*default texture width*/ 256, /*default texture height*/
                256);
    }

    @Override
    @Deprecated
    public @NonNull CompletableFuture</* @Nullable */ EnvironmentToken> loadEnvironment(
            InputStream asset,
            int regionSizeBytes,
            int regionOffsetBytes,
            String url,
            int textureWidth,
            int textureHeight) {
        return mExtensions
                .loadEnvironment(
                        asset, regionSizeBytes, regionOffsetBytes, url, textureWidth, textureHeight)
                .thenApply(token -> TokenConverter.toLibrary(token));
    }

    @Override
    @Deprecated
    public @NonNull CompletableFuture</* @Nullable */ SceneToken> loadImpressScene(
            InputStream asset, int regionSizeBytes, int regionOffsetBytes) {
        return mExtensions
                .loadImpressScene(asset, regionSizeBytes, regionOffsetBytes)
                .thenApply(token -> TokenConverter.toLibrary(token));
    }

    @Override
    public @NonNull SplitEngineBridge createSplitEngineBridge() {
        return SplitEngineTypeConverter.toLibrary(mExtensions.createSplitEngineBridge());
    }

    @Override
    public @NonNull XrSpatialAudioExtensions getXrSpatialAudioExtensions() {
        return MediaTypeConverter.toLibrary(mExtensions.getXrSpatialAudioExtensions());
    }

    @Override
    @Deprecated
    public void attachSpatialScene(
            @NonNull Activity activity, @NonNull Node sceneNode, @NonNull Node windowNode) {
        mExtensions.attachSpatialScene(
                activity,
                NodeTypeConverter.toFramework(sceneNode),
                NodeTypeConverter.toFramework(windowNode));
    }

    @Override
    public void attachSpatialScene(
            @NonNull Activity activity,
            @NonNull Node sceneNode,
            @NonNull Node windowNode,
            @NonNull Consumer<XrExtensionResult> callback,
            @NonNull Executor executor) {
        mExtensions.attachSpatialScene(
                activity,
                NodeTypeConverter.toFramework(sceneNode),
                NodeTypeConverter.toFramework(windowNode),
                /* callback= */ (result) -> {
                    callback.accept(new XrExtensionResultImpl(result));
                },
                executor);
    }

    @Override
    @Deprecated
    public void detachSpatialScene(@NonNull Activity activity) {
        mExtensions.detachSpatialScene(activity);
    }

    @Override
    public void detachSpatialScene(
            @NonNull Activity activity,
            @NonNull Consumer<XrExtensionResult> callback,
            @NonNull Executor executor) {
        mExtensions.detachSpatialScene(
                activity,
                /* callback= */ (result) -> {
                    callback.accept(new XrExtensionResultImpl(result));
                },
                executor);
    }

    @Override
    @Deprecated
    public void setMainWindowSize(@NonNull Activity activity, int width, int height) {
        mExtensions.setMainWindowSize(activity, width, height);
    }

    @Override
    public void setMainWindowSize(
            @NonNull Activity activity,
            int width,
            int height,
            @NonNull Consumer<XrExtensionResult> callback,
            @NonNull Executor executor) {
        mExtensions.setMainWindowSize(
                activity,
                width,
                height,
                /* callback= */ (result) -> {
                    callback.accept(new XrExtensionResultImpl(result));
                },
                executor);
    }

    @Override
    @Deprecated
    public void setMainWindowCurvatureRadius(@NonNull Activity activity, float curvatureRadius) {
        mExtensions.setMainWindowCurvatureRadius(activity, curvatureRadius);
    }

    @Override
    @Deprecated
    public void attachSpatialEnvironment(
            @NonNull Activity activity, @NonNull Node environmentNode) {
        mExtensions.attachSpatialEnvironment(
                activity, NodeTypeConverter.toFramework(environmentNode));
    }

    @Override
    public void attachSpatialEnvironment(
            @NonNull Activity activity,
            @NonNull Node environmentNode,
            @NonNull Consumer<XrExtensionResult> callback,
            @NonNull Executor executor) {
        mExtensions.attachSpatialEnvironment(
                activity,
                NodeTypeConverter.toFramework(environmentNode),
                /* callback= */ (result) -> {
                    callback.accept(new XrExtensionResultImpl(result));
                },
                executor);
    }

    @Override
    @Deprecated
    public void detachSpatialEnvironment(@NonNull Activity activity) {
        mExtensions.detachSpatialEnvironment(activity);
    }

    @Override
    public void detachSpatialEnvironment(
            @NonNull Activity activity,
            @NonNull Consumer<XrExtensionResult> callback,
            @NonNull Executor executor) {
        mExtensions.detachSpatialEnvironment(
                activity,
                /* callback= */ (result) -> {
                    callback.accept(new XrExtensionResultImpl(result));
                },
                executor);
    }

    @Override
    @Deprecated
    public void setSpatialStateCallback(
            @NonNull Activity activity,
            @NonNull Consumer<SpatialStateEvent> callback,
            @NonNull Executor executor) {
        mExtensions.setSpatialStateCallbackDeprecated(
                activity,
                /* callback= */ (event) -> {
                    callback.accept(SpaceTypeConverter.toLibrary(event));
                },
                executor);
    }

    @Override
    public void registerSpatialStateCallback(
            @NonNull Activity activity,
            @NonNull Consumer<SpatialState> callback,
            @NonNull Executor executor) {
        mExtensions.setSpatialStateCallback(
                activity,
                /* callback= */ (state) -> {
                    callback.accept(SpaceTypeConverter.toLibrary(state));
                },
                executor);
    }

    @Override
    public void clearSpatialStateCallback(@NonNull Activity activity) {
        mExtensions.clearSpatialStateCallback(activity);
    }

    @Override
    public @NonNull ActivityPanel createActivityPanel(
            @NonNull Activity host, @NonNull ActivityPanelLaunchParameters launchParameters) {
        return SpaceTypeConverter.toLibrary(
                mExtensions.createActivityPanel(
                        host, SpaceTypeConverter.toFramework(launchParameters)));
    }

    @Override
    @Deprecated
    public boolean canEmbedActivityPanel(@NonNull Activity activity) {
        // TODO(coderleon): update the doc when we support spatial task fragment.
        return mExtensions.canEmbedActivityPanel(activity);
    }

    @Override
    @Deprecated
    public boolean requestFullSpaceMode(@NonNull Activity activity) {
        return mExtensions.requestFullSpaceMode(activity);
    }

    @Override
    @Deprecated
    public boolean requestHomeSpaceMode(@NonNull Activity activity) {
        return mExtensions.requestHomeSpaceMode(activity);
    }

    @Override
    public void requestFullSpaceMode(
            @NonNull Activity activity,
            boolean requestEnter,
            @NonNull Consumer<XrExtensionResult> callback,
            @NonNull Executor executor) {
        mExtensions.requestFullSpaceMode(
                activity,
                requestEnter,
                /* callback= */ (result) -> {
                    callback.accept(new XrExtensionResultImpl(result));
                },
                executor);
    }

    @Override
    public @NonNull Bundle setFullSpaceMode(@NonNull Bundle bundle) {
        return mExtensions.setFullSpaceStartMode(bundle);
    }

    @Override
    public @NonNull Bundle setFullSpaceModeWithEnvironmentInherited(@NonNull Bundle bundle) {
        return mExtensions.setFullSpaceStartModeWithEnvironmentInherited(bundle);
    }

    @Override
    @Deprecated
    public @NonNull Bundle setMainPanelCurvatureRadius(
            @NonNull Bundle bundle, float panelCurvatureRadius) {
        return mExtensions.setMainPanelCurvatureRadius(bundle, panelCurvatureRadius);
    }

    @Override
    public @NonNull Config getConfig() {
        return new ConfigImpl(mExtensions.getConfig());
    }

    @Override
    public void hitTest(
            @NonNull Activity activity,
            @NonNull Vec3 origin,
            @NonNull Vec3 direction,
            @NonNull Consumer<HitTestResult> callback,
            @NonNull Executor executor) {
        mExtensions.hitTest(
                activity,
                NodeTypeConverter.toFramework(origin),
                NodeTypeConverter.toFramework(direction),
                /* callback= */ (result) -> {
                    callback.accept(SpaceTypeConverter.toLibrary(result));
                },
                executor);
    }

    @Override
    public int getOpenXrWorldSpaceType() {
        return mExtensions.getOpenXrWorldReferenceSpaceType();
    }

    @Override
    public @NonNull ReformOptions createReformOptions(
            @NonNull Consumer<ReformEvent> callback, @NonNull Executor executor) {
        return NodeTypeConverter.toLibrary(
                mExtensions.createReformOptions(
                        /* callback= */ (event) ->
                                callback.accept(NodeTypeConverter.toLibrary(event)),
                        executor));
    }

    @Override
    public void addFindableView(@NonNull View view, @NonNull ViewGroup group) {
        mExtensions.addFindableView(view, group);
    }

    @Override
    public void removeFindableView(@NonNull View view, @NonNull ViewGroup group) {
        mExtensions.removeFindableView(view, group);
    }

    @Override
    public @Nullable Node getSurfaceTrackingNode(@NonNull View view) {
        return NodeTypeConverter.toLibrary(mExtensions.getSurfaceTrackingNode(view));
    }

    @Override
    @Deprecated
    public void setPreferredAspectRatio(@NonNull Activity activity, float preferredRatio) {
        mExtensions.setPreferredAspectRatio(activity, preferredRatio);
    }

    @Override
    public void setPreferredAspectRatio(
            @NonNull Activity activity,
            float preferredRatio,
            @NonNull Consumer<XrExtensionResult> callback,
            @NonNull Executor executor) {
        mExtensions.setPreferredAspectRatio(
                activity,
                preferredRatio,
                /* callback= */ (result) -> {
                    callback.accept(new XrExtensionResultImpl(result));
                },
                executor);
    }

    @Override
    @Deprecated
    public void getSpatialCapabilities(
            @NonNull Activity activity,
            @NonNull Consumer<SpatialCapabilities> callback,
            @NonNull Executor executor) {
        mExtensions.getSpatialCapabilities(
                activity,
                /* callback= */ (capabilities) -> {
                    callback.accept(SpaceTypeConverter.toLibrary(capabilities));
                },
                executor);
    }

    @Override
    @Deprecated
    public void getBounds(
            @NonNull Activity activity,
            @NonNull Consumer<Bounds> callback,
            @NonNull Executor executor) {
        mExtensions.getBounds(
                activity,
                /* callback= */ (bounds) -> {
                    callback.accept(SpaceTypeConverter.toLibrary(bounds));
                },
                executor);
    }

    @Override
    public @NonNull SpatialState getSpatialState(@NonNull Activity activity) {
        return SpaceTypeConverter.toLibrary(mExtensions.getSpatialState(activity));
    }
}
