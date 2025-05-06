/*
 * Copyright 2023 The Android Open Source Project
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import androidx.compose.ui.*
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.InteropViewCatchPointerModifier
import androidx.compose.ui.window.pointerInteropFilter
import androidx.compose.runtime.key
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.materializerOf
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.ComposeUiNode
import androidx.compose.ui.node.ComposeUiNode.Companion.SetCompositeKeyHash
import androidx.compose.ui.node.ComposeUiNode.Companion.SetMeasurePolicy
import androidx.compose.ui.node.ComposeUiNode.Companion.SetResolvedCompositionLocals
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.v2.nativefoundation.AdaptiveCanvas
import androidx.compose.ui.semantics.AccessibilityKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.uikit.LocalRenderBackend
import androidx.compose.ui.uikit.RenderBackend
import androidx.compose.ui.uikit.utils.TMMInteropWrapView
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toSize
import kotlinx.atomicfu.atomic
import kotlinx.cinterop.CValue
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSThread
import platform.UIKit.UIColor
import platform.UIKit.UIView
import platform.UIKit.UIViewController
import platform.UIKit.addChildViewController
import platform.UIKit.didMoveToParentViewController
import platform.UIKit.removeFromParentViewController
import platform.UIKit.willMoveToParentViewController
import kotlin.math.ceil

private val STUB_CALLBACK_WITH_RECEIVER: Any.() -> Unit = {}
private val DefaultViewResize: UIView.(CValue<CGRect>) -> Unit = { rect -> this.setFrame(rect) }
private val DefaultViewControllerResize: UIViewController.(CValue<CGRect>) -> Unit = { rect -> this.view.setFrame(rect) }
// region Tencent Code
private val DefaultPlacementScope: Placeable.PlacementScope.() -> Unit = {}
private val DefaultEmptyComposeContent: @Composable @UiComposable () -> Unit = {}
// endregion

/**
 * @param factory The block creating the [UIView] to be composed.
 * @param modifier The modifier to be applied to the layout. Size should be specified in modifier.
 * Modifier may contains crop() modifier with different shapes.
 * @param update A callback to be invoked after the layout is inflated.
 * @param background A color of [UIView] background wrapping the view created by [factory].
 * @param onRelease A callback invoked as a signal that this view instance has exited the
 * composition hierarchy entirely and will not be reused again. Any additional resources used by the
 * View should be freed at this time.
 * @param onResize May be used to custom resize logic.
 * @param interactive If true, then user touches will be passed to this UIView
 */
@Composable
fun <T : UIView> UIKitView(
    factory: () -> T,
    modifier: Modifier,
    update: (T) -> Unit = STUB_CALLBACK_WITH_RECEIVER,
    background: Color = Color.Unspecified,
    onRelease: (T) -> Unit = STUB_CALLBACK_WITH_RECEIVER,
    onResize: (view: T, rect: CValue<CGRect>) -> Unit = DefaultViewResize,
    interactive: Boolean = true,
) {
    // region Tencent Code
    val renderBackend = LocalRenderBackend.current
    when (renderBackend) {
        RenderBackend.Skia -> UIKitViewForSkiaBackend(factory, modifier, update, background, onRelease, onResize, interactive)
        RenderBackend.UIView -> UIKitViewForUIViewBackend(factory, modifier, update, background, onRelease, onResize, interactive)
    }
    // endregion
}

// region Tencent Code
/**
 * @param factory The block creating the [UIView] to be composed.
 * @param modifier The modifier to be applied to the layout. Size should be specified in modifier.
 * Modifier may contains crop() modifier with different shapes.
 * @param update A callback to be invoked after the layout is inflated.
 * @param background A color of [UIView] background wrapping the view created by [factory].
 * @param onRelease A callback invoked as a signal that this view instance has exited the
 * composition hierarchy entirely and will not be reused again. Any additional resources used by the
 * View should be freed at this time.
 * @param onResize May be used to custom resize logic.
 * @param interactive If true, then user touches will be passed to this UIView
 */
