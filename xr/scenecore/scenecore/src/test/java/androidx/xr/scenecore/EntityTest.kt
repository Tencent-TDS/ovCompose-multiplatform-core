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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.TextView
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.MoreExecutors.directExecutor
import java.util.UUID
import java.util.concurrent.Executor
import java.util.function.Consumer
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

// TODO: b/329902726 - Add a fake runtime and verify CPM integration.
// TODO: b/369199417 - Update EntityTest once createGltfResourceAsync is default.
@RunWith(RobolectricTestRunner::class)
class EntityTest {
    private val activity = Robolectric.buildActivity(Activity::class.java).create().start().get()
    private val mockPlatformAdapter = mock<JxrPlatformAdapter>()
    private val mockGltfModelEntityImpl = mock<JxrPlatformAdapter.GltfEntity>()
    private val mockPanelEntityImpl = mock<JxrPlatformAdapter.PanelEntity>()
    private val mockAnchorEntityImpl = mock<JxrPlatformAdapter.AnchorEntity>()
    private val mockActivityPanelEntity = mock<JxrPlatformAdapter.ActivityPanelEntity>()
    private val mockContentlessEntity = mock<JxrPlatformAdapter.Entity>()
    private val mockSurfaceEntity = mock<JxrPlatformAdapter.SurfaceEntity>()
    private val entityManager = EntityManager()
    private lateinit var session: Session
    private lateinit var activitySpace: ActivitySpace
    private lateinit var gltfModel: GltfModel
    private lateinit var gltfModelEntity: GltfModelEntity
    private lateinit var panelEntity: PanelEntity
    private lateinit var anchorEntity: AnchorEntity
    private lateinit var activityPanelEntity: ActivityPanelEntity
    private lateinit var contentlessEntity: Entity
    private lateinit var surfaceEntity: SurfaceEntity

    interface FakeComponent : Component

    interface SubtypeFakeComponent : FakeComponent

    // Introduce a test Runtime ActivitySpace to test the bounds changed callback.
    class TestRtActivitySpace : JxrPlatformAdapter.ActivitySpace {
        private var boundsChangedListener:
            JxrPlatformAdapter.ActivitySpace.OnBoundsChangedListener? =
            null
        var getBoundsCalled: Boolean = false
        var setScaleCalled: Boolean = false
        var getScaleCalled: Boolean = false
        var setAlphaCalled: Boolean = false
        var getAlphaCalled: Boolean = false
        var setHiddenCalled: Boolean = false
        var getHiddenCalled: Boolean = false
        var getWorldSpaceScaleCalled: Boolean = false
        var getActivitySpaceScaleCalled: Boolean = false

        override fun setPose(pose: Pose, @SpaceValue space: Int) {}

        override fun getPose(@SpaceValue space: Int): Pose {
            return Pose()
        }

        override fun getActivitySpacePose(): Pose {
            return Pose()
        }

        override fun transformPoseTo(
            pose: Pose,
            destination: JxrPlatformAdapter.ActivityPose
        ): Pose {
            return Pose()
        }

        override fun getAlpha(@SpaceValue space: Int): Float = 1.0f.apply { getAlphaCalled = true }

        override fun setAlpha(alpha: Float, @SpaceValue space: Int) {
            setAlphaCalled = true
        }

        override fun isHidden(includeParents: Boolean): Boolean {
            getHiddenCalled = true
            return false
        }

        override fun setHidden(hidden: Boolean) {
            setHiddenCalled = true
        }

        override fun setScale(scale: Vector3, @SpaceValue space: Int) {
            setScaleCalled = true
        }

        override fun getScale(@SpaceValue space: Int): Vector3 {
            getScaleCalled = true
            return Vector3()
        }

        override fun getWorldSpaceScale(): Vector3 {
            getWorldSpaceScaleCalled = true
            return Vector3()
        }

        override fun getActivitySpaceScale(): Vector3 {
            getActivitySpaceScaleCalled = true
            return Vector3()
        }

        override fun addInputEventListener(
            executor: Executor,
            consumer: JxrPlatformAdapter.InputEventListener,
        ) {}

        override fun removeInputEventListener(consumer: JxrPlatformAdapter.InputEventListener) {}

        override fun getParent(): JxrPlatformAdapter.Entity? {
            return null
        }

        override fun setParent(parent: JxrPlatformAdapter.Entity?) {}

        override fun addChild(child: JxrPlatformAdapter.Entity) {}

        override fun addChildren(children: List<JxrPlatformAdapter.Entity>) {}

        override fun getChildren(): List<JxrPlatformAdapter.Entity> {
            return emptyList()
        }

        override fun setContentDescription(text: String) {}

        override fun dispose() {}

        override fun addComponent(component: JxrPlatformAdapter.Component): Boolean = false

        override fun removeComponent(component: JxrPlatformAdapter.Component) {}

        override fun removeAllComponents() {}

        override fun getBounds(): JxrPlatformAdapter.Dimensions =
            JxrPlatformAdapter.Dimensions(1f, 1f, 1f).apply { getBoundsCalled = true }

        override fun addOnBoundsChangedListener(
            listener: JxrPlatformAdapter.ActivitySpace.OnBoundsChangedListener
        ) {
            boundsChangedListener = listener
        }

        override fun removeOnBoundsChangedListener(
            listener: JxrPlatformAdapter.ActivitySpace.OnBoundsChangedListener
        ) {
            boundsChangedListener = null
        }

        fun sendBoundsChanged(dimensions: JxrPlatformAdapter.Dimensions) {
            boundsChangedListener?.onBoundsChanged(dimensions)
        }

        override fun setOnSpaceUpdatedListener(
            listener: JxrPlatformAdapter.SystemSpaceEntity.OnSpaceUpdatedListener?,
            executor: Executor?,
        ) {}
    }

    private val testActivitySpace = TestRtActivitySpace()

