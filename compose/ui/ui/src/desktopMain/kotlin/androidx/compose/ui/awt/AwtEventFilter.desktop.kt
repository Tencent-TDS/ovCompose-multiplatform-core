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

package androidx.compose.ui.awt

import java.awt.Component
import java.awt.Rectangle
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import javax.swing.SwingUtilities

internal interface AwtEventListener {
    /**
     * @return true if the event was consumed, false otherwise
     */
    fun onMouseEvent(event: MouseEvent): Boolean = false

    /**
     * @return true if the event was consumed, false otherwise
     */
    fun onKeyEvent(event: KeyEvent): Boolean = false

    companion object {
        val Empty = object : AwtEventListener {
        }
    }
}

internal class AwtEventListeners(
    private vararg val listeners: AwtEventListener
) : AwtEventListener {
    override fun onMouseEvent(event: MouseEvent): Boolean {
        for (listener in listeners) {
            if (listener.onMouseEvent(event)) {
                return true
            }
        }
        return false
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        for (listener in listeners) {
            if (listener.onKeyEvent(event)) {
                return true
            }
        }
        return false
    }
}

internal class BoundsEventFilter(
    var bounds: Rectangle,
    private val relativeTo: Component,
    private val onOutside: (event: MouseEvent) -> Unit
) : AwtEventListener {
    override fun onMouseEvent(event: MouseEvent): Boolean {
        when (event.id) {
            // Do not filter motion events
            MouseEvent.MOUSE_MOVED,
            MouseEvent.MOUSE_ENTERED,
            MouseEvent.MOUSE_EXITED,
            MouseEvent.MOUSE_DRAGGED -> return false
        }
        return if (event.isInBounds) {
            false
        } else {
            onOutside(event)
            true
        }
    }

    private val MouseEvent.isInBounds: Boolean get() {
        val localPoint = if (component != relativeTo) {
            SwingUtilities.convertPoint(component, point, relativeTo)
        } else {
            point
        }
        return bounds.contains(localPoint)
    }
}

/**
 * Filter out mouse events that report the primary button has changed state to pressed,
 * but aren't themselves a mouse press event. This is needed because on macOS, AWT sends
 * us spurious enter/exit events that report the primary button as pressed when resizing
 * the window by its corner/edge. This causes false-positives in detectTapGestures.
 * See https://github.com/JetBrains/compose-multiplatform/issues/2850 for more details.
*/
internal object OnlyValidPrimaryMouseButtonFilter : AwtEventListener {
    private var isPrimaryButtonPressed = false

    override fun onMouseEvent(event: MouseEvent): Boolean {
        val eventReportsPrimaryButtonPressed =
            (event.modifiersEx and MouseEvent.BUTTON1_DOWN_MASK) != 0
        if ((event.button == MouseEvent.BUTTON1) &&
            ((event.id == MouseEvent.MOUSE_PRESSED) ||
                (event.id == MouseEvent.MOUSE_RELEASED))) {
            isPrimaryButtonPressed = eventReportsPrimaryButtonPressed  // Update state
        }
        if (eventReportsPrimaryButtonPressed && !isPrimaryButtonPressed) {
            return true  // Ignore such events
        }

        return false
    }
}
