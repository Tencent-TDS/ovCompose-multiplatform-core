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

package androidx.compose.ui.draganddrop

import androidx.collection.ArraySet
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import java.awt.dnd.DropTargetDragEvent
import java.awt.dnd.DropTargetDropEvent
import java.awt.dnd.DropTargetEvent
import java.awt.dnd.DropTargetListener

/**
 * A Class that provides access to [DropTargetListener] APIs for a [DragAndDropNode].
 */
private class DragAndDropModifierOnDragListener(
    var density: Float,
    private val startDrag: (
        transferData: DragAndDropTransferData,
        decorationSize: Size,
        drawDragDecoration: DrawScope.() -> Unit
    ) -> Boolean
) : DropTargetListener, DragAndDropManager {

    private val rootDragAndDropNode = DragAndDropNode { null }

    /**
     * A collection [DragAndDropModifierNode] instances that registered interested in a
     * drag and drop session by returning true in [DragAndDropModifierNode.onStarted].
     */
    private val interestedNodes = ArraySet<DragAndDropModifierNode>()

    override val modifier: Modifier = object : ModifierNodeElement<DragAndDropNode>() {
        override fun create() = rootDragAndDropNode

        override fun update(node: DragAndDropNode) = Unit

        override fun InspectorInfo.inspectableProperties() {
            name = "RootDragAndDropNode"
        }

        override fun hashCode(): Int = rootDragAndDropNode.hashCode()

        override fun equals(other: Any?) = other === this
    }

    private fun DropTargetEvent.toDragAndDropEvent() = DragAndDropEvent(
        rootPosition = when (this) {
            is DropTargetDragEvent -> Offset(
                x = density * location.x,
                y = density * location.x,
            )

            is DropTargetDropEvent -> Offset(
                x = density * location.x,
                y = density * location.x,
            )

            else -> Offset.Unspecified
        },
        nativeEvent = this,
    )

    private var inDragAndDropSession: Boolean = false

    override fun dragEnter(nativeEvent: DropTargetDragEvent?) {
        if (nativeEvent == null) return
        val dragAndDropEvent = nativeEvent.toDragAndDropEvent()

        inDragAndDropSession = rootDragAndDropNode.acceptDragAndDropTransfer(dragAndDropEvent)
        interestedNodes.forEach { it.onStarted(dragAndDropEvent) }

        if (inDragAndDropSession) rootDragAndDropNode.onEntered(dragAndDropEvent)
    }

    override fun dragOver(nativeEvent: DropTargetDragEvent?) {
        if (nativeEvent == null || !inDragAndDropSession) return
        val dragAndDropEvent = nativeEvent.toDragAndDropEvent()

        rootDragAndDropNode.onMoved(dragAndDropEvent)
    }

    override fun dropActionChanged(nativeEvent: DropTargetDragEvent?) {
        if (nativeEvent == null || !inDragAndDropSession) return
        val dragAndDropEvent = nativeEvent.toDragAndDropEvent()

        rootDragAndDropNode.onChanged(dragAndDropEvent)
    }

    override fun dragExit(nativeEvent: DropTargetEvent?) {
        if (nativeEvent == null || !inDragAndDropSession) {
            inDragAndDropSession = false
            return
        }
        val dragAndDropEvent = nativeEvent.toDragAndDropEvent()

        rootDragAndDropNode.onExited(dragAndDropEvent)
        rootDragAndDropNode.onEnded(dragAndDropEvent)
        interestedNodes.clear()
        inDragAndDropSession = false
    }

    override fun drop(nativeEvent: DropTargetDropEvent?) {
        if (nativeEvent == null || !inDragAndDropSession) {
            inDragAndDropSession = false
            return
        }
        val dragAndDropEvent = nativeEvent.toDragAndDropEvent()
        nativeEvent.acceptDrop(nativeEvent.dropAction)

        val completedDrop = rootDragAndDropNode.onDrop(dragAndDropEvent)
        nativeEvent.dropComplete(completedDrop)

        rootDragAndDropNode.onEnded(dragAndDropEvent)
        interestedNodes.clear()
        inDragAndDropSession = false
    }

    override fun drag(
        transferData: DragAndDropTransferData,
        decorationSize: Size,
        drawDragDecoration: DrawScope.() -> Unit,
    ): Boolean = startDrag(
        transferData,
        decorationSize,
        drawDragDecoration,
    )

    override fun registerNodeInterest(node: DragAndDropModifierNode) {
        interestedNodes.add(node)
    }

    override fun isInterestedNode(node: DragAndDropModifierNode): Boolean {
        return interestedNodes.contains(node)
    }
}

/**
 * The original raw native event from AWT.
 *
 * Null if:
 * - the native event is sent by another framework (when Compose UI is embed into it)
 * - there is no native event (in tests, for example)
 *
 * Always check for null when you want to handle the native event.
 */
val DragAndDropEvent.awtEventOrNull: DropTargetEvent?
    get() {
        return nativeEvent as? DropTargetEvent?
    }