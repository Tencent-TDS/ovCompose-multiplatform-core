package androidx.compose.ui.scene

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.platform.v2.DumpComposeCanvas
import androidx.compose.ui.text.flushNativeParagraphHandlesOnMainThread
import org.jetbrains.skia.Rect

class ComposeSceneRenderForRenderNode(
    private val renderDelegate: ComposeSceneRender.Delegate
) :
    ComposeSceneRender() {
    interface Delegate {
        fun render(canvas: Canvas, timestamp: Long)
    }

    private val canvas = DumpComposeCanvas()

    override fun setSize(width: Int, height: Int) {
        if (this.width != width || this.height != height) {
            this.width = width
            this.height = height
            this.renderRect = Rect(0f, 0f, width.toFloat(), height.toFloat())
        }
    }

    override fun draw(timestamp: Long) {
        renderDelegate.render(canvas, timestamp)
        flushNativeParagraphHandlesOnMainThread()
    }

    override fun close() {
        // noting to do
    }
}