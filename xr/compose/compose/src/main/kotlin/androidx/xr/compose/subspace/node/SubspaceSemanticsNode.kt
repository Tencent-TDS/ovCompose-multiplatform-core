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

package androidx.xr.compose.subspace.node

import androidx.annotation.RestrictTo
import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.xr.compose.subspace.layout.CoreEntity
import androidx.xr.compose.unit.IntVolumeSize
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.Component

/**
 * A list of key/value pairs associated with a layout node or its subtree.
 *
 * Each SubspaceSemanticsNode takes its id and initial key/value list from the outermost modifier on
 * one layout node. It also contains the "collapsed" configuration of any other semantics modifiers
 * on the same layout node.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class SubspaceSemanticsNode
internal constructor(private val layoutNode: SubspaceLayoutNode) {
    /** The unique ID of this semantics node. */
    public val id: Int = layoutNode.semanticsId

    /** The size of the bounding box for this node. */
    public val size: IntVolumeSize
        get() = layoutNode.measurableLayout.size

    /**
     * The position of this node relative to its parent layout node in the Compose hierarchy, in
     * pixels.
     */
    public val position: Vector3
        get() = layoutNode.measurableLayout.pose.translation

    /** The position of this node relative to the root of this Compose hierarchy, in pixels. */
    public val positionInRoot: Vector3
        get() = layoutNode.measurableLayout.poseInRoot.translation

    /** The rotation of this node relative to its parent layout node in the Compose hierarchy. */
    public val rotation: Quaternion
        get() = layoutNode.measurableLayout.pose.rotation

    /** The rotation of this node relative to the root of this Compose hierarchy. */
    public val rotationInRoot: Quaternion
        get() = layoutNode.measurableLayout.poseInRoot.rotation

    /** The components attached to this node by SubspaceLayoutNode update. */
    @get:Suppress("NullableCollection")
    public val components: List<Component>?
        get() = layoutNode.coreEntity?.entity?.getComponents()

    /** The scale factor of this node relative to its parent. */
    public val scale: Float
        get() = layoutNode.coreEntity?.entity?.getScale() ?: 1.0f

    /** The CoreEntity attached to this node. */
    internal val coreEntity: CoreEntity?
        get() = layoutNode.coreEntity

    /**
     * The semantics configuration of this node.
     *
     * This includes all properties attached as modifiers to the current layout node.
     */
    public val config: SemanticsConfiguration
        get() {
            val config = SemanticsConfiguration()
            layoutNode.nodes.getAll<SubspaceSemanticsModifierNode>().forEach {
                with(config) { with(it) { applySemantics() } }
            }
            return config
        }

    /**
     * The children of this node in the semantics tree.
     *
     * The children are ordered in inverse hit test order (i.e., paint order).
     */
    public val children: List<SubspaceSemanticsNode>
        get() {
            val list: MutableList<SubspaceSemanticsNode> = mutableListOf()
            layoutNode.fillOneLayerOfSemanticsWrappers(list)
            return list
        }

    /** Whether this node is the root of a semantics tree. */
    public val isRoot: Boolean
        get() = parent == null

    /** The parent of this node in the semantics tree. */
    public val parent: SubspaceSemanticsNode?
        get() {
            var node: SubspaceLayoutNode? = layoutNode.parent
            while (node != null) {
                if (node.hasSemantics) return SubspaceSemanticsNode(node)
                node = node.parent
            }
            return null
        }

    private fun SubspaceLayoutNode.fillOneLayerOfSemanticsWrappers(
        list: MutableList<SubspaceSemanticsNode>
    ) {
        children.forEach { child ->
            if (child.hasSemantics) {
                list.add(SubspaceSemanticsNode(child))
            } else {
                child.fillOneLayerOfSemanticsWrappers(list)
            }
        }
    }

    private val SubspaceLayoutNode.hasSemantics: Boolean
        get() = nodes.getLast<SubspaceSemanticsModifierNode>() != null
}
