package androidx.compose.ui.node

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.unit.Density

abstract class OwnedLayerFactory {

    internal abstract fun createLayer(
        density: Density,
        drawBlock: (Canvas) -> Unit,
        invalidateParentLayer: () -> Unit,
        onDestroy: () -> Unit
    ): OwnedLayer

}