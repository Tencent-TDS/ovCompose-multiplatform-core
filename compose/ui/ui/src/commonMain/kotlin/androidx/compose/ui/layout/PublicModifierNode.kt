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

package androidx.compose.ui.layout

import androidx.compose.runtime.collection.MutableVector
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusEventModifierNode
import androidx.compose.ui.focus.FocusPropertiesModifierNode
import androidx.compose.ui.input.key.KeyInputModifierNode
import androidx.compose.ui.input.key.SoftKeyboardInterceptionModifierNode
import androidx.compose.ui.input.rotary.RotaryInputModifierNode
import androidx.compose.ui.modifier.ModifierLocalModifierNode
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ParentDataModifierNode
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.visitAncestors
import androidx.compose.ui.node.visitChildren
import androidx.compose.ui.node.visitLocalAncestors
import androidx.compose.ui.node.visitLocalDescendants
import androidx.compose.ui.node.visitSubtree
import kotlin.jvm.JvmInline
import kotlin.jvm.JvmStatic
import androidx.compose.ui.layout.visitAncestors as publicVisitAncestors
import androidx.compose.ui.layout.visitChildren as publicVisitChildren
import androidx.compose.ui.layout.visitLocalAncestors as publicVisitLocalAncestors
import androidx.compose.ui.layout.visitLocalDescendants as publicVisitLocalDescendants
import androidx.compose.ui.layout.visitSubtree as publicVisitSubtree

/**
 * Visit ancestors of [node][this] with a given [mask] in the global tree.
 * @param mask Specify the kind of traversal node.
 * @param includeSelf Whether to include self[this] in the traversal.
 * @param block Return boolean value will determine if the traversal will continue (true = continue, false = cancel).
 */
fun DelegatableNode.visitAncestors(
    mask: Int,
    includeSelf: Boolean = false,
    block: (Modifier.Node) -> Boolean,
) {
    visitAncestors(mask, includeSelf) {
        if (!block(it)) return
    }
}

/**
 * Visit subtree of [node][this] with a given [mask] in the global tree.
 * @param mask Specify the kind of traversal node.
 * @param includeSelf Whether to include self[this] in the traversal.
 * @param block Return boolean value will determine if the traversal will continue (true = continue, false = cancel).
 */
fun DelegatableNode.visitSubtree(
    mask: Int,
    includeSelf: Boolean = false,
    block: (Modifier.Node) -> Boolean,
) {
    checkIncludeSelf(includeSelf, block) {
        visitSubtree(mask) {
            if (!block(it)) return
        }
    }
}

/**
 * Visit children of [node][this] with a given [mask] in the global tree.
 * @param mask Specify the kind of traversal node.
 * @param includeSelf Whether to include self[this] in the traversal.
 * @param block Return boolean value will determine if the traversal will continue (true = continue, false = cancel).
 */
fun DelegatableNode.visitChildren(
    mask: Int,
    includeSelf: Boolean = false,
    block: (Modifier.Node) -> Boolean,
) {
    checkIncludeSelf(includeSelf, block) {
        visitChildren(mask) {
            if (!block(it)) return
        }
    }
}

/**
 * Visit ancestors of [node][this] with a given [mask] in the current node.
 * @param mask Specify the kind of traversal node.
 * @param includeSelf Whether to include self[this] in the traversal.
 * @param block Return boolean value will determine if the traversal will continue (true = continue, false = cancel).
 */
fun DelegatableNode.visitLocalAncestors(
    mask: Int,
    includeSelf: Boolean = false,
    block: (Modifier.Node) -> Boolean,
) {
    checkIncludeSelf(includeSelf, block) {
        visitLocalAncestors(mask) {
            if (!block(it)) return
        }
    }
}

/**
 * Visit descendants of [node][this] with a given [mask] in the current node.
 * @param mask Specify the kind of traversal node.
 * @param includeSelf Whether to include self[this] in the traversal.
 * @param block Return boolean value will determine if the traversal will continue (true = continue, false = cancel).
 */
