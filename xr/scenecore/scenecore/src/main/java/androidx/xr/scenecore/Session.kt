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

@file:Suppress("BanConcurrentHashMap", "Deprecation")

package androidx.xr.scenecore

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import androidx.xr.arcore.Anchor
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.JxrPlatformAdapter.Entity as RtEntity
import androidx.xr.scenecore.JxrPlatformAdapter.SpatialCapabilities as RtSpatialCapabilities
import androidx.xr.scenecore.SpatialCapabilities.SpatialCapability
import androidx.xr.scenecore.impl.JxrPlatformAdapterAxr
import com.google.common.util.concurrent.ListenableFuture
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.function.Consumer

/**
 * The Session provides the primary interface to SceneCore functionality for the application. Each
 * spatialized Activity must create and hold an instance of Session.
 *
 * Once created, the application can use the Session interfaces to create spatialized entities, such
 * as Widget panels and geometric models, set the background environment, and anchor content to the
 * real world.
 */
// TODO: Make this class thread safe.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class Session(
    public val activity: Activity,
    public val platformAdapter: JxrPlatformAdapter,
    public val spatialEnvironment: SpatialEnvironment,
) {
    internal val entityManager by lazy { EntityManager() }

    private val spatialCapabilitiesListeners:
        ConcurrentMap<Consumer<SpatialCapabilities>, Consumer<RtSpatialCapabilities>> =
        ConcurrentHashMap()

    /**
     * The ActivitySpace is a special entity that represents the space in which the application is
     * launched. It is the default parent of all entities in the scene.
     *
     * The ActivitySpace is created automatically when the Session is created.
     */
    public val activitySpace: ActivitySpace = ActivitySpace.create(platformAdapter, entityManager)

    /** The SpatialUser contains information about the user. */
    public val spatialUser: SpatialUser = SpatialUser.create(platformAdapter)

    /**
     * A spatialized PanelEntity associated with the "main window" for the Activity. When in
     * HomeSpace mode, this is the application's "main window".
     *
     * If called multiple times, this will return the same PanelEntity.
     */
    public val mainPanelEntity: PanelEntity =
        PanelEntity.createMainPanelEntity(platformAdapter, entityManager)

    /**
     * The PerceptionSpace represents the origin of the space in which the ARCore for XR API
     * provides tracking info. The transformations provided by the PerceptionSpace are only valid
     * for the call frame, as the transformation can be changed by the system at any time.
     */
    public val perceptionSpace: PerceptionSpace = PerceptionSpace.create(platformAdapter)

    // TODO: 378706624 - Remove this method once we have a better way to handle the root entity.
    public val activitySpaceRoot: Entity by lazy {
        entityManager.getEntityForRtEntity(platformAdapter.activitySpaceRootImpl)!!
    }

    public companion object {
        private const val TAG = "Session"
        private val activitySessionMap = ConcurrentHashMap<Activity, Session>()

        // TODO: b/323060217 - Move the platformAdapter behind a loader class that loads it in.
        /**
         * Creates a session and pairs it with an Activity and its lifecycle. If a session is
         * already paired with an Activity, return that Session instead of creating a new one.
         *
         * For our Alpha release, we just directly instantiate the Android XR PlatformAdapter.
         */
        // TODO(b/326748782): Change the returned Session here to be nullable or asynchronous.
        @JvmStatic
        @JvmOverloads
        public fun create(
            activity: Activity,
            platformAdapter: JxrPlatformAdapter? = null
        ): Session {
            // TODO(bhavsar): Rethink moving this check when integration with Spatial Activity
            // happens.
            if (
                !PermissionHelper.hasPermission(
                    activity,
                    PermissionHelper.SCENE_UNDERSTANDING_PERMISSION
                )
            ) {
                PermissionHelper.requestPermission(
                    activity,
                    PermissionHelper.SCENE_UNDERSTANDING_PERMISSION,
                    PermissionHelper.SCENE_UNDERSTANDING_PERMISSION_CODE,
                )
            }
            return activitySessionMap.computeIfAbsent(activity) {
                Log.i(TAG, "Creating session for activity $activity")
                val session =
                    when (platformAdapter) {
                        null -> {
                            val platformAdapterImpl =
                                JxrPlatformAdapterAxr.create(
                                    activity,
                                    Executors.newSingleThreadScheduledExecutor(
                                        object : ThreadFactory {
                                            override fun newThread(r: Runnable): Thread {
                                                return Thread(r, "JXRCoreSession")
                                            }
                                        }
                                    ),
                                )
                            Session(
                                activity,
                                platformAdapterImpl,
                                SpatialEnvironment(platformAdapterImpl)
                            )
                        }
                        else ->
                            Session(activity, platformAdapter, SpatialEnvironment(platformAdapter))
                    }
                activity.registerActivityLifecycleCallbacks(
                    object : ActivityLifecycleCallbacks {
                        override fun onActivityCreated(
                            activity: Activity,
                            savedInstanceState: Bundle?
                        ) {}

                        override fun onActivityStarted(activity: Activity) {}

                        override fun onActivityResumed(activity: Activity) {
                            session.platformAdapter.startRenderer()
                        }

                        override fun onActivityPaused(activity: Activity) {
                            session.platformAdapter.stopRenderer()
                        }

                        override fun onActivityStopped(activity: Activity) {}

                        override fun onActivitySaveInstanceState(
                            activity: Activity,
                            outState: Bundle
                        ) {}

                        override fun onActivityDestroyed(activity: Activity) {
                            activitySessionMap.remove(activity)
                            session.entityManager.clear()
                            session.platformAdapter.dispose()
                        }
                    }
                )
                session
            }
        }
    }

    /**
     * Returns true if the Session is currently capable of the given [SpatialCapability], false
     * otherwise. The available set of capabilities can change within a session.
     */
    @Deprecated(message = "Removing in favor of getSpatialCapabilities().hasCapability()")
    // TODO: b/366214441 - Remove this method
    public fun hasSpatialCapability(@SpatialCapability capability: Int): Boolean =
        getSpatialCapabilities().hasCapability(capability)

    /**
     * Returns the current [SpatialCapabilities] of the Session. The set of capabilities can change
     * within a session. The returned object will not update if the capabilities change; this method
     * should be called again to get the latest set of capabilities.
     */
    public fun getSpatialCapabilities(): SpatialCapabilities =
        platformAdapter.spatialCapabilities.toSpatialCapabilities()

    /**
     * Adds the given [Consumer] as a listener to be invoked when this Session's current
     * [SpatialCapabilities] change. [Consumer#accept(SpatialCapabilities)] will be invoked on the
     * main thread.
     */
    public fun addSpatialCapabilitiesChangedListener(
        listener: Consumer<SpatialCapabilities>
    ): Unit = addSpatialCapabilitiesChangedListener(HandlerExecutor.mainThreadExecutor, listener)

    /**
     * Adds the given [Consumer] as a listener to be invoked when this Session's current
     * [SpatialCapabilities] change. [Consumer#accept(SpatialCapabilities)] will be invoked on the
     * given callbackExecutor, or the main thread if the callbackExecutor is null (default).
     */
    public fun addSpatialCapabilitiesChangedListener(
        callbackExecutor: Executor,
        listener: Consumer<SpatialCapabilities>,
    ): Unit {
        // wrap the client's listener in a callback that receives & converts the platformAdapter
        // SpatialCapabilities type.
        val rtListener: Consumer<RtSpatialCapabilities> =
            Consumer<RtSpatialCapabilities> { rtCaps: RtSpatialCapabilities ->
                listener.accept(rtCaps.toSpatialCapabilities())
            }
        spatialCapabilitiesListeners.compute(
            listener,
            { _, _ ->
                platformAdapter.addSpatialCapabilitiesChangedListener(callbackExecutor, rtListener)
                rtListener
            },
        )
    }

    /**
     * Releases the given [Consumer] from receiving updates when the Session's [SpatialCapabilities]
     * change.
     */
    @Suppress("PairedRegistration") // The corresponding remove method does not accept an Executor
    public fun removeSpatialCapabilitiesChangedListener(
        listener: Consumer<SpatialCapabilities>
    ): Unit {
        spatialCapabilitiesListeners.computeIfPresent(
            listener,
            { _, rtListener ->
                platformAdapter.removeSpatialCapabilitiesChangedListener(rtListener)
                null
            },
        )
    }

    /**
     * Public factory function for a [GltfModel], where the glTF is asynchronously loaded.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * Currently, only URLs and relative paths from the android_assets/ directory are supported.
     * Currently, only binary glTF (.glb) files are supported.
     *
     * @param name The URL or asset-relative path of a binary glTF (.glb) model to be loaded
     * @return a ListenableFuture<GltfModel>. Listeners will be called on the main thread if
     *   Runnable::run is supplied.
     */
    @MainThread
    public fun createGltfResourceAsync(name: String): ListenableFuture<GltfModel> {
        return GltfModel.createAsync(platformAdapter, name)
    }

    /**
     * Public factory function for an EXRImage, where the EXR is loaded from a local file.
     *
     * @param name The path for an EXR image to be loaded
     * @return an EXRImage instance.
     */
    public fun createExrImageResource(name: String): ExrImage =
        ExrImage.create(platformAdapter, name)

    /**
     * Public factory function for a [GltfModelEntity].
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * @param model The [GltfModel] this Entity is referencing.
     * @param pose The initial pose of the entity.
     * @return a GltfModelEntity instance
     */
    // TODO: b/341372472 - Rename createGltfEntity to createGltfModelEntity
    @JvmOverloads
    @MainThread
    public fun createGltfEntity(model: GltfModel, pose: Pose = Pose.Identity): GltfModelEntity =
        GltfModelEntity.create(platformAdapter, entityManager, model, pose)

    // TODO(b/352629832): Update surfaceDimensionsPx to be a PixelDimensions
    /**
     * Public factory function for a spatialized PanelEntity.
     *
     * @param view View to embed in this panel entity.
     * @param surfaceDimensionsPx Dimensions for the underlying surface for the given view.
     * @param dimensions Dimensions for the panel in meters.
     * @param name Name of the panel.
     * @param pose Pose of this entity relative to its parent, default value is Identity.
     * @return a PanelEntity instance.
     */
    @JvmOverloads
    public fun createPanelEntity(
        view: View,
        surfaceDimensionsPx: Dimensions,
        dimensions: Dimensions,
        name: String,
        pose: Pose = Pose.Identity,
    ): PanelEntity =
        PanelEntity.create(
            platformAdapter,
            entityManager,
            view,
            surfaceDimensionsPx,
            dimensions,
            name,
            activity,
            pose,
        )

    /** Helper function to query if given activity can be a host to ActivityPanel. */
    @Deprecated(
        message = "Use getSpatialCapabilities.hasCapabilility(SPATIAL_CAPABILITY_EMBED_ACTIVITY)"
    )
    public fun canEmbedActivityPanel(@Suppress("UNUSED_PARAMETER") activity: Activity): Boolean =
        getSpatialCapabilities()
            .hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_EMBED_ACTIVITY)

    // TODO(b/352629832): Update windowBoundsPx to be a PixelDimensions
    /**
     * Public factory function for a spatial ActivityPanelEntity.
     *
     * @param windowBoundsPx Bounds for the panel window in pixels.
     * @param name Name of the panel.
     * @param pose Pose of this entity relative to its parent, default value is Identity.
     * @return an ActivityPanelEntity instance.
     */
    @JvmOverloads
    public fun createActivityPanelEntity(
        windowBoundsPx: Rect,
        name: String,
        pose: Pose = Pose.Identity,
    ): ActivityPanelEntity =
        ActivityPanelEntity.create(
            platformAdapter,
            entityManager,
            PixelDimensions(windowBoundsPx.width(), windowBoundsPx.height()),
            name,
            activity,
            pose,
        )

    /**
     * Public factory function for an AnchorEntity which searches for a location to create an Anchor
     * among the tracked planes available to the perception system.
     *
     * Note that this function will fail if the application has not been granted the
     * "android.permission.SCENE_UNDERSTANDING" permission. Consider using PermissionHelper to help
     * request permission from the User.
     *
     * @param bounds Bounds for this AnchorEntity.
     * @param planeType Orientation of plane to which this Anchor should attach.
     * @param planeSemantic Semantics of the plane to which this Anchor should attach.
     * @param timeout The amount of time as a [Duration] to search for the a suitable plane to
     *   attach to. If a plane is not found within the timeout, the returned AnchorEntity state will
     *   be set to AnchorEntity.State.TIMEDOUT. It may take longer than the timeout period before
     *   the anchor state is updated. If the timeout duration is zero it will search for the anchor
     *   indefinitely.
     */
    @JvmOverloads
    public fun createAnchorEntity(
        bounds: Dimensions,
        planeType: @PlaneTypeValue Int,
        planeSemantic: @PlaneSemanticValue Int,
        timeout: Duration = Duration.ZERO,
    ): AnchorEntity {
        return AnchorEntity.create(
            platformAdapter,
            entityManager,
            bounds,
            planeType,
            planeSemantic,
            timeout,
        )
    }

    /**
     * Public factory function for an AnchorEntity which uses an Anchor from ARCore for XR.
     *
     * @param anchor The PerceptionAnchor to use for this AnchorEntity.
     */
    public fun createAnchorEntity(anchor: Anchor): AnchorEntity {
        return AnchorEntity.create(platformAdapter, entityManager, anchor)
    }

    /**
     * Unpersists an anchor. It will clean up the data in the storage that is required to retrieve
     * the anchor.
     *
     * @param uuid UUID of the anchor to unpersist.
     */
    public fun unpersistAnchor(uuid: UUID): Boolean {
        return platformAdapter.unpersistAnchor(uuid)
    }

    /**
     * Public factory function for creating a content-less entity. This entity is used as a
     * connection point for attaching children entities and managing them (i.e. setPose()) as a
     * group.
     *
     * @param name Name of the entity.
     * @param pose Initial pose of the entity.
     */
    @JvmOverloads
    public fun createEntity(name: String, pose: Pose = Pose.Identity): Entity =
        ContentlessEntity.create(platformAdapter, entityManager, name, pose)

    /**
     * Public factory function for a persisted AnchorEntity using UUID. Note that the system keeps a
     * limited number of scenes and anchors. If the anchor is pruned due to the system limitation,
     * the creation will fail. It will return null if there is a failure.
     *
     * @param uuid The UUID of the persisted anchor to recreate.
     * @return a persisted AnchorEntity instance.
     */
    public fun createPersistedAnchorEntity(uuid: UUID): AnchorEntity {
        return AnchorEntity.create(platformAdapter, entityManager, uuid)
    }

    /**
     * Sets the full space mode flag to the given [android.os.Bundle].
     *
     * The [android.os.Bundle] then could be used to launch an [android.app.Activity] with
     * requesting to enter full space mode through [android.app.Activity.startActivity]. If there's
     * a bundle used for customizing how the [android.app.Activity] should be started by
     * [android.app.ActivityOptions.toBundle] or [androidx.core.app.ActivityOptionsCompat.toBundle],
     * it's suggested to use the bundle to call this method.
     *
     * The flag will be ignored when no [android.content.Intent.FLAG_ACTIVITY_NEW_TASK] is set in
     * the bundle, or it is not started from a focused Activity context.
     *
     * This flag is also ignored when the [android.window.PROPERTY_XR_ACTIVITY_START_MODE] property
     * is set to a value other than [XR_ACTIVITY_START_MODE_UNDEFINED] in the AndroidManifest.xml
     * file for the activity being launched.
     *
     * @param bundle the input bundle to set with the full space mode flag.
     * @return the input bundle with the full space mode flag set.
     */
    public fun setFullSpaceMode(bundle: Bundle): Bundle = platformAdapter.setFullSpaceMode(bundle)

    /**
     * Sets the inherit full space mode environvment flag to the given [android.os.Bundle].
     *
     * The [android.os.Bundle] then could be used to launch an [android.app.Activity] with
     * requesting to enter full space mode while inherit the existing environment through
     * [android.app.Activity.startActivity]. If there's a bundle used for customizing how the
     * [android.app.Activity] should be started by [android.app.ActivityOptions.toBundle] or
     * [androidx.core.app.ActivityOptionsCompat.toBundle], it's suggested to use the bundle to call
     * this method.
     *
     * When launched, the activity will be in full space mode and also inherits the environment from
     * the launching activity. If the inherited environment needs to be animated, the launching
     * activity has to continue updating the environment even after the activity is put into the
     * stopped state.
     *
     * The flag will be ignored when no [android.content.Intent.FLAG_ACTIVITY_NEW_TASK] is set in
     * the intent, or it is not started from a focused Activity context.
     *
     * The flag will also be ignored when there is no environment to inherit or the activity has its
     * own environment set already.
     *
     * This flag is ignored too when the [android.window.PROPERTY_XR_ACTIVITY_START_MODE] property
     * is set to a value other than [XR_ACTIVITY_START_MODE_UNDEFINED] in the AndroidManifest.xml
     * file for the activity being launched.
     *
     * For security reasons, Z testing for the new activity is disabled, and the activity is always
     * drawn on top of the inherited environment. Because Z testing is disabled, the activity should
     * not spatialize itself.
     *
     * @param bundle the input bundle to set with the inherit full space mode environment flag.
     * @return the input bundle with the inherit full space mode flag set.
     */
    public fun setFullSpaceModeWithEnvironmentInherited(bundle: Bundle): Bundle =
        platformAdapter.setFullSpaceModeWithEnvironmentInherited(bundle)

    /**
     * Sets a preferred main panel aspect ratio for home space mode.
     *
     * The ratio is only applied to the activity. If the activity launches another activity in the
     * same task, the ratio is not applied to the new activity. Also, while the activity is in full
     * space mode, the preference is temporarily removed.
     *
     * If the activity's current aspect ratio differs from the preferredRatio, the panel is
     * automatically resized. This resizing preserves the panel's area. To avoid runtime resizing,
     * consider specifying the desired aspect ratio in your AndroidManifest.xml. This ensures your
     * activity launches with the preferred aspect ratio from the start.
     *
     * @param activity the activity to set the preference.
     * @param preferredRatio the aspect ratio determined by taking the panel's width over its
     *   height. A value <= 0.0f means there are no preferences.
     */
    public fun setPreferredAspectRatio(activity: Activity, preferredRatio: Float): Unit =
        platformAdapter.setPreferredAspectRatio(activity, preferredRatio)

    /**
     * Returns all [Entity]s of the given type or its subtypes.
     *
     * @param type the type of [Entity] to return.
     * @return a list of all [Entity]s of the given type.
     */
    public fun <T : Entity> getEntitiesOfType(type: Class<out T>): List<T> =
        entityManager.getEntitiesOfType(type)

    internal fun getEntityForRtEntity(entity: RtEntity): Entity? {
        return entityManager.getEntityForRtEntity(entity)
    }
}
