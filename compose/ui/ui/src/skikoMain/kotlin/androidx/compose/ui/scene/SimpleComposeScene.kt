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
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionContext
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.pointer.PointerInputEvent
import androidx.compose.ui.node.RootNodeOwner
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toIntRect
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers

@InternalComposeUiApi
fun ComposeScene(
    density: Density = Density(1f),
    layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    size: IntSize? = null,
    coroutineContext: CoroutineContext = Dispatchers.Unconfined,
    composeSceneContext: ComposeSceneContext = ComposeSceneContext.Empty,
    invalidate: () -> Unit = {},
): ComposeScene = SimpleComposeSceneImpl(
    density = density,
    layoutDirection = layoutDirection,
    size = size,
    coroutineContext = coroutineContext,
    composeSceneContext = composeSceneContext,
    invalidate = invalidate
)

@OptIn(InternalComposeUiApi::class)
private class SimpleComposeSceneImpl(
    density: Density,
    layoutDirection: LayoutDirection,
    size: IntSize?,
    coroutineContext: CoroutineContext,
    composeSceneContext: ComposeSceneContext,
    invalidate: () -> Unit = {},
) : BaseComposeScene(
    coroutineContext = coroutineContext,
    composeSceneContext = composeSceneContext,
    invalidate = invalidate
) {
    private val mainOwner by lazy {
        RootNodeOwner(
            density = density,
            layoutDirection = layoutDirection,
            coroutineContext = compositionContext.effectCoroutineContext,
            bounds = size?.toIntRect(),
            platformContext = composeSceneContext.platformContext,
            snapshotInvalidationTracker = snapshotInvalidationTracker,
            inputHandler = inputHandler,
        )
    }

    override var density: Density = density
        set(value) {
            check(!isClosed) { "ComposeScene is closed" }
            field = value
            mainOwner.density = value
        }

    override var layoutDirection: LayoutDirection = layoutDirection
        set(value) {
            check(!isClosed) { "ComposeScene is closed" }
            field = value
            mainOwner.layoutDirection = value
        }

    override var size: IntSize? = size
        set(value) {
            check(!isClosed) { "ComposeScene is closed" }
            field = value
            mainOwner.bounds = value?.toIntRect()
        }

    override val focusManager: ComposeSceneFocusManager =
        ComposeSceneFocusManagerImpl()

    init {
        mainOwner.focusOwner.takeFocus()
    }

    override fun close() {
        check(!isClosed) { "ComposeScene is already closed" }
        mainOwner.dispose()
        super.close()
    }

    override fun calculateContentSize(): IntSize {
        check(!isClosed) { "ComposeScene is closed" }
        return mainOwner.measureInConstraints(Constraints())
    }

    override fun createComposition(content: @Composable () -> Unit): Composition {
        return mainOwner.setContent(
            compositionContext,
            { compositionLocalContext },
            content = content
        )
    }

    @Deprecated("To be removed. Temporary hack for iOS interop")
    override fun hitTestInteropView(position: Offset): Boolean {
        return mainOwner.hitTestInteropView(position)
    }

    override fun processPointerInputEvent(event: PointerInputEvent) =
        mainOwner.onPointerInput(event)

    override fun processKeyEvent(keyEvent: KeyEvent): Boolean =
        mainOwner.onKeyEvent(keyEvent)

    override fun measureAndLayout() {
        mainOwner.measureAndLayout()
    }

    override fun draw(canvas: Canvas) {
        mainOwner.draw(canvas)
    }

    override fun createLayer(
        density: Density,
        layoutDirection: LayoutDirection,
        focusable: Boolean,
        compositionContext: CompositionContext,
    ): ComposeSceneLayer = composeSceneContext.createPlatformLayer(
        density = density,
        layoutDirection = layoutDirection,
        focusable = focusable,
        compositionContext = compositionContext
    )

    private inner class ComposeSceneFocusManagerImpl : ComposeSceneFocusManager {
        private val focusOwner get() = mainOwner.focusOwner
        override fun requestFocus() = focusOwner.takeFocus()
        override fun releaseFocus() = focusOwner.releaseFocus()
        override fun getFocusRect(): Rect? = focusOwner.getFocusRect()
        override fun clearFocus(force: Boolean) = focusOwner.clearFocus(force)
        override fun moveFocus(focusDirection: FocusDirection): Boolean =
            focusOwner.moveFocus(focusDirection)
    }
}