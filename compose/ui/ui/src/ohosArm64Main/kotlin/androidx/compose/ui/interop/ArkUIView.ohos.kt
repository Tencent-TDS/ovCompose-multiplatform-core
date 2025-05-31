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

import androidx.compose.runtime.Applier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReusableComposeNode
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.currentCompositeKeyHash
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.arkui.ArkUIRootView
import androidx.compose.ui.arkui.ArkUIView
import androidx.compose.ui.arkui.ArkUIViewContainer
import androidx.compose.ui.arkui.pointerInteropFilter
import androidx.compose.ui.arkui.pointerInteropPlaceholderFilter
import androidx.compose.ui.arkui.trackUIKitInterop
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isDebugLogEnabled
import androidx.compose.ui.graphics.kLog
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.materializerOf
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.napi.JsObject
import androidx.compose.ui.napi.js
import androidx.compose.ui.node.ComposeUiNode
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.atomicfu.atomic

private val STUB_CALLBACK_WITH_RECEIVER: Any.() -> Unit = {}

// This is about 16 million pixels. That should be big enough. We'll treat anything bigger as an
// error.
private const val MaxLayoutDimension = (1 shl 24) - 1

private const val MaxLayoutDimensionInList = 4096

private val RECT_UNDEFINED = Rect(Float.NaN, Float.NaN, Float.NaN, Float.NaN)


/**
 * 解决频道页卡片高度不能自适应的问题。
 * // TODO: 目前还未涉及宽度自适应的场景，未来待补全。
 *
 * translateHeight：为占位高度。
 * maxHeight：为最大高度，同时也是测量时的最大高度。
 */
data class AdaptiveParams(
    internal val translateHeight: Dp? = null,
    internal val maxHeight: Dp = 500.dp,
)

