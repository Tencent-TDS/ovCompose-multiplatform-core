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

package androidx.compose.foundation.text.selection

import androidx.compose.foundation.MagnifierStyle
import androidx.compose.foundation.magnifier
import androidx.compose.foundation.text.Handle
import androidx.compose.foundation.text.InternalFoundationTextApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize

internal actual fun Modifier.textFieldMagnifier(manager: TextFieldSelectionManager): Modifier {
    if (!MagnifierStyle.TextDefault.isSupported) {
        return this
    }

    return composed {
        val density = LocalDensity.current
        var magnifierSize by remember { mutableStateOf(IntSize.Zero) }
        animatedSelectionMagnifier(
            magnifierCenter = {
                calculateSelectionMagnifierCenterIOS(manager, magnifierSize)
            },
            platformMagnifier = { center ->
                Modifier.magnifier(
                    sourceCenter = { center() },
                    onSizeChanged = { size ->
                        magnifierSize = with(density) {
                            IntSize(size.width.roundToPx(), size.height.roundToPx())
                        }
                    },
                    style = MagnifierStyle.TextDefault
                )
            }
        )
    }
}


@OptIn(InternalFoundationTextApi::class)
internal fun calculateSelectionMagnifierCenterIOS(
    manager: TextFieldSelectionManager,
    magnifierSize: IntSize
): Offset {

    val rawTextOffset = when (manager.draggingHandle) {
        null -> return Offset.Unspecified
        Handle.Cursor,
        Handle.SelectionStart -> manager.value.selection.start

        Handle.SelectionEnd -> manager.value.selection.end
    }
    val layoutResult = manager.state?.layoutResult?.value ?: return Offset.Unspecified
    val transformedText = manager.state?.textDelegate?.text ?: return Offset.Unspecified
    val textOffset = manager.offsetMapping
        .takeIf { transformedText.isNotEmpty() }
        ?.originalToTransformed(rawTextOffset)
        ?.coerceIn(transformedText.indices)
        ?: 0

    val containerCoordinates = manager.state?.layoutCoordinates ?: return Offset.Unspecified
    val fieldCoordinates =
        manager.state?.layoutResult?.innerTextFieldCoordinates ?: return Offset.Unspecified
    val localDragPosition = manager.currentDragPosition?.let {
        fieldCoordinates.localPositionOf(containerCoordinates, it)
    } ?: return Offset.Unspecified
    val dragX = localDragPosition.x

    // Center vertically on the current line.
    val offsetCenter = if (transformedText.isNotEmpty())
        layoutResult.getBoundingBox(textOffset).center
    else fieldCoordinates.localBoundingBoxOf(containerCoordinates).center

    val textFieldOffsetInDecorationBox = manager.state?.layoutResult?.decorationBoxCoordinates
        ?.localPositionOf(fieldCoordinates, Offset.Zero)?.x ?: 0f

    val containerRect = containerCoordinates.boundsInWindow()
    val centerX = dragX.coerceIn(0f, containerRect.right)

    // hide magnifier when drag goes below text field (native behavior)
    // TODO: magnifier doesn't hide if text field was scrolled vertically. fix
    if (containerCoordinates.localPositionOf(fieldCoordinates, localDragPosition).y >
        layoutResult.lastBaseline + magnifierSize.height/2) {
        return Offset.Unspecified
    }


    return containerCoordinates.localPositionOf(
        fieldCoordinates,
        Offset(centerX - textFieldOffsetInDecorationBox, offsetCenter.y)
    )
}