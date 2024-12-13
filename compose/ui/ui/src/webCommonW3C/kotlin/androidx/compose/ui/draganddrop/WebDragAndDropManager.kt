package androidx.compose.ui.draganddrop

import androidx.compose.ui.events.EventTargetListener
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.PlatformDragAndDropManager
import androidx.compose.ui.platform.PlatformDragAndDropSource
import androidx.compose.ui.scene.ComposeSceneDragAndDropNode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.roundToInt
import kotlinx.browser.document
import kotlinx.browser.window
import org.khronos.webgl.Uint8ClampedArray
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.DragEvent
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.ImageData
import org.w3c.dom.HTMLElement

internal abstract class WebDragAndDropManager(eventListener: EventTargetListener, globalEventsListener: EventTargetListener, private val density: Density) :
    PlatformDragAndDropManager {
    override val isRequestDragAndDropTransferRequired: Boolean
        get() = false

    abstract val rootDragAndDropNode: ComposeSceneDragAndDropNode

    private val startTransferScope = object : PlatformDragAndDropSource.StartTransferScope {
        /**
         * Context for an ongoing drag session initiated from Compose.
         */
        var dragSessionContext: DragSessionContext? = null


        override fun startDragAndDropTransfer(
            transferData: DragAndDropTransferData,
            decorationSize: Size,
            drawDragDecoration: DrawScope.() -> Unit
        ): Boolean {
            dragSessionContext = DragSessionContext()

            val imageBitmap = ImageBitmap(
                width = decorationSize.width.roundToInt(),
                height = decorationSize.height.roundToInt()
            )

            val canvas = Canvas(imageBitmap)
            val canvasScope = CanvasDrawScope()

            canvasScope.draw(density, LayoutDirection.Ltr, canvas, decorationSize, drawDragDecoration)

            val intArray = IntArray(imageBitmap.width * imageBitmap.height)
            imageBitmap.readPixels(intArray)

            val uint8ClampedArray = intArray.toUint8ClampedArray()

            val imageData = ImageData(uint8ClampedArray, imageBitmap.width, imageBitmap.height)

            val canvasConverter = document.createElement("canvas") as HTMLCanvasElement

            val scale = density.density

            canvasConverter.width = imageBitmap.width
            canvasConverter.height = imageBitmap.height

            require(scale > 0f)

            val width = (decorationSize.width / scale).toInt()
            val height = (decorationSize.height / scale).toInt()

            canvasConverter.style.width = "${width}px"
            canvasConverter.style.height = "${height}px"

            val canvasConverterContext = canvasConverter.getContext("2d") as CanvasRenderingContext2D
            canvasConverterContext.putImageData(imageData, 0.0, 0.0)

            dragSessionContext?.ghostImage = canvasConverter

            return true
        }
    }


    init {
        initEvents(eventListener, globalEventsListener)
    }

    private fun DragEvent.setAsDragImage(ghostImage: HTMLElement) {
        with (ghostImage.style) {
            position = "absolute"

            top = "0"
            left = "0"

            setProperty("pointer-events", "none")
        }

        // non-image elements passed to setDragImage should be present on document
        // the only browser the only browser not burdened with this limitation is Firefox
        document.body?.appendChild(ghostImage)

        dataTransfer?.setDragImage(ghostImage, 0, 0)

        // After browser made a snapshot we can safely remove ghostImage from document
        // But it should be done in different frame
        window.requestAnimationFrame {
            ghostImage.remove()
        }
    }

    private fun initEvents(eventListener: EventTargetListener, globalEventsListener: EventTargetListener) {
        eventListener.addDisposableEvent("dragstart") { event ->
            event as DragEvent

            with (rootDragAndDropNode) {
                startTransferScope.startDragAndDropTransfer(event.offset) {
                    startTransferScope.dragSessionContext != null
                }

                if (startTransferScope.dragSessionContext != null) {
                    val dragEvent = DragAndDropEvent(event.offset)
                    val acceptedTransfer = acceptDragAndDropTransfer(dragEvent)

                    if (acceptedTransfer) {
                        onStarted(dragEvent)
                        onEntered(dragEvent)

                        startTransferScope.dragSessionContext?.ghostImage?.let { ghostImage ->
                            event.setAsDragImage(ghostImage)
                        }
                    }
                } else {
                    event.preventDefault()
                }
            }
        }

        eventListener.addDisposableEvent("drag") { event ->
            event as DragEvent
            rootDragAndDropNode.onMoved(DragAndDropEvent(event.offset))
        }

        eventListener.addDisposableEvent("dragend") { event ->
            event as DragEvent
            val dragAndDropEvent = DragAndDropEvent(event.offset)
            rootDragAndDropNode.onDrop(dragAndDropEvent)
            rootDragAndDropNode.onEnded(dragAndDropEvent)

            startTransferScope.dragSessionContext = null
        }

        globalEventsListener.addDisposableEvent("dragover") { event ->
            event as DragEvent
            event.preventDefault()
            event.dataTransfer?.dropEffect = "move"
        }
    }

    private val DragEvent.offset get() = Offset(
        x = offsetX.toFloat(),
        y = offsetY.toFloat()
    ) * density.density
}

private class DragSessionContext(
    var ghostImage: HTMLCanvasElement? = null
)

@Suppress("UNUSED_PARAMETER")
private fun setMethodImplForUint8ClampedArray(obj: Uint8ClampedArray, index: Int, value: Int) { js("obj[index] = value;") }
private operator fun Uint8ClampedArray.set(index: Int, value: Int) = setMethodImplForUint8ClampedArray(this, index, value)

private fun IntArray.toUint8ClampedArray(): Uint8ClampedArray {
    val uint8ClampedArray = Uint8ClampedArray(size * 4)

    forEachIndexed { index, intValue ->
        val offset = index * 4

        // red
        uint8ClampedArray[offset] = (intValue shr 16) and 0xFF

        // green
        uint8ClampedArray[offset + 1] = (intValue shr 8) and 0xFF

        // blue
        uint8ClampedArray[offset + 2] = intValue and 0xFF

        // alpha
        uint8ClampedArray[offset + 3] = (intValue shr 24) and 0xFF
    }

    return uint8ClampedArray
}