enum class InteropContainer { BACK, FORE, TOUCHABLE }

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
internal fun InternalArkUIView(
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
    container: InteropContainer = InteropContainer.BACK
) {
    // TODO: adapt UIKitView to reuse inside LazyColumn like in AndroidView:
    //  https://developer.android.com/reference/kotlin/androidx/compose/ui/viewinterop/package-summary#AndroidView(kotlin.Function1,kotlin.Function1,androidx.compose.ui.Modifier,kotlin.Function1,kotlin.Function1)

    val rootView = when (container) {
        InteropContainer.BACK -> LocalBackInteropContainer.current
        InteropContainer.FORE -> LocalForeInteropContainer.current
        InteropContainer.TOUCHABLE -> LocalTouchableInteropContainer.current
    }
    val embeddedInteropComponent = remember {
        EmbeddedInteropView(
            rootView = rootView,
            onRelease
        )
    }
    val density = LocalDensity.current.density
    var componentOffset: IntOffset by remember { mutableStateOf(IntOffset.Zero) }
    var componentSize: IntSize by remember { mutableStateOf(IntSize.Zero) }
    var componentClipBounds by remember { mutableStateOf(RECT_UNDEFINED) }

    val interopContext = LocalArkUIInteropContext.current
    // A state to notify relayout. Any better way to handle this?
    var layoutKey by remember { mutableStateOf(0) }

    Place(
        key = layoutKey,
        modifier = modifier.onGloballyPositioned { coordinates ->
            val rootCoordinates = coordinates.findRootCoordinates()
            // Calculates the unclipped bounds in the root coordinates.
            val bounds = rootCoordinates.localBoundingBoxOf(coordinates, clipBounds = false)
            // When the component has been clipped, we need to check whether the clipping bounds have changed.
            val newOffset = bounds.topLeft
            val newSize = coordinates.size
            interopContext.deferAction {
                // TODO: Gavin Temporarily fix setPosition not taking effect for the emulator
                // It's possible that setTranslation is a better choice.
                // In the current version of Harmony OS, setPosition causes a redraw, and it does not take effect on the emulator.
                OhosTrace.traceSync("ArkUIView[${newOffset.y}]") {
                    embeddedInteropComponent.component.setTranslation(
                        newOffset.x / density,
                        newOffset.y / density
                    )
                }
            }
            if (componentSize != newSize) {
                interopContext.deferAction {
                    val measureHeight = adaptiveParams?.maxHeight?.value ?: newSize.height / density
                    if (isDebugLogEnabled) {
                        kLog(
                            "ArkUIView setSize tag=${tag}, id=${embeddedInteropComponent.component.id}" +
                                "newSize=${newSize}, adaptiveParams=$adaptiveParams, measureHeight=$measureHeight"
                        )
                    }
                    embeddedInteropComponent.component.setSize(
                        newSize.width / density,
                        measureHeight
                    )
                }
                componentSize = newSize
            }

            // Calculates the clipped bounds in the root coordinates.
            val clipBounds = rootCoordinates.localBoundingBoxOf(coordinates, clipBounds = true)
            // Reduce rect to zero if it is empty.
            val clipBoundsBasedSelf =
                clipBounds.translate(-bounds.left, -bounds.top).takeIf { !it.isEmpty } ?: Rect.Zero

            if (bounds != clipBounds || componentClipBounds != RECT_UNDEFINED) {
                // bounds != clipBounds means need to clip
                // componentClipBounds != RECT_UNDEFINED means that clipping has been set before and needs to be updated
                if (componentClipBounds != clipBoundsBasedSelf) {
                    componentClipBounds = clipBoundsBasedSelf
                    interopContext.deferAction {
                        embeddedInteropComponent.component.setVisible(!componentClipBounds.isEmpty)
                        embeddedInteropComponent.component.setClipBounds(
                            componentClipBounds.left,
                            componentClipBounds.top,
                            componentClipBounds.right,
                            componentClipBounds.bottom
                        )
                    }
                }
            }
        }.trackUIKitInterop(embeddedInteropComponent.container).let {
            when (container) {
                InteropContainer.BACK -> it.drawBehind {
                    // draw transparent hole
                    drawRect(Color.Transparent, blendMode = BlendMode.DstAtop)
                }

                InteropContainer.FORE, InteropContainer.TOUCHABLE -> it
            }
        }.let {
            when (container) {
                InteropContainer.FORE, InteropContainer.BACK ->
                    if (interactive) {
                        it.pointerInteropFilter(embeddedInteropComponent.container)
                    } else {
                        it
                    }

                InteropContainer.TOUCHABLE ->
                    it.pointerInteropPlaceholderFilter(embeddedInteropComponent.container)
            }
        },
        measurePolicy = { _, constraints ->
//            interopContext.deferAction {
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
            OhosTrace.traceSync("ArkUIViewMeasure") {
                embeddedInteropComponent.component.measure(localConstraints, density)
            }

//            }

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

            componentSize = IntSize(layoutWidth, layoutHeight)

            layout(
                layoutWidth,
                layoutHeight
            ) {}
        }
    )

    DisposableEffect(Unit) {
        embeddedInteropComponent.component = ArkUIView(
            name, parameter,
            onMeasured = { width, height ->
                if (isDebugLogEnabled) {
                    kLog(
                        "ArkUIView onMeasured tag=$tag, id=${embeddedInteropComponent.component.id}" +
                            ", width=$width, height=$height, " +
                            ", old.width=${componentSize.width}, old.height=${componentSize.height}"
                    )
                }
                if (width != componentSize.width || height != componentSize.height) {
                    layoutKey++
                }
            },
            composeParameterUpdater = {
                // Need more work. JsObject will be invalid when recomposing.
                update(it)
            }
        )

        embeddedInteropComponent.rootView.buildView(embeddedInteropComponent.component)

        onCreate(embeddedInteropComponent.component)

        embeddedInteropComponent.updater = Updater(embeddedInteropComponent.component, updater) {
            interopContext.deferAction(action = it)
        }

        // Call addToHierarchy immediately to trigger the measurement of ArkUI a.s.a.p.
        // It may be lucky to get the measuredWidth and measuredHeight in the measurePolicy above in the first time,
        // but most of the time it is not.
        // Need a better way to interop with the measurement of ArkUI component.

        interopContext.deferAction(ArkUIInteropViewHierarchyChange.VIEW_ADDED) {
            embeddedInteropComponent.addToHierarchy()
        }

        onDispose {
            interopContext.deferAction(ArkUIInteropViewHierarchyChange.VIEW_REMOVED) {
                embeddedInteropComponent.removeFromHierarchy()
            }
        }
    }

    LaunchedEffect(background) {
        interopContext.deferAction {
            embeddedInteropComponent.setBackgroundColor(background)
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
@Deprecated("Use ArkUIView2")
@Composable
fun ArkUIView(
    name: String,
    modifier: Modifier,
    parameter: JsObject = js(),
    update: (ArkUIView) -> Unit = STUB_CALLBACK_WITH_RECEIVER,
    background: Color = Color.Unspecified,
    onRelease: (ArkUIView) -> Unit = STUB_CALLBACK_WITH_RECEIVER,
    interactive: Boolean = true
) = ArkUIView2(
    name = name,
    modifier = modifier,
    parameter = parameter,
    background = background,
    onRelease = onRelease,
    interactive = interactive
)

@Composable
fun ArkUIView2(
    name: String,
    modifier: Modifier,
    parameter: JsObject = js(),
    update: (JsObject) -> Unit = STUB_CALLBACK_WITH_RECEIVER,
    background: Color = Color.Unspecified,
    onRelease: (ArkUIView) -> Unit = STUB_CALLBACK_WITH_RECEIVER,
    interactive: Boolean = true
) = ArkUIViewAdaptive(
    name = name,
    modifier = modifier,
    parameter = parameter,
    update = update,
    background = background,
    onRelease = onRelease,
    interactive = interactive
)

@Composable
fun ArkUIViewAdaptive(
    name: String,
    modifier: Modifier,
    parameter: JsObject = js(),
    update: (JsObject) -> Unit = STUB_CALLBACK_WITH_RECEIVER,
    background: Color = Color.Unspecified,
    onRelease: (ArkUIView) -> Unit = STUB_CALLBACK_WITH_RECEIVER,
    interactive: Boolean = true,
    adaptiveParams: AdaptiveParams? = null,
    tag: String? = null,
) = InternalArkUIView(
    name = name,
    modifier = modifier,
    parameter = parameter,
    update = update,
    background = background,
    onRelease = onRelease,
    interactive = interactive,
    adaptiveParams = adaptiveParams,
    tag = tag
)

@Composable
fun ForeArkUIView(
    name: String,
    modifier: Modifier,
    parameter: JsObject = js(),
    update: (JsObject) -> Unit = STUB_CALLBACK_WITH_RECEIVER,
    background: Color = Color.Unspecified,
    onRelease: (ArkUIView) -> Unit = STUB_CALLBACK_WITH_RECEIVER,
    interactive: Boolean = true,
    adaptiveParams: AdaptiveParams? = null,
    tag: String? = null,
) = InternalArkUIView(
    name = name,
    modifier = modifier,
    parameter = parameter,
    update = update,
    background = background,
    onRelease = onRelease,
    interactive = interactive,
    adaptiveParams = adaptiveParams,
    tag = tag,
    container = InteropContainer.FORE,
)

/**
 * A special ArkUI interop view above the compose, which is touchable on the ArkUI side,
 * following the ArkUI framework touch test instead of forwarding via compose.
 *
 * **Keep [TouchableArkUIView] at the top of the compose side to avoid touch event conflicts.*.
 */
@Composable
fun TouchableArkUIView(
    name: String,
    modifier: Modifier,
    parameter: JsObject = js(),
    update: (JsObject) -> Unit = STUB_CALLBACK_WITH_RECEIVER,
    background: Color = Color.Unspecified,
    onRelease: (ArkUIView) -> Unit = STUB_CALLBACK_WITH_RECEIVER,
    adaptiveParams: AdaptiveParams? = null,
    tag: String? = null,
) = InternalArkUIView(
    name = name,
    modifier = modifier,
    parameter = parameter,
    update = update,
    background = background,
    onRelease = onRelease,
    adaptiveParams = adaptiveParams,
    tag = tag,
    container = InteropContainer.TOUCHABLE,
)

@Composable
fun ArkUIView(
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
    container: InteropContainer = InteropContainer.BACK
) = InternalArkUIView(
    name = name,
    modifier = modifier,
    parameter = parameter,
    update = update,
    background = background,
    updater = updater,
    onCreate = onCreate,
    onRelease = onRelease,
    interactive = interactive,
    adaptiveParams = adaptiveParams,
    tag = tag,
    container = container,
)

@Composable
private fun Place(key: Int, modifier: Modifier, measurePolicy: MeasurePolicy) {
    key(key) {

        val compositeKeyHash = currentCompositeKeyHash
        val localMap = currentComposer.currentCompositionLocalMap
        ReusableComposeNode<ComposeUiNode, Applier<Any>>(
            factory = ComposeUiNode.Constructor,
            update = {
                set(measurePolicy, ComposeUiNode.SetMeasurePolicy)
                set(localMap, ComposeUiNode.SetResolvedCompositionLocals)
                @OptIn(ExperimentalComposeUiApi::class)
                set(compositeKeyHash, ComposeUiNode.SetCompositeKeyHash)
            },
            skippableUpdate = materializerOf(modifier),
            content = {}
        )
    }
}

private abstract class EmbeddedInteropComponent(
    val rootView: ArkUIRootView,
    val onRelease: (ArkUIView) -> Unit
) {
    val container = ArkUIViewContainer()
    lateinit var component: ArkUIView
    lateinit var updater: Updater<ArkUIView>

    fun setBackgroundColor(color: Color) {
        component.backgroundColor = color
    }

    abstract fun addToHierarchy()
    abstract fun removeFromHierarchy()

    protected fun addViewToHierarchy(view: ArkUIView) {
        container.arkUIView = view
        rootView.addInteropView(container)
    }

    protected fun removeViewFromHierarchy(view: ArkUIView) {
        rootView.removeInteropView(container)
        updater.dispose()
        onRelease(component)
    }
}

private class EmbeddedInteropView(
    rootView: ArkUIRootView,
    onRelease: (ArkUIView) -> Unit
) : EmbeddedInteropComponent(rootView, onRelease) {
    override fun addToHierarchy() {
        addViewToHierarchy(component)
    }

    override fun removeFromHierarchy() {
        removeViewFromHierarchy(component)
    }
}

private class Updater<T : Any>(
    private val component: T,
    update: (T) -> Unit,

    /**
     * Updater will not execute the [update] method by itself, but will pass it to this lambda
     */
    private val deferAction: (() -> Unit) -> Unit,
) {
    private var isDisposed = false
    private val isUpdateScheduled = atomic(false)
    private val snapshotObserver = SnapshotStateObserver { command ->
        command()
    }

    private val scheduleUpdate = { _: T ->
        if (!isUpdateScheduled.getAndSet(true)) {
            deferAction {
                // TODO: check(Thread.isMainThread)

                isUpdateScheduled.value = false
                if (!isDisposed) {
                    performUpdate()
                }
            }
        }
    }

    var update: (T) -> Unit = update
        set(value) {
            if (field != value) {
                field = value
                performUpdate()
            }
        }

    init {
        snapshotObserver.start()
        performUpdate()
    }

    private fun performUpdate() {
        // don't replace scheduleUpdate by lambda reference,
        // scheduleUpdate should always be the same instance
        snapshotObserver.observeReads(component, scheduleUpdate) {
            update(component)
        }
    }

    fun dispose() {
        snapshotObserver.stop()
        snapshotObserver.clear()
        isDisposed = true
    }
}

private fun Int.limitSizeWhenExceeds(limit: Int, target: Int?): Int =
    if (this < limit) this else (target ?: limit)

fun Dp.toPx(density: Float): Int = (this.value * density).toInt()