@Composable
fun <T : UIView> UIKitView2(
    factory: () -> T,
    modifier: Modifier,
    update: (T) -> Unit = STUB_CALLBACK_WITH_RECEIVER,
    background: Color = Color.Unspecified,
    onRelease: (T) -> Unit = STUB_CALLBACK_WITH_RECEIVER,
    onResize: (view: T, rect: CValue<CGRect>) -> Unit = DefaultViewResize,
    interactive: Boolean = true,
) {
    val renderBackend = LocalRenderBackend.current
    when (renderBackend) {
        RenderBackend.Skia -> UIKitViewForSkiaBackend(factory, modifier, update, background, onRelease, onResize, interactive)
        RenderBackend.UIView -> UIKitViewForUIViewBackend(factory, modifier, update, background, onRelease, onResize, interactive, true)
    }
}

/**
 * @param factory The block creating the [UIView] to be composed.
 * @param modifier The modifier to be applied to the layout. Size should be specified in modifier.
 * Modifier may contains crop() modifier with different shapes.
 * @param update A callback to be invoked after the layout is inflated.
 * @param background A color of [UIView] background wrapping the view created by [factory].
 * @param onRelease A callback invoked as a signal that this view instance has exited the
 * composition hierarchy entirely and will not be reused again. Any additional resources used by the
 * View should be freed at this time.
 * @param onResize May be used to custom resize logic.
 * @param interactive If true, then user touches will be passed to this UIView
 */
@Composable
private fun <T : UIView> UIKitViewForUIViewBackend(
    factory: () -> T,
    modifier: Modifier,
    update: (T) -> Unit = STUB_CALLBACK_WITH_RECEIVER,
    background: Color = Color.Unspecified,
    onRelease: (T) -> Unit = STUB_CALLBACK_WITH_RECEIVER,
    onResize: (view: T, rect: CValue<CGRect>) -> Unit = DefaultViewResize,
    interactive: Boolean = true,
    enableNestedScroll: Boolean = false
) {
    val interopContext = LocalUIKitInteropContext.current
    var layoutKey by remember { mutableStateOf(0) }
    val embeddedInteropComponent = remember {
        EmbeddedInteropForUIViewBackend(
            onRelease = onRelease
        )
    }
    if (enableNestedScroll) {
        embeddedInteropComponent.bindComposeInteractionUIView(interopContext.interactionUIView)
    }

    val density = LocalDensity.current.density
    var size by remember { mutableStateOf(IntSize.Zero) }
    embeddedInteropComponent.wrappingView.userInteractionEnabled = interactive
    Place(
        measurePolicy = { _, constraints ->
            embeddedInteropComponent.component.measure(density, this, constraints)
        },
        modifier = Modifier.then(modifier)
            .nativeAccessibility(isEnabled = true, embeddedInteropComponent.wrappingView)
            .uiViewLayer { view ->
                embeddedInteropComponent.wrappingView.setFrame(view.frame.useContents {
                    CGRectMake(0.0,0.0, this.size.width, this.size.height)
                })
            }.drawLayer { canvas ->
                (canvas as AdaptiveCanvas).drawLayer(embeddedInteropComponent.wrappingView.layer)
            }.onGloballyPositioned { coordinates ->
                if (size != coordinates.size) {
                    val newSize = coordinates.size.toSize() / density
                    interopContext.deferAction {
                        onResize(
                            embeddedInteropComponent.component,
                            CGRectMake(
                                0.0,
                                0.0,
                                newSize.width.toDouble(),
                                newSize.height.toDouble()
                            ),
                        )
                    }
                    size = coordinates.size
                }
            }.let {
                if (interactive) {
                    it.pointerInteropFilter(wrappingView = embeddedInteropComponent.wrappingView)
                } else {
                    it
                }
            }
        ,
        factory = {
            LayoutNode().also {
                embeddedInteropComponent.refreshLayoutNode(it)
            }
        }
    )

    DisposableEffect(Unit) {
        embeddedInteropComponent.component = factory()
        embeddedInteropComponent.updater = Updater(embeddedInteropComponent.component, update) {
            interopContext.deferAction(action = it)
        }

        interopContext.deferAction(UIKitInteropViewHierarchyChange.VIEW_ADDED) {
            embeddedInteropComponent.addToHierarchy()
        }

        onDispose {
            interopContext.deferAction(UIKitInteropViewHierarchyChange.VIEW_REMOVED) {
                embeddedInteropComponent.removeFromHierarchy()
            }
        }
    }

    LaunchedEffect(background) {
        interopContext.deferAction {
            embeddedInteropComponent.wrappingView.backgroundColor = parseColor(background)
        }
    }

    SideEffect {
        embeddedInteropComponent.updater.update = update
    }
}

