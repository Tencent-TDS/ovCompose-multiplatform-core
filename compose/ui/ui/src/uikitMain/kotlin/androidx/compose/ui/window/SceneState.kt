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

package androidx.compose.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.interop.LocalUIKitInteropContext
import androidx.compose.ui.interop.UIKitInteropContext
import androidx.compose.ui.platform.DefaultInputModeManager
import androidx.compose.ui.platform.EmptyViewConfiguration
import androidx.compose.ui.platform.LocalLayoutMargins
import androidx.compose.ui.platform.LocalSafeArea
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformInsets
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.UIKitTextInputService
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.platform.WindowInfoImpl
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.scene.ComposeSceneContext
import androidx.compose.ui.scene.ComposeSceneLayer
import androidx.compose.ui.scene.MultiLayerComposeScene
import androidx.compose.ui.scene.SingleLayerComposeScene
import androidx.compose.ui.text.input.PlatformTextInputService
import androidx.compose.ui.uikit.LocalKeyboardOverlapHeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToInt
import kotlinx.cinterop.CValue
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import org.jetbrains.skiko.SkikoKeyboardEvent
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSize
import platform.QuartzCore.CATransaction
import platform.UIKit.NSLayoutConstraint
import platform.UIKit.UIView
import platform.UIKit.UIViewController

internal interface SceneState<V> {
    val sceneView: V
    val scene: ComposeScene
    fun display(focusable: Boolean, onDisplayed: () -> Unit)
    fun dispose()
    fun needRedraw()

    var isForcedToPresentWithTransactionEveryFrame: Boolean
    val layers:MutableList<LayerState<V>>
    val windowInfo: WindowInfo
    fun updateLayout()
    val keyboardVisibilityListener: KeyboardVisibilityListener
    val densityProvider: DensityProvider
    val interopContext: UIKitInteropContext
    val platformContext: PlatformContext

    fun setConstraintsToCenterInView(parentView: V, size: CValue<CGSize>)
    fun setConstraintsToFillView(parentView: V)
    fun setContentWithCompositionLocals(content: @Composable () -> Unit)
    fun updateSafeArea()

    val delegate: SkikoUIViewDelegate
    val keyboardEventHandler: KeyboardEventHandler
    val uiKitTextInputService: UIKitTextInputService
    var bounds: IntRect
}