fun DelegatableNode.visitLocalDescendants(
    mask: Int,
    includeSelf: Boolean = false,
    block: (Modifier.Node) -> Boolean,
) {
    checkIncludeSelf(includeSelf, block) {
        visitLocalDescendants(mask) {
            if (!block(it)) return
        }
    }
}

private inline fun DelegatableNode.checkIncludeSelf(
    includeSelf: Boolean = false,
    check: (Modifier.Node) -> Boolean,
    block: DelegatableNode.() -> Unit,
) {
    val self = node
    check(self.isAttached) { "visit node called on an unattached node" }
    if (!includeSelf || check(self)) {
        self.block()
    }
}

/**
 * Visit descendants of [node][this] with a given [type] in the current node.
 * @param type Specify the kind of traversal node.
 * @param includeSelf Whether to include self[this] in the traversal.
 * @param block Return boolean value will determine if the traversal will continue (true = continue, false = cancel).
 */
inline fun <reified T> DelegatableNode.visitLocalDescendants(
    type: NodeKind<T>,
    includeSelf: Boolean = false,
    crossinline block: (T) -> Boolean,
) = publicVisitLocalDescendants(type.mask, includeSelf) {
    it.dispatchForKind(type, block)
}

/**
 * Visit ancestors of [node][this] with a given [type] in the current node.
 * @param type Specify the kind of traversal node.
 * @param includeSelf Whether to include self[this] in the traversal.
 * @param block Return boolean value will determine if the traversal will continue (true = continue, false = cancel).
 */
inline fun <reified T> DelegatableNode.visitLocalAncestors(
    type: NodeKind<T>,
    includeSelf: Boolean = false,
    crossinline block: (T) -> Boolean,
) = publicVisitLocalAncestors(type.mask, includeSelf) {
    it.dispatchForKind(type, block)
}

/**
 * Visit ancestors of [node][this] with a given [type] in the global tree.
 * @param type Specify the kind of traversal node.
 * @param includeSelf Whether to include self[this] in the traversal.
 * @param block Return boolean value will determine if the traversal will continue (true = continue, false = cancel).
 */
inline fun <reified T> DelegatableNode.visitAncestors(
    type: NodeKind<T>,
    includeSelf: Boolean = false,
    crossinline block: (T) -> Boolean,
) = publicVisitAncestors(type.mask, includeSelf) { it.dispatchForKind(type, block) }

/**
 * Visit subtree of [node][this] with a given [type] in the global tree.
 * @param type Specify the kind of traversal node.
 * @param includeSelf Whether to include self[this] in the traversal.
 * @param block Return boolean value will determine if the traversal will continue (true = continue, false = cancel).
 */
inline fun <reified T> DelegatableNode.visitSubtree(
    type: NodeKind<T>,
    includeSelf: Boolean = false,
    crossinline block: (T) -> Boolean,
) = publicVisitSubtree(type.mask, includeSelf) { it.dispatchForKind(type, block) }

/**
 * Visit children of [node][this] with a given [type] in the global tree.
 * @param type Specify the kind of traversal node.
 * @param includeSelf Whether to include self[this] in the traversal.
 * @param block Return boolean value will determine if the traversal will continue (true = continue, false = cancel).
 */
inline fun <reified T> DelegatableNode.visitChildren(
    type: NodeKind<T>,
    includeSelf: Boolean = false,
    crossinline block: (T) -> Boolean,
) = publicVisitChildren(type.mask, includeSelf) { it.dispatchForKind(type, block) }

fun DelegatableNode.has(type: NodeKind<*>): Boolean =
    node.aggregateChildKindSet and type.mask != 0

@JvmInline
value class NodeKind<T>(val mask: Int) {
    inline infix fun or(other: NodeKind<*>): Int = mask or other.mask
    inline infix fun or(other: Int): Int = mask or other
}