/**
 * @param factory The block creating the [UIView] to be composed.
 * @param modifier The modifier to be applied to the layout. Size should be specified in modifier.
 * Modifier may contains crop() modifier with different shapes.
 * @param update A callback to be invoked after the layout is inflated.
 * @param background A color of [UIView] background wrapping the view created by [factory].
 * @param onRelease A callback invoked as a signal that this view instance has exited the
 * composition hierarchy entirely and will not be reused again. Any additional resources used by the
 * View should be freed at this time.
 * @param onResize May be used to custom resize logic.
 * @param interactive If true, then user touches will be passed to this UIView
 */
@Composable
private fun <T : UIView> UIKitViewForSkiaBackend(
    factory: () -> T,
    modifier: Modifier,
    update: (T) -> Unit = STUB_CALLBACK_WITH_RECEIVER,
    background: Color = Color.Unspecified,
    onRelease: (T) -> Unit = STUB_CALLBACK_WITH_RECEIVER,
    onResize: (view: T, rect: CValue<CGRect>) -> Unit = DefaultViewResize,
    interactive: Boolean = true,
// end region
) {
    // TODO: adapt UIKitView to reuse inside LazyColumn like in AndroidView:
    //  https://developer.android.com/reference/kotlin/androidx/compose/ui/viewinterop/package-summary#AndroidView(kotlin.Function1,kotlin.Function1,androidx.compose.ui.Modifier,kotlin.Function1,kotlin.Function1)
    val rootView = LocalInteropContainer.current
    val embeddedInteropComponent = remember {
        EmbeddedInteropView(
            rootView = rootView,
            onRelease
        )
    }
    val density = LocalDensity.current.density
    var rectInPixels by remember { mutableStateOf(IntRect(0, 0, 0, 0)) }
    var localToWindowOffset: IntOffset by remember { mutableStateOf(IntOffset.Zero) }
    val interopContext = LocalUIKitInteropContext.current

    Place(
        modifier.onGloballyPositioned { coordinates ->
            localToWindowOffset = coordinates.positionInRoot().round()
            val newRectInPixels = IntRect(localToWindowOffset, coordinates.size)
            if (rectInPixels != newRectInPixels) {
                val rect = newRectInPixels / density

                interopContext.deferAction {
                    embeddedInteropComponent.wrappingView.setFrame(rect.toCGRect())
                }

                if (rectInPixels.width != newRectInPixels.width || rectInPixels.height != newRectInPixels.height) {
                    interopContext.deferAction {
                        onResize(
                            embeddedInteropComponent.component,
                            CGRectMake(0.0, 0.0, rect.width.toDouble(), rect.height.toDouble()),
                        )
                    }
                }
                rectInPixels = newRectInPixels
            }
        }.drawBehind {
            drawRect(Color.Transparent, blendMode = BlendMode.DstAtop) // draw transparent hole
        }.let {
            if (interactive) {
                it.then(InteropViewCatchPointerModifier())
            } else {
                it
            }
        }
    )

    DisposableEffect(Unit) {
        embeddedInteropComponent.component = factory()
        embeddedInteropComponent.updater = Updater(embeddedInteropComponent.component, update) {
            interopContext.deferAction(action = it)
        }

        interopContext.deferAction(UIKitInteropViewHierarchyChange.VIEW_ADDED) {
            embeddedInteropComponent.addToHierarchy()
        }

        onDispose {
            interopContext.deferAction(UIKitInteropViewHierarchyChange.VIEW_REMOVED) {
                embeddedInteropComponent.removeFromHierarchy()
            }
        }
    }

    LaunchedEffect(background) {
        interopContext.deferAction {
            embeddedInteropComponent.setBackgroundColor(background)
        }
    }

    SideEffect {
        embeddedInteropComponent.updater.update = update
    }
}

