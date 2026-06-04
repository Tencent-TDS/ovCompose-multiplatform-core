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

package androidx.compose.ui.interop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.arkui.ArkUIView
import androidx.compose.ui.arkui.ArkUIViewContainer
import androidx.compose.ui.arkui.pointerInteropFilter
import androidx.compose.ui.arkui.trackUIKitInterop
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.isDebugLogEnabled
import androidx.compose.ui.graphics.kLog
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.napi.JsObject
import androidx.compose.ui.napi.call
import androidx.compose.ui.napi.js
import androidx.compose.ui.napi.nApiValue
import androidx.compose.ui.napi.set
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalEnableCApi
import androidx.compose.ui.platform.LocalFrameManager
import androidx.compose.ui.platform.LocalRootNodeId
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.util.trace
import org.jetbrains.skiko.ExternalRenderNode

private val STUB_CALLBACK_WITH_RECEIVER: Any.() -> Unit = {}

private const val MaxLayoutDimensionInList = 4096

internal var ExtNodeId = 0

/**
 * @param factory The block creating the [ArkUIView] to be composed.
 * @param modifier The modifier to be applied to the layout. Size should be specified in modifier.
 * Modifier may contains crop() modifier with different shapes.
 * @param update A callback to be invoked after the layout is inflated.
 * @param background A color of [ArkUIView] background wrapping the view created by [factory].
 * @param onRelease A callback invoked as a signal that this view instance has exited the
 * composition hierarchy entirely and will not be reused again. Any additional resources used by the
 * View should be freed at this time.
 * @param onResize May be used to custom resize logic.
 * @param interactive If true, then user touches will be passed to this ArkUIView
 */
@Composable
internal fun ArkUIViewArkUIBacked(
    name: String,
    modifier: Modifier,
    parameter: JsObject = js(),
    update: (JsObject) -> Unit = STUB_CALLBACK_WITH_RECEIVER,
    background: Color = Color.Unspecified,
    updater: (ArkUIView) -> Unit = STUB_CALLBACK_WITH_RECEIVER,
    onCreate: (ArkUIView) -> Unit = STUB_CALLBACK_WITH_RECEIVER,
    onRelease: (ArkUIView) -> Unit = STUB_CALLBACK_WITH_RECEIVER,
    interactive: Boolean = true,
    adaptiveParams: AdaptiveParams? = null,
    tag: String? = null,
) {
    val frameMgr = LocalFrameManager.current
    val density = LocalDensity.current.density
    val enableCApi = LocalEnableCApi.current
    val rootNodeId = LocalRootNodeId.current
    val interopContext = LocalArkUIInteropContext.current

    // A state to notify relayout. Any better way to handle this?
    var layoutKey by remember { mutableStateOf(0) }

    val embeddedInteropComponent = remember {
        val component = ArkUIView(
            name, parameter,
            onMeasured = { width, height ->
                layoutKey++
            },
            composeParameterUpdater = {
                // Need more work. JsObject will be invalid when recomposing.
                update(it)
            }
        )
        EmbeddedInteropViewNew(onRelease, component).also {
            frameMgr.buildArkUIView(component)
            it.container.arkUIView = it.component
            onCreate(component)
        }
    }

    val painter = remember {
        ArkUIViewPainter(embeddedInteropComponent, enableCApi, rootNodeId, ExtNodeId++)
    }
    Place(
        key = layoutKey,
        modifier = modifier.paint(painter)
            .trackUIKitInterop(embeddedInteropComponent.container).let {
                if (interactive && LocalEnableCApi.current) {
                    it.pointerInteropFilter(embeddedInteropComponent.container)
                } else {
                    it
                }
            },
        measurePolicy = { _, constraints ->
            val localConstraints = Constraints(
                minWidth = constraints.minWidth,
                maxWidth = constraints.maxWidth,
                minHeight = constraints.minHeight,
                maxHeight = adaptiveParams?.maxHeight?.toPx(density) ?: constraints.maxHeight
            )
            if (isDebugLogEnabled) {
                kLog(
                    "ArkUIView measure, tag=$tag, id=${embeddedInteropComponent.component.id}" +
                        ", constraints=$constraints, localConstraints=$localConstraints"
                )
            }
            trace("ArkUIViewMeasure") {
                embeddedInteropComponent.component.measure(localConstraints, density)
            }

            // If already measured, use the measure result from ArkUI.
            // This is designed to handle wrap content.
            val intrinsicMeasuredWidth =
                embeddedInteropComponent.component.measuredWidth?.toInt()
            val intrinsicMeasuredHeight =
                embeddedInteropComponent.component.measuredHeight?.toInt()

            val width = intrinsicMeasuredWidth?.coerceIn(constraints.minWidth, constraints.maxWidth)
                ?: constraints.maxWidth
            val height = intrinsicMeasuredHeight?.coerceIn(constraints.minHeight, constraints.maxHeight)
                ?: constraints.maxHeight

            // Make sure the layout size is bounded.
            val boundedMaxWidth =
                if (constraints.hasBoundedWidth) constraints.maxWidth else MaxLayoutDimensionInList
            val boundedMaxHeight =
                if (constraints.hasBoundedHeight) constraints.maxHeight else MaxLayoutDimensionInList

            val layoutWidth: Int = width.coerceAtMost(boundedMaxWidth)

            val layoutHeight: Int = adaptiveParams?.run {
                height.limitSizeWhenExceeds(this.maxHeight.toPx(density), this.translateHeight?.toPx(density))
            } ?: height.coerceAtMost(boundedMaxHeight)

            if (isDebugLogEnabled) {
                kLog(
                    "ArkUIView measurePolicy, tag=$tag, id=${embeddedInteropComponent.component.id}" +
                        ", width=$width, height=$height" +
                        ", layoutWidth=$layoutWidth, layoutHeight=$layoutHeight" +
                        ", intrinsicMeasuredWidth=$intrinsicMeasuredWidth, intrinsicMeasuredHeight=$intrinsicMeasuredHeight" +
                        ", constraints.maxWidth=$constraints" +
                        ", adaptiveParams=$adaptiveParams"
                )
            }

            layout(
                layoutWidth,
                layoutHeight
            ) {}
        }
    )

    DisposableEffect(Unit) {
        embeddedInteropComponent.updater = Updater(embeddedInteropComponent.component, updater) {
            interopContext.deferAction(action = it)
        }
        onDispose {
            embeddedInteropComponent.dispose()
        }
    }

    LaunchedEffect(parameter) {
        interopContext.deferAction {
            embeddedInteropComponent.component.update(parameter)
        }
    }

    SideEffect {
        embeddedInteropComponent.updater.update = updater
    }
}

