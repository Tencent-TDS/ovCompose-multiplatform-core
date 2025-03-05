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

package androidx.xr.scenecore

import androidx.xr.runtime.math.Matrix4
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.JxrPlatformAdapter.Dimensions as RuntimeDimensions
import androidx.xr.scenecore.JxrPlatformAdapter.Entity as RuntimeEntity
import androidx.xr.scenecore.JxrPlatformAdapter.InputEvent as RuntimeInputEvent
import androidx.xr.scenecore.JxrPlatformAdapter.InputEvent.HitInfo as RuntimeHitInfo
import androidx.xr.scenecore.JxrPlatformAdapter.MoveEvent as RuntimeMoveEvent
import androidx.xr.scenecore.JxrPlatformAdapter.PixelDimensions as RuntimePixelDimensions
import androidx.xr.scenecore.JxrPlatformAdapter.ResizeEvent as RuntimeResizeEvent
import androidx.xr.scenecore.JxrPlatformAdapter.SpatialCapabilities as RuntimeSpatialCapabilities
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class UtilsTest {
    @Test
    fun verifyDefaultPoseIsIdentity() {
        val rtPose = Pose.Identity

        assertThat(rtPose.translation.x).isZero()
        assertThat(rtPose.translation.y).isZero()
        assertThat(rtPose.translation.z).isZero()
        assertThat(rtPose.rotation.x).isZero()
        assertThat(rtPose.rotation.y).isZero()
        assertThat(rtPose.rotation.z).isZero()
        assertThat(rtPose.rotation.w).isEqualTo(1f)
    }

    @Test
    fun verifyPoseToRtPoseConversion() {
        val rtPose = Pose(Vector3(1f, 2f, 3f), Quaternion(1f, 2f, 3f, 4f).toNormalized())

        assertThat(rtPose.translation.x).isEqualTo(1f)
        assertThat(rtPose.translation.y).isEqualTo(2f)
        assertThat(rtPose.translation.z).isEqualTo(3f)

        // The quaternion is always normalized, so the retrieved values differ from the original
        // ones.
        assertThat(rtPose.rotation.x).isWithin(1e-5f).of(0.18257418f)
        assertThat(rtPose.rotation.y).isWithin(1e-5f).of(0.36514837f)
        assertThat(rtPose.rotation.z).isWithin(1e-5f).of(0.5477225f)
        assertThat(rtPose.rotation.w).isWithin(1e-5f).of(0.73029673f)
    }

    @Test
    fun verifyRtPoseToPoseConversion() {
        val pose = Pose(Vector3(1f, 2f, 3f), Quaternion(1f, 2f, 3f, 4f).toNormalized())

        assertThat(pose.translation.x).isEqualTo(1f)
        assertThat(pose.translation.y).isEqualTo(2f)
        assertThat(pose.translation.z).isEqualTo(3f)

        // The quaternion is always normalized, so the retrieved values differ from the original
        // ones.
        assertThat(pose.rotation.x).isWithin(1e-5f).of(0.18257418f)
        assertThat(pose.rotation.y).isWithin(1e-5f).of(0.36514837f)
        assertThat(pose.rotation.z).isWithin(1e-5f).of(0.5477225f)
        assertThat(pose.rotation.w).isWithin(1e-5f).of(0.73029673f)
    }

    @Test
    fun verifyRtVector3toVector3() {
        val vector3: Vector3 = Vector3(1f, 2f, 3f)
        assertThat(vector3.x).isEqualTo(1f)
        assertThat(vector3.y).isEqualTo(2f)
        assertThat(vector3.z).isEqualTo(3f)
    }

    @Test
    fun verifyVector3toRtVector3() {
        val rtVector3: Vector3 = Vector3(1f, 2f, 3f)
        assertThat(rtVector3.x).isEqualTo(1f)
        assertThat(rtVector3.y).isEqualTo(2f)
        assertThat(rtVector3.z).isEqualTo(3f)
    }

    @Test
    fun verifyRtMoveEventToMoveEvent() {
        val vector0 = Vector3(0f, 0f, 0f)
        val vector1 = Vector3(1f, 1f, 1f)
        val vector2 = Vector3(2f, 2f, 2f)

        val initialInputRay = JxrPlatformAdapter.Ray(vector0, vector1)
        val currentInputRay = JxrPlatformAdapter.Ray(vector1, vector2)
        val entityManager = EntityManager()
        val activitySpace = mock<JxrPlatformAdapter.ActivitySpace>()
        entityManager.setEntityForRtEntity(activitySpace, mock<Entity>())
        val moveEvent =
            RuntimeMoveEvent(
                    RuntimeMoveEvent.MOVE_STATE_ONGOING,
                    initialInputRay,
                    currentInputRay,
                    Pose(),
                    Pose(vector1, Quaternion.Identity),
                    vector1,
                    vector1,
                    activitySpace,
                    null,
                    null,
                )
                .toMoveEvent(entityManager)

        assertThat(moveEvent.moveState).isEqualTo(MoveEvent.MOVE_STATE_ONGOING)

        assertThat(moveEvent.initialInputRay.origin.x).isEqualTo(0f)
        assertThat(moveEvent.initialInputRay.origin.y).isEqualTo(0f)
        assertThat(moveEvent.initialInputRay.origin.z).isEqualTo(0f)
        assertThat(moveEvent.initialInputRay.direction.x).isEqualTo(1f)
        assertThat(moveEvent.initialInputRay.direction.y).isEqualTo(1f)
        assertThat(moveEvent.initialInputRay.direction.z).isEqualTo(1f)

        assertThat(moveEvent.currentInputRay.origin.x).isEqualTo(1f)
        assertThat(moveEvent.currentInputRay.origin.y).isEqualTo(1f)
        assertThat(moveEvent.currentInputRay.origin.z).isEqualTo(1f)
        assertThat(moveEvent.currentInputRay.direction.x).isEqualTo(2f)
        assertThat(moveEvent.currentInputRay.direction.y).isEqualTo(2f)
        assertThat(moveEvent.currentInputRay.direction.z).isEqualTo(2f)

        assertThat(moveEvent.previousPose.translation.x).isEqualTo(0f)
        assertThat(moveEvent.previousPose.translation.y).isEqualTo(0f)
        assertThat(moveEvent.previousPose.translation.z).isEqualTo(0f)
        assertThat(moveEvent.previousPose.rotation.x).isEqualTo(0f)
        assertThat(moveEvent.previousPose.rotation.y).isEqualTo(0f)
        assertThat(moveEvent.previousPose.rotation.z).isEqualTo(0f)
        assertThat(moveEvent.previousPose.rotation.w).isEqualTo(1f)

        assertThat(moveEvent.currentPose.translation.x).isEqualTo(1f)
        assertThat(moveEvent.currentPose.translation.y).isEqualTo(1f)
        assertThat(moveEvent.currentPose.translation.z).isEqualTo(1f)
        assertThat(moveEvent.currentPose.rotation.x).isEqualTo(0f)
        assertThat(moveEvent.currentPose.rotation.y).isEqualTo(0f)
        assertThat(moveEvent.currentPose.rotation.z).isEqualTo(0f)
        assertThat(moveEvent.currentPose.rotation.w).isEqualTo(1f)

        assertThat(moveEvent.previousScale).isEqualTo(1f)

        assertThat(moveEvent.currentScale).isEqualTo(1f)
    }

    @Test
    fun verifyRtInputEventToInputEventConversion() {
        val entityManager = EntityManager()
        val activitySpace = mock<JxrPlatformAdapter.ActivitySpace>()
        entityManager.setEntityForRtEntity(activitySpace, mock<Entity>())
        val inputEvent =
            RuntimeInputEvent(
                    RuntimeInputEvent.SOURCE_HANDS,
                    RuntimeInputEvent.POINTER_TYPE_LEFT,
                    123456789,
                    Vector3(1f, 2f, 3f),
                    Vector3(4f, 5f, 6f),
                    RuntimeInputEvent.ACTION_DOWN,
                    null,
                    null,
                )
                .toInputEvent(entityManager)
        assertThat(inputEvent.source).isEqualTo(InputEvent.SOURCE_HANDS)
        assertThat(inputEvent.pointerType).isEqualTo(InputEvent.POINTER_TYPE_LEFT)
        assertThat(inputEvent.timestamp).isEqualTo(123456789)
        assertThat(inputEvent.origin.x).isEqualTo(1f)
        assertThat(inputEvent.origin.y).isEqualTo(2f)
        assertThat(inputEvent.origin.z).isEqualTo(3f)
        assertThat(inputEvent.direction.x).isEqualTo(4f)
        assertThat(inputEvent.direction.y).isEqualTo(5f)
        assertThat(inputEvent.direction.z).isEqualTo(6f)
        assertThat(inputEvent.action).isEqualTo(InputEvent.ACTION_DOWN)
    }

    @Test
    fun verifyRtHitInfoToHitInfoConversion() {
        val entityManager = EntityManager()
        val rtMockEntity = mock<RuntimeEntity>()
        val mockEntity = mock<Entity>()
        entityManager.setEntityForRtEntity(rtMockEntity, mockEntity)
        val hitPosition = Vector3(1f, 2f, 3f)
        val transform = Matrix4.Identity
        val hitInfo = RuntimeHitInfo(rtMockEntity, hitPosition, transform).toHitInfo(entityManager)

        assertThat(hitInfo).isNotNull()
        assertThat(hitInfo!!.inputEntity).isEqualTo(mockEntity)
        assertThat(hitInfo.hitPosition).isEqualTo(hitPosition)
        assertThat(hitInfo.transform).isEqualTo(transform)
    }

    @Test
    fun verifyRtHitInfoToHitInfoConversionWhenEntityNotFound() {
        val entityManager = EntityManager()
        val rtMockEntity = mock<RuntimeEntity>()
        val hitPosition = Vector3(1f, 2f, 3f)
        val transform = Matrix4.Identity
        val hitInfo = RuntimeHitInfo(rtMockEntity, hitPosition, transform).toHitInfo(entityManager)

        // EntityManager does not have the entity for the given RuntimeEntity, so the hit info is
        // null.
        assertThat(hitInfo).isNull()
    }

    @Test
    fun verifyRtDimensionsToDimensions() {
        val dimensions: Dimensions = RuntimeDimensions(2f, 4f, 6f).toDimensions()
        assertThat(dimensions.width).isEqualTo(2f)
        assertThat(dimensions.height).isEqualTo(4f)
        assertThat(dimensions.depth).isEqualTo(6f)
    }

    @Test
    fun verifyRtPixelDimensionsToPixelDimensions() {
        val pixelDimensions: PixelDimensions = RuntimePixelDimensions(14, 15).toPixelDimensions()
        assertThat(pixelDimensions.width).isEqualTo(14)
        assertThat(pixelDimensions.height).isEqualTo(15)
    }

    @Test
    fun verifyPixelDimensionsToRtPixelDimensions() {
        val pixelDimensions: PixelDimensions = RuntimePixelDimensions(17, 18).toPixelDimensions()
        assertThat(pixelDimensions.width).isEqualTo(17)
        assertThat(pixelDimensions.height).isEqualTo(18)
    }

    @Test
    fun verifyRuntimeResizeEventToResizeEvent() {
        val resizeEvent: ResizeEvent =
            RuntimeResizeEvent(RuntimeResizeEvent.RESIZE_STATE_START, RuntimeDimensions(1f, 3f, 5f))
                .toResizeEvent()
        assertThat(resizeEvent.resizeState).isEqualTo(ResizeEvent.RESIZE_STATE_START)
        assertThat(resizeEvent.newSize.width).isEqualTo(1f)
        assertThat(resizeEvent.newSize.height).isEqualTo(3f)
        assertThat(resizeEvent.newSize.depth).isEqualTo(5f)
    }

    @Test
    fun runtimeSpatialCapabilitiesToSpatialCapabilities_noCapabilities() {
        val caps = RuntimeSpatialCapabilities(0).toSpatialCapabilities()
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_UI)).isFalse()
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_3D_CONTENT)).isFalse()
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL))
            .isFalse()
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_APP_ENVIRONMENT))
            .isFalse()
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_SPATIAL_AUDIO))
            .isFalse()
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_EMBED_ACTIVITY))
            .isFalse()
    }

    @Test
    fun runtimeSpatialCapabilitiesToSpatialCapabilities_singleCapability() {
        var caps =
            RuntimeSpatialCapabilities(RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_UI)
                .toSpatialCapabilities()
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_UI)).isTrue()
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_3D_CONTENT)).isFalse()
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL))
            .isFalse()
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_APP_ENVIRONMENT))
            .isFalse()
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_SPATIAL_AUDIO))
            .isFalse()
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_EMBED_ACTIVITY))
            .isFalse()

        caps =
            RuntimeSpatialCapabilities(RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_SPATIAL_AUDIO)
                .toSpatialCapabilities()
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_UI)).isFalse()
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_3D_CONTENT)).isFalse()
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL))
            .isFalse()
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_APP_ENVIRONMENT))
            .isFalse()
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_SPATIAL_AUDIO))
            .isTrue()
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_EMBED_ACTIVITY))
            .isFalse()
    }

    @Test
    fun runtimeSpatialCapabilitiesToSpatialCapabilities_allCapabilities() {
        val caps =
            RuntimeSpatialCapabilities(
                    RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_UI or
                        RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_3D_CONTENT or
                        RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL or
                        RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_APP_ENVIRONMENT or
                        RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_SPATIAL_AUDIO or
                        RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_EMBED_ACTIVITY
                )
                .toSpatialCapabilities()
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_UI)).isTrue()
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_3D_CONTENT)).isTrue()
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL))
            .isTrue()
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_APP_ENVIRONMENT))
            .isTrue()
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_SPATIAL_AUDIO))
            .isTrue()
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_EMBED_ACTIVITY))
            .isTrue()
    }

    @Test
    fun runtimeSpatialCapabilitiesToSpatialCapabilities_mixedCapabilities() {
        var caps =
            RuntimeSpatialCapabilities(
                    RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_3D_CONTENT or
                        RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_SPATIAL_AUDIO
                )
                .toSpatialCapabilities()
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_UI)).isFalse()
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_3D_CONTENT)).isTrue()
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL))
            .isFalse()
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_APP_ENVIRONMENT))
            .isFalse()
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_SPATIAL_AUDIO))
            .isTrue()
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_EMBED_ACTIVITY))
            .isFalse()

        caps =
            RuntimeSpatialCapabilities(
                    RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_UI or
                        RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL or
                        RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_APP_ENVIRONMENT or
                        RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_EMBED_ACTIVITY
                )
                .toSpatialCapabilities()
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_UI)).isTrue()
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_3D_CONTENT)).isFalse()
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL))
            .isTrue()
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_APP_ENVIRONMENT))
            .isTrue()
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_SPATIAL_AUDIO))
            .isFalse()
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_EMBED_ACTIVITY))
            .isTrue()
    }

    @Test
    fun intToMoveState_convertsCorrectly() {
        assertThat(
                listOf(
                        JxrPlatformAdapter.MoveEvent.MOVE_STATE_START,
                        JxrPlatformAdapter.MoveEvent.MOVE_STATE_ONGOING,
                        JxrPlatformAdapter.MoveEvent.MOVE_STATE_END,
                    )
                    .map { it.toMoveState() }
            )
            .containsExactly(
                MoveEvent.MOVE_STATE_START,
                MoveEvent.MOVE_STATE_ONGOING,
                MoveEvent.MOVE_STATE_END,
            )
            .inOrder()
    }

    @Test
    fun intToMoveState_invalidValue_throwsError() {
        assertFailsWith<IllegalStateException> { 100.toMoveState() }
    }

    @Test
    fun intToResizeState_convertsCorrectly() {
        assertThat(
                listOf(
                        JxrPlatformAdapter.ResizeEvent.RESIZE_STATE_UNKNOWN,
                        JxrPlatformAdapter.ResizeEvent.RESIZE_STATE_START,
                        JxrPlatformAdapter.ResizeEvent.RESIZE_STATE_ONGOING,
                        JxrPlatformAdapter.ResizeEvent.RESIZE_STATE_END,
                    )
                    .map { it.toResizeState() }
            )
            .containsExactly(
                ResizeEvent.RESIZE_STATE_UNKNOWN,
                ResizeEvent.RESIZE_STATE_START,
                ResizeEvent.RESIZE_STATE_ONGOING,
                ResizeEvent.RESIZE_STATE_END,
            )
            .inOrder()
    }

    @Test
    fun intToResizeState_invalidValue_throwsError() {
        assertFailsWith<IllegalStateException> { 100.toResizeState() }
    }

    @Test
    fun intToInputEventSource_convertsCorrectly() {
        assertThat(
                listOf(
                        JxrPlatformAdapter.InputEvent.SOURCE_UNKNOWN,
                        JxrPlatformAdapter.InputEvent.SOURCE_HEAD,
                        JxrPlatformAdapter.InputEvent.SOURCE_CONTROLLER,
                        JxrPlatformAdapter.InputEvent.SOURCE_HANDS,
                        JxrPlatformAdapter.InputEvent.SOURCE_MOUSE,
                        JxrPlatformAdapter.InputEvent.SOURCE_GAZE_AND_GESTURE,
                    )
                    .map { it.toInputEventSource() }
            )
            .containsExactly(
                InputEvent.SOURCE_UNKNOWN,
                InputEvent.SOURCE_HEAD,
                InputEvent.SOURCE_CONTROLLER,
                InputEvent.SOURCE_HANDS,
                InputEvent.SOURCE_MOUSE,
                InputEvent.SOURCE_GAZE_AND_GESTURE,
            )
            .inOrder()
    }

    @Test
    fun intToInputEventSource_invalidValue_throwsError() {
        assertFailsWith<IllegalStateException> { 100.toInputEventSource() }
    }

    @Test
    fun intToInputEventPointerType_convertsCorrectly() {
        assertThat(
                listOf(
                        JxrPlatformAdapter.InputEvent.POINTER_TYPE_DEFAULT,
                        JxrPlatformAdapter.InputEvent.POINTER_TYPE_LEFT,
                        JxrPlatformAdapter.InputEvent.POINTER_TYPE_RIGHT,
                    )
                    .map { it.toInputEventPointerType() }
            )
            .containsExactly(
                InputEvent.POINTER_TYPE_DEFAULT,
                InputEvent.POINTER_TYPE_LEFT,
                InputEvent.POINTER_TYPE_RIGHT,
            )
            .inOrder()
    }

    @Test
    fun intToInputEventPointerType_invalidValue_throwsError() {
        assertFailsWith<IllegalStateException> { 100.toInputEventPointerType() }
    }

    @Test
    fun intToSpatialCapability_convertsCorrectly() {
        assertThat(
                listOf(
                        JxrPlatformAdapter.SpatialCapabilities.SPATIAL_CAPABILITY_UI,
                        JxrPlatformAdapter.SpatialCapabilities.SPATIAL_CAPABILITY_3D_CONTENT,
                        JxrPlatformAdapter.SpatialCapabilities
                            .SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL,
                        JxrPlatformAdapter.SpatialCapabilities.SPATIAL_CAPABILITY_APP_ENVIRONMENT,
                        JxrPlatformAdapter.SpatialCapabilities.SPATIAL_CAPABILITY_SPATIAL_AUDIO,
                        JxrPlatformAdapter.SpatialCapabilities.SPATIAL_CAPABILITY_EMBED_ACTIVITY,
                    )
                    .map { it.toSpatialCapability() }
            )
            .containsExactly(
                SpatialCapabilities.SPATIAL_CAPABILITY_UI,
                SpatialCapabilities.SPATIAL_CAPABILITY_3D_CONTENT,
                SpatialCapabilities.SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL,
                SpatialCapabilities.SPATIAL_CAPABILITY_APP_ENVIRONMENT,
                SpatialCapabilities.SPATIAL_CAPABILITY_SPATIAL_AUDIO,
                SpatialCapabilities.SPATIAL_CAPABILITY_EMBED_ACTIVITY,
            )
            .inOrder()
    }

    @Test
    fun intToInputEventAction_convertsCorrectly() {
        assertThat(
                listOf(
                        JxrPlatformAdapter.InputEvent.ACTION_DOWN,
                        JxrPlatformAdapter.InputEvent.ACTION_UP,
                        JxrPlatformAdapter.InputEvent.ACTION_MOVE,
                        JxrPlatformAdapter.InputEvent.ACTION_CANCEL,
                        JxrPlatformAdapter.InputEvent.ACTION_HOVER_MOVE,
                        JxrPlatformAdapter.InputEvent.ACTION_HOVER_ENTER,
                        JxrPlatformAdapter.InputEvent.ACTION_HOVER_EXIT,
                    )
                    .map { it.toInputEventAction() }
            )
            .containsExactly(
                InputEvent.ACTION_DOWN,
                InputEvent.ACTION_UP,
                InputEvent.ACTION_MOVE,
                InputEvent.ACTION_CANCEL,
                InputEvent.ACTION_HOVER_MOVE,
                InputEvent.ACTION_HOVER_ENTER,
                InputEvent.ACTION_HOVER_EXIT,
            )
            .inOrder()
    }

    @Test
    fun intToInputEventAction_invalidValue_throwsError() {
        assertFailsWith<IllegalStateException> { 100.toInputEventAction() }
    }

    @Test
    fun anchorPlacementToRuntimeAnchorPlacement_setsCorrectly() {
        val mockRuntime = mock<JxrPlatformAdapter>()
        val mockAnchorPlacement1 = mock<JxrPlatformAdapter.AnchorPlacement>()
        val mockAnchorPlacement2 = mock<JxrPlatformAdapter.AnchorPlacement>()
        whenever(
                mockRuntime.createAnchorPlacementForPlanes(
                    setOf(JxrPlatformAdapter.PlaneType.HORIZONTAL),
                    setOf(JxrPlatformAdapter.PlaneSemantic.ANY),
                )
            )
            .thenReturn(mockAnchorPlacement1)
        whenever(
                mockRuntime.createAnchorPlacementForPlanes(
                    setOf(JxrPlatformAdapter.PlaneType.ANY),
                    setOf(
                        JxrPlatformAdapter.PlaneSemantic.WALL,
                        JxrPlatformAdapter.PlaneSemantic.FLOOR
                    ),
                )
            )
            .thenReturn(mockAnchorPlacement2)

        val anchorPlacement1 =
            AnchorPlacement.createForPlanes(planeTypeFilter = setOf(PlaneType.HORIZONTAL))
        val anchorPlacement2 =
            AnchorPlacement.createForPlanes(
                planeSemanticFilter = setOf(PlaneSemantic.WALL, PlaneSemantic.FLOOR)
            )

        val rtPlacementSet =
            setOf(anchorPlacement1, anchorPlacement2).toRtAnchorPlacement(mockRuntime)

        assertThat(rtPlacementSet.size).isEqualTo(2)
        assertThat(rtPlacementSet).containsExactly(mockAnchorPlacement1, mockAnchorPlacement2)
    }

    @Test
    fun anchorPlacementToRuntimeAnchotPlacementEmptySet_returnsEmptySet() {
        val mockRuntime = mock<JxrPlatformAdapter>()

        val rtPlacementSet = emptySet<AnchorPlacement>().toRtAnchorPlacement(mockRuntime)

        assertThat(rtPlacementSet).isEmpty()
    }
}
