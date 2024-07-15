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

package androidx.compose.ui.platform

import androidx.collection.ArraySet
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.AwtDragAndDropTransferable
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropModifierNode
import androidx.compose.ui.draganddrop.DragAndDropNode
import androidx.compose.ui.draganddrop.DragAndDropTransferAction
import androidx.compose.ui.draganddrop.DragAndDropTransferAction.Companion.Copy
import androidx.compose.ui.draganddrop.DragAndDropTransferAction.Companion.Link
import androidx.compose.ui.draganddrop.DragAndDropTransferAction.Companion.Move
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.density
import androidx.compose.ui.window.layoutDirectionFor
import java.awt.Image
import java.awt.MouseInfo
import java.awt.Point
import java.awt.datatransfer.Transferable
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetDragEvent
import java.awt.dnd.DropTargetDropEvent
import java.awt.dnd.DropTargetEvent
import java.awt.dnd.DropTargetListener
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.TransferHandler
import kotlin.math.roundToInt

/**
 * Returns the AWT transfer action corresponding to the [DragAndDropTransferAction].
 */
private val DragAndDropTransferAction.awtAction: Int
    get() = when (this) {
        Copy -> TransferHandler.COPY
        Move -> TransferHandler.MOVE
        Link -> TransferHandler.LINK
        else -> TransferHandler.NONE
    }

/**
 * Returns the [DragAndDropTransferAction] corresponding to the given AWT transfer [action].
 */
internal fun DragAndDropTransferAction.Companion.fromAwtAction(
    action: Int
): DragAndDropTransferAction? = when (action) {
    TransferHandler.COPY -> Copy
    TransferHandler.MOVE -> Move
    TransferHandler.LINK -> Link
    else -> null
}

/**
 * Implements [PlatformDragAndDropManager] via the AWT drag-and-drop system.
 */