private class EmbeddedInteropViewNew(
    val onRelease: (ArkUIView) -> Unit,
    val component: ArkUIView
) {
    val container = ArkUIViewContainer()

    lateinit var updater: Updater<ArkUIView>

    fun dispose() {
        component.disposeView()
        component.dispose()
        updater.dispose()
        onRelease(component)
    }
}

private fun Int.limitSizeWhenExceeds(limit: Int, target: Int?): Int =
    if (this < limit) this else (target ?: limit)

private class ArkUIViewPainter(
    embeddedInteropComponent: EmbeddedInteropViewNew,
    enableCApi: Boolean,
    rootNodeId: Int,
    extNodeId: Int,
) : Painter() {
    private val externalRenderNode: ExternalRenderNode

    init {
        val node = if (enableCApi) {
            embeddedInteropComponent.component.jsArkUIViewRef["rootNodeId"] = rootNodeId.nApiValue()
            embeddedInteropComponent.component.jsArkUIViewRef["extNodeId"] = extNodeId.nApiValue()
            val frameNode = embeddedInteropComponent.component.jsArkUIViewRef.call("getFrameNode")
            requireNotNull(frameNode) { "ArkUIViewPainter init failed: empty frame node" }
            ExternalRenderNode.makeFromNApiValue(rootNodeId, extNodeId, frameNode)
        } else {
            val renderNode = embeddedInteropComponent.component.jsArkUIViewRef.call("getRenderNode")
            requireNotNull(renderNode) { "ArkUIViewPainter init failed: empty render node" }
            ExternalRenderNode.makeFromNApiValue(renderNode)
        }
        require(node != null) { "ArkUIViewPainter init failed: empty jsArkUIView" }
        externalRenderNode = node
    }

    override fun DrawScope.onDraw() {
        drawRenderNode(externalRenderNode.ptr)
    }

    override val intrinsicSize: Size = Size.Unspecified

    override fun toString(): String {
        return "ArkUIViewPainter(externalRenderNode=$externalRenderNode)"
    }
}
