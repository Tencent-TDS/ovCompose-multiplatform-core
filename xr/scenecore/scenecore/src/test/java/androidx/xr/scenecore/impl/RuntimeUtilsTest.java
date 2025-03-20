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

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.util.Log;

import androidx.xr.runtime.math.Matrix4;
import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Quaternion;
import androidx.xr.runtime.math.Vector3;
import androidx.xr.scenecore.JxrPlatformAdapter.InputEvent;
import androidx.xr.scenecore.JxrPlatformAdapter.PlaneSemantic;
import androidx.xr.scenecore.JxrPlatformAdapter.PlaneType;
import androidx.xr.scenecore.JxrPlatformAdapter.ResizeEvent;
import androidx.xr.scenecore.JxrPlatformAdapter.SpatialCapabilities;
import androidx.xr.scenecore.impl.perception.Plane;

import com.android.extensions.xr.environment.EnvironmentVisibilityState;
import com.android.extensions.xr.environment.PassthroughVisibilityState;
import com.android.extensions.xr.environment.ShadowPassthroughVisibilityState;
import com.android.extensions.xr.node.Mat4f;
import com.android.extensions.xr.node.ReformEvent;
import com.android.extensions.xr.space.ShadowSpatialCapabilities;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.junit.rules.ExpectedLogMessagesRule;

import java.util.regex.Pattern;

@RunWith(RobolectricTestRunner.class)
public final class RuntimeUtilsTest {

    @Rule
    public final ExpectedLogMessagesRule expectedLogMessagesRule = new ExpectedLogMessagesRule();

    @Test
    public void getPlaneTypeHorizontal_returnsHorizontal() {
        assertThat(RuntimeUtils.getPlaneType(PlaneType.HORIZONTAL))
                .isEqualTo(Plane.Type.HORIZONTAL_UPWARD_FACING);
    }

    @Test
    public void getPlaneTypeVertical_returnsVertical() {
        assertThat(RuntimeUtils.getPlaneType(PlaneType.VERTICAL)).isEqualTo(Plane.Type.VERTICAL);
    }

    @Test
    public void getPlaneTypeAny_returnsArbitrary() {
        assertThat(RuntimeUtils.getPlaneType(PlaneType.ANY)).isEqualTo(Plane.Type.ARBITRARY);
    }

    @Test
    public void getPlaneTypeHorizontalUpwardFacingFromPerception_returnsHorizontal() {
        assertThat(RuntimeUtils.getPlaneType(Plane.Type.HORIZONTAL_UPWARD_FACING))
                .isEqualTo(PlaneType.HORIZONTAL);
    }

    @Test
    public void getPlaneTypeHorizontalDownwardFacingFromPerception_returnsHorizontal() {
        assertThat(RuntimeUtils.getPlaneType(Plane.Type.HORIZONTAL_DOWNWARD_FACING))
                .isEqualTo(PlaneType.HORIZONTAL);
    }

    @Test
    public void getPlaneTypeVerticalFromPerception_returnsVertical() {
        assertThat(RuntimeUtils.getPlaneType(Plane.Type.VERTICAL)).isEqualTo(PlaneType.VERTICAL);
    }

    @Test
    public void getPlaneTypeArbitraryFromPerception_returnsAny() {
        assertThat(RuntimeUtils.getPlaneType(PlaneType.ANY)).isEqualTo(Plane.Type.ARBITRARY);
    }

    @Test
    public void getPlaneLabelWall_returnsWall() {
        assertThat(RuntimeUtils.getPlaneLabel(PlaneSemantic.WALL)).isEqualTo(Plane.Label.WALL);
    }

    @Test
    public void getPlaneLabelFloor_returnsFloor() {
        assertThat(RuntimeUtils.getPlaneLabel(PlaneSemantic.FLOOR)).isEqualTo(Plane.Label.FLOOR);
    }

    @Test
    public void getPlaneLabelCeiling_returnsCeiling() {
        assertThat(RuntimeUtils.getPlaneLabel(PlaneSemantic.CEILING))
                .isEqualTo(Plane.Label.CEILING);
    }

    @Test
    public void getPlaneLabelTable_returnsTable() {
        assertThat(RuntimeUtils.getPlaneLabel(PlaneSemantic.TABLE)).isEqualTo(Plane.Label.TABLE);
    }