    @Before
    fun setUp() {
        whenever(mockPlatformAdapter.spatialEnvironment).thenReturn(mock())
        whenever(mockPlatformAdapter.activitySpace).thenReturn(testActivitySpace)
        whenever(mockPlatformAdapter.activitySpaceRootImpl).thenReturn(mock())
        whenever(mockPlatformAdapter.headActivityPose).thenReturn(mock())
        whenever(mockPlatformAdapter.perceptionSpaceActivityPose).thenReturn(mock())
        whenever(mockPlatformAdapter.loadGltfByAssetNameSplitEngine(Mockito.anyString()))
            .thenReturn(Futures.immediateFuture(mock()))
        whenever(mockPlatformAdapter.createGltfEntity(any(), any(), any()))
            .thenReturn(mockGltfModelEntityImpl)
        whenever(
                mockPlatformAdapter.createPanelEntity(
                    any<Context>(),
                    any<Pose>(),
                    any<View>(),
                    any<JxrPlatformAdapter.PixelDimensions>(),
                    any<String>(),
                    any<JxrPlatformAdapter.Entity>(),
                )
            )
            .thenReturn(mockPanelEntityImpl)
        whenever(mockPlatformAdapter.createAnchorEntity(any(), any(), any(), any()))
            .thenReturn(mockAnchorEntityImpl)
        whenever(mockAnchorEntityImpl.state)
            .thenReturn(JxrPlatformAdapter.AnchorEntity.State.UNANCHORED)
        whenever(mockAnchorEntityImpl.persistState)
            .thenReturn(JxrPlatformAdapter.AnchorEntity.PersistState.PERSIST_NOT_REQUESTED)
        whenever(mockPlatformAdapter.createActivityPanelEntity(any(), any(), any(), any(), any()))
            .thenReturn(mockActivityPanelEntity)
        whenever(mockPlatformAdapter.createEntity(any(), any(), any()))
            .thenReturn(mockContentlessEntity)
        whenever(mockPlatformAdapter.createSurfaceEntity(any(), any(), any(), any()))
            .thenReturn(mockSurfaceEntity)
        whenever(mockPlatformAdapter.getMainPanelEntity()).thenReturn(mockPanelEntityImpl)
        session = Session.create(activity, mockPlatformAdapter)
        activitySpace = ActivitySpace.create(mockPlatformAdapter, entityManager)
        gltfModel = GltfModel.create(session, "test.glb").get()
        gltfModelEntity = GltfModelEntity.create(mockPlatformAdapter, entityManager, gltfModel)
        panelEntity =
            PanelEntity.create(
                context = activity,
                adapter = mockPlatformAdapter,
                entityManager = entityManager,
                view = TextView(activity),
                pixelDimensions = PixelDimensions(720, 480),
                name = "test",
            )
        anchorEntity =
            AnchorEntity.create(
                mockPlatformAdapter,
                entityManager,
                Dimensions(),
                PlaneType.ANY,
                PlaneSemantic.ANY,
                10.seconds.toJavaDuration(),
            )
        activityPanelEntity =
            ActivityPanelEntity.create(
                mockPlatformAdapter,
                entityManager = entityManager,
                PixelDimensions(640, 480),
                "test",
                activity,
            )
        contentlessEntity = ContentlessEntity.create(mockPlatformAdapter, entityManager, "test")
        surfaceEntity =
            SurfaceEntity.create(
                mockPlatformAdapter,
                entityManager,
                SurfaceEntity.StereoMode.SIDE_BY_SIDE,
                Pose.Identity,
                SurfaceEntity.CanvasShape.Quad(1.0f, 1.0f),
            )
    }

    @Test
    fun anchorEntityCreateWithNullTimeout_passesNullToImpl() {
        whenever(mockPlatformAdapter.createAnchorEntity(any(), any(), any(), any()))
            .thenReturn(mockAnchorEntityImpl)
        anchorEntity =
            AnchorEntity.create(
                mockPlatformAdapter,
                entityManager,
                Dimensions(),
                PlaneType.ANY,
                PlaneSemantic.ANY,
            )

        assertThat(anchorEntity).isNotNull()
    }

    @Test
    fun allEntitySetParent_callsRuntimeEntityImplSetParent() {
        panelEntity.setParent(activitySpace)
        gltfModelEntity.setParent(activitySpace)
        anchorEntity.setParent(activitySpace)
        activityPanelEntity.setParent(activitySpace)

        verify(mockPanelEntityImpl).parent = session.platformAdapter.activitySpace
        verify(mockGltfModelEntityImpl).parent = session.platformAdapter.activitySpace
        verify(mockAnchorEntityImpl).parent = session.platformAdapter.activitySpace
        verify(mockActivityPanelEntity).parent = session.platformAdapter.activitySpace
    }

    @Test
    fun allEntitySetParentNull_SetsNullParent() {
        panelEntity.setParent(null)
        gltfModelEntity.setParent(null)
        anchorEntity.setParent(null)
        activityPanelEntity.setParent(null)

        verify(mockPanelEntityImpl).parent = null
        verify(mockGltfModelEntityImpl).parent = null
        verify(mockAnchorEntityImpl).parent = null
        verify(mockActivityPanelEntity).parent = null
    }

    @Test
    fun allEntityGetParent_callsRuntimeEntityImplGetParent() {
        val rtActivitySpace = session.platformAdapter.activitySpace
        whenever(mockActivityPanelEntity.parent).thenReturn(rtActivitySpace)
        whenever(mockPanelEntityImpl.parent).thenReturn(mockActivityPanelEntity)
        whenever(mockGltfModelEntityImpl.parent).thenReturn(mockPanelEntityImpl)
        whenever(mockContentlessEntity.parent).thenReturn(mockGltfModelEntityImpl)
        whenever(mockAnchorEntityImpl.parent).thenReturn(mockContentlessEntity)

        assertThat(activityPanelEntity.getParent()).isEqualTo(activitySpace)
        assertThat(panelEntity.getParent()).isEqualTo(activityPanelEntity)
        assertThat(gltfModelEntity.getParent()).isEqualTo(panelEntity)
        assertThat(contentlessEntity.getParent()).isEqualTo(gltfModelEntity)
        assertThat(anchorEntity.getParent()).isEqualTo(contentlessEntity)

        verify(mockActivityPanelEntity).parent
        verify(mockPanelEntityImpl).parent
        verify(mockGltfModelEntityImpl).parent
        verify(mockContentlessEntity).parent
        verify(mockAnchorEntityImpl).parent
    }

