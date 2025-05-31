package androidx.compose.ui.platform.v2

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.node.OwnedLayer
import androidx.compose.ui.node.OwnedLayerFactory
import androidx.compose.ui.unit.Density
import platform.UIKit.UIView

class UIViewLayerFactory(
    private val rootView: UIView,
    private val nativeReusePool: Long = 0,
    private val clipChildren: Boolean = true
) : OwnedLayerFactory() {

    override fun createLayer(
        density: Density,
        drawBlock: (Canvas) -> Unit,
        invalidateParentLayer: () -> Unit,
        onDestroy: () -> Unit
    ): OwnedLayer {
        return UIViewLayer(
            density,
            invalidateParentLayer,
            drawBlock,
            onDestroy,
            rootView,
            nativeReusePool,
            clipChildren
        )
    }
}