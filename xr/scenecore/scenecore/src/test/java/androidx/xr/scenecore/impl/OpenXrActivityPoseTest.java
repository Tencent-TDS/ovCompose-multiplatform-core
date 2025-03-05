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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import androidx.xr.runtime.math.Matrix4;
import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Quaternion;
import androidx.xr.runtime.math.Vector3;
import androidx.xr.scenecore.JxrPlatformAdapter.CameraViewActivityPose;
import androidx.xr.scenecore.common.BaseActivityPose;
import androidx.xr.scenecore.impl.perception.Fov;
import androidx.xr.scenecore.impl.perception.PerceptionLibrary;
import androidx.xr.scenecore.impl.perception.Session;
import androidx.xr.scenecore.impl.perception.ViewProjection;
import androidx.xr.scenecore.impl.perception.ViewProjections;
import androidx.xr.scenecore.testing.FakeScheduledExecutorService;
import androidx.xr.scenecore.testing.FakeXrExtensions;
import androidx.xr.scenecore.testing.FakeXrExtensions.FakeGltfModelToken;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.ParameterizedRobolectricTestRunner.Parameter;
import org.robolectric.ParameterizedRobolectricTestRunner.Parameters;

import java.util.Arrays;
import java.util.List;

/** Test for common behaviour for ActivityPoses whose world position is retrieved from OpenXr. */
@RunWith(ParameterizedRobolectricTestRunner.class)
public final class OpenXrActivityPoseTest {
    private final AndroidXrEntity mActivitySpaceRoot = mock(AndroidXrEntity.class);
    private final FakeXrExtensions mFakeExtensions = new FakeXrExtensions();
    private final PerceptionLibrary mPerceptionLibrary = mock(PerceptionLibrary.class);
    private final Session mSession = mock(Session.class);
    private final FakeScheduledExecutorService mExecutor = new FakeScheduledExecutorService();
    private final EntityManager mEntityManager = new EntityManager();
    private final ActivitySpaceImpl mActivitySpace =
            new ActivitySpaceImpl(
                    mFakeExtensions.createNode(),
                    mFakeExtensions,
                    mEntityManager,
                    () -> mFakeExtensions.fakeSpatialState,
                    mExecutor);

    enum OpenXrActivityPoseType {
        HEAD_ACTIVITY_POSE,
        CAMERA_ACTIVITY_POSE,
    }

    @Parameter(0)
    public OpenXrActivityPoseType testActivityPoseType;

    BaseActivityPose mTestActivityPose;

    /** Creates and return list of OpenXrActivityPoseType values. */
    @Parameters
    public static List<Object> data() throws Exception {
        return Arrays.asList(
                new Object[] {
                    OpenXrActivityPoseType.HEAD_ACTIVITY_POSE,
                    OpenXrActivityPoseType.CAMERA_ACTIVITY_POSE
                });
    }

    @Before
    public void doBeforeEachTest() {
        // By default, set the activity space to the root of the underlying OpenXR reference space.
        mActivitySpace.setOpenXrReferenceSpacePose(Matrix4.Identity);
    }

    /** Creates a HeadActivityPoseImpl instance. */
    private HeadActivityPoseImpl createHeadActivityPose(
            ActivitySpaceImpl activitySpace, AndroidXrEntity activitySpaceRoot) {
        return new HeadActivityPoseImpl(activitySpace, activitySpaceRoot, mPerceptionLibrary);
    }

    /** Creates a CameraViewActivityPoseImpl instance. */
    private CameraViewActivityPoseImpl createCameraViewActivityPose(
            ActivitySpaceImpl activitySpace, AndroidXrEntity activitySpaceRoot) {
        return new CameraViewActivityPoseImpl(
                CameraViewActivityPose.CAMERA_TYPE_LEFT_EYE,
                activitySpace,
                activitySpaceRoot,
                mPerceptionLibrary);
    }

    private BaseActivityPose createTestActivityPose() {
        return createTestActivityPose(mActivitySpace, mActivitySpaceRoot);
    }