    @Test
    fun allEntityGetParent_nullParent_callsRuntimeEntityImplGetParent() {
        whenever(mockActivityPanelEntity.parent).thenReturn(null)
        whenever(mockPanelEntityImpl.parent).thenReturn(null)
        whenever(mockGltfModelEntityImpl.parent).thenReturn(null)
        whenever(mockContentlessEntity.parent).thenReturn(null)
        whenever(mockAnchorEntityImpl.parent).thenReturn(null)

        assertThat(activityPanelEntity.getParent()).isEqualTo(null)
        assertThat(panelEntity.getParent()).isEqualTo(null)
        assertThat(gltfModelEntity.getParent()).isEqualTo(null)
        assertThat(contentlessEntity.getParent()).isEqualTo(null)
        assertThat(anchorEntity.getParent()).isEqualTo(null)

        verify(mockActivityPanelEntity).parent
        verify(mockPanelEntityImpl).parent
        verify(mockGltfModelEntityImpl).parent
        verify(mockContentlessEntity).parent
        verify(mockAnchorEntityImpl).parent
    }

    @Test
    fun allEntityAddChild_callsRuntimeEntityImplAddChild() {
        anchorEntity.addChild(panelEntity)
        panelEntity.addChild(gltfModelEntity)
        gltfModelEntity.addChild(activityPanelEntity)

        verify(mockAnchorEntityImpl).addChild(mockPanelEntityImpl)
        verify(mockPanelEntityImpl).addChild(mockGltfModelEntityImpl)
        verify(mockGltfModelEntityImpl).addChild(mockActivityPanelEntity)
    }

    @Test
    fun allEntitySetPose_callsRuntimeEntityImplSetPose() {
        val pose = Pose.Identity

        panelEntity.setPose(pose)
        gltfModelEntity.setPose(pose, Space.PARENT)
        anchorEntity.setPose(pose, Space.ACTIVITY)
        activityPanelEntity.setPose(pose, Space.REAL_WORLD)

        verify(mockPanelEntityImpl).setPose(any(), eq(JxrPlatformAdapter.Space.PARENT))
        verify(mockGltfModelEntityImpl).setPose(any(), eq(JxrPlatformAdapter.Space.PARENT))
        verify(mockAnchorEntityImpl).setPose(any(), eq(JxrPlatformAdapter.Space.ACTIVITY))
        verify(mockActivityPanelEntity).setPose(any(), eq(JxrPlatformAdapter.Space.REAL_WORLD))
    }

    @Test
    fun allEntityGetPose_callsRuntimeEntityImplGetPose() {
        whenever(mockPanelEntityImpl.getPose(JxrPlatformAdapter.Space.PARENT)).thenReturn(Pose())
        whenever(mockGltfModelEntityImpl.getPose(JxrPlatformAdapter.Space.PARENT))
            .thenReturn(Pose())
        whenever(mockAnchorEntityImpl.getPose(JxrPlatformAdapter.Space.ACTIVITY)).thenReturn(Pose())
        whenever(mockActivityPanelEntity.getPose(JxrPlatformAdapter.Space.REAL_WORLD))
            .thenReturn(Pose())
        val pose = Pose.Identity

        assertThat(panelEntity.getPose()).isEqualTo(pose)
        assertThat(gltfModelEntity.getPose(Space.PARENT)).isEqualTo(pose)
        assertThat(anchorEntity.getPose(Space.ACTIVITY)).isEqualTo(pose)
        assertThat(activityPanelEntity.getPose(Space.REAL_WORLD)).isEqualTo(pose)

        verify(mockPanelEntityImpl).getPose(JxrPlatformAdapter.Space.PARENT)
        verify(mockGltfModelEntityImpl).getPose(JxrPlatformAdapter.Space.PARENT)
        verify(mockAnchorEntityImpl).getPose(JxrPlatformAdapter.Space.ACTIVITY)
        verify(mockActivityPanelEntity).getPose(JxrPlatformAdapter.Space.REAL_WORLD)
    }

    @Test
    fun allEntityGetActivitySpacePose_callsRuntimeEntityImplGetActivitySpacePose() {
        whenever(mockPanelEntityImpl.activitySpacePose).thenReturn(Pose())
        whenever(mockGltfModelEntityImpl.activitySpacePose).thenReturn(Pose())
        whenever(mockAnchorEntityImpl.activitySpacePose).thenReturn(Pose())
        whenever(mockActivityPanelEntity.activitySpacePose).thenReturn(Pose())
        val pose = Pose.Identity

        assertThat(panelEntity.getActivitySpacePose()).isEqualTo(pose)
        assertThat(gltfModelEntity.getActivitySpacePose()).isEqualTo(pose)
        assertThat(anchorEntity.getActivitySpacePose()).isEqualTo(pose)
        assertThat(activityPanelEntity.getActivitySpacePose()).isEqualTo(pose)

        verify(mockPanelEntityImpl).activitySpacePose
        verify(mockGltfModelEntityImpl).activitySpacePose
        verify(mockAnchorEntityImpl).activitySpacePose
        verify(mockActivityPanelEntity).activitySpacePose
    }

    @Test
    fun allEntitySetAlpha_callsRuntimeEntityImplSetAlpha() {
        val alpha = 0.1f

        panelEntity.setAlpha(alpha)
        gltfModelEntity.setAlpha(alpha, Space.PARENT)
        anchorEntity.setAlpha(alpha, Space.ACTIVITY)
        activityPanelEntity.setAlpha(alpha, Space.REAL_WORLD)
        contentlessEntity.setAlpha(alpha)
        activitySpace.setAlpha(alpha)

        verify(mockPanelEntityImpl).setAlpha(alpha, JxrPlatformAdapter.Space.PARENT)
        verify(mockGltfModelEntityImpl).setAlpha(alpha, JxrPlatformAdapter.Space.PARENT)
        verify(mockAnchorEntityImpl).setAlpha(alpha, JxrPlatformAdapter.Space.ACTIVITY)
        verify(mockActivityPanelEntity).setAlpha(alpha, JxrPlatformAdapter.Space.REAL_WORLD)
        verify(mockContentlessEntity).setAlpha(alpha, JxrPlatformAdapter.Space.PARENT)
        assertThat(testActivitySpace.setAlphaCalled).isTrue()
    }

