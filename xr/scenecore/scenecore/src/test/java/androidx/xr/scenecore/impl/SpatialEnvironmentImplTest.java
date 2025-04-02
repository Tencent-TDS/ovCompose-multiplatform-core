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

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;

import androidx.xr.extensions.environment.EnvironmentVisibilityState;
import androidx.xr.extensions.environment.PassthroughVisibilityState;
import androidx.xr.scenecore.JxrPlatformAdapter.ExrImageResource;
import androidx.xr.scenecore.JxrPlatformAdapter.MaterialResource;
import androidx.xr.scenecore.JxrPlatformAdapter.SpatialEnvironment.SetPassthroughOpacityPreferenceResult;
import androidx.xr.scenecore.JxrPlatformAdapter.SpatialEnvironment.SetSpatialEnvironmentPreferenceResult;
import androidx.xr.scenecore.JxrPlatformAdapter.SpatialEnvironment.SpatialEnvironmentPreference;
import androidx.xr.scenecore.impl.extensions.XrExtensionsProvider;
import androidx.xr.scenecore.testing.FakeImpressApi;
import androidx.xr.scenecore.testing.FakeImpressApi.MaterialData;

import com.android.extensions.xr.ShadowXrExtensions;
import com.android.extensions.xr.XrExtensions;
import com.android.extensions.xr.asset.EnvironmentToken;
import com.android.extensions.xr.asset.FakeEnvironmentToken;
import com.android.extensions.xr.asset.GltfModelToken;
import com.android.extensions.xr.environment.ShadowEnvironmentVisibilityState;
import com.android.extensions.xr.environment.ShadowPassthroughVisibilityState;
import com.android.extensions.xr.node.Node;
import com.android.extensions.xr.node.NodeRepository;
import com.android.extensions.xr.space.ShadowSpatialCapabilities;
import com.android.extensions.xr.space.ShadowSpatialState;
import com.android.extensions.xr.space.SpatialState;

import com.google.androidxr.splitengine.SplitEngineSubspaceManager;
import com.google.androidxr.splitengine.SubspaceNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

// Technically this doesn't need to be a Robolectric test, since it doesn't directly depend on
// any Android subsystems. However, we're currently using an Android test runner for consistency
// with other Android XR impl tests in this directory.
/**
 * Unit tests for the AndroidXR implementation of JXRCore's SpatialEnvironment module.
 *
 * <p>TODO(b/326748782): Update the FakeExtensions to support better asserts.
 */
@RunWith(RobolectricTestRunner.class)
@SuppressWarnings({"deprecation", "UnnecessarilyFullyQualified"}) // TODO(b/373435470): Remove
public final class SpatialEnvironmentImplTest {
    private static final int SUBSPACE_ID = 5;
    private static final int INVALID_SPLIT_ENGINE_ID = -1;
    private static final long WATER_MATERIAL_ID = 1;
    private final FakeImpressApi mFakeImpressApi = new FakeImpressApi();
    private ActivityController<Activity> mActivityController;
    private Activity mActivity;
    private XrExtensions mXrExtensions = null;
    private Node mSubspaceNode;
    private SubspaceNode mExpectedSubspace;
    private SpatialEnvironmentImpl mEnvironment = null;
    private SplitEngineSubspaceManager mSplitEngineSubspaceManager;
    private NodeRepository mNodeRepository = NodeRepository.getInstance();
    EnvironmentToken mNullSkyboxToken =
            new FakeEnvironmentToken(
                    /* url= */ "nullSkyboxToken", /* textureWidth= */ 1, /* textureHeight= */ 1);
    ListenableFuture<ExrImageResource> mNullSkyboxResourceFuture =
            Futures.immediateFuture(new ExrImageResourceImpl(mNullSkyboxToken));

