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

package androidx.compose.ui.scene

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeTabService
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.kLog
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.pointer.HistoricalChange
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.interop.LocalInteropContainer
import androidx.compose.ui.interop.LocalUIKitInteropContext
import androidx.compose.ui.interop.UIKitInteropContext
import androidx.compose.ui.interop.UIKitInteropTransaction
import androidx.compose.ui.platform.AccessibilityMediator
import androidx.compose.ui.platform.AccessibilitySyncOptions
import androidx.compose.ui.platform.IOSPlatformContextImpl
import androidx.compose.ui.platform.LocalLayoutMargins
import androidx.compose.ui.platform.LocalSafeArea
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformInsets
import androidx.compose.ui.platform.PlatformWindowContext
import androidx.compose.ui.platform.UIKitTextInputService
import androidx.compose.ui.platform.v2.UIViewLayerFactory
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.uikit.systemDensity
import androidx.compose.ui.toDpOffset
import androidx.compose.ui.toDpRect
import androidx.compose.ui.uikit.ComposeConfiguration
import androidx.compose.ui.uikit.ComposeUIViewConfiguration
import androidx.compose.ui.uikit.ComposeUIViewControllerConfiguration
import androidx.compose.ui.uikit.LocalDrawInSkia
import androidx.compose.ui.uikit.LocalKeyboardOverlapHeight
import androidx.compose.ui.uikit.RenderBackend
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.roundToIntRect
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.window.FocusStack
import androidx.compose.ui.window.HitTestViewType
import androidx.compose.ui.window.InteractionUIView
import androidx.compose.ui.window.InteropContainer
import androidx.compose.ui.window.KeyboardEventHandler
import androidx.compose.ui.window.KeyboardVisibilityListenerImpl
import androidx.compose.ui.window.KeyboardVisibilityListenerImplV2
import androidx.compose.ui.window.RenderingComponent
import androidx.compose.ui.window.UITouchesEventPhase
import kotlin.coroutines.CoroutineContext
import kotlin.math.floor
import kotlin.math.roundToLong
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import org.jetbrains.skiko.SkikoKeyboardEvent
import platform.CoreGraphics.CGAffineTransformIdentity
import platform.CoreGraphics.CGAffineTransformInvert
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGRectZero
import platform.CoreGraphics.CGSize
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSSelectorFromString
import platform.Foundation.NSTimeInterval
import platform.QuartzCore.CATransaction
import platform.UIKit.NSLayoutConstraint
import platform.UIKit.UIEvent
import platform.UIKit.UIKeyboardWillHideNotification
import platform.UIKit.UIKeyboardWillShowNotification
import platform.UIKit.UITouch
import platform.UIKit.UITouchPhase
import platform.UIKit.UIView
import platform.UIKit.UIViewControllerTransitionCoordinatorProtocol
import platform.darwin.NSObject

/**
 * Layout of sceneView on the screen
 */
internal sealed interface SceneLayout {
    object Undefined : SceneLayout
    object UseConstraintsToFillContainer : SceneLayout
    class UseConstraintsToCenter(val size: CValue<CGSize>) : SceneLayout
    class Bounds(val rect: IntRect) : SceneLayout
}

@OptIn(ExperimentalComposeApi::class)
private class SemanticsOwnerListenerImpl(
    private val container: UIView,
    private val coroutineContext: CoroutineContext,
    private val getAccessibilitySyncOptions: () -> AccessibilitySyncOptions
) : PlatformContext.SemanticsOwnerListener {
    var current: Pair<SemanticsOwner, AccessibilityMediator>? = null

    override fun onSemanticsOwnerAppended(semanticsOwner: SemanticsOwner) {
        if (current == null) {
            current = semanticsOwner to AccessibilityMediator(
                view = container,
                owner = semanticsOwner,
                coroutineContext = coroutineContext,
                performEscape = { false },
                onKeyboardPresses = {},
                onScreenReaderActive = {},
                getAccessibilitySyncOptions()
            )
        }
    }

    override fun onSemanticsOwnerRemoved(semanticsOwner: SemanticsOwner) {
        val current = current ?: return

        if (current.first == semanticsOwner) {
            current.second.dispose()
            this.current = null
        }
    }

    override fun onSemanticsChange(semanticsOwner: SemanticsOwner) {
        val current = current ?: return

        if (current.first == semanticsOwner) {
            current.second.onSemanticsChange()
        }
    }
}

