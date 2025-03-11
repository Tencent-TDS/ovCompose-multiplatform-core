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

import static androidx.xr.runtime.testing.math.MathAssertions.assertPose;
import static androidx.xr.runtime.testing.math.MathAssertions.assertVector3;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.util.concurrent.Futures.immediateFuture;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;

import androidx.xr.extensions.space.Bounds;
import androidx.xr.runtime.math.Matrix4;
import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Vector3;
import androidx.xr.scenecore.JxrPlatformAdapter;
import androidx.xr.scenecore.JxrPlatformAdapter.ActivitySpace;
import androidx.xr.scenecore.JxrPlatformAdapter.Dimensions;
import androidx.xr.scenecore.impl.perception.PerceptionLibrary;
import androidx.xr.scenecore.impl.perception.Session;
import androidx.xr.scenecore.testing.FakeImpressApi;
import androidx.xr.scenecore.testing.FakeScheduledExecutorService;
import androidx.xr.scenecore.testing.FakeXrExtensions;
import androidx.xr.scenecore.testing.FakeXrExtensions.FakeSpatialState;

import com.google.androidxr.splitengine.SplitEngineSubspaceManager;
import com.google.ar.imp.view.splitengine.ImpSplitEngineRenderer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

@RunWith(RobolectricTestRunner.class)
public final class ActivitySpaceImplTest extends SystemSpaceEntityImplTest {
    // TODO(b/329902726): Move this boilerplate for creating a TestJxrPlatformAdapter into a test
    // util
    private final ActivityController<Activity> mActivityController =
            Robolectric.buildActivity(Activity.class);
    private final Activity mActivity = mActivityController.create().start().get();
    private final FakeScheduledExecutorService mFakeExecutor = new FakeScheduledExecutorService();
    private final PerceptionLibrary mPerceptionLibrary = Mockito.mock(PerceptionLibrary.class);
    private final SplitEngineSubspaceManager mSplitEngineSubspaceManager =
            Mockito.mock(SplitEngineSubspaceManager.class);
    private final ImpSplitEngineRenderer mSplitEngineRenderer =
            Mockito.mock(ImpSplitEngineRenderer.class);

    private FakeXrExtensions mFakeExtensions;
    private FakeImpressApi mFakeImpressApi;
    private JxrPlatformAdapter mTestRuntime;
    private ActivitySpace mActivitySpace;

    @Before
    public void setUp() {
        mFakeExtensions = new FakeXrExtensions();
        mFakeImpressApi = new FakeImpressApi();
        when(mPerceptionLibrary.initSession(eq(mActivity), anyInt(), eq(mFakeExecutor)))
                .thenReturn(immediateFuture(Mockito.mock(Session.class)));

        mTestRuntime =
                JxrPlatformAdapterAxr.create(
                        mActivity,
                        mFakeExecutor,
                        mFakeExtensions,
                        mFakeImpressApi,
                        new EntityManager(),
                        mPerceptionLibrary,
                        mSplitEngineSubspaceManager,
                        mSplitEngineRenderer,
                        /* useSplitEngine= */ false);

        mActivitySpace = mTestRuntime.getActivitySpace();

        // This is slightly hacky. We're grabbing the singleton instance of the ActivitySpaceImpl
        // that
        // was created by the RuntimeImpl. Ideally we'd have an interface to inject the
        // ActivitySpace
        // for testing.  For now this is fine since there isn't an interface difference (yet).
        assertThat(mActivitySpace).isInstanceOf(ActivitySpaceImpl.class);
        assertThat(mActivitySpace).isNotNull();
    }

    @After
    public void tearDown() {
        // Dispose the runtime between test cases to clean up lingering references.
        mTestRuntime.dispose();
    }

    @Override
    protected SystemSpaceEntityImpl getSystemSpaceEntityImpl() {
        return (SystemSpaceEntityImpl) mActivitySpace;
    }

