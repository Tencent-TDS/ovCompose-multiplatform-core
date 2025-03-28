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

package androidx.compose.ui.awt

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntRect
import java.awt.Component
import java.awt.EventQueue
import java.awt.Graphics
import java.awt.Rectangle
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JLayeredPane
import kotlin.math.ceil
import kotlin.math.floor
import org.jetbrains.skiko.GraphicsApi
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.hostOs

internal fun Component.isParentOf(component: Component?): Boolean {
    var parent = component?.parent
    while (parent != null) {
        if (parent == this) {
            return true
        }
        parent = parent.parent
    }
    return false
}

internal fun IntRect.toAwtRectangle(density: Density): Rectangle {
    val left = floor(left / density.density).toInt()
    val top = floor(top / density.density).toInt()
    val right = ceil(right / density.density).toInt()
    val bottom = ceil(bottom / density.density).toInt()
    val width = right - left
    val height = bottom - top
    return Rectangle(
        left, top, width, height
    )
}

internal fun Color.toAwtColor() = java.awt.Color(red, green, blue, alpha)

internal fun getTransparentWindowBackground(
    isWindowTransparent: Boolean,
    renderApi: GraphicsApi
): java.awt.Color? {
    /**
     * There is a hack inside skiko OpenGL and Software redrawers for Windows that makes current
     * window transparent without setting `background` to JDK's window. It's done by getting native
     * component parent and calling `DwmEnableBlurBehindWindow`.
     *
     * FIXME: Make OpenGL work inside transparent window (background == Color(0, 0, 0, 0)) without this hack.
     *
     * See `enableTransparentWindow` (skiko/src/awtMain/cpp/windows/window_util.cc)
     */
    val skikoTransparentWindowHack = hostOs == OS.Windows && renderApi != GraphicsApi.DIRECT3D
    return if (isWindowTransparent && !skikoTransparentWindowHack) java.awt.Color(0, 0, 0, 0) else null
}


/**
 * Windows makes clicks on transparent pixels fall through, but it doesn't work
 * with GPU accelerated rendering since this check requires having access to pixels from CPU.
 *
 * JVM doesn't allow override this behaviour with low-level windows methods, so hack this by filling
 * the background with an almost transparent color.
 * Based on tests, it doesn't affect resulting pixel color.
 */
internal open class JLayeredPaneWithTransparencyHack: JLayeredPane() {
    override fun paint(g: Graphics) {
        if (!isOpaque && UseTransparencyHack) {
            // Fill the background with an almost transparent color
            g.color = AlmostTransparent
            val r = g.clipBounds
            if (r != null) {
                g.fillRect(r.x, r.y, r.width, r.height)
            } else {
                g.fillRect(0, 0, width, height)
            }
        }

        super.paint(g)
    }

    private companion object {

        @JvmStatic
        val AlmostTransparent = java.awt.Color(0, 0, 0, 1)

        @JvmStatic
        private val UseTransparencyHack = hostOs == OS.Windows

    }
}

/**
 * A utility for running code on the event dispatching thread, making sure it is not queued more
 * than once.
 */
internal class DebouncingEdtExecutor {

    /**
     * Whether any code has been scheduled.
     */
    private val isScheduled = AtomicBoolean(false)

    /**
     * Calls [block] on the event dispatching thread.
     *
     * If the thread calling this function is the event dispatching thread, executes [block] and
     * cancels any previously scheduled blocks. Otherwise, if no block is currently scheduled,
     * schedules [block] to run event dispatching thread. If a block is already scheduled, does
     * nothing.
     *
     * Note that this utility is not intended to run or schedule multiple different blocks of code
     * at the same time, as only one block of code can be scheduled at a time.
     */
    fun runOrScheduleDebounced(block: () -> Unit) {
        if (EventQueue.isDispatchThread()) {
            isScheduled.set(false)
            block()
        } else if (!isScheduled.getAndSet(true)) {
            EventQueue.invokeLater {
                if (isScheduled.getAndSet(false)) {
                    block()
                }
            }
        }
    }
}