@OptIn(InternalComposeApi::class)
internal fun RootViewControllerState<UIViewController, UIView>.createSceneState(
    focusable: Boolean,
    transparentBackground: Boolean,
    buildScene: (SceneState<UIView>) -> ComposeScene,
): SceneState<UIView> = object : SceneState<UIView> {
    override val layers: MutableList<LayerState<UIView>> = mutableListOf()
    override val windowInfo = WindowInfoImpl().apply {
        isWindowFocused = focusable
    }

    fun calcSafeArea(): PlatformInsets =
        rootViewController.view.safeAreaInsets.useContents {
            PlatformInsets(
                left = left.dp,
                top = top.dp,
                right = right.dp,
                bottom = bottom.dp,
            )
        }

    fun calcLayoutMargin(): PlatformInsets =
        rootViewController.view.directionalLayoutMargins.useContents {
            PlatformInsets(
                left = leading.dp, // TODO: Check RTL support
                top = top.dp,
                right = trailing.dp, // TODO: Check RTL support
                bottom = bottom.dp,
            )
        }

    /**
     * TODO This is workaround we need to fix.
     *  https://github.com/JetBrains/compose-multiplatform-core/pull/861
     *  Density problem already was fixed.
     *  But there are still problem with safeArea.
     */
    val isReadyToShowContent: State<Boolean> get() = sceneView.isReadyToShowContent

    val safeAreaState: MutableState<PlatformInsets> by lazy {
        //TODO It calc 0,0,0,0 on initialization
        mutableStateOf(calcSafeArea())
    }
    val layoutMarginsState: MutableState<PlatformInsets> by lazy {
        //TODO It calc 0,0,0,0 on initialization
        mutableStateOf(calcLayoutMargin())
    }

    val keyboardOverlapHeightState: MutableState<Float> = mutableStateOf(0f)

    override fun updateSafeArea() {
        safeAreaState.value = calcSafeArea()
        layoutMarginsState.value = calcLayoutMargin()
    }

    override val densityProvider by lazy {
        DensityProviderImpl(
            uiViewControllerProvider = { rootViewController },
            viewProvider = { sceneView },
        )
    }

    override fun updateLayout() {
        val density = densityProvider()
        val scale = density.density
        //TODO: Old code updates layout based on rootViewController size.
        // Maybe we need to rewrite it for SingleLayerComposeScene.
        val size = rootViewController.view.frame.useContents {
            IntSize(
                width = (size.width * scale).roundToInt(),
                height = (size.height * scale).roundToInt()
            )
        }
        windowInfo.containerSize = size
        scene.density = density
        scene.size = size
        needRedraw()
    }

    override val sceneView: SkikoUIView by lazy {
        SkikoUIView(focusable, keyboardEventHandler, delegate, transparentBackground)
    }

    override var bounds: IntRect
        get() = delegate.bounds ?: IntRect.Zero
        set(value) {
            constraints = emptyList()
            delegate.bounds = value
            val density = densityProvider().density
            sceneView.setFrame(
                CGRectMake(
                    value.left.toDouble() / density,
                    value.top.toDouble() / density,
                    value.width.toDouble() / density,
                    value.height.toDouble() / density
                )
            )
        }

    override fun needRedraw() = sceneView.needRedraw()

    override fun dispose() {
        if (focusable) {
            focusStack.popUntilNext(sceneView)
        }
        sceneView.dispose()
        scene.close()
        // After scene is disposed all UIKit interop actions can't be deferred to be synchronized with rendering
        // Thus they need to be executed now.
        interopContext.retrieve().actions.forEach { it.invoke() }
    }

    override var isForcedToPresentWithTransactionEveryFrame: Boolean
        get() = sceneView.isForcedToPresentWithTransactionEveryFrame
        set(value) {
            sceneView.isForcedToPresentWithTransactionEveryFrame = value
        }

    override val scene: ComposeScene by lazy { buildScene(this) }

    override val interopContext: UIKitInteropContext by lazy {
        UIKitInteropContext(
            requestRedraw = { needRedraw() })
    }

    override val platformContext: PlatformContext by lazy {
        PlatformContextImpl(
            inputServices = uiKitTextInputService,
            textToolbar = uiKitTextInputService,
            windowInfo = windowInfo,
            densityProvider = densityProvider,
        )
    }

    override val keyboardVisibilityListener by lazy {
        KeyboardVisibilityListenerImpl(
            configuration = configuration,
            keyboardOverlapHeightState = keyboardOverlapHeightState,
            viewProvider = { rootViewController.view },
            sceneStates = sceneStates,
            densityProvider = densityProvider,
            sceneStateProvider = { this },
        )
    }

    @Composable
    fun SceneCompositionLocal(content: @Composable () -> Unit) =
        CompositionLocalProvider(
            LocalUIKitInteropContext provides interopContext,
            LocalKeyboardOverlapHeight provides keyboardVisibilityListener.keyboardOverlapHeightState.value,
            LocalSafeArea provides safeAreaState.value,
            LocalLayoutMargins provides layoutMarginsState.value,
            content = content
        )

    override fun setContentWithCompositionLocals(content: @Composable () -> Unit) {
        scene.setContent {
            if (isReadyToShowContent.value) {
                EntrypointCompositionLocals {
                    SceneCompositionLocal {
                        content()
                    }
                }
            }
        }
    }

    override fun display(focusable: Boolean, onDisplayed: () -> Unit) {
        sceneView.onAttachedToWindow = {
            sceneView.onAttachedToWindow = null
            updateLayout()
            onDisplayed()
            if (focusable) {
                focusStack.pushAndFocus(sceneView)
            }
        }
        rootViewController.view.addSubview(sceneView)
        setConstraintsToFillView(rootViewController.view)
    }

    override val uiKitTextInputService: UIKitTextInputService by lazy {
        UIKitTextInputService(
            updateView = {
                sceneView.setNeedsDisplay() // redraw on next frame
                CATransaction.flush() // clear all animations
            },
            rootViewProvider = { rootViewController.view },
            densityProvider = densityProvider,
            focusStack = focusStack,
            keyboardEventHandler = keyboardEventHandler
        )
    }

    override val keyboardEventHandler: KeyboardEventHandler by lazy {
        object : KeyboardEventHandler {
            override fun onKeyboardEvent(event: SkikoKeyboardEvent) {
                val composeEvent = KeyEvent(event)
                if (!uiKitTextInputService.onPreviewKeyEvent(composeEvent)) {
                    scene.sendKeyEvent(composeEvent)
                }
            }
        }
    }

    override val delegate: SkikoUIViewDelegate by lazy {
        SkikoUIViewDelegateImpl(
            { scene },
            interopContext,
            densityProvider,
        )
    }

    private var constraints: List<NSLayoutConstraint> = emptyList()
        set(value) {
            if (field.isNotEmpty()) {
                NSLayoutConstraint.deactivateConstraints(field)
            }
            field = value
            NSLayoutConstraint.activateConstraints(value)
        }

    override fun setConstraintsToCenterInView(parentView: UIView, size: CValue<CGSize>) {
//        if (delegate.bounds != null) return
        size.useContents {
            constraints = listOf(
                sceneView.centerXAnchor.constraintEqualToAnchor(parentView.centerXAnchor),
                sceneView.centerYAnchor.constraintEqualToAnchor(parentView.centerYAnchor),
                sceneView.widthAnchor.constraintEqualToConstant(width),
                sceneView.heightAnchor.constraintEqualToConstant(height)
            )
        }
    }

    override fun setConstraintsToFillView(parentView: UIView) {
//        if (delegate.bounds != null) return
        constraints = listOf(
            sceneView.leftAnchor.constraintEqualToAnchor(parentView.leftAnchor),
            sceneView.rightAnchor.constraintEqualToAnchor(parentView.rightAnchor),
            sceneView.topAnchor.constraintEqualToAnchor(parentView.topAnchor),
            sceneView.bottomAnchor.constraintEqualToAnchor(parentView.bottomAnchor)
        )
    }
}