    @Test
    public void getPlaneLabelAny_returnsUnknown() {
        assertThat(RuntimeUtils.getPlaneLabel(PlaneSemantic.ANY)).isEqualTo(Plane.Label.UNKNOWN);
    }

    @Test
    public void getPlaneSemanticlWall_returnsWall() {
        assertThat(RuntimeUtils.getPlaneSemantic(Plane.Label.WALL)).isEqualTo(PlaneSemantic.WALL);
    }

    @Test
    public void getPlaneSemanticFloor_returnsFloor() {
        assertThat(RuntimeUtils.getPlaneSemantic(Plane.Label.FLOOR)).isEqualTo(PlaneSemantic.FLOOR);
    }

    @Test
    public void getPlaneSemanticCeiling_returnsCeiling() {
        assertThat(RuntimeUtils.getPlaneSemantic(Plane.Label.CEILING))
                .isEqualTo(PlaneSemantic.CEILING);
    }

    @Test
    public void getPlaneSemanticTable_returnsTable() {
        assertThat(RuntimeUtils.getPlaneSemantic(Plane.Label.TABLE)).isEqualTo(PlaneSemantic.TABLE);
    }

    @Test
    public void getPlaneSemanticUnknown_returnsAny() {
        assertThat(RuntimeUtils.getPlaneSemantic(Plane.Label.UNKNOWN)).isEqualTo(PlaneSemantic.ANY);
    }