internal class AwtDragAndDropManager(
    private val rootContainer: JComponent
): PlatformDragAndDropManager {

    private val rootDragAndDropNode = DragAndDropNode { null }

    private val transferHandler = ComposeTransferHandler()

    private val dropTarget = ComposeDropTarget()

    private val interestedNodes = ArraySet<DragAndDropModifierNode>()

    private val density: Density
        get() = rootContainer.density

    private val scale: Float
        get() = density.density

    override val modifier: Modifier
        get() = Modifier
            .then(DragAndDropModifier(rootDragAndDropNode))
            .onPlaced {
                rootContainer.transferHandler = transferHandler
                rootContainer.dropTarget = dropTarget
            }

    private fun Point.toOffset(): Offset {
        val scale = this@AwtDragAndDropManager.scale
        return Offset(
            x = x * scale,
            y = y * scale
        )
    }

    private fun Offset.toPoint(): Point {
        val scale = this@AwtDragAndDropManager.scale
        return Point(
            (x / scale).roundToInt(),
            (y / scale).roundToInt()
        )
    }

    override fun drag(
        transferData: DragAndDropTransferData,
        decorationSize: Size,
        drawDragDecoration: DrawScope.() -> Unit
    ): Boolean {
        // These should actually be the values in the local composition where dragAndDropSource was
        // used, but we don't currently have access to them, so we use the ones corresponding to
        // the root container.
        val density = this@AwtDragAndDropManager.density
        val layoutDirection = layoutDirectionFor(rootContainer)

        transferHandler.startOutgoingTransfer(
            transferData = transferData,
            dragImage = renderDragImage(
                size = decorationSize,
                density = density,
                layoutDirection = layoutDirection,
                drawDragDecoration = drawDragDecoration
            ),
            dragImageOffset = transferData.dragOffset.toPoint()
        )

        return true
    }

    override fun registerNodeInterest(node: DragAndDropModifierNode) {
        interestedNodes.add(node)
    }

    override fun isInterestedNode(node: DragAndDropModifierNode): Boolean {
        return interestedNodes.contains(node)
    }

    /**
     * Renders the image to represent the dragged object for AWT.
     */
    private fun renderDragImage(
        size: Size,
        density: Density,
        layoutDirection: LayoutDirection,
        drawDragDecoration: DrawScope.() -> Unit
    ): Image {
        val imageBitmap = ImageBitmap(
            width = size.width.roundToInt(),
            height = size.height.roundToInt()
        )
        // This results in blurry text for some reason.
        val canvas = Canvas(imageBitmap)
        val canvasScope = CanvasDrawScope()
        canvasScope.draw(density, layoutDirection, canvas, size, drawDragDecoration)
        return imageBitmap.toAwtImage().let {
            it.getScaledInstance(
                (it.width / density.density).roundToInt(),
                (it.height / density.density).roundToInt(),
                Image.SCALE_SMOOTH
            )
        }
    }

    private inner class ComposeTransferHandler : TransferHandler() {

        private var outgoingTransfer: OutgoingTransfer? = null

        fun startOutgoingTransfer(
            transferData: DragAndDropTransferData,
            dragImage: Image,
            dragImageOffset: Point,
        ) {
            outgoingTransfer = OutgoingTransfer(
                transferData = transferData,
                dragImage = dragImage,
                dragImageOffset = dragImageOffset
            )

            val rootContainerLocation = rootContainer.locationOnScreen
            val mouseLocation = MouseInfo.getPointerInfo().location?.let {
                IntOffset(
                    x = it.x - rootContainerLocation.x,
                    y = it.y - rootContainerLocation.y
                )
            } ?: rootContainerLocation.let { IntOffset(it.x, it.y) }
            transferHandler.exportAsDrag(
                rootContainer,
                MouseEvent(
                    rootContainer,
                    MouseEvent.MOUSE_DRAGGED,
                    System.currentTimeMillis(),
                    0,
                    mouseLocation.x,
                    mouseLocation.y,
                    0,
                    false
                ),
                // This seems to be ignored, and the initial action is MOVE regardless
                DnDConstants.ACTION_MOVE
            )
        }

        override fun createTransferable(c: JComponent?): Transferable? {
            return (outgoingTransfer?.transferData?.transferable as? AwtDragAndDropTransferable)
                ?.toAwtTransferable()
        }

        override fun getSourceActions(c: JComponent?): Int {
            val actions = outgoingTransfer?.transferData?.supportedActions ?: emptyList()
            return actions.fold(
                initial = NONE,
                operation = { acc, action -> acc or action.awtAction },
            )
        }

        override fun getDragImage() = outgoingTransfer?.dragImage

        override fun getDragImageOffset() = outgoingTransfer?.dragImageOffset

        override fun exportDone(source: JComponent?, data: Transferable?, action: Int) {
            super.exportDone(source, data, action)

            val transferAction = DragAndDropTransferAction.fromAwtAction(action)
            outgoingTransfer?.transferData?.onTransferCompleted?.invoke(transferAction)
            outgoingTransfer = null
        }
    }

    private class OutgoingTransfer(
        val transferData: DragAndDropTransferData,
        val dragImage: Image,
        dragImageOffset: Point
    ) {
        val dragImageOffset: Point = dragImageOffset.let {
            when (DesktopPlatform.Current) {
                DesktopPlatform.MacOS -> Point(-it.x, -it.y)
                else -> it
            }
        }
    }

    private inner class ComposeDropTarget : DropTarget(
        rootContainer,
        DnDConstants.ACTION_MOVE or DnDConstants.ACTION_COPY or DnDConstants.ACTION_LINK,
        object : DropTargetListener {
            override fun dragEnter(dtde: DropTargetDragEvent) {
                val event = DragAndDropEvent(dtde)

                // There's no drag-start event in AWT, so start in dragEnter, and stop in dragExit
                val accepted = rootDragAndDropNode.acceptDragAndDropTransfer(event)
                interestedNodes.forEach { it.onStarted(event) }
                rootDragAndDropNode.onEntered(event)
                if (!accepted) {
                    dtde.rejectDrag()
                }
            }

            override fun dragExit(dte: DropTargetEvent) {
                val event = DragAndDropEvent(dte)
                rootDragAndDropNode.onExited(event)
                endDrag(event)
            }

            override fun dragOver(dtde: DropTargetDragEvent) {
                rootDragAndDropNode.onMoved(DragAndDropEvent(dtde))
            }

            override fun dropActionChanged(dtde: DropTargetDragEvent) {
                rootDragAndDropNode.onChanged(DragAndDropEvent(dtde))
            }

            override fun drop(dtde: DropTargetDropEvent) {
                val event = DragAndDropEvent(dtde)
                dtde.acceptDrop(dtde.dropAction)
                dtde.dropComplete(rootDragAndDropNode.onDrop(event))
                endDrag(event)
            }

            private fun endDrag(event: DragAndDropEvent) {
                rootDragAndDropNode.onEnded(event)
                interestedNodes.clear()
            }

            private fun DragAndDropEvent(dragEvent: DropTargetDragEvent) = DragAndDropEvent(
                nativeEvent = dragEvent,
                action = DragAndDropTransferAction.fromAwtAction(dragEvent.dropAction),
                positionInRootImpl = dragEvent.location.toOffset()
            )

            private fun DragAndDropEvent(dropEvent: DropTargetDropEvent) = DragAndDropEvent(
                nativeEvent = dropEvent,
                action = DragAndDropTransferAction.fromAwtAction(dropEvent.dropAction),
                positionInRootImpl = dropEvent.location.toOffset()
            )

            private fun DragAndDropEvent(dropEvent: DropTargetEvent) = DragAndDropEvent(
                nativeEvent = dropEvent,
                action = null,
                positionInRootImpl = Offset.Zero
            )
        },
        true
    )

}

private class DragAndDropModifier(
    val dragAndDropNode: DragAndDropNode
) : ModifierNodeElement<DragAndDropNode>() {

    override fun create() = dragAndDropNode

    override fun update(node: DragAndDropNode) = Unit

    override fun InspectorInfo.inspectableProperties() {
        name = "RootDragAndDropNode"
    }

    override fun hashCode(): Int = dragAndDropNode.hashCode()

    override fun equals(other: Any?) = other === this

}