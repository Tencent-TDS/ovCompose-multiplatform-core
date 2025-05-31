package androidx.compose.ui.platform.v2

import androidx.compose.ui.geometry.MutableRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.boundingRect
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.CanvasType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.DefaultCameraDistance
import androidx.compose.ui.graphics.DefaultShadowColor
import androidx.compose.ui.graphics.Fields
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.ReusableGraphicsLayerScope
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.alphaMultiplier
import androidx.compose.ui.node.OwnedLayer
import androidx.compose.ui.platform.OutlineCache
import androidx.compose.ui.platform.invertTo
import androidx.compose.ui.platform.isInOutline
import androidx.compose.ui.platform.v2.nativefoundation.AdaptiveCanvas
import androidx.compose.ui.platform.v2.nativefoundation.IOSNativeCanvas
import androidx.compose.ui.toCGRect
import androidx.compose.ui.uikit.utils.ITMMCanvasViewProxyProtocol
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toSize
import kotlinx.cinterop.useContents
import platform.CoreFoundation.CFRelease
import platform.CoreGraphics.CGPathCreateWithRect
import platform.CoreGraphics.CGPathCreateWithRoundedRect
import platform.CoreGraphics.CGPathRef
import platform.UIKit.UIColor
import platform.UIKit.UILabel
import platform.UIKit.UIView
import kotlin.math.abs
import kotlin.math.max

