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

package androidx.compose.ui.dnd

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toPainter
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.window.LocalWindow
import androidx.compose.ui.window.density
import java.awt.Image
import java.awt.Point
import java.awt.Window
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.DataFlavor.selectBestTextFlavor
import java.awt.datatransfer.Transferable
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetDragEvent
import java.awt.dnd.DropTargetDropEvent
import java.awt.dnd.DropTargetEvent
import java.awt.dnd.DropTargetListener
import java.awt.image.BufferedImage
import java.io.File

/**
 * Represent data types drag and dropped to an application from outside.
 */
@ExperimentalComposeUiApi
sealed interface DropData {
    /**
     * Represents list of files drag and dropped to an application in a raw [java.net.URI] format.
     */
    data class FilesList(val rawUris: List<String>) : DropData

    /**
     * Represents an image drag and dropped to an application.
     */
    data class Image(val painter: Painter) : DropData

    /**
     * Represent text drag and dropped to an application.
     *
     * @param mimeType mimeType of the [content] such as "text/plain", "text/html", etc.
     */
    data class Text(val content: String, val mimeType: String?) : DropData
}

/**
 * Adds detector of external drag and drop (e.g. files DnD from Finder to an application)
 *
 * @param onDragStart will be called when the pointer with external content entered the component.
 * @param onDrag will be called for all drag events inside the component.
 * @param onDrop is called when the pointer is released with [DropData] the pointer held.
 * @param onDragCancel is called if the pointer exited the component bounds or unknown data was dropped.
 */
@ExperimentalComposeUiApi
@Composable
fun Modifier.onExternalDrag(
    enabled: Boolean = true,
    onDragStart: (Offset) -> Unit = {},
    onDrag: (Offset) -> Unit = {},
    onDragCancel: () -> Unit = {},
    onDrop: (DropData) -> Unit = {},
): Modifier = composed {
    if (!enabled) {
        return@composed Modifier
    }
    val window = LocalWindow.current ?: return@composed Modifier

    val onDragStartState by rememberUpdatedState(onDragStart)
    val onDragState by rememberUpdatedState(onDrag)
    val onDragCancelState by rememberUpdatedState(onDragCancel)
    val onDropState by rememberUpdatedState(onDrop)

    var componentDragHandleId by remember { mutableStateOf<Int?>(null) }

    DisposableEffect(window) {
        when (val currentDropTarget = window.dropTarget) {
            is AwtWindowDropTarget -> {
                // if our drop target is already assigned simply add new drag handler for the current component
                componentDragHandleId = currentDropTarget.installComponentDragHandler(
                    onDragStartState, onDragState, onDragCancelState, onDropState
                )
            }

            null -> {
                // drop target is not installed for the window, so assign it and add new drag handler for the current component
                val newDropTarget = AwtWindowDropTarget(window)
                componentDragHandleId = newDropTarget.installComponentDragHandler(
                    onDragStartState, onDragState, onDragCancelState, onDropState
                )
                window.dropTarget = newDropTarget
            }

            else -> {
                error("Window already has unknown external dnd handler, cannot attach onExternalDrag")
            }
        }

        onDispose {
            // stop drag events handling for this component when window is changed
            // or the component leaves the composition
            val dropTarget = window.dropTarget
            if (dropTarget is AwtWindowDropTarget) {
                val handleIdToRemove = componentDragHandleId
                if (handleIdToRemove != null) {
                    dropTarget.stopDragHandling(handleIdToRemove)
                }
            }
        }
    }

    Modifier
        .onGloballyPositioned { position ->
            // provide new component bounds to Swing to properly detect drag events
            val dropTarget = window.dropTarget as? AwtWindowDropTarget
                ?: return@onGloballyPositioned
            val handleIdToUpdate = componentDragHandleId ?: return@onGloballyPositioned
            val componentBounds = position.boundsInWindow()
            dropTarget.updateComponentBounds(handleIdToUpdate, componentBounds)
        }
}

/**
 * Provides a way to subscribe on external drag for given [window] using [installComponentDragHandler]
 *
 * [Window] allows having only one [DropTarget], so this is the main [DropTarget] that handles all the drag subscriptions
 *
 * @VisibleForTesting
 */
