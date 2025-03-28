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

package androidx.compose.ui.platform

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import androidx.compose.ui.assertThat
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.isEqualTo
import androidx.compose.ui.node.OwnedLayer
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.jetbrains.skia.Surface

class OwnerLayerTest {

    private val layer = TestRenderNodeLayer()
    private val cos45 = cos(PI / 4).toFloat()

    private val matrix get() = layer.underlyingMatrix
    private val inverseMatrix get() = Matrix().apply {
        matrix.invertTo(this)
    }


    @Test
    fun initial() {
        assertMapping(
            from = Offset(0f, 0f),
            to = Offset(0f, 0f)
        )
        assertMapping(
            from = Offset(100f, 10f),
            to = Offset(100f, 10f)
        )
    }

    @Test
    fun move() {
        layer.move(IntOffset(10, 20))

        assertMapping(
            from = Offset(0f, 0f),
            to = Offset(0f, 0f)
        )
        assertMapping(
            from = Offset(100f, 10f),
            to = Offset(100f, 10f)
        )
    }

    @Test
    fun resize() {
        layer.resize(IntSize(100, 10))

        assertMapping(
            from = Offset(0f, 0f),
            to = Offset(0f, 0f)
        )
        assertMapping(
            from = Offset(100f, 10f),
            to = Offset(100f, 10f)
        )
    }

    @Test
    fun resize_and_move() {
        layer.resize(IntSize(100, 10))
        layer.move(IntOffset(10, 20))

        assertMapping(
            from = Offset(0f, 0f),
            to = Offset(0f, 0f)
        )
        assertMapping(
            from = Offset(100f, 10f),
            to = Offset(100f, 10f)
        )
    }

