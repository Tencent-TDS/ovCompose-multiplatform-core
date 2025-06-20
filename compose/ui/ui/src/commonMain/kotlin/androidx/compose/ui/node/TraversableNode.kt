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
package androidx.compose.ui.node

import androidx.compose.ui.ExperimentalTencentComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.areObjectsOfSameType
import androidx.compose.ui.node.TraversableNode.Companion.TraverseDescendantsAction

/**
 * Allows [Modifier.Node] classes to traverse up/down the Node tree for classes of the same type or
 * for a particular key (traverseKey).
 *
 * Note: The actual traversals are done in extension functions (see bottom of file).
 */
interface TraversableNode : DelegatableNode {
    val traverseKey: Any

    companion object {
        /**
         * Tree traversal actions for the traverseDescendantsIf related functions:
         *  - Continue - continue the traversal
         *  - SkipSubtreeAndContinue - continue the traversal BUT skip the matching node's subtree
         *  (this is a rarer case)
         *  - CancelTraversal - cancels the traversal (returns from function call)
         *
         * To see examples of all the actions, see TraversableModifierNodeTest. For a return/cancel
         * example specifically, see
         * traverseSubtreeWithSameKeyIf_cancelTraversalOfDifferentClassSameKey().
         */
        enum class TraverseDescendantsAction {
            ContinueTraversal,
            SkipSubtreeAndContinueTraversal,
            CancelTraversal
        }
    }
}

// *********** Nearest Traversable Ancestor methods ***********
/**
 * Finds the nearest traversable ancestor with a matching [key].
 */
fun DelegatableNode.findNearestAncestor(
    key: Any?
): TraversableNode? {
    visitAncestors(Nodes.Traversable) {
        if (key == it.traverseKey) {
            return it
        }
    }
    return null
}

// region Tencent Code
/**
 * Finds the nearest traversable ancestor with a matching key based on the given [predicate].
 */
@ExperimentalTencentComposeUiApi
fun DelegatableNode.findNearestAncestor(
    predicate: (key: Any?) -> Boolean
): TraversableNode? {
    visitAncestors(Nodes.Traversable) {
        if (predicate(it.traverseKey)) {
            return it
        }
    }
    return null
}
// endregion

/**
 * Finds the nearest ancestor of the same class and key.
 */
fun <T> T.findNearestAncestor(): T? where T : TraversableNode {
    visitAncestors(Nodes.Traversable) {
        if (this.traverseKey == it.traverseKey && areObjectsOfSameType(this, it)) {
            @Suppress("UNCHECKED_CAST")
            return it as T
        }
    }
    return null
}

// region Tencent Code
// *********** Nearest Local Traversable Ancestor methods ***********
/**
 * Finds the nearest local traversable ancestor with a matching [key].
 */
@ExperimentalTencentComposeUiApi
fun DelegatableNode.findNearestLocalAncestor(
    key: Any?
): TraversableNode? {
    visitLocalAncestors(Nodes.Traversable) {
        if (key == it.traverseKey) {
            return it
        }
    }
    return null
}

/**
 * Finds the nearest local traversable ancestor with a matching key based on the given [predicate].
 */
@ExperimentalTencentComposeUiApi
fun DelegatableNode.findNearestLocalAncestor(
    predicate: (key: Any?) -> Boolean
): TraversableNode? {
    visitLocalAncestors(Nodes.Traversable) {
        if (predicate(it.traverseKey)) {
            return it
        }
    }
    return null
}

/**
 * Finds the nearest local ancestor of the same class and key.
 */
@ExperimentalTencentComposeUiApi
fun <T> T.findNearestLocalAncestor(): T? where T : TraversableNode {
    visitLocalAncestors(Nodes.Traversable) {
        if (this.traverseKey == it.traverseKey && areObjectsOfSameType(this, it)) {
            @Suppress("UNCHECKED_CAST")
            return it as T
        }
    }
    return null
}
// endregion

// *********** Traverse Ancestors methods ***********
/**
 * Executes [block] for all ancestors with a matching [key].
 *
 * Note: The parameter [block]'s return boolean value will determine if the traversal will
 * continue (true = continue, false = cancel).
 *
 *  @sample androidx.compose.ui.samples.traverseAncestorsWithKeyDemo
 */
fun DelegatableNode.traverseAncestors(
    key: Any?,
    block: (TraversableNode) -> Boolean
) {
    visitAncestors(Nodes.Traversable) {
        val continueTraversal = if (key == it.traverseKey) {
            block(it)
        } else {
            true
        }
        if (!continueTraversal) return
    }
}

/**
 * Executes [block] for all ancestors of the same class and key.
 *
 * Note: The parameter [block]'s return boolean value will determine if the traversal will
 * continue (true = continue, false = cancel).
 *
 *  @sample androidx.compose.ui.samples.traverseAncestorsDemo
 */
fun <T> T.traverseAncestors(block: (T) -> Boolean) where T : TraversableNode {
    visitAncestors(Nodes.Traversable) {
        val continueTraversal =
            if (this.traverseKey == it.traverseKey && areObjectsOfSameType(this, it)) {
                @Suppress("UNCHECKED_CAST")
                block(it as T)
            } else {
                true
            }
        if (!continueTraversal) return
    }
}