private class RenderingUIViewDelegateImpl(
    private val interopContext: UIKitInteropContext,
    private val getBoundsInPx: () -> IntRect,
    private val scene: ComposeScene,
// region Tencent Code
    private val onContentSizeChanged: ((IntSize) -> Unit)?
) : RenderingComponent.Delegate {

    private var contentSize: IntSize? = null
// endregion

    override fun retrieveInteropTransaction(): UIKitInteropTransaction =
        interopContext.retrieve()

    // region Tencent Code
    override fun render(canvas: Canvas, targetTimestamp: NSTimeInterval) {
        val topLeft = getBoundsInPx().topLeft.toOffset()
        canvas.translate(-topLeft.x, -topLeft.y)
        scene.render(canvas, targetTimestamp.toNanoSeconds())
        canvas.translate(topLeft.x, topLeft.y)

        if (onContentSizeChanged != null) {
            val contentSize = scene.getMeasuredContentSize()
            if (contentSize != this.contentSize) {
                // onContentSizeChanged(contentSize) is not possible due to compiler issue.
                onContentSizeChanged.invoke(contentSize)
                this.contentSize = contentSize
            }
        }
    }
    // endregion
}

private class NativeKeyboardVisibilityListener(
    private val keyboardVisibilityListener: KeyboardVisibilityListenerImpl
) : NSObject() {
    @Suppress("unused")
    @ObjCAction
    fun keyboardWillShow(arg: NSNotification) {
        keyboardVisibilityListener.keyboardWillShow(arg)
    }

    @Suppress("unused")
    @ObjCAction
    fun keyboardWillHide(arg: NSNotification) {
        keyboardVisibilityListener.keyboardWillHide(arg)
    }
}

private class ComposeSceneMediatorRootUIView : UIView(CGRectZero.readValue()) {
    override fun hitTest(point: CValue<CGPoint>, withEvent: UIEvent?): UIView? {
        // forwards touches forward to the children, is never a target for a touch
        val result = super.hitTest(point, withEvent)
        // region Tencent Code
        ComposeTabService.composeHitTestLog("ComposeSceneMediatorRootUIView result:${result.toString()}")
        // endregion
        return if (result == this) {
            null
        } else {
            result
        }
    }
}