    @Test
    fun allEntityGetAlpha_callsRuntimeEntityImplGetAlpha() {
        whenever(mockPanelEntityImpl.getAlpha(JxrPlatformAdapter.Space.PARENT)).thenReturn(0.1f)
        whenever(mockGltfModelEntityImpl.getAlpha(JxrPlatformAdapter.Space.PARENT)).thenReturn(0.1f)
        whenever(mockAnchorEntityImpl.getAlpha(JxrPlatformAdapter.Space.ACTIVITY)).thenReturn(0.1f)
        whenever(mockActivityPanelEntity.getAlpha(JxrPlatformAdapter.Space.REAL_WORLD))
            .thenReturn(0.1f)

        assertThat(panelEntity.getAlpha()).isEqualTo(0.1f)
        assertThat(gltfModelEntity.getAlpha(Space.PARENT)).isEqualTo(0.1f)
        assertThat(anchorEntity.getAlpha(Space.ACTIVITY)).isEqualTo(0.1f)
        assertThat(activityPanelEntity.getAlpha(Space.REAL_WORLD)).isEqualTo(0.1f)

        assertThat(activitySpace.getAlpha()).isEqualTo(1.0f)
        assertThat(testActivitySpace.getAlphaCalled).isTrue()

        verify(mockPanelEntityImpl).getAlpha(JxrPlatformAdapter.Space.PARENT)
        verify(mockGltfModelEntityImpl).getAlpha(JxrPlatformAdapter.Space.PARENT)
        verify(mockAnchorEntityImpl).getAlpha(JxrPlatformAdapter.Space.ACTIVITY)
        verify(mockActivityPanelEntity).getAlpha(JxrPlatformAdapter.Space.REAL_WORLD)
    }

    @Test
    fun allEntitySetHiddened_callsRuntimeEntityImplSetHidden() {
        panelEntity.setHidden(true)
        gltfModelEntity.setHidden(true)
        anchorEntity.setHidden(true)
        activityPanelEntity.setHidden(false)
        contentlessEntity.setHidden(false)
        activitySpace.setHidden(false)

        verify(mockPanelEntityImpl).setHidden(true)
        verify(mockGltfModelEntityImpl).setHidden(true)
        verify(mockAnchorEntityImpl).setHidden(true)
        verify(mockActivityPanelEntity).setHidden(false)
        verify(mockContentlessEntity).setHidden(false)
        assertThat(testActivitySpace.setHiddenCalled).isTrue()
    }

    @Test
    fun allEntityGetActivitySpaceAlpha_callsRuntimeEntityImplGetActivitySpaceAlpha() {
        whenever(mockPanelEntityImpl.getAlpha(JxrPlatformAdapter.Space.PARENT)).thenReturn(0.1f)
        whenever(mockGltfModelEntityImpl.getAlpha(JxrPlatformAdapter.Space.PARENT)).thenReturn(0.1f)
        whenever(mockAnchorEntityImpl.getAlpha(JxrPlatformAdapter.Space.ACTIVITY)).thenReturn(0.1f)
        whenever(mockActivityPanelEntity.getAlpha(JxrPlatformAdapter.Space.REAL_WORLD))
            .thenReturn(0.1f)

        assertThat(panelEntity.getAlpha(Space.PARENT)).isEqualTo(0.1f)
        assertThat(gltfModelEntity.getAlpha(Space.PARENT)).isEqualTo(0.1f)
        assertThat(anchorEntity.getAlpha(Space.ACTIVITY)).isEqualTo(0.1f)
        assertThat(activityPanelEntity.getAlpha(Space.REAL_WORLD)).isEqualTo(0.1f)

        assertThat(activitySpace.getAlpha(Space.PARENT)).isEqualTo(1.0f)

        verify(mockPanelEntityImpl).getAlpha(JxrPlatformAdapter.Space.PARENT)
        verify(mockGltfModelEntityImpl).getAlpha(JxrPlatformAdapter.Space.PARENT)
        verify(mockAnchorEntityImpl).getAlpha(JxrPlatformAdapter.Space.ACTIVITY)
        verify(mockActivityPanelEntity).getAlpha(JxrPlatformAdapter.Space.REAL_WORLD)
    }

    @Test
    fun allEntitySetScale_callsRuntimeEntityImplSetScale() {
        val scale = 0.1f

        panelEntity.setScale(scale)
        gltfModelEntity.setScale(scale, Space.PARENT)
        // Note that in production we expect this to raise an exception, but that should be handled
        // by the runtime Entity.
        anchorEntity.setScale(scale, Space.ACTIVITY)
        activityPanelEntity.setScale(scale, Space.REAL_WORLD)
        contentlessEntity.setScale(scale)
        // Note that in production we expect this to do nothing.
        activitySpace.setScale(scale)

        verify(mockPanelEntityImpl).setScale(any(), eq(JxrPlatformAdapter.Space.PARENT))
        verify(mockGltfModelEntityImpl).setScale(any(), eq(JxrPlatformAdapter.Space.PARENT))
        verify(mockAnchorEntityImpl).setScale(any(), eq(JxrPlatformAdapter.Space.ACTIVITY))
        verify(mockActivityPanelEntity).setScale(any(), eq(JxrPlatformAdapter.Space.REAL_WORLD))
        verify(mockContentlessEntity).setScale(any(), eq(JxrPlatformAdapter.Space.PARENT))
        assertThat(testActivitySpace.setScaleCalled).isTrue()
    }