    @Test
    public void getMatrix_returnsMatrix() {
        float[] expected = new float[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
        Mat4f matrix = new Mat4f(expected);

        assertThat(RuntimeUtils.getMatrix(matrix).getData())
                .usingExactEquality()
                .containsExactly(new Matrix4(expected).getData())
                .inOrder();
    }

    @Test
    public void fromPerceptionPose_returnsPose() {
        Pose expectedPose = new Pose(new Vector3(1, 2, 3), new Quaternion(0, 0, 0, 1));
        androidx.xr.scenecore.impl.perception.Pose perceptionPose =
                new androidx.xr.scenecore.impl.perception.Pose(1, 2, 3, 0, 0, 0, 1);

        assertPose(RuntimeUtils.fromPerceptionPose(perceptionPose), expectedPose);
    }

    @Test
    public void poseToPerceptionPose_returnsPerceptionPose() {
        androidx.xr.scenecore.impl.perception.Pose expectedPerceptionPose =
                new androidx.xr.scenecore.impl.perception.Pose(1, 2, 3, 0, 0, 0, 1);
        Pose pose = new Pose(new Vector3(1, 2, 3), new Quaternion(0, 0, 0, 1));

        assertThat(RuntimeUtils.poseToPerceptionPose(pose)).isEqualTo(expectedPerceptionPose);
    }

    @Test
    public void convertSpatialCapabilities_noCapabilities() {
        com.android.extensions.xr.space.SpatialCapabilities extensionCapabilities =
                ShadowSpatialCapabilities.create();
        SpatialCapabilities caps = RuntimeUtils.convertSpatialCapabilities(extensionCapabilities);
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_UI)).isFalse();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_3D_CONTENT)).isFalse();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL))
                .isFalse();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_APP_ENVIRONMENT))
                .isFalse();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_SPATIAL_AUDIO))
                .isFalse();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_EMBED_ACTIVITY))
                .isFalse();
    }

    @Test
    public void convertSpatialCapabilities_allCapabilities() {
        com.android.extensions.xr.space.SpatialCapabilities extensionCapabilities =
                ShadowSpatialCapabilities.createAll();
        SpatialCapabilities caps = RuntimeUtils.convertSpatialCapabilities(extensionCapabilities);
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_UI)).isTrue();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_3D_CONTENT)).isTrue();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL))
                .isTrue();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_APP_ENVIRONMENT))
                .isTrue();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_SPATIAL_AUDIO))
                .isTrue();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_EMBED_ACTIVITY))
                .isTrue();
    }

    @Test
    public void convertSpatialCapabilities_singleCapability() {
        // check conversions of a few different instances of the extensions SpatialCapabilities that
        // each have exactly one capability.
        com.android.extensions.xr.space.SpatialCapabilities extensionCapabilities =
                ShadowSpatialCapabilities.create(
                        androidx.xr.extensions.space.SpatialCapabilities.SPATIAL_UI_CAPABLE);
        SpatialCapabilities caps = RuntimeUtils.convertSpatialCapabilities(extensionCapabilities);
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_UI)).isTrue();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_3D_CONTENT)).isFalse();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL))
                .isFalse();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_APP_ENVIRONMENT))
                .isFalse();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_SPATIAL_AUDIO))
                .isFalse();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_EMBED_ACTIVITY))
                .isFalse();

        extensionCapabilities =
                ShadowSpatialCapabilities.create(
                        androidx.xr.extensions.space.SpatialCapabilities
                                .SPATIAL_3D_CONTENTS_CAPABLE);
        caps = RuntimeUtils.convertSpatialCapabilities(extensionCapabilities);
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_UI)).isFalse();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_3D_CONTENT)).isTrue();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL))
                .isFalse();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_APP_ENVIRONMENT))
                .isFalse();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_SPATIAL_AUDIO))
                .isFalse();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_EMBED_ACTIVITY))
                .isFalse();

        extensionCapabilities =
                ShadowSpatialCapabilities.create(
                        androidx.xr.extensions.space.SpatialCapabilities.SPATIAL_AUDIO_CAPABLE);
        caps = RuntimeUtils.convertSpatialCapabilities(extensionCapabilities);
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_UI)).isFalse();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_3D_CONTENT)).isFalse();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL))
                .isFalse();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_APP_ENVIRONMENT))
                .isFalse();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_SPATIAL_AUDIO))
                .isTrue();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_EMBED_ACTIVITY))
                .isFalse();
    }

    @Test
    public void convertSpatialCapabilities_mixedCapabilities() {
        // Check conversions for a couple of different combinations of capabilities.
        com.android.extensions.xr.space.SpatialCapabilities extensionCapabilities =
                ShadowSpatialCapabilities.create(
                        androidx.xr.extensions.space.SpatialCapabilities.SPATIAL_AUDIO_CAPABLE,
                        androidx.xr.extensions.space.SpatialCapabilities
                                .SPATIAL_3D_CONTENTS_CAPABLE);
        SpatialCapabilities caps = RuntimeUtils.convertSpatialCapabilities(extensionCapabilities);
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_UI)).isFalse();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_3D_CONTENT)).isTrue();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL))
                .isFalse();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_APP_ENVIRONMENT))
                .isFalse();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_SPATIAL_AUDIO))
                .isTrue();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_EMBED_ACTIVITY))
                .isFalse();

        extensionCapabilities =
                extensionCapabilities =
                        ShadowSpatialCapabilities.create(
                                androidx.xr.extensions.space.SpatialCapabilities.SPATIAL_UI_CAPABLE,
                                androidx.xr.extensions.space.SpatialCapabilities
                                        .PASSTHROUGH_CONTROL_CAPABLE,
                                androidx.xr.extensions.space.SpatialCapabilities
                                        .APP_ENVIRONMENTS_CAPABLE,
                                androidx.xr.extensions.space.SpatialCapabilities
                                        .SPATIAL_ACTIVITY_EMBEDDING_CAPABLE);
        caps = RuntimeUtils.convertSpatialCapabilities(extensionCapabilities);
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_UI)).isTrue();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_3D_CONTENT)).isFalse();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL))
                .isTrue();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_APP_ENVIRONMENT))
                .isTrue();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_SPATIAL_AUDIO))
                .isFalse();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_EMBED_ACTIVITY))
                .isTrue();
    }

    @Test
    public void getIsSpatialEnvironmentPreferenceActive_convertsFromExtensionState() {
        assertThat(
                        RuntimeUtils.getIsSpatialEnvironmentPreferenceActive(
                                EnvironmentVisibilityState.INVISIBLE))
                .isFalse();

        assertThat(
                        RuntimeUtils.getIsSpatialEnvironmentPreferenceActive(
                                EnvironmentVisibilityState.HOME_VISIBLE))
                .isFalse();

        assertThat(
                        RuntimeUtils.getIsSpatialEnvironmentPreferenceActive(
                                EnvironmentVisibilityState.APP_VISIBLE))
                .isTrue();
    }

    @Test
    public void getPassthroughOpacity_returnsZeroFromDisabledExtensionState() {
        PassthroughVisibilityState passthroughVisibilityState =
                ShadowPassthroughVisibilityState.create(PassthroughVisibilityState.DISABLED, 0.0f);
        assertThat(RuntimeUtils.getPassthroughOpacity(passthroughVisibilityState)).isEqualTo(0.0f);

        passthroughVisibilityState =
                ShadowPassthroughVisibilityState.create(PassthroughVisibilityState.DISABLED, 1.0f);
        assertThat(RuntimeUtils.getPassthroughOpacity(passthroughVisibilityState)).isEqualTo(0.0f);
    }

    @Test
    public void getPassthroughOpacity_convertsValidValuesFromExtensionState() {
        PassthroughVisibilityState passthroughVisibilityState =
                ShadowPassthroughVisibilityState.create(PassthroughVisibilityState.HOME, 0.5f);
        assertThat(RuntimeUtils.getPassthroughOpacity(passthroughVisibilityState)).isEqualTo(0.5f);

        passthroughVisibilityState =
                ShadowPassthroughVisibilityState.create(PassthroughVisibilityState.APP, 0.75f);
        assertThat(RuntimeUtils.getPassthroughOpacity(passthroughVisibilityState)).isEqualTo(0.75f);

        passthroughVisibilityState =
                ShadowPassthroughVisibilityState.create(PassthroughVisibilityState.SYSTEM, 1.0f);
        assertThat(RuntimeUtils.getPassthroughOpacity(passthroughVisibilityState)).isEqualTo(1.0f);
    }

    @Test
    public void getPassthroughOpacity_convertsInvalidValuesFromExtensionStateToOneAndLogsError() {
        PassthroughVisibilityState passthroughVisibilityState =
                ShadowPassthroughVisibilityState.create(PassthroughVisibilityState.HOME, 0.0f);
        assertThat(RuntimeUtils.getPassthroughOpacity(passthroughVisibilityState)).isEqualTo(1.0f);

        passthroughVisibilityState =
                ShadowPassthroughVisibilityState.create(
                        PassthroughVisibilityState.APP, -0.0000001f);
        assertThat(RuntimeUtils.getPassthroughOpacity(passthroughVisibilityState)).isEqualTo(1.0f);

        passthroughVisibilityState =
                ShadowPassthroughVisibilityState.create(PassthroughVisibilityState.SYSTEM, -1.0f);
        assertThat(RuntimeUtils.getPassthroughOpacity(passthroughVisibilityState)).isEqualTo(1.0f);

        expectedLogMessagesRule.expectLogMessagePattern(
                Log.ERROR,
                "RuntimeUtils",
                Pattern.compile(".* Opacity should be greater than zero.*"));
    }

    @Test
    public void getInputEventSource_convertsFromExtensionSource() {
        assertThat(
                        RuntimeUtils.getInputEventSource(
                                androidx.xr.extensions.node.InputEvent.SOURCE_UNKNOWN))
                .isEqualTo(InputEvent.SOURCE_UNKNOWN);
        assertThat(
                        RuntimeUtils.getInputEventSource(
                                androidx.xr.extensions.node.InputEvent.SOURCE_HEAD))
                .isEqualTo(InputEvent.SOURCE_HEAD);
        assertThat(
                        RuntimeUtils.getInputEventSource(
                                androidx.xr.extensions.node.InputEvent.SOURCE_CONTROLLER))
                .isEqualTo(InputEvent.SOURCE_CONTROLLER);
        assertThat(
                        RuntimeUtils.getInputEventSource(
                                androidx.xr.extensions.node.InputEvent.SOURCE_HANDS))
                .isEqualTo(InputEvent.SOURCE_HANDS);
        assertThat(
                        RuntimeUtils.getInputEventSource(
                                androidx.xr.extensions.node.InputEvent.SOURCE_MOUSE))
                .isEqualTo(InputEvent.SOURCE_MOUSE);
        assertThat(
                        RuntimeUtils.getInputEventSource(
                                androidx.xr.extensions.node.InputEvent.SOURCE_GAZE_AND_GESTURE))
                .isEqualTo(InputEvent.SOURCE_GAZE_AND_GESTURE);
    }

    @Test
    public void getInputEventSource_throwsExceptionForInvalidValue() {
        assertThrows(IllegalArgumentException.class, () -> RuntimeUtils.getInputEventSource(100));
    }

    @Test
    public void getInputEventPointerType_convertsFromExtensionPointerType() {
        assertThat(
                        RuntimeUtils.getInputEventPointerType(
                                androidx.xr.extensions.node.InputEvent.POINTER_TYPE_DEFAULT))
                .isEqualTo(InputEvent.POINTER_TYPE_DEFAULT);
        assertThat(
                        RuntimeUtils.getInputEventPointerType(
                                androidx.xr.extensions.node.InputEvent.POINTER_TYPE_LEFT))
                .isEqualTo(InputEvent.POINTER_TYPE_LEFT);
        assertThat(
                        RuntimeUtils.getInputEventPointerType(
                                androidx.xr.extensions.node.InputEvent.POINTER_TYPE_RIGHT))
                .isEqualTo(InputEvent.POINTER_TYPE_RIGHT);
    }

    @Test
    public void getInputEventPointerType_throwsExceptionForInvalidValue() {
        assertThrows(
                IllegalArgumentException.class, () -> RuntimeUtils.getInputEventPointerType(100));
    }

    @Test
    public void getInputEventAction_convertsFromExtensionAction() {
        assertThat(
                        RuntimeUtils.getInputEventAction(
                                androidx.xr.extensions.node.InputEvent.ACTION_DOWN))
                .isEqualTo(InputEvent.ACTION_DOWN);
        assertThat(
                        RuntimeUtils.getInputEventAction(
                                androidx.xr.extensions.node.InputEvent.ACTION_UP))
                .isEqualTo(InputEvent.ACTION_UP);
        assertThat(
                        RuntimeUtils.getInputEventAction(
                                androidx.xr.extensions.node.InputEvent.ACTION_MOVE))
                .isEqualTo(InputEvent.ACTION_MOVE);
        assertThat(
                        RuntimeUtils.getInputEventAction(
                                androidx.xr.extensions.node.InputEvent.ACTION_CANCEL))
                .isEqualTo(InputEvent.ACTION_CANCEL);
        assertThat(
                        RuntimeUtils.getInputEventAction(
                                androidx.xr.extensions.node.InputEvent.ACTION_HOVER_MOVE))
                .isEqualTo(InputEvent.ACTION_HOVER_MOVE);
        assertThat(
                        RuntimeUtils.getInputEventAction(
                                androidx.xr.extensions.node.InputEvent.ACTION_HOVER_ENTER))
                .isEqualTo(InputEvent.ACTION_HOVER_ENTER);
        assertThat(
                        RuntimeUtils.getInputEventAction(
                                androidx.xr.extensions.node.InputEvent.ACTION_HOVER_EXIT))
                .isEqualTo(InputEvent.ACTION_HOVER_EXIT);
    }

    @Test
    public void getInputEventAction_throwsExceptionForInvalidValue() {
        assertThrows(IllegalArgumentException.class, () -> RuntimeUtils.getInputEventAction(100));
    }

    @Test
    public void getResizeEventState_convertsFromExtensionResizeState() {
        assertThat(RuntimeUtils.getResizeEventState(ReformEvent.REFORM_STATE_UNKNOWN))
                .isEqualTo(ResizeEvent.RESIZE_STATE_UNKNOWN);
        assertThat(RuntimeUtils.getResizeEventState(ReformEvent.REFORM_STATE_START))
                .isEqualTo(ResizeEvent.RESIZE_STATE_START);
        assertThat(RuntimeUtils.getResizeEventState(ReformEvent.REFORM_STATE_ONGOING))
                .isEqualTo(ResizeEvent.RESIZE_STATE_ONGOING);
        assertThat(RuntimeUtils.getResizeEventState(ReformEvent.REFORM_STATE_END))
                .isEqualTo(ResizeEvent.RESIZE_STATE_END);
    }

    @Test
    public void getResizeEventState_throwsExceptionForInvalidValue() {
        assertThrows(IllegalArgumentException.class, () -> RuntimeUtils.getResizeEventState(100));
    }
}