/**
 * @param factory The block creating the [UIViewController] to be composed.
 * @param modifier The modifier to be applied to the layout. Size should be specified in modifier.
 * Modifier may contains crop() modifier with different shapes.
 * @param update A callback to be invoked after the layout is inflated.
 * @param background A color of [UIView] background wrapping the view of [UIViewController] created by [factory].
 * @param onRelease A callback invoked as a signal that this view controller instance has exited the
 * composition hierarchy entirely and will not be reused again. Any additional resources used by the
 * view controller should be freed at this time.
 * @param onResize May be used to custom resize logic.
 * @param interactive If true, then user touches will be passed to this UIViewController
 */
@Composable
fun <T : UIViewController> UIKitViewController(
    factory: () -> T,
    modifier: Modifier,
    update: (T) -> Unit = STUB_CALLBACK_WITH_RECEIVER,
    background: Color = Color.Unspecified,
    onRelease: (T) -> Unit = STUB_CALLBACK_WITH_RECEIVER,
    onResize: (viewController: T, rect: CValue<CGRect>) -> Unit = DefaultViewControllerResize,
    interactive: Boolean = true,
) {
    // TODO: adapt UIKitViewController to reuse inside LazyColumn like in AndroidView:
    //  https://developer.android.com/reference/kotlin/androidx/compose/ui/viewinterop/package-summary#AndroidView(kotlin.Function1,kotlin.Function1,androidx.compose.ui.Modifier,kotlin.Function1,kotlin.Function1)
    val rootView = LocalInteropContainer.current
    val rootViewController = LocalUIViewController.current
    val embeddedInteropComponent = remember {
        EmbeddedInteropViewController(
            rootView,
            rootViewController,
            onRelease
        )
    }

    val density = LocalDensity.current.density
    var rectInPixels by remember { mutableStateOf(IntRect(0, 0, 0, 0)) }
    var localToWindowOffset: IntOffset by remember { mutableStateOf(IntOffset.Zero) }
    val interopContext = LocalUIKitInteropContext.current

    Place(
        modifier.onGloballyPositioned { coordinates ->
            localToWindowOffset = coordinates.positionInRoot().round()
            val newRectInPixels = IntRect(localToWindowOffset, coordinates.size)
            if (rectInPixels != newRectInPixels) {
                val rect = newRectInPixels / density

                interopContext.deferAction {
                    embeddedInteropComponent.wrappingView.setFrame(rect.toCGRect())
                }

                if (rectInPixels.width != newRectInPixels.width || rectInPixels.height != newRectInPixels.height) {
                    interopContext.deferAction {
                        onResize(
                            embeddedInteropComponent.component,
                            CGRectMake(0.0, 0.0, rect.width.toDouble(), rect.height.toDouble()),
                        )
                    }
                }
                rectInPixels = newRectInPixels
            }
        }.drawBehind {
            drawRect(Color.Transparent, blendMode = BlendMode.DstAtop) // draw transparent hole
        }.let {
            if (interactive) {
                it.then(InteropViewCatchPointerModifier())
            } else {
                it
            }
        }
    )

    DisposableEffect(Unit) {
        embeddedInteropComponent.component = factory()
        embeddedInteropComponent.updater = Updater(embeddedInteropComponent.component, update) {
            interopContext.deferAction(action = it)
        }

        interopContext.deferAction(UIKitInteropViewHierarchyChange.VIEW_ADDED) {
            embeddedInteropComponent.addToHierarchy()
        }

        onDispose {
            interopContext.deferAction(UIKitInteropViewHierarchyChange.VIEW_REMOVED) {
                embeddedInteropComponent.removeFromHierarchy()
            }
        }
    }

    LaunchedEffect(background) {
        interopContext.deferAction {
            embeddedInteropComponent.setBackgroundColor(background)
        }
    }

    SideEffect {
        embeddedInteropComponent.updater.update = update
    }
}

