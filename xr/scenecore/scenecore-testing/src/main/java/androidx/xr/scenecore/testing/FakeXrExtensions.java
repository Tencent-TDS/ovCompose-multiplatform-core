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

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Rect;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.view.AttachedSurfaceControl;
import android.view.SurfaceControl;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.xr.extensions.Config;
import androidx.xr.extensions.Consumer;
import androidx.xr.extensions.XrExtensionResult;
import androidx.xr.extensions.XrExtensions;
import androidx.xr.extensions.XrExtensionsProvider;
import androidx.xr.extensions.asset.TokenConverter;
import androidx.xr.extensions.environment.EnvironmentVisibilityState;
import androidx.xr.extensions.environment.PassthroughVisibilityState;
import androidx.xr.extensions.media.AudioTrackExtensions;
import androidx.xr.extensions.media.MediaPlayerExtensions;
import androidx.xr.extensions.media.PointSourceAttributes;
import androidx.xr.extensions.media.SoundFieldAttributes;
import androidx.xr.extensions.media.SoundPoolExtensions;
import androidx.xr.extensions.media.SpatializerExtensions;
import androidx.xr.extensions.media.XrSpatialAudioExtensions;
import androidx.xr.extensions.node.InputEvent;
import androidx.xr.extensions.node.Node;
import androidx.xr.extensions.node.NodeTransaction;
import androidx.xr.extensions.node.NodeTransform;
import androidx.xr.extensions.node.NodeTypeConverter;
import androidx.xr.extensions.node.ReformEvent;
import androidx.xr.extensions.node.ReformOptions;
import androidx.xr.extensions.node.Vec3;
import androidx.xr.extensions.space.ActivityPanel;
import androidx.xr.extensions.space.ActivityPanelLaunchParameters;
import androidx.xr.extensions.space.Bounds;
import androidx.xr.extensions.space.HitTestResult;
import androidx.xr.extensions.space.SpatialCapabilities;
import androidx.xr.extensions.space.SpatialState;
import androidx.xr.extensions.splitengine.SplitEngineBridge;
import androidx.xr.extensions.subspace.Subspace;

import com.android.extensions.xr.asset.FakeGltfModelToken;
import com.android.extensions.xr.node.NodeRepository;
import com.android.extensions.xr.node.ShadowNode;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.io.Closeable;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * A fake for the XrExtensions.
 *
 * <p>This has fake implementations for a subset of the XrExtension capability that is used by the
 * JXRCore runtime for AndroidXR.
 */
