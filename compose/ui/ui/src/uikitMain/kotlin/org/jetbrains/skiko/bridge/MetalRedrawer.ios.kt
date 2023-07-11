package org.jetbrains.skiko.bridge

import kotlinx.cinterop.*
import org.jetbrains.skia.*
import org.jetbrains.skiko.InternalSkikoApi
import org.jetbrains.skiko.RenderException
import org.jetbrains.skiko.SkiaLayer
import platform.CoreGraphics.CGColorCreate
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGContextRef
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSRunLoop
import platform.Foundation.NSSelectorFromString
import platform.Metal.MTLCreateSystemDefaultDevice
import platform.Metal.MTLDeviceProtocol
import platform.Metal.MTLPixelFormatBGRA8Unorm
import platform.QuartzCore.*
import platform.darwin.*

private enum class DrawSchedulingState {
    AVAILABLE_ON_NEXT_FRAME,
    AVAILABLE_ON_CURRENT_FRAME,
    SCHEDULED_ON_NEXT_FRAME
}

@InternalSkikoApi
class MetalRedrawer(
    private val layer: SkiaLayer,
    private val device: MTLDeviceProtocol,
    private val metalLayer: CAMetalLayer,
) {
    private var currentWidth = 0
    private var currentHeight = 0
    private var context: DirectContext? = null
    private var renderTarget: BackendRenderTarget? = null
    private var surface: Surface? = null
    private var canvas: Canvas? = null

    fun initContext(): Boolean {
        try {
            if (context == null) {
                context = makeContext()
            }
        } catch (e: Exception) {
            println("${e.message}\nFailed to create Skia Metal context!")
            return false
        }
        return true
    }

    fun initCanvas() {
        disposeCanvas()
        val scale = layer.contentScale
        val (w, h) = layer.view!!.frame.useContents {
            (size.width * scale).toInt().coerceAtLeast(0) to (size.height * scale).toInt()
                .coerceAtLeast(0)
        }

        if (w > 0 && h > 0) {
            renderTarget = makeRenderTarget(w, h)

            surface = Surface.makeFromBackendRenderTarget(
                context!!,
                renderTarget!!,
                SurfaceOrigin.TOP_LEFT,
                SurfaceColorFormat.BGRA_8888,
                ColorSpace.sRGB,
                SurfaceProps(pixelGeometry = layer.pixelGeometry)
            ) ?: throw RenderException("Cannot create surface")

            canvas = surface!!.canvas
        } else {
            renderTarget = null
            surface = null
            canvas = null
        }
    }

    fun flush() {
        // TODO: maybe make flush async as in JVM version.
        context?.flush()
        surface?.flushAndSubmit()
        finishFrame()
    }

    fun disposeCanvas() {
        surface?.close()
        renderTarget?.close()
    }

    // throws RenderException if initialization of graphic context was not successful
    fun drawContextHandler() {
        if (!initContext()) {
            throw RenderException("Cannot init graphic context")
        }
        initCanvas()
        canvas?.apply {
            clear(Color.WHITE)
            layer.draw(this)
        }
        flush()
    }

    fun rendererInfo(): String {
        return "Native Metal: device ${device.name}"
    }

    val renderInfo: String get() = rendererInfo()
    private var isDisposed = false
    private val queue = device.newCommandQueue() ?: throw IllegalStateException("Couldn't create Metal command queue")
    private var currentDrawable: CAMetalDrawableProtocol? = null

    // Semaphore for preventing command buffers count more than swapchain size to be scheduled/executed at the same time
    private val inflightSemaphore = dispatch_semaphore_create(metalLayer.maximumDrawableCount.toLong())

    /*
     * Initial value is [DrawSchedulingState.AVAILABLE_ON_NEXT_FRAME] because voluntarily dispatching a frame
     * disregarding CADisplayLink timing (which is not accessible while it's paused) can cause frame drifting in worst
     * cases adding one frame latency due to presentation mechanism, if followed by steady draw dispatch
     * (which is often the case).
     * TODO: look closer to what happens after blank frames leave it in AVAILABLE_ON_CURRENT_FRAME. Touch driven events sequence negate that problem.
     */
    private var drawSchedulingState = DrawSchedulingState.AVAILABLE_ON_NEXT_FRAME

    /**
     * Needs scheduling displayLink for forcing UITouch events to come at the fastest possible cadence.
     * Otherwise, touch events can come at rate lower than actual display refresh rate.
     */
    var needsProactiveDisplayLink = false
        set(value) {
            field = value

            if (value) {
                caDisplayLink.setPaused(false)
            }
        }

    private val frameListener: NSObject = FrameTickListener {
        when (drawSchedulingState) {
            DrawSchedulingState.AVAILABLE_ON_NEXT_FRAME -> {
                drawSchedulingState = DrawSchedulingState.AVAILABLE_ON_CURRENT_FRAME
            }

            DrawSchedulingState.SCHEDULED_ON_NEXT_FRAME -> {
                drawIfLayerIsShowing()

                drawSchedulingState = DrawSchedulingState.AVAILABLE_ON_NEXT_FRAME
            }

            DrawSchedulingState.AVAILABLE_ON_CURRENT_FRAME -> {
                // still available, do nothing
            }
        }

        if (!needsProactiveDisplayLink) {
            caDisplayLink.setPaused(true)
        }
    }

    private val caDisplayLink = CADisplayLink.displayLinkWithTarget(
        target = frameListener,
        selector = NSSelectorFromString(FrameTickListener::onDisplayLinkTick.name)
    )

    var maximumFramesPerSecond: NSInteger
        get() = caDisplayLink.preferredFramesPerSecond
        set(value) {
            caDisplayLink.preferredFramesPerSecond = value
        }

    init {
        caDisplayLink.setPaused(true)
        caDisplayLink.addToRunLoop(NSRunLoop.mainRunLoop, NSRunLoop.mainRunLoop.currentMode)
    }

    /**
     * UITouch events are dispatched right before next CADisplayLink callback by iOS.
     * It's too late to encode any work for this frame after this happens.
     * Any work dispatched before the next CADisplayLink callback should be scheduled after that callback.
     */
    fun preventDrawDispatchDuringCurrentFrame() {
        if (drawSchedulingState == DrawSchedulingState.AVAILABLE_ON_CURRENT_FRAME) {
            drawSchedulingState = DrawSchedulingState.AVAILABLE_ON_NEXT_FRAME
        }
    }

    fun makeContext() = DirectContext.makeMetal(device.objcPtr(), queue.objcPtr())

    fun makeRenderTarget(width: Int, height: Int): BackendRenderTarget {
        // If more than swapchain size count of command buffers are inflight
        // wait until one finishes work
        dispatch_semaphore_wait(inflightSemaphore, DISPATCH_TIME_FOREVER)
        currentDrawable = metalLayer.nextDrawable()!!
        return BackendRenderTarget.makeMetal(width, height, currentDrawable!!.texture.objcPtr())
    }

    fun dispose() {
        if (!isDisposed) {
            caDisplayLink.invalidate()

            disposeCanvas()
            context?.close()

//            metalLayer.dispose() //TODO check need or not ?
            isDisposed = true
        }
    }

    fun needRedraw() {
        check(!isDisposed) { "MetalRedrawer is disposed" }

        drawImmediatelyIfPossible()

        if (drawSchedulingState == DrawSchedulingState.SCHEDULED_ON_NEXT_FRAME) {
            caDisplayLink.setPaused(false)
        }
    }

    fun redrawImmediately() {
        check(!isDisposed) { "MetalRedrawer is disposed" }
        draw()
    }

    /*
     * Dispatch redraw immediately during current frame if possible and updates [drawSchedulingState] to relevant value
     */
    private fun drawImmediatelyIfPossible() {
        when (drawSchedulingState) {
            DrawSchedulingState.AVAILABLE_ON_NEXT_FRAME -> {
                drawSchedulingState = DrawSchedulingState.SCHEDULED_ON_NEXT_FRAME
            }

            DrawSchedulingState.AVAILABLE_ON_CURRENT_FRAME -> {
                drawIfLayerIsShowing()

                drawSchedulingState = DrawSchedulingState.AVAILABLE_ON_NEXT_FRAME
            }

            DrawSchedulingState.SCHEDULED_ON_NEXT_FRAME -> {
                // already scheduled, do nothing
            }
        }
    }

    private fun drawIfLayerIsShowing() {
        if (layer.isShowing()) {
            draw()
        }
    }

    private fun draw() {
        // TODO: maybe make flush async as in JVM version.
        autoreleasepool { //todo measure performance without autoreleasepool
            if (!isDisposed) {
                drawContextHandler()
            }
        }
    }

    fun finishFrame() {
        autoreleasepool {
            currentDrawable?.let {
                val commandBuffer = queue.commandBuffer()!!
                commandBuffer.label = "Present"
                commandBuffer.presentDrawable(it)
                commandBuffer.addCompletedHandler {
                    // Signal work finish, allow a new command buffer to be scheduled
                    dispatch_semaphore_signal(inflightSemaphore)
                }
                commandBuffer.commit()
                currentDrawable = null
            }
        }
    }
}

private class FrameTickListener(val onFrameTick: () -> Unit) : NSObject() {
    @ObjCAction
    fun onDisplayLinkTick() {
        onFrameTick()
    }
}
