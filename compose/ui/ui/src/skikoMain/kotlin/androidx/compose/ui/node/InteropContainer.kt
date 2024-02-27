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

package androidx.compose.ui.node

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.OverlayLayout
import androidx.compose.ui.node.TraversableNode.Companion.TraverseDescendantsAction.CancelTraversal
import androidx.compose.ui.node.TraversableNode.Companion.TraverseDescendantsAction.ContinueTraversal

internal interface InteropContainer<T> {
    var rootModifier: TrackInteropModifierNode<T>?

    fun addInteropView(nativeView: T)
    fun removeInteropView(nativeView: T)
}

internal fun <T> InteropContainer<T>.countInteropComponentsBefore(nativeView: T): Int {
    var componentsBefore = 0
    rootModifier?.traverseDescendants {
        if (it.nativeView != nativeView) {
            componentsBefore++
            ContinueTraversal
        } else {
            CancelTraversal
        }
    }
    return componentsBefore
}

@Composable
internal fun <T> InteropContainer<T>.TrackInteropContainer(container: T, content: @Composable () -> Unit) {
    OverlayLayout(
        modifier = TrackInteropModifierElement(
            nativeView = container
        ) { rootModifier = it },
        content = content
    )
}

internal data class TrackInteropModifierElement<T>(
    var nativeView: T,
    val block: ((TrackInteropModifierNode<T>) -> Unit)? = null
) : ModifierNodeElement<TrackInteropModifierNode<T>>() {
    override fun create() = TrackInteropModifierNode(
        nativeView = nativeView
    ).also {
        block?.invoke(it)
    }

    override fun update(node: TrackInteropModifierNode<T>) {
        node.nativeView = nativeView
    }
}

private const val TRAVERSAL_NODE_KEY =
    "androidx.compose.ui.node.TRACK_INTEROP_TRAVERSAL_NODE_KEY"

internal class TrackInteropModifierNode<T>(
    var nativeView: T
) : Modifier.Node(), TraversableNode {
    override val traverseKey = TRAVERSAL_NODE_KEY
}