/*
 * Tencent is pleased to support the open source community by making ovCompose available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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

private var lastId = 0
private const val DEBUG = false

internal class UIViewLayer(
    private var density: Density,
    private val invalidateParentLayer: () -> Unit,
    private val drawBlock: (Canvas) -> Unit,
    private val onDestroy: () -> Unit = {},
    private val rootView: UIView,
    nativeReusePool: Long = 0,
    clipChildren: Boolean = true
) : OwnedLayer {
    private val id = lastId++
    private var size = IntSize.Zero
    private var position = IntOffset(-1, -1)
    private var outlineCache =
        OutlineCache(density, size, RectangleShape, LayoutDirection.Ltr)

    // Internal for testing
    internal val matrix = Matrix()
    private var _inverseMatrix = Matrix().apply {
        // Mark this as invalid.
        values[0] = Float.NaN
    }
    private val inverseMatrix: Matrix
        get() {
            if (_inverseMatrix.values[0].isNaN()) {
                matrix.invertTo(_inverseMatrix)
            }
            return _inverseMatrix
        }

    private var isDestroyed = false

    private var transformOrigin: TransformOrigin = TransformOrigin.Center
    private var translationX: Float = 0f
    private var translationY: Float = 0f
    private var rotationX: Float = 0f
    private var rotationY: Float = 0f
    private var rotationZ: Float = 0f
    private var cameraDistance: Float = DefaultCameraDistance
    private var scaleX: Float = 1f
    private var scaleY: Float = 1f
    private var alpha: Float = 1f
    private var clip: Boolean = false
    private var renderEffect: RenderEffect? = null
    private var shadowElevation: Float = 0f
    private var ambientShadowColor: Color = DefaultShadowColor
    private var spotShadowColor: Color = DefaultShadowColor
    private var compositingStrategy: CompositingStrategy = CompositingStrategy.Auto

    private val canvas = AdaptiveCanvas(nativeReusePool)
    val viewProxy: ITMMCanvasViewProxyProtocol = canvas.viewProxy
    private var cachedParentLayer: OwnedLayer? = null

    private var isInvalidated = true

    private var superRenderEffect: RenderEffect? = null
    private var mutatedFields: Int = 0

    private var applyRenderEffect: RenderEffect? = null
        set(value) {
            if (value != field) {
                field = value
                canvas.applyRenderEffect(value)
                invalidate()
            }
        }

    init {
        if (clipChildren) {
            this.viewProxy.setClipsToBounds(true)
        }

        if (DEBUG) setupDebugView()
    }

    private fun setupDebugView() {
        val idLabel = UILabel()
        idLabel.text = "$id"
        idLabel.font = idLabel.font.fontWithSize(8.0)
        idLabel.sizeToFit()
        this.viewProxy.addSubview(idLabel)
    }

    override fun destroy() {
        if (!isDestroyed) {
            isDestroyed = true
            onDestroy()
            this.canvas.destroy()
        }
    }

    override fun reuseLayer(drawBlock: (Canvas) -> Unit, invalidateParentLayer: () -> Unit) {
        // TODO: in destroy, call recycle, and reconfigure this layer to be ready to use here.
    }

    override fun resize(size: IntSize) {
        if (size != this.size) {
            this.size = size
            outlineCache.size = size

            val layerWidth = (size.width / density.density)
            val layerHeight = (size.height / density.density)
            // IMPORTANT: set bounds instead of frame to work with transform3D properly.
            // Frame should be ignored when transform is applied.
            // Likely, we update the position of view by updating its center property.
            viewProxy.setBounds(0.0f, 0.0f, layerWidth, layerHeight)
            updateLayerCenter()
            updateMatrix()
            invalidate()
        }
    }

    override fun move(position: IntOffset) {
        if (position != this.position) {
            this.position = position
            updateLayerCenter()

            invalidateParentLayer()
        }
    }

    private fun updateLayerCenter() {
        val centerX = transformOrigin.pivotFractionX * size.width
        val centerPositionX = ((position.x + centerX) / density.density)
        val centerY = transformOrigin.pivotFractionY * size.height
        val centerPositionY = ((position.y + centerY) / density.density)

        // IMPORTANT: updating the position of view with center rather than frame
        // to respect the transformation applied in [updateMatrix].
        viewProxy.setCenter(centerPositionX, centerPositionY)
    }

    override fun mapOffset(point: Offset, inverse: Boolean): Offset {
        return if (inverse) {
            inverseMatrix
        } else {
            matrix
        }.map(point)
    }

    override fun mapBounds(rect: MutableRect, inverse: Boolean) {
        if (inverse) {
            inverseMatrix
        } else {
            matrix
        }.map(rect)
    }

    override fun isInLayer(position: Offset): Boolean {
        if (!clip) {
            return true
        }

        val x = position.x
        val y = position.y
        if (outlineCache.shape === RectangleShape) {
            return 0f <= x && x < size.width && 0f <= y && y < size.height
        }

        return isInOutline(outlineCache.outline, x, y)
    }

    override fun updateLayerProperties(
        scope: ReusableGraphicsLayerScope,
        layoutDirection: LayoutDirection,
        density: Density,
    ) {
        val maybeChangedFields = scope.mutatedFields or mutatedFields
        if (this.transformOrigin != scope.transformOrigin) {
            this.transformOrigin = scope.transformOrigin
            // TODO: move this to updateMatrix together with the updating of anchorPoint
            //  after we properly handle the transitions on the out most draw call.
            updateLayerCenter()
        }
        this.translationX = scope.translationX
        this.translationY = scope.translationY
        this.rotationX = scope.rotationX
        this.rotationY = scope.rotationY
        this.rotationZ = scope.rotationZ
        this.cameraDistance = max(scope.cameraDistance, 0.001f)
        this.scaleX = scope.scaleX
        this.scaleY = scope.scaleY
        this.alpha = scope.alpha
        this.clip = scope.clip
        this.shadowElevation = scope.shadowElevation
        this.density = density
        this.renderEffect = scope.renderEffect
        this.ambientShadowColor = scope.ambientShadowColor
        this.spotShadowColor = scope.spotShadowColor
        this.compositingStrategy = scope.compositingStrategy
        outlineCache.shape = scope.shape
        outlineCache.layoutDirection = layoutDirection
        outlineCache.density = density
        if (maybeChangedFields and Fields.MatrixAffectingFields != 0) {
            updateMatrix()
        }
        mutatedFields = scope.mutatedFields
        this.viewProxy.setAlpha(scope.alpha)
        updateShadow()
    }

    private fun updateMatrix() {
        val pivotX = transformOrigin.pivotFractionX * size.width
        val pivotY = transformOrigin.pivotFractionY * size.height

        matrix.reset()
        // Mark inverseMatrix as invalid. It will be lazy evaluated when accessed.
        _inverseMatrix.values[0] = Float.NaN

        matrix.translate(x = -pivotX, y = -pivotY)
        matrix *= Matrix().apply {
            rotateZ(rotationZ)
            rotateY(rotationY)
            rotateX(rotationX)
            scale(scaleX, scaleY)
        }

        // 记录iOSMatrix变化需要用到的属性，对齐Compose原生
        var m34Transform = 0.0
        // Perspective transform should be applied only in case of rotations to avoid
        // multiply application in hierarchies.
        // See Android's frameworks/base/libs/hwui/RenderProperties.cpp for reference
        if (!rotationX.isZero() || !rotationY.isZero()) {
            // The camera location is passed in inches, set in pt
            val depth = cameraDistance * 72f
            val value = -1f / depth
            matrix *= Matrix().apply {
                this[2, 3] = value
            }
            m34Transform = value.toDouble()
        }
        matrix *= Matrix().apply {
            translate(x = pivotX + translationX, y = pivotY + translationY)
        }

        // Third column and row are irrelevant for 2D space.
        // Zeroing required to get correct inverse transformation matrix.
        matrix[2, 0] = 0f
        matrix[2, 1] = 0f
        matrix[2, 3] = 0f
        matrix[0, 2] = 0f
        matrix[1, 2] = 0f
        matrix[3, 2] = 0f
        this.viewProxy.setAnchorPoint(transformOrigin.pivotFractionX, transformOrigin.pivotFractionY)

        // 将Matrix参数传递到OC侧计算矩阵
        canvas.applyTransformMatrix(
            rotationX,
            rotationY,
            rotationZ,
            scaleX,
            scaleY,
            translationX / density.density,
            translationY / density.density,
            m34Transform
        )
    }

    override fun invalidate() {
        if (!isDestroyed && !isInvalidated) {
            isInvalidated = true
            invalidateParentLayer()
        }
    }

    override fun drawLayer(canvas: Canvas) {
        if (isInvalidated) {
            isInvalidated = false
            val bounds = size.toSize().toRect()
            this.canvas.onPreDraw()
            performDrawLayer(this.canvas, bounds)
            this.canvas.onPostDraw()
        }

        if (canvas is IOSNativeCanvas) {
            canvas.drawLayerWithNativeCanvas(this.canvas)
        } else {
            // Basically, this only happens to the direct child view of the root view.
            viewProxy.bringSelfToFront()
        }
    }

    override fun transform(matrix: Matrix) {
        matrix.timesAssign(this.matrix)
    }

    override fun inverseTransform(matrix: Matrix) {
        matrix.timesAssign(inverseMatrix)
    }

    private fun performDrawLayer(canvas: IOSNativeCanvas, bounds: Rect) {
      // 这里去掉 alpha > 0 的绘制条件判断（无论是否为0绘制应该触发），否则会丢掉 alpha 初始值为 0， 且后不调用 invalidate（） 的节点绘制
        if (clip) {
            canvas.save()
            when (val outline = outlineCache.outline) {
                is Outline.Rectangle -> canvas.clipRect(outline.rect)
                is Outline.Rounded -> canvas.clipRoundRect(outline.roundRect)
                is Outline.Generic -> canvas.clipPath(outline.path)
            }
        } else {
            canvas.clearClip()
        }

        val currentRenderEffect = renderEffect
        val requiresLayer =
            (alpha < 1 && compositingStrategy != CompositingStrategy.ModulateAlpha) ||
                    currentRenderEffect != null ||
                    compositingStrategy == CompositingStrategy.Offscreen
        if (requiresLayer) {
            canvas.saveLayer(
                bounds,
                Paint().apply {
                    alpha = this@UIViewLayer.alpha
                    asFrameworkPaint().imageFilter = currentRenderEffect?.asSkiaImageFilter()
                }
            )
        } else {
            canvas.save()
        }

        if (canvas.canvasType == CanvasType.Skia) {
            canvas.alphaMultiplier =
                if (compositingStrategy == CompositingStrategy.ModulateAlpha) {
                    alpha
                } else {
                    1.0f
                }
        }

        drawBlock(canvas)
        canvas.restore()
        if (clip) {
            canvas.restore()
        }
    }

    override fun updateDisplayList() = Unit

    private fun updateShadow() {
        if (shadowElevation > 0) {
            val outline = outlineCache.outline
            val shadowRadius =  if (outline is Outline.Rounded) {
                outline.roundRect.topLeftCornerRadius.x
            }  else 0.0f

            // Process density consistently in Objective-C
            this.viewProxy.setShadowWithElevation(
                shadowElevation = shadowElevation,
                shadowRadius = shadowRadius,
                shadowColorRed = spotShadowColor.red,
                shadowColorBlue = spotShadowColor.blue,
                shadowColorGreen = spotShadowColor.green,
                shadowColorAlpha = spotShadowColor.alpha
            )
        } else {
            this.viewProxy.clearShadow()
        }
    }

    override fun setPlaced(isPlaced: Boolean) {
        this.viewProxy.setHidden(!isPlaced)
    }

    override fun updateParentLayer(parentLayer: OwnedLayer?) {
        val parentViewLayer = (parentLayer as? UIViewLayer)
        if (parentViewLayer != null) {
            if (cachedParentLayer != parentViewLayer) {
                cachedParentLayer = parentViewLayer
                viewProxy.setParent(parentViewLayer.viewProxy)
            }
            didUpdateParentLayer(parentViewLayer)
        } else {
            viewProxy.attachToRootView(rootView)
        }
    }

    private inline fun didUpdateParentLayer(superLayer: UIViewLayer) {
        superRenderEffect = superLayer.renderEffect ?: superLayer.superRenderEffect
        applyRenderEffect = renderEffect ?: superRenderEffect
    }

    override fun toString(): String = "UIViewLayer(@$id)"
}

// Copy from Android's frameworks/base/libs/hwui/utils/MathUtils.h
private const val NON_ZERO_EPSILON = 0.001f
private inline fun Float.isZero(): Boolean = abs(this) <= NON_ZERO_EPSILON

private fun UIView.toDebugString(): String {
    val center = center.useContents { x to y }
    val size = bounds.useContents { size.width to size.height }
    return "UIView(${center}, $size)"
}