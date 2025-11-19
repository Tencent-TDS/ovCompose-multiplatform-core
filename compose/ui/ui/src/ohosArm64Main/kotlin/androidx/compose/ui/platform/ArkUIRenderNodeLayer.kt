/*
 * Copyright 2025 The Android Open Source Project
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

import androidx.compose.common.interop.LogPrintUtil
import androidx.compose.common.interop.TraceUtil
import androidx.compose.ui.geometry.MutableRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
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
import androidx.compose.ui.platform.nativefoundation.AdaptiveCanvas
import androidx.compose.ui.platform.nativefoundation.OHOSNativeCanvas
import androidx.compose.ui.node.LayerSourceType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toSize
import kotlinx.cinterop.COpaquePointer
import kotlin.math.abs
import kotlin.math.max
import androidx.compose.ui.platform.nativefoundation.OHNativeCanvasProxy

private var lastId = 0

internal class ArkUIRenderNodeLayer(
    private var density: Density,
    private val invalidateParentLayer: () -> Unit,
    private val drawBlock: (Canvas) -> Unit,
    private val onDestroy: () -> Unit = {},
    nativeCanvasFactory: COpaquePointer,
    private val sourceType: LayerSourceType = LayerSourceType.REGULAR
): OwnedLayer {
    // ---- 基础属性 ----
    // 当前层的尺寸（宽度和高度），以像素为单位。通过 resize 方法更新
    private var size = IntSize.Zero
    // 当前层的位置（x 和 y 坐标），以像素为单位。初始值为 (-1, -1)，通过 move 方法更新
    private var position = IntOffset(-1, -1)
    // 用于缓存当前层的轮廓信息（如形状，布局方向，密度等），以优化绘制性能
    private var outlineCache =
        OutlineCache(density, size, RectangleShape, LayoutDirection.Ltr)
    // ----- 基础属性 ----

    // ---- 矩阵与变换相关 ----
    // Internal for testing
    // 当前层的变换矩阵，用于表示平移、旋转、缩放等几何变换操作
    internal val matrix = Matrix()
    private var _inverseMatrix = Matrix().apply {
        // Mark this as invalid.
        values[0] = Float.NaN
    }
    // 当前层的逆变换矩阵，用于将屏幕坐标转换回层的本地坐标。初始值标记为无效（NaN），在访问时懒加载计算。
    private val inverseMatrix: Matrix
        get() {
            if (_inverseMatrix.values[0].isNaN()) {
                matrix.invertTo(_inverseMatrix)
            }
            return _inverseMatrix
        }
    // 当前层的变换原点，决定旋转、缩放等操作的参考点。默认值为中心点。
    private var transformOrigin: TransformOrigin = TransformOrigin.Center
    // 当前层在 x 和 y 方向的平移距离，以像素为单位。默认值为 0。
    private var translationX: Float = 0f
    private var translationY: Float = 0f
    // 当前层绕 x、y、z 轴的旋转角度，以度为单位。默认值为 0。
    private var rotationX: Float = 0f
    private var rotationY: Float = 0f
    private var rotationZ: Float = 0f
    // 当前层的摄像机距离，影响 3D 变换的透视效果。默认值为系统定义的 DefaultCameraDistance。
    private var cameraDistance: Float = DefaultCameraDistance
    // 当前层在 x 和 y 方向的缩放比例。默认值为 1（不缩放）。
    private var scaleX: Float = 1f
    private var scaleY: Float = 1f
    // ---- 矩阵与变换相关 ----

    // ---- 视觉属性 ----
    // 当前层的透明度，范围为 0（完全透明）到 1
    private var alpha: Float = 1f
    // 是否裁剪超出边界的内容。默认值为 false（不裁剪）。
    private var clip: Boolean = false
    // 当前层的渲染效果，如模糊、阴影等。默认值为 null（无效果）。
    private var renderEffect: RenderEffect? = null
    // 当前层的阴影属性，包括阴影高度、环境光颜色和聚光灯颜色。
    private var shadowElevation: Float = 0f
    // 环境阴影颜色和聚光阴影颜色
    private var ambientShadowColor: Color = DefaultShadowColor
    private var spotShadowColor: Color = DefaultShadowColor
    // 当前层的合成测量，决定如何将层内容与父层合成，如是否开启离屏渲染等。默认值为 Auto。
    private var compositingStrategy: CompositingStrategy = CompositingStrategy.Auto
    // ---- 视觉属性 ----

    // ---- 绘制相关 ----
    // 当前层的画布对象，用于绘制内容
    private val canvas = AdaptiveCanvas(nativeCanvasFactory, sourceType)

    // 当前层的视图代理，用于与底层平台（iOS的UIView）交互
    val nativeCanvasProxy: OHNativeCanvasProxy = canvas.nativeCanvasProxy
    // 当前层的父层缓存，用于优化层级关系的更新
    private var cachedParentLayer: OwnedLayer? = null
    // 标记当前层是否需要重慧
    private var isInvalidated = true
    // 父层的渲染效果，用于继承父层的视觉效果
    private var superRenderEffect: RenderEffect? = null
    // 标记当前层的哪些属性发生了变化，用于优化更新逻辑
    private var mutatedFields: Int = 0
    // ---- 绘制相关 ----

    // ---- 调试相关 ----
    // 当前 ArkUIRenderNodeLayer 的唯一标识符，由静态变量 lastId 自增生成。用于调试或区分不同的 ArkUIRenderNodeLayer 实例。
    private val id = lastId++
    // 仅用于调试，标记当前层是否已被销毁，防止重复销毁操作
    private var isDestroyed = false
    // 当前层应用的渲染效果，设置时会触发重绘
    private var applyRenderEffect: RenderEffect? = null
        set(value) {
            if (value != field) {
                field = value
                canvas.applyRenderEffect(value)
                invalidate()
            }
        }

//    init {
//        if (clipChildren) {
//            nativeCanvasProxy.setMasksToBounds(true)
//        }
//    }

    override fun updateLayerProperties(
        scope: ReusableGraphicsLayerScope,
        layoutDirection: LayoutDirection,
        density: Density
    ) {
        TraceUtil.traceSync("ArkUIRenderNodeLayer:updateLayerProperties") {
            val maybeChangedFields = scope.mutatedFields or mutatedFields
            if (this.transformOrigin != scope.transformOrigin) {
                this.transformOrigin = scope.transformOrigin
                // TODO: move this to updateMatrix together with the updating of anchorPoint
                //  after we properly handle the transitions on the out most draw call.
                updateLayerPosition()
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
            this.nativeCanvasProxy.setOpacity(scope.alpha)
            updateShadow()
        }
    }

    private fun updateShadow() {
        if (shadowElevation > 0) {
            val outline = outlineCache.outline
            val shadowRadius =  if (outline is Outline.Rounded) {
                outline.roundRect.topLeftCornerRadius.x
            }  else 0.0f

            // Process density consistently in Objective-C
//            TODO this.nativeCanvasProxy.setShadowWithElevation(
//                shadowElevation = shadowElevation,
//                shadowRadius = shadowRadius,
//                shadowColorRed = spotShadowColor.red,
//                shadowColorBlue = spotShadowColor.blue,
//                shadowColorGreen = spotShadowColor.green,
//                shadowColorAlpha = spotShadowColor.alpha
//            )
        } else {
            // TODO: this.nativeCanvasProxy.clearShadow()
        }
    }

    override fun isInLayer(position: Offset): Boolean {
        val x = position.x
        val y = position.y
        if (outlineCache.shape === RectangleShape) {
            return 0f <= x && x < size.width && 0f <= y && y < size.height
        }

        return isInOutline(outlineCache.outline, x, y)
    }

    override fun move(position: IntOffset) {
        TraceUtil.traceSync("ArkUIRenderNodeLayer:move") {
            if (position != this.position) {
                this.position = position
                updateLayerPosition()
                invalidateParentLayer()
            }
        }
    }

    override fun resize(size: IntSize) {
        TraceUtil.traceSync("ArkUIRenderNodeLayer:resize") {
            if (size != this.size) {
                this.size = size  // ← 关键修复：更新 size 变量
                outlineCache.size = size
                nativeCanvasProxy.setBounds(position.x, position.y, size.width, size.height)
                updateMatrix()
                invalidate()
            }
        }
    }

    private fun updateLayerPosition() {
        nativeCanvasProxy.setPosition(position.x, position.y)
    }

    override fun drawLayer(canvas: Canvas) {
        TraceUtil.traceSync("ArkUIRenderNodeLayer:drawLayer") {
            if (isInvalidated) {
                isInvalidated = false
                val bounds = size.toSize().toRect()
                this.canvas.onPreDraw()
                performDrawLayer(this.canvas, bounds)
                this.canvas.onPostDraw()
            }

            if (canvas is OHOSNativeCanvas) {
                canvas.drawLayerWithNativeCanvas(this.canvas)
            } else {
                // TODO this.nativeCanvasProxy.bringSelfToFroun()
            }
        }
    }

    private fun performDrawLayer(canvas: OHOSNativeCanvas, bounds: Rect) {
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
                    alpha = this@ArkUIRenderNodeLayer.alpha
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

    override fun updateDisplayList() {
        // TODO("Not yet implemented")
        LogPrintUtil.verbose { "ArkUIRenderNodeLayer::updateDisplayList" }
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

        // 记录Matrix变化需要用到的属性，对齐Compose原生
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
        this.nativeCanvasProxy.setPivot(transformOrigin.pivotFractionX, transformOrigin.pivotFractionY)

        // 将Matrix参数传递到C侧计算矩阵
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

    override fun destroy() {
        if (!isDestroyed) {
            isDestroyed = true
            onDestroy()
            this.canvas.destroy()
        }
    }

    override fun mapOffset(
        point: Offset,
        inverse: Boolean
    ): Offset {
        return if (inverse) {
            inverseMatrix
        } else {
            matrix
        }.map(point)
    }

    override fun mapBounds(
        rect: MutableRect,
        inverse: Boolean
    ) {
        if (inverse) {
            inverseMatrix
        } else {
            matrix
        }.map(rect)
    }

    override fun reuseLayer(
        drawBlock: (Canvas) -> Unit,
        invalidateParentLayer: () -> Unit
    ) {
        // Note: Currently this is just a placeholder as LazyColumn might not be destroying layers
        // If layers are being destroyed and reused, we would need to:
        // 1. Reset isDestroyed flag
        // 2. Update callbacks
        // 3. Re-invalidate
    }

    override fun transform(matrix: Matrix) {
        matrix.timesAssign(this.matrix)
    }

    override fun inverseTransform(matrix: Matrix) {
        matrix.timesAssign(inverseMatrix)
    }

    override fun updateParentLayer(parentLayer: OwnedLayer?) {
        val parentViewLayer = (parentLayer as? ArkUIRenderNodeLayer)
        if (parentViewLayer != null) {
            if (cachedParentLayer != parentViewLayer) {
                cachedParentLayer = parentViewLayer
                nativeCanvasProxy.setParent(parentViewLayer.nativeCanvasProxy)
            }
            didUpdateParentLayer(parentViewLayer)
        } else {
            nativeCanvasProxy.attachToRootView()
        }
    }

    override fun setPlaced(isPlaced: Boolean) {
        // TODO("Not yet implemented")
        LogPrintUtil.verbose { "ArkUIRenderNodeLayer::setPlaced" }
    }

    override fun getSourceType(): LayerSourceType = sourceType

    /**
     * Helper method to check if this layer was created by a LazyList item.
     * This is useful for implementing special pooling strategies on OHOS platform.
     */
    fun isLazyListItem(): Boolean = sourceType == LayerSourceType.LAZY_LIST_ITEM

    private inline fun didUpdateParentLayer(superLayer: ArkUIRenderNodeLayer) {
        superRenderEffect = superLayer.renderEffect ?: superLayer.superRenderEffect
        applyRenderEffect = renderEffect ?: superRenderEffect
    }
}


// Copy from Android's frameworks/base/libs/hwui/utils/MathUtils.h
private const val NON_ZERO_EPSILON = 0.001f
private inline fun Float.isZero(): Boolean = abs(this) <= NON_ZERO_EPSILON