    private BaseActivityPose createTestActivityPose(
            ActivitySpaceImpl activitySpace, AndroidXrEntity activitySpaceRoot) {
        switch (testActivityPoseType) {
            case HEAD_ACTIVITY_POSE:
                return createHeadActivityPose(activitySpace, activitySpaceRoot);
            case CAMERA_ACTIVITY_POSE:
                return createCameraViewActivityPose(activitySpace, activitySpaceRoot);
        }
        return null;
    }

    private void setPerceptionPose(Pose pose) {
        when(mPerceptionLibrary.getSession()).thenReturn(mSession);
        androidx.xr.scenecore.impl.perception.Pose perceptionPose =
                pose == null ? null : RuntimeUtils.poseToPerceptionPose(pose);
        switch (testActivityPoseType) {
            case HEAD_ACTIVITY_POSE:
                {
                    when(mSession.getHeadPose()).thenReturn(perceptionPose);
                    break;
                }
            case CAMERA_ACTIVITY_POSE:
                {
                    if (perceptionPose == null) {
                        when(mSession.getStereoViews()).thenReturn(null);
                        break;
                    }
                    ViewProjection viewProjection =
                            new ViewProjection(perceptionPose, new Fov(0, 0, 0, 0));
                    when(mSession.getStereoViews())
                            .thenReturn(new ViewProjections(viewProjection, viewProjection));
                    break;
                }
        }
    }

    /** Creates a generic glTF entity. */
    private GltfEntityImpl createGltfEntity() {
        FakeGltfModelToken modelToken = new FakeGltfModelToken("model");
        GltfModelResourceImpl model = new GltfModelResourceImpl(modelToken);
        return new GltfEntityImpl(
                model, mActivitySpace, mFakeExtensions, mEntityManager, mExecutor);
    }

    @Test
    public void
            getPoseInActivitySpace_noActivitySpaceOpenXrReferenceSpacePose_returnsIdentityPose() {
        mTestActivityPose = createTestActivityPose();
        Pose pose = new Pose(new Vector3(1, 1, 1), new Quaternion(0, 1, 0, 1));
        setPerceptionPose(pose);
        mActivitySpace.mOpenXrReferenceSpacePose = null;

        assertPose(mTestActivityPose.getPoseInActivitySpace(), new Pose());
    }

    @Test
    public void getPoseInActivitySpace_whenAtSamePose_returnsIdentityPose() {
        mTestActivityPose = createTestActivityPose();
        Pose pose = new Pose(new Vector3(1, 1, 1), new Quaternion(0, 1, 0, 1).toNormalized());
        setPerceptionPose(pose);
        mActivitySpace.setOpenXrReferenceSpacePose(Matrix4.fromPose(pose));

        assertPose(mTestActivityPose.getPoseInActivitySpace(), new Pose());
    }

    @Test
    public void getPoseInActivitySpace_returnsDifferencePose() {
        mTestActivityPose = createTestActivityPose();
        Pose pose = new Pose(new Vector3(1, 1, 1), new Quaternion(0, 1, 0, 1));
        setPerceptionPose(pose);
        mActivitySpace.setOpenXrReferenceSpacePose(Matrix4.Identity);

        assertPose(mTestActivityPose.getPoseInActivitySpace(), pose);
    }

    @Test
    public void getActivitySpaceScale_returnsInverseOfActivitySpaceWorldScale() throws Exception {
        float activitySpaceScale = 5f;
        mActivitySpace.setOpenXrReferenceSpacePose(Matrix4.fromScale(activitySpaceScale));
        mTestActivityPose = createTestActivityPose();
        assertVector3(
                mTestActivityPose.getActivitySpaceScale(),
                new Vector3(1f, 1f, 1f).div(activitySpaceScale));
    }

    @Test
    public void getPoseInActivitySpace_withNoActivitySpace_returnsIdentityPose() {
        mTestActivityPose = createTestActivityPose(/* activitySpace= */ null, mActivitySpaceRoot);

        assertPose(mTestActivityPose.getPoseInActivitySpace(), new Pose());
    }

