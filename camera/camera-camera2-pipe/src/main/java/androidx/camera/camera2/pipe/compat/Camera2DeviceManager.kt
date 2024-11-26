/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.camera2.pipe.compat

import androidx.camera.camera2.pipe.CameraError
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.GraphState
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.core.Permissions
import androidx.camera.camera2.pipe.core.Threads
import androidx.camera.camera2.pipe.core.WakeLock
import androidx.camera.camera2.pipe.graph.GraphListener
import androidx.camera.camera2.pipe.graph.GraphRequestProcessor
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal sealed class CameraRequest

internal data class RequestOpen(
    val virtualCamera: VirtualCameraState,
    val sharedCameraIds: List<CameraId>,
    val graphListener: GraphListener,
    val isPrewarm: Boolean,
    val isForegroundObserver: (Unit) -> Boolean,
) : CameraRequest()

/**
 * Sends a request to close an active camera. Note: RequestOpen() & RequestClose() may not be
 * executed sequentially, as the camera may take a while to be fully opened, and RequestClose()
 * might execute in parallel.
 */
internal data class RequestClose(val activeCamera: ActiveCamera) : CameraRequest()

internal data class RequestCloseById(val activeCameraId: CameraId) : CameraRequest()

internal object RequestCloseAll : CameraRequest()

internal object NoOpGraphListener : GraphListener {
    override fun onGraphStarted(requestProcessor: GraphRequestProcessor) {}

    override fun onGraphStopped(requestProcessor: GraphRequestProcessor?) {}

    override fun onGraphModified(requestProcessor: GraphRequestProcessor) {}

    override fun onGraphError(graphStateError: GraphState.GraphStateError) {}
}

internal interface Camera2DeviceManager {
    /**
     * Issue a request to open the specified camera. The camera will be delivered through
     * [VirtualCamera.state] when opened, and the state will continue to provide updates to the
     * state of the camera. If shared camera IDs are specified, the cameras won't be provided until
     * all cameras are opened.
     */
    fun open(
        cameraId: CameraId,
        sharedCameraIds: List<CameraId>,
        graphListener: GraphListener,
        isPrewarm: Boolean,
        isForegroundObserver: (Unit) -> Boolean,
    ): VirtualCamera?

    /**
     * Connects and starts the underlying camera. Once the active camera timeout elapses and it
     * hasn't been utilized, the camera is closed.
     */
    fun prewarm(cameraId: CameraId)

    /** Submits a request to close the underlying camera. */
    fun close(cameraId: CameraId)

    /** Instructs Camera2DeviceManager to close all cameras. */
    fun closeAll()
}

internal class ActiveCamera(
    private val androidCameraState: AndroidCameraState,
    internal val allCameraIds: Set<CameraId>,
    scope: CoroutineScope,
    channel: SendChannel<CameraRequest>
) {
    val cameraId: CameraId
        get() = androidCameraState.cameraId

    private var current: VirtualCameraState? = null

    private val wakelock =
        WakeLock(
            scope,
            timeout = 1000,
            callback = { channel.trySend(RequestClose(this)).isSuccess },
            // Every ActiveCamera is associated with an opened camera. We should ensure that we
            // issue a RequestClose eventually for every ActiveCamera created.
            //
            // A notable bug is b/264396089 where, because camera opens took too long, we didn't
            // acquire a WakeLockToken, and thereby not issuing the request to close camera
            // eventually.
            startTimeoutOnCreation = true
        )

    init {
        scope.launch {
            androidCameraState.state.first { it is CameraStateClosing || it is CameraStateClosed }
            wakelock.release()
        }
    }

    suspend fun connectTo(virtualCameraState: VirtualCameraState) {
        val token = wakelock.acquire()
        val previous = current
        current = virtualCameraState

        previous?.disconnect()
        virtualCameraState.connect(androidCameraState.state, token)
    }

    fun close() {
        wakelock.release()
        androidCameraState.close()
    }

    suspend fun awaitClosed() {
        androidCameraState.awaitClosed()
    }
}

// TODO: b/307396261 - A queue depth of 64 was deemed necessary in b/276051078 and b/307396261 where
//  a flood of requests can cause the queue depth to grow larger than anticipated. Rewrite the
//  camera manager such that it handles these abnormal scenarios more robustly.
private const val requestQueueDepth = 64

