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
import androidx.compose.runtime.CompositionLocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.scene.ComposeSceneContext
import androidx.compose.ui.scene.ComposeSceneLayer
import androidx.compose.ui.scene.SingleLayerComposeScene
import androidx.compose.ui.uikit.ComposeUIViewControllerConfiguration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.LayoutDirection
import kotlin.coroutines.coroutineContext
import kotlinx.cinterop.CValue
import platform.CoreGraphics.CGSize
import platform.UIKit.UIView
import platform.UIKit.UIViewControllerTransitionCoordinatorProtocol

internal class UIViewComposeSceneLayer(
    private val composeContainer: ComposeContainer,
    density: Density,
    layoutDirection: LayoutDirection,
    configuration: ComposeUIViewControllerConfiguration,
    focusStack: FocusStack<UIView>?,
    windowInfo: WindowInfo,
    compositionContext: CompositionContext,
    compositionLocalContext: CompositionLocalContext?,
) : ComposeSceneLayer {

    private val mediator = ComposeSceneMediator(
        viewController = composeContainer,
        configuration = configuration,
        focusStack = focusStack,
        windowInfo = windowInfo,
        coroutineContext = compositionContext.effectCoroutineContext,
        skikoUIViewFactory = ::createSkikoUIView,
        composeSceneFactory = ::createComposeScene,
    )

    init {
        mediator.compositionLocalContext = compositionLocalContext
        composeContainer.attachLayer(this)
    }

    private fun createSkikoUIView(mediator: ComposeSceneMediator):SkikoUIView = SkikoUIView(
        renderDelegate = mediator.renderDelegate,
        transparency = true,
    )

    private fun createComposeScene(mediator: ComposeSceneMediator): ComposeScene =
        SingleLayerComposeScene(
            coroutineContext = mediator.coroutineContext,
            composeSceneContext = composeContainer.createComposeSceneContext(mediator.platformContext),
            density = density,
            invalidate = mediator::onComposeSceneInvalidate,
            layoutDirection = layoutDirection,
        )

    override var density: Density = density
        set(value) {
            //todo set to scene
        }
    override var layoutDirection: LayoutDirection = layoutDirection
        set(value) {
            //todo set to scene
        }
    override var bounds: IntRect
        get() = mediator.getPixelBounds()
        set(value) {
            mediator.setLayout(
                SceneLayout.Bounds(rect = value)
            )
        }
    override var scrimColor: Color? = null
    override var focusable: Boolean = focusStack != null

    override fun close() {
        mediator.dispose()
        composeContainer.detachLayer(this)
    }

    override fun setContent(content: @Composable () -> Unit) {
        mediator.setContent {
            ProvideContainerCompositionLocals(composeContainer) {
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

    fun viewDidAppear(animated: Boolean) {
        mediator.viewDidAppear(animated)
    }

    fun viewWillDisappear(animated: Boolean) {
        mediator.viewWillDisappear(animated)
    }

    fun viewSafeAreaInsetsDidChange() {
        mediator.viewSafeAreaInsetsDidChange()
    }

    fun viewWillLayoutSubviews() {
        mediator.viewWillLayoutSubviews()
    }

    fun viewWillTransitionToSize(
        targetSize: CValue<CGSize>,
        coordinator: UIViewControllerTransitionCoordinatorProtocol
    ) {
        mediator.viewWillTransitionToSize(targetSize, coordinator)
    }

}