// region Tencent Code
internal class ComposeSceneMediator(
    private val container: UIView,
    private val configuration: ComposeConfiguration,
    private val focusStack: FocusStack<UIView>?,
    private val windowContext: PlatformWindowContext,
    val coroutineContext: CoroutineContext,
    private val renderingComponentFactory: (RenderingComponent.Delegate) -> RenderingComponent<*>,
    private val boundsInWindow: (() -> IntRect)?,
    private val onContentSizeChanged: ((IntSize) -> Unit)?,
    private val nativeReusePool: Long = 0,
// endregion
    composeSceneFactory: (
        invalidate: () -> Unit,
        platformContext: PlatformContext,
        coroutineContext: CoroutineContext
    ) -> ComposeScene,
) {
    // region Tencent Code
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    // endregion
    private val focusable: Boolean get() = focusStack != null
    private val keyboardOverlapHeightState: MutableState<Float> = mutableStateOf(0f)
    private var _layout: SceneLayout = SceneLayout.Undefined
    private var constraints: List<NSLayoutConstraint> = emptyList()
        set(value) {
            if (field.isNotEmpty()) {
                NSLayoutConstraint.deactivateConstraints(field)
            }
            field = value
            NSLayoutConstraint.activateConstraints(value)
        }

    private val scene: ComposeScene by lazy {
        composeSceneFactory(
            ::onComposeSceneInvalidate,
            platformContext,
            coroutineContext,
        )
    }
    var compositionLocalContext
        get() = scene.compositionLocalContext
        set(value) {
            scene.compositionLocalContext = value
        }
    private val focusManager get() = scene.focusManager

    // region Tencent Code
    private val renderingComponent by lazy {
        renderingComponentFactory(renderDelegate)
    }

    @OptIn(ExperimentalComposeApi::class)
    private val renderingView by lazy {
        renderingComponent.view.also { it.opaque = configuration.opaque }
    }
    // endregion

    /**
     * view, that contains [interopViewContainer] and [interactionView] and is added to [container]
     */
    private val rootView = ComposeSceneMediatorRootUIView()

    /**
     * Container for UIKitView and UIKitViewController
     */
    private val interopViewContainer = InteropContainer()

    private val interactionView by lazy {
        InteractionUIView(
            keyboardEventHandler = keyboardEventHandler,
            touchesDelegate = touchesDelegate,
            updateTouchesCount = { count ->
                val needHighFrequencyPolling = count > 0
                // region Tencent Code
                renderingComponent.redrawer.needsProactiveDisplayLink = needHighFrequencyPolling
                // endregion
            },
            checkBounds = { dpPoint: DpOffset ->
                val point = dpPoint.toOffset(container.systemDensity)
                getBoundsInPx().contains(point.round())
            },
            // region Tencent Code
            becomeFirstResponder = configuration.canBecomeFirstResponder,
            drawInSkia = configuration.renderBackend == RenderBackend.Skia,
            // endregion
        )
    }

    private val interopContext: UIKitInteropContext by lazy {
        // region Tencent Code
        UIKitInteropContext(
            requestRedraw = { onComposeSceneInvalidate() },
            interactionUIView = interactionView
        )
        // endregion
    }

    @OptIn(ExperimentalComposeApi::class)
    private val semanticsOwnerListener by lazy {
        SemanticsOwnerListenerImpl(rootView, coroutineContext, getAccessibilitySyncOptions = {
            configuration.accessibilitySyncOptions
        })
    }

    private val platformContext: PlatformContext by lazy {
        IOSPlatformContextImpl(
            inputServices = uiKitTextInputService,
            textToolbar = uiKitTextInputService,
            windowInfo = windowContext.windowInfo,
            density = container.systemDensity,
            semanticsOwnerListener = semanticsOwnerListener,
            // region Tencent Code
            boundsPositionCalculator = configuration.boundsPositionCalculator,
            nativeReusePool = nativeReusePool,
            drawInSkia = configuration.renderBackend == RenderBackend.Skia,
            // endregion
        )
    }

    private val keyboardVisibilityListener by lazy {
        KeyboardVisibilityListenerImplV2(
            configuration = configuration,
            keyboardOverlapHeightState = keyboardOverlapHeightState,
            viewProvider = { container },
            densityProvider = { container.systemDensity },
            composeSceneMediatorProvider = { this },
            focusManager = focusManager,
        )
    }

    private val keyboardEventHandler: KeyboardEventHandler by lazy {
        object : KeyboardEventHandler {
            override fun onKeyboardEvent(event: SkikoKeyboardEvent) {
                val composeEvent = KeyEvent(event)
                if (!uiKitTextInputService.onPreviewKeyEvent(composeEvent)) {
                    scene.sendKeyEvent(composeEvent)
                }
            }
        }
    }

    private val uiKitTextInputService: UIKitTextInputService by lazy {
        UIKitTextInputService(
            updateView = {
                renderingView.setNeedsDisplay() // redraw on next frame
                CATransaction.flush() // clear all animations
            },
            rootViewProvider = { container },
            densityProvider = { container.systemDensity },
            focusStack = focusStack,
            keyboardEventHandler = keyboardEventHandler
        )
    }

    private val touchesDelegate: InteractionUIView.Delegate by lazy {
        object : InteractionUIView.Delegate {
            // region Tencent Code
            override fun pointInside(point: CValue<CGPoint>, event: UIEvent?): HitTestViewType =
                point.useContents {
                    val position = this.toDpOffset().toOffset(density)
                    val hitInteropView = scene.hitTestInteropView(position, event)
                    if (hitInteropView) return@useContents HitTestViewType.NATIVEVIEW
                    val hitComposeView = scene.hitTestComposeView(position, event)
                    if (hitComposeView) return@useContents  HitTestViewType.COMPOSEVIEW
                    return HitTestViewType.NONE
                }
            // endregion

            override fun onTouchesEvent(view: UIView, event: UIEvent, phase: UITouchesEventPhase) {
                // region Tencent Code
                // scene.sendPointerEvent(
                //     eventType = phase.toPointerEventType(),
                //     pointers = event.touchesForView(view)?.map {
                //         val touch = it as UITouch
                //         val id = touch.hashCode().toLong()
                //         val position = touch.offsetInView(view, density.density)
                //         ComposeScenePointer(
                //             id = PointerId(id),
                //             position = position,
                //             pressed = touch.isPressed,
                //             type = PointerType.Touch,
                //             pressure = touch.force.toFloat(),
                //             historical = event.historicalChangesForTouch(
                //                 touch,
                //                 view,
                //                 density.density
                //             )
                //         )
                //     } ?: emptyList(),
                //     timeMillis = (event.timestamp * 1e3).toLong(),
                //     nativeEvent = event
                // )
                val finalPhase =
                    if (ComposeTabService.composeGestureEnable && phase == UITouchesEventPhase.CANCELLED) {
                        PointerEventType.Unknown
                    } else phase.toPointerEventType()
                scene.sendPointerEvent(
                    eventType = finalPhase,
                    pointers = (event.touchesForView(view) ?: event.allTouches)?.map {
                        val touch = it as UITouch
                        val id = touch.hashCode().toLong()
                        val position = touch.offsetInView(view, density.density)
                        ComposeScenePointer(
                            id = PointerId(id),
                            position = position,
                            pressed = touch.isPressed,
                            type = PointerType.Touch,
                            pressure = touch.force.toFloat(),
                            historical = event.historicalChangesForTouch(
                                touch,
                                view,
                                density.density
                            )
                        )
                    } ?: emptyList(),
                    timeMillis = (event.timestamp * 1e3).toLong(),
                    nativeEvent = event
                )
                // endregion
            }
        }
    }

    private val renderDelegate by lazy {
        RenderingUIViewDelegateImpl(
            interopContext = interopContext,
            getBoundsInPx = ::getBoundsInPx,
            scene = scene,
            onContentSizeChanged = onContentSizeChanged
        )
    }

    private var onAttachedToWindow: (() -> Unit)? = null
    private fun runOnceViewAttached(block: () -> Unit) {
        if (renderingView.window == null) {
            onAttachedToWindow = {
                onAttachedToWindow = null
                block()
            }
        } else {
            block()
        }
    }

    // region Tencent Code
    var density: Density
        get() = scene.density
        set(value) {
            scene.density = value
        }
    var layoutDirection: LayoutDirection
        get() = scene.layoutDirection
        set(value) {
            scene.layoutDirection = value
        }
    // endregion

    fun hitTestInteractionView(point: CValue<CGPoint>, withEvent: UIEvent?): UIView? =
        interactionView.hitTest(point, withEvent)

    init {
        // region Tencent Code
        renderingComponent.onAttachedToWindow = {
            renderingComponent.onAttachedToWindow = null
        // endregion
            viewWillLayoutSubviews()
            this.onAttachedToWindow?.invoke()
            focusStack?.pushAndFocus(interactionView)
        }

        // region Tencent Code
        if (configuration.renderBackend == RenderBackend.UIView) {
            val clipChildren = when (configuration) {
                is ComposeUIViewControllerConfiguration ->  configuration.clipChildren
                is ComposeUIViewConfiguration -> configuration.clipChildren
                else -> true
            }
            scene.setLayerFactory(UIViewLayerFactory(renderingView, nativeReusePool, clipChildren))
        }
        // endregion

        rootView.translatesAutoresizingMaskIntoConstraints = false
        container.addSubview(rootView)
        NSLayoutConstraint.activateConstraints(
            getConstraintsToFillParent(rootView, container)
        )

        interopViewContainer.translatesAutoresizingMaskIntoConstraints = false
        rootView.addSubview(interopViewContainer)
        NSLayoutConstraint.activateConstraints(
            getConstraintsToFillParent(interopViewContainer, rootView)
        )

        interactionView.translatesAutoresizingMaskIntoConstraints = false
        rootView.addSubview(interactionView)
        NSLayoutConstraint.activateConstraints(
            getConstraintsToFillParent(interactionView, rootView)
        )
        interactionView.addSubview(renderingView)
    }

    // region Tencent Code
    internal fun accessibilityMediator(): AccessibilityMediator? =
        semanticsOwnerListener.current?.second
    // endregion

    fun setContent(content: @Composable () -> Unit) {
        runOnceViewAttached {
            scene.setContent {
                /**
                 * TODO isReadyToShowContent it is workaround we need to fix.
                 *  https://github.com/JetBrains/compose-multiplatform-core/pull/861
                 *  Density problem already was fixed.
                 *  But there are still problem with safeArea.
                 *  Elijah founded possible solution:
                 *   https://developer.apple.com/documentation/uikit/uiviewcontroller/4195485-viewisappearing
                 *   It is public for iOS 17 and hope back ported for iOS 13 as well (but we need to check)
                 */
                if (renderingComponent.isReadyToShowContent.value) {
                    ProvideComposeSceneMediatorCompositionLocals {
                        content()
                    }
                }
            }
        }
    }

    fun setContentImmediately(content: @Composable () -> Unit) {
        scene.setContent {
            ProvideComposeSceneMediatorCompositionLocals {
                content()
            }
        }
    }

    private val safeAreaState: MutableState<PlatformInsets> by lazy {
        //TODO It calc 0,0,0,0 on initialization
        mutableStateOf(calcSafeArea())
    }
    private val layoutMarginsState: MutableState<PlatformInsets> by lazy {
        //TODO It calc 0,0,0,0 on initialization
        mutableStateOf(calcLayoutMargin())
    }

    private val drawInSkiaState: MutableState<Boolean> by lazy {
        mutableStateOf(configuration.renderBackend == RenderBackend.Skia)
    }

    fun viewSafeAreaInsetsDidChange() {
        safeAreaState.value = calcSafeArea()
        layoutMarginsState.value = calcLayoutMargin()
    }

    @OptIn(InternalComposeApi::class)
    @Composable
    private fun ProvideComposeSceneMediatorCompositionLocals(content: @Composable () -> Unit) =
        CompositionLocalProvider(
            LocalUIKitInteropContext provides interopContext,
            LocalKeyboardOverlapHeight provides keyboardOverlapHeightState.value,
            LocalSafeArea provides safeAreaState.value,
            LocalLayoutMargins provides layoutMarginsState.value,
            LocalInteropContainer provides interopViewContainer,
            LocalDrawInSkia provides drawInSkiaState.value,
            content = content
        )

    fun dispose() {
        // region Tencent Code
        // focusStack?.popUntilNext(renderingView)
        // renderingView.dispose()
        // interactionView.dispose()
        // rootView.removeFromSuperview()
        // scene.close()
        // // After scene is disposed all UIKit interop actions can't be deferred to be synchronized with rendering
        // // Thus they need to be executed now.
        // interopContext.retrieve().actions.forEach { it.invoke() }

        coroutineScope.cancel()
        focusStack?.popUntilNext(interactionView)
        renderingComponent.dispose()
        interactionView.dispose()
        rootView.removeFromSuperview()
        // 漏掉 interactionView 和 renderingView 的 removeFromSuperview() 调用将会导致整个 vc 内存泄漏
        interactionView.removeFromSuperview()
        renderingView.removeFromSuperview()
        keyboardVisibilityListener.dispose()
        scene.close()
        // After scene is disposed all UIKit interop actions can't be deferred to be synchronized with rendering
        // Thus they need to be executed now.
        interopContext.retrieve().actions.forEach { it.invoke() }
        // endregion
    }

    fun onComposeSceneInvalidate() = renderingComponent.needRedraw()

    // region Tencent Code
    fun outerContainerOffsetChange(x: Float, y: Float) = scene.outerContainerOffsetChange(x, y)
    // endregion

    fun setLayout(value: SceneLayout) {
        _layout = value
        when (value) {
            SceneLayout.UseConstraintsToFillContainer -> {
                renderingView.setFrame(CGRectZero.readValue())
                renderingView.translatesAutoresizingMaskIntoConstraints = false
                constraints = getConstraintsToFillParent(renderingView, interactionView)
            }

            is SceneLayout.UseConstraintsToCenter -> {
                renderingView.setFrame(CGRectZero.readValue())
                renderingView.translatesAutoresizingMaskIntoConstraints = false
                constraints =
                    getConstraintsToCenterInParent(renderingView, interactionView, value.size)
            }

            is SceneLayout.Bounds -> {
                val density = container.systemDensity.density
                renderingView.translatesAutoresizingMaskIntoConstraints = true
                renderingView.setFrame(
                    with(value.rect) {
                        CGRectMake(
                            x = left.toDouble() / density,
                            y = top.toDouble() / density,
                            width = width.toDouble() / density,
                            height = height.toDouble() / density
                        )
                    }
                )
                constraints = emptyList()
            }

            is SceneLayout.Undefined -> error("setLayout, SceneLayout.Undefined")
        }
    }

    fun viewWillLayoutSubviews() {
        val density = container.systemDensity
        //TODO: Current code updates layout based on rootViewController size.
        // Maybe we need to rewrite it for SingleLayerComposeScene.

        scene.density = density // TODO: Maybe it is wrong to set density to scene here?
        // TODO: it should be updated on any container bounds change: resize or move itself or any parent
        val newBounds = boundsInWindow?.invoke() ?: calcBoundsInWindow()
        // Redraw on bounds changed only.
        // It will be always true for the first time layout.
        if (scene.boundsInWindow != newBounds) {
            renderingComponent.needRedrawSynchronously()
            scene.boundsInWindow = newBounds
        }
    }

    private fun calcBoundsInWindow(): IntRect {
        val offsetInWindow = windowContext.offsetInWindow(container)
        val size = container.bounds.useContents {
            with(density) {
                toDpRect().toRect().roundToIntRect()
            }
        }
        return IntRect(
            offset = offsetInWindow,
            size = IntSize(
                width = size.width,
                height = size.height,
            )
        )
    }

    private fun calcSafeArea(): PlatformInsets =
        container.safeAreaInsets.useContents {
            PlatformInsets(
                left = left.dp,
                top = top.dp,
                right = right.dp,
                bottom = bottom.dp,
            )
        }

    private fun calcLayoutMargin(): PlatformInsets =
        container.directionalLayoutMargins.useContents {
            PlatformInsets(
                left = leading.dp, // TODO: Check RTL support
                top = top.dp,
                right = trailing.dp, // TODO: Check RTL support
                bottom = bottom.dp,
            )
        }

    fun getBoundsInDp(): DpRect = renderingView.frame.useContents { this.toDpRect() }

    fun getBoundsInPx(): IntRect = with(container.systemDensity) {
        getBoundsInDp().toRect().roundToIntRect()
    }

    fun viewWillTransitionToSize(
        targetSize: CValue<CGSize>,
        coordinator: UIViewControllerTransitionCoordinatorProtocol
    ) {
        if (_layout is SceneLayout.Bounds) {
            //TODO Add logic to SceneLayout.Bounds too
            return
        }

        val startSnapshotView = renderingView.snapshotViewAfterScreenUpdates(false) ?: return
        startSnapshotView.translatesAutoresizingMaskIntoConstraints = false
        container.addSubview(startSnapshotView)
        targetSize.useContents {
            NSLayoutConstraint.activateConstraints(
                listOf(
                    startSnapshotView.widthAnchor.constraintEqualToConstant(height),
                    startSnapshotView.heightAnchor.constraintEqualToConstant(width),
                    startSnapshotView.centerXAnchor.constraintEqualToAnchor(container.centerXAnchor),
                    startSnapshotView.centerYAnchor.constraintEqualToAnchor(container.centerYAnchor)
                )
            )
        }

        renderingComponent.isForcedToPresentWithTransactionEveryFrame = true

        setLayout(SceneLayout.UseConstraintsToCenter(size = targetSize))
        renderingView.transform = coordinator.targetTransform

        coordinator.animateAlongsideTransition(
            animation = {
                startSnapshotView.alpha = 0.0
                startSnapshotView.transform = CGAffineTransformInvert(coordinator.targetTransform)
                renderingView.transform = CGAffineTransformIdentity.readValue()
            },
            completion = {
                startSnapshotView.removeFromSuperview()
                setLayout(SceneLayout.UseConstraintsToFillContainer)
                renderingComponent.isForcedToPresentWithTransactionEveryFrame = false
            }
        )
    }

    fun viewDidAppear(animated: Boolean) {
        keyboardVisibilityListener.prepare()
        // region Tencent Code Modify
        /*
        NSNotificationCenter.defaultCenter.addObserver(
            observer = nativeKeyboardVisibilityListener,
            selector = NSSelectorFromString(nativeKeyboardVisibilityListener::keyboardWillShow.name + ":"),
            name = UIKeyboardWillShowNotification,
            `object` = null
        )
        NSNotificationCenter.defaultCenter.addObserver(
            observer = nativeKeyboardVisibilityListener,
            selector = NSSelectorFromString(nativeKeyboardVisibilityListener::keyboardWillHide.name + ":"),
            name = UIKeyboardWillHideNotification,
            `object` = null
        )
        */
        // end region
    }

    // viewDidUnload() is deprecated and not called.
    fun viewWillDisappear(animated: Boolean) {
        // region Tencent Code Modify
        /*
        NSNotificationCenter.defaultCenter.removeObserver(
            observer = nativeKeyboardVisibilityListener,
            name = UIKeyboardWillShowNotification,
            `object` = null
        )
        NSNotificationCenter.defaultCenter.removeObserver(
            observer = nativeKeyboardVisibilityListener,
            name = UIKeyboardWillHideNotification,
            `object` = null
        )
        */
        // end region
    }

    fun getViewHeight(): Double = renderingView.frame.useContents {
        size.height
    }

    // region Tencent Code
    /**
     * Keep this for later use of measuring size from outside.
     */
    fun calculateContentSize(): IntSize = scene.calculateContentSize()
    // endregion

}