    @Test
    fun allEntityGetScale_callsRuntimeEntityImplGetScale() {
        val runtimeScale = Vector3(0.1f, 0.1f, 0.1f)
        val sdkScale = 0.1f

        whenever(mockPanelEntityImpl.getScale(JxrPlatformAdapter.Space.PARENT))
            .thenReturn(runtimeScale)
        whenever(mockGltfModelEntityImpl.getScale(JxrPlatformAdapter.Space.PARENT))
            .thenReturn(runtimeScale)
        whenever(mockAnchorEntityImpl.getScale(JxrPlatformAdapter.Space.ACTIVITY))
            .thenReturn(runtimeScale)
        whenever(mockActivityPanelEntity.getScale(JxrPlatformAdapter.Space.REAL_WORLD))
            .thenReturn(runtimeScale)

        assertThat(panelEntity.getScale()).isEqualTo(sdkScale)
        assertThat(gltfModelEntity.getScale(Space.PARENT)).isEqualTo(sdkScale)
        assertThat(anchorEntity.getScale(Space.ACTIVITY)).isEqualTo(sdkScale)
        assertThat(activityPanelEntity.getScale(Space.REAL_WORLD)).isEqualTo(sdkScale)

        // This is unrealistic, but we want to make sure the SDK delegates to the runtimeImpl.
        assertThat(activitySpace.getScale()).isEqualTo(0f)
        assertThat(testActivitySpace.getScaleCalled).isTrue()

        verify(mockPanelEntityImpl).getScale(JxrPlatformAdapter.Space.PARENT)
        verify(mockGltfModelEntityImpl).getScale(JxrPlatformAdapter.Space.PARENT)
        verify(mockAnchorEntityImpl).getScale(JxrPlatformAdapter.Space.ACTIVITY)
        verify(mockActivityPanelEntity).getScale(JxrPlatformAdapter.Space.REAL_WORLD)
    }

    @Test
    fun allEntityTransformPoseTo_callsRuntimeEntityImplTransformPoseTo() {
        whenever(mockPanelEntityImpl.transformPoseTo(any(), any())).thenReturn(Pose())
        whenever(mockGltfModelEntityImpl.transformPoseTo(any(), any())).thenReturn(Pose())
        whenever(mockAnchorEntityImpl.transformPoseTo(any(), any())).thenReturn(Pose())
        whenever(mockActivityPanelEntity.transformPoseTo(any(), any())).thenReturn(Pose())
        whenever(mockContentlessEntity.transformPoseTo(any(), any())).thenReturn(Pose())
        val pose = Pose.Identity

        assertThat(panelEntity.transformPoseTo(pose, panelEntity)).isEqualTo(pose)
        assertThat(gltfModelEntity.transformPoseTo(pose, panelEntity)).isEqualTo(pose)
        assertThat(anchorEntity.transformPoseTo(pose, panelEntity)).isEqualTo(pose)
        assertThat(activityPanelEntity.transformPoseTo(pose, panelEntity)).isEqualTo(pose)
        assertThat(contentlessEntity.transformPoseTo(pose, panelEntity)).isEqualTo(pose)

        verify(mockPanelEntityImpl).transformPoseTo(any(), any())
        verify(mockGltfModelEntityImpl).transformPoseTo(any(), any())
        verify(mockAnchorEntityImpl).transformPoseTo(any(), any())
        verify(mockActivityPanelEntity).transformPoseTo(any(), any())
        verify(mockContentlessEntity).transformPoseTo(any(), any())
    }

    @Test
    fun allPanelEntitySetSizeInPixels_callsRuntimeEntityImplsetSizeInPixels() {
        val dimensions = PixelDimensions(320, 240)
        panelEntity.setSizeInPixels(dimensions)
        activityPanelEntity.setSizeInPixels(dimensions)

        verify(mockPanelEntityImpl).sizeInPixels = any()
        verify(mockActivityPanelEntity).setSizeInPixels(any())
    }

    @Test
    fun allPanelEntityGetSizeInPixels_callsRuntimeEntityImplgetSizeInPixels() {
        val pixelDimensions = JxrPlatformAdapter.PixelDimensions(320, 240)
        val expectedPixelDimensions = pixelDimensions.toPixelDimensions()

        whenever(mockPanelEntityImpl.sizeInPixels).thenReturn(pixelDimensions)
        whenever(mockActivityPanelEntity.sizeInPixels).thenReturn(pixelDimensions)

        assertThat(panelEntity.getSizeInPixels()).isEqualTo(expectedPixelDimensions)
        assertThat(activityPanelEntity.getSizeInPixels()).isEqualTo(expectedPixelDimensions)

        verify(mockPanelEntityImpl).sizeInPixels
        verify(mockActivityPanelEntity).sizeInPixels
    }

    @Test
    fun allEntityDispose_callsRuntimeEntityImplDispose() {
        gltfModelEntity.dispose()
        panelEntity.dispose()
        anchorEntity.dispose()
        activityPanelEntity.dispose()

        verify(mockGltfModelEntityImpl).dispose()
        verify(mockPanelEntityImpl).dispose()
        verify(mockAnchorEntityImpl).dispose()
        verify(mockActivityPanelEntity).dispose()
    }

    @Test
    fun activityPanelEntityLaunchActivity_callsImplLaunchActivity() {
        val launchIntent = Intent(activity.applicationContext, Activity::class.java)
        activityPanelEntity.launchActivity(launchIntent, null)

        verify(mockActivityPanelEntity).launchActivity(launchIntent, null)
    }

    @Test
    fun activityPanelEntityMoveActivity_callsImplMoveActivity() {
        activityPanelEntity.moveActivity(activity)

        verify(mockActivityPanelEntity).moveActivity(any())
    }

    @Test
    fun activitySpaceGetBounds_callsImplGetBounds() {
        val activitySpace = ActivitySpace.create(mockPlatformAdapter, entityManager)
        val bounds = activitySpace.getBounds()
        assertThat(bounds).isNotNull()
        assertThat(testActivitySpace.getBoundsCalled).isTrue()
    }

    @Test
    fun activitySpaceSetBoundsListener_receivesBoundsChangedCallback() {
        val activitySpace = ActivitySpace.create(mockPlatformAdapter, entityManager)
        var called = false
        val boundsChangedListener =
            Consumer<Dimensions> { newBounds ->
                assertThat(newBounds.width).isEqualTo(0.3f)
                assertThat(newBounds.height).isEqualTo(0.2f)
                assertThat(newBounds.depth).isEqualTo(0.1f)
                called = true
            }

        activitySpace.addBoundsChangedListener(directExecutor(), boundsChangedListener)
        testActivitySpace.sendBoundsChanged(JxrPlatformAdapter.Dimensions(0.3f, 0.2f, 0.1f))
        assertThat(called).isTrue()

        called = false
        activitySpace.removeBoundsChangedListener(boundsChangedListener)
        testActivitySpace.sendBoundsChanged(JxrPlatformAdapter.Dimensions(0.5f, 0.5f, 0.5f))
        assertThat(called).isFalse()
    }

