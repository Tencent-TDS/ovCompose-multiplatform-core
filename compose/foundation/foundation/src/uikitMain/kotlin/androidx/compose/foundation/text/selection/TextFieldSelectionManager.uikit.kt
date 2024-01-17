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

package androidx.compose.foundation.text.selection

import androidx.compose.foundation.isPlatformMagnifierSupported
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
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize

internal actual fun Modifier.textFieldMagnifier(manager: TextFieldSelectionManager): Modifier {
    if (!isPlatformMagnifierSupported()) {
        return this
    }

    return composed {
        val density = LocalDensity.current
        var magnifierSize by remember { mutableStateOf(IntSize.Zero) }

        val color = LocalTextSelectionColors.current

        animatedSelectionMagnifier(
            magnifierCenter = {
                calculateSelectionMagnifierCenterIOS(
                    manager = manager,
                    magnifierSize = magnifierSize,
                    density = density.density
                )
            },
            platformMagnifier = { center ->
                Modifier.magnifier(
                    sourceCenter = {
                        center()
                    },
                    onSizeChanged = { size ->
                        magnifierSize = with(density) {
                            IntSize(size.width.roundToPx(), size.height.roundToPx())
                        }
                    },
                    color = color.handleColor, // align magnifier border color with selection handleColor
                )
            }
        )
    }
}

// similar to calculateSelectionMagnifierCenterAndroid, but
// 1) doesn't hide when drag does horizontally out of text but is still in the text field bounds
// 2) hides when drag goes below the text field
// TODO: magnifier should also hide when selection goes to the next line in multiline text field (but shouldn't when it goes to the previous line)
@OptIn(InternalFoundationTextApi::class)
private fun calculateSelectionMagnifierCenterIOS(
    manager: TextFieldSelectionManager,
    magnifierSize: IntSize,
    density : Float,
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

    val textFieldRect = fieldCoordinates.localBoundingBoxOf(containerCoordinates)

    val maxExtraMagnifierOffset = magnifierSize.width / 4 //

    // for some reason Android center calculation is shifted by the decoration box offset on iOS
    // so this offset is substracted
    val centerX = dragX.coerceIn(
        textFieldOffsetInDecorationBox - maxExtraMagnifierOffset,
        textFieldOffsetInDecorationBox + textFieldRect.right + maxExtraMagnifierOffset
    ) - textFieldOffsetInDecorationBox

    // hide magnifier when selection goes below text field
    // TODO: magnifier doesn't hide if text field was scrolled vertically :(
    if (containerCoordinates.localPositionOf(fieldCoordinates, localDragPosition).y >
        layoutResult.lastBaseline + HideThresholdDp * density
    ) {
        return Offset.Unspecified
    }

    return containerCoordinates.localPositionOf(
        fieldCoordinates,
        Offset(centerX, offsetCenter.y)
    )
}

private const val HideThresholdDp = 54