    @Test
    public void getActivitySpacePose_whenAtSamePose_returnsIdentityPose() {
        mTestActivityPose = createTestActivityPose();
        Pose pose = new Pose(new Vector3(1, 1, 1), new Quaternion(0, 1, 0, 1).toNormalized());
        setPerceptionPose(pose);
        when(mActivitySpaceRoot.getPoseInActivitySpace()).thenReturn(pose);

        assertPose(mTestActivityPose.getActivitySpacePose(), new Pose());
    }

    @Test
    public void getActivitySpacePose_returnsDifferencePose() {
        mTestActivityPose = createTestActivityPose();
        Pose pose = new Pose(new Vector3(1, 1, 1), new Quaternion(0, 1, 0, 1));
        setPerceptionPose(pose);
        when(mActivitySpaceRoot.getPoseInActivitySpace()).thenReturn(new Pose());

        assertPose(mTestActivityPose.getActivitySpacePose(), pose);
    }

    @Test
    public void getActivitySpacePoseWithError_returnsLastKnownPose() {
        mTestActivityPose = createTestActivityPose();
        Pose pose = new Pose(new Vector3(1, 1, 1), new Quaternion(0, 1, 0, 1));
        setPerceptionPose(pose);
        when(mActivitySpaceRoot.getPoseInActivitySpace()).thenReturn(new Pose());
        assertPose(mTestActivityPose.getActivitySpacePose(), pose);

        setPerceptionPose(null);
        assertPose(mTestActivityPose.getActivitySpacePose(), pose);
    }

    @Test
    public void getActivitySpacePose_withNonAndroidXrActivitySpaceRoot_returnsIdentityPose()
            throws Exception {
        mTestActivityPose = createTestActivityPose(mActivitySpace, /* activitySpaceRoot= */ null);

        assertPose(mTestActivityPose.getActivitySpacePose(), new Pose());
    }

    @Test
    public void transformPoseTo_withActivitySpace_returnsTransformedPose() {
        mTestActivityPose = createTestActivityPose();
        Pose pose = new Pose(new Vector3(1f, 2f, 3f), Quaternion.Identity);
        setPerceptionPose(pose);
        mActivitySpace.setOpenXrReferenceSpacePose(Matrix4.Identity);

        Pose userHeadSpaceOffset =
                new Pose(
                        new Vector3(10f, 0f, 0f),
                        Quaternion.fromEulerAngles(new Vector3(0f, 0f, 90f)));
        Pose transformedPose =
                mTestActivityPose.transformPoseTo(userHeadSpaceOffset, mActivitySpace);
        assertPose(
                transformedPose,
                new Pose(
                        new Vector3(11f, 2f, 3f),
                        Quaternion.fromEulerAngles(new Vector3(0f, 0f, 90f))));
    }

    @Test
    public void transformPoseTo_fromActivitySpaceChild_returnsUserHeadSpacePose() {
        mTestActivityPose = createTestActivityPose();
        GltfEntityImpl childEntity1 = createGltfEntity();
        Pose pose = new Pose(new Vector3(1f, 2f, 3f), Quaternion.Identity);
        Pose childPose = new Pose(new Vector3(-1f, -2f, -3f), Quaternion.Identity);

        mActivitySpace.setOpenXrReferenceSpacePose(Matrix4.Identity);
        mActivitySpace.addChild(childEntity1);
        childEntity1.setPose(childPose);
        setPerceptionPose(pose);

        assertPose(
                mActivitySpace.transformPoseTo(new Pose(), mTestActivityPose),
                new Pose(new Vector3(-1f, -2f, -3f), Quaternion.Identity));

        Pose transformedPose = childEntity1.transformPoseTo(new Pose(), mTestActivityPose);
        assertPose(transformedPose, new Pose(new Vector3(-2f, -4f, -6f), Quaternion.Identity));
    }
}