    @Test
    fun translation_left_top_origin() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            translationX = 10f,
            translationY = 20f,
            transformOrigin = TransformOrigin(0f, 0f)
        )

        assertMapping(
            from = Offset(10f, 20f),
            to = Offset(0f, 0f)
        )
        assertMapping(
            from = Offset(110f, 30f),
            to = Offset(100f, 10f)
        )
    }

    @Test
    fun translation_bottom_right_origin() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            translationX = 10f,
            translationY = 20f,
            transformOrigin = TransformOrigin(1f, 1f)
        )

        assertMapping(
            from = Offset(10f, 20f),
            to = Offset(0f, 0f)
        )
        assertMapping(
            from = Offset(110f, 30f),
            to = Offset(100f, 10f)
        )
    }

    @Test
    fun scale_left_top_origin() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            scaleX = 2f,
            scaleY = 4f,
            transformOrigin = TransformOrigin(0f, 0f)
        )

        assertMapping(
            from = Offset(0f, 0f),
            to = Offset(0f, 0f)
        )
        assertMapping(
            from = Offset(200f, 40f),
            to = Offset(100f, 10f)
        )
    }

    @Test
    fun scale_bottom_right_origin() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            scaleX = 2f,
            scaleY = 4f,
            transformOrigin = TransformOrigin(1f, 1f)
        )

        assertMapping(
            from = Offset(-100f, -30f),
            to = Offset(0f, 0f)
        )
        assertMapping(
            from = Offset(100f, 10f),
            to = Offset(100f, 10f)
        )
    }

    @Test
    fun rotationX_left_top_origin() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            rotationX = 45f,
            transformOrigin = TransformOrigin(0f, 0f),
            cameraDistance = Float.MAX_VALUE
        )

        val y = 10 * cos45
        assertMapping(
            from = Offset(0f, 0f),
            to = Offset(0f, 0f)
        )
        assertMapping(
            from = Offset(100f, y),
            to = Offset(100f, 10f)
        )
    }

    @Test
    fun rotationX_bottom_right_origin() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            rotationX = 45f,
            transformOrigin = TransformOrigin(1f, 1f),
            cameraDistance = Float.MAX_VALUE
        )

        val y = 10 * (1 - cos45)
        assertMapping(
            from = Offset(0f, y),
            to = Offset(0f, 0f)
        )
        assertMapping(
            from = Offset(100f, 10f),
            to = Offset(100f, 10f)
        )
    }

    @Test
    fun rotationY_left_top_origin() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            rotationY = 45f,
            transformOrigin = TransformOrigin(0f, 0f),
            cameraDistance = Float.MAX_VALUE
        )

        val x = 100 * cos45
        assertMapping(
            from = Offset(0f, 0f),
            to = Offset(0f, 0f)
        )
        assertMapping(
            from = Offset(x, 10f),
            to = Offset(100f, 10f)
        )
    }

    @Test
    fun rotationY_bottom_right_origin() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            rotationY = 45f,
            transformOrigin = TransformOrigin(1f, 1f),
            cameraDistance = Float.MAX_VALUE
        )

        val x = 100 * (1 - cos45)
        assertMapping(
            from = Offset(x, 0f),
            to = Offset(0f, 0f)
        )
        assertMapping(
            from = Offset(100f, 10f),
            to = Offset(100f, 10f)
        )
    }

    @Test
    fun rotationZ_left_top_origin() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            rotationZ = 90f,
            transformOrigin = TransformOrigin(0f, 0f)
        )

        assertMapping(
            from = Offset(0f, 0f),
            to = Offset(0f, 0f)
        )
        assertMapping(
            from = Offset(-10f, 100f),
            to = Offset(100f, 10f)
        )
    }

    @Test
    fun rotationZ_bottom_right_origin() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            rotationZ = 90f,
            transformOrigin = TransformOrigin(1f, 1f)
        )

        assertMapping(
            from = Offset(110f, -90f),
            to = Offset(0f, 0f)
        )
        assertMapping(
            from = Offset(100f, 10f),
            to = Offset(100f, 10f)
        )
    }

    @Test
    fun translation_scale_left_top_origin() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            translationX = 60f,
            translationY = 7f,
            scaleX = 2f,
            scaleY = 4f,
            transformOrigin = TransformOrigin(0f, 0f)
        )

        assertMapping(
            from = Offset(60f, 7f),
            to = Offset(0f, 0f)
        )
        assertMapping(
            from = Offset(260f, 47f),
            to = Offset(100f, 10f)
        )
    }

    @Test
    fun translation_rotationZ_left_top_origin() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            translationX = 60f,
            translationY = 7f,
            rotationZ = 90f,
            transformOrigin = TransformOrigin(0f, 0f)
        )

        assertMapping(
            from = Offset(60f, 7f),
            to = Offset(0f, 0f)
        )
        assertMapping(
            from = Offset(50f, 107f),
            to = Offset(100f, 10f)
        )
    }

    @Test
    fun translation_rotationX_left_top_origin() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            translationX = 60f,
            translationY = 7f,
            rotationX = 45f,
            transformOrigin = TransformOrigin(0f, 0f),
            cameraDistance = Float.MAX_VALUE
        )

        val y = 10 * cos45
        assertMapping(
            from = Offset(60f, 7f),
            to = Offset(0f, 0f)
        )
        assertMapping(
            from = Offset(160f, 7f + y),
            to = Offset(100f, 10f)
        )
    }

    @Test
    fun translation_rotationY_left_top_origin() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            translationX = 60f,
            translationY = 7f,
            rotationY = 45f,
            transformOrigin = TransformOrigin(0f, 0f),
            cameraDistance = Float.MAX_VALUE
        )

        val x = 100 * cos45
        assertMapping(
            from = Offset(60f, 7f),
            to = Offset(0f, 0f)
        )
        assertMapping(
            from = Offset(x + 60f, 17f),
            to = Offset(100f, 10f)
        )
    }

    @Test
    fun scale_rotationZ_left_top_origin() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            scaleX = 2f,
            scaleY = 4f,
            rotationZ = 90f,
            transformOrigin = TransformOrigin(0f, 0f)
        )

        assertMapping(
            from = Offset(0f, 0f),
            to = Offset(0f, 0f)
        )
        assertMapping(
            from = Offset(-40f, 200f),
            to = Offset(100f, 10f)
        )
    }

    @Test
    fun translation_scale_rotationZ_left_top_origin() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            translationX = 60f,
            translationY = 7f,
            scaleX = 2f,
            scaleY = 4f,
            rotationZ = 90f,
            transformOrigin = TransformOrigin(0f, 0f)
        )

        assertMapping(
            from = Offset(60f, 7f),
            to = Offset(0f, 0f)
        )
        assertMapping(
            from = Offset(20f, 207f),
            to = Offset(100f, 10f)
        )
    }

    @Test
    fun is_in_layer() {
        layer.resize(IntSize(0, 0))
        layer.updateProperties(
            clip = false
        )

        assertTrue(layer.isInLayer(Offset(-1f, -1f)))
        assertTrue(layer.isInLayer(Offset(0f, 0f)))
        assertTrue(layer.isInLayer(Offset(1f, 1f)))

        layer.resize(IntSize(0, 0))
        layer.updateProperties(
            clip = true
        )

        assertFalse(layer.isInLayer(Offset(-1f, -1f)))
        assertFalse(layer.isInLayer(Offset(0f, 0f)))
        assertFalse(layer.isInLayer(Offset(1f, 1f)))

        layer.resize(IntSize(0, 0))
        layer.updateProperties(
            clip = true,
            shape = CircleShape
        )

        assertFalse(layer.isInLayer(Offset(-1f, -1f)))
        assertFalse(layer.isInLayer(Offset(0f, 0f)))
        assertFalse(layer.isInLayer(Offset(1f, 1f)))

        layer.resize(IntSize(1, 2))
        layer.updateProperties(
            clip = true,
            size = Size(1f, 2f)
        )

        assertFalse(layer.isInLayer(Offset(-1f, -1f)))
        assertTrue(layer.isInLayer(Offset(0f, 0f)))
        assertTrue(layer.isInLayer(Offset(0f, 1f)))
        assertFalse(layer.isInLayer(Offset(0f, 2f)))
        assertFalse(layer.isInLayer(Offset(1f, 0f)))

        layer.resize(IntSize(100, 200))
        layer.updateProperties(
            clip = true,
            shape = CircleShape,
            size = Size(100f, 200f)
        )

        assertFalse(layer.isInLayer(Offset(5f, 5f)))
        assertFalse(layer.isInLayer(Offset(95f, 195f)))
        assertTrue(layer.isInLayer(Offset(50f, 100f)))
    }

    // we run in UI thread to not be in concurrent with GlobalSnapshotManager
    // we have to be non-concurrent with it to not have races with sendApplyNotifications
    // it is currently hard to isolate Snapshot changes
    // because SnapshotStateObserver/sendApplyNotifications are coupled with global state
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun invalidate_on_state_change() = runComposeUiTest {
        runOnUiThread {
            var drawCount = 0
            var invalidateCount = 0
            var state by mutableStateOf(0f)
            val layer = TestRenderNodeLayer(
                invalidateBlock = {
                    invalidateCount++
                },
                drawBlock = { canvas, _ ->
                    canvas.drawLine(Offset(0f, state), Offset.Zero, Paint())
                    drawCount++
                },
            )

            val surface = Surface.makeRasterN32Premul(100, 100)
            val canvas = surface.canvas
            val stateObserver = SnapshotStateObserver { it.invoke() }

            fun draw() {
                stateObserver.observeReads(Unit, { layer.invalidate() }) {
                    layer.drawLayer(canvas.asComposeCanvas(), null)
                }
            }

            try {
                stateObserver.start()

                draw()
                Snapshot.sendApplyNotifications()
                assertEquals(0, invalidateCount)
                assertEquals(1, drawCount)  // no cache, draw

                draw()
                Snapshot.sendApplyNotifications()
                assertEquals(0, invalidateCount)
                assertEquals(1, drawCount) // no draw, as it is cached

                state = 1f
                Snapshot.sendApplyNotifications()
                assertEquals(1, invalidateCount)  // invalidate, as draw state changed
                assertEquals(1, drawCount)

                state = 2f
                Snapshot.sendApplyNotifications()
                assertEquals(2, invalidateCount)  // invalidate, as draw state changed again
                assertEquals(1, drawCount)

                draw()
                Snapshot.sendApplyNotifications()
                assertEquals(2, invalidateCount)
                assertEquals(2, drawCount)  // draw, because we invalidated and cache cleared

                draw()
                Snapshot.sendApplyNotifications()
                assertEquals(2, invalidateCount)
                assertEquals(2, drawCount) // no draw, as it is cached
            } finally {
                stateObserver.stop()
                stateObserver.clear()
            }
        }
    }

    @Test
    fun destroyReleasesGraphicsLayer() {
        val layer = TestRenderNodeLayer()
        assertTrue(graphicsContext!!.layers.contains(layer.graphicsLayer))
        layer.destroy()
        assertFalse(graphicsContext!!.layers.contains(layer.graphicsLayer))
    }

    private var graphicsContext: TestGraphicsContext? = null

    private class TestGraphicsContext : GraphicsContext {
        private val skiaContext = SkiaGraphicsContext()
        val layers = mutableListOf<GraphicsLayer>()

        override fun createGraphicsLayer(): GraphicsLayer {
            return skiaContext.createGraphicsLayer().also {
                layers += it
            }
        }

        override fun releaseGraphicsLayer(layer: GraphicsLayer) {
            layers -= layer
            skiaContext.releaseGraphicsLayer(layer)
        }
    }

    private fun TestRenderNodeLayer(
        invalidateBlock: () -> Unit = {},
        drawBlock: (Canvas, GraphicsLayer?) -> Unit = { _, _ -> },
    ): GraphicsLayerOwnerLayer {
        if (graphicsContext == null) {
            graphicsContext = TestGraphicsContext()
        }
        return GraphicsLayerOwnerLayer(
            graphicsLayer = graphicsContext!!.createGraphicsLayer(),
            context = graphicsContext,
            layerManager = object : OwnedLayerManager {
                override fun createLayer(
                    drawBlock: (canvas: Canvas, parentLayer: GraphicsLayer?) -> Unit,
                    invalidateParentLayer: () -> Unit,
                    explicitLayer: GraphicsLayer?
                ): OwnedLayer = throw NotImplementedError()
                override fun recycle(layer: OwnedLayer) = false
                override fun notifyLayerIsDirty(layer: OwnedLayer, isDirty: Boolean) = Unit

                override fun invalidate() {
                    invalidateBlock()
                }
            },
            drawBlock = drawBlock,
            invalidateParentLayer = {}
        )
    }

    private fun assertMapping(from: Offset, to: Offset) {
        assertEquals(from, matrix.map(to), 0.001f)
        assertEquals(to, inverseMatrix.map(from), 0.001f)
    }

    private fun assertEquals(expected: Offset, actual: Offset, absoluteTolerance: Float) {
        val message = "Expected <$expected>, actual <$actual>."
        assertEquals(expected.x, actual.x, absoluteTolerance, message)
        assertEquals(expected.y, actual.y, absoluteTolerance, message)
    }

    private fun OwnedLayer.updateProperties(
        scaleX: Float = 1f,
        scaleY: Float = 1f,
        alpha: Float = 1f,
        translationX: Float = 0f,
        translationY: Float = 0f,
        shadowElevation: Float = 0f,
        ambientShadowColor: Color = DefaultShadowColor,
        spotShadowColor: Color = DefaultShadowColor,
        rotationX: Float = 0f,
        rotationY: Float = 0f,
        rotationZ: Float = 0f,
        cameraDistance: Float = DefaultCameraDistance,
        transformOrigin: TransformOrigin = TransformOrigin.Center,
        shape: Shape = RectangleShape,
        clip: Boolean = false,
        renderEffect: RenderEffect? = null,
        compositingStrategy: CompositingStrategy = CompositingStrategy.Auto,
        size: Size = Size.Zero
    ) {
        val scope = ReusableGraphicsLayerScope()
        scope.cameraDistance = cameraDistance
        scope.scaleX = scaleX
        scope.scaleY = scaleY
        scope.alpha = alpha
        scope.translationX = translationX
        scope.translationY = translationY
        scope.shadowElevation = shadowElevation
        scope.ambientShadowColor = ambientShadowColor
        scope.spotShadowColor = spotShadowColor
        scope.rotationX = rotationX
        scope.rotationY = rotationY
        scope.rotationZ = rotationZ
        scope.cameraDistance = cameraDistance
        scope.transformOrigin = transformOrigin
        scope.shape = shape
        scope.clip = clip
        scope.renderEffect = renderEffect
        scope.compositingStrategy = compositingStrategy
        scope.layoutDirection = LayoutDirection.Ltr
        scope.graphicsDensity = Density(1f)
        scope.outline = shape.createOutline(size, scope.layoutDirection, scope.graphicsDensity)
        updateLayerProperties(scope)
    }
}