// region Tencent Code
/**
 * Executes [block] for all ancestors.
 *
 * Note: The parameter [block]'s return boolean value will determine if the traversal will
 * continue (true = continue, false = cancel).
 *
 *  @sample androidx.compose.ui.samples.traverseAncestorsWithKeyDemo
 */
@ExperimentalTencentComposeUiApi
fun DelegatableNode.traverseAllAncestors(
    block: (TraversableNode) -> Boolean
) {
    visitAncestors(Nodes.Traversable) {
        val continueTraversal = block(it)
        if (!continueTraversal) return
    }
}
// endregion

// region Tencent Code
// *********** Traverse Local Ancestors methods ***********
/**
 * Executes [block] for all local ancestors with a matching [key].
 *
 * Note: The parameter [block]'s return boolean value will determine if the traversal will
 * continue (true = continue, false = cancel).
 *
 *  @sample androidx.compose.ui.samples.traverseAncestorsWithKeyDemo
 */
@ExperimentalTencentComposeUiApi
fun DelegatableNode.traverseLocalAncestors(
    key: Any?,
    block: (TraversableNode) -> Boolean
) {
    visitLocalAncestors(Nodes.Traversable) {
        val continueTraversal = if (key == it.traverseKey) {
            block(it)
        } else {
            true
        }
        if (!continueTraversal) return
    }
}

/**
 * Executes [block] for all local ancestors of the same class and key.
 *
 * Note: The parameter [block]'s return boolean value will determine if the traversal will
 * continue (true = continue, false = cancel).
 */
@ExperimentalTencentComposeUiApi
fun <T> T.traverseLocalAncestors(block: (T) -> Boolean) where T : TraversableNode {
    visitLocalAncestors(Nodes.Traversable) {
        val continueTraversal =
            if (this.traverseKey == it.traverseKey && areObjectsOfSameType(this, it)) {
                @Suppress("UNCHECKED_CAST")
                block(it as T)
            } else {
                true
            }
        if (!continueTraversal) return
    }
}

/**
 * Executes [block] for all local ancestors.
 *
 * Note: The parameter [block]'s return boolean value will determine if the traversal will
 * continue (true = continue, false = cancel).
 *
 *  @sample androidx.compose.ui.samples.traverseAncestorsWithKeyDemo
 */
@ExperimentalTencentComposeUiApi
fun DelegatableNode.traverseAllLocalAncestors(
    block: (TraversableNode) -> Boolean
) {
    visitLocalAncestors(Nodes.Traversable) {
        val continueTraversal = block(it)
        if (!continueTraversal) return
    }
}
// endregion

// *********** Traverse Children methods ***********
/**
 * Executes [block] for all direct children of the node with a matching [key].
 *
 * Note 1: This stops at the children and does not include grandchildren and so on down the tree.
 *
 * Note 2: The parameter [block]'s return boolean value will determine if the traversal will
 * continue (true = continue, false = cancel).
 *
 *  @sample androidx.compose.ui.samples.traverseChildrenWithKeyDemo
 */
fun DelegatableNode.traverseChildren(
    key: Any?,
    block: (TraversableNode) -> Boolean
) {
    visitChildren(Nodes.Traversable) {
        val continueTraversal = if (key == it.traverseKey) {
            block(it)
        } else {
            true
        }
        if (!continueTraversal) return
    }
}

/**
 * Executes [block] for all direct children of the node that are of the same class.
 *
 * Note 1: This stops at the children and does not include grandchildren and so on down the tree.
 *
 * Note 2: The parameter [block]'s return boolean value will determine if the traversal will
 * continue (true = continue, false = cancel).
 *
 *  @sample androidx.compose.ui.samples.traverseChildrenDemo
 */
fun <T> T.traverseChildren(block: (T) -> Boolean) where T : TraversableNode {
    visitChildren(Nodes.Traversable) {
        val continueTraversal =
            if (this.traverseKey == it.traverseKey && areObjectsOfSameType(this, it)) {
                @Suppress("UNCHECKED_CAST")
                block(it as T)
            } else {
                true
            }
        if (!continueTraversal) return
    }
}

// region Tencent Code
/**
 * Executes [block] for all direct children of the node.
 *
 * Note 1: This stops at the children and does not include grandchildren and so on down the tree.
 *
 * Note 2: The parameter [block]'s return boolean value will determine if the traversal will
 * continue (true = continue, false = cancel).
 *
 *  @sample androidx.compose.ui.samples.traverseChildrenWithKeyDemo
 */
@ExperimentalTencentComposeUiApi
fun DelegatableNode.traverseAllChildren(
    block: (TraversableNode) -> Boolean
) {
    visitChildren(Nodes.Traversable) {
        val continueTraversal = block(it)
        if (!continueTraversal) return
    }
}
// endregion

