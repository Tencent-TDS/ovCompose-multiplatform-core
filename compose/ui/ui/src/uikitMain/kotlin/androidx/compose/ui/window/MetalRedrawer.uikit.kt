/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.window

import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.interop.UIKitInteropState
import androidx.compose.ui.interop.doLocked
import androidx.compose.ui.interop.isNotEmpty
import androidx.compose.ui.util.fastForEach
import kotlinx.cinterop.autoreleasepool
import kotlinx.cinterop.objcPtr
import kotlinx.cinterop.useContents
import org.jetbrains.skia.BackendRenderTarget
import org.jetbrains.skia.Color
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.DirectContext
import org.jetbrains.skia.PictureRecorder
import org.jetbrains.skia.PixelGeometry
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface
import org.jetbrains.skia.SurfaceColorFormat
import org.jetbrains.skia.SurfaceOrigin
import org.jetbrains.skia.SurfaceProps
import platform.Foundation.NSLock
import platform.Foundation.NSThread
import platform.Foundation.NSTimeInterval
import platform.Metal.MTLCommandBufferProtocol
import platform.Metal.MTLCommandQueueProtocol
import platform.Metal.MTLDeviceProtocol
import platform.QuartzCore.CACurrentMediaTime
import platform.QuartzCore.CAMetalLayer
import platform.darwin.DISPATCH_TIME_FOREVER
import platform.darwin.dispatch_async
import platform.darwin.dispatch_queue_create
import platform.darwin.dispatch_semaphore_create
import platform.darwin.dispatch_semaphore_signal
import platform.darwin.dispatch_semaphore_wait
import kotlin.math.roundToInt

internal class InflightCommandBuffers(
    private val maxInflightCount: Int
) {
    private val lock = NSLock()
    private val list = mutableListOf<MTLCommandBufferProtocol>()

    fun waitUntilAllAreScheduled() = lock.doLocked {
        list.fastForEach {
            it.waitUntilScheduled()
        }
    }

    fun add(commandBuffer: MTLCommandBufferProtocol) = lock.doLocked {
        if (list.size == maxInflightCount) {
            list.removeAt(0)
        }

        list.add(commandBuffer)
    }
}