// region Tencent Code
@Composable
private fun Place(
    modifier: Modifier,
    measurePolicy:
    MeasurePolicy = PlaceMeasurePolicy,
    factory: () -> ComposeUiNode = ComposeUiNode.Constructor
) {
    val compositeKeyHash = currentCompositeKeyHash
    val localMap = currentComposer.currentCompositionLocalMap
    ReusableComposeNode<ComposeUiNode, Applier<Any>>(
        factory = factory,
        update = {
            set(measurePolicy, SetMeasurePolicy)
            set(localMap, SetResolvedCompositionLocals)
            @OptIn(ExperimentalComposeUiApi::class)
            set(compositeKeyHash, SetCompositeKeyHash)
        },
        skippableUpdate = materializerOf(modifier),
        content = DefaultEmptyComposeContent
    )
}
// endregion

private object PlaceMeasurePolicy : MeasurePolicy {
    override fun MeasureScope.measure(measurables: List<Measurable>, constraints: Constraints) =
        layout(constraints.maxWidth, constraints.maxHeight) {}
}

// region Tencent Code
private inline fun UIView.measure(density: Float, measureScope: MeasureScope, constraints: Constraints): MeasureResult {
    val hasFixedWidth = constraints.hasFixedWidth
    val hasFixedHeight = constraints.hasFixedHeight
    return if (hasFixedWidth && hasFixedHeight) {
        measureScope.layout(constraints.maxWidth, constraints.maxHeight, emptyMap(), DefaultPlacementScope)
    } else {
        val scaledWidth = (constraints.maxWidth / density).toDouble()
        val scaledHeight = (constraints.maxHeight / density).toDouble()
        val measuredSize = sizeThatFits(CGSizeMake(scaledWidth, scaledHeight))
        measuredSize.useContents {
            val layoutWidth = if (hasFixedWidth) {
                constraints.maxWidth
            } else {
                ceil(width * density).toInt().coerceIn(constraints.minWidth, constraints.maxWidth)
            }
            val layoutHeight = if (hasFixedHeight) {
                constraints.maxHeight
            } else {
                ceil(height * density).toInt().coerceIn(constraints.minHeight, constraints.maxHeight)
            }
            measureScope.layout(layoutWidth, layoutHeight, emptyMap(), DefaultPlacementScope)
        }
    }
}
// endregion

private fun parseColor(color: Color): UIColor {
    return UIColor(
        red = color.red.toDouble(),
        green = color.green.toDouble(),
        blue = color.blue.toDouble(),
        alpha = color.alpha.toDouble()
    )
}

// region Tencent Code
// onSizeChange回调的是 dp，使用时需要注意单位转换。
private class EmbeddedInteropForUIViewBackend<T : UIView>(
    val onRelease: (T) -> Unit
) {
    var wrappingView: TMMInteropWrapView = TMMInteropWrapView()
    var layoutNode: LayoutNode? = null
    lateinit var component: T
    lateinit var updater: Updater<T>

    init {
        wrappingView.setOnSizeChange { _: Double, _: Double ->
            layoutNode?.requestRemeasure()
        }
    }

    fun bindComposeInteractionUIView(view: UIView) {
        wrappingView.bindComposeInteropContainer(view)
    }

    fun addToHierarchy() {
        wrappingView.addSubview(component)
    }

    fun removeFromHierarchy() {
        component.removeFromSuperview()
        wrappingView.removeFromSuperview()
        updater.dispose()
        onRelease(component)
    }

    fun refreshLayoutNode(node: LayoutNode) {
        layoutNode = node
    }
}
// endregion

