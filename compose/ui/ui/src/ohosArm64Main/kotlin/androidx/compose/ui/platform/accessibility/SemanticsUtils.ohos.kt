/*
 * Tencent is pleased to support the open source community by making ovCompose available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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

package androidx.compose.ui.platform.accessibility

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsOwner

internal class SemanticsNodeWithAdjustedBounds(
    val semanticsNode: SemanticsNode,
    val adjustedBounds: Rect
)

internal val DefaultFakeNodeBounds = Rect(0f, 0f, 10f, 10f)

internal fun SemanticsOwner.getAllHierarchicalTraversalSemanticsNodesToMap(): Map<Int, SemanticsNodeWithAdjustedBounds> =
    unmergedRootSemanticsNode
        .hierarchicalTraversal {
            (it.layoutNode.isPlaced && it.layoutNode.isAttached) || it.isFake
        }
        .associateBy(
            keySelector = { it.id },
            valueTransform = { node ->
                val bounds = if (node.isFake) {
                    val parentNode = node.parent
                    // use parent bounds for fake node
                    if (parentNode?.layoutInfo?.isPlaced == true) {
                        parentNode.boundsInRoot
                    } else {
                        DefaultFakeNodeBounds
                    }
                } else {
                    node.touchBoundsInRoot
                }
                SemanticsNodeWithAdjustedBounds(node, bounds)
            }
        )

private fun SemanticsNode.hierarchicalTraversal(predicate: (SemanticsNode) -> Boolean) =
    hierarchicalTraversal(predicate) { replacedChildren }

// hierarchical traversal.
private fun <T> T.hierarchicalTraversal(
    predicate: (T) -> Boolean,
    children: T.() -> Collection<T>
) = sequence {
    val queue = mutableListOf(this@hierarchicalTraversal)
    while (queue.isNotEmpty()) {
        val node = queue.removeFirst()
        if (predicate(node)) {
            yield(node)
            queue.addAll(node.children())
        }
    }
}