internal class MetalRedrawer(
    private val metalLayer: CAMetalLayer,
    callbacks: RedrawerCallbacks
// region Tencent Code
) : Redrawer(callbacks) {
// endregion
    // Workaround for KN compiler bug
    // Type mismatch: inferred type is objcnames.protocols.MTLDeviceProtocol but platform.Metal.MTLDeviceProtocol was expected
    @Suppress("USELESS_CAST")
    private val device = metalLayer.device as platform.Metal.MTLDeviceProtocol?
        ?: throw IllegalStateException("CAMetalLayer.device can not be null")
    private val queue = getCachedCommandQueue(device)
    private val context = DirectContext.makeMetal(device.objcPtr(), queue.objcPtr())
    private var lastRenderTimestamp: NSTimeInterval = CACurrentMediaTime()
    private val pictureRecorder = PictureRecorder()

    // Semaphore for preventing command buffers count more than swapchain size to be scheduled/executed at the same time
    private val inflightSemaphore =
        dispatch_semaphore_create(metalLayer.maximumDrawableCount.toLong())
    private val inflightCommandBuffers =
        InflightCommandBuffers(metalLayer.maximumDrawableCount.toInt())

    // region Tencent Code
    override var opaque: Boolean = true
        set(value) {
            field = value

            updateLayerOpacity()
        }
    // endregion

    /**
     * `true` if Metal rendering is synchronized with changes of UIKit interop views, `false` otherwise
     */
    private var isInteropActive = false
        set(value) {
            field = value

            // If active, make metalLayer transparent, opaque otherwise.
            // Rendering into opaque CAMetalLayer allows direct-to-screen optimization.
            updateLayerOpacity()
            metalLayer.drawsAsynchronously = !value
        }

    private fun updateLayerOpacity() {
        metalLayer.setOpaque(!isInteropActive && opaque)
    }

    // region Tencent Code
    init {
        updateLayerOpacity()
    }

    override fun onApplicationStateChanged(isApplicationActive: Boolean) {
        if (!isApplicationActive) {
            // If application goes background, synchronously schedule all inflightCommandBuffers, as per
            // https://developer.apple.com/documentation/metal/gpu_devices_and_work_submission/preparing_your_metal_app_to_run_in_the_background?language=objc
            inflightCommandBuffers.waitUntilAllAreScheduled()
        }
    }

    override fun dispose() {
        super.dispose()

        releaseCachedCommandQueue(queue)

        pictureRecorder.close()
        context.close()
    }

    override fun draw(waitUntilCompletion: Boolean, targetTimestamp: NSTimeInterval) {
    // endregion
        check(NSThread.isMainThread)

        lastRenderTimestamp = maxOf(targetTimestamp, lastRenderTimestamp)

        autoreleasepool {
            val (width, height) = metalLayer.drawableSize.useContents {
                width.roundToInt() to height.roundToInt()
            }

            if (width <= 0 || height <= 0) {
                return@autoreleasepool
            }

            // Perform timestep and record all draw commands into [Picture]
            pictureRecorder.beginRecording(
                Rect(
                    left = 0f,
                    top = 0f,
                    width.toFloat(),
                    height.toFloat()
                )
            ).also { canvas ->
                canvas.clear(if (metalLayer.opaque) Color.WHITE else Color.TRANSPARENT)
                // region Tencent Code
                callbacks.render(canvas.asComposeCanvas(), lastRenderTimestamp)
                // endregion
            }

            val picture = pictureRecorder.finishRecordingAsPicture()

            dispatch_semaphore_wait(inflightSemaphore, DISPATCH_TIME_FOREVER)

            val metalDrawable = metalLayer.nextDrawable()

            if (metalDrawable == null) {
                // TODO: anomaly, log
                // Logger.warn { "'metalLayer.nextDrawable()' returned null. 'metalLayer.allowsNextDrawableTimeout' should be set to false. Skipping the frame." }
                picture.close()
                dispatch_semaphore_signal(inflightSemaphore)
                return@autoreleasepool
            }

            val renderTarget =
                BackendRenderTarget.makeMetal(width, height, metalDrawable.texture.objcPtr())

            val surface = Surface.makeFromBackendRenderTarget(
                context,
                renderTarget,
                SurfaceOrigin.TOP_LEFT,
                SurfaceColorFormat.BGRA_8888,
                ColorSpace.sRGB,
                SurfaceProps(pixelGeometry = PixelGeometry.UNKNOWN)
            )

            if (surface == null) {
                // TODO: anomaly, log
                // Logger.warn { "'Surface.makeFromBackendRenderTarget' returned null. Skipping the frame." }
                picture.close()
                renderTarget.close()
                dispatch_semaphore_signal(inflightSemaphore)
                return@autoreleasepool
            }

            val interopTransaction = callbacks.retrieveInteropTransaction()
            if (interopTransaction.state == UIKitInteropState.BEGAN) {
                isInteropActive = true
            }
            val presentsWithTransaction =
                isForcedToPresentWithTransactionEveryFrame || interopTransaction.isNotEmpty()
            metalLayer.presentsWithTransaction = presentsWithTransaction

            // TODO: encoding on separate thread requires investigation for reported crashes
            //  https://github.com/JetBrains/compose-multiplatform/issues/3862
            //  https://youtrack.jetbrains.com/issue/COMPOSE-608/iOS-reproduce-and-investigate-parallel-rendering-encoding-crash
            // val mustEncodeAndPresentOnMainThread = presentsWithTransaction || waitUntilCompletion
            val mustEncodeAndPresentOnMainThread = true

            val encodeAndPresentBlock = {
                surface.canvas.drawPicture(picture)
                picture.close()
                surface.flushAndSubmit()

                val commandBuffer = queue.commandBuffer()!!
                commandBuffer.label = "Present"

                if (!presentsWithTransaction) {
                    commandBuffer.presentDrawable(metalDrawable)
                }

                commandBuffer.addCompletedHandler {
                    // Signal work finish, allow a new command buffer to be scheduled
                    dispatch_semaphore_signal(inflightSemaphore)
                }
                commandBuffer.commit()

                if (presentsWithTransaction) {
                    // If there are pending changes in UIKit interop, [waitUntilScheduled](https://developer.apple.com/documentation/metal/mtlcommandbuffer/1443036-waituntilscheduled) is called
                    // to ensure that transaction is available
                    commandBuffer.waitUntilScheduled()
                    metalDrawable.present()

                    interopTransaction.actions.fastForEach {
                        it.invoke()
                    }

                    if (interopTransaction.state == UIKitInteropState.ENDED) {
                        isInteropActive = false
                    }
                }

                surface.close()
                renderTarget.close()

                // Track current inflight command buffers to synchronously wait for their schedule in case app goes background
                inflightCommandBuffers.add(commandBuffer)

                if (waitUntilCompletion) {
                    commandBuffer.waitUntilCompleted()
                }
            }

            if (mustEncodeAndPresentOnMainThread) {
                encodeAndPresentBlock()
            } else {
                dispatch_async(renderingDispatchQueue) {
                    autoreleasepool {
                        encodeAndPresentBlock()
                    }
                }
            }
        }
    }

    companion object {
        private val renderingDispatchQueue =
            dispatch_queue_create("RenderingDispatchQueue", null)

        private class CachedCommandQueue(
            val queue: MTLCommandQueueProtocol,
            var refCount: Int = 1
        )

        /**
         * Cached command queue record. Assumed to be associated with default MTLDevice.
         */
        private var cachedCommandQueue: CachedCommandQueue? = null

        /**
         * Get an existing command queue associated with the device or create a new one and cache it.
         * Assumed to be run on the main thread.
         */
        private fun getCachedCommandQueue(device: MTLDeviceProtocol): MTLCommandQueueProtocol {
            val cached = cachedCommandQueue
            if (cached != null) {
                cached.refCount++
                return cached.queue
            } else {
                val queue = device.newCommandQueue() ?: throw IllegalStateException("MTLDevice.newCommandQueue() returned null")
                cachedCommandQueue = CachedCommandQueue(queue)
                return queue
            }
        }

        /**
         * Release the cached command queue. Release the cache if refCount reaches 0.
         * Assumed to be run on the main thread.
         */
        private fun releaseCachedCommandQueue(queue: MTLCommandQueueProtocol) {
            val cached = cachedCommandQueue ?: return
            if (cached.queue == queue) {
                cached.refCount--
                if (cached.refCount == 0) {
                    cachedCommandQueue = null
                }
            }
        }
    }
}