internal fun getConstraintsToFillParent(view: UIView, parent: UIView) =
    listOf(
        view.leftAnchor.constraintEqualToAnchor(parent.leftAnchor),
        view.rightAnchor.constraintEqualToAnchor(parent.rightAnchor),
        view.topAnchor.constraintEqualToAnchor(parent.topAnchor),
        view.bottomAnchor.constraintEqualToAnchor(parent.bottomAnchor)
    )

private fun getConstraintsToCenterInParent(
    view: UIView,
    parentView: UIView,
    size: CValue<CGSize>,
) = size.useContents {
    listOf(
        view.centerXAnchor.constraintEqualToAnchor(parentView.centerXAnchor),
        view.centerYAnchor.constraintEqualToAnchor(parentView.centerYAnchor),
        view.widthAnchor.constraintEqualToConstant(width),
        view.heightAnchor.constraintEqualToConstant(height)
    )
}

fun UITouchesEventPhase.toPointerEventType(): PointerEventType =
    when (this) {
        UITouchesEventPhase.BEGAN -> PointerEventType.Press
        UITouchesEventPhase.MOVED -> PointerEventType.Move
        UITouchesEventPhase.ENDED -> PointerEventType.Release
        UITouchesEventPhase.CANCELLED -> PointerEventType.Release
        UITouchesEventPhase.REDIRECTED -> PointerEventType.Release
    }

