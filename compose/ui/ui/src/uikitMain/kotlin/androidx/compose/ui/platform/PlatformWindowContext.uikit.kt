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

import androidx.compose.ui.animation.withAnimationProgress
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.uikit.density
import androidx.compose.ui.unit.asCGPoint
import androidx.compose.ui.unit.asCGRect
import androidx.compose.ui.unit.asDpOffset
import androidx.compose.ui.unit.asDpRect
import androidx.compose.ui.unit.asDpSize
import androidx.compose.ui.unit.roundToIntSize
import androidx.compose.ui.unit.toDpOffset
import androidx.compose.ui.unit.toDpRect
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toRect
import androidx.compose.ui.unit.toSize
import kotlin.time.Duration
import kotlinx.cinterop.useContents
import platform.UIKit.UIView

/**
 * Tracking a state of window.
 */
internal class PlatformWindowContext {
    private val _windowInfo = WindowInfoImpl().apply {
        isWindowFocused = true
    }

    val windowInfo: WindowInfo get() = _windowInfo

    /**
     * A container used for additional layers and as reference for window coordinate space.
     */
    private var windowContainer: UIView? = null

    fun setWindowContainer(windowContainer: UIView) {
        this.windowContainer = windowContainer

        updateWindowContainerSize()
    }

    private var isAnimating = false
    fun prepareAndGetSizeTransitionAnimation(): suspend (Duration) -> Unit {
        isAnimating = true
        val initialSize = _windowInfo.containerSize.toSize()

        return { duration ->
            try {
                if (initialSize != currentWindowContainerSize) {
                    withAnimationProgress(duration) { progress ->
                        val size = currentWindowContainerSize ?: initialSize
                        _windowInfo.containerSize =
                            lerp(initialSize, size, progress).roundToIntSize()
                    }
                }
            } finally {
                isAnimating = false
                updateWindowContainerSize()
            }
        }
    }

    fun updateWindowContainerSize() {
        if (isAnimating) return

        _windowInfo.containerSize = currentWindowContainerSize?.roundToIntSize() ?: return
    }

    private val currentWindowContainerSize: Size? get() {
        val windowContainer = windowContainer ?: return null

        return windowContainer.bounds.useContents {
            with(windowContainer.density) {
                size.asDpSize().toSize()
            }
        }
    }

    fun convertLocalToWindowPosition(container: UIView, localPosition: Offset): Offset {
        val windowContainer = windowContainer ?: return localPosition
        return convertPoint(
            point = localPosition,
            fromView = container,
            toView = windowContainer
        )
    }

    fun convertWindowToLocalPosition(container: UIView, positionInWindow: Offset): Offset {
        val windowContainer = windowContainer ?: return positionInWindow
        return convertPoint(
            point = positionInWindow,
            fromView = windowContainer,
            toView = container
        )
    }

    fun convertLocalToScreenPosition(container: UIView, localPosition: Offset): Offset {
        val nativeWindow =
            container.window ?: return convertLocalToWindowPosition(container, localPosition)
        val density = container.density
        val positionInNativeWindow = container.convertPoint(
            point = localPosition.toDpOffset(density).asCGPoint(),
            toView = nativeWindow,
        )
        val positionOnScreen = nativeWindow.convertPoint(
            point = positionInNativeWindow,
            toWindow = null
        )
        return positionOnScreen.useContents {
            asDpOffset().toOffset(density)
        }
    }

    fun convertScreenToLocalPosition(container: UIView, positionOnScreen: Offset): Offset {
        val nativeWindow =
            container.window ?: return convertWindowToLocalPosition(container, positionOnScreen)
        val density = container.density
        val positionInNativeWindow = nativeWindow.convertPoint(
            point = positionOnScreen.toDpOffset(density).asCGPoint(),
            fromWindow = null
        )
        val localPosition = container.convertPoint(
            point = positionInNativeWindow,
            fromView = nativeWindow,
        )
        return localPosition.useContents {
            asDpOffset().toOffset(density)
        }
    }

    /**
     * Converts the given [boundsInWindow] from the coordinate space of the container window to [toView] space.
     */
    fun convertWindowRect(boundsInWindow: Rect, toView: UIView): Rect {
        val windowContainer = windowContainer ?: return boundsInWindow
        return convertRect(
            rect = boundsInWindow,
            fromView = windowContainer,
            toView = toView
        )
    }

    private fun convertRect(rect: Rect, fromView: UIView, toView: UIView): Rect {
        return if (fromView != toView) {
            val density = fromView.density

            fromView.convertRect(
                rect = rect.toDpRect(density).asCGRect(),
                toView = toView,
            ).useContents {
                asDpRect().toRect(density)
            }
        } else {
            rect
        }
    }

    private fun convertPoint(point: Offset, fromView: UIView, toView: UIView): Offset {
        return if (fromView != toView) {
            val density = fromView.density
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
