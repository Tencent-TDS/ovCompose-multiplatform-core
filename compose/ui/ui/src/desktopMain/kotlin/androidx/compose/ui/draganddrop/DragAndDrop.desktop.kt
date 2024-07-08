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

import androidx.compose.ui.DragData
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.dragData
import androidx.compose.ui.geometry.Offset
import java.awt.datatransfer.Transferable
import androidx.compose.ui.platform.PlatformDragAndDropManager
import java.awt.dnd.DropTargetDropEvent

/**
 * Encapsulates the information needed to start a drag-and-drop session from Compose on the desktop.
 */
@ExperimentalComposeUiApi
actual class DragAndDropTransferData(
    val transferable: DragAndDropTransferable,
    val supportedActions: Iterable<DragAndDropTransferAction>,
    val initialAction: DragAndDropTransferAction,
    val dragOffset: Offset = Offset.Zero,
    val onTransferCompleted: ((userAction: DragAndDropTransferAction?) -> Unit)? = null,
)

/**
 * Represents the actual object transferred during a drag-and-drop.
 */
@ExperimentalComposeUiApi
abstract class DragAndDropTransferable {
    /**
     * Returns the AWT [Transferable] to pass to the AWT drag-and-drop system.
     *
     * This must return a non-null value in order to work with the AWT implementation of
     * [PlatformDragAndDropManager].
     */
    abstract fun toAwtTransferable(): Transferable?
}

/**
 * The possible actions on the transferred object in a drag-and-drop session.
 */
@ExperimentalComposeUiApi
class DragAndDropTransferAction private constructor() {
    companion object {
        val Copy = DragAndDropTransferAction()
        val Move = DragAndDropTransferAction()
        val Link = DragAndDropTransferAction()
    }
}

/**
 * The event dispatched to [DragAndDropTarget] implementations during a drag-and-drop session.
 */
@ExperimentalComposeUiApi
actual class DragAndDropEvent(
    /**
     * The underlying native event dispatched when the drag-and-drop gesture ends in a drop; only
     * available in [DragAndDropTarget.onDrop].
     *
     * Use [DragAndDropEvent.awtTransferable] to access it..
     */
    val nativeDropEvent: Any?,

    /**
     * The position of the dragged object relative to the root Compose container.
     */
    internal val positionInRootImpl: Offset
)

/**
 * A [DragAndDropTransferable] that simply wraps an AWT [Transferable] instance.
 */
@ExperimentalComposeUiApi
class AwtDragAndDropTransferable(
    private val transferable: Transferable
) : DragAndDropTransferable() {
    override fun toAwtTransferable(): Transferable = transferable
}

/**
 * Returns the AWT [Transferable] associated with the [DragAndDropEvent].
 *
 * This may only be called on a [DragAndDropEvent] received in [DragAndDropTarget.onDrop].
 */
@ExperimentalComposeUiApi
val DragAndDropEvent.awtTransferable: Transferable
    get() = (nativeDropEvent as DropTargetDropEvent).transferable

/**
 * Returns the [DragData] associated with the given [DragAndDropEvent].
 *
 * This may only be called on a [DragAndDropEvent] received in [DragAndDropTarget.onDrop].
 */
@ExperimentalComposeUiApi
fun DragAndDropEvent.dragData(): DragData = awtTransferable.dragData()

internal actual val DragAndDropEvent.positionInRoot: Offset
    get() = positionInRootImpl