fun UIEvent.historicalChangesForTouch(
    touch: UITouch,
    view: UIView,
    density: Float
): List<HistoricalChange> {
    val touches = coalescedTouchesForTouch(touch) ?: return emptyList()

    return if (touches.size > 1) {
        // subList last index is exclusive, so the last touch in the list is not included
        // because it's the actual touch for which coalesced touches were requested
        touches.dropLast(1).map {
            val historicalTouch = it as UITouch
            val position = historicalTouch.offsetInView(view, density)
            HistoricalChange(
                uptimeMillis = (historicalTouch.timestamp * 1e3).toLong(),
                position = position,
                originalEventPosition = position
            )
        }
    } else {
        emptyList()
    }
}

val UITouch.isPressed
    get() = when (phase) {
        UITouchPhase.UITouchPhaseEnded, UITouchPhase.UITouchPhaseCancelled -> false
        else -> true
    }

fun UITouch.offsetInView(view: UIView, density: Float): Offset =
    locationInView(view).useContents {
        Offset(x.toFloat() * density, y.toFloat() * density)
    }

fun NSTimeInterval.toNanoSeconds(): Long {
    // The calculation is split in two instead of
    // `(targetTimestamp * 1e9).toLong()`
    // to avoid losing precision for fractional part
    val integral = floor(this)
    val fractional = this - integral
    val secondsToNanos = 1_000_000_000L
    val nanos = integral.roundToLong() * secondsToNanos + (fractional * 1e9).roundToLong()
    return nanos
}
