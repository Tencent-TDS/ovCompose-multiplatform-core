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
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;

import androidx.xr.extensions.node.NodeTypeConverter;
import androidx.xr.extensions.node.ReformEvent;
import androidx.xr.extensions.node.ReformOptions;
import androidx.xr.runtime.math.Pose;
import androidx.xr.scenecore.JxrPlatformAdapter;
import androidx.xr.scenecore.JxrPlatformAdapter.Dimensions;
import androidx.xr.scenecore.JxrPlatformAdapter.Entity;
import androidx.xr.scenecore.JxrPlatformAdapter.MoveEventListener;
import androidx.xr.scenecore.JxrPlatformAdapter.ResizeEvent;
import androidx.xr.scenecore.JxrPlatformAdapter.ResizeEventListener;
import androidx.xr.scenecore.impl.extensions.XrExtensionsProvider;
import androidx.xr.scenecore.impl.perception.PerceptionLibrary;
import androidx.xr.scenecore.impl.perception.Session;
import androidx.xr.scenecore.testing.FakeImpressApi;
import androidx.xr.scenecore.testing.FakeScheduledExecutorService;
import androidx.xr.scenecore.testing.FakeXrExtensions.FakeNode;

import com.android.extensions.xr.XrExtensions;
import com.android.extensions.xr.node.Node;
import com.android.extensions.xr.node.ShadowReformEvent;
import com.android.extensions.xr.node.Vec3;

import com.google.androidxr.splitengine.SplitEngineSubspaceManager;
import com.google.ar.imp.view.splitengine.ImpSplitEngineRenderer;
import com.google.common.collect.ImmutableSet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

@RunWith(RobolectricTestRunner.class)
public class ResizableComponentImplTest {
    private static final Dimensions kMinDimensions = new Dimensions(0f, 0f, 0f);
    private static final Dimensions kMaxDimensions = new Dimensions(10f, 10f, 10f);
    private final ActivityController<Activity> mActivityController =
            Robolectric.buildActivity(Activity.class);
    private final Activity mActivity = mActivityController.create().start().get();
    private final FakeScheduledExecutorService mFakeExecutor = new FakeScheduledExecutorService();
    private final PerceptionLibrary mPerceptionLibrary = mock(PerceptionLibrary.class);
    private final XrExtensions mXrExtensions = XrExtensionsProvider.getXrExtensions();
    private final FakeImpressApi mFakeImpressApiImpl = new FakeImpressApi();
    private final EntityManager mEntityManager = new EntityManager();
    private final Node mActivitySpaceNode = mXrExtensions.createNode();
    private final ActivitySpaceImpl mActivitySpaceImpl =
            new ActivitySpaceImpl(
                    mActivitySpaceNode,
                    mXrExtensions,
                    mEntityManager,
                    () -> mXrExtensions.getSpatialState(mActivity),
                    mFakeExecutor);
    private final AndroidXrEntity mActivitySpaceRoot = Mockito.mock(AndroidXrEntity.class);
    private final PerceptionSpaceActivityPoseImpl mPerceptionSpaceActivityPose =
            new PerceptionSpaceActivityPoseImpl(mActivitySpaceImpl, mActivitySpaceRoot);
    private final PanelShadowRenderer mPanelShadowRenderer =
            Mockito.mock(PanelShadowRenderer.class);

    private final SplitEngineSubspaceManager mSplitEngineSubspaceManager =
            Mockito.mock(SplitEngineSubspaceManager.class);
    private final ImpSplitEngineRenderer mSplitEngineRenderer =
            Mockito.mock(ImpSplitEngineRenderer.class);
    private JxrPlatformAdapter mFakeRuntime;

    @Before
    public void setUp() {
        when(mPerceptionLibrary.initSession(eq(mActivity), anyInt(), eq(mFakeExecutor)))
                .thenReturn(immediateFuture(mock(Session.class)));
        mFakeRuntime =
                JxrPlatformAdapterAxr.create(
                        mActivity,
                        mFakeExecutor,
                        mXrExtensions,
                        mFakeImpressApiImpl,
                        mEntityManager,
                        mPerceptionLibrary,
                        mSplitEngineSubspaceManager,
                        mSplitEngineRenderer,
                        /* useSplitEngine= */ false);
    }

    @After
    public void tearDown() {
        // Dispose the runtime between test cases to clean up lingering references.
        mFakeRuntime.dispose();
    }

    private Entity createTestEntity() {
        return mFakeRuntime.createEntity(new Pose(), "test", mFakeRuntime.getActivitySpace());
    }