    @Test
    fun setOnSpaceUpdatedListener_withNullParams_callsRuntimeSetOnSpaceUpdatedListener() {
        val mockRtActivitySpace = mock<JxrPlatformAdapter.ActivitySpace>()
        whenever(mockPlatformAdapter.activitySpace).thenReturn(mockRtActivitySpace)
        val activitySpace = ActivitySpace.create(mockPlatformAdapter, entityManager)

        activitySpace.setOnSpaceUpdatedListener(null)
        verify(mockRtActivitySpace).setOnSpaceUpdatedListener(null, null)
    }

    @Test
    fun setOnSpaceUpdatedListener_receivesRuntimeSetOnSpaceUpdatedListenerCallbacks() {
        val mockRtActivitySpace = mock<JxrPlatformAdapter.ActivitySpace>()
        whenever(mockPlatformAdapter.activitySpace).thenReturn(mockRtActivitySpace)
        val activitySpace = ActivitySpace.create(mockPlatformAdapter, entityManager)

        var listenerCalled = false
        val captor = argumentCaptor<JxrPlatformAdapter.SystemSpaceEntity.OnSpaceUpdatedListener>()
        activitySpace.setOnSpaceUpdatedListener({ listenerCalled = true }, directExecutor())
        verify(mockRtActivitySpace).setOnSpaceUpdatedListener(captor.capture(), any())
        captor.firstValue.onSpaceUpdated()
        assertThat(listenerCalled).isTrue()
    }

    @Test
    fun setOnSpaceUpdatedListener_anchorEntity_withNullParams_callsRuntimeSetOnSpaceUpdatedListener() {
        anchorEntity.setOnSpaceUpdatedListener(null)
        verify(mockAnchorEntityImpl).setOnSpaceUpdatedListener(null, null)
    }

    @Test
    fun setOnSpaceUpdatedListener_anchorEntity_receivesRuntimeSetOnSpaceUpdatedListenerCallbacks() {
        var listenerCalled = false
        val captor = argumentCaptor<JxrPlatformAdapter.SystemSpaceEntity.OnSpaceUpdatedListener>()
        anchorEntity.setOnSpaceUpdatedListener({ listenerCalled = true }, directExecutor())

        verify(mockAnchorEntityImpl).setOnSpaceUpdatedListener(captor.capture(), any())
        assertThat(listenerCalled).isFalse()
        captor.firstValue.onSpaceUpdated()
        assertThat(listenerCalled).isTrue()
    }

    @Test
    fun mainPanelEntity_isMainPanelEntity() {
        val mainPanelEntity = session.mainPanelEntity
        assertThat(mainPanelEntity.isMainPanelEntity).isTrue()
    }

    @Test
    fun mainPanelEntity_isSingleton() {
        val mainPanelEntity = session.mainPanelEntity
        val mainPanelEntity2 = session.mainPanelEntity

        assertThat(mainPanelEntity2).isSameInstanceAs(mainPanelEntity)
        verify(mockPlatformAdapter, times(1)).mainPanelEntity
    }

    @Test
    fun contentlessEntity_isCreated() {
        val entity = ContentlessEntity.create(session, "test")
        assertThat(entity).isNotNull()
    }

    @Test
    fun contentlessEntity_canSetPose() {
        val entity = ContentlessEntity.create(session, "test")
        val setPose = Pose.Identity
        entity.setPose(setPose)

        val captor = argumentCaptor<Pose>()
        verify(mockContentlessEntity).setPose(captor.capture(), eq(JxrPlatformAdapter.Space.PARENT))

        val pose = captor.firstValue
        assertThat(pose.translation.x).isEqualTo(setPose.translation.x)
        assertThat(pose.translation.y).isEqualTo(setPose.translation.y)
        assertThat(pose.translation.z).isEqualTo(setPose.translation.z)
    }

    @Test
    fun contentlessEntity_canGetPose() {
        whenever(mockContentlessEntity.getPose(JxrPlatformAdapter.Space.PARENT)).thenReturn(Pose())

        val entity = ContentlessEntity.create(session, "test")
        val pose = Pose.Identity

        assertThat(entity.getPose()).isEqualTo(pose)
        verify(mockContentlessEntity).getPose(JxrPlatformAdapter.Space.PARENT)
    }

    @Test
    fun contentlessEntity_canGetActivitySpacePose() {
        whenever(mockContentlessEntity.activitySpacePose).thenReturn(Pose())

        val entity = ContentlessEntity.create(session, "test")
        val pose = Pose.Identity

        assertThat(entity.getActivitySpacePose()).isEqualTo(pose)
        verify(mockContentlessEntity).activitySpacePose
    }

    @Test
    fun allEntity_addComponentInvokesOnAttach() {
        val component = mock<Component>()
        whenever(component.onAttach(any())).thenReturn(true)

        assertThat(panelEntity.addComponent(component)).isTrue()
        verify(component).onAttach(panelEntity)

        assertThat(gltfModelEntity.addComponent(component)).isTrue()
        verify(component).onAttach(gltfModelEntity)

        assertThat(anchorEntity.addComponent(component)).isTrue()
        verify(component).onAttach(anchorEntity)

        assertThat(activityPanelEntity.addComponent(component)).isTrue()
        verify(component).onAttach(activityPanelEntity)
    }

    @Test
    fun allEntity_addComponentFailsIfOnAttachFails() {
        val component = mock<Component>()
        whenever(component.onAttach(any())).thenReturn(false)

        assertThat(panelEntity.addComponent(component)).isFalse()
        verify(component).onAttach(panelEntity)

        assertThat(gltfModelEntity.addComponent(component)).isFalse()
        verify(component).onAttach(gltfModelEntity)

        assertThat(anchorEntity.addComponent(component)).isFalse()
        verify(component).onAttach(anchorEntity)

        assertThat(activityPanelEntity.addComponent(component)).isFalse()
        verify(component).onAttach(activityPanelEntity)
    }

