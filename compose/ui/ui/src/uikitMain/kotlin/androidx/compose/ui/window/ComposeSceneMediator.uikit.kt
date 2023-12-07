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
import platform.UIKit.UIViewControllerTransitionCoordinatorProtocol

/**
 * Layout of sceneView on the screen
 */
internal sealed interface SceneLayout {
    object Undefined : SceneLayout
    object UseConstraintsToFillContainer : SceneLayout
    class UseConstraintsToCenter(val size: CValue<CGSize>) : SceneLayout
    class Bounds(val rect: IntRect) : SceneLayout
}

internal class ComposeSceneMediator(
    private val container: ComposeContainer,
    private val focusable: Boolean,
    transparentBackground: Boolean,
    buildScene: (ComposeSceneMediator) -> ComposeScene,
) {
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

    val scene: ComposeScene by lazy { buildScene(this) }

    val sceneView: SkikoUIView by lazy {
        SkikoUIView(focusable, keyboardEventHandler, delegate, transparentBackground)
    }

    val densityProvider by lazy {
        DensityProviderImpl(
            uiViewControllerProvider = { container.containerViewController },
            viewProvider = { sceneView },
        )
    }

    private val interopContext: UIKitInteropContext by lazy {
        UIKitInteropContext(
            requestRedraw = { needRedraw() }
        )
    }

    val platformContext: PlatformContext by lazy {
        IOSPlatformContextImpl(
            inputServices = uiKitTextInputService,
            textToolbar = uiKitTextInputService,
            windowInfo = windowInfo,
            densityProvider = densityProvider,
        )
    }

    val keyboardVisibilityListener by lazy {
        KeyboardVisibilityListenerImpl(
            configuration = container.configuration,
            keyboardOverlapHeightState = keyboardOverlapHeightState,
            viewProvider = { container.containerViewController.view },
            densityProvider = densityProvider,
            composeSceneMediatorProvider = { this },
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
            rootViewProvider = { container.containerViewController.view },
            densityProvider = densityProvider,
            focusStack = container.focusStack,
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

    fun setContent(content: @Composable () -> Unit) {
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
                container.ProvideRootCompositionLocals {
                    ProvideComposeSceneMediatorCompositionLocals {
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

    fun updateSafeArea() {
        safeAreaState.value = calcSafeArea()
        layoutMarginsState.value = calcLayoutMargin()
    }

    @OptIn(InternalComposeApi::class)
    @Composable
    private fun ProvideComposeSceneMediatorCompositionLocals(content: @Composable () -> Unit) =
        CompositionLocalProvider(
            LocalUIKitInteropContext provides interopContext,
            LocalKeyboardOverlapHeight provides keyboardVisibilityListener.keyboardOverlapHeightState.value,
            LocalSafeArea provides safeAreaState.value,
            LocalLayoutMargins provides layoutMarginsState.value,
            content = content
        )

    fun display(focusable: Boolean, onDisplayed: () -> Unit) {
        sceneView.onAttachedToWindow = {
            sceneView.onAttachedToWindow = null
            updateLayout()
            onDisplayed()
            if (focusable) {
                container.focusStack.pushAndFocus(sceneView)
            }
        }
        container.containerViewController.view.addSubview(sceneView)
    }

    fun dispose() {
        if (focusable) {
            container.focusStack.popUntilNext(sceneView)
        }
        sceneView.dispose()
        sceneView.removeFromSuperview()
        scene.close()
        // After scene is disposed all UIKit interop actions can't be deferred to be synchronized with rendering
        // Thus they need to be executed now.
        interopContext.retrieve().actions.forEach { it.invoke() }
    }

    fun needRedraw() = sceneView.needRedraw()

    fun setLayout(value: SceneLayout) {
        _layout = value
        when (value) {
            SceneLayout.UseConstraintsToFillContainer -> {
                delegate.metalOffset = Offset.Zero
                sceneView.setFrame(CGRectZero.readValue())
                sceneView.translatesAutoresizingMaskIntoConstraints = false
                constraints = listOf(
                    sceneView.leftAnchor.constraintEqualToAnchor(container.containerViewController.view.leftAnchor),
                    sceneView.rightAnchor.constraintEqualToAnchor(container.containerViewController.view.rightAnchor),
                    sceneView.topAnchor.constraintEqualToAnchor(container.containerViewController.view.topAnchor),
                    sceneView.bottomAnchor.constraintEqualToAnchor(container.containerViewController.view.bottomAnchor)
                )
            }

            is SceneLayout.UseConstraintsToCenter -> {
                delegate.metalOffset = Offset.Zero
                sceneView.setFrame(CGRectZero.readValue())
                sceneView.translatesAutoresizingMaskIntoConstraints = false
                constraints = value.size.useContents {
                    listOf(
                        sceneView.centerXAnchor.constraintEqualToAnchor(container.containerViewController.view.centerXAnchor),
                        sceneView.centerYAnchor.constraintEqualToAnchor(container.containerViewController.view.centerYAnchor),
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

    fun updateLayout() {
        val density = densityProvider()
        val scale = density.density
        //TODO: Current code updates layout based on rootViewController size.
        // Maybe we need to rewrite it for SingleLayerComposeScene.
        val size = container.containerViewController.view.frame.useContents {
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
        container.containerViewController.view.safeAreaInsets.useContents {
            PlatformInsets(
                left = left.dp,
                top = top.dp,
                right = right.dp,
                bottom = bottom.dp,
            )
        }

    private fun calcLayoutMargin(): PlatformInsets =
        container.containerViewController.view.directionalLayoutMargins.useContents {
            PlatformInsets(
                left = leading.dp, // TODO: Check RTL support
                top = top.dp,
                right = trailing.dp, // TODO: Check RTL support
                bottom = bottom.dp,
            )
        }

    fun getViewBounds(): IntRect = sceneView.frame.useContents {
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

    fun animateTransition(
        targetSize: CValue<CGSize>,
        coordinator: UIViewControllerTransitionCoordinatorProtocol
    ) {
        if (_layout is SceneLayout.Bounds) {
            //TODO Add logic to SceneLayout.Bounds too
            return
        }

        val startSnapshotView = sceneView.snapshotViewAfterScreenUpdates(false) ?: return
        startSnapshotView.translatesAutoresizingMaskIntoConstraints = false
        container.containerViewController.view.addSubview(startSnapshotView)
        targetSize.useContents {
            NSLayoutConstraint.activateConstraints(
                listOf(
                    startSnapshotView.widthAnchor.constraintEqualToConstant(height),
                    startSnapshotView.heightAnchor.constraintEqualToConstant(width),
                    startSnapshotView.centerXAnchor.constraintEqualToAnchor(container.containerViewController.view.centerXAnchor),
                    startSnapshotView.centerYAnchor.constraintEqualToAnchor(container.containerViewController.view.centerYAnchor)
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
