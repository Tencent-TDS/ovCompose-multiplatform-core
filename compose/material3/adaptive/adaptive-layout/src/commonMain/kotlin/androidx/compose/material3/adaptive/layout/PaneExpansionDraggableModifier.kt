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

package androidx.compose.material3.adaptive.layout

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ParentDataModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp

/**
 * This function sets up the default semantics of pane expansion drag handles with the given
 * [PaneExpansionState]. It will provide suitable [contentDescription] as well as [onClick] function
 * to move the pane expansion among anchors that can be operated via accessibility services.
 *
 * It's supposed to be used with a [PaneScaffoldScope.paneExpansionDraggable] modifier, or a plain
 * [semantics] modifier associated with a drag handle composable.
 */
@ExperimentalMaterial3AdaptiveApi
@Composable
fun PaneExpansionState.defaultDragHandleSemantics(): SemanticsPropertyReceiver.() -> Unit {
    val description = getString(Strings.defaultPaneExpansionDragHandleContentDescription)
    val nextAnchor = nextAnchor
    val actionLabel =
        if (nextAnchor != null) {
            getString(
                Strings.defaultPaneExpansionDragHandleActionDescription,
                nextAnchor.description
            )
        } else {
            null
        }
    return semantics@{
        contentDescription = description
        if (nextAnchor == null) {
            // TODO(conrachen): handle this case
            return@semantics
        }
        onClick(label = actionLabel) {
            snapToAnchor(nextAnchor)
            return@onClick true
        }
    }
}

internal expect fun Modifier.systemGestureExclusion(): Modifier

internal data class MinTouchTargetSizeElement(val size: Dp) :
    ModifierNodeElement<MinTouchTargetSizeNode>() {
    private val inspectorInfo = debugInspectorInfo {
        name = "minTouchTargetSize"
        properties["size"] = size
    }

    override fun create(): MinTouchTargetSizeNode {
        return MinTouchTargetSizeNode(size)
    }

    override fun update(node: MinTouchTargetSizeNode) {
        node.size = size
    }

    override fun InspectorInfo.inspectableProperties() {
        inspectorInfo()
    }
}

internal class MinTouchTargetSizeNode(var size: Dp) : ParentDataModifierNode, Modifier.Node() {
    override fun Density.modifyParentData(parentData: Any?) =
        ((parentData as? PaneScaffoldParentDataImpl) ?: PaneScaffoldParentDataImpl()).also {
            it.minTouchTargetSize = size
        }
}