@Suppress("EXPERIMENTAL_API_USAGE")
@Singleton
internal class Camera2DeviceManagerImpl
@Inject
constructor(
    private val permissions: Permissions,
    private val retryingCameraStateOpener: RetryingCameraStateOpener,
    private val camera2DeviceCloser: Camera2DeviceCloser,
    private val camera2ErrorProcessor: Camera2ErrorProcessor,
    private val threads: Threads
) : Camera2DeviceManager {
    // TODO: Consider rewriting this as a MutableSharedFlow
    private val requestQueue: Channel<CameraRequest> = Channel(requestQueueDepth)
    private val activeCameras: MutableSet<ActiveCamera> = mutableSetOf()
    private val pendingRequestOpens = mutableListOf<RequestOpen>()

    init {
        threads.globalScope.launch(CoroutineName("CXCP-Camera2DeviceManager")) { requestLoop() }
    }

    override fun open(
        cameraId: CameraId,
        sharedCameraIds: List<CameraId>,
        graphListener: GraphListener,
        isPrewarm: Boolean,
        isForegroundObserver: (Unit) -> Boolean,
    ): VirtualCamera? {
        val result = VirtualCameraState(cameraId, graphListener, threads.globalScope)
        if (
            !offerChecked(
                RequestOpen(result, sharedCameraIds, graphListener, isPrewarm, isForegroundObserver)
            )
        ) {
            Log.error { "Camera open request failed: Camera2DeviceManagerImpl queue size exceeded" }
            graphListener.onGraphError(
                GraphState.GraphStateError(
                    CameraError.ERROR_CAMERA_OPENER,
                    willAttemptRetry = false
                )
            )
            return null
        }
        return result
    }

    override fun prewarm(cameraId: CameraId) {
        open(
            cameraId = cameraId,
            sharedCameraIds = emptyList(),
            graphListener = NoOpGraphListener,
            isPrewarm = true,
        ) { _ ->
            false
        }
    }

    override fun close(cameraId: CameraId) {
        offerChecked(RequestCloseById(cameraId))
    }

    override fun closeAll() {
        if (!offerChecked(RequestCloseAll)) {
            Log.warn { "Failed to close all cameras: Close request submission failed" }
            return
        }
    }

    private fun offerChecked(request: CameraRequest): Boolean {
        return requestQueue.trySend(request).isSuccess
    }

    private suspend fun requestLoop() = coroutineScope {
        val requests = arrayListOf<CameraRequest>()

        while (true) {
            // Stage 1: We have a request, but there is a chance we have received multiple
            //   requests.
            readRequestQueue(requests)

            // Prioritize requests that remove specific cameras from the list of active cameras.
            val closeRequest = requests.firstOrNull { it is RequestClose } as? RequestClose
            if (closeRequest != null) {
                requests.remove(closeRequest)
                if (activeCameras.contains(closeRequest.activeCamera)) {
                    activeCameras.remove(closeRequest.activeCamera)
                }
                pendingRequestOpens.removeAll {
                    it.virtualCamera.cameraId == closeRequest.activeCamera.cameraId
                }

                launch { closeRequest.activeCamera.close() }
                closeRequest.activeCamera.awaitClosed()
                continue
            }

            // Ensures the closure of a camera device happens after any preceding RequestOpen().
            val closeRequestById = requests.firstOrNull()
            if (closeRequestById != null && closeRequestById is RequestCloseById) {
                requests.remove(closeRequestById)
                pendingRequestOpens.removeAll {
                    it.virtualCamera.cameraId == closeRequestById.activeCameraId
                }
                val activeCamera =
                    activeCameras.firstOrNull { it.cameraId == closeRequestById.activeCameraId }
                if (activeCamera != null) {
                    activeCameras.remove(activeCamera)
                    launch { activeCamera.close() }
                    activeCamera.awaitClosed()
                }
                continue
            }

            // If we received a closeAll request, then close every request leading up to it.
            val closeAll = requests.indexOfLast { it is RequestCloseAll }
            if (closeAll >= 0) {
                for (i in 0..closeAll) {
                    val request = requests[0]
                    if (request is RequestOpen) {
                        request.virtualCamera.disconnect()
                    }
                    requests.removeAt(0)
                }

                // Close all active cameras.
                for (activeCamera in activeCameras) {
                    launch { activeCamera.close() }
                }
                for (camera in activeCameras) {
                    camera.awaitClosed()
                }
                activeCameras.clear()
                pendingRequestOpens.clear()
                continue
            }

            // The only way we get to this point is if:
            // A) We received a request
            // B) That request was NOT a Close, or CloseAll request
            val request = requests[0]
            check(request is RequestOpen)
            if (request.isPrewarm) {
                check(request.sharedCameraIds.isEmpty()) {
                    "Prewarming concurrent cameras is not supported"
                }
            }

            // Sanity Check: If the camera we are attempting to open is now closed or disconnected,
            // skip this virtual camera request.
            if (request.virtualCamera.value !is CameraStateUnopened) {
                requests.remove(request)
                continue
            }

            // Stage 2: Intermediate requests have been discarded, and we need to evaluate the set
            //   of currently open cameras to the set of desired cameras and close ones that are not
            //   needed. Since close may block, we will re-evaluate the next request after the
            //   desired cameras are closed since new requests may have arrived.
            val cameraIdToOpen = request.virtualCamera.cameraId
            val camerasToClose =
                if (request.sharedCameraIds.isEmpty()) {
                    activeCameras.filter { it.cameraId != cameraIdToOpen }
                } else {
                    val allCameraIds =
                        (request.sharedCameraIds + request.virtualCamera.cameraId).toSet()
                    activeCameras.filter { it.allCameraIds != allCameraIds }
                }

            if (camerasToClose.isNotEmpty()) {
                // Shutdown of cameras should always happen first (and suspend until complete)
                activeCameras.removeAll(camerasToClose)
                pendingRequestOpens.removeAll { requestOpen ->
                    camerasToClose.any { it.cameraId == requestOpen.virtualCamera.cameraId }
                }
                for (camera in camerasToClose) {
                    // TODO: This should be a dispatcher instead of scope.launch

                    launch {
                        // TODO: Figure out if this should be blocking or not. If we are directly
                        // invoking
                        //   close this method could block for 0-1000ms
                        camera.close()
                    }
                }
                for (realCamera in camerasToClose) {
                    realCamera.awaitClosed()
                }
                continue
            }

            // Stage 3: Open or select an active camera device.
            camera2ErrorProcessor.setActiveVirtualCamera(cameraIdToOpen, request.virtualCamera)
            var realCamera = activeCameras.firstOrNull { it.cameraId == cameraIdToOpen }
            if (realCamera == null) {
                val openResult =
                    openCameraWithRetry(
                        cameraIdToOpen,
                        request.sharedCameraIds,
                        request.isForegroundObserver,
                        scope = this
                    )
                if (openResult.activeCamera != null) {
                    realCamera = openResult.activeCamera
                    activeCameras.add(realCamera)
                } else {
                    request.virtualCamera.disconnect(openResult.lastCameraError)
                    requests.remove(request)
                }
                continue
            }

            // Stage 4: Attach camera(s)
            if (request.sharedCameraIds.isNotEmpty()) {
                // Both sharedCameraIds and activeCameras are small collections. Looping over them
                // in what equates to nested for-loops are actually going to be more efficient than
                // say, replacing activeCameras with a hashmap.
                if (
                    request.sharedCameraIds.all { cameraId ->
                        activeCameras.any { it.cameraId == cameraId }
                    }
                ) {
                    // If the camera of the request and the cameras it is shared with have been
                    // opened, we can connect the ActiveCameras.
                    check(!request.isPrewarm)
                    realCamera.connectTo(request.virtualCamera)
                    connectPendingRequestOpens(request.sharedCameraIds)
                } else {
                    // Else, save the request in the pending request queue, and connect the request
                    // once other cameras are opened.
                    pendingRequestOpens.add(request)
                }
            } else {
                if (!request.isPrewarm) {
                    realCamera.connectTo(request.virtualCamera)
                }
            }
            requests.remove(request)
        }
    }

    private suspend fun openCameraWithRetry(
        cameraId: CameraId,
        sharedCameraIds: List<CameraId>,
        isForegroundObserver: (Unit) -> Boolean,
        scope: CoroutineScope
    ): OpenVirtualCameraResult {
        // TODO: Figure out how 1-time permissions work, and see if they can be reset without
        //   causing the application process to restart.
        check(permissions.hasCameraPermission) { "Missing camera permissions!" }

        Log.debug { "Opening $cameraId with retries..." }
        val result =
            retryingCameraStateOpener.openCameraWithRetry(
                cameraId,
                camera2DeviceCloser,
                isForegroundObserver
            )
        if (result.cameraState == null) {
            return OpenVirtualCameraResult(lastCameraError = result.errorCode)
        }
        return OpenVirtualCameraResult(
            activeCamera =
                ActiveCamera(
                    androidCameraState = result.cameraState,
                    allCameraIds = (sharedCameraIds + cameraId).toSet(),
                    scope = scope,
                    channel = requestQueue
                )
        )
    }

    private suspend fun connectPendingRequestOpens(cameraIds: List<CameraId>) {
        val requestOpensToRemove = mutableListOf<RequestOpen>()
        val requestOpens =
            pendingRequestOpens.filter { cameraIds.contains(it.virtualCamera.cameraId) }
        for (request in requestOpens) {
            // If the request is shared with this pending request, then we should be
            // able to connect this pending request too, since we don't allow
            // overlapping.
            val allCameraIds = listOf(request.virtualCamera.cameraId) + request.sharedCameraIds
            check(allCameraIds.all { cameraId -> activeCameras.any { it.cameraId == cameraId } })

            val realCamera = activeCameras.find { it.cameraId == request.virtualCamera.cameraId }
            checkNotNull(realCamera)
            realCamera.connectTo(request.virtualCamera)
            requestOpensToRemove.add(request)
        }
        pendingRequestOpens.removeAll(requestOpensToRemove)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private suspend fun readRequestQueue(requests: MutableList<CameraRequest>) {
        if (requests.isEmpty()) {
            requests.add(requestQueue.receive())
        }

        // We have a request, but there is a chance we have received multiple requests while we
        // were doing other things (like opening a camera).
        while (!requestQueue.isEmpty) {
            requests.add(requestQueue.receive())
        }
    }

    /**
     * There are 3 possible scenarios with [OpenVirtualCameraResult]. Suppose we denote the values
     * in pairs of ([activeCamera], [lastCameraError]):
     * - ([activeCamera], null): Camera opened without an issue.
     * - (null, [lastCameraError]): Camera opened failed and the last error was [lastCameraError].
     * - (null, null): Camera open didn't complete, likely due to CameraGraph being stopped or
     *   closed during the process.
     */
    private data class OpenVirtualCameraResult(
        val activeCamera: ActiveCamera? = null,
        val lastCameraError: CameraError? = null
    )
}
