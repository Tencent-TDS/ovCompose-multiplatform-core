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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.scene.ComposeSceneContext
import androidx.compose.ui.scene.ComposeSceneLayer
import androidx.compose.ui.scene.SingleLayerComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.LayoutDirection
import kotlin.coroutines.CoroutineContext

internal interface ComposeSceneLayerBridge {
    val layer: ComposeSceneLayer
    fun display()
}

internal fun ComposeBridge.createComposeSceneLayerBridge(
    coroutineContext: CoroutineContext,
    composeSceneContext: ComposeSceneContext,
    focusable: Boolean,
    densityProvider: DensityProvider,
    needRedraw: () -> Unit
): ComposeSceneLayerBridge = object : ComposeSceneLayerBridge {

    /**
     * Internal Compose scene for rendering
     */
    private val composeSceneBridge: ComposeSceneBridge =
        createComposeSceneBridge(focusable = focusable, transparentBackground = true) {
            SingleLayerComposeScene(
                coroutineContext = coroutineContext,
                composeSceneContext = composeSceneContext,
                density = densityProvider(),
                invalidate = needRedraw,
                layoutDirection = layoutDirection,
            )
        }

    override fun display() {
        composeSceneBridge.display(focusable = focusable, onDisplayed = {})
    }

    override val layer = object : ComposeSceneLayer {
        override var density: Density = densityProvider()
        override var layoutDirection: LayoutDirection = this@createComposeSceneLayerBridge.layoutDirection
        override var bounds: IntRect
            get() = composeSceneBridge.getViewBounds()
            set(value) {
                composeSceneBridge.setLayout(
                    SceneLayout.Bounds(rect = value)
                )
            }
        override var scrimColor: Color? = null
        override var focusable: Boolean = focusable

        override fun close() {
            composeSceneBridge.dispose()
        }

        override fun setContent(content: @Composable () -> Unit) {
            composeSceneBridge.setContent(content)
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
