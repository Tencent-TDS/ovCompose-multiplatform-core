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

package androidx.compose.ui.scene

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

@InternalComposeUiApi
interface ComposeSceneLayer {
    /**
     * Density of the content which will be used to convert [dp] units.
     */
    var density: Density

    /**
     * The layout direction of the content, provided to the composition via [LocalLayoutDirection].
     */
    var layoutDirection: LayoutDirection

    var bounds: IntRect

    var scrimColor: Color?

    var focusable: Boolean

    /**
     * Close all resources and subscriptions. Not calling this method when [ComposeScene] is no
     * longer needed will cause a memory leak.
     *
     * All effects launched via [LaunchedEffect] or [rememberCoroutineScope] will be cancelled
     * (but not immediately).
     *
     * After calling this method, you cannot call any other method of this [ComposeScene].
     */
    fun close()

    /**
     * Update the composition with the content described by the [content] composable. After this
     * has been called the changes to produce the initial composition has been calculated and
     * applied to the composition.
     *
     * Will throw an [IllegalStateException] if the composition has been disposed.
     *
     * @param content Content of the [ComposeScene]
     */
    fun setContent(content: @Composable () -> Unit)

    /**
     * Sets the key event listener.
     *
     * @param onPreviewKeyEvent This callback is invoked when the user interacts with the hardware
     * keyboard. It gives ancestors of a focused component the chance to intercept a [KeyEvent].
     * Return true to stop propagation of this event. If you return false, the key event will be sent
     * to this [onPreviewKeyEvent]'s child. If none of the children consume the event, it will be
     * sent back up to the root using the [onKeyEvent] callback.
     * @param onKeyEvent This callback is invoked when the user interacts with the hardware keyboard.
     * While implementing this callback, return true to stop propagation of this event.
     */
    fun setKeyEventListener(
        onPreviewKeyEvent: ((KeyEvent) -> Boolean)? = null,
        onKeyEvent: ((KeyEvent) -> Boolean)? = null,
    )

    fun setOutsidePointerEventListener(
        onOutsidePointerEvent: ((Boolean) -> Unit)? = null,
    )
}

@OptIn(InternalComposeUiApi::class)
@Composable
internal fun rememberComposeSceneLayer(
    focusable: Boolean = false
): ComposeSceneLayer {
    val scene = LocalComposeScene.requireCurrent()
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val parentComposition = rememberCompositionContext()
    val layer = remember {
        scene.createLayer(
            density = density,
            layoutDirection = layoutDirection,
            focusable = focusable,
            compositionContext = parentComposition,
        )
    }
    layer.focusable = focusable
    DisposableEffect(Unit) {
        onDispose {
            layer.close()
        }
    }
    SideEffect {
        layer.density = density
        layer.layoutDirection = layoutDirection
    }
    return layer
}