@OptIn(ExperimentalComposeUiApi::class)
object Nodes {
    @JvmStatic
    inline val Any get() = NodeKind<Modifier.Node>(0b1 shl 0)

    @JvmStatic
    inline val Layout get() = NodeKind<LayoutModifierNode>(0b1 shl 1)

    @JvmStatic
    inline val Draw get() = NodeKind<DrawModifierNode>(0b1 shl 2)

    @JvmStatic
    inline val Semantics get() = NodeKind<SemanticsModifierNode>(0b1 shl 3)

    @JvmStatic
    inline val PointerInput get() = NodeKind<PointerInputModifierNode>(0b1 shl 4)

    @JvmStatic
    inline val Locals get() = NodeKind<ModifierLocalModifierNode>(0b1 shl 5)

    @JvmStatic
    inline val ParentData get() = NodeKind<ParentDataModifierNode>(0b1 shl 6)

    @JvmStatic
    inline val LayoutAware get() = NodeKind<LayoutAwareModifierNode>(0b1 shl 7)

    @JvmStatic
    inline val GlobalPositionAware get() = NodeKind<GlobalPositionAwareModifierNode>(0b1 shl 8)

    @JvmStatic
    inline val FocusProperties get() = NodeKind<FocusPropertiesModifierNode>(0b1 shl 11)

    @JvmStatic
    inline val FocusEvent get() = NodeKind<FocusEventModifierNode>(0b1 shl 12)

    @JvmStatic
    inline val KeyInput get() = NodeKind<KeyInputModifierNode>(0b1 shl 13)

    @JvmStatic
    inline val RotaryInput get() = NodeKind<RotaryInputModifierNode>(0b1 shl 14)

    @JvmStatic
    inline val CompositionLocalConsumer get() = NodeKind<CompositionLocalConsumerModifierNode>(0b1 shl 15)

    @JvmStatic
    inline val SoftKeyboardKeyInput get() = NodeKind<SoftKeyboardInterceptionModifierNode>(0b1 shl 17)

    @JvmStatic
    inline val Traversable get() = NodeKind<TraversableNode>(0b1 shl 18)
}

@PublishedApi
internal inline fun <reified T> Modifier.Node.dispatchForKind(
    kind: NodeKind<T>,
    block: (T) -> Boolean,
): Boolean {
    var stack: MutableVector<Modifier.Node>? = null
    var node: Modifier.Node? = this
    while (node != null) {
        if (node is T) {
            if (!block(node)) return false
        } else if (node.isKindOf(kind) && node is DelegatingNode) {
            // We jump through a few extra hoops here to avoid the vector allocation in the
            // case where there is only one delegate node that implements this particular kind.
            // It is very likely that a delegating node will have one or zero delegates of a
            // particular kind, so this seems like a worthwhile optimization to make.
            var count = 0
            node.forEachDelegate { next ->
                if (next.isKindOf(kind)) {
                    count++
                    if (count == 1) {
                        node = next
                    } else {
                        // turns out there are multiple delegates that implement this kind, so we
                        // have to allocate in this case.
                        stack = stack ?: mutableVectorOf()
                        val theNode = node
                        if (theNode != null) {
                            stack?.add(theNode)
                            node = null
                        }
                        stack?.add(next)
                    }
                }
            }
            if (count == 1) {
                // if count == 1 then `node` is pointing to the "next" node we need to look at
                continue
            }
        }
        node = stack.pop()
    }
    return true
}

@PublishedApi
internal fun Modifier.Node.isKindOf(kind: NodeKind<*>) = kindSet and kind.mask != 0

@PublishedApi
internal fun MutableVector<Modifier.Node>?.pop(): Modifier.Node? {
    return if (this == null || isEmpty()) null
    else removeAt(size - 1)
}

@PublishedApi
internal fun DelegatingNode.forEachDelegate(block: (Modifier.Node) -> Unit) {
    var node: Modifier.Node? = delegate
    while (node != null) {
        block(node)
        node = node.child
    }
}