@SuppressWarnings("deprecation")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class FakeXrExtensions implements XrExtensions {
    private static final String NOT_IMPLEMENTED_IN_FAKE =
            "This function is not implemented yet in FakeXrExtensions.  Please add an"
                    + " implementation if support is desired for testing.";

    // Because there are many tests which are written to use the FakeXrExtensions (and associated
    // classes) interface to minimize the scope of changes FakeXrExtensions will defer to
    // XrExtensionsProvider which when used in a host test will use the fake implementation of the
    // com.android.extensions.xr classes. FakeXrExtensions is thus a thin androidx.xr.extensions
    // wrapper around the com.android.extensions.xr fake classes. Once all tests are migrated to use
    // the com.android.extensions.xr types this fake can be removed.
    // TODO: b/399893709 - Remove FakeXrExtensions once tests are migrated to use the real
    // XrExtensionsProvider.
    @NonNull private final XrExtensions extensions = XrExtensionsProvider.getXrExtensions();

    @NonNull private final NodeRepository mNodeRepository = NodeRepository.getInstance();

    @NonNull public final List<FakeNode> createdNodes = new ArrayList<>();

    @NonNull public final List<FakeGltfModelToken> createdGltfModelTokens = new ArrayList<>();

    @NonNull
    public final List<androidx.xr.extensions.asset.EnvironmentToken> createdEnvironmentTokens =
            new ArrayList<>();

    @NonNull public final Map<Activity, FakeActivityPanel> activityPanelMap = new HashMap<>();

    FakeNode mFakeTaskNode = null;
    Node mEnvironmentNode = null;
    Node mNodeForMainWindow = null;

    // TODO: b/370033054 - fakeSpatialState should be updated according to some fake extensions
    // calls
    // like requestFullSpaceMode after migration to SpatialState API
    @NonNull public final FakeSpatialState fakeSpatialState = new FakeSpatialState();

    // Technically this could be set per-activity, but we're assuming that there's a single activity
    // associated with each JXRCore session, so we're only tracking it once for now.
    SpaceMode mSpaceMode = SpaceMode.NONE;

    int mMainWindowWidth = 0;
    int mMainWindowHeight = 0;

    Consumer<SpatialState> mSpatialStateCallback = null;

    float mPreferredAspectRatioHsm = 0.0f;
    int mOpenXrWorldSpaceType = 0;
    NodeTransaction mLastFakeNodeTransaction = null;

    @NonNull
    public final FakeSpatialAudioExtensions fakeSpatialAudioExtensions =
            new FakeSpatialAudioExtensions();

    @Nullable
    public Node getFakeEnvironmentNode() {
        return mEnvironmentNode;
    }

    @Nullable
    public Node getFakeNodeForMainWindow() {
        return mNodeForMainWindow;
    }

    @Override
    public int getApiVersion() {
        return extensions.getApiVersion();
    }

    @Override
    @NonNull
    public Node createNode() {
        Node node = extensions.createNode();
        createdNodes.add(new FakeNode(node));
        return node;
    }

    @Override
    @NonNull
    public NodeTransaction createNodeTransaction() {
        mLastFakeNodeTransaction = extensions.createNodeTransaction();
        return mLastFakeNodeTransaction;
    }

    @Override
    @NonNull
    public Subspace createSubspace(@NonNull SplitEngineBridge splitEngineBridge, int subspaceId) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_IN_FAKE);
    }

    @Override
    @NonNull
    public Config getConfig() {
        return extensions.getConfig();
    }

    @NonNull
    public SpaceMode getSpaceMode() {
        return mSpaceMode;
    }

    @Override
    public void setMainWindowSize(
            @NonNull Activity activity,
            int width,
            int height,
            @NonNull Consumer<XrExtensionResult> callback,
            @NonNull Executor executor) {
        mMainWindowWidth = width;
        mMainWindowHeight = height;
        executor.execute(() -> callback.accept(createAsyncResult()));
    }

    /**
     * @deprecated This method is no longer supported.
     */
    @Override
    @Deprecated
    public void setMainWindowCurvatureRadius(@NonNull Activity activity, float curvatureRadius) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_IN_FAKE);
    }

    public int getMainWindowWidth() {
        return mMainWindowWidth;
    }

    public int getMainWindowHeight() {
        return mMainWindowHeight;
    }

    private SpatialCapabilities getCapabilities(boolean allowAll) {
        return new SpatialCapabilities() {
            @Override
            public boolean get(int capQuery) {
                return allowAll;
            }
        };
    }

    @Override
    @NonNull
    public SpatialState getSpatialState(@NonNull Activity activity) {
        return fakeSpatialState;
    }

    @Override
    public void requestFullSpaceMode(
            @NonNull Activity activity,
            boolean requestEnter,
            @NonNull Consumer<XrExtensionResult> callback,
            @NonNull Executor executor) {
        FakeSpatialState spatialState = new FakeSpatialState();
        spatialState.mBounds =
                requestEnter
                        ? new Bounds(
                                Float.POSITIVE_INFINITY,
                                Float.POSITIVE_INFINITY,
                                Float.POSITIVE_INFINITY)
                        : new Bounds(10f, 10f, 10f);
        spatialState.mCapabilities = getCapabilities(requestEnter);
        sendSpatialState(spatialState);
        mSpaceMode = requestEnter ? SpaceMode.FULL_SPACE : SpaceMode.HOME_SPACE;

        executor.execute(() -> callback.accept(createAsyncResult()));
    }

    @Override
    @SuppressLint("PairedRegistration")
    public void registerSpatialStateCallback(
            @NonNull Activity activity,
            @NonNull Consumer<SpatialState> callback,
            @NonNull Executor executor) {
        // note that we assume this is only called for the (single) primary activity associated with
        // the JXRCore session and we also don't honor the executor here
        mSpatialStateCallback = callback;
    }

    @Override
    public void clearSpatialStateCallback(@NonNull Activity activity) {
        mSpatialStateCallback = null;
    }

    /**
     * Tests can use this method to trigger the spatial state callback. It is invoked on the calling
     * thread.
     */
    public void sendSpatialState(@NonNull SpatialState spatialState) {
        if (mSpatialStateCallback != null) {
            mSpatialStateCallback.accept(spatialState);
        }
    }

    @Nullable
    public Consumer<SpatialState> getSpatialStateCallback() {
        return mSpatialStateCallback;
    }

    private XrExtensionResult createAsyncResult() {
        return new XrExtensionResult() {
            @Override
            public int getResult() {
                return XrExtensionResult.XR_RESULT_SUCCESS;
            }
        };
    }

    @Override
    public void attachSpatialScene(
            @NonNull Activity activity,
            @NonNull Node sceneNode,
            @NonNull Node windowNode,
            @NonNull Consumer<XrExtensionResult> callback,
            @NonNull Executor executor) {
        mFakeTaskNode = new FakeNode(sceneNode);
        mFakeTaskNode.mName = "taskNode";

        mNodeForMainWindow = windowNode;

        executor.execute(() -> callback.accept(createAsyncResult()));
    }

    @Override
    public void detachSpatialScene(
            @NonNull Activity activity,
            @NonNull Consumer<XrExtensionResult> callback,
            @NonNull Executor executor) {
        mFakeTaskNode = null;
        mNodeForMainWindow = null;

        executor.execute(() -> callback.accept(createAsyncResult()));
    }

    @Nullable
    public FakeNode getFakeTaskNode() {
        return mFakeTaskNode;
    }

    @Override
    public void attachSpatialEnvironment(
            @NonNull Activity activity,
            @NonNull Node environmentNode,
            @NonNull Consumer<XrExtensionResult> callback,
            @NonNull Executor executor) {
        mEnvironmentNode = environmentNode;

        executor.execute(() -> callback.accept(createAsyncResult()));
    }

    @Override
    public void detachSpatialEnvironment(
            @NonNull Activity activity,
            @NonNull Consumer<XrExtensionResult> callback,
            @NonNull Executor executor) {
        mEnvironmentNode = null;

        executor.execute(() -> callback.accept(createAsyncResult()));
    }

    /**
     * Suppressed to allow CompletableFuture.
     *
     * @deprecated This method is no longer supported.
     */
    @SuppressWarnings({"AndroidJdkLibsChecker", "BadFuture"})
    @Override
    @NonNull
    @Deprecated
    public CompletableFuture</* @Nullable */ androidx.xr.extensions.asset.GltfModelToken>
            loadGltfModel(
                    @Nullable InputStream asset,
                    int regionSizeBytes,
                    int regionOffsetBytes,
                    @Nullable String url) {
        FakeGltfModelToken modelToken = new FakeGltfModelToken(url);
        createdGltfModelTokens.add(modelToken);
        return CompletableFuture.completedFuture(TokenConverter.toLibrary(modelToken));
    }

    /**
     * Suppressed to allow CompletableFuture.
     *
     * @deprecated This method is no longer supported.
     */
    @SuppressWarnings({"AndroidJdkLibsChecker", "BadFuture"})
    @Override
    @NonNull
    @Deprecated
    public CompletableFuture</* @Nullable */ androidx.xr.extensions.asset.EnvironmentToken>
            loadEnvironment(
                    @Nullable InputStream asset,
                    int regionSizeBytes,
                    int regionOffsetBytes,
                    @Nullable String url) {
        CompletableFuture<androidx.xr.extensions.asset.EnvironmentToken> future =
                extensions.loadEnvironment(asset, regionSizeBytes, regionOffsetBytes, url);
        return future.thenApply(
                imageToken -> {
                    createdEnvironmentTokens.add(imageToken);
                    return imageToken;
                });
    }

    /**
     * Suppressed to allow CompletableFuture.
     *
     * @deprecated This method is no longer supported.
     */
    @SuppressWarnings("AndroidJdkLibsChecker")
    @Override
    @NonNull
    @Deprecated
    public CompletableFuture</* @Nullable */ androidx.xr.extensions.asset.EnvironmentToken>
            loadEnvironment(
                    InputStream asset,
                    int regionSizeBytes,
                    int regionOffsetBytes,
                    String url,
                    int textureWidth,
                    int textureHeight) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_IN_FAKE);
    }

    /**
     * Suppressed to allow CompletableFuture.
     *
     * @deprecated This method is no longer supported.
     */
    @SuppressWarnings("AndroidJdkLibsChecker")
    @Override
    @Deprecated
    @NonNull
    public CompletableFuture</* @Nullable */ androidx.xr.extensions.asset.SceneToken>
            loadImpressScene(InputStream asset, int regionSizeBytes, int regionOffsetBytes) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_IN_FAKE);
    }

    @Override
    @NonNull
    public SplitEngineBridge createSplitEngineBridge() {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_IN_FAKE);
    }

    /**
     * Returns a FakeNode with corresponding gltfModelToken if it was created and found
     *
     * @deprecated This method is no longer supported.
     */
    @NonNull
    @Deprecated
    public FakeNode testGetNodeWithGltfToken(
            @NonNull androidx.xr.extensions.asset.GltfModelToken token) {
        com.android.extensions.xr.node.Node node =
                mNodeRepository.findNode(
                        (NodeRepository.NodeMetadata metadata) ->
                                TokenConverter.toFramework(token)
                                        .equals(metadata.getGltfModelToken()));

        return (node != null) ? new FakeNode(node) : null;
    }

    /**
     * Returns a FakeNode with corresponding environmentToken if it was created and found
     *
     * @deprecated This method is no longer supported.
     */
    @NonNull
    @Deprecated
    public FakeNode testGetNodeWithEnvironmentToken(
            @NonNull androidx.xr.extensions.asset.EnvironmentToken token) {
        com.android.extensions.xr.node.Node node =
                mNodeRepository.findNode(
                        (NodeRepository.NodeMetadata metadata) ->
                                TokenConverter.toFramework(token)
                                        .equals(metadata.getEnvironmentToken()));

        return (node != null) ? new FakeNode(node) : null;
    }

    @Override
    @NonNull
    public ActivityPanel createActivityPanel(
            @NonNull Activity host, @NonNull ActivityPanelLaunchParameters launchParameters) {
        FakeActivityPanel fakeActivityPanel = new FakeActivityPanel(createNode());
        activityPanelMap.put(host, fakeActivityPanel);
        return fakeActivityPanel;
    }

    /** Returns the FakeActivityPanel for the given Activity. */
    @NonNull
    public FakeActivityPanel getActivityPanelForHost(@NonNull Activity host) {
        return activityPanelMap.get(host);
    }

    @Override
    @NonNull
    public ReformOptions createReformOptions(
            @NonNull Consumer<ReformEvent> callback, @NonNull Executor executor) {
        return extensions.createReformOptions(callback, executor);
    }

    @Override
    public void addFindableView(@NonNull View view, @NonNull ViewGroup group) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_IN_FAKE);
    }

    @Override
    public void removeFindableView(@NonNull View view, @NonNull ViewGroup group) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_IN_FAKE);
    }

    @Override
    @Nullable
    public Node getSurfaceTrackingNode(@NonNull View view) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_IN_FAKE);
    }

    @Override
    public void hitTest(
            @NonNull Activity activity,
            @NonNull Vec3 origin,
            @NonNull Vec3 direction,
            @NonNull Consumer<HitTestResult> callback,
            @NonNull Executor executor) {
        HitTestResult fakeHitTestResult = new HitTestResult();
        executor.execute(() -> callback.accept(fakeHitTestResult));
    }

    @Override
    public int getOpenXrWorldSpaceType() {
        return mOpenXrWorldSpaceType;
    }

    public void setOpenXrWorldSpaceType(int openXrWorldSpaceType) {
        mOpenXrWorldSpaceType = openXrWorldSpaceType;
    }

    @Override
    @NonNull
    public Bundle setFullSpaceMode(@NonNull Bundle bundle) {
        return bundle;
    }

    @Override
    @NonNull
    public Bundle setFullSpaceModeWithEnvironmentInherited(@NonNull Bundle bundle) {
        return bundle;
    }

    @Override
    public void setPreferredAspectRatio(
            @NonNull Activity activity,
            float preferredRatio,
            @NonNull Consumer<XrExtensionResult> callback,
            @NonNull Executor executor) {
        mPreferredAspectRatioHsm = preferredRatio;

        executor.execute(() -> callback.accept(createAsyncResult()));
    }

    public float getPreferredAspectRatio() {
        return mPreferredAspectRatioHsm;
    }

    @NonNull
    @Override
    public XrSpatialAudioExtensions getXrSpatialAudioExtensions() {
        return fakeSpatialAudioExtensions;
    }

    /** Tracks whether an Activity has requested a mode for when it's focused. */
    public enum SpaceMode {
        NONE,
        HOME_SPACE,
        FULL_SPACE
    }

    /** A fake implementation of Closeable. */
    @SuppressWarnings("NotCloseable")
    public static class FakeCloseable implements Closeable {
        boolean mClosed = false;

        @Override
        public void close() {
            mClosed = true;
        }

        public boolean isClosed() {
            return mClosed;
        }
    }

    /** A fake implementation of the XR extensions Node. */
    @SuppressWarnings("ParcelCreator")
    public static final class FakeNode implements Node {
        /**
         * A shadow of {@link com.android.extensions.xr.node.Node} which has additional methods for
         * testing.
         */
        public final ShadowNode shadowNode;

        /** The actual node returned from XrExtensions.createNode(). */
        public final com.android.extensions.xr.node.Node realNode;

        /**
         * The {@link androidx.xr.extensions.node.Node} wrapper of the {@link realNode}.
         *
         * <p>This node is an adapter, it converts a {@link com.android.extensions.xr.node.Node} so
         * it can be passed into the {@link androidx.xr.extensions} set of APIs.
         */
        public final Node node;

        @NonNull private final NodeRepository nodeRepository = NodeRepository.getInstance();

        String mName = null;

        public FakeNode(@NonNull com.android.extensions.xr.node.Node node) {
            requireNonNull(node);
            realNode = node;
            shadowNode = ShadowNode.extract(node);
            this.node = NodeTypeConverter.toLibrary(node);
        }

        public FakeNode(@NonNull Node node) {
            requireNonNull(node);
            realNode = NodeTypeConverter.toFramework(node);
            shadowNode = ShadowNode.extract(realNode);
            this.node = node;
        }

        @Override
        public void listenForInput(
                @NonNull Consumer<InputEvent> listener, @NonNull Executor executor) {
            node.listenForInput(listener, executor);
        }

        @Override
        public void stopListeningForInput() {
            node.stopListeningForInput();
        }

        @Override
        public void setNonPointerFocusTarget(@NonNull AttachedSurfaceControl focusTarget) {
            node.setNonPointerFocusTarget(focusTarget);
        }

        @Override
        public void requestPointerCapture(
                @NonNull Consumer<Integer> stateCallback, @NonNull Executor executor) {
            node.requestPointerCapture(stateCallback, executor);
        }

        @Override
        public void stopPointerCapture() {
            node.stopPointerCapture();
        }

        /**
         * Fires the InputEvent callback with the given event. It is invoked on the executor
         * provided in listenForInput.
         */
        public void sendInputEvent(@NonNull com.android.extensions.xr.node.InputEvent event) {
            shadowNode
                    .getInputExecutor()
                    .execute(() -> shadowNode.getInputListener().accept(event));
        }

        /**
         * Fires the nodeTransform callback with the given transform. It is invoked on the executor
         * provided in listenForInput.
         */
        public void sendTransformEvent(
                @NonNull com.android.extensions.xr.node.NodeTransform nodeTransform) {
            shadowNode
                    .getTransformExecutor()
                    .execute(() -> shadowNode.getTransformListener().accept(nodeTransform));
        }

        @Override
        @NonNull
        public Closeable subscribeToTransform(
                @NonNull Consumer<NodeTransform> transformCallback, @NonNull Executor executor) {
            return node.subscribeToTransform(transformCallback, executor);
        }

        @Nullable
        public com.android.extensions.xr.function.Consumer<
                        com.android.extensions.xr.node.NodeTransform>
                getTransformListener() {
            return shadowNode.getTransformListener();
        }

        @Nullable
        public Executor getTransformExecutor() {
            return shadowNode.getTransformExecutor();
        }

        @Override
        public void writeToParcel(@NonNull Parcel in, int flags) {}

        @Override
        public int describeContents() {
            return 0;
        }

        @Nullable
        public Node getParent() {
            return NodeTypeConverter.toLibrary(nodeRepository.getParent(realNode));
        }

        public float getXPosition() {
            return nodeRepository.getPosition(realNode).x;
        }

        public float getYPosition() {
            return nodeRepository.getPosition(realNode).y;
        }

        public float getZPosition() {
            return nodeRepository.getPosition(realNode).z;
        }

        public float getXOrientation() {
            return nodeRepository.getOrientation(realNode).x;
        }

        public float getYOrientation() {
            return nodeRepository.getOrientation(realNode).y;
        }

        public float getZOrientation() {
            return nodeRepository.getOrientation(realNode).z;
        }

        public float getWOrientation() {
            return nodeRepository.getOrientation(realNode).w;
        }

        public float getCornerRadius() {
            return nodeRepository.getCornerRadius(realNode);
        }

        public boolean isVisible() {
            return nodeRepository.isVisible(realNode);
        }

        public float getAlpha() {
            return nodeRepository.getAlpha(realNode);
        }

        /**
         * @deprecated This method is no longer supported.
         */
        @Nullable
        @Deprecated
        public androidx.xr.extensions.asset.GltfModelToken getGltfModel() {
            return TokenConverter.toLibrary(nodeRepository.getGltfModelToken(realNode));
        }

        @Nullable
        public IBinder getAnchorId() {
            return nodeRepository.getAnchorId(realNode);
        }

        @Nullable
        public String getName() {
            return nodeRepository.getName(realNode);
        }

        @Nullable
        public SurfaceControl getSurfaceControl() {
            return nodeRepository.getSurfaceControl(realNode);
        }

        /**
         * @deprecated This method is no longer supported.
         */
        @Nullable
        @Deprecated
        public androidx.xr.extensions.asset.EnvironmentToken getEnvironment() {
            return TokenConverter.toLibrary(nodeRepository.getEnvironmentToken(realNode));
        }

        @Nullable
        public com.android.extensions.xr.function.Consumer<
                        com.android.extensions.xr.node.InputEvent>
                getListener() {
            return shadowNode.getInputListener();
        }

        @Nullable
        public Consumer<Integer> getPointerCaptureStateCallback() {
            com.android.extensions.xr.function.Consumer<Integer> callback =
                    shadowNode.getPointerCaptureStateCallback();

            if (callback == null) {
                return null;
            }

            return callback::accept;
        }

        @Nullable
        public Executor getExecutor() {
            return shadowNode.getInputExecutor();
        }

        @Nullable
        public ReformOptions getReformOptions() {
            com.android.extensions.xr.node.ReformOptions options =
                    nodeRepository.getReformOptions(realNode);

            if (options == null) {
                return null;
            }

            return NodeTypeConverter.toLibrary(options);
        }
    }

    /** A fake implementation of the XR extensions EnvironmentVisibilityState. */
    public static class FakeEnvironmentVisibilityState implements EnvironmentVisibilityState {
        @EnvironmentVisibilityState.State int mState;

        public FakeEnvironmentVisibilityState(@EnvironmentVisibilityState.State int state) {
            this.mState = state;
        }

        @Override
        @EnvironmentVisibilityState.State
        public int getCurrentState() {
            return mState;
        }
    }

    /** A fake implementation of the XR extensions EnvironmentVisibilityState. */
    public static class FakePassthroughVisibilityState implements PassthroughVisibilityState {
        @PassthroughVisibilityState.State int mState;
        float mOpacity;

        public FakePassthroughVisibilityState(
                @PassthroughVisibilityState.State int state, float opacity) {
            this.mState = state;
            this.mOpacity = opacity;
        }

        @Override
        @PassthroughVisibilityState.State
        public int getCurrentState() {
            return mState;
        }

        @Override
        public float getOpacity() {
            return mOpacity;
        }
    }

    /** Creates fake activity panel. */
    public static class FakeActivityPanel implements ActivityPanel {
        Intent mLaunchIntent;
        Bundle mBundle;
        Activity mActivity;
        Rect mBounds;
        boolean mIsDeleted = false;
        Node mNode;

        FakeActivityPanel(@NonNull Node node) {
            mNode = node;
        }

        @Override
        public void launchActivity(@NonNull Intent intent, @Nullable Bundle options) {
            mLaunchIntent = intent;
            mBundle = options;
        }

        @Nullable
        public Intent getLaunchIntent() {
            return mLaunchIntent;
        }

        @NonNull
        public Bundle getBundle() {
            return mBundle;
        }

        @Override
        public void moveActivity(@NonNull Activity activity) {
            mActivity = activity;
        }

        @Nullable
        public Activity getActivity() {
            return mActivity;
        }

        @NonNull
        @Override
        public Node getNode() {
            return mNode;
        }

        @Override
        public void setWindowBounds(@NonNull Rect windowBounds) {
            mBounds = windowBounds;
        }

        @Nullable
        public Rect getBounds() {
            return mBounds;
        }

        @Override
        public void delete() {
            mIsDeleted = true;
        }

        public boolean isDeleted() {
            return mIsDeleted;
        }
    }

    /** A fake implementation of the XR extensions SpatialState. */
    public static class FakeSpatialState implements SpatialState {
        Bounds mBounds;
        SpatialCapabilities mCapabilities;
        EnvironmentVisibilityState mEnvironmentVisibilityState;
        PassthroughVisibilityState mPassthroughVisibilityState;

        public FakeSpatialState() {
            // Initialize params to any non-null values
            // TODO: b/370033054 - Revisit the default values for the bounds and capabilities.
            mBounds =
                    new Bounds(
                            Float.POSITIVE_INFINITY,
                            Float.POSITIVE_INFINITY,
                            Float.POSITIVE_INFINITY);
            this.setAllSpatialCapabilities(true);
            mEnvironmentVisibilityState =
                    new FakeEnvironmentVisibilityState(EnvironmentVisibilityState.INVISIBLE);
            mPassthroughVisibilityState =
                    new FakePassthroughVisibilityState(PassthroughVisibilityState.DISABLED, 0.0f);
        }

        @Override
        @NonNull
        public Bounds getBounds() {
            return mBounds;
        }

        public void setBounds(@NonNull Bounds bounds) {
            mBounds = bounds;
        }

        @Override
        @NonNull
        public SpatialCapabilities getSpatialCapabilities() {
            return mCapabilities;
        }

        @Override
        @NonNull
        public EnvironmentVisibilityState getEnvironmentVisibility() {
            return mEnvironmentVisibilityState;
        }

        public void setEnvironmentVisibility(
                @NonNull EnvironmentVisibilityState environmentVisibilityState) {
            mEnvironmentVisibilityState = environmentVisibilityState;
        }

        @Override
        @NonNull
        public PassthroughVisibilityState getPassthroughVisibility() {
            return mPassthroughVisibilityState;
        }

        public void setPassthroughVisibility(
                @NonNull PassthroughVisibilityState passthroughVisibilityState) {
            mPassthroughVisibilityState = passthroughVisibilityState;
        }

        // Methods for tests to set the capabilities.
        public void setSpatialCapabilities(@NonNull SpatialCapabilities capabilities) {
            mCapabilities = capabilities;
        }

        public void setAllSpatialCapabilities(boolean allowAll) {
            mCapabilities =
                    new SpatialCapabilities() {
                        @Override
                        public boolean get(int capQuery) {
                            return allowAll;
                        }
                    };
        }
    }

    /** Fake XrSpatialAudioExtensions. */
    public static class FakeSpatialAudioExtensions implements XrSpatialAudioExtensions {

        @NonNull
        public final FakeSoundPoolExtensions soundPoolExtensions = new FakeSoundPoolExtensions();

        FakeAudioTrackExtensions mAudioTrackExtensions = new FakeAudioTrackExtensions();

        @NonNull
        public final FakeMediaPlayerExtensions mediaPlayerExtensions =
                new FakeMediaPlayerExtensions();

        @NonNull
        @Override
        public SoundPoolExtensions getSoundPoolExtensions() {
            return soundPoolExtensions;
        }

        @NonNull
        @Override
        public AudioTrackExtensions getAudioTrackExtensions() {
            return mAudioTrackExtensions;
        }

        public void setFakeAudioTrackExtensions(
                @NonNull FakeAudioTrackExtensions audioTrackExtensions) {
            mAudioTrackExtensions = audioTrackExtensions;
        }

        @NonNull
        @Override
        public MediaPlayerExtensions getMediaPlayerExtensions() {
            return mediaPlayerExtensions;
        }
    }

    /** Fake SoundPoolExtensions. */
    public static class FakeSoundPoolExtensions implements SoundPoolExtensions {

        int mPlayAsPointSourceResult = 0;
        int mPlayAsSoundFieldResult = 0;
        int mSourceType = SpatializerExtensions.SOURCE_TYPE_BYPASS;

        public void setPlayAsPointSourceResult(int result) {
            mPlayAsPointSourceResult = result;
        }

        public void setPlayAsSoundFieldResult(int result) {
            mPlayAsSoundFieldResult = result;
        }

        public void setSourceType(@SpatializerExtensions.SourceType int sourceType) {
            mSourceType = sourceType;
        }

        @Override
        public int playAsPointSource(
                @NonNull SoundPool soundPool,
                int soundID,
                @NonNull PointSourceAttributes attributes,
                float volume,
                int priority,
                int loop,
                float rate) {
            return mPlayAsPointSourceResult;
        }

        @Override
        public int playAsSoundField(
                @NonNull SoundPool soundPool,
                int soundID,
                @NonNull SoundFieldAttributes attributes,
                float volume,
                int priority,
                int loop,
                float rate) {
            return mPlayAsSoundFieldResult;
        }

        @Override
        @SpatializerExtensions.SourceType
        public int getSpatialSourceType(@NonNull SoundPool soundPool, int streamID) {
            return mSourceType;
        }
    }

    /** Fake AudioTrackExtensions. */
    public static class FakeAudioTrackExtensions implements AudioTrackExtensions {

        PointSourceAttributes mPointSourceAttributes;

        SoundFieldAttributes mSoundFieldAttributes;

        @SpatializerExtensions.SourceType int mSourceType;

        @CanIgnoreReturnValue
        @Override
        @NonNull
        public AudioTrack.Builder setPointSourceAttributes(
                @NonNull AudioTrack.Builder builder, @Nullable PointSourceAttributes attributes) {
            mPointSourceAttributes = attributes;
            return builder;
        }

        public void setPointSourceAttributes(
                @Nullable PointSourceAttributes pointSourceAttributes) {
            mPointSourceAttributes = pointSourceAttributes;
        }

        @CanIgnoreReturnValue
        @Override
        @NonNull
        public AudioTrack.Builder setSoundFieldAttributes(
                @NonNull AudioTrack.Builder builder, @Nullable SoundFieldAttributes attributes) {
            mSoundFieldAttributes = attributes;
            return builder;
        }

        public void setSoundFieldAttributes(@Nullable SoundFieldAttributes soundFieldAttributes) {
            mSoundFieldAttributes = soundFieldAttributes;
        }

        @Override
        @Nullable
        public PointSourceAttributes getPointSourceAttributes(@NonNull AudioTrack track) {
            return mPointSourceAttributes;
        }

        @Nullable
        public PointSourceAttributes getPointSourceAttributes() {
            return mPointSourceAttributes;
        }

        @Override
        @Nullable
        public SoundFieldAttributes getSoundFieldAttributes(@NonNull AudioTrack track) {
            return mSoundFieldAttributes;
        }

        @Nullable
        public SoundFieldAttributes getSoundFieldAttributes() {
            return mSoundFieldAttributes;
        }

        @Override
        public int getSpatialSourceType(@NonNull AudioTrack track) {
            return mSourceType;
        }

        public void setSourceType(@SpatializerExtensions.SourceType int sourceType) {
            mSourceType = sourceType;
        }
    }

    /** Fake MediaPlayerExtensions. */
    public static class FakeMediaPlayerExtensions implements MediaPlayerExtensions {

        PointSourceAttributes mPointSourceAttributes;

        SoundFieldAttributes mSoundFieldAttributes;

        @CanIgnoreReturnValue
        @Override
        @NonNull
        public MediaPlayer setPointSourceAttributes(
                @NonNull MediaPlayer mediaPlayer, @Nullable PointSourceAttributes attributes) {
            mPointSourceAttributes = attributes;
            return mediaPlayer;
        }

        @CanIgnoreReturnValue
        @Override
        @NonNull
        public MediaPlayer setSoundFieldAttributes(
                @NonNull MediaPlayer mediaPlayer, @Nullable SoundFieldAttributes attributes) {
            mSoundFieldAttributes = attributes;
            return mediaPlayer;
        }

        @Nullable
        public PointSourceAttributes getPointSourceAttributes() {
            return mPointSourceAttributes;
        }

        @Nullable
        public SoundFieldAttributes getSoundFieldAttributes() {
            return mSoundFieldAttributes;
        }
    }
}
