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

package androidx.compose.ui.interop.arkc

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.arkui.pointerInteropFilter
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.interop.ExtNodeId
import androidx.compose.ui.interop.LocalArkUIInteropContext
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.napi.JsEnv
import androidx.compose.ui.napi.call
import androidx.compose.ui.napi.get
import androidx.compose.ui.platform.LocalEnableCApi
import androidx.compose.ui.platform.LocalFrameManager
import androidx.compose.ui.platform.LocalRootNodeId
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.FrameManager
import org.jetbrains.skiko.ExternalRenderNode
import platform.arkui.ArkUI_NodeHandle
import platform.ohos.napi_ref

private val STUB_CALLBACK_WITH_RECEIVER: Any.() -> Unit = {}

@Composable
fun ArkUINodeHandleNew(
    factory: () -> ArkUI_NodeHandle,
    modifier: Modifier,
    update: (ArkUI_NodeHandle) -> Unit = STUB_CALLBACK_WITH_RECEIVER,
    background: Color = Color.Unspecified,
    onRelease: (ArkUI_NodeHandle) -> Unit = STUB_CALLBACK_WITH_RECEIVER,
    interactive: Boolean = true,
) {
    ArkUINativeViewArkUIBacked(
        factory = { ArkUINativeView(factory()) },
        modifier = modifier,
        update = { update(it.handle) },
        background = background,
        onRelease = { onRelease(it.handle) },
        interactive = interactive,
    )
}

/**
 * @param factory The block creating the [ArkUINativeView] to be composed.
 * @param modifier The modifier to be applied to the layout. Size should be specified in modifier.
 * Modifier may contains crop() modifier with different shapes.
 * @param update A callback to be invoked after the layout is inflated.
 * @param background A color of [ArkUINativeView] background wrapping the view created by [factory].
 * @param onRelease A callback invoked as a signal that this view instance has exited the
 * composition hierarchy entirely and will not be reused again. Any additional resources used by the
 * View should be freed at this time.
 * @param interactive If true, then user touches will be passed to this UIView
 */
@Composable
internal fun <T : ArkUINativeView> ArkUINativeViewArkUIBacked(
    factory: () -> T,
    modifier: Modifier,
    update: (T) -> Unit = STUB_CALLBACK_WITH_RECEIVER,
    background: Color = Color.Unspecified,
    onRelease: (T) -> Unit = STUB_CALLBACK_WITH_RECEIVER,
    interactive: Boolean = true,
) {
    val enableCApi = LocalEnableCApi.current
    val rootNodeId = LocalRootNodeId.current
    val interopContext = LocalArkUIInteropContext.current
    var layoutKey by remember { mutableStateOf(0) }
    val embeddedInteropComponent = remember {
        EmbeddedInteropViewNew(onRelease).apply {
           component = factory().also {
               wrappingView.interopView = it
               wrappingView.addChild(it)
            }
            wrappingView.onRequestReMeasure = {
                layoutKey++
            }
            updater = Updater(component, update) {
                interopContext.deferAction(action = it)
            }
        }
    }
    val frameMgr = LocalFrameManager.current
    val painter = remember {
        ArkUINativeViewPainter(
            frameMgr,
            enableCApi,
            rootNodeId,
            ExtNodeId++,
            embeddedInteropComponent.wrappingView.handle
        )
    }
    Place(
        key = layoutKey,
        modifier = modifier.paint(painter).pointerInteropFilter(embeddedInteropComponent.wrappingView),
        measurePolicy = { _, constraints ->
            with(embeddedInteropComponent.wrappingView) {
                measure(constraints)
                val measuredSize = measuredSize
                painter.srcSize = measuredSize
                layout(measuredSize.width, measuredSize.height) {}
            }
        }
    )

    DisposableEffect(Unit) {
        onDispose {
            painter.dispose()
            embeddedInteropComponent.removeFromHierarchy()
        }
    }
    SideEffect {
        embeddedInteropComponent.updater.update = update
    }
}


@Composable
private fun Place(key: Int, modifier: Modifier, measurePolicy: MeasurePolicy) {
    key(key) {
        Layout({}, measurePolicy = measurePolicy, modifier = modifier)
    }
}

