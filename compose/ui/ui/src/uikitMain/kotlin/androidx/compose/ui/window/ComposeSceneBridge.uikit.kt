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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.interop.LocalUIKitInteropContext
import androidx.compose.ui.interop.UIKitInteropContext
import androidx.compose.ui.platform.IOSPlatformContextImpl
import androidx.compose.ui.platform.LocalLayoutMargins
import androidx.compose.ui.platform.LocalSafeArea
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformInsets
import androidx.compose.ui.platform.UIKitTextInputService
import androidx.compose.ui.platform.WindowInfoImpl
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.uikit.LocalKeyboardOverlapHeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toOffset
import kotlin.math.roundToInt
import kotlinx.cinterop.CValue
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import org.jetbrains.skiko.SkikoKeyboardEvent
import platform.CoreGraphics.CGAffineTransformIdentity
import platform.CoreGraphics.CGAffineTransformInvert
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGRectZero
import platform.CoreGraphics.CGSize
import platform.QuartzCore.CATransaction
import platform.UIKit.NSLayoutConstraint
import platform.UIKit.UIView
import platform.UIKit.UIViewControllerTransitionCoordinatorProtocol

/**
 * ComposeSceneBridge represents scene: ComposeScene with sceneView: UIView and state manipulation functions.
 */
internal interface ComposeSceneBridge {
    /**
     * ComposeScene with @Composable content
     */
    val scene: ComposeScene

    /**
     * iOS view to display
     */
    val sceneView: UIView
    fun setContent(content: @Composable () -> Unit)
    fun display(focusable: Boolean, onDisplayed: () -> Unit)
    fun dispose()
    fun needRedraw()
    fun setLayout(value: SceneLayout)
    fun updateLayout()
    val keyboardVisibilityListener: KeyboardVisibilityListener
    val densityProvider: DensityProvider
    val platformContext: PlatformContext
    fun updateSafeArea()
    fun getViewBounds(): IntRect
    fun animateTransition(
        targetSize: CValue<CGSize>,
        coordinator: UIViewControllerTransitionCoordinatorProtocol
    )
}

/**
 * Layout of sceneView on the screen
 */
internal sealed interface SceneLayout {
    object Undefined : SceneLayout
    object UseConstraintsToFillContainer : SceneLayout
    class UseConstraintsToCenter(val size: CValue<CGSize>) : SceneLayout
    class Bounds(val rect: IntRect) : SceneLayout
}

/**
 * Builder of ComposeSceneBridge with UIView inside.
 */
