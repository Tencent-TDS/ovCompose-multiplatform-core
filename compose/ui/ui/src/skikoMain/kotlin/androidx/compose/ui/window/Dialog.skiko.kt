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

package androidx.compose.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputEvent
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.semantics.dialog
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.center
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxBy

/**
 * The default scrim opacity.
 */
private const val DefaultScrimOpacity = 0.6f
private val DefaultScrimColor = Color.Black.copy(alpha = DefaultScrimOpacity)

@Immutable
actual class DialogProperties @ExperimentalComposeUiApi constructor(
    actual val dismissOnBackPress: Boolean = true,
    actual val dismissOnClickOutside: Boolean = true,
    val usePlatformDefaultWidth: Boolean = true,
    val scrimColor: Color = DefaultScrimColor,
) {
    actual constructor(
        dismissOnBackPress: Boolean,
        dismissOnClickOutside: Boolean
    ) : this(
        dismissOnBackPress = dismissOnBackPress,
        dismissOnClickOutside = dismissOnClickOutside,
        usePlatformDefaultWidth = true,
        scrimColor = DefaultScrimColor
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DialogProperties) return false

        if (dismissOnBackPress != other.dismissOnBackPress) return false
        if (dismissOnClickOutside != other.dismissOnClickOutside) return false

        return true
    }

    override fun hashCode(): Int {
        var result = dismissOnBackPress.hashCode()
        result = 31 * result + dismissOnClickOutside.hashCode()
        return result
    }
}

@Composable
actual fun Dialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties,
    content: @Composable () -> Unit
) {
    var modifier = Modifier
        .semantics { dialog() }
        .drawBehind {
            drawRect(
                color = properties.scrimColor,
                blendMode = BlendMode.SrcAtop
            )
        }
    if (properties.dismissOnBackPress) {
        modifier = modifier.onKeyEvent { event: KeyEvent ->
            if (event.isDismissRequest()) {
                onDismissRequest()
                true
            } else {
                false
            }
        }
    }
    val onOutsidePointerEvent = if (properties.dismissOnClickOutside) {
        { event: PointerInputEvent ->
            if (event.isDismissRequest()) {
                onDismissRequest()
            }
        }
    } else {
        null
    }
    DialogLayout(
        modifier = modifier,
        onOutsidePointerEvent = onOutsidePointerEvent,
        properties = properties,
        content = content
    )
}

@Composable
private fun DialogLayout(
    properties: DialogProperties,
    modifier: Modifier = Modifier,
    onOutsidePointerEvent: ((PointerInputEvent) -> Unit)? = null,
    content: @Composable () -> Unit
) {
    /*
     * Keep empty layout as workaround to trigger layout after remove dialog.
     * Required to properly update mouse hover state.
     */
    EmptyLayout()
    RootLayout(
        modifier = modifier,
        focusable = true,
        onOutsidePointerEvent = onOutsidePointerEvent
    ) { owner ->
        val measurePolicy = rememberDialogMeasurePolicy(properties) {
            owner.bounds = it
        }
        Layout(
            content = content,
            measurePolicy = measurePolicy
        )
    }
}

@Composable
private fun rememberDialogMeasurePolicy(
    properties: DialogProperties,
    onBoundsChanged: (IntRect) -> Unit
) = remember(properties) {
    MeasurePolicy { measurables, constraints ->
            val dialogConstraints = if (properties.usePlatformDefaultWidth) {
            platformDefaultConstrains(constraints)
            } else constraints
        val placeables = measurables.fastMap { it.measure(dialogConstraints) }
        val width = placeables.fastMaxBy { it.width }?.width ?: constraints.minWidth
        val height = placeables.fastMaxBy { it.height }?.height ?: constraints.minHeight

        val placeableSize = IntSize(width, height)
        val windowSize = IntSize(constraints.maxWidth, constraints.maxHeight)
        val position = windowSize.center - placeableSize.center
        onBoundsChanged(IntRect(position, placeableSize))

        layout(windowSize.width, windowSize.height) {
            placeables.fastForEach {
                it.place(position.x, position.y)
            }
        }
    }
}

internal expect fun MeasureScope.platformDefaultConstrains(
    constraints: Constraints
): Constraints

private fun PointerInputEvent.isMainAction() =
    button == PointerButton.Primary ||
        button == null && pointers.size == 1

private fun PointerInputEvent.isDismissRequest() =
    eventType == PointerEventType.Release && isMainAction()

private fun KeyEvent.isDismissRequest() =
    type == KeyEventType.KeyDown && key == Key.Escape
