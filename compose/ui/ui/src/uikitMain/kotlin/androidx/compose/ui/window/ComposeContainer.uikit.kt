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
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.LocalSystemTheme
import androidx.compose.ui.SystemTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.interop.LocalLayerContainer
import androidx.compose.ui.interop.LocalUIViewController
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.WindowInfoImpl
import androidx.compose.ui.scene.ComposeSceneContext
import androidx.compose.ui.scene.ComposeSceneLayer
import androidx.compose.ui.scene.MultiLayerComposeScene
import androidx.compose.ui.scene.SingleLayerComposeScene
import androidx.compose.ui.uikit.ComposeUIViewControllerConfiguration
import androidx.compose.ui.uikit.InterfaceOrientation
import androidx.compose.ui.uikit.LocalInterfaceOrientation
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachReversed
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToInt
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import platform.Foundation.NSNotification
import platform.UIKit.UIApplication
import platform.UIKit.UIUserInterfaceLayoutDirection
import platform.UIKit.UIView
import platform.UIKit.UIViewController

private val coroutineDispatcher = Dispatchers.Main

@OptIn(InternalComposeApi::class)
internal class ComposeContainer(
    val configuration: ComposeUIViewControllerConfiguration,
    val containerViewController: UIViewController,
) {
    private val composeSceneMediators: MutableList<ComposeSceneMediator> = mutableListOf()
    private val layers: MutableList<ComposeSceneLayer> = mutableListOf()

    val layoutDirection get() = getLayoutDirection()

    /*
     * Initial value is arbitrarily chosen to avoid propagating invalid value logic
     * It's never the case in real usage scenario to reflect that in type system
     */
    val interfaceOrientationState: MutableState<InterfaceOrientation> = mutableStateOf(
        InterfaceOrientation.Portrait
    )
    val systemThemeState: MutableState<SystemTheme> = mutableStateOf(SystemTheme.Unknown)
    val focusStack: FocusStack<UIView> = FocusStackImpl()
    private val windowInfo = WindowInfoImpl()

    @Composable
    fun ProvideContainerCompositionLocals(content: @Composable () -> Unit) =
        CompositionLocalProvider(
            LocalUIViewController provides containerViewController,
            LocalLayerContainer provides containerViewController.view,
            LocalInterfaceOrientation provides interfaceOrientationState.value,
            LocalSystemTheme provides systemThemeState.value,
            content = content
        )

    @OptIn(ExperimentalComposeApi::class)
    fun setContent(content: @Composable () -> Unit) {
        if (composeSceneMediators.isNotEmpty()) {
            return // already attached
        }

        val mediator = if (configuration.platformLayers) {
            createSingleLayerComposeSceneMediator()
        } else {
            createMultiLayerComposeSceneMediator()
        }
        composeSceneMediators.add(mediator)
        mediator.display(
            focusable = true,
            onDisplayed = {
                mediator.setContent {
                    ProvideContainerCompositionLocals {
                        content()
                    }
                }
            }
        )
        mediator.setLayout(SceneLayout.UseConstraintsToFillContainer)
    }

    val keyboardVisibilityListener = object : KeyboardVisibilityListener {
        override fun keyboardWillShow(arg: NSNotification) = composeSceneMediators.fastForEach {
            it.keyboardWillShow(arg)
        }

        override fun keyboardWillHide(arg: NSNotification) = composeSceneMediators.fastForEach {
            it.keyboardWillHide(arg)
        }
    }

    fun createLayer(
        currentComposeSceneContext: ComposeSceneContext,
        focusable: Boolean,
        sceneMediator: ComposeSceneMediator,
        coroutineDispatcher: CoroutineContext,
    ): ComposeSceneLayer {
        val mediator = ComposeSceneMediator(
            viewControllerProvider = { containerViewController },
            configuration = configuration,
            focusStack = focusStack,
            windowInfo = windowInfo,
            transparency = true,
        ) {
            SingleLayerComposeScene(
                coroutineContext = coroutineDispatcher,
                composeSceneContext = currentComposeSceneContext,
                density = sceneMediator.densityProvider(),
                invalidate = sceneMediator::onComposeSceneInvalidate,
                layoutDirection = layoutDirection,
            )
        }
        val layer = object : ComposeSceneLayer {
            override var density: Density = sceneMediator.densityProvider()
            override var layoutDirection: LayoutDirection = this@ComposeContainer.layoutDirection
            override var bounds: IntRect
                get() = mediator.getViewBounds()
                set(value) {
                    mediator.setLayout(
                        SceneLayout.Bounds(rect = value)
                    )
                }
            override var scrimColor: Color? = null
            override var focusable: Boolean = focusable

            override fun close() {
                mediator.dispose()
                composeSceneMediators.remove(mediator)
                layers.remove(this)
            }

            override fun setContent(content: @Composable () -> Unit) {
                mediator.setContent {
                    ProvideContainerCompositionLocals {
                        content()
                    }
                }
            }

            override fun setKeyEventListener(
                onPreviewKeyEvent: ((KeyEvent) -> Boolean)?,
                onKeyEvent: ((KeyEvent) -> Boolean)?
            ) {
                //todo
            }

            override fun setOutsidePointerEventListener(
                onOutsidePointerEvent: ((mainEvent: Boolean) -> Unit)?
            ) {
                //todo
            }
        }

        mediator.display(focusable = focusable, onDisplayed = {})
        composeSceneMediators.add(mediator)
        layers.add(layer)
        return layer
    }

    fun dispose() {
        composeSceneMediators.fastForEachReversed {
            it.dispose()
        }
        composeSceneMediators.clear()
    }

    private fun createSingleLayerComposeSceneMediator(): ComposeSceneMediator =
        ComposeSceneMediator(
            viewControllerProvider = { containerViewController },
            configuration = configuration,
            focusStack = focusStack,
            windowInfo = windowInfo,
            transparency = false
        ) { mediator: ComposeSceneMediator ->
            val context = object : ComposeSceneContext {
                override val platformContext: PlatformContext get() = mediator.platformContext
                override fun createPlatformLayer(
                    density: Density,
                    layoutDirection: LayoutDirection,
                    focusable: Boolean,
                    compositionContext: CompositionContext
                ): ComposeSceneLayer =
                    createLayer(
                        currentComposeSceneContext = this,
                        focusable = focusable,
                        sceneMediator = mediator,
                        coroutineDispatcher = compositionContext.effectCoroutineContext
                    )
            }

            SingleLayerComposeScene(
                coroutineContext = coroutineDispatcher,
                density = mediator.densityProvider(),
                invalidate = mediator::onComposeSceneInvalidate,
                layoutDirection = layoutDirection,
                composeSceneContext = context,
            )
        }

    private fun createMultiLayerComposeSceneMediator(): ComposeSceneMediator =
        ComposeSceneMediator(
            viewControllerProvider = { containerViewController },
            configuration = configuration,
            focusStack = focusStack,
            windowInfo = windowInfo,
            transparency = false,
        ) { mediator ->
            MultiLayerComposeScene(
                coroutineContext = coroutineDispatcher,
                composeSceneContext = object : ComposeSceneContext {
                    override val platformContext: PlatformContext get() = mediator.platformContext
                },
                density = mediator.densityProvider(),
                invalidate = mediator::onComposeSceneInvalidate,
                layoutDirection = layoutDirection,
            )
        }

    fun updateLayout() {
        val scale = containerViewController.view.window?.screen?.scale ?: 1.0
        println("scale: $scale")//todo check
        val size = containerViewController.view.frame.useContents {
            IntSize(
                width = (size.width * scale).roundToInt(),
                height = (size.height * scale).roundToInt()
            )
        }
        windowInfo.containerSize = containerSize
        composeSceneMediators.fastForEach {
            it.updateLayout()
        }
    }

}