    @Test
    fun allEntity_removeComponentInvokesOnDetach() {
        val component = mock<Component>()
        whenever(component.onAttach(any())).thenReturn(true)

        assertThat(panelEntity.addComponent(component)).isTrue()
        verify(component).onAttach(panelEntity)

        assertThat(gltfModelEntity.addComponent(component)).isTrue()
        verify(component).onAttach(gltfModelEntity)

        assertThat(anchorEntity.addComponent(component)).isTrue()
        verify(component).onAttach(anchorEntity)

        assertThat(activityPanelEntity.addComponent(component)).isTrue()
        verify(component).onAttach(activityPanelEntity)

        panelEntity.removeComponent(component)
        verify(component).onDetach(panelEntity)

        gltfModelEntity.removeComponent(component)
        verify(component).onDetach(gltfModelEntity)

        anchorEntity.removeComponent(component)
        verify(component).onDetach(anchorEntity)

        activityPanelEntity.removeComponent(component)
        verify(component).onDetach(activityPanelEntity)
    }

    @Test
    fun allEntity_addingSameComponentTypeAgainSucceeds() {
        val component1 = mock<Component>()
        val component2 = mock<Component>()
        whenever(component1.onAttach(any())).thenReturn(true)
        whenever(component2.onAttach(any())).thenReturn(true)

        assertThat(panelEntity.addComponent(component1)).isTrue()
        assertThat(panelEntity.addComponent(component2)).isTrue()
        verify(component1).onAttach(panelEntity)
        verify(component2).onAttach(panelEntity)

        assertThat(gltfModelEntity.addComponent(component1)).isTrue()
        assertThat(gltfModelEntity.addComponent(component2)).isTrue()
        verify(component1).onAttach(gltfModelEntity)
        verify(component2).onAttach(panelEntity)

        assertThat(anchorEntity.addComponent(component1)).isTrue()
        assertThat(anchorEntity.addComponent(component2)).isTrue()
        verify(component1).onAttach(anchorEntity)
        verify(component2).onAttach(panelEntity)

        assertThat(activityPanelEntity.addComponent(component1)).isTrue()
        assertThat(activityPanelEntity.addComponent(component2)).isTrue()
        verify(component1).onAttach(activityPanelEntity)
        verify(component2).onAttach(panelEntity)
    }

    @Test
    fun allEntity_addDifferentComponentTypesInvokesOnAttachOnAll() {
        val component1 = mock<Component>()
        val component2 = mock<FakeComponent>()
        whenever(component1.onAttach(any())).thenReturn(true)
        whenever(component2.onAttach(any())).thenReturn(true)

        assertThat(panelEntity.addComponent(component1)).isTrue()
        assertThat(panelEntity.addComponent(component2)).isTrue()
        verify(component1).onAttach(panelEntity)
        verify(component2).onAttach(panelEntity)

        assertThat(gltfModelEntity.addComponent(component1)).isTrue()
        assertThat(gltfModelEntity.addComponent(component2)).isTrue()
        verify(component1).onAttach(gltfModelEntity)
        verify(component2).onAttach(gltfModelEntity)

        assertThat(anchorEntity.addComponent(component1)).isTrue()
        assertThat(anchorEntity.addComponent(component2)).isTrue()
        verify(component1).onAttach(anchorEntity)
        verify(component2).onAttach(anchorEntity)

        assertThat(activityPanelEntity.addComponent(component1)).isTrue()
        assertThat(activityPanelEntity.addComponent(component2)).isTrue()
        verify(component1).onAttach(activityPanelEntity)
        verify(component2).onAttach(activityPanelEntity)
    }

    @Test
    fun allEntity_removeAllComponentsInvokesOnDetachOnAll() {
        val component1 = mock<Component>()
        val component2 = mock<FakeComponent>()
        whenever(component1.onAttach(any())).thenReturn(true)
        whenever(component2.onAttach(any())).thenReturn(true)

        assertThat(panelEntity.addComponent(component1)).isTrue()
        assertThat(panelEntity.addComponent(component2)).isTrue()
        verify(component1).onAttach(panelEntity)
        verify(component2).onAttach(panelEntity)

        assertThat(gltfModelEntity.addComponent(component1)).isTrue()
        assertThat(gltfModelEntity.addComponent(component2)).isTrue()
        verify(component1).onAttach(gltfModelEntity)
        verify(component2).onAttach(gltfModelEntity)

        assertThat(anchorEntity.addComponent(component1)).isTrue()
        assertThat(anchorEntity.addComponent(component2)).isTrue()
        verify(component1).onAttach(anchorEntity)
        verify(component2).onAttach(anchorEntity)

        assertThat(activityPanelEntity.addComponent(component1)).isTrue()
        assertThat(activityPanelEntity.addComponent(component2)).isTrue()
        verify(component1).onAttach(activityPanelEntity)
        verify(component2).onAttach(activityPanelEntity)

        panelEntity.removeAllComponents()
        verify(component1).onDetach(panelEntity)
        verify(component2).onDetach(panelEntity)

        gltfModelEntity.removeAllComponents()
        verify(component1).onDetach(gltfModelEntity)
        verify(component2).onDetach(gltfModelEntity)

        anchorEntity.removeAllComponents()
        verify(component1).onDetach(anchorEntity)
        verify(component2).onDetach(anchorEntity)

        activityPanelEntity.removeAllComponents()
        verify(component1).onDetach(activityPanelEntity)
        verify(component2).onDetach(activityPanelEntity)
    }

    @Test
    fun allEntity_addSameComponentMultipleTimesInvokesOnAttachMutipleTimes() {
        val component = mock<Component>()
        whenever(component.onAttach(any())).thenReturn(true)

        assertThat(panelEntity.addComponent(component)).isTrue()
        assertThat(panelEntity.addComponent(component)).isTrue()
        verify(component, times(2)).onAttach(panelEntity)

        assertThat(gltfModelEntity.addComponent(component)).isTrue()
        assertThat(gltfModelEntity.addComponent(component)).isTrue()
        verify(component, times(2)).onAttach(gltfModelEntity)

        assertThat(anchorEntity.addComponent(component)).isTrue()
        assertThat(anchorEntity.addComponent(component)).isTrue()
        verify(component, times(2)).onAttach(anchorEntity)

        assertThat(activityPanelEntity.addComponent(component)).isTrue()
        assertThat(activityPanelEntity.addComponent(component)).isTrue()
        verify(component, times(2)).onAttach(activityPanelEntity)
    }