private abstract class EmbeddedInteropComponent<T : Any>(
    val rootView: UIView,
    val onRelease: (T) -> Unit
) {
    lateinit var wrappingView: UIView
    lateinit var component: T
    lateinit var updater: Updater<T>

    fun setBackgroundColor(color: Color) {
        if (color == Color.Unspecified) {
            wrappingView.backgroundColor = rootView.backgroundColor
        } else {
            wrappingView.backgroundColor = parseColor(color)
        }
    }

    abstract fun addToHierarchy()
    abstract fun removeFromHierarchy()

    protected fun addViewToHierarchy(view: UIView) {
        wrappingView = UIView().apply {
            addSubview(view)
        }
        rootView.addSubview(wrappingView)
    }

    protected fun removeViewFromHierarchy(view: UIView) {
        wrappingView.removeFromSuperview()
        updater.dispose()
        onRelease(component)
    }
}

private class EmbeddedInteropView<T : UIView>(
    rootView: UIView,
    onRelease: (T) -> Unit
) : EmbeddedInteropComponent<T>(rootView, onRelease) {
    override fun addToHierarchy() {
        addViewToHierarchy(component)
    }

    override fun removeFromHierarchy() {
        removeViewFromHierarchy(component)
    }
}

private class EmbeddedInteropViewController<T : UIViewController>(
    rootView: UIView,
    private val rootViewController: UIViewController,
    onRelease: (T) -> Unit
) : EmbeddedInteropComponent<T>(rootView, onRelease) {
    override fun addToHierarchy() {
        rootViewController.addChildViewController(component)
        addViewToHierarchy(component.view)
        component.didMoveToParentViewController(rootViewController)
    }

    override fun removeFromHierarchy() {
        component.willMoveToParentViewController(null)
        removeViewFromHierarchy(component.view)
        component.removeFromParentViewController()
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
                check(NSThread.isMainThread)

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

    private fun performUpdate() {
        // don't replace scheduleUpdate by lambda reference,
        // scheduleUpdate should always be the same instance
        snapshotObserver.observeReads(component, scheduleUpdate) {
            update(component)
        }
    }

    init {
        snapshotObserver.start()
        performUpdate()
    }

    fun dispose() {
        snapshotObserver.stop()
        snapshotObserver.clear()
        isDisposed = true
    }
}

internal val NativeAccessibilityViewSemanticsKey = AccessibilityKey<TMMInteropWrapView>(
    name = "NativeAccessibilityView",
    mergePolicy = { parentValue, childValue ->
        if (parentValue == null) {
            childValue
        } else {
            println(
                "Warning: Merging accessibility for multiple interop views is not supported. " +
                        "Multiple [UIKitView] are grouped under one node that should be represented as a single accessibility element." +
                        "It isn't recommended because the accessibility system can only recognize the first one. " +
                        "If you need multiple native views for accessibility, make sure to place them inside a single [UIKitView]."
            )

            parentValue
        }
    }
)

private var SemanticsPropertyReceiver.nativeAccessibilityView by NativeAccessibilityViewSemanticsKey

// TODO: align "platform" vs "native" naming
/**
 * Chain [this] with [Modifier.semantics] that sets the [nativeAccessibilityView] of the node to
 * the [interopWrappingView] if [isEnabled] is true.
 * If [isEnabled] is false, [this] is returned as is.
 *
 * See [UIKitView] and [UIKitViewController] accessibility argument for description of effects introduced by this semantics.
 */
fun Modifier.nativeAccessibility(isEnabled: Boolean, interopWrappingView: TMMInteropWrapView) =
    if (isEnabled) {
        this.semantics { nativeAccessibilityView = interopWrappingView }
    } else {
        this
    }