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

package androidx.compose.ui.platform

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.density
import java.awt.Component
import java.awt.Container
import java.awt.Point
import javax.swing.SwingUtilities
import kotlin.math.roundToInt

/**
 * Tracking a state of window.
 */
internal class PlatformWindowContext {
    private val _windowInfo = WindowInfoImpl()
    val windowInfo: WindowInfo get() = _windowInfo

    private var _windowContainer: Container? = null

    /**
     * Indicates whether the window is transparent or not.
     * Used for determining the right blending mode for [Dialog]'s scrim.
     */
    var isWindowTransparent: Boolean by mutableStateOf(false)

    fun setKeyboardModifiers(modifiers: PointerKeyboardModifiers) {
        _windowInfo.keyboardModifiers = modifiers
    }

    fun setWindowFocused(focused: Boolean) {
        _windowInfo.isWindowFocused = focused
    }

    fun setWindowContainer(windowContainer: Container) {
        _windowContainer = windowContainer
    }

    fun setContainerSize(size: Size) {
        _windowInfo.containerSize = IntSize(
            width = size.width.roundToInt(),
            height = size.height.roundToInt()
        )
    }

    fun calculatePositionInWindow(container: Component, localPosition: Offset): Offset {
        val windowContainer = _windowContainer ?: return localPosition
        return convertPoint(
            position = localPosition,
            fromView = container,
            toView = windowContainer
        )
    }

    fun calculateLocalPosition(container: Component, positionInWindow: Offset): Offset {
        val windowContainer = _windowContainer ?: return positionInWindow
        return convertPoint(
            position = positionInWindow,
            fromView = windowContainer,
            toView = container
        )
    }

    private fun convertPoint(position: Offset, fromView: Component, toView: Component): Offset {
        return if (fromView != toView) {
            val fromPoint = position.toPoint(fromView.density)
            val toPoint = SwingUtilities.convertPoint(fromView, fromPoint, toView)
            toPoint.toOffset(toView.density)
        } else {
            position
        }
    }
}

private fun Offset.toPoint(density: Density): Point {
    val scale = density.density
    return Point(
        (x / scale).roundToInt(),
        (y / scale).roundToInt()
    )
}

private fun Point.toOffset(density: Density): Offset {
    val scale = density.density
    return Offset(
        x = x * scale,
        y = y * scale
    )
}