    @Test
    fun allEntity_removeSameComponentMultipleTimesInvokesOnDetachOnce() {
        val component = mock<Component>()
        whenever(component.onAttach(any())).thenReturn(true)

        assertThat(panelEntity.addComponent(component)).isTrue()
        verify(component).onAttach(panelEntity)

        panelEntity.removeComponent(component)
        panelEntity.removeComponent(component)
        verify(component).onDetach(panelEntity)

        assertThat(gltfModelEntity.addComponent(component)).isTrue()
        verify(component).onAttach(gltfModelEntity)

        gltfModelEntity.removeComponent(component)
        gltfModelEntity.removeComponent(component)
        verify(component).onDetach(gltfModelEntity)

        assertThat(anchorEntity.addComponent(component)).isTrue()
        verify(component).onAttach(anchorEntity)

        anchorEntity.removeComponent(component)
        anchorEntity.removeComponent(component)
        verify(component).onDetach(anchorEntity)

        assertThat(activityPanelEntity.addComponent(component)).isTrue()
        verify(component).onAttach(activityPanelEntity)

        activityPanelEntity.removeComponent(component)
        activityPanelEntity.removeComponent(component)
        verify(component).onDetach(activityPanelEntity)
    }

    @Test
    fun anchorEntity_canGetDefaultPersistState() {
        assertThat(anchorEntity.getPersistState())
            .isEqualTo(AnchorEntity.PersistState.PERSIST_NOT_REQUESTED)
    }

    @Test
    fun anchorEntity_createPersistAnchorSuccess() {
        whenever(mockPlatformAdapter.createPersistedAnchorEntity(any(), any()))
            .thenReturn(mockAnchorEntityImpl)
        val persistAnchorEntity =
            AnchorEntity.create(mockPlatformAdapter, entityManager, UUID.randomUUID())
        assertThat(persistAnchorEntity).isNotNull()
    }

    @Test
    fun allEntity_disposeRemovesAllComponents() {
        val component = mock<Component>()
        whenever(component.onAttach(any())).thenReturn(true)

        assertThat(panelEntity.addComponent(component)).isTrue()
        verify(component).onAttach(panelEntity)

        panelEntity.dispose()
        verify(component).onDetach(panelEntity)

        assertThat(gltfModelEntity.addComponent(component)).isTrue()
        verify(component).onAttach(gltfModelEntity)

        gltfModelEntity.dispose()
        verify(component).onDetach(gltfModelEntity)

        assertThat(anchorEntity.addComponent(component)).isTrue()
        verify(component).onAttach(anchorEntity)

        anchorEntity.dispose()
        verify(component).onDetach(anchorEntity)

        assertThat(activityPanelEntity.addComponent(component)).isTrue()
        verify(component).onAttach(activityPanelEntity)

        activityPanelEntity.dispose()
        verify(component).onDetach(activityPanelEntity)
    }

    @Test
    fun allEntity_getComponentsReturnsAttachedComponents() {
        val component1 = mock<Component>()
        val component2 = mock<FakeComponent>()
        whenever(component1.onAttach(any())).thenReturn(true)
        whenever(component2.onAttach(any())).thenReturn(true)

        assertThat(panelEntity.addComponent(component1)).isTrue()
        assertThat(panelEntity.addComponent(component2)).isTrue()
        verify(component1).onAttach(panelEntity)
        verify(component2).onAttach(panelEntity)
        assertThat(panelEntity.getComponents()).containsExactly(component1, component2)

        assertThat(gltfModelEntity.addComponent(component1)).isTrue()
        assertThat(gltfModelEntity.addComponent(component2)).isTrue()
        verify(component1).onAttach(gltfModelEntity)
        verify(component2).onAttach(gltfModelEntity)
        assertThat(gltfModelEntity.getComponents()).containsExactly(component1, component2)

        assertThat(anchorEntity.addComponent(component1)).isTrue()
        assertThat(anchorEntity.addComponent(component2)).isTrue()
        verify(component1).onAttach(anchorEntity)
        verify(component2).onAttach(anchorEntity)
        assertThat(anchorEntity.getComponents()).containsExactly(component1, component2)

        assertThat(activityPanelEntity.addComponent(component1)).isTrue()
        assertThat(activityPanelEntity.addComponent(component2)).isTrue()
        verify(component1).onAttach(activityPanelEntity)
        verify(component2).onAttach(activityPanelEntity)
        assertThat(activityPanelEntity.getComponents()).containsExactly(component1, component2)
    }

    @Test
    fun getComponentsOfType_returnsAttachedComponents() {
        val component1 = mock<Component>()
        val component2 = mock<FakeComponent>()
        val component3 = mock<SubtypeFakeComponent>()
        whenever(component1.onAttach(any())).thenReturn(true)
        whenever(component2.onAttach(any())).thenReturn(true)
        whenever(component3.onAttach(any())).thenReturn(true)

        assertThat(panelEntity.addComponent(component1)).isTrue()
        assertThat(panelEntity.addComponent(component2)).isTrue()
        assertThat(panelEntity.addComponent(component3)).isTrue()
        verify(component1).onAttach(panelEntity)
        verify(component2).onAttach(panelEntity)
        verify(component3).onAttach(panelEntity)
        assertThat(panelEntity.getComponentsOfType(FakeComponent::class.java))
            .containsExactly(component2, component3)
    }

    @Test
    fun SurfaceEntity_redirectsCallsToRtEntity() {
        surfaceEntity.stereoMode = SurfaceEntity.StereoMode.TOP_BOTTOM
        verify(mockSurfaceEntity).setStereoMode(SurfaceEntity.StereoMode.TOP_BOTTOM)

        @Suppress("UNUSED_VARIABLE") var unusedMode = surfaceEntity.stereoMode
        verify(mockSurfaceEntity).getStereoMode()

        surfaceEntity.canvasShape = SurfaceEntity.CanvasShape.Vr360Sphere(1.0f)
        verify(mockSurfaceEntity).setCanvasShape(any())

        // no equivalent test for getter - that just returns the Kotlin object for now.
    }

    @Test
    fun setCornerRadius() {
        val radius = 2.0f
        panelEntity.setCornerRadius(radius)
        verify(mockPanelEntityImpl).cornerRadius = radius
    }
}