    @Test
    public void addResizableComponentToTwoEntity_fails() {
        Entity entity1 = createTestEntity();
        Entity entity2 = createTestEntity();
        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor, mXrExtensions, kMinDimensions, kMaxDimensions);
        assertThat(resizableComponent).isNotNull();
        assertThat(entity1.addComponent(resizableComponent)).isTrue();
        assertThat(entity2.addComponent(resizableComponent)).isFalse();
    }

    @Test
    public void addResizableComponent_addsReformOptionsToNode() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();
        assertThat(entity).isNotNull();
        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor, mXrExtensions, kMinDimensions, kMaxDimensions);
        assertThat(resizableComponent).isNotNull();

        assertThat(entity.addComponent(resizableComponent)).isTrue();

        FakeNode node = new FakeNode(entity.getNode());
        assertThat(node.getReformOptions().getEnabledReform())
                .isEqualTo(ReformOptions.ALLOW_RESIZE);
        assertThat(node.getReformOptions().getMinimumSize().x).isEqualTo(kMinDimensions.width);
        assertThat(node.getReformOptions().getMinimumSize().y).isEqualTo(kMinDimensions.height);
        assertThat(node.getReformOptions().getMinimumSize().z).isEqualTo(kMinDimensions.depth);
        assertThat(node.getReformOptions().getMaximumSize().x).isEqualTo(kMaxDimensions.width);
        assertThat(node.getReformOptions().getMaximumSize().y).isEqualTo(kMaxDimensions.height);
        assertThat(node.getReformOptions().getMaximumSize().z).isEqualTo(kMaxDimensions.depth);
    }

    @Test
    public void setSizeOnResizableComponent_setsSizeOnNodeReformOptions() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();
        assertThat(entity).isNotNull();
        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor, mXrExtensions, kMinDimensions, kMaxDimensions);
        assertThat(resizableComponent).isNotNull();

        assertThat(entity.addComponent(resizableComponent)).isTrue();
        FakeNode node = new FakeNode(entity.getNode());

        resizableComponent.setSize(kMaxDimensions);
        assertThat(node.getReformOptions().getCurrentSize().x).isEqualTo(kMaxDimensions.width);
        assertThat(node.getReformOptions().getCurrentSize().y).isEqualTo(kMaxDimensions.height);
        assertThat(node.getReformOptions().getCurrentSize().z).isEqualTo(kMaxDimensions.depth);
    }

    @Test
    public void setMinimumSizeOnResizableComponent_setsMinimumSizeOnNodeReformOptions() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();
        assertThat(entity).isNotNull();
        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor, mXrExtensions, kMinDimensions, kMaxDimensions);
        assertThat(resizableComponent).isNotNull();

        assertThat(entity.addComponent(resizableComponent)).isTrue();
        FakeNode node = new FakeNode(entity.getNode());

        resizableComponent.setMinimumSize(kMaxDimensions);
        assertThat(node.getReformOptions().getMinimumSize().x).isEqualTo(kMaxDimensions.width);
        assertThat(node.getReformOptions().getMinimumSize().y).isEqualTo(kMaxDimensions.height);
        assertThat(node.getReformOptions().getMinimumSize().z).isEqualTo(kMaxDimensions.depth);
    }

    @Test
    public void setMaximumSizeOnResizableComponent_setsMaximumSizeOnNodeReformOptions() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();
        assertThat(entity).isNotNull();
        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor, mXrExtensions, kMinDimensions, kMaxDimensions);
        assertThat(resizableComponent).isNotNull();

        assertThat(entity.addComponent(resizableComponent)).isTrue();
        FakeNode node = new FakeNode(entity.getNode());

        resizableComponent.setMaximumSize(kMinDimensions);
        assertThat(node.getReformOptions().getMaximumSize().x).isEqualTo(kMinDimensions.width);
        assertThat(node.getReformOptions().getMaximumSize().y).isEqualTo(kMinDimensions.height);
        assertThat(node.getReformOptions().getMaximumSize().z).isEqualTo(kMinDimensions.depth);
    }

    @Test
    public void setFixedAspectRatioOnResizableComponent_setsFixedAspectRatioOnNodeReformOptions() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();
        assertThat(entity).isNotNull();
        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor, mXrExtensions, kMinDimensions, kMaxDimensions);
        assertThat(resizableComponent).isNotNull();

        assertThat(entity.addComponent(resizableComponent)).isTrue();
        FakeNode node = new FakeNode(entity.getNode());

        resizableComponent.setFixedAspectRatio(2.0f);
        assertThat(node.getReformOptions().getFixedAspectRatio()).isEqualTo(2.0f);
        resizableComponent.setFixedAspectRatio(0.0f);
        assertThat(node.getReformOptions().getFixedAspectRatio()).isEqualTo(0.0f);
        resizableComponent.setFixedAspectRatio(-1.0f);
        assertThat(node.getReformOptions().getFixedAspectRatio()).isEqualTo(-1.0f);
    }

    @Test
    public void
            setForceShowResizeOverlayOnResizableComponent_setsForceShowResizeOverlayOnNodeReformOptions() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();
        assertThat(entity).isNotNull();
        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor, mXrExtensions, kMinDimensions, kMaxDimensions);
        assertThat(resizableComponent).isNotNull();

        assertThat(entity.addComponent(resizableComponent)).isTrue();
        FakeNode node = new FakeNode(entity.getNode());

        resizableComponent.setForceShowResizeOverlay(true);
        assertThat(node.getReformOptions().getForceShowResizeOverlay()).isTrue();
    }

    @Test
    public void addResizableComponentLater_addsReformOptionsToNode() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();
        assertThat(entity).isNotNull();
        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor, mXrExtensions, kMinDimensions, kMaxDimensions);
        assertThat(resizableComponent).isNotNull();
        Dimensions testSize = new Dimensions(1f, 1f, 1f);
        Dimensions testMinSize = new Dimensions(0.25f, 0.25f, 0.25f);
        Dimensions testMaxSize = new Dimensions(5f, 5f, 5f);
        resizableComponent.setSize(testSize);
        resizableComponent.setMinimumSize(testMinSize);
        resizableComponent.setMaximumSize(testMaxSize);

        assertThat(entity.addComponent(resizableComponent)).isTrue();

        FakeNode node = new FakeNode(entity.getNode());
        assertThat(node.getReformOptions().getEnabledReform())
                .isEqualTo(ReformOptions.ALLOW_RESIZE);
        assertThat(node.getReformOptions().getCurrentSize().x).isEqualTo(testSize.width);
        assertThat(node.getReformOptions().getCurrentSize().y).isEqualTo(testSize.height);
        assertThat(node.getReformOptions().getCurrentSize().z).isEqualTo(testSize.depth);
        assertThat(node.getReformOptions().getMinimumSize().x).isEqualTo(testMinSize.width);
        assertThat(node.getReformOptions().getMinimumSize().y).isEqualTo(testMinSize.height);
        assertThat(node.getReformOptions().getMinimumSize().z).isEqualTo(testMinSize.depth);
        assertThat(node.getReformOptions().getMaximumSize().x).isEqualTo(testMaxSize.width);
        assertThat(node.getReformOptions().getMaximumSize().y).isEqualTo(testMaxSize.height);
        assertThat(node.getReformOptions().getMaximumSize().z).isEqualTo(testMaxSize.depth);
    }

    @Test
    public void addResizeEventListener_onlyInvokedOnResizeEvent() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();
        assertThat(entity).isNotNull();
        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor, mXrExtensions, kMinDimensions, kMaxDimensions);
        assertThat(resizableComponent).isNotNull();
        assertThat(entity.addComponent(resizableComponent)).isTrue();
        FakeNode node = new FakeNode(entity.getNode());
        ResizeEventListener mockResizeEventListener = mock(ResizeEventListener.class);

        resizableComponent.addResizeEventListener(directExecutor(), mockResizeEventListener);
        assertThat(node.getReformOptions().getEventCallback()).isNotNull();
        assertThat(node.getReformOptions().getEventExecutor()).isNotNull();
        assertThat(entity.mReformEventConsumerMap).isNotEmpty();

        com.android.extensions.xr.node.ReformEvent realReformEvent =
                ShadowReformEvent.create(
                        /* type= */ ReformEvent.REFORM_TYPE_MOVE, /* state= */ 0, /* id= */ 0);

        final ReformEvent moveReformEvent = NodeTypeConverter.toLibrary(realReformEvent);
        node.getReformOptions()
                .getEventExecutor()
                .execute(() -> node.getReformOptions().getEventCallback().accept(moveReformEvent));
        assertThat(mFakeExecutor.hasNext()).isTrue();
        mFakeExecutor.runAll();
        verify(mockResizeEventListener, never()).onResizeEvent(any());

        realReformEvent =
                ShadowReformEvent.create(
                        /* type= */ ReformEvent.REFORM_TYPE_RESIZE, /* state= */ 0, /* id= */ 0);
        final ReformEvent resizeReformEvent = NodeTypeConverter.toLibrary(realReformEvent);
        node.getReformOptions()
                .getEventExecutor()
                .execute(
                        () -> node.getReformOptions().getEventCallback().accept(resizeReformEvent));
        assertThat(mFakeExecutor.hasNext()).isTrue();
        mFakeExecutor.runAll();
        verify(mockResizeEventListener).onResizeEvent(any());
    }

    @Test
    public void addResizeEventListenerWithExecutor_invokesListenerOnGivenExecutor() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();
        assertThat(entity).isNotNull();
        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor, mXrExtensions, kMinDimensions, kMaxDimensions);
        assertThat(resizableComponent).isNotNull();
        assertThat(entity.addComponent(resizableComponent)).isTrue();
        FakeNode node = new FakeNode(entity.getNode());
        ResizeEventListener mockResizeEventListener = mock(ResizeEventListener.class);
        FakeScheduledExecutorService executorService = new FakeScheduledExecutorService();

        resizableComponent.addResizeEventListener(executorService, mockResizeEventListener);
        assertThat(node.getReformOptions().getEventCallback()).isNotNull();
        assertThat(node.getReformOptions().getEventExecutor()).isNotNull();

        com.android.extensions.xr.node.ReformEvent realReformEvent =
                ShadowReformEvent.create(
                        /* type= */ ReformEvent.REFORM_TYPE_RESIZE, /* state= */ 0, /* id= */ 0);

        final ReformEvent resizeReformEvent = NodeTypeConverter.toLibrary(realReformEvent);
        node.getReformOptions()
                .getEventExecutor()
                .execute(
                        () -> node.getReformOptions().getEventCallback().accept(resizeReformEvent));
        assertThat(mFakeExecutor.hasNext()).isTrue();
        mFakeExecutor.runAll();
        assertThat(executorService.hasNext()).isTrue();
        executorService.runAll();
        verify(mockResizeEventListener).onResizeEvent(any());
    }

    @Test
    public void addResizeEventListenerMultiple_invokesAllListeners() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();
        assertThat(entity).isNotNull();
        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor, mXrExtensions, kMinDimensions, kMaxDimensions);
        assertThat(resizableComponent).isNotNull();
        assertThat(entity.addComponent(resizableComponent)).isTrue();
        FakeNode node = new FakeNode(entity.getNode());
        ResizeEventListener mockResizeEventListener1 = mock(ResizeEventListener.class);
        ResizeEventListener mockResizeEventListener2 = mock(ResizeEventListener.class);
        FakeScheduledExecutorService executorService = new FakeScheduledExecutorService();

        resizableComponent.addResizeEventListener(executorService, mockResizeEventListener1);
        resizableComponent.addResizeEventListener(executorService, mockResizeEventListener2);
        assertThat(node.getReformOptions().getEventCallback()).isNotNull();
        assertThat(node.getReformOptions().getEventExecutor()).isNotNull();

        com.android.extensions.xr.node.ReformEvent realReformEvent =
                ShadowReformEvent.create(
                        /* type= */ ReformEvent.REFORM_TYPE_RESIZE, /* state= */ 0, /* id= */ 0);

        final ReformEvent resizeReformEvent = NodeTypeConverter.toLibrary(realReformEvent);
        node.getReformOptions()
                .getEventExecutor()
                .execute(
                        () -> node.getReformOptions().getEventCallback().accept(resizeReformEvent));
        assertThat(mFakeExecutor.hasNext()).isTrue();
        mFakeExecutor.runAll();
        assertThat(executorService.hasNext()).isTrue();
        executorService.runAll();
        verify(mockResizeEventListener1).onResizeEvent(any());
        verify(mockResizeEventListener2).onResizeEvent(any());
    }

    @Test
    public void removeResizeEventListenerMultiple_removesGivenListener() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();
        assertThat(entity).isNotNull();
        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor, mXrExtensions, kMinDimensions, kMaxDimensions);
        assertThat(resizableComponent).isNotNull();
        assertThat(entity.addComponent(resizableComponent)).isTrue();
        FakeNode node = new FakeNode(entity.getNode());
        ResizeEventListener mockResizeEventListener1 = mock(ResizeEventListener.class);
        ResizeEventListener mockResizeEventListener2 = mock(ResizeEventListener.class);
        FakeScheduledExecutorService executorService = new FakeScheduledExecutorService();

        resizableComponent.addResizeEventListener(executorService, mockResizeEventListener1);
        resizableComponent.addResizeEventListener(executorService, mockResizeEventListener2);
        assertThat(node.getReformOptions().getEventCallback()).isNotNull();
        assertThat(node.getReformOptions().getEventExecutor()).isNotNull();

        com.android.extensions.xr.node.ReformEvent realReformEvent =
                ShadowReformEvent.create(
                        /* type= */ ReformEvent.REFORM_TYPE_RESIZE, /* state= */ 0, /* id= */ 0);

        final ReformEvent resizeReformEvent = NodeTypeConverter.toLibrary(realReformEvent);
        node.getReformOptions()
                .getEventExecutor()
                .execute(
                        () -> node.getReformOptions().getEventCallback().accept(resizeReformEvent));
        assertThat(mFakeExecutor.hasNext()).isTrue();
        mFakeExecutor.runAll();
        assertThat(executorService.hasNext()).isTrue();
        executorService.runAll();

        resizableComponent.removeResizeEventListener(mockResizeEventListener1);
        node.getReformOptions()
                .getEventExecutor()
                .execute(
                        () -> node.getReformOptions().getEventCallback().accept(resizeReformEvent));
        assertThat(mFakeExecutor.hasNext()).isTrue();
        mFakeExecutor.runAll();
        assertThat(executorService.hasNext()).isTrue();
        executorService.runAll();
        verify(mockResizeEventListener1).onResizeEvent(any());
        verify(mockResizeEventListener2, times(2)).onResizeEvent(any());
    }

    @Test
    public void removeAllResizeEventListeners_removesReformEventConsumer() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();
        assertThat(entity).isNotNull();
        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor, mXrExtensions, kMinDimensions, kMaxDimensions);
        assertThat(resizableComponent).isNotNull();
        assertThat(entity.addComponent(resizableComponent)).isTrue();
        FakeNode node = new FakeNode(entity.getNode());
        ResizeEventListener mockResizeEventListener1 = mock(ResizeEventListener.class);
        ResizeEventListener mockResizeEventListener2 = mock(ResizeEventListener.class);
        FakeScheduledExecutorService executorService = new FakeScheduledExecutorService();

        resizableComponent.addResizeEventListener(executorService, mockResizeEventListener1);
        resizableComponent.addResizeEventListener(executorService, mockResizeEventListener2);
        assertThat(node.getReformOptions().getEventCallback()).isNotNull();
        assertThat(node.getReformOptions().getEventExecutor()).isNotNull();

        com.android.extensions.xr.node.ReformEvent realReformEvent =
                ShadowReformEvent.create(
                        /* type= */ ReformEvent.REFORM_TYPE_RESIZE, /* state= */ 0, /* id= */ 0);

        final ReformEvent resizeReformEvent = NodeTypeConverter.toLibrary(realReformEvent);
        node.getReformOptions()
                .getEventExecutor()
                .execute(
                        () -> node.getReformOptions().getEventCallback().accept(resizeReformEvent));
        assertThat(mFakeExecutor.hasNext()).isTrue();
        mFakeExecutor.runAll();
        assertThat(executorService.hasNext()).isTrue();
        executorService.runAll();
        verify(mockResizeEventListener1).onResizeEvent(any());
        verify(mockResizeEventListener2).onResizeEvent(any());

        resizableComponent.removeResizeEventListener(mockResizeEventListener1);
        resizableComponent.removeResizeEventListener(mockResizeEventListener2);

        assertThat(entity.mReformEventConsumerMap).isEmpty();
    }

    @Test
    public void removeResizableComponent_clearsResizeReformOptionsAndResizeEventListeners() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();
        assertThat(entity).isNotNull();
        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor, mXrExtensions, kMinDimensions, kMaxDimensions);
        assertThat(resizableComponent).isNotNull();
        assertThat(entity.addComponent(resizableComponent)).isTrue();
        FakeNode node = new FakeNode(entity.getNode());
        ResizeEventListener mockResizeEventListener = mock(ResizeEventListener.class);

        resizableComponent.addResizeEventListener(directExecutor(), mockResizeEventListener);
        assertThat(resizableComponent.mReformEventConsumer).isNotNull();

        entity.removeComponent(resizableComponent);
        assertThat(node.getReformOptions()).isNull();
        assertThat(entity.mReformEventConsumerMap).isEmpty();
    }

    @Test
    public void addMoveAndResizeComponents_setsCombinedReformsOptions() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();
        assertThat(entity).isNotNull();
        MovableComponentImpl movableComponent =
                new MovableComponentImpl(
                        true,
                        true,
                        ImmutableSet.of(),
                        /* shouldDisposeParentAnchor= */ true,
                        mPerceptionLibrary,
                        mXrExtensions,
                        mActivitySpaceImpl,
                        mActivitySpaceRoot,
                        mPerceptionSpaceActivityPose,
                        mEntityManager,
                        mPanelShadowRenderer,
                        mFakeExecutor);
        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor,
                        mXrExtensions,
                        new Dimensions(0f, 0f, 0f),
                        new Dimensions(5f, 5f, 5f));
        assertThat(entity.addComponent(movableComponent)).isTrue();
        assertThat(entity.addComponent(resizableComponent)).isTrue();
        FakeNode node = new FakeNode(entity.getNode());
        assertThat(node.getReformOptions().getEnabledReform())
                .isEqualTo(ReformOptions.ALLOW_MOVE | ReformOptions.ALLOW_RESIZE);
    }

    @Test
    public void addMoveAndResizeComponents_removingMoveKeepsResize() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();
        assertThat(entity).isNotNull();
        MovableComponentImpl movableComponent =
                new MovableComponentImpl(
                        true,
                        true,
                        ImmutableSet.of(),
                        /* shouldDisposeParentAnchor= */ true,
                        mPerceptionLibrary,
                        mXrExtensions,
                        mActivitySpaceImpl,
                        mActivitySpaceRoot,
                        mPerceptionSpaceActivityPose,
                        mEntityManager,
                        mPanelShadowRenderer,
                        mFakeExecutor);
        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor,
                        mXrExtensions,
                        new Dimensions(0f, 0f, 0f),
                        new Dimensions(5f, 5f, 5f));
        assertThat(entity.addComponent(movableComponent)).isTrue();
        assertThat(entity.addComponent(resizableComponent)).isTrue();
        MoveEventListener moveEventListener = mock(MoveEventListener.class);
        movableComponent.addMoveEventListener(directExecutor(), moveEventListener);
        ResizeEventListener resizeEventListener = mock(ResizeEventListener.class);
        resizableComponent.addResizeEventListener(directExecutor(), resizeEventListener);
        FakeNode node = new FakeNode(entity.getNode());
        assertThat(node.getReformOptions().getEnabledReform())
                .isEqualTo(ReformOptions.ALLOW_MOVE | ReformOptions.ALLOW_RESIZE);

        entity.removeComponent(movableComponent);
        assertThat(node.getReformOptions().getEnabledReform())
                .isEqualTo(ReformOptions.ALLOW_RESIZE);

        com.android.extensions.xr.node.ReformEvent realReformEvent =
                ShadowReformEvent.create(
                        /* type= */ ReformEvent.REFORM_TYPE_RESIZE, /* state= */ 0, /* id= */ 0);

        final ReformEvent resizeReformEvent = NodeTypeConverter.toLibrary(realReformEvent);
        node.getReformOptions()
                .getEventExecutor()
                .execute(
                        () -> node.getReformOptions().getEventCallback().accept(resizeReformEvent));
        assertThat(mFakeExecutor.hasNext()).isTrue();
        mFakeExecutor.runAll();
        verify(resizeEventListener).onResizeEvent(any());

        realReformEvent =
                ShadowReformEvent.create(
                        /* type= */ ReformEvent.REFORM_TYPE_MOVE, /* state= */ 0, /* id= */ 0);

        final ReformEvent moveReformEvent = NodeTypeConverter.toLibrary(realReformEvent);
        node.getReformOptions()
                .getEventExecutor()
                .execute(() -> node.getReformOptions().getEventCallback().accept(moveReformEvent));
        assertThat(mFakeExecutor.hasNext()).isTrue();
        mFakeExecutor.runAll();
        verify(moveEventListener, never()).onMoveEvent(any());
    }

    @Test
    public void addMoveAndResizeComponents_removingResizeKeepsMove() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();
        assertThat(entity).isNotNull();
        MovableComponentImpl movableComponent =
                new MovableComponentImpl(
                        true,
                        true,
                        ImmutableSet.of(),
                        /* shouldDisposeParentAnchor= */ true,
                        mPerceptionLibrary,
                        mXrExtensions,
                        mActivitySpaceImpl,
                        mActivitySpaceRoot,
                        mPerceptionSpaceActivityPose,
                        mEntityManager,
                        mPanelShadowRenderer,
                        mFakeExecutor);
        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor,
                        mXrExtensions,
                        new Dimensions(0f, 0f, 0f),
                        new Dimensions(5f, 5f, 5f));
        assertThat(entity.addComponent(movableComponent)).isTrue();
        assertThat(entity.addComponent(resizableComponent)).isTrue();
        MoveEventListener moveEventListener = mock(MoveEventListener.class);
        movableComponent.addMoveEventListener(directExecutor(), moveEventListener);
        ResizeEventListener resizeEventListener = mock(ResizeEventListener.class);
        resizableComponent.addResizeEventListener(directExecutor(), resizeEventListener);
        FakeNode node = new FakeNode(entity.getNode());
        assertThat(node.getReformOptions().getEnabledReform())
                .isEqualTo(ReformOptions.ALLOW_MOVE | ReformOptions.ALLOW_RESIZE);

        entity.removeComponent(resizableComponent);
        assertThat(node.getReformOptions().getEnabledReform()).isEqualTo(ReformOptions.ALLOW_MOVE);

        com.android.extensions.xr.node.ReformEvent realReformEvent =
                ShadowReformEvent.create(
                        /* type= */ ReformEvent.REFORM_TYPE_MOVE, /* state= */ 0, /* id= */ 0);

        final ReformEvent moveReformEvent = NodeTypeConverter.toLibrary(realReformEvent);
        node.getReformOptions()
                .getEventExecutor()
                .execute(() -> node.getReformOptions().getEventCallback().accept(moveReformEvent));
        assertThat(mFakeExecutor.hasNext()).isTrue();
        mFakeExecutor.runAll();
        verify(moveEventListener).onMoveEvent(any());

        realReformEvent =
                ShadowReformEvent.create(
                        /* type= */ ReformEvent.REFORM_TYPE_RESIZE, /* state= */ 0, /* id= */ 0);

        final ReformEvent resizeReformEvent = NodeTypeConverter.toLibrary(realReformEvent);
        node.getReformOptions()
                .getEventExecutor()
                .execute(
                        () -> node.getReformOptions().getEventCallback().accept(resizeReformEvent));
        assertThat(mFakeExecutor.hasNext()).isTrue();
        mFakeExecutor.runAll();
        verify(resizeEventListener, never()).onResizeEvent(any());
    }

    @Test
    public void resizableComponent_canAttachAgainAfterDetach() {
        Entity entity = createTestEntity();
        assertThat(entity).isNotNull();
        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor, mXrExtensions, kMinDimensions, kMaxDimensions);
        assertThat(resizableComponent).isNotNull();
        assertThat(entity.addComponent(resizableComponent)).isTrue();
        entity.removeComponent(resizableComponent);
        assertThat(entity.addComponent(resizableComponent)).isTrue();
    }

    @Test
    public void resizableComponent_hidesEntityDuringResize() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();
        assertThat(entity).isNotNull();
        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor, mXrExtensions, kMinDimensions, kMaxDimensions);
        assertThat(resizableComponent).isNotNull();
        assertThat(entity.addComponent(resizableComponent)).isTrue();
        entity.setAlpha(0.9f);
        assertThat(entity.getAlpha()).isEqualTo(0.9f);
        FakeNode node = new FakeNode(entity.getNode());
        ResizeEventListener mockResizeEventListener = mock(ResizeEventListener.class);

        resizableComponent.addResizeEventListener(directExecutor(), mockResizeEventListener);
        assertThat(node.getReformOptions().getEventCallback()).isNotNull();
        assertThat(node.getReformOptions().getEventExecutor()).isNotNull();
        assertThat(entity.mReformEventConsumerMap).isNotEmpty();

        // Start the resize.
        com.android.extensions.xr.node.ReformEvent realReformEvent =
                ShadowReformEvent.create(
                        /* type= */ ReformEvent.REFORM_TYPE_RESIZE,
                        /* state= */ ReformEvent.REFORM_STATE_START,
                        /* id= */ 0);

        final ReformEvent startReformEvent = NodeTypeConverter.toLibrary(realReformEvent);
        node.getReformOptions()
                .getEventExecutor()
                .execute(() -> node.getReformOptions().getEventCallback().accept(startReformEvent));
        assertThat(mFakeExecutor.hasNext()).isTrue();
        mFakeExecutor.runAll();
        ArgumentCaptor<ResizeEvent> resizeEventCaptor = ArgumentCaptor.forClass(ResizeEvent.class);
        verify(mockResizeEventListener).onResizeEvent(resizeEventCaptor.capture());
        ResizeEvent resizeEvent = resizeEventCaptor.getValue();
        assertThat(resizeEvent.resizeState).isEqualTo(ResizeEvent.RESIZE_STATE_START);
        assertThat(node.getAlpha()).isEqualTo(0.0f);

        // End the resize.
        realReformEvent =
                ShadowReformEvent.create(
                        /* type= */ ReformEvent.REFORM_TYPE_RESIZE,
                        /* state= */ ReformEvent.REFORM_STATE_END,
                        /* id= */ 0);

        final ReformEvent endReformEvent = NodeTypeConverter.toLibrary(realReformEvent);
        node.getReformOptions()
                .getEventExecutor()
                .execute(() -> node.getReformOptions().getEventCallback().accept(endReformEvent));
        assertThat(mFakeExecutor.hasNext()).isTrue();
        mFakeExecutor.runAll();
        verify(mockResizeEventListener, times(2)).onResizeEvent(resizeEventCaptor.capture());
        resizeEvent = resizeEventCaptor.getAllValues().get(2);
        assertThat(resizeEvent.resizeState).isEqualTo(ResizeEvent.RESIZE_STATE_END);
        assertThat(node.getAlpha()).isEqualTo(0.9f);
    }

    @Test
    public void resizableComponent_withAutoHideContentDisabled_doesNotHideEntityDuringResize() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();
        assertThat(entity).isNotNull();
        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor, mXrExtensions, kMinDimensions, kMaxDimensions);
        assertThat(resizableComponent).isNotNull();
        assertThat(entity.addComponent(resizableComponent)).isTrue();
        entity.setAlpha(0.9f);
        assertThat(entity.getAlpha()).isEqualTo(0.9f);
        FakeNode node = new FakeNode(entity.getNode());
        ResizeEventListener mockResizeEventListener = mock(ResizeEventListener.class);

        resizableComponent.setAutoHideContent(false);
        resizableComponent.addResizeEventListener(directExecutor(), mockResizeEventListener);
        assertThat(node.getReformOptions().getEventCallback()).isNotNull();
        assertThat(node.getReformOptions().getEventExecutor()).isNotNull();
        assertThat(entity.mReformEventConsumerMap).isNotEmpty();

        // Start the resize.
        com.android.extensions.xr.node.ReformEvent realReformEvent =
                ShadowReformEvent.create(
                        /* type= */ ReformEvent.REFORM_TYPE_RESIZE,
                        /* state= */ ReformEvent.REFORM_STATE_START,
                        /* id= */ 0);

        final ReformEvent startReformEvent = NodeTypeConverter.toLibrary(realReformEvent);
        node.getReformOptions()
                .getEventExecutor()
                .execute(() -> node.getReformOptions().getEventCallback().accept(startReformEvent));
        assertThat(mFakeExecutor.hasNext()).isTrue();
        mFakeExecutor.runAll();
        ArgumentCaptor<ResizeEvent> resizeEventCaptor = ArgumentCaptor.forClass(ResizeEvent.class);
        verify(mockResizeEventListener).onResizeEvent(resizeEventCaptor.capture());
        ResizeEvent resizeEvent = resizeEventCaptor.getValue();
        assertThat(resizeEvent.resizeState).isEqualTo(ResizeEvent.RESIZE_STATE_START);
        assertThat(node.getAlpha()).isEqualTo(0.9f);

        // End the resize.
        realReformEvent =
                ShadowReformEvent.create(
                        /* type= */ ReformEvent.REFORM_TYPE_RESIZE,
                        /* state= */ ReformEvent.REFORM_STATE_END,
                        /* id= */ 0);

        final ReformEvent endReformEvent = NodeTypeConverter.toLibrary(realReformEvent);
        node.getReformOptions()
                .getEventExecutor()
                .execute(() -> node.getReformOptions().getEventCallback().accept(endReformEvent));
        assertThat(mFakeExecutor.hasNext()).isTrue();
        mFakeExecutor.runAll();
        verify(mockResizeEventListener, times(2)).onResizeEvent(resizeEventCaptor.capture());
        resizeEvent = resizeEventCaptor.getAllValues().get(2);
        assertThat(resizeEvent.resizeState).isEqualTo(ResizeEvent.RESIZE_STATE_END);
        assertThat(node.getAlpha()).isEqualTo(0.9f);
    }

    @Test
    public void resizableComponent_updatesComponentSizeAfterResize() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();
        assertThat(entity).isNotNull();
        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor, mXrExtensions, kMinDimensions, kMaxDimensions);
        assertThat(resizableComponent).isNotNull();
        assertThat(entity.addComponent(resizableComponent)).isTrue();
        FakeNode node = new FakeNode(entity.getNode());
        resizableComponent.setSize(new Dimensions(1.0f, 2.0f, 3.0f));
        assertThat(node.getReformOptions().getCurrentSize().x).isEqualTo(1.0f);
        assertThat(node.getReformOptions().getCurrentSize().y).isEqualTo(2.0f);
        assertThat(node.getReformOptions().getCurrentSize().z).isEqualTo(3.0f);
        ResizeEventListener mockResizeEventListener = mock(ResizeEventListener.class);

        resizableComponent.addResizeEventListener(directExecutor(), mockResizeEventListener);
        assertThat(node.getReformOptions().getEventCallback()).isNotNull();
        assertThat(node.getReformOptions().getEventExecutor()).isNotNull();
        assertThat(entity.mReformEventConsumerMap).isNotEmpty();

        // Start the resize.
        com.android.extensions.xr.node.ReformEvent realReformEvent =
                ShadowReformEvent.create(
                        /* type= */ ReformEvent.REFORM_TYPE_RESIZE,
                        /* state= */ ReformEvent.REFORM_STATE_START,
                        /* id= */ 0);

        final ReformEvent startReformEvent = NodeTypeConverter.toLibrary(realReformEvent);
        node.getReformOptions()
                .getEventExecutor()
                .execute(() -> node.getReformOptions().getEventCallback().accept(startReformEvent));
        assertThat(mFakeExecutor.hasNext()).isTrue();
        mFakeExecutor.runAll();
        ArgumentCaptor<ResizeEvent> resizeEventCaptor = ArgumentCaptor.forClass(ResizeEvent.class);
        verify(mockResizeEventListener).onResizeEvent(resizeEventCaptor.capture());
        ResizeEvent resizeEvent = resizeEventCaptor.getValue();
        assertThat(resizeEvent.resizeState).isEqualTo(ResizeEvent.RESIZE_STATE_START);

        // End the resize.
        realReformEvent =
                ShadowReformEvent.create(
                        /* type= */ ReformEvent.REFORM_TYPE_RESIZE,
                        /* state= */ ReformEvent.REFORM_STATE_END,
                        /* id= */ 0);
        ShadowReformEvent.extract(realReformEvent).setProposedSize(new Vec3(4.0f, 5.0f, 6.0f));

        final ReformEvent endReformEvent = NodeTypeConverter.toLibrary(realReformEvent);
        node.getReformOptions()
                .getEventExecutor()
                .execute(() -> node.getReformOptions().getEventCallback().accept(endReformEvent));
        assertThat(mFakeExecutor.hasNext()).isTrue();
        mFakeExecutor.runAll();
        verify(mockResizeEventListener, times(2)).onResizeEvent(resizeEventCaptor.capture());
        resizeEvent = resizeEventCaptor.getAllValues().get(2);
        assertThat(resizeEvent.resizeState).isEqualTo(ResizeEvent.RESIZE_STATE_END);
        assertThat(node.getReformOptions().getCurrentSize().x).isEqualTo(4.0f);
        assertThat(node.getReformOptions().getCurrentSize().y).isEqualTo(5.0f);
        assertThat(node.getReformOptions().getCurrentSize().z).isEqualTo(6.0f);
    }

    @Test
    public void
            resizableComponent_withAutoUpdateSizeDisabled_doesNotUpdateComponentSizeAfterResize() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();
        assertThat(entity).isNotNull();
        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor, mXrExtensions, kMinDimensions, kMaxDimensions);
        assertThat(resizableComponent).isNotNull();
        assertThat(entity.addComponent(resizableComponent)).isTrue();
        FakeNode node = new FakeNode(entity.getNode());
        resizableComponent.setAutoUpdateSize(false);
        resizableComponent.setSize(new Dimensions(1.0f, 2.0f, 3.0f));
        assertThat(node.getReformOptions().getCurrentSize().x).isEqualTo(1.0f);
        assertThat(node.getReformOptions().getCurrentSize().y).isEqualTo(2.0f);
        assertThat(node.getReformOptions().getCurrentSize().z).isEqualTo(3.0f);
        ResizeEventListener mockResizeEventListener = mock(ResizeEventListener.class);

        resizableComponent.addResizeEventListener(directExecutor(), mockResizeEventListener);
        assertThat(node.getReformOptions().getEventCallback()).isNotNull();
        assertThat(node.getReformOptions().getEventExecutor()).isNotNull();
        assertThat(entity.mReformEventConsumerMap).isNotEmpty();

        // Start the resize.
        com.android.extensions.xr.node.ReformEvent realReformEvent =
                ShadowReformEvent.create(
                        /* type= */ ReformEvent.REFORM_TYPE_RESIZE,
                        /* state= */ ReformEvent.REFORM_STATE_START,
                        /* id= */ 0);

        final ReformEvent startReformEvent = NodeTypeConverter.toLibrary(realReformEvent);
        node.getReformOptions()
                .getEventExecutor()
                .execute(() -> node.getReformOptions().getEventCallback().accept(startReformEvent));
        assertThat(mFakeExecutor.hasNext()).isTrue();
        mFakeExecutor.runAll();
        ArgumentCaptor<ResizeEvent> resizeEventCaptor = ArgumentCaptor.forClass(ResizeEvent.class);
        verify(mockResizeEventListener).onResizeEvent(resizeEventCaptor.capture());
        ResizeEvent resizeEvent = resizeEventCaptor.getValue();
        assertThat(resizeEvent.resizeState).isEqualTo(ResizeEvent.RESIZE_STATE_START);

        // End the resize.
        realReformEvent =
                ShadowReformEvent.create(
                        /* type= */ ReformEvent.REFORM_TYPE_RESIZE,
                        /* state= */ ReformEvent.REFORM_STATE_END,
                        /* id= */ 0);
        ShadowReformEvent.extract(realReformEvent).setProposedSize(new Vec3(4.0f, 5.0f, 6.0f));

        final ReformEvent endReformEvent = NodeTypeConverter.toLibrary(realReformEvent);
        node.getReformOptions()
                .getEventExecutor()
                .execute(() -> node.getReformOptions().getEventCallback().accept(endReformEvent));
        assertThat(mFakeExecutor.hasNext()).isTrue();
        mFakeExecutor.runAll();
        verify(mockResizeEventListener, times(2)).onResizeEvent(resizeEventCaptor.capture());
        resizeEvent = resizeEventCaptor.getAllValues().get(2);
        assertThat(resizeEvent.resizeState).isEqualTo(ResizeEvent.RESIZE_STATE_END);
        // Reform size should be unchanged.
        assertThat(node.getReformOptions().getCurrentSize().x).isEqualTo(1.0f);
        assertThat(node.getReformOptions().getCurrentSize().y).isEqualTo(2.0f);
        assertThat(node.getReformOptions().getCurrentSize().z).isEqualTo(3.0f);
    }
}
