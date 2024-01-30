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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.toDpRect
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.roundToIntRect
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.uiContentSizeCategoryToFontScaleMap
import kotlinx.cinterop.useContents
import platform.UIKit.UIContentSizeCategoryUnspecified
import platform.UIKit.UIScreen
import platform.UIKit.UIView

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

    private var _windowContainer: UIView? = null

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

    fun setWindowContainer(windowContainer: UIView) {
        _windowContainer = windowContainer
    }

    fun setContainerSize(size: IntSize) {
        if (_windowInfo.containerSize != size) {
            _windowInfo.containerSize = size
        }
    }

    /**
     * Calculates the bounds of the given [container] within the window.
     * It uses [_windowContainer] as a reference for window coordinate space.
     *
     * @param container The container component whose bounds need to be calculated.
     * @return The bounds of the container within the window as an [IntRect] object.
     */
    fun boundsInWindow(container: UIView): IntRect {
        fun getSystemDensity(): Density {
            val contentSizeCategory = container.traitCollection.preferredContentSizeCategory
                ?: UIContentSizeCategoryUnspecified
            return Density(
                density = UIScreen.mainScreen.scale.toFloat(),
                fontScale = uiContentSizeCategoryToFontScaleMap[contentSizeCategory] ?: 1.0f
            )
        }

        val density = getSystemDensity()
        return if (_windowContainer != null && _windowContainer != container) {
            container.convertRect(
                rect = container.bounds,
                toView = _windowContainer,
            )
        } else {
            container.bounds
        }.useContents {
            with(density) {
                toDpRect().toRect().roundToIntRect()
            }
        }
    }

}
