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
import androidx.compose.ui.scene.ComposeSceneLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.cinterop.CValue
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSize
import platform.UIKit.NSLayoutConstraint
import platform.UIKit.UIView
import platform.UIKit.UIViewController

internal interface LayerState<V> {
    val layer: ComposeSceneLayer
    val sceneState: SceneState<V>
}

internal fun RootViewControllerState<UIViewController, UIView>.createLayerState(
    parentSceneState: SceneState<UIView>,
    density: Density,
    layoutDirection: LayoutDirection,
    focusable: Boolean,
    compositionContext: CompositionContext
): LayerState<UIView> = object : LayerState<UIView> {

    override val sceneState: SceneState<UIView> = createSingleLayerSceneUIViewState(focusable = focusable)

    override val layer = object : ComposeSceneLayer {
        override var density: Density = density
        override var layoutDirection: LayoutDirection = layoutDirection
        override var bounds: IntRect
            get() = IntRect(
                offset = IntOffset(
                    x = sceneState.sceneView.bounds.useContents { origin.x.toInt() },
                    y = sceneState.sceneView.bounds.useContents { origin.y.toInt() },
                ),
                size = IntSize(
                    width = sceneState.sceneView.bounds.useContents { size.width.toInt() },
                    height = sceneState.sceneView.bounds.useContents { size.height.toInt() },
                )
            )
            set(value) {
                println("ComposeSceneLayer, set bounds $value")
                sceneState.sceneView.setBounds(
                    CGRectMake(
                        value.left.toDouble(),
                        value.top.toDouble(),
                        value.width.toDouble(),
                        value.height.toDouble()
                    )
                )
            }
        override var scrimColor: Color? = null
        override var focusable: Boolean = focusable

        override fun close() {
            println("ComposeSceneContext close")
            sceneState.dispose()
            sceneState.sceneView.removeFromSuperview()
        }

        override fun setContent(content: @Composable () -> Unit) {
            //todo New Compose Scene  Размер сцены scene.size - полный экран
            //  Сделать translate Canvas по размеру -bounds.position, размер канвы bounds.size
            //  translate делать при каждой отрисовке
            //  canvas.translate(x, y)
            //  drawContainedDrawModifiers(canvas)
            //  canvas.translate(-x, -y)
            //  А размер канвы задавать в bounds set(value) {...
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
