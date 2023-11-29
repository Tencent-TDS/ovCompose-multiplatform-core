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
import androidx.compose.ui.LocalSystemTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.interop.LocalLayerContainer
import androidx.compose.ui.interop.LocalUIKitInteropContext
import androidx.compose.ui.interop.LocalUIViewController
import androidx.compose.ui.interop.UIKitInteropContext
import androidx.compose.ui.platform.LocalLayoutMargins
import androidx.compose.ui.platform.LocalSafeArea
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.UIKitTextInputService
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.scene.ComposeSceneContext
import androidx.compose.ui.scene.ComposeSceneLayer
import androidx.compose.ui.scene.MultiLayerComposeScene
import androidx.compose.ui.scene.SingleLayerComposeScene
import androidx.compose.ui.text.input.PlatformTextInputService
import androidx.compose.ui.uikit.LocalInterfaceOrientation
import androidx.compose.ui.uikit.LocalKeyboardOverlapHeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
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
import platform.UIKit.reloadInputViews

internal interface SceneViewState<V> {
    val sceneView: V
    val isReadyToShowContent: State<Boolean>
    fun needRedraw()
    fun dispose()
    var isForcedToPresentWithTransactionEveryFrame: Boolean

    val scene: ComposeScene
    val interopContext: UIKitInteropContext
    val platformContext: PlatformContext

    fun setConstraintsToCenterInView(parentView: V, size: CValue<CGSize>)
    fun setConstraintsToFillView(parentView: V)
    fun setContentWithCompositionLocals(content: @Composable () -> Unit)
    fun display(focusable: Boolean)
}

private val coroutineDispatcher = Dispatchers.Main

internal fun ComposeViewState<UIViewController, UIView>.createSingleLayerSceneUIViewState(
    updateContainerSize: (IntSize) -> Unit,
): SceneViewState<UIView> =
    prepareSingleLayerComposeScene(
        densityProvider = densityProvider,
        layoutDirection = LayoutDirection.Ltr, //TODO get from system
        focusable = true,
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
                    display(focusable)
                    sceneView.alpha = 0.5

                    object : ComposeSceneLayer {
                        override var density: Density = density
                        override var layoutDirection: LayoutDirection = layoutDirection
                        override var bounds: IntRect
                            get() = IntRect(
                                offset = IntOffset(
                                    x = sceneView.bounds.useContents { origin.x.toInt() },
                                    y = sceneView.bounds.useContents { origin.y.toInt() },
                                ),
                                size = IntSize(
                                    width = sceneView.bounds.useContents { size.width.toInt() },
                                    height = sceneView.bounds.useContents { size.height.toInt() },
                                )
                            )
                            set(value) {
                                println("ComposeSceneLayer, set bounds $value")
                                sceneView.setBounds(
                                    CGRectMake(
                                        value.left.toDouble(),
                                        value.top.toDouble(),
                                        value.width.toDouble(),
                                        value.height.toDouble()
                                    )
                                )
                            }
                        override var scrimColor: Color? = null
                        override var focusable: Boolean = true

                        override fun close() {
                            println("ComposeSceneContext close")
                            dispose()
                            sceneView.removeFromSuperview()
                        }

                        override fun setContent(content: @Composable () -> Unit) {
                            //todo New Compose Scene  Размер сцены scene.size - полный экран
                            //  Сделать translate Canvas по размеру -bounds.position, размер канвы bounds.size
                            //  translate делать при каждой отрисовке
                            //  canvas.translate(x, y)
                            //  drawContainedDrawModifiers(canvas)
                            //  canvas.translate(-x, -y)
                            //  А размер канвы задавать в bounds set(value) {...
                            setContentWithCompositionLocals(content)
                        }

                        override fun setKeyEventListener(
                            onPreviewKeyEvent: ((KeyEvent) -> Boolean)?,
                            onKeyEvent: ((KeyEvent) -> Boolean)?
                        ) {
                            //todo
                        }

                        override fun setOutsidePointerEventListener(onOutsidePointerEvent: ((mainEvent: Boolean) -> Unit)?) {
                            //todo
                        }
                    }
                }
            }
        }
    }

internal fun ComposeViewState<UIViewController, UIView>.createMultiLayerSceneUIViewState(): SceneViewState<UIView> {
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

private fun ComposeViewState<UIViewController, UIView>.createStateWithSceneBuilder(
    focusable: Boolean,
    buildScene: SceneViewState<UIView>.() -> ComposeScene,
): SceneViewState<UIView> = object : SceneViewState<UIView> {
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
        rootView.view.addSubview(sceneView)
        setConstraintsToFillView(rootView.view)
        updateLayout(this)
        if (focusable) {
            focusStack.pushAndFocus(sceneView)
        }
    }

    val uiKitTextInputService: UIKitTextInputService by lazy {
        UIKitTextInputService(
            updateView = {
                sceneView.setNeedsDisplay() // redraw on next frame
                CATransaction.flush() // clear all animations
                sceneView.reloadInputViews() // update input (like screen keyboard)//todo redundant?
            },
            rootViewProvider = { rootView.view },
            densityProvider = densityProvider,
            focusStack = focusStack,
            keyboardEventHandler = keyboardEventHandler
        )
    }
    val keyboardEventHandler: KeyboardEventHandler by lazy {
        object : KeyboardEventHandler {
            override fun onKeyboardEvent(event: SkikoKeyboardEvent) {
                val composeEvent = KeyEvent(event)
                if (!uiKitTextInputService.onPreviewKeyEvent(composeEvent)) {
                    scene.sendKeyEvent(composeEvent)
                }
            }
        }
    }
    val delegate: SkikoUIViewDelegate by lazy {
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

private fun ComposeViewState<UIViewController, UIView>.prepareSingleLayerComposeScene(
    densityProvider: DensityProvider,
    layoutDirection: LayoutDirection,
    focusable: Boolean,
    coroutineContext: CoroutineContext,
    prepareComposeSceneContext: () -> ComposeSceneContext,
): SceneViewState<UIView> = createStateWithSceneBuilder(focusable) {
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
