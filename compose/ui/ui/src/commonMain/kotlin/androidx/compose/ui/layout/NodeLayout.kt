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

import androidx.compose.runtime.Applier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReusableComposeNode
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.currentCompositeKeyHash
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.node.ComposeUiNode.Companion.SetCompositeKeyHash
import androidx.compose.ui.node.ComposeUiNode.Companion.SetMeasurePolicy
import androidx.compose.ui.node.ComposeUiNode.Companion.SetResolvedCompositionLocals
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.requireLayoutNode
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap

/**
 * A Layout with [onAttach] and [onDetach] of current [LayoutNode].
 */
@UiComposable
@Composable
fun NodeLayout(
    onAttach: (node: LayoutInfo) -> Unit = NoOp,
    onDetach: (node: LayoutInfo) -> Unit = NoOp,
    content: @Composable @UiComposable () -> Unit,
) {
    val compositeKeyHash = currentCompositeKeyHash
    val localMap = currentComposer.currentCompositionLocalMap
    ReusableComposeNode<LayoutNode, Applier<Any>>(
        factory = LayoutNode.Constructor,
        update = {
            set(NodeMeasurePolicy, SetMeasurePolicy)
            set(localMap, SetResolvedCompositionLocals)
            @OptIn(ExperimentalComposeUiApi::class)
            set(compositeKeyHash, SetCompositeKeyHash)
            set(onAttach, SetOnAttach)
            set(onDetach, SetOnDetach)
        },
        content = content
    )
}

val LayoutInfo.parent: LayoutInfo? get() = parentInfo

fun LayoutInfo.children(): List<LayoutInfo> = asLayoutNode().children

val LayoutInfo.modifier: Modifier get() = asLayoutNode().modifier

val LayoutInfo.modifierNode: Modifier.Node get() = asLayoutNode().nodes.head

val LayoutInfo.modifierNodeTail: Modifier.Node get() = asLayoutNode().nodes.tail

val Modifier.Node.layoutInfo: LayoutInfo get() = requireLayoutNode()

private val NoOp: (node: LayoutInfo) -> Unit = { }

private val SetOnAttach: LayoutNode.((node: LayoutInfo) -> Unit) -> Unit =
    { onAttach ->
        this.onAttach = { onAttach(this) }
    }

private val SetOnDetach: LayoutNode.((node: LayoutInfo) -> Unit) -> Unit =
    { onDetach ->
        this.onDetach = { onDetach(this) }
    }

private fun LayoutInfo.asLayoutNode() = this as LayoutNode

private object NodeMeasurePolicy : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints,
    ): MeasureResult {
        return when (measurables.size) {
            0 -> layout(constraints.minWidth, constraints.minHeight) {}

            1 -> measurables[0].measure(constraints).run {
                layout(width, height) {
                    placeRelativeWithLayer(0, 0)
                }
            }

            else -> measurables.fastMap { it.measure(constraints) }.run {
                layout(constraints.maxWidth, constraints.maxHeight) {
                    fastForEach { placeable ->
                        placeable.placeRelativeWithLayer(0, 0)
                    }
                }
            }
        }
    }
}