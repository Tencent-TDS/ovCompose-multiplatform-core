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
import androidx.compose.ui.scene.ComposeSceneContext
import androidx.compose.ui.scene.ComposeSceneLayer
import androidx.compose.ui.scene.SingleLayerComposeScene
import androidx.compose.ui.uikit.ComposeUIViewControllerConfiguration
import androidx.compose.ui.uikit.InterfaceOrientation
import androidx.compose.ui.uikit.LocalInterfaceOrientation
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastForEach
import kotlin.coroutines.CoroutineContext
import platform.Foundation.NSNotification
import platform.UIKit.UIApplication
import platform.UIKit.UIUserInterfaceLayoutDirection
import platform.UIKit.UIView
import platform.UIKit.UIViewController

@OptIn(InternalComposeApi::class)
internal class ComposeContainer(
    val configuration: ComposeUIViewControllerConfiguration,
    val containerViewController: UIViewController,
) {
    val composeSceneMediators: MutableList<ComposeSceneMediator> = mutableListOf()
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

    @Composable
    fun ProvideRootCompositionLocals(content: @Composable () -> Unit) =
        CompositionLocalProvider(
            LocalUIViewController provides containerViewController,
            LocalLayerContainer provides containerViewController.view,
            LocalInterfaceOrientation provides interfaceOrientationState.value,
            LocalSystemTheme provides systemThemeState.value,
            content = content
        )

    @OptIn(ExperimentalComposeApi::class)
    fun createRootComposeSceneMediator(): ComposeSceneMediator =
        if (configuration.platformLayers) {
            createSingleLayerComposeSceneMediator()
        } else {
            createMultiLayerComposeSceneMediator()
        }

    val keyboardVisibilityListener = object : KeyboardVisibilityListener {
        override fun keyboardWillShow(arg: NSNotification) = composeSceneMediators.fastForEach {
            it.keyboardVisibilityListener.keyboardWillShow(arg)
        }

        override fun keyboardWillHide(arg: NSNotification) = composeSceneMediators.fastForEach {
            it.keyboardVisibilityListener.keyboardWillHide(arg)
        }
    }

    fun createLayer(
        currentComposeSceneContext: ComposeSceneContext,
        focusable: Boolean,
        sceneMediator: ComposeSceneMediator,
        coroutineDispatcher: CoroutineContext,
    ): ComposeSceneLayer {
        val mediator = ComposeSceneMediator(
            container = this,
            focusable = focusable,
            transparentBackground = true
        ) {
            SingleLayerComposeScene(
                coroutineContext = coroutineDispatcher,
                composeSceneContext = currentComposeSceneContext,
                density = sceneMediator.densityProvider(),
                invalidate = sceneMediator::needRedraw,
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
            }

            override fun setContent(content: @Composable () -> Unit) {
                mediator.setContent(content)
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

}

private fun getLayoutDirection() =
    when (UIApplication.sharedApplication().userInterfaceLayoutDirection) {
        UIUserInterfaceLayoutDirection.UIUserInterfaceLayoutDirectionRightToLeft -> LayoutDirection.Rtl
        else -> LayoutDirection.Ltr
    }
