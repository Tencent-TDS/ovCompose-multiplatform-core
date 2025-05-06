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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.pointer.PointerInputEvent
import androidx.compose.ui.node.OwnedLayerFactory
import androidx.compose.ui.node.RootNodeOwner
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

/**
 * Constructs a single-layer [ComposeScene] using the specified parameters.
 *
 * After [ComposeScene] will no longer needed, you should call [ComposeScene.close] method, so
 * all resources and subscriptions will be properly closed. Otherwise, there can be a memory leak.
 *
 * @param density Initial density of the content which will be used to convert [Dp] units.
 * @param layoutDirection Initial layout direction of the content.
 * @param boundsInWindow The bounds of the [ComposeScene]. Default value is `null`, which means
 * the size will be determined by the content.
 * @param coroutineContext Context which will be used to launch effects ([LaunchedEffect],
 * [rememberCoroutineScope]) and run recompositions.
 * @param composeSceneContext The context to share resources between multiple scenes and provide
 * a way for platform interaction.
 * @param invalidate The function to be called when the content need to be recomposed or
 * re-rendered. If you draw your content using [ComposeScene.render] method, in this callback you
 * should schedule the next [ComposeScene.render] in your rendering loop.
 * @return The created [ComposeScene].
 *
 * @see ComposeScene
 */
@InternalComposeUiApi
fun SingleLayerComposeScene(
    density: Density = Density(1f),
    layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    boundsInWindow: IntRect? = null,
    coroutineContext: CoroutineContext = Dispatchers.Unconfined,
    composeSceneContext: ComposeSceneContext = ComposeSceneContext.Empty,
    invalidate: () -> Unit = {},
): ComposeScene = SingleLayerComposeSceneImpl(
    density = density,
    layoutDirection = layoutDirection,
    boundsInWindow = boundsInWindow,
    coroutineContext = coroutineContext,
    composeSceneContext = composeSceneContext,
    invalidate = invalidate
)

private class SingleLayerComposeSceneImpl(
    density: Density,
    layoutDirection: LayoutDirection,
    boundsInWindow: IntRect?,
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
            bounds = boundsInWindow,
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

    override var boundsInWindow: IntRect? = boundsInWindow
        set(value) {
            check(!isClosed) { "ComposeScene is closed" }
            check(value == null || (value.size.width >= 0 && value.size.height >= 0)) {
                "Size of ComposeScene cannot be negative"
            }
            field = value
            mainOwner.bounds = value
        }

    override val focusManager: ComposeSceneFocusManager =
        ComposeSceneFocusManagerImpl()

    init {
        onOwnerAppended(mainOwner)
    }

    override fun close() {
        check(!isClosed) { "ComposeScene is already closed" }
        onOwnerRemoved(mainOwner)
        mainOwner.dispose()
        super.close()
    }

    override fun calculateContentSize(): IntSize {
        check(!isClosed) { "ComposeScene is closed" }
        return mainOwner.measureInConstraints(Constraints())
    }


    // region Tencent Code
    override fun setLayerFactory(factory: OwnedLayerFactory) {
        mainOwner.layerFactory = factory
    }

    override fun outerContainerOffsetChange(x: Float, y: Float) {
        mainOwner.outerContainerOffsetChange(x, y)
    }

    override fun getMeasuredContentSize(): IntSize {
        check(!isClosed) { "ComposeScene is closed" }
        // Don't use mainOwner.root.width here, as it strictly coerced by [constraints]
        val children = mainOwner.owner.root.children
        return IntSize(
            width = children.maxOfOrNull { it.outerCoordinator.measuredWidth } ?: 0,
            height = children.maxOfOrNull { it.outerCoordinator.measuredHeight } ?: 0,
        )
    }
    // endregion

    override fun createComposition(content: @Composable () -> Unit): Composition {
        return mainOwner.setContent(
            compositionContext,
            { compositionLocalContext },
            content = content
        )
    }

    // region Tencent Code
    @Deprecated("To be removed. Temporary hack for iOS interop")
    override fun hitTestInteropView(position: Offset): Boolean =
        mainOwner.hitTestInteropView(position, null)

    @Deprecated("To be removed. Temporary hack for iOS interop")
    override fun hitTestInteropView(position: Offset, event: Any?): Boolean {
        return mainOwner.hitTestInteropView(position, event)
    }

    override fun hitTestComposeView(position: Offset, event: Any?): Boolean {
        return mainOwner.hitTestComposeView(position, event)
    }
    // endregion

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

    private fun onOwnerAppended(owner: RootNodeOwner) {
        owner.focusOwner.takeFocus()
        semanticsOwnerListener?.onSemanticsOwnerAppended(owner.semanticsOwner)
    }

    private fun onOwnerRemoved(owner: RootNodeOwner) {
        semanticsOwnerListener?.onSemanticsOwnerRemoved(owner.semanticsOwner)
    }

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