// *********** Traverse Descendants methods ***********
/**
 * Conditionally executes [block] for each descendant with a matching [key].
 *
 * Note 1: For nodes that do not have the same key, it will continue to execute the [block] for
 * descendants below that non-matching node (where there may be a node that matches).
 *
 * Note 2: The parameter [block]'s return value [TraverseDescendantsAction] will determine the next
 * step in the traversal.
 *
 *  @sample androidx.compose.ui.samples.traverseDescendantsWithKeyDemo
 */
fun DelegatableNode.traverseDescendants(
    key: Any?,
    block: (TraversableNode) -> TraverseDescendantsAction
) {
    visitSubtreeIf(Nodes.Traversable) {
        val action = if (key == it.traverseKey) {
            block(it)
        } else {
            TraverseDescendantsAction.ContinueTraversal
        }
        if (action == TraverseDescendantsAction.CancelTraversal) return

        // visitSubtreeIf() requires a true to continue down the subtree and a false if you
        // want to skip the subtree, so we check if the action is NOT EQUAL to the subtree
        // to trigger false if the action is Skip subtree and true otherwise.
        action != TraverseDescendantsAction.SkipSubtreeAndContinueTraversal
    }
}

/**
 * Conditionally executes [block] for each descendant of the same class.
 *
 * Note 1: For nodes that do not have the same key, it will continue to execute the [block] for
 * the descendants below that non-matching node (where there may be a node that matches).
 *
 * Note 2: The parameter [block]'s return value [TraverseDescendantsAction] will determine the
 * next step in the traversal.
 *
 *  @sample androidx.compose.ui.samples.traverseDescendantsDemo
 */
fun <T> T.traverseDescendants(block: (T) -> TraverseDescendantsAction) where T : TraversableNode {
    visitSubtreeIf(Nodes.Traversable) {
        val action =
            if (this.traverseKey == it.traverseKey && areObjectsOfSameType(this, it)) {
                @Suppress("UNCHECKED_CAST")
                block(it as T)
            } else {
                TraverseDescendantsAction.ContinueTraversal
            }
        if (action == TraverseDescendantsAction.CancelTraversal) return

        // visitSubtreeIf() requires a true to continue down the subtree and a false if you
        // want to skip the subtree, so we check if the action is NOT EQUAL to the subtree
        // to trigger false if the action is Skip subtree and true otherwise.
        action != TraverseDescendantsAction.SkipSubtreeAndContinueTraversal
    }
}

// region Tencent Code
/**
 * Conditionally executes [block] for each descendant.
 *
 * Note: The parameter [block]'s return value [TraverseDescendantsAction] will determine the next
 * step in the traversal.
 *
 *  @sample androidx.compose.ui.samples.traverseDescendantsWithKeyDemo
 */
@ExperimentalTencentComposeUiApi
fun DelegatableNode.traverseAllDescendants(
    block: (TraversableNode) -> TraverseDescendantsAction
) {
    visitSubtreeIf(Nodes.Traversable) {
        val action = block(it)
        if (action == TraverseDescendantsAction.CancelTraversal) return

        // visitSubtreeIf() requires a true to continue down the subtree and a false if you
        // want to skip the subtree, so we check if the action is NOT EQUAL to the subtree
        // to trigger false if the action is Skip subtree and true otherwise.
        action != TraverseDescendantsAction.SkipSubtreeAndContinueTraversal
    }
}
// endregion

// region Tencent Code
// *********** Traverse Local Descendants methods ***********
/**
 * Executes [block] for each descendant with a matching [key].
 *
 * Note: The parameter [block]'s return boolean value will determine if the traversal will
 * continue (true = continue, false = cancel).
 */
@ExperimentalTencentComposeUiApi
fun DelegatableNode.traverseLocalDescendants(
    key: Any?,
    block: (TraversableNode) -> Boolean
) {
    visitLocalDescendants(Nodes.Traversable) {
        val continueTraversal = if (key == it.traverseKey) {
            block(it)
        } else {
            true
        }
        if (!continueTraversal) return
    }
}

/**
 * Executes [block] for all local descendants of the same class and key.
 *
 * Note: The parameter [block]'s return boolean value will determine if the traversal will
 * continue (true = continue, false = cancel).
 */
@ExperimentalTencentComposeUiApi
fun <T> T.traverseLocalDescendants(block: (T) -> Boolean) where T : TraversableNode {
    visitLocalDescendants(Nodes.Traversable) {
        val continueTraversal =
            if (this.traverseKey == it.traverseKey && areObjectsOfSameType(this, it)) {
                @Suppress("UNCHECKED_CAST")
                block(it as T)
            } else {
                true
            }
        if (!continueTraversal) return
    }
}

/**
 * Executes [block] for all local descendants.
 *
 * Note: The parameter [block]'s return boolean value will determine if the traversal will
 * continue (true = continue, false = cancel).
 */
@ExperimentalTencentComposeUiApi
fun DelegatableNode.traverseAllLocalDescendants(
    block: (TraversableNode) -> Boolean
) {
    visitLocalDescendants(Nodes.Traversable) {
        val continueTraversal = block(it)
        if (!continueTraversal) return
    }
}
// endregion