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
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.interop.LocalUIKitInteropContext
import androidx.compose.ui.interop.UIKitInteropContext
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.UIKitTextInputService
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.scene.ComposeSceneContext
import androidx.compose.ui.scene.ComposeSceneLayer
import androidx.compose.ui.scene.MultiLayerComposeScene
import androidx.compose.ui.scene.SingleLayerComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.coroutines.CoroutineContext
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
    val isReadyToShowContent: State<Boolean>//TODO it is redundant workaround
    fun needRedraw()
    fun dispose()
    var isForcedToPresentWithTransactionEveryFrame: Boolean
    val layers:MutableList<LayerState<V>>

    val scene: ComposeScene
    val interopContext: UIKitInteropContext
    val platformContext: PlatformContext

    fun setConstraintsToCenterInView(parentView: V, size: CValue<CGSize>)
    fun setConstraintsToFillView(parentView: V)
    fun setContentWithCompositionLocals(content: @Composable () -> Unit)
    fun display(focusable: Boolean)

    val delegate: SkikoUIViewDelegate
    val keyboardEventHandler: KeyboardEventHandler
    val uiKitTextInputService: UIKitTextInputService
}

private val coroutineDispatcher = Dispatchers.Main

internal fun RootViewControllerState<UIViewController, UIView>.createSingleLayerSceneUIViewState(focusable: Boolean): SceneState<UIView> =
    prepareSingleLayerComposeScene(
        densityProvider = densityProvider,
        layoutDirection = LayoutDirection.Ltr, //TODO get from system
        focusable = focusable,
        coroutineContext = coroutineDispatcher
    ) {
        object : ComposeSceneContext {
            override fun createPlatformLayer(
                density: Density,
                layoutDirection: LayoutDirection,
                focusable: Boolean,
                compositionContext: CompositionContext
            ): ComposeSceneLayer {
                return prepareSingleLayerComposeScene(
                    densityProvider,
                    layoutDirection,
                    focusable,
                    compositionContext.effectCoroutineContext,
                    { this },
                ).run {
                    val layerState = createLayerState(density, layoutDirection, focusable)
                    layerState.display()
                    layers.add(layerState)
                    layerState.layer
                }
            }
        }
    }

internal fun RootViewControllerState<UIViewController, UIView>.createMultiLayerSceneUIViewState(): SceneState<UIView> {
    return createStateWithSceneBuilder(focusable = true) {
        MultiLayerComposeScene(
            coroutineContext = coroutineDispatcher,
            composeSceneContext = object : ComposeSceneContext {
                override val platformContext: PlatformContext get() = this@createStateWithSceneBuilder.platformContext
            },
            density = densityProvider(),
            invalidate = ::needRedraw,
        )
    }
}

private fun RootViewControllerState<UIViewController, UIView>.createStateWithSceneBuilder(
    focusable: Boolean,
    buildScene: SceneState<UIView>.() -> ComposeScene,
): SceneState<UIView> = object : SceneState<UIView> {
    override val layers: MutableList<LayerState<UIView>> = mutableListOf()
    override val sceneView: SkikoUIView by lazy {
        SkikoUIView(
            focusable,
            keyboardEventHandler,
            delegate
        )
    }
    override val isReadyToShowContent: State<Boolean> by lazy {
        sceneView.isReadyToShowContent
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

    override val scene: ComposeScene by lazy { buildScene() }

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

    @Composable
    fun SceneCompositionLocal(content: @Composable () -> Unit) =
        CompositionLocalProvider(
            LocalUIKitInteropContext provides interopContext,
            content = content
        )

    override fun setContentWithCompositionLocals(content: @Composable () -> Unit) {
        scene.setContent {
            if (isReadyToShowContent.value) { // TODO add link to issue with recomposition twice description
                EntrypointCompositionLocals {
                    SceneCompositionLocal {
                        content()
                    }
                }
            }
        }
    }

    override fun display(focusable: Boolean) {
        rootViewController.view.addSubview(sceneView)
        setConstraintsToFillView(rootViewController.view)
        updateLayout(this)
        if (focusable) {
            focusStack.pushAndFocus(sceneView)
        }
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

    private var constraints: List<NSLayoutConstraint> = emptyList()//todo duplicate
        set(value) {
            if (field.isNotEmpty()) {
                NSLayoutConstraint.deactivateConstraints(field)
            }
            field = value
            NSLayoutConstraint.activateConstraints(value)
        }

    override fun setConstraintsToCenterInView(parentView: UIView, size: CValue<CGSize>) {
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
        constraints = listOf(
            sceneView.leftAnchor.constraintEqualToAnchor(parentView.leftAnchor),
            sceneView.rightAnchor.constraintEqualToAnchor(parentView.rightAnchor),
            sceneView.topAnchor.constraintEqualToAnchor(parentView.topAnchor),
            sceneView.bottomAnchor.constraintEqualToAnchor(parentView.bottomAnchor)
        )
    }
}

private fun RootViewControllerState<UIViewController, UIView>.prepareSingleLayerComposeScene(
    densityProvider: DensityProvider,
    layoutDirection: LayoutDirection,
    focusable: Boolean,
    coroutineContext: CoroutineContext,
    prepareComposeSceneContext: () -> ComposeSceneContext,
): SceneState<UIView> = createStateWithSceneBuilder(focusable) {
    SingleLayerComposeScene(
        coroutineContext = coroutineContext,
        composeSceneContext = object :
            ComposeSceneContext by prepareComposeSceneContext() {
            //todo do we need new platform context on every SingleLayerComposeScene?
            override val platformContext: PlatformContext get() = this@createStateWithSceneBuilder.platformContext
        },
        density = densityProvider(),
        invalidate = ::needRedraw,
        layoutDirection = layoutDirection,
    )
}
