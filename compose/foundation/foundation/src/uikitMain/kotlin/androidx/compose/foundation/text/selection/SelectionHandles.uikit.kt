/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.foundation.text.selection

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.Handle
import androidx.compose.foundation.text.selection.HandleReferencePoint.TopLeft
import androidx.compose.foundation.text.selection.HandleReferencePoint.TopMiddle
import androidx.compose.foundation.text.selection.HandleReferencePoint.TopRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.valueToState
import kotlin.math.roundToInt

private const val PADDING = 5f
private const val RADIUS = 6f
private const val THICKNESS = 2f

@Composable
internal actual fun SelectionHandle(
    position: Offset,
    isStartHandle: Boolean,
    direction: ResolvedTextDirection,
    handlesCrossed: Boolean,
    lineHeight: Float,
    modifier: Modifier,
    content: @Composable (() -> Unit)?
) {
    val isLeft = isLeft(isStartHandle, direction, handlesCrossed)
    // The left selection handle's top right is placed at the given position, and vice versa.
    val handleReferencePoint = if (isLeft) {
        HandleReferencePoint.TopRight
    } else {
        HandleReferencePoint.TopLeft
    }

    val y = if (isLeft) position.y - PADDING - lineHeight - RADIUS * 2 else position.y - PADDING

    val positionState: State<IntOffset> = valueToState(
        IntOffset(position.x.roundToInt(), y.roundToInt())
    )

    HandlePopup(position = positionState, handleReferencePoint = handleReferencePoint) {
        if (content == null) {
            DefaultSelectionHandle(
                modifier = modifier
                    .semantics {
                        this[SelectionHandleInfoKey] = SelectionHandleInfo(
                            handle = if (isStartHandle) {
                                Handle.SelectionStart
                            } else {
                                Handle.SelectionEnd
                            },
                            position = position
                        )
                    },
                isStartHandle = isStartHandle,
                direction = direction,
                handlesCrossed = handlesCrossed,
                isLeft = isLeft,
                lineHeight = lineHeight
            )
        } else {
            content()
        }
    }
}

@Composable
/*@VisibleForTesting*/
internal fun DefaultSelectionHandle(
    modifier: Modifier,
    isStartHandle: Boolean,
    direction: ResolvedTextDirection,
    handlesCrossed: Boolean,
    isLeft: Boolean,
    lineHeight: Float
) {
    val handleColor = LocalTextSelectionColors.current.handleColor
    androidx.compose.foundation.Canvas(Modifier) {
        drawRect(
            handleColor,
            topLeft = Offset(-THICKNESS / 2, if (isLeft) lineHeight - RADIUS else PADDING - lineHeight),
            size = Size(THICKNESS, lineHeight + RADIUS)
        )
    }
    Spacer(
        modifier
            .size((PADDING * 2 + RADIUS * 2).dp, (PADDING * 2 + RADIUS * 2).dp)
//            .offset(x = -RADIUS.dp, y = -lineHeight.dp)
            .drawSelectionHandle(isStartHandle, direction, handlesCrossed, lineHeight)
    )
}

@Suppress("ModifierInspectorInfo")
internal fun Modifier.drawSelectionHandle(
    isStartHandle: Boolean,
    direction: ResolvedTextDirection,
    handlesCrossed: Boolean,
    lineHeight: Float
) = composed {
    val handleColor = LocalTextSelectionColors.current.handleColor
    this.then(
        Modifier.drawWithCache {
            val isLeft = isLeft(isStartHandle, direction, handlesCrossed)
            val handleImage = createHandleImage(lineHeight, isLeft)
            val colorFilter = ColorFilter.tint(handleColor)
            onDrawWithContent {
                drawContent()
                drawImage(
                    image = handleImage,
                    colorFilter = colorFilter
                )
            }
        }
    )
}

/**
 * The cache for the image mask created to draw selection/cursor handle, so that we don't need to
 * recreate them.
 */
private object HandleImageCache {
    var imageBitmap: ImageBitmap? = null
    var canvas: Canvas? = null
    var canvasDrawScope: CanvasDrawScope? = null
}

/**
 * Create an image bitmap for the basic shape of a selection handle or cursor handle. It is an
 * circle with a rectangle covering its left top part.
 *
 * To draw the right selection handle, directly draw this image bitmap.
 * To draw the left selection handle, mirror the canvas first and then draw this image bitmap.
 * To draw the cursor handle, translate and rotated the canvas 45 degrees, then draw this image
 * bitmap.
 *
 * @param lineHeight the lineHeight of text line in selection/cursor handle.
 * CanvasDrawScope objects so that we only recreate them when necessary.
 */
