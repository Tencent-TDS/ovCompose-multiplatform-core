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

package androidx.xr.runtime

import android.app.Activity
import android.content.pm.PackageManager
import androidx.annotation.RestrictTo
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.xr.runtime.internal.Runtime
import androidx.xr.runtime.internal.RuntimeFactory
import java.util.ServiceLoader
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * A session is the main entrypoint to features provided by ARCore for Jetpack XR. It manages the
 * system's state and its lifecycle, and contains the state of objects tracked by ARCore for Jetpack
 * XR.
 *
 * This class owns a significant amount of native heap memory. Apps using a `Session` consider its
 * lifecycle to ensure that native resources are released when the session is no longer needed. If
 * your activity is a single XR-enabled activity, it is recommended to call the `Session` object's
 * lifecycle methods from the activity's lifecycle methods using a
 * [lifecycle-aware component](https://developer.android.com/topic/libraries/architecture/lifecycle).
 * See [create], [resume], [pause], and [destroy] for more details.
 */
@Suppress("NotCloseable")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class Session
internal constructor(
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) public val runtime: Runtime,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public val stateExtenders: List<StateExtender>,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public val coroutineScope: CoroutineScope,
    private val activity: Activity,
) : LifecycleOwner {
    public companion object {
        /**
         * Creates a new [Session].
         *
         * Creating a session requires the `android.permission.SCENE_UNDERSTANDING_COARSE` and
         * `android.permission.HAND_TRACKING` permissions to be granted.
         *
         * @param activity the [Activity] that owns the session.
         * @param coroutineDispatcher the [CoroutineDispatcher] that will be used to handle the
         *   session's coroutines.
         * @return the result of the operation. Can be [SessionCreateSuccess], which contains the
         *   newly created session, or [SessionCreatePermissionsNotGranted] if the required
         *   permissions haven't been granted.
         */
        @JvmOverloads
        @JvmStatic
        public fun create(
            activity: Activity,
            coroutineDispatcher: CoroutineDispatcher = CoroutineDispatchers.Lightweight,
        ): SessionCreateResult {
            val missingPermissions = activity.selectMissing(SESSION_PERMISSIONS)
            if (missingPermissions.isNotEmpty()) {
                return SessionCreatePermissionsNotGranted(missingPermissions)
            }

            val runtimeFactory: RuntimeFactory =
                ServiceLoader.load(RuntimeFactory::class.java).single()
            val runtime = runtimeFactory.createRuntime(activity)
            runtime.lifecycleManager.create()

            val stateExtenders = ServiceLoader.load(StateExtender::class.java).toList()
            for (stateExtender in stateExtenders) {
                stateExtender.initialize(runtime)
            }
            val session =
                Session(runtime, stateExtenders, CoroutineScope(coroutineDispatcher), activity)

            session.lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

            return SessionCreateSuccess(session)
        }

        // TODO(b/392919087): Move the Hand Tracking permission to another place.
        internal val SESSION_PERMISSIONS: List<String> =
            listOf(SCENE_UNDERSTANDING_COARSE, HAND_TRACKING)
    }

    private val lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)

    override public val lifecycle: Lifecycle = lifecycleRegistry

    private val _state = MutableStateFlow<CoreState>(CoreState(TimeSource.Monotonic.markNow()))

    /** A [StateFlow] of the current state. */
    public val state: StateFlow<CoreState> = _state.asStateFlow()

    private var updateJob: Job? = null

    /**
     * Sets or changes the configuration to use.
     *
     * @return the result of the operation. This will be a [SessionConfigureSuccess] if the
     *   configuration was successful, or a [SessionConfigureConfigurationNotSupported] if the
     *   [SessionConfiguration] is not supported.
     * @throws IllegalStateException if the session has been destroyed.
     */
    public fun configure(): SessionConfigureResult {
        check(lifecycleRegistry.currentState != Lifecycle.State.DESTROYED) {
            "Session has been destroyed."
        }
        runtime.lifecycleManager.configure()
        return SessionConfigureSuccess()
    }

    /**
     * Starts or resumes the session.
     *
     * Resuming a session requires the `android.permission.SCENE_UNDERSTANDING_COARSE` and
     * `android.permission.HAND_TRACKING` permissions to be granted.
     *
     * @return the result of the operation. Can be [SessionResumeSuccess] if the session was
     *   successfully resumed, or [SessionResumePermissionsNotGranted] if the required permissions
     *   haven't been granted.
     * @throws IllegalStateException if the session has been destroyed.
     */
    public fun resume(): SessionResumeResult {
        check(lifecycleRegistry.currentState != Lifecycle.State.DESTROYED) {
            "Session has been destroyed."
        }
        if (lifecycleRegistry.currentState != Lifecycle.State.RESUMED) {
            val missingPermissions = activity.selectMissing(SESSION_PERMISSIONS)
            if (missingPermissions.isNotEmpty()) {
                return SessionResumePermissionsNotGranted(missingPermissions)
            }

            runtime.lifecycleManager.resume()
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            updateJob = coroutineScope.launch { updateLoop() }
        }

        return SessionResumeSuccess()
    }

    /**
     * Pauses execution of the session. Objects tracked by the session will not receive updates. The
     * system state will be retained in memory.
     *
     * Calling this method on an inactive session is a no-op.
     *
     * @throws IllegalStateException if the session has been destroyed.
     */
    public fun pause() {
        check(lifecycleRegistry.currentState != Lifecycle.State.DESTROYED) {
            "Session has been destroyed."
        }

        if (lifecycleRegistry.currentState == Lifecycle.State.RESUMED) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            runtime.lifecycleManager.pause()
            updateJob?.cancel()
            updateJob = null
        }
    }

    /**
     * Destroys the session, releasing any resources acquired by the session. Objects tracked by the
     * system will not receive updates.
     *
     * Calling this method on a destroyed session is a no-op. Additionally, calling this method on
     * an active session will first call [pause].
     */
    public fun destroy() {
        if (lifecycleRegistry.currentState == Lifecycle.State.DESTROYED) {
            return
        } else if (lifecycleRegistry.currentState == Lifecycle.State.RESUMED) {
            pause()
        }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        runtime.lifecycleManager.stop()
        coroutineScope.cancel()
    }

    private suspend fun updateLoop() {
        while (lifecycleRegistry.currentState == Lifecycle.State.RESUMED) {
            update()
        }
    }

    /** Produces the latest [CoreState] so it can be emitted downstream. */
    private suspend fun update() {
        val timeMark = runtime.lifecycleManager.update()
        val state = CoreState(timeMark)

        for (stateExtender in stateExtenders) {
            stateExtender.extend(state)
        }

        _state.emit(state)
    }
}

private fun Activity.selectMissing(permissions: List<String>): List<String> =
    permissions.filter { permission -> !hasPermission(permission) }

private fun Activity.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