@OptIn(ExperimentalComposeUiApi::class)
internal class AwtWindowDropTarget(
    private val window: Window
) : DropTarget(window, DnDConstants.ACTION_MOVE, null, true) {
    private var idsCounter = 0

    // all components that are subscribed to external drag and drop for the window
    private val handlers = mutableMapOf<Int, ComponentDragHandler>()

    // drag coordinates used to detect that drag entered/exited components
    private var windowDragCoordinates: Offset? = null

    // @VisibleForTesting
    val dragTargetListener = AwtWindowDragTargetListener(
        window,
        // notify components on window border that drag is started.
        onDragEnterWindow = { newWindowDragCoordinates ->
            for (handler in handlers.values) {
                val componentBounds = handler.currentComponentBounds ?: continue
                val isInside = isExternalDragInsideComponent(
                    componentBounds,
                    newWindowDragCoordinates
                )
                if (isInside) {
                    handler.onDragStart(
                        calculateOffset(componentBounds, newWindowDragCoordinates)
                    )
                }
            }
            windowDragCoordinates = newWindowDragCoordinates
        },
        // drag moved inside window, we should calculate whether drag entered/exited components or just moved inside them
        onDragInsideWindow = { newWindowDragCoordinates ->
            for (handler in handlers.values) {
                val componentBounds = handler.currentComponentBounds ?: continue
                val oldDragCoordinates = windowDragCoordinates

                val wasDragInside =
                    isExternalDragInsideComponent(componentBounds, oldDragCoordinates)
                val newIsDragInside =
                    isExternalDragInsideComponent(componentBounds, newWindowDragCoordinates)

                val dragOffset = calculateOffset(componentBounds, newWindowDragCoordinates)

                if (!wasDragInside && newIsDragInside) {
                    handler.onDragStart(dragOffset)
                }

                if (wasDragInside && !newIsDragInside) {
                    handler.onDragCancel()
                }

                if (newIsDragInside) {
                    handler.onDrag(dragOffset)
                }
            }
            windowDragCoordinates = newWindowDragCoordinates
        },
        // notify components on window border drag exited window
        onDragExit = {
            for (handler in handlers.values) {
                val componentBounds = handler.currentComponentBounds ?: continue
                val oldDragCoordinates = windowDragCoordinates
                val wasDragInside =
                    isExternalDragInsideComponent(componentBounds, oldDragCoordinates)
                if (wasDragInside) {
                    handler.onDragCancel()
                }
            }
            windowDragCoordinates = null
        },
        // notify all components under the pointer that drop happened
        onDrop = {
            var anyDrops = false
            for (handler in handlers.values) {
                val componentBounds = handler.currentComponentBounds ?: continue
                val isInside = isExternalDragInsideComponent(
                    componentBounds,
                    windowDragCoordinates
                )
                if (isInside) {
                    handler.onDrop(it)
                    anyDrops = true
                }
            }
            windowDragCoordinates = null
            // tell swing whether some components accepted the drop
            return@AwtWindowDragTargetListener anyDrops
        }
    )

    init {
        addDropTargetListener(dragTargetListener)
    }

    override fun setActive(isActive: Boolean) {
        super.setActive(isActive)
        if (!isActive) {
            windowDragCoordinates = null
        }
    }

    /**
     * Adds handler that will be notified on drag events for [window].
     * If component bounds are provided using [updateComponentBounds],
     * given lambdas will be called on drag events.
     *
     * @return handler id that can be used later to remove subscription using [stopDragHandling]
     * or to update component bounds using [updateComponentBounds]
     */
    fun installComponentDragHandler(
        onDragStart: (Offset) -> Unit,
        onDrag: (Offset) -> Unit,
        onDragCancel: () -> Unit,
        onDrop: (DropData) -> Unit
    ): Int {
        isActive = true
        handlers[idsCounter] = ComponentDragHandler(
            currentComponentBounds = null,
            onDragStart, onDrag, onDragCancel, onDrop
        )
        return idsCounter++
    }

    /**
     * Unsubscribes handler with [handleId].
     * Calls [ComponentDragHandler.onDragCancel] if drag is going and handler's component is under pointer
     *
     * Disable drag handling for [window] if there are no more handlers.
     *
     * @param handleId id provided by [installComponentDragHandler] function
     */
    fun stopDragHandling(handleId: Int) {
        val handler = handlers[handleId]
        if (handler != null &&
            isExternalDragInsideComponent(handler.currentComponentBounds, windowDragCoordinates)
        ) {
            handler.onDragCancel()
        }
        handlers.remove(handleId)

        if (handlers.isEmpty()) {
            isActive = false
        }
    }

    /**
     * Updates component bounds within the [window], so drag events will be properly handled.
     * If drag is going and component is under the pointer, onDragStart and onDrag will be called.
     * If drag is going and component moved/become smaller, so that pointer now is not the component, onDragCancel is called.
     *
     * All further drag events will use [newComponentBounds] to notify handler with [handleId].
     *
     * @param newComponentBounds new bounds of the component inside [window] used to properly detect when drag entered/exited component
     */
    fun updateComponentBounds(handleId: Int, newComponentBounds: Rect) {
        val handler = handlers[handleId] ?: return
        val oldComponentBounds = handler.currentComponentBounds
        val currentWindowDragCoordinates = windowDragCoordinates
        if (currentWindowDragCoordinates != null) {
            // drag is going
            val wasDragInside =
                isExternalDragInsideComponent(oldComponentBounds, currentWindowDragCoordinates)
            val newIsDragInside =
                isExternalDragInsideComponent(newComponentBounds, currentWindowDragCoordinates)
            val dragOffset = calculateOffset(newComponentBounds, currentWindowDragCoordinates)

            if (!wasDragInside && newIsDragInside) {
                handler.onDragStart(dragOffset)
            }

            if (wasDragInside && !newIsDragInside) {
                handler.onDragCancel()
            }

            if (newIsDragInside) {
                handler.onDrag(dragOffset)
            }
        }
        handler.currentComponentBounds = newComponentBounds
    }

    private class ComponentDragHandler(
        var currentComponentBounds: Rect?,
        val onDragStart: (Offset) -> Unit,
        val onDrag: (Offset) -> Unit,
        val onDragCancel: () -> Unit,
        val onDrop: (DropData) -> Unit
    )

    companion object {
        private fun isExternalDragInsideComponent(
            componentBounds: Rect?,
            windowDragCoordinates: Offset?
        ): Boolean {
            if (componentBounds == null || windowDragCoordinates == null) {
                return false
            }

            return componentBounds.contains(windowDragCoordinates)
        }

        private fun calculateOffset(
            componentBounds: Rect,
            windowDragCoordinates: Offset
        ): Offset {
            return windowDragCoordinates - componentBounds.topLeft
        }
    }
}

