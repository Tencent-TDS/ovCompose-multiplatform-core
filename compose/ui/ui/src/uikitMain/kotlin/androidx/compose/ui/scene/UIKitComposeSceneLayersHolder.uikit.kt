/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.platform.PlatformWindowContext
import androidx.compose.ui.uikit.embedSubview
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.viewinterop.UIKitInteropTransaction
import androidx.compose.ui.window.ComposeView
import androidx.compose.ui.window.GestureEvent
import androidx.compose.ui.window.MetalView
import kotlin.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import org.jetbrains.skia.Canvas
import platform.UIKit.UIEvent
import platform.UIKit.UIWindow

/**
 * A class responsible for managing and rendering [UIKitComposeSceneLayer]s.
 */
internal class UIKitComposeSceneLayersHolder(
    private val windowContext: PlatformWindowContext,
    useSeparateRenderThreadWhenPossible: Boolean
) {
    val hasInvalidations: Boolean
        get() = this.layers.any { it.hasInvalidations }

    private val layers = mutableListOf<UIKitComposeSceneLayer>()

    private val layersCache = CopiedList {
        it.addAll(this.layers)
    }

    fun withLayers(block: (List<UIKitComposeSceneLayer>) -> Unit) = layersCache.withCopy(block)

    private var ongoingGesturesCount = 0

    /**
     * Transactions of the layers that were imperatively removed before their changes were applied.
     */
    private var removedLayersTransactions = mutableListOf<UIKitInteropTransaction>()

    val metalView: MetalView = MetalView(
        ::retrieveAndMergeInteropTransactions,
        useSeparateRenderThreadWhenPossible,
        ::render
    ).apply {
        canBeOpaque = false
    }

    private val view = ComposeView(
        onDidMoveToWindow = {},
        onLayoutSubviews = { windowContext.updateWindowContainerSize() },
        useOpaqueConfiguration = false,
        transparentForTouches = true,
        metalView = metalView
    )

    fun animateSizeTransition(scope: CoroutineScope, duration: Duration) {
        if (this.layers.isEmpty()) {
            return
        }
        val animations = listOf(
            windowContext.prepareAndGetSizeTransitionAnimation()
        ) + this.layers.map {
            it.prepareAndGetSizeTransitionAnimation()
        }

        view.animateSizeTransition(scope) {
            animations.map {
                scope.launch { it.invoke(duration) }
            }.joinAll()
        }
    }

    /**
     * When there is an ongoing gesture, we need notify redrawer about it. It should unconditionally
     * unpause CADisplayLink which affects frequency of polling UITouch events on high frequency
     * display and force it to match display refresh rate.
     *
     * Otherwise [UIEvent]s will be dispatched with the 60hz frequency.
     */
    fun onGestureEvent(event: GestureEvent) {
        val hadAnyOngoingGestures = ongoingGesturesCount > 0

        when (event) {
            GestureEvent.BEGAN -> {
                ongoingGesturesCount++

                if (!hadAnyOngoingGestures && ongoingGesturesCount > 0) {
                    metalView.needsProactiveDisplayLink = true
                }
            }
            GestureEvent.ENDED -> {
                ongoingGesturesCount--

                if (hadAnyOngoingGestures && ongoingGesturesCount == 0) {
                    metalView.needsProactiveDisplayLink = false
                }
            }
        }
    }

    fun dispose(hasViewAppeared: Boolean) {
        metalView.dispose()

        // `dispose` is called instead of `close`, because `close` is also used imperatively
        // to remove the layer from the array based on user interaction.
        while (this.layers.isNotEmpty()) {
            val layer = this.layers.removeLast()

            if (hasViewAppeared) {
                layer.sceneWillDisappear()
            }

            layer.dispose()
        }

        view.dispose()
        view.removeFromSuperview()
    }

    fun attach(window: UIWindow, layer: UIKitComposeSceneLayer, hasViewAppeared: Boolean) {
        val isFirstLayer = this.layers.isEmpty()

        this.layers.add(layer)

        view.embedSubview(layer.view)
        view.bringSubviewToFront(metalView)

        if (isFirstLayer) {
            // The content of previous layers drawn on the Metal view should be cleared and
            // redrawn synchronously after the new layer is attached to avoid flickering.

            metalView.setNeedsSynchronousDrawOnNextLayout()

            window.embedSubview(view)
            window.layoutIfNeeded()
        }

        if (hasViewAppeared) {
            layer.sceneDidAppear()
        }
    }

    fun detach(layer: UIKitComposeSceneLayer, hasViewAppeared: Boolean) {
        if (hasViewAppeared) {
            layer.sceneWillDisappear()
        }

        this.layers.remove(layer)

        // Intercept the actions UIKitInteropTransaction from the layer
        val transaction = layer.retrieveInteropTransaction()

        if (this.layers.isEmpty()) {
            // It was the last layer, remove the view and executed the actions immediately
            view.removeFromSuperview()

            transaction.actions.fastForEach { it.invoke() }
        } else {
            // It wasn't the last layer, pending transactions should be added to the list
            removedLayersTransactions.add(transaction)

            // Redraw content with layer removed
            metalView.redrawer.setNeedsRedraw()

        }
    }

    fun viewDidAppear() {
        this.layers.fastForEach {
            it.sceneDidAppear()
        }
    }

    fun viewWillDisappear() {
        this.layers.fastForEach {
            it.sceneWillDisappear()
        }
    }

    /**
     * Iterate through existing layers and merge their interop transactions to be consumed by the
     * [MetalView], also include transactions of the layers that were removed and are not
     * present in [layers] anymore.
     */
    private fun retrieveAndMergeInteropTransactions(): UIKitInteropTransaction {
        val removedLayersTransactionsCopy = removedLayersTransactions.toList()
        removedLayersTransactions.clear()

        val transactions = this.layers.map {
            it.retrieveInteropTransaction()
        } + removedLayersTransactionsCopy
        return UIKitInteropTransaction.merge(
            transactions = transactions
        )
    }

    private fun render(canvas: Canvas, nanoTime: Long) {
        val composeCanvas = canvas.asComposeCanvas()

        // Some layers may be removed during rendering, because recomposition will happen in the
        // process, so we need to make a temporary copy of the list
        layersCache.withCopy {
            it.fastForEach {
                it.render(composeCanvas, nanoTime)
            }
        }
    }
}