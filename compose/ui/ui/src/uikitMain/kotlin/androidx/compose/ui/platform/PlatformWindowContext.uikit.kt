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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.uikit.systemDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.asCGPoint
import androidx.compose.ui.unit.asCGRect
import androidx.compose.ui.unit.asDpOffset
import androidx.compose.ui.unit.toDpOffset
import androidx.compose.ui.unit.toDpRect
import androidx.compose.ui.unit.toOffset
import kotlinx.cinterop.CValue
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectZero
import platform.UIKit.UIView
import platform.UIKit.UIWindow

/**
 * Tracking a state of window.
 */
internal class PlatformWindowContext {
    private val _windowInfo = WindowInfoImpl()
    val windowInfo: WindowInfo get() = _windowInfo

    /**
     * A container used for additional layers and as reference for window coordinate space.
     */
    private var _windowContainer: UIView? = null

    fun setWindowFocused(focused: Boolean) {
        _windowInfo.isWindowFocused = focused
    }

    fun setWindowContainer(windowContainer: UIView) {
        _windowContainer = windowContainer
    }

    fun setContainerSize(size: IntSize) {
        _windowInfo.containerSize = size
    }

    fun calculatePositionInWindow(container: UIView, localPosition: Offset): Offset {
        val windowContainer = _windowContainer ?: return localPosition
        return convertPoint(
            point = localPosition,
            fromView = container,
            toView = windowContainer
        )
    }

    fun calculateLocalPosition(container: UIView, positionInWindow: Offset): Offset {
        val windowContainer = _windowContainer ?: return positionInWindow
        return convertPoint(
            point = positionInWindow,
            fromView = windowContainer,
            toView = container
        )
    }

    /**
     * Converts the given [Rect] from the coordinate space of the container window to the coordinate
     * space of the root UIWindow in which hierarchy with the container window resides.
     */
    fun convertContainerWindowRectToRootWindowCGRect(rect: Rect): CValue<CGRect> {
        val windowContainer = _windowContainer ?: return CGRectZero.readValue()

        val windowContainerWindow = windowContainer.window

        fun UIWindow.density(): Density {
            return Density(screen.scale.toFloat())
        }

        return if (windowContainerWindow == null) {
            if (windowContainer is UIWindow) {
                // windowContainer is the root window itself
                rect.toDpRect(windowContainer.density()).asCGRect()
            } else {
                // windowContainer is not attached to any window
                CGRectZero.readValue()
            }
        } else {
            val cgRect = rect
                .toDpRect(windowContainerWindow.density())
                .asCGRect()

            windowContainer.convertRect(cgRect, toView = windowContainerWindow)
        }
    }

    private fun convertPoint(point: Offset, fromView: UIView, toView: UIView): Offset {
        return if (fromView != toView) {
            val density = fromView.systemDensity
            fromView.convertPoint(
                point = point.toDpOffset(density).asCGPoint(),
                toView = toView,
            ).useContents {
                asDpOffset().toOffset(density)
            }
        } else {
            point
        }
    }
}
