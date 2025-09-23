package androidx.compose.ui.scene

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.platform.v2.DumpComposeCanvas

class ComposeSceneRenderForRenderNode(
    private val renderDelegate: ComposeSceneRender.Delegate
) :
    ComposeSceneRender() {
    interface Delegate {
        fun render(canvas: Canvas, timestamp: Long)
    }

    private val canvas = DumpComposeCanvas()

    override fun setSize(width: Int, height: Int) {
        // nothing to do
    }

    override fun draw(timestamp: Long) {
        renderDelegate.render(canvas, timestamp)
    }

    override fun close() {
        // noting to do
    }
}