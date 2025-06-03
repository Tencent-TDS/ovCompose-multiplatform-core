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

package androidx.compose.ui.platform

import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.uikit.utils.TMMComposeCoreDeviceDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.cinterop.useContents
import platform.UIKit.UIApplication
import platform.UIKit.UIView
import kotlin.math.roundToInt

/**
 * This class copied from Desktop sourceSet.
 * Tracking a state of window.
 *
 * TODO: Extract information about window from [PlatformContext] in skiko source set.
 *
 */
internal class PlatformWindowContext {
    private val _windowInfo = WindowInfoImpl()
    val windowInfo: WindowInfo get() = _windowInfo

    private val density = TMMComposeCoreDeviceDensity()

    private var _windowContainer: UIView? = null

    fun setKeyboardModifiers(modifiers: PointerKeyboardModifiers) {
        _windowInfo.keyboardModifiers = modifiers
    }

    fun setWindowFocused(focused: Boolean) {
        _windowInfo.isWindowFocused = focused
    }

    fun setWindowContainer(windowContainer: UIView) {
        _windowContainer = windowContainer
    }

    fun setContainerSize(size: IntSize) {
        if (_windowInfo.containerSize != size) {
            _windowInfo.containerSize = size
        }
    }

    /**
     * Calculates the offset of the given [container] within the window.
     * It uses [_windowContainer] as a reference for window coordinate space.
     *
     * @param container The container component whose offset needs to be calculated.
     * @return The offset of the container within the window as an [IntOffset] object.
     */
    fun offsetInWindow(container: UIView): IntOffset {
        // region Tencent Code Modify
        /**
         * val density = container.systemDensity
         * return if (_windowContainer != null && _windowContainer != container) {
         *     container.convertPoint(
         *         point = cValue { container.bounds.useContents { origin } },
         *         toView = _windowContainer,
         *     )
         * } else {
         *     cValue { container.bounds.useContents { origin } }
         * }.useContents {
         *     toDpOffset().toOffset(density).round()
         * }
         */
        val window = container.window ?: UIApplication.sharedApplication.keyWindow
        return window?.convertRect(container.bounds, fromView = container)?.useContents {
            val offsetX = if (origin.x.isNaN()) 0 else (origin.x * density).roundToInt()
            val offsetY = if (origin.y.isNaN()) 0 else (origin.y * density).roundToInt()
            IntOffset(offsetX, offsetY)
        } ?: IntOffset(0, 0)
        // endregion

    }
}
