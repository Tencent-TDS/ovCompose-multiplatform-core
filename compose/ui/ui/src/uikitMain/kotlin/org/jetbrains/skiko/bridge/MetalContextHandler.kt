package org.jetbrains.skiko.bridge

import kotlinx.cinterop.useContents
import org.jetbrains.skia.*
import org.jetbrains.skiko.*

@InternalSkikoApi
abstract class ContextHandler2(
    protected val layer: SkiaLayer,
    private val drawContent: Canvas.() -> Unit
) {
    protected var context: DirectContext? = null
    protected var renderTarget: BackendRenderTarget? = null
    protected var surface: Surface? = null
    protected var canvas: Canvas? = null

    protected abstract fun initContext(): Boolean
    protected abstract fun initCanvas()

    protected open fun flush() {
        context?.flush()
    }

    open fun dispose() {
        disposeCanvas()
        context?.close()
    }

    protected open fun disposeCanvas() {
        surface?.close()
        renderTarget?.close()
    }

    open fun rendererInfo(): String {
        return "GraphicsApi: ${layer.renderApi}\n" +
            "OS: ${hostOs.id} ${hostArch.id}\n"
    }

    // throws RenderException if initialization of graphic context was not successful
    fun draw() {
        if (!initContext()) {
            throw RenderException("Cannot init graphic context")
        }
        initCanvas()
        canvas?.apply {
            clear(if (isTransparentBackground()) Color.TRANSPARENT else Color.WHITE)
            drawContent()
        }
        flush()
    }

    protected open fun isTransparentBackground(): Boolean {
        if (hostOs == OS.MacOS) {
            // MacOS transparency is always supported
            return true
        }
        if (layer.fullscreen) {
            // for non-MacOS in fullscreen transparency is not supported
            return false
        }
        // for non-MacOS in non-fullscreen transparency provided by [layer]
        return layer.transparency
    }
}


@OptIn(InternalSkikoApi::class)
internal class MetalContextHandler(layer: SkiaLayer, val metalRedrawer: MetalRedrawer) : ContextHandler2(layer, layer::draw) {

    override fun initContext(): Boolean {
        try {
            if (context == null) {
                context = metalRedrawer.makeContext()
            }
        } catch (e: Exception) {
            println("${e.message}\nFailed to create Skia Metal context!")
            return false
        }
        return true
    }

    private var currentWidth = 0
    private var currentHeight = 0
    private fun isSizeChanged(width: Int, height: Int): Boolean {
        if (width != currentWidth || height != currentHeight) {
            currentWidth = width
            currentHeight = height
            return true
        }
        return false
    }

    override fun initCanvas() {
        disposeCanvas()
        val scale = layer.contentScale
        val (w, h) = layer.view!!.frame.useContents {
            (size.width * scale).toInt().coerceAtLeast(0) to (size.height * scale).toInt().coerceAtLeast(0)
        }

        if (isSizeChanged(w, h)) {
            metalRedrawer.syncSize()
        }

        if (w > 0 && h > 0) {
            renderTarget = metalRedrawer.makeRenderTarget(w, h)

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

    override fun flush() {
        // TODO: maybe make flush async as in JVM version.
        super.flush()
        surface?.flushAndSubmit()
        metalRedrawer.finishFrame()
    }

   override fun rendererInfo(): String {
        return "Native Metal: device ${metalRedrawer.device.name}"
    }
}

