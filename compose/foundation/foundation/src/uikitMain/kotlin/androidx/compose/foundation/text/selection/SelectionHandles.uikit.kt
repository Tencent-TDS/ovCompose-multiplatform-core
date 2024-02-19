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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

/**
 * Clickable padding of handler
 */
private val PADDING = 5.dp

/**
 * Radius of handle circle
 */
private val RADIUS = 6.dp

/**
 * Thickness of handlers vertical line
 */
private val THICKNESS = 2.dp

@Composable
internal actual fun SelectionHandle(
    offsetProvider: OffsetProvider,
    isStartHandle: Boolean,
    direction: ResolvedTextDirection,
    handlesCrossed: Boolean,
    lineHeight: Float,
    modifier: Modifier,
) {
    val isLeft = isLeftSelectionHandle(isStartHandle, direction, handlesCrossed)
    // The left selection handle's top right is placed at the given position, and vice versa.
    val alignment = if (isLeft) Alignment.BottomCenter else Alignment.TopCenter
    val offset = if (isLeft) Offset.Zero else Offset(0f, -lineHeight)

    HandlePopup(positionProvider = offsetProvider, alignment = alignment, offset = offset) {
        SelectionHandleIcon(
            modifier = modifier.semantics {
                val position = offsetProvider.provide()
                this[SelectionHandleInfoKey] = SelectionHandleInfo(
                    handle = if (isStartHandle) Handle.SelectionStart else Handle.SelectionEnd,
                    position = position,
                    anchor = if (isLeft) SelectionHandleAnchor.Left else SelectionHandleAnchor.Right,
                    visible = position.isSpecified,
                )
            },
            iconVisible = { offsetProvider.provide().isSpecified },
            lineHeight = lineHeight,
            isLeft = isLeft,
        )
    }
}

@Composable
/*@VisibleForTesting*/
internal fun SelectionHandleIcon(
    modifier: Modifier,
    iconVisible: () -> Boolean,
    lineHeight: Float,
    isLeft: Boolean,
) {
    val density = LocalDensity.current
    val lineHeightDp = with(density) { lineHeight.toDp() }
    Spacer(
        modifier
            .size(
                width = (PADDING + RADIUS) * 2,
                height = RADIUS * 2 + PADDING + lineHeightDp
            )
            .drawSelectionHandle(iconVisible, lineHeight, isLeft)
    )
}

internal fun Modifier.drawSelectionHandle(
    iconVisible: () -> Boolean,
    lineHeight: Float,
    isLeft: Boolean
): Modifier = composed {
    val density = LocalDensity.current
    val paddingPx = with(density) { PADDING.toPx() }
    val radiusPx = with(density) { RADIUS.toPx() }
    val thicknessPx = with(density) { THICKNESS.toPx() }
    val handleColor = LocalTextSelectionColors.current.handleColor
    this.drawWithCache {
        onDrawWithContent {
            drawContent()
            if (!iconVisible()) return@onDrawWithContent

            // vertical line
            drawRect(
                color = handleColor,
                topLeft = Offset(
                    x = paddingPx + radiusPx - thicknessPx / 2,
                    y = if (isLeft) paddingPx + radiusPx else 0f
                ),
                size = Size(thicknessPx, lineHeight + radiusPx)
            )
            // handle circle
            drawCircle(
                color = handleColor,
                radius = radiusPx,
                center = center.copy(
                    y = if (isLeft) paddingPx + radiusPx else lineHeight + radiusPx
                )
            )
        }
    }
}

@Composable
internal fun HandlePopup(
    positionProvider: OffsetProvider,
    alignment: Alignment,
    offset: Offset = Offset.Zero,
    content: @Composable () -> Unit
) {
    val popupPositionProvider = remember(alignment, positionProvider, offset) {
        HandlePositionProvider(alignment, positionProvider, offset)
    }
    Popup(
        popupPositionProvider = popupPositionProvider,
        properties = PopupProperties(clippingEnabled = false),
        content = content,
    )
}