private abstract class EmbeddedInteropComponentNew<T : Any>(
    val onRelease: (T) -> Unit
) {
    val wrappingView = InteropWrapper()
    lateinit var component: T
    lateinit var updater: Updater<T>

    abstract fun addToHierarchy()
    abstract fun removeFromHierarchy()

    protected fun addViewToHierarchy(view: ArkUINativeView) {
        view.handle.removeFromParent()
        wrappingView.insertChildAt(view, 0)
    }

    protected fun removeViewFromHierarchy(view: ArkUINativeView) {
        wrappingView.removeChild(view)
        wrappingView.dispose()
        updater.dispose()
        onRelease(component)
    }
}

private class EmbeddedInteropViewNew<T : ArkUINativeView>(
    onRelease: (T) -> Unit
) : EmbeddedInteropComponentNew<T>(onRelease) {
    override fun addToHierarchy() {
        addViewToHierarchy(component)
    }

    override fun removeFromHierarchy() {
        removeViewFromHierarchy(component)
    }
}

class ArkUINativeViewPainter(
    frameMgr: FrameManager,
    enableCApi: Boolean,
    rootNodeId: Int,
    extNodeId: Int,
    handle: ArkUI_NodeHandle,
    private val srcOffset: IntOffset = IntOffset.Zero,
) : Painter() {
    private var externalRenderNode: ExternalRenderNode
    private var nativeViewRef: napi_ref? = JsEnv.createReference(frameMgr.buildNativeView())
    var srcSize: IntSize = IntSize.Zero
    init {
        val node: ExternalRenderNode? = if (enableCApi) {
            ExternalRenderNode.makeFromNativeValue(rootNodeId, extNodeId, handle)
        } else {
            val nodeContent = requireNotNull(nativeViewRef["nodeContent"]) {
                "ArkUINativeViewPainter init failed: empty nodeContent"
            }
            val renderNode = requireNotNull(nativeViewRef.call("getRenderNode")) {
                "ArkUINativeViewPainter init failed: empty renderNode"
            }
            ExternalRenderNode.makeFromNativeValue(nodeContent, handle, renderNode)
        }
        require(node != null) {
            "ArkUINativeViewPainter init failed: empty nativeArkUIView"
        }
        externalRenderNode = node
    }

    internal var filterQuality: FilterQuality = FilterQuality.Low

    private var alpha: Float = 1.0f

    private var colorFilter: ColorFilter? = null

    override fun DrawScope.onDraw() {
        drawRenderNode(externalRenderNode.ptr)
    }

    override val intrinsicSize: Size get() = validateSize(srcOffset, srcSize).toSize()

    override fun applyAlpha(alpha: Float): Boolean {
        this.alpha = alpha
        return true
    }

    override fun applyColorFilter(colorFilter: ColorFilter?): Boolean {
        this.colorFilter = colorFilter
        return true
    }

    private fun validateSize(srcOffset: IntOffset, srcSize: IntSize): IntSize {
        @Suppress("ExceptionMessage")
        require(
            srcOffset.x >= 0 &&
                srcOffset.y >= 0 &&
                srcSize.width >= 0 &&
                srcSize.height >= 0
        )
        return srcSize
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ArkUINativeViewPainter) return false

        if (srcOffset != other.srcOffset) return false
        if (srcSize != other.srcSize) return false
        if (filterQuality != other.filterQuality) return false
        return true
    }

    override fun hashCode(): Int {
        var result = externalRenderNode.hashCode()
        result = 31 * result + srcOffset.hashCode()
        result = 31 * result + srcSize.hashCode()
        result = 31 * result + filterQuality.hashCode()
        return result
    }

    override fun toString(): String {
        return "ArkUINativeViewPainter(externalRenderNode=$externalRenderNode, srcOffset=$srcOffset, srcSize=$srcSize, " +
            "filterQuality=$filterQuality)"
    }

    fun dispose() {
        nativeViewRef.call("dispose")
        JsEnv.deleteReference(nativeViewRef)
        nativeViewRef = null
    }
}