// @VisibleForTesting
@OptIn(ExperimentalComposeUiApi::class)
internal class AwtWindowDragTargetListener(
    private val window: Window,
    val onDragEnterWindow: (Offset) -> Unit,
    val onDragInsideWindow: (Offset) -> Unit,
    val onDragExit: () -> Unit,
    val onDrop: (DropData) -> Boolean,
) : DropTargetListener {
    private val density = window.density.density

    override fun dragEnter(dtde: DropTargetDragEvent) {
        onDragEnterWindow(dtde.location.windowOffset())
    }

    override fun dragOver(dtde: DropTargetDragEvent) {
        onDragInsideWindow(dtde.location.windowOffset())
    }

    // takes title bar and other insets into account
    private fun Point.windowOffset(): Offset {
        val offsetX = (x - window.insets.left) * density
        val offsetY = (y - window.insets.top) * density

        return Offset(offsetX, offsetY)
    }

    override fun dropActionChanged(dtde: DropTargetDragEvent) {
        // Should we notify about it?
    }

    override fun dragExit(dte: DropTargetEvent) {
        onDragExit()
    }

    override fun drop(dtde: DropTargetDropEvent) {
        dtde.acceptDrop(dtde.dropAction)

        val transferable = dtde.transferable
        try {
            val dropData = transferable.dropData() ?: run {
                onDragExit()
                dtde.dropComplete(false)
                return
            }
            onDrop(dropData)
            dtde.dropComplete(true)
            return
        } catch (e: Exception) {
            onDragExit()
            dtde.dropComplete(false)
        }
    }

    private fun Transferable.dropData(): DropData? {
        val bestTextFlavor = selectBestTextFlavor(transferDataFlavors)

        return when {
            isDataFlavorSupported(DataFlavor.javaFileListFlavor) -> {
                val files = getTransferData(DataFlavor.javaFileListFlavor) as? List<*> ?: return null
                DropData.FilesList(files.filterIsInstance<File>().map { it.toURI().toString() })
            }

            isDataFlavorSupported(DataFlavor.imageFlavor) -> {
                val image = getTransferData(DataFlavor.imageFlavor) as? Image ?: return null
                DropData.Image(image.painter())
            }

            bestTextFlavor != null -> {
                val reader = bestTextFlavor.getReaderForText(this) ?: return null
                DropData.Text(content = reader.readText(), mimeType = bestTextFlavor.mimeType)
            }

            else -> null
        }
    }

    private fun Image.painter(): Painter {
        if (this is BufferedImage) {
            return this.toPainter()
        }
        val bufferedImage = BufferedImage(getWidth(null), getHeight(null), BufferedImage.TYPE_INT_ARGB)

        val g2 = bufferedImage.createGraphics()
        try {
            g2.drawImage(this, 0, 0, null)
        } finally {
            g2.dispose()
        }

        return bufferedImage.toPainter()
    }
}