    @Override
    protected FakeScheduledExecutorService getDefaultFakeExecutor() {
        return mFakeExecutor;
    }

    @Override
    protected AndroidXrEntity createChildAndroidXrEntity() {
        return (AndroidXrEntity) mTestRuntime.createEntity(new Pose(), "child", mActivitySpace);
    }

    @Override
    protected ActivitySpaceImpl getActivitySpaceEntity() {
        return (ActivitySpaceImpl) mActivitySpace;
    }

    @Test
    public void getBounds_returnsBounds() {
        assertThat(mActivitySpace.getBounds().width).isPositiveInfinity();
        assertThat(mActivitySpace.getBounds().height).isPositiveInfinity();
        assertThat(mActivitySpace.getBounds().depth).isPositiveInfinity();

        FakeSpatialState spatialState = new FakeSpatialState();
        spatialState.setBounds(new Bounds(100.0f, 200.0f, 300.0f));
        mFakeExtensions.sendSpatialState(spatialState);

        assertThat(mActivitySpace.getBounds().width).isEqualTo(100f);
        assertThat(mActivitySpace.getBounds().height).isEqualTo(200f);
        assertThat(mActivitySpace.getBounds().depth).isEqualTo(300f);
    }

    @Test
    public void addBoundsChangedListener_happyPath() {
        JxrPlatformAdapter.ActivitySpace.OnBoundsChangedListener listener =
                Mockito.mock(JxrPlatformAdapter.ActivitySpace.OnBoundsChangedListener.class);

        FakeSpatialState spatialState = new FakeSpatialState();
        spatialState.setBounds(new Bounds(100.0f, 200.0f, 300.0f));
        mActivitySpace.addOnBoundsChangedListener(listener);
        mFakeExtensions.sendSpatialState(spatialState);

        verify(listener).onBoundsChanged(Mockito.refEq(new Dimensions(100.0f, 200.0f, 300.0f)));
    }

    @Test
    public void removeBoundsChangedListener_happyPath() {
        JxrPlatformAdapter.ActivitySpace.OnBoundsChangedListener listener =
                Mockito.mock(JxrPlatformAdapter.ActivitySpace.OnBoundsChangedListener.class);

        mActivitySpace.addOnBoundsChangedListener(listener);
        mActivitySpace.removeOnBoundsChangedListener(listener);
        FakeSpatialState spatialState = new FakeSpatialState();
        spatialState.setBounds(new Bounds(100.0f, 200.0f, 300.0f));
        mFakeExtensions.sendSpatialState(spatialState);

        verify(listener, Mockito.never()).onBoundsChanged(Mockito.any());
    }

    @Test
    public void getPoseInActivitySpace_returnsIdentity() {
        ActivitySpaceImpl activitySpaceImpl = (ActivitySpaceImpl) mActivitySpace;

        assertPose(activitySpaceImpl.getPoseInActivitySpace(), new Pose());
    }

    @Test
    public void getActivitySpaceScale_returnsUnitScale() {
        ActivitySpaceImpl activitySpaceImpl = (ActivitySpaceImpl) mActivitySpace;
        activitySpaceImpl.setOpenXrReferenceSpacePose(Matrix4.fromScale(5f));
        assertVector3(activitySpaceImpl.getActivitySpaceScale(), new Vector3(1f, 1f, 1f));
    }

    @Test
    public void setScale_doesNothing() throws Exception {
        Vector3 scale = new Vector3(1, 1, 9999);
        mActivitySpace.setScale(scale);

        // The returned scale(s) here should be the identity scale despite the setScale call.
        assertThat(mActivitySpace.getScale().getX()).isWithin(1e-5f).of(1.0f);
        assertThat(mActivitySpace.getScale().getY()).isWithin(1e-5f).of(1.0f);
        assertThat(mActivitySpace.getScale().getZ()).isWithin(1e-5f).of(1.0f);

        // Note that there's no exception thrown.
    }
}
