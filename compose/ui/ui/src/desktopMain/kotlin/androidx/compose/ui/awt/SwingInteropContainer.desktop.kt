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

package androidx.compose.ui.awt

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.OverlayLayout
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.TraversableNode.Companion.TraverseDescendantsAction.CancelTraversal
import androidx.compose.ui.node.TraversableNode.Companion.TraverseDescendantsAction.ContinueTraversal
import androidx.compose.ui.node.traverseDescendants
import java.awt.Component
import javax.swing.JLayeredPane

internal val LocalSwingInteropContainer = staticCompositionLocalOf<SwingInteropContainer> {
    error("LocalSwingInteropContainer not provided")
}

// Key used to match my custom Modifier.Node that implements TraversableNode.
private const val TRAVERSAL_NODE_KEY =
    "androidx.compose.ui.awt.SWING_INTEROP_TRAVERSAL_NODE_KEY"

internal class SwingInteropContainer(
    val container: JLayeredPane,
    private val useInteropBlending: Boolean,
    private val useLayers: Boolean
) {
    private var rootModifier: SwingInteropModifierNode? = null
    private var interopComponentsCount = 0

    private val contentLayer: Int = 10
    private val interopLayer: Int
        get() = if (useInteropBlending) 0 else 20

    private fun JLayeredPane.addToLayer(component: Component, layer: Int, index: Int = -1) {
        if (!useLayers) {
            add(component, index)
        } else {
            setLayer(component, layer)
            add(component, null, index)
        }
    }

    fun addContentComponent(component: Component) {
        container.addToLayer(component, contentLayer)
    }

    fun addInteropComponent(component: Component): Component {
        interopComponentsCount++
        val index = interopComponentsCount - countInteropComponentsBefore(component)
        container.addToLayer(component, interopLayer, index)
        return component
    }

    fun removeInteropComponent(component: Component) {
        interopComponentsCount--
        container.remove(component)
    }

    private fun countInteropComponentsBefore(component: Component): Int {
        var componentsBefore = 0
        rootModifier?.traverseDescendants {
            if (it.component != component) {
                componentsBefore++
                ContinueTraversal
            } else {
                CancelTraversal
            }
        }
        return componentsBefore
    }

    @Composable
    operator fun invoke(content: @Composable () -> Unit) {
        CompositionLocalProvider(
            LocalSwingInteropContainer provides this,
        ) {
            OverlayLayout(
                modifier = Modifier then SwingInteropModifierElement(
                    component = container
                ) { rootModifier = it },
                content = content
            )
        }
    }
}

internal fun Modifier.swingInterop(
    component: Component
): Modifier = this then SwingInteropModifierElement(
    component = component
)

private data class SwingInteropModifierElement(
    var component: Component,
    val block: ((SwingInteropModifierNode) -> Unit)? = null
) : ModifierNodeElement<SwingInteropModifierNode>() {
    override fun create() = SwingInteropModifierNode(
        component = component
    ).also {
        block?.invoke(it)
    }

    override fun update(node: SwingInteropModifierNode) {
        node.component = component
    }
}


private class SwingInteropModifierNode(
    var component: Component
) : Modifier.Node(), TraversableNode {
    override val traverseKey = TRAVERSAL_NODE_KEY
}