    @Before
    public void setUp() {
        mActivityController = Robolectric.buildActivity(Activity.class);
        mActivity = mActivityController.create().start().get();
        // Reset our state.
        mXrExtensions = XrExtensionsProvider.getXrExtensions();
        Node sceneRootNode = mXrExtensions.createNode();
        mSubspaceNode = mXrExtensions.createNode();
        mExpectedSubspace = new SubspaceNode(SUBSPACE_ID, mSubspaceNode);

        mSplitEngineSubspaceManager = Mockito.mock(SplitEngineSubspaceManager.class);

        mEnvironment =
                new SpatialEnvironmentImpl(
                        mActivity,
                        mXrExtensions,
                        sceneRootNode,
                        this::getSpatialState,
                        /* useSplitEngine= */ false);
        mEnvironment.onSplitEngineReady(mSplitEngineSubspaceManager, mFakeImpressApi);
        try {
            mEnvironment.onNullSkyboxResourceReady(mNullSkyboxResourceFuture.get());
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void setupSplitEngineEnvironmentImpl() {
        Node sceneRootNode = mXrExtensions.createNode();

        when(mSplitEngineSubspaceManager.createSubspace(anyString(), anyInt()))
                .thenReturn(mExpectedSubspace);

        mEnvironment =
                new SpatialEnvironmentImpl(
                        mActivity,
                        mXrExtensions,
                        sceneRootNode,
                        this::getSpatialState,
                        /* useSplitEngine= */ true);
        mEnvironment.onSplitEngineReady(mSplitEngineSubspaceManager, mFakeImpressApi);
        try {
            mEnvironment.onNullSkyboxResourceReady(mNullSkyboxResourceFuture.get());
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @SuppressWarnings({"FutureReturnValueIgnored", "AndroidJdkLibsChecker"})
    private EnvironmentToken fakeLoadEnvironment(String name) {
        try {
            return mXrExtensions.loadEnvironment(null, 0, 0, name).get();
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return null;
        }
    }

    @SuppressWarnings({"FutureReturnValueIgnored", "AndroidJdkLibsChecker"})
    private long fakeLoadEnvironmentSplitEngine(String name) {
        try {
            return mFakeImpressApi.loadImageBasedLightingAsset(name).get();
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return INVALID_SPLIT_ENGINE_ID;
        }
    }

    @SuppressWarnings({"FutureReturnValueIgnored", "AndroidJdkLibsChecker"})
    private GltfModelToken fakeLoadGltfModel(String name) {
        try {
            return mXrExtensions.loadGltfModel(null, 0, 0, name).get();
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return null;
        }
    }

    @SuppressWarnings({"FutureReturnValueIgnored", "AndroidJdkLibsChecker"})
    private long fakeLoadGltfModelSplitEngine(String name) {
        try {
            return mFakeImpressApi.loadGltfModel(name).get();
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return INVALID_SPLIT_ENGINE_ID;
        }
    }

    @SuppressWarnings({"FutureReturnValueIgnored", "AndroidJdkLibsChecker"})
    private MaterialResource fakeLoadMaterialSplitEngine(boolean isAlphaMapVersion) {
        try {
            return new MaterialResourceImpl(
                    mFakeImpressApi.createWaterMaterial(isAlphaMapVersion).get().getNativeHandle());
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return null;
        }
    }

    private SpatialState getSpatialState() {
        return mXrExtensions.getSpatialState(mActivity);
    }

    @Test
    public void setPassthroughOpacityPreference() {
        mEnvironment.setPassthroughOpacityPreference(null);
        assertThat(mEnvironment.getPassthroughOpacityPreference()).isNull();

        mEnvironment.setPassthroughOpacityPreference(0.1f);
        assertThat(mEnvironment.getPassthroughOpacityPreference()).isEqualTo(0.1f);
    }

    @Test
    public void setPassthroughOpacityPreferenceNearOrUnderZero_getsZeroOpacity() {
        // Opacity values below 1% should be treated as zero.
        mEnvironment.setPassthroughOpacityPreference(0.009f);
        assertThat(mEnvironment.getPassthroughOpacityPreference()).isEqualTo(0.0f);

        mEnvironment.setPassthroughOpacityPreference(-0.1f);
        assertThat(mEnvironment.getPassthroughOpacityPreference()).isEqualTo(0.0f);
    }

    @Test
    public void setPassthroughOpacityPreferenceNearOrOverOne_getsFullOpacity() {
        // Opacity values above 99% should be treated as full opacity.
        mEnvironment.setPassthroughOpacityPreference(0.991f);
        assertThat(mEnvironment.getPassthroughOpacityPreference()).isEqualTo(1.0f);

        mEnvironment.setPassthroughOpacityPreference(1.1f);
        assertThat(mEnvironment.getPassthroughOpacityPreference()).isEqualTo(1.0f);
    }

    @Test
    public void setPassthroughOpacityPreference_returnsAccordingToSpatialCapabilities() {
        // Change should be applied if the spatial capabilities allow it, otherwise should be
        // pending.
        SpatialState state = mXrExtensions.getSpatialState(mActivity);
        ShadowSpatialState.extract(state)
                .setSpatialCapabilities(ShadowSpatialCapabilities.createAll());
        assertThat(mEnvironment.setPassthroughOpacityPreference(0.5f))
                .isEqualTo(SetPassthroughOpacityPreferenceResult.CHANGE_APPLIED);

        ShadowSpatialState.extract(state)
                .setSpatialCapabilities(ShadowSpatialCapabilities.create());
        assertThat(mEnvironment.setPassthroughOpacityPreference(0.6f))
                .isEqualTo(SetPassthroughOpacityPreferenceResult.CHANGE_PENDING);
    }

    @Test
    public void getCurrentPassthroughOpacity_returnsZeroInitially() {
        assertThat(mEnvironment.getCurrentPassthroughOpacity()).isEqualTo(0.0f);
    }

    @Test
    public void onPassthroughOpacityChangedListener_firesOnPassthroughOpacityChange() {
        @SuppressWarnings(value = "unchecked")
        Consumer<Float> listener1 = (Consumer<Float>) mock(Consumer.class);
        @SuppressWarnings(value = "unchecked")
        Consumer<Float> listener2 = (Consumer<Float>) mock(Consumer.class);

        mEnvironment.addOnPassthroughOpacityChangedListener(listener1);
        mEnvironment.addOnPassthroughOpacityChangedListener(listener2);

        mEnvironment.firePassthroughOpacityChangedEvent(0.5f);
        verify(listener1).accept(0.5f);
        verify(listener2).accept(0.5f);

        mEnvironment.removeOnPassthroughOpacityChangedListener(listener1);
        mEnvironment.firePassthroughOpacityChangedEvent(0.0f);
        verify(listener1)
                .accept(any()); // Verify the removed listener was called exactly once total
        verify(listener2).accept(0.0f); // Verify the active listener was called again with false
    }

    @Test
    public void getSpatialEnvironmentPreference_returnsSetSpatialEnvironmentPreference() {
        SpatialEnvironmentPreference preference = mock(SpatialEnvironmentPreference.class);
        mEnvironment.setSpatialEnvironmentPreference(preference);
        assertThat(mEnvironment.getSpatialEnvironmentPreference()).isEqualTo(preference);
    }

    @Test
    public void setSpatialEnvironmentPreference_returnsAppliedWhenCapable() {
        setupSplitEngineEnvironmentImpl();
        // Change should be applied if the spatial capabilities allow it, otherwise should be
        // pending.
        SpatialState state = mXrExtensions.getSpatialState(mActivity);
        ShadowSpatialState.extract(state)
                .setSpatialCapabilities(ShadowSpatialCapabilities.createAll());
        SpatialEnvironmentPreference preference = mock(SpatialEnvironmentPreference.class);
        assertThat(mEnvironment.setSpatialEnvironmentPreference(preference))
                .isEqualTo(SetSpatialEnvironmentPreferenceResult.CHANGE_APPLIED);

        ShadowSpatialState.extract(state)
                .setSpatialCapabilities(ShadowSpatialCapabilities.create());
        preference = mock(SpatialEnvironmentPreference.class);
        assertThat(mEnvironment.setSpatialEnvironmentPreference(preference))
                .isEqualTo(SetSpatialEnvironmentPreferenceResult.CHANGE_PENDING);
    }

    private Node getNodeWithEnvironmentToken(EnvironmentToken token) {
        return mNodeRepository.findNode(
                (NodeRepository.NodeMetadata metadata) ->
                        token.equals(metadata.getEnvironmentToken()));
    }

    private Node getNodeWithGltfToken(GltfModelToken token) {
        return mNodeRepository.findNode(
                (NodeRepository.NodeMetadata metadata) ->
                        token.equals(metadata.getGltfModelToken()));
    }

    @Test
    public void setSpatialEnvironmentPreferenceNull_removesEnvironment() {
        EnvironmentToken exr = fakeLoadEnvironment("fakeEnvironment");
        GltfModelToken gltf = fakeLoadGltfModel("fakeGltfModel");

        // Ensure that an environment is set.
        mEnvironment.setSpatialEnvironmentPreference(
                new SpatialEnvironmentPreference(
                        new ExrImageResourceImpl(exr), new GltfModelResourceImpl(gltf)));

        Node skyboxNode = getNodeWithEnvironmentToken(exr);
        Node geometryNode = getNodeWithGltfToken(gltf);

        assertThat(skyboxNode).isNotNull();
        assertThat(geometryNode).isNotNull();

        assertThat(mNodeRepository.getParent(skyboxNode)).isNotNull();
        assertThat(mNodeRepository.getParent(geometryNode)).isNotNull();

        // Ensure environment is removed
        mEnvironment.setSpatialEnvironmentPreference(null);

        assertThat(mNodeRepository.getParent(skyboxNode)).isNull();
        assertThat(mNodeRepository.getParent(geometryNode)).isNull();
        assertThat(ShadowXrExtensions.extract(mXrExtensions).getEnvironmentNode(mActivity))
                .isNull();
    }

    @Test
    public void setSpatialEnvironmentPreferenceNullWithGeometrySplitEngine_removesEnvironment() {
        setupSplitEngineEnvironmentImpl();

        EnvironmentToken exr = fakeLoadEnvironment("fakeEnvironment");
        long gltf = fakeLoadGltfModelSplitEngine("fakeGltfModel");

        // Ensure that an environment is set.
        mEnvironment.setSpatialEnvironmentPreference(
                new SpatialEnvironmentPreference(
                        new ExrImageResourceImpl(exr), new GltfModelResourceImplSplitEngine(gltf)));

        Node skyboxNode = getNodeWithEnvironmentToken(exr);
        List<Integer> geometryNodes = mFakeImpressApi.getImpressNodesForToken(gltf);
        Map<Long, MaterialData> materials = mFakeImpressApi.getMaterials();
        int animatingNodes = mFakeImpressApi.impressNodeAnimatingSize();
        int loopingAnimatingNodes = mFakeImpressApi.impressNodeLoopAnimatingSize();

        assertThat(skyboxNode).isNotNull();
        assertThat(geometryNodes).isNotEmpty();
        assertThat(materials).isEmpty();
        assertThat(animatingNodes).isEqualTo(0);
        assertThat(loopingAnimatingNodes).isEqualTo(0);

        assertThat(mNodeRepository.getParent(skyboxNode)).isNotNull();
        assertThat(mFakeImpressApi.impressNodeHasParent(geometryNodes.get(0))).isTrue();

        // Ensure environment is removed
        mEnvironment.setSpatialEnvironmentPreference(null);

        assertThat(mNodeRepository.getParent(skyboxNode)).isNull();
        // TODO: b/354711945 - Uncomment when we can test the SetGeometrySplitEngine(null) path.
        // assertThat(fakeImpressApi.impressNodeHasParent(geometryNodes.get(0))).isFalse();
        assertThat(ShadowXrExtensions.extract(mXrExtensions).getEnvironmentNode(mActivity))
                .isNull();
    }

    @Test
    public void
            setSpatialEnvironmentPreferenceNullWithSkyboxAndGeometrySplitEngine_removesEnvironment() {
        setupSplitEngineEnvironmentImpl();

        long exr = fakeLoadEnvironmentSplitEngine("fakeEnvironment");
        long gltf = fakeLoadGltfModelSplitEngine("fakeGltfModel");

        // Ensure that an environment is set.
        mEnvironment.setSpatialEnvironmentPreference(
                new SpatialEnvironmentPreference(
                        new ExrImageResourceImplSplitEngine(exr),
                        new GltfModelResourceImplSplitEngine(gltf)));

        long initialSkybox = mFakeImpressApi.getCurrentEnvironmentLight();
        List<Integer> geometryNodes = mFakeImpressApi.getImpressNodesForToken(gltf);

        assertThat(initialSkybox).isNotEqualTo(INVALID_SPLIT_ENGINE_ID);
        assertThat(geometryNodes).isNotEmpty();
        assertThat(mFakeImpressApi.impressNodeHasParent(geometryNodes.get(0))).isTrue();

        // Ensure environment is removed
        mEnvironment.setSpatialEnvironmentPreference(null);

        long finalSkybox = mFakeImpressApi.getCurrentEnvironmentLight();
        assertThat(finalSkybox).isEqualTo(INVALID_SPLIT_ENGINE_ID);
        // TODO: b/354711945 - Uncomment when we can test the SetGeometrySplitEngine(null) path.
        // assertThat(fakeImpressApi.impressNodeHasParent(geometryNodes.get(0))).isFalse();
        assertThat(ShadowXrExtensions.extract(mXrExtensions).getEnvironmentNode(mActivity))
                .isNull();
    }

    @Test
    public void
            setSpatialEnvironmentPreferenceWithNullSkyboxAndGeometry_doesNotDetachEnvironment() {
        EnvironmentToken exr = fakeLoadEnvironment("fakeEnvironment");
        GltfModelToken gltf = fakeLoadGltfModel("fakeGltfModel");

        // Ensure that an environment is set.
        mEnvironment.setSpatialEnvironmentPreference(
                new SpatialEnvironmentPreference(
                        new ExrImageResourceImpl(exr), new GltfModelResourceImpl(gltf)));

        Node skyboxNode = getNodeWithEnvironmentToken(exr);
        Node geometryNode = getNodeWithGltfToken(gltf);

        assertThat(skyboxNode).isNotNull();
        assertThat(geometryNode).isNotNull();

        assertThat(mNodeRepository.getParent(skyboxNode)).isNotNull();
        assertThat(mNodeRepository.getParent(geometryNode)).isNotNull();

        // Ensure environment is not removed if both skybox and geometry are updated to null.
        mEnvironment.setSpatialEnvironmentPreference(new SpatialEnvironmentPreference(null, null));

        assertThat(mNodeRepository.getParent(skyboxNode))
                .isNull(); // Skybox should be set to a black skybox node.
        assertThat(mNodeRepository.getParent(geometryNode)).isNull();

        // The skybox should be set to a black skybox node. This isn't relevant for end users but it
        // confirms the environment implementation is working as designed.
        assertThat(ShadowXrExtensions.extract(mXrExtensions).getEnvironmentNode(mActivity))
                .isNotNull();
    }

    @Test
    public void
            setSpatialEnvironmentPreferenceWithNullSkyboxExtensionAndNullGeometrySplitEngine_doesNotDetachEnvironment() {
        setupSplitEngineEnvironmentImpl();
        EnvironmentToken exr = fakeLoadEnvironment("fakeEnvironment");
        long gltf = fakeLoadGltfModelSplitEngine("fakeGltfModel");

        // Ensure that an environment is set.
        mEnvironment.setSpatialEnvironmentPreference(
                new SpatialEnvironmentPreference(
                        new ExrImageResourceImpl(exr), new GltfModelResourceImplSplitEngine(gltf)));

        Node skyboxNode = getNodeWithEnvironmentToken(exr);
        List<Integer> geometryNodes = mFakeImpressApi.getImpressNodesForToken(gltf);

        assertThat(skyboxNode).isNotNull();
        assertThat(geometryNodes).isNotEmpty();

        assertThat(mNodeRepository.getParent(skyboxNode)).isNotNull();
        assertThat(mFakeImpressApi.impressNodeHasParent(geometryNodes.get(0))).isTrue();

        // Ensure environment is not removed if both skybox and geometry are updated to null.
        mEnvironment.setSpatialEnvironmentPreference(new SpatialEnvironmentPreference(null, null));
        skyboxNode = getNodeWithEnvironmentToken(mNullSkyboxToken);
        assertThat(skyboxNode).isNotNull(); // Skybox should be set to a black skybox node.
        assertThat(mNodeRepository.getParent(skyboxNode)).isNotNull();
        // TODO: b/354711945 - Uncomment when we can test the SetGeometrySplitEngine(null) path.
        // assertThat(fakeImpressApi.impressNodeHasParent(geometryNodes.get(0))).isFalse();

        // The skybox should be set to a black skybox node. This isn't relevant for end users but it
        // confirms the environment implementation is working as designed.
        assertThat(ShadowXrExtensions.extract(mXrExtensions).getEnvironmentNode(mActivity))
                .isNotNull();
    }

    @Test
    public void
            setSpatialEnvironmentPreferenceWithNullSkyboxSplitEngineAndNullGeometrySplitEngine_doesNotDetachEnvironment() {
        setupSplitEngineEnvironmentImpl();
        long exr = fakeLoadEnvironmentSplitEngine("fakeEnvironment");
        long gltf = fakeLoadGltfModelSplitEngine("fakeGltfModel");

        // Ensure that an environment is set.
        mEnvironment.setSpatialEnvironmentPreference(
                new SpatialEnvironmentPreference(
                        new ExrImageResourceImplSplitEngine(exr),
                        new GltfModelResourceImplSplitEngine(gltf)));

        long initialSkybox = mFakeImpressApi.getCurrentEnvironmentLight();
        List<Integer> geometryNodes = mFakeImpressApi.getImpressNodesForToken(gltf);

        assertThat(initialSkybox).isNotEqualTo(INVALID_SPLIT_ENGINE_ID);
        assertThat(geometryNodes).isNotEmpty();
        assertThat(mFakeImpressApi.impressNodeHasParent(geometryNodes.get(0))).isTrue();

        // Ensure environment is not removed if both skybox and geometry are updated to null.
        mEnvironment.setSpatialEnvironmentPreference(new SpatialEnvironmentPreference(null, null));

        long finalSkybox = mFakeImpressApi.getCurrentEnvironmentLight();
        assertThat(finalSkybox)
                .isNotEqualTo(
                        INVALID_SPLIT_ENGINE_ID); // Skybox should be set to a black skybox node.
        // TODO: b/354711945 - Uncomment when we can test the SetGeometrySplitEngine(null) path.
        // assertThat(fakeImpressApi.impressNodeHasParent(geometryNodes.get(0))).isFalse();

        // The skybox should be set to a black skybox node. This isn't relevant for end users but it
        // confirms the environment implementation is working as designed.
        assertThat(ShadowXrExtensions.extract(mXrExtensions).getEnvironmentNode(mActivity))
                .isNotNull();
    }

    @Test
    public void
            setSpatialEnvironmentPreferenceWithSkyboxSplitEngineAndGeometryWithMeshAndAnimationSplitEngine_doesNotDetachEnvironment() {
        setupSplitEngineEnvironmentImpl();
        long exr = fakeLoadEnvironmentSplitEngine("fakeEnvironment");
        long gltf = fakeLoadGltfModelSplitEngine("fakeGltfModel");
        // Create dummy regular version of the water material.
        MaterialResource material = fakeLoadMaterialSplitEngine(false);
        String meshName = "fakeMesh";
        String animationName = "fakeAnimation";

        // Ensure that an environment is set.
        mEnvironment.setSpatialEnvironmentPreference(
                new SpatialEnvironmentPreference(
                        new ExrImageResourceImplSplitEngine(exr),
                        new GltfModelResourceImplSplitEngine(gltf),
                        material,
                        meshName,
                        animationName));

        long initialSkybox = mFakeImpressApi.getCurrentEnvironmentLight();
        List<Integer> geometryNodes = mFakeImpressApi.getImpressNodesForToken(gltf);
        Map<Long, MaterialData> materials = mFakeImpressApi.getMaterials();
        int animatingNodes = mFakeImpressApi.impressNodeAnimatingSize();
        int loopingAnimatingNodes = mFakeImpressApi.impressNodeLoopAnimatingSize();

        assertThat(initialSkybox).isNotEqualTo(INVALID_SPLIT_ENGINE_ID);
        assertThat(geometryNodes).isNotEmpty();
        assertThat(mFakeImpressApi.impressNodeHasParent(geometryNodes.get(0))).isTrue();
        assertThat(materials).isNotEmpty();
        assertThat(materials.keySet().toArray()[0]).isEqualTo(WATER_MATERIAL_ID);
        assertThat(materials.get(WATER_MATERIAL_ID).type).isEqualTo(MaterialData.Type.WATER);
        assertThat(animatingNodes).isEqualTo(0);
        assertThat(loopingAnimatingNodes).isEqualTo(1);

        // Ensure environment is not removed if both skybox and geometry are updated to null.
        mEnvironment.setSpatialEnvironmentPreference(
                new SpatialEnvironmentPreference(null, null, null, null, null));

        long finalSkybox = mFakeImpressApi.getCurrentEnvironmentLight();
        assertThat(finalSkybox)
                .isNotEqualTo(
                        INVALID_SPLIT_ENGINE_ID); // Skybox should be set to a black skybox node.
        // TODO: b/354711945 - Uncomment when we can test the SetGeometrySplitEngine(null) path.
        // assertThat(fakeImpressApi.impressNodeHasParent(geometryNodes.get(0))).isFalse();

        // The skybox should be set to a black skybox node. This isn't relevant for end users but it
        // confirms the environment implementation is working as designed.
        assertThat(ShadowXrExtensions.extract(mXrExtensions).getEnvironmentNode(mActivity))
                .isNotNull();
    }

    @Test
    public void
            setSpatialEnvironmentPreferenceWithNullSkyboxAndGeometry_attachesEnvironmentWithSkybox() {
        setupSplitEngineEnvironmentImpl();
        // Ensure environment is attached if both skybox and geometry are set to null at start.
        mEnvironment.setSpatialEnvironmentPreference(new SpatialEnvironmentPreference(null, null));
        Node skyboxNode = getNodeWithEnvironmentToken(mNullSkyboxToken);
        assertThat(skyboxNode).isNotNull();
        assertThat(mNodeRepository.getParent(skyboxNode)).isNotNull();

        // The skybox should be set to a black skybox node. This isn't relevant for end users but it
        // confirms the environment implementation is working as designed.
        assertThat(ShadowXrExtensions.extract(mXrExtensions).getEnvironmentNode(mActivity))
                .isNotNull();
    }

    @Test
    public void
            setSpatialEnvironmentPreferenceWithNullSkyboxAndGeometryButMisingResource_doesntAttachEnvironment() {
        // Reset our state and setup environment with null skybox resource.
        ShadowXrExtensions.extract(mXrExtensions).removeStateFor(mActivity);

        Node sceneRootNode = mXrExtensions.createNode();
        mSubspaceNode = mXrExtensions.createNode();
        mExpectedSubspace = new SubspaceNode(SUBSPACE_ID, mSubspaceNode);
        mSplitEngineSubspaceManager = Mockito.mock(SplitEngineSubspaceManager.class);
        when(mSplitEngineSubspaceManager.createSubspace(anyString(), anyInt()))
                .thenReturn(mExpectedSubspace);
        mEnvironment =
                new SpatialEnvironmentImpl(
                        mActivity,
                        mXrExtensions,
                        sceneRootNode,
                        this::getSpatialState,
                        /* useSplitEngine= */ true);
        mEnvironment.onSplitEngineReady(mSplitEngineSubspaceManager, mFakeImpressApi);
        mEnvironment.onNullSkyboxResourceReady(null);

        // If the skybox resource is missing, we don't attach anything to the environment.
        mEnvironment.setSpatialEnvironmentPreference(new SpatialEnvironmentPreference(null, null));
        Node skyboxNode = getNodeWithEnvironmentToken(mNullSkyboxToken);
        assertThat(skyboxNode).isNull();
        assertThat(ShadowXrExtensions.extract(mXrExtensions).getEnvironmentNode(mActivity))
                .isNull();
    }

    @Test
    public void
            setSpatialEnvironmentPreferenceFromNullPreferenceToNullSkyboxAndGeometrySplitEngine_doesNotDetachEnvironment() {
        setupSplitEngineEnvironmentImpl();
        EnvironmentToken exr = fakeLoadEnvironment("fakeEnvironment");
        long gltf = fakeLoadGltfModelSplitEngine("fakeGltfModel");

        // Ensure that an environment is set.
        mEnvironment.setSpatialEnvironmentPreference(null);

        Node skyboxNode = getNodeWithEnvironmentToken(exr);
        List<Integer> geometryNodes = mFakeImpressApi.getImpressNodesForToken(gltf);

        assertThat(skyboxNode).isNull();
        assertThat(geometryNodes).isEmpty();

        // Ensure environment is not removed if both skybox and geometry are updated to null.
        mEnvironment.setSpatialEnvironmentPreference(new SpatialEnvironmentPreference(null, null));
        skyboxNode = getNodeWithEnvironmentToken(mNullSkyboxToken);
        assertThat(skyboxNode).isNotNull(); // Skybox should be set to a black skybox node.
        assertThat(mNodeRepository.getParent(skyboxNode)).isNotNull();
        // TODO: b/354711945 - Uncomment when we can test the SetGeometrySplitEngine(null) path.
        // assertThat(fakeImpressApi.impressNodeHasParent(geometryNodes.get(0))).isFalse();

        // The skybox should be set to a black skybox node. This isn't relevant for end users but it
        // confirms the environment implementation is working as designed.
        assertThat(ShadowXrExtensions.extract(mXrExtensions).getEnvironmentNode(mActivity))
                .isNotNull();
    }

    @Test
    public void setNewSpatialEnvironmentPreference_replacesOldSpatialEnvironmentPreference() {
        EnvironmentToken exr = fakeLoadEnvironment("fakeEnvironment");
        EnvironmentToken newExr = fakeLoadEnvironment("newFakeEnvironment");
        GltfModelToken gltf = fakeLoadGltfModel("fakeGltfModel");
        GltfModelToken newGltf = fakeLoadGltfModel("newFakeGltfModel");

        // Ensure that an environment is set a first time.
        mEnvironment.setSpatialEnvironmentPreference(
                new SpatialEnvironmentPreference(
                        new ExrImageResourceImpl(exr), new GltfModelResourceImpl(gltf)));

        Node skyboxNode = getNodeWithEnvironmentToken(exr);
        Node geometryNode = getNodeWithGltfToken(gltf);

        // Ensure that an environment is set a second time.
        mEnvironment.setSpatialEnvironmentPreference(
                new SpatialEnvironmentPreference(
                        new ExrImageResourceImpl(newExr), new GltfModelResourceImpl(newGltf)));

        Node newSkyboxNode = getNodeWithEnvironmentToken(newExr);
        Node newGeometryNode = getNodeWithGltfToken(newGltf);

        // None of the nodes should be null.
        assertThat(skyboxNode).isNotNull();
        assertThat(geometryNode).isNotNull();
        assertThat(newSkyboxNode).isNotNull();
        assertThat(newGeometryNode).isNotNull();

        // Only the new nodes should have a parent.
        assertThat(mNodeRepository.getParent(skyboxNode)).isNull();
        assertThat(mNodeRepository.getParent(geometryNode)).isNull();
        assertThat(mNodeRepository.getParent(newSkyboxNode)).isNotNull();
        assertThat(mNodeRepository.getParent(newGeometryNode)).isNotNull();

        // The names should be the same, but the resources should be different.
        assertThat(mNodeRepository.getEnvironmentToken(skyboxNode))
                .isNotEqualTo(mNodeRepository.getEnvironmentToken(newSkyboxNode));
        assertThat(mNodeRepository.getName(skyboxNode))
                .isEqualTo(SpatialEnvironmentImpl.SKYBOX_NODE_NAME);
        assertThat(mNodeRepository.getName(newSkyboxNode))
                .isEqualTo(SpatialEnvironmentImpl.SKYBOX_NODE_NAME);
        assertThat(mNodeRepository.getGltfModelToken(geometryNode))
                .isNotEqualTo(mNodeRepository.getGltfModelToken(newGeometryNode));
        assertThat(mNodeRepository.getName(geometryNode))
                .isEqualTo(SpatialEnvironmentImpl.GEOMETRY_NODE_NAME);
        assertThat(mNodeRepository.getName(newGeometryNode))
                .isEqualTo(SpatialEnvironmentImpl.GEOMETRY_NODE_NAME);

        // The environment node should still be attached.
        assertThat(ShadowXrExtensions.extract(mXrExtensions).getEnvironmentNode(mActivity))
                .isNotNull();
    }

    @Test
    public void
            setNewSpatialEnvironmentPreferenceSplitEngine_replacesOldSpatialEnvironmentPreference() {
        setupSplitEngineEnvironmentImpl();
        EnvironmentToken exr = fakeLoadEnvironment("fakeEnvironment");
        EnvironmentToken newExr = fakeLoadEnvironment("newFakeEnvironment");
        long gltf = fakeLoadGltfModelSplitEngine("fakeGltfModel");
        long newGltf = fakeLoadGltfModelSplitEngine("newFakeGltfModel");

        // Ensure that an environment is set a first time.
        mEnvironment.setSpatialEnvironmentPreference(
                new SpatialEnvironmentPreference(
                        new ExrImageResourceImpl(exr), new GltfModelResourceImplSplitEngine(gltf)));

        Node skyboxNode = getNodeWithEnvironmentToken(exr);
        List<Integer> geometryNodes = mFakeImpressApi.getImpressNodesForToken(gltf);

        // Ensure that an environment is set a second time.
        mEnvironment.setSpatialEnvironmentPreference(
                new SpatialEnvironmentPreference(
                        new ExrImageResourceImpl(newExr),
                        new GltfModelResourceImplSplitEngine(newGltf)));

        Node newSkyboxNode = getNodeWithEnvironmentToken(newExr);
        List<Integer> newGeometryNodes = mFakeImpressApi.getImpressNodesForToken(newGltf);

        // None of the nodes should be null.
        assertThat(skyboxNode).isNotNull();
        assertThat(geometryNodes).isNotEmpty();
        assertThat(newSkyboxNode).isNotNull();
        assertThat(newGeometryNodes).isNotEmpty();

        // Only the new nodes should have a parent.
        assertThat(mNodeRepository.getParent(skyboxNode)).isNull();
        // TODO: b/354711945 - Uncomment when we can test the SetGeometrySplitEngine(null) path.
        // assertThat(fakeImpressApi.impressNodeHasParent(geometryNodes.get(0))).isFalse();
        assertThat(mNodeRepository.getParent(newSkyboxNode)).isNotNull();
        assertThat(mFakeImpressApi.impressNodeHasParent(newGeometryNodes.get(0))).isTrue();

        // The resources should be different.
        assertThat(mNodeRepository.getEnvironmentToken(skyboxNode))
                .isNotEqualTo(mNodeRepository.getEnvironmentToken(newSkyboxNode));
        assertThat(mNodeRepository.getName(skyboxNode))
                .isEqualTo(SpatialEnvironmentImpl.SKYBOX_NODE_NAME);
        assertThat(mNodeRepository.getName(newSkyboxNode))
                .isEqualTo(SpatialEnvironmentImpl.SKYBOX_NODE_NAME);
        assertThat(geometryNodes.get(0)).isNotEqualTo(newGeometryNodes.get(0));

        // The environment node should still be attached.
        assertThat(ShadowXrExtensions.extract(mXrExtensions).getEnvironmentNode(mActivity))
                .isNotNull();
    }

    @Test
    public void
            setSpatialEnvironmentPreferenceWithSplitEngineAndGeometryWithMaterialAndMeshNameSplitEngine_materialIsOverriden() {
        setupSplitEngineEnvironmentImpl();
        long exr = fakeLoadEnvironmentSplitEngine("fakeEnvironment");
        long gltf = fakeLoadGltfModelSplitEngine("fakeGltfModel");
        // Create dummy regular version of the water material.
        MaterialResource material = fakeLoadMaterialSplitEngine(false);
        String meshName = "fakeMesh";
        String animationName = "fakeAnimation";

        // Ensure that an environment is set.
        mEnvironment.setSpatialEnvironmentPreference(
                new SpatialEnvironmentPreference(
                        new ExrImageResourceImplSplitEngine(exr),
                        new GltfModelResourceImplSplitEngine(gltf),
                        material,
                        meshName,
                        animationName));

        Map<Long, MaterialData> materials = mFakeImpressApi.getMaterials();
        int loopingAnimatingNodes = mFakeImpressApi.impressNodeLoopAnimatingSize();

        assertThat(
                        mFakeImpressApi.getImpressNodes().keySet().stream()
                                .filter(
                                        node ->
                                                node.materialOverride != null
                                                        && node.materialOverride.type
                                                                == MaterialData.Type.WATER)
                                .toArray())
                .hasLength(1); // 1 glTF node that should be overridden with the water material.

        assertThat(materials).isNotEmpty();
        assertThat(materials.keySet().toArray()[0]).isEqualTo(WATER_MATERIAL_ID);
        assertThat(materials.get(WATER_MATERIAL_ID).type).isEqualTo(MaterialData.Type.WATER);
        assertThat(loopingAnimatingNodes).isEqualTo(1);
    }

    @Test
    public void
            setSpatialEnvironmentPreferenceWithSplitEngineAndGeometryWithMaterialAndNoMeshNameSplitEngine_materialIsNotOverriden() {
        setupSplitEngineEnvironmentImpl();
        long exr = fakeLoadEnvironmentSplitEngine("fakeEnvironment");
        long gltf = fakeLoadGltfModelSplitEngine("fakeGltfModel");
        // Create dummy regular version of the water material.
        MaterialResource material = fakeLoadMaterialSplitEngine(false);
        String animationName = "fakeAnimation";

        // Ensure that an environment is set.
        mEnvironment.setSpatialEnvironmentPreference(
                new SpatialEnvironmentPreference(
                        new ExrImageResourceImplSplitEngine(exr),
                        new GltfModelResourceImplSplitEngine(gltf),
                        material,
                        null,
                        animationName));

        Map<Long, MaterialData> materials = mFakeImpressApi.getMaterials();

        assertThat(
                        mFakeImpressApi.getImpressNodes().keySet().stream()
                                .filter(node -> node.materialOverride == null)
                                .toArray())
                .hasLength(2); // 2 nodes are subspace (parent) and glTF (child) used for the
        // environment. Both
        // have no material override so we expect the length of the filter to be 2.

        assertThat(materials).isNotEmpty();
        assertThat(materials.keySet().toArray()[0]).isEqualTo(WATER_MATERIAL_ID);
        assertThat(materials.get(WATER_MATERIAL_ID).type).isEqualTo(MaterialData.Type.WATER);
    }

    @Test
    public void
            setSpatialEnvironmentPreferenceWithSplitEngineAndGeometryWithNoMaterialAndMeshNameSplitEngine_materialIsNotOverriden() {
        setupSplitEngineEnvironmentImpl();
        long exr = fakeLoadEnvironmentSplitEngine("fakeEnvironment");
        long gltf = fakeLoadGltfModelSplitEngine("fakeGltfModel");
        String meshName = "fakeMesh";
        String animationName = "fakeAnimation";

        // Ensure that an environment is set.
        mEnvironment.setSpatialEnvironmentPreference(
                new SpatialEnvironmentPreference(
                        new ExrImageResourceImplSplitEngine(exr),
                        new GltfModelResourceImplSplitEngine(gltf),
                        null,
                        meshName,
                        animationName));

        Map<Long, MaterialData> materials = mFakeImpressApi.getMaterials();

        assertThat(
                        mFakeImpressApi.getImpressNodes().keySet().stream()
                                .filter(node -> node.materialOverride == null)
                                .toArray())
                .hasLength(2); // 2 nodes are subspace (parent) and glTF (child) used for the
        // environment. Both
        // have no material override so we expect the length of the filter to be 2.

        assertThat(materials).isEmpty();
    }

    @Test
    public void
            setSpatialEnvironmentPreferenceWithSplitEngineAndGeometryWithNoAnimationNameSplitEngine_geometryIsNotAnimating() {
        setupSplitEngineEnvironmentImpl();
        long exr = fakeLoadEnvironmentSplitEngine("fakeEnvironment");
        long gltf = fakeLoadGltfModelSplitEngine("fakeGltfModel");
        String animationName = "fakeAnimation";

        // Ensure that an environment is set.
        mEnvironment.setSpatialEnvironmentPreference(
                new SpatialEnvironmentPreference(
                        new ExrImageResourceImplSplitEngine(exr),
                        new GltfModelResourceImplSplitEngine(gltf),
                        null,
                        null,
                        animationName));

        int loopingAnimatingNodes = mFakeImpressApi.impressNodeLoopAnimatingSize();
        Map<Long, MaterialData> materials = mFakeImpressApi.getMaterials();

        assertThat(loopingAnimatingNodes).isEqualTo(1);

        assertThat(
                        mFakeImpressApi.getImpressNodes().keySet().stream()
                                .filter(node -> node.materialOverride == null)
                                .toArray())
                .hasLength(2); // 2 nodes are subspace (parent) and glTF (child) used for the
        // environment. Both
        // have no material override so we expect the length of the filter to be 2.
        assertThat(materials).isEmpty();
    }

    @Test
    public void isSpatialEnvironmentPreferenceActive_defaultsToFalse() {
        assertThat(mEnvironment.isSpatialEnvironmentPreferenceActive()).isFalse();
    }

    @Test
    public void onSpatialEnvironmentChangedListener_firesOnEnvironmentChange() {
        @SuppressWarnings(value = "unchecked")
        Consumer<Boolean> listener1 = (Consumer<Boolean>) mock(Consumer.class);
        @SuppressWarnings(value = "unchecked")
        Consumer<Boolean> listener2 = (Consumer<Boolean>) mock(Consumer.class);

        mEnvironment.addOnSpatialEnvironmentChangedListener(listener1);
        mEnvironment.addOnSpatialEnvironmentChangedListener(listener2);

        mEnvironment.fireOnSpatialEnvironmentChangedEvent(true);
        verify(listener1).accept(true);
        verify(listener2).accept(true);

        mEnvironment.removeOnSpatialEnvironmentChangedListener(listener1);
        mEnvironment.fireOnSpatialEnvironmentChangedEvent(false);
        verify(listener1)
                .accept(any()); // Verify the removed listener was called exactly once total
        verify(listener2).accept(false); // Verify the active listener was called again with false
    }

    @Test
    public void dispose_clearsSpatialEnvironmentPreferenceListeners() {
        @SuppressWarnings(value = "unchecked")
        Consumer<Boolean> listener = (Consumer<Boolean>) mock(Consumer.class);
        mEnvironment.addOnSpatialEnvironmentChangedListener(listener);

        mEnvironment.fireOnSpatialEnvironmentChangedEvent(true);
        verify(listener).accept(true);

        mEnvironment.dispose();
        mEnvironment.fireOnSpatialEnvironmentChangedEvent(false);
        verify(listener, never()).accept(false);
    }

    @Test
    public void dispose_clearsPassthroughOpacityPreferenceListeners() {
        @SuppressWarnings(value = "unchecked")
        Consumer<Float> listener = (Consumer<Float>) mock(Consumer.class);
        mEnvironment.addOnPassthroughOpacityChangedListener(listener);

        mEnvironment.firePassthroughOpacityChangedEvent(1.0f);
        verify(listener).accept(1.0f);

        // Ensure the listener is called exactly once, even if the event is fired after dispose.
        mEnvironment.dispose();
        mEnvironment.firePassthroughOpacityChangedEvent(0.5f);
        verify(listener).accept(any());
    }

    @Test
    public void dispose_clearsNullSkyboxResource() {
        mEnvironment.setSpatialEnvironmentPreference(new SpatialEnvironmentPreference(null, null));
        assertThat(ShadowXrExtensions.extract(mXrExtensions).getEnvironmentNode(mActivity))
                .isNotNull();

        // Ensure null skybox resource is cleared and doesn't re-attach the environment after a
        // dispose.
        mEnvironment.dispose();
        // TODO(b/401587057): Follow up on this assertThrows which is new behaviour.
        Exception thrown =
                assertThrows(
                        NullPointerException.class,
                        () ->
                                mEnvironment.setSpatialEnvironmentPreference(
                                        new SpatialEnvironmentPreference(null, null)));
        assertThat(thrown).hasMessageThat().contains("Activity cannot be null");
        assertThat(ShadowXrExtensions.extract(mXrExtensions).getEnvironmentNode(mActivity))
                .isNull();
    }

    @Test
    public void dispose_clearsResources() {
        EnvironmentToken exr = fakeLoadEnvironment("fakeEnvironment");
        GltfModelToken gltf = fakeLoadGltfModel("fakeGltfModel");
        SpatialState spatialState = ShadowSpatialState.create();
        ShadowSpatialState.extract(spatialState)
                .setEnvironmentVisibilityState(
                        /* environmentVisibilityState= */ ShadowEnvironmentVisibilityState.create(
                                EnvironmentVisibilityState.APP_VISIBLE));
        ShadowSpatialState.extract(spatialState)
                .setPassthroughVisibilityState(
                        /* passthroughVisibilityState= */ ShadowPassthroughVisibilityState.create(
                                PassthroughVisibilityState.APP, 0.5f));

        mEnvironment.setSpatialState(spatialState);

        mEnvironment.setSpatialEnvironmentPreference(
                new SpatialEnvironmentPreference(
                        new ExrImageResourceImpl(exr), new GltfModelResourceImpl(gltf)));
        mEnvironment.setPassthroughOpacityPreference(0.5f);

        Node skyboxNode = getNodeWithEnvironmentToken(exr);
        Node geometryNode = getNodeWithGltfToken(gltf);

        assertThat(skyboxNode).isNotNull();
        assertThat(geometryNode).isNotNull();

        assertThat(mNodeRepository.getParent(skyboxNode)).isNotNull();
        assertThat(mNodeRepository.getParent(geometryNode)).isNotNull();

        assertThat(mEnvironment.getSpatialEnvironmentPreference()).isNotNull();
        assertThat(mEnvironment.isSpatialEnvironmentPreferenceActive()).isTrue();

        assertThat(mEnvironment.getPassthroughOpacityPreference()).isNotNull();
        assertThat(mEnvironment.getCurrentPassthroughOpacity()).isEqualTo(0.5f);

        mEnvironment.dispose();
        assertThat(mNodeRepository.getParent(skyboxNode)).isNull();
        assertThat(mNodeRepository.getParent(geometryNode)).isNull();
        assertThat(ShadowXrExtensions.extract(mXrExtensions).getEnvironmentNode(mActivity))
                .isNull();
        assertThat(mEnvironment.getSpatialEnvironmentPreference()).isNull();
        assertThat(mEnvironment.isSpatialEnvironmentPreferenceActive()).isFalse();
        assertThat(mEnvironment.getPassthroughOpacityPreference()).isNull();
        assertThat(mEnvironment.getCurrentPassthroughOpacity()).isEqualTo(0.0f);
    }
}