internal fun CacheDrawScope.createHandleImage(lineHeight: Float, isLeft: Boolean): ImageBitmap {
    // The edge length of the square bounding box of the selection/cursor handle. This is also
    // the size of the bitmap needed for the bitmap mask.

    var imageBitmap = HandleImageCache.imageBitmap
    var canvas = HandleImageCache.canvas
    var drawScope = HandleImageCache.canvasDrawScope

    // If the cached bitmap is null or too small, we need to create new bitmap.
    if (
        imageBitmap == null ||
        canvas == null
    ) {
        imageBitmap = ImageBitmap(
            width = (PADDING * 2 + RADIUS * 2).toInt(),
            height = (PADDING * 2 + RADIUS * 2).toInt(),
            config = ImageBitmapConfig.Alpha8
        )
        HandleImageCache.imageBitmap = imageBitmap
        canvas = Canvas(imageBitmap)
        HandleImageCache.canvas = canvas
    }
    if (drawScope == null) {
        drawScope = CanvasDrawScope()
        HandleImageCache.canvasDrawScope = drawScope
    }

    drawScope.draw(
        this,
        layoutDirection,
        canvas,
        Size(imageBitmap.width.toFloat(), imageBitmap.height.toFloat())
    ) {
        // Clear the previously rendered portion within this ImageBitmap as we could
        // be re-using it
        drawRect(
            color = Color.Black,
            size = size,
            blendMode = BlendMode.Clear
        )
        // Draw the circle
        drawCircle(
            color = Color(0xFF000000),
            radius = RADIUS,
            center = Offset(PADDING + RADIUS, PADDING + RADIUS)
        )
    }
    return imageBitmap
}

@Composable
internal fun HandlePopup(
    position: State<IntOffset>,
    handleReferencePoint: HandleReferencePoint,
    content: @Composable () -> Unit
) {
    val popupPositioner = remember(handleReferencePoint) {
        HandlePositionProvider(handleReferencePoint) { position.value }
    }

    Popup(
        popupPositionProvider = popupPositioner,
        content = content
    )
}

/**
 * The enum that specifies how a selection/cursor handle is placed to its given position.
 * When this value is [TopLeft], the top left corner of the handle will be placed at the
 * given position.
 * When this value is [TopRight], the top right corner of the handle will be placed at the
 * given position.
 * When this value is [TopMiddle], the handle top edge's middle point will be placed at the given
 * position.
 */
internal enum class HandleReferencePoint {
    TopLeft,
    TopRight,
    TopMiddle
}

/**
 * This [PopupPositionProvider] for [HandlePopup]. It will position the selection handle
 * to the [getOffset] in its anchor layout.
 *
 * @see HandleReferencePoint
 */
/*@VisibleForTesting*/
internal class HandlePositionProvider(
    private val handleReferencePoint: HandleReferencePoint,
    private val getOffset: () -> IntOffset
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val offset = getOffset()
        return IntOffset(
            x = anchorBounds.left + offset.x - popupContentSize.width / 2,
            y = anchorBounds.top + offset.y
        )
    }
}

/**
 * Computes whether the handle's appearance should be left-pointing or right-pointing.
 */
private fun isLeft(
    isStartHandle: Boolean,
    direction: ResolvedTextDirection,
    handlesCrossed: Boolean
): Boolean {
    return if (isStartHandle) {
        isHandleLtrDirection(direction, handlesCrossed)
    } else {
        !isHandleLtrDirection(direction, handlesCrossed)
    }
}

/**
 * This method is to check if the selection handles should use the natural Ltr pointing
 * direction.
 * If the context is Ltr and the handles are not crossed, or if the context is Rtl and the handles
 * are crossed, return true.
 *
 * In Ltr context, the start handle should point to the left, and the end handle should point to
 * the right. However, in Rtl context or when handles are crossed, the start handle should point to
 * the right, and the end handle should point to left.
 */
/*@VisibleForTesting*/
internal fun isHandleLtrDirection(
    direction: ResolvedTextDirection,
    areHandlesCrossed: Boolean
): Boolean {
    return direction == ResolvedTextDirection.Ltr && !areHandlesCrossed ||
        direction == ResolvedTextDirection.Rtl && areHandlesCrossed
}