@OptIn(InternalComposeApi::class)
internal fun ComposeBridge.createComposeSceneBridge(
    focusable: Boolean,
    transparentBackground: Boolean,
    buildScene: (ComposeSceneBridge) -> ComposeScene,
): ComposeSceneBridge = object : ComposeSceneBridge {
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
    private val windowInfo = WindowInfoImpl().apply {
        isWindowFocused = focusable
    }

    override val scene: ComposeScene by lazy { buildScene(this) }

    override val sceneView: SkikoUIView by lazy {
        SkikoUIView(focusable, keyboardEventHandler, delegate, transparentBackground)
    }

    override val densityProvider by lazy {
        DensityProviderImpl(
            uiViewControllerProvider = { rootViewController },
            viewProvider = { sceneView },
        )
    }

    private val interopContext: UIKitInteropContext by lazy {
        UIKitInteropContext(
            requestRedraw = { needRedraw() }
        )
    }

    override val platformContext: PlatformContext by lazy {
        IOSPlatformContextImpl(
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
            composeSceneBridges = composeSceneBridges,
            densityProvider = densityProvider,
            composeSceneBridgeProvider = { this },
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
                sceneView.setNeedsDisplay() // redraw on next frame
                CATransaction.flush() // clear all animations
            },
            rootViewProvider = { rootViewController.view },
            densityProvider = densityProvider,
            focusStack = focusStack,
            keyboardEventHandler = keyboardEventHandler
        )
    }

    private val delegate: SkikoUIViewDelegate by lazy {
        SkikoUIViewDelegateImpl(
            { scene },
            interopContext,
            densityProvider,
        )
    }

    override fun setContent(content: @Composable () -> Unit) {
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
            if (sceneView.isReadyToShowContent.value) {
                ProvideRootCompositionLocals {
                    ProvideComposeSceneBridgeCompositionLocals {
                        content()
                    }
                }
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

    override fun updateSafeArea() {
        safeAreaState.value = calcSafeArea()
        layoutMarginsState.value = calcLayoutMargin()
    }

    @Composable
    private fun ProvideComposeSceneBridgeCompositionLocals(content: @Composable () -> Unit) =
        CompositionLocalProvider(
            LocalUIKitInteropContext provides interopContext,
            LocalKeyboardOverlapHeight provides keyboardVisibilityListener.keyboardOverlapHeightState.value,
            LocalSafeArea provides safeAreaState.value,
            LocalLayoutMargins provides layoutMarginsState.value,
            content = content
        )

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
    }

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

    override fun needRedraw() = sceneView.needRedraw()

    override fun setLayout(value: SceneLayout) {
        _layout = value
        when (value) {
            SceneLayout.UseConstraintsToFillContainer -> {
                delegate.metalOffset = Offset.Zero
                sceneView.setFrame(CGRectZero.readValue())
                sceneView.translatesAutoresizingMaskIntoConstraints = false
                constraints = listOf(
                    sceneView.leftAnchor.constraintEqualToAnchor(rootViewController.view.leftAnchor),
                    sceneView.rightAnchor.constraintEqualToAnchor(rootViewController.view.rightAnchor),
                    sceneView.topAnchor.constraintEqualToAnchor(rootViewController.view.topAnchor),
                    sceneView.bottomAnchor.constraintEqualToAnchor(rootViewController.view.bottomAnchor)
                )
            }

            is SceneLayout.UseConstraintsToCenter -> {
                delegate.metalOffset = Offset.Zero
                sceneView.setFrame(CGRectZero.readValue())
                sceneView.translatesAutoresizingMaskIntoConstraints = false
                constraints = value.size.useContents {
                    listOf(
                        sceneView.centerXAnchor.constraintEqualToAnchor(rootViewController.view.centerXAnchor),
                        sceneView.centerYAnchor.constraintEqualToAnchor(rootViewController.view.centerYAnchor),
                        sceneView.widthAnchor.constraintEqualToConstant(width),
                        sceneView.heightAnchor.constraintEqualToConstant(height)
                    )
                }
            }

            is SceneLayout.Bounds -> {
                delegate.metalOffset = -value.rect.topLeft.toOffset()
                val density = densityProvider().density
                sceneView.translatesAutoresizingMaskIntoConstraints = true
                sceneView.setFrame(
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
        sceneView.updateMetalLayerSize()
    }

    override fun updateLayout() {
        val density = densityProvider()
        val scale = density.density
        //TODO: Current code updates layout based on rootViewController size.
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

    private fun calcSafeArea(): PlatformInsets =
        rootViewController.view.safeAreaInsets.useContents {
            PlatformInsets(
                left = left.dp,
                top = top.dp,
                right = right.dp,
                bottom = bottom.dp,
            )
        }

    private fun calcLayoutMargin(): PlatformInsets =
        rootViewController.view.directionalLayoutMargins.useContents {
            PlatformInsets(
                left = leading.dp, // TODO: Check RTL support
                top = top.dp,
                right = trailing.dp, // TODO: Check RTL support
                bottom = bottom.dp,
            )
        }

    override fun getViewBounds(): IntRect = sceneView.frame.useContents {
        val density = densityProvider().density
        IntRect(
            offset = IntOffset(
                x = (origin.x * density).roundToInt(),
                y = (origin.y * density).roundToInt(),
            ),
            size = IntSize(
                width = (size.width * density).roundToInt(),
                height = (size.height * density).roundToInt(),
            )
        )
    }

    override fun animateTransition(
        targetSize: CValue<CGSize>,
        coordinator: UIViewControllerTransitionCoordinatorProtocol
    ) {
        if (_layout is SceneLayout.Bounds) {
            //TODO Add logic to SceneLayout.Bounds too
            return
        }

        val startSnapshotView = sceneView.snapshotViewAfterScreenUpdates(false) ?: return
        startSnapshotView.translatesAutoresizingMaskIntoConstraints = false
        rootViewController.view.addSubview(startSnapshotView)
        targetSize.useContents {
            NSLayoutConstraint.activateConstraints(
                listOf(
                    startSnapshotView.widthAnchor.constraintEqualToConstant(height),
                    startSnapshotView.heightAnchor.constraintEqualToConstant(width),
                    startSnapshotView.centerXAnchor.constraintEqualToAnchor(rootViewController.view.centerXAnchor),
                    startSnapshotView.centerYAnchor.constraintEqualToAnchor(rootViewController.view.centerYAnchor)
                )
            )
        }

        sceneView.isForcedToPresentWithTransactionEveryFrame = true

        setLayout(SceneLayout.UseConstraintsToCenter(size = targetSize))
        sceneView.transform = coordinator.targetTransform

        coordinator.animateAlongsideTransition(
            animation = {
                startSnapshotView.alpha = 0.0
                startSnapshotView.transform = CGAffineTransformInvert(coordinator.targetTransform)
                sceneView.transform = CGAffineTransformIdentity.readValue()
            },
            completion = {
                startSnapshotView.removeFromSuperview()
                setLayout(SceneLayout.UseConstraintsToFillContainer)
                sceneView.isForcedToPresentWithTransactionEveryFrame = false
            }
        )
    }
}
