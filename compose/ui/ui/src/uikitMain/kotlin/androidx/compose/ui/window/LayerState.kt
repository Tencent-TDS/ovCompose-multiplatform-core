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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.scene.ComposeSceneContext
import androidx.compose.ui.scene.ComposeSceneLayer
import androidx.compose.ui.scene.SingleLayerComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.coroutines.CoroutineContext
import kotlinx.cinterop.CValue
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSize
import platform.UIKit.NSLayoutConstraint
import platform.UIKit.UIView
import platform.UIKit.UIViewController

internal interface LayerState<V> {
    val layer: ComposeSceneLayer
    fun display()
}

internal fun RootViewControllerState<UIViewController, UIView>.createLayerState(
    parentSceneState: SceneState<UIView>,
    coroutineContext: CoroutineContext,
    composeSceneContext: ComposeSceneContext,
    focusable: Boolean,
): LayerState<UIView> = object : LayerState<UIView> {

    /**
     * Internal Compose scene for rendering
     */
    val sceneState: SceneState<UIView> =
        createSceneState(focusable = focusable, transparentBackground = true) {
            SingleLayerComposeScene(
                coroutineContext = coroutineContext,
                composeSceneContext = composeSceneContext,
                density = parentSceneState.densityProvider(),
                invalidate = parentSceneState::needRedraw,
                layoutDirection = layoutDirection,
            )
        }.also {
            //it.sceneView.alpha = 0.5 //todo for debugging only
        }

    override fun display() {
        sceneState.display(focusable = focusable, onDisplayed = {})
    }

    override val layer = object : ComposeSceneLayer {
        override var density: Density = parentSceneState.densityProvider()
        override var layoutDirection: LayoutDirection = this@createLayerState.layoutDirection
        override var bounds: IntRect
            get() = sceneState.bounds
            set(value) {
                with(bounds) {
                    println("ComposeSceneLayer, set bounds (x:$left, y:$top, w:$width, h:$height)")
                }
                sceneState.bounds = value
            }
        override var scrimColor: Color? = null
        override var focusable: Boolean = focusable

        override fun close() {
            sceneState.dispose()
            sceneState.sceneView.removeFromSuperview()
        }

        override fun setContent(content: @Composable () -> Unit) {
            sceneState.setContentWithCompositionLocals(content)
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
