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

import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.assertThat
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.isEqualTo
import androidx.compose.ui.sendMouseEvent
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.launchApplication
import androidx.compose.ui.window.runApplicationTest
import java.awt.Point
import java.awt.event.MouseEvent
import javax.accessibility.AccessibleContext
import javax.swing.JLayeredPane
import javax.swing.SwingUtilities
import org.jetbrains.skiko.SkiaLayer
import org.junit.Test

class ApplicationAccessibilityTest {
    @Test
    fun `popup text is accessible on hover`() = runApplicationTest {
        lateinit var window: ComposeWindow
        val clickCount = mutableStateOf<Int>(0)
        var popupButtonClicked = false

        launchApplication {
            Window(onCloseRequest = {}) {
                window = this.window
                // show popup on second click, first click is to focus button
                Button(
                    modifier = Modifier.size(100.dp),
                    onClick = {
                        clickCount.value++
                    }
                ) {
                    Text("Accessible button")
                }

                if (clickCount.value >= 2) {
                    // show popup on top of the accessible button
                    val position = object : PopupPositionProvider {
                        override fun calculatePosition(
                            anchorBounds: IntRect,
                            windowSize: IntSize,
                            layoutDirection: LayoutDirection,
                            popupContentSize: IntSize
                        ): IntOffset = IntOffset.Zero
                    }
                    Popup(position, focusable = true) {
                        Button(
                            onClick = {
                                popupButtonClicked = true
                            },
                            modifier = Modifier.size(100.dp)
                        ) {
                            Text("Accessible popup button")
                        }
                    }
                }
            }
        }
        awaitIdle()
        // focus button
        window.sendMouseEvent(MouseEvent.MOUSE_PRESSED, 20, 20, MouseEvent.BUTTON1_DOWN_MASK)
        awaitIdle()
        window.sendMouseEvent(MouseEvent.MOUSE_RELEASED, 20, 20)
        awaitIdle()
        assertThat(clickCount.value).isEqualTo(1)

        checkAccessibleOnPoint(window, 20, 20) {
            assertThat(accessibleName).isEqualTo("Accessible button")
        }

        // open popup
        window.sendMouseEvent(MouseEvent.MOUSE_PRESSED, 20, 20, MouseEvent.BUTTON1_DOWN_MASK)
        awaitIdle()
        window.sendMouseEvent(MouseEvent.MOUSE_RELEASED, 20, 20)
        awaitIdle()
        assertThat(clickCount.value).isEqualTo(2)

        // focus popup button
        window.sendMouseEvent(MouseEvent.MOUSE_PRESSED, 20, 20, MouseEvent.BUTTON1_DOWN_MASK)
        awaitIdle()
        window.sendMouseEvent(MouseEvent.MOUSE_RELEASED, 20, 20)
        awaitIdle()
        assertThat(popupButtonClicked).isEqualTo(true)

        checkAccessibleOnPoint(window, 20, 20) {
            assertThat(accessibleName).isEqualTo("Accessible popup button")
        }
    }

    private inline fun checkAccessibleOnPoint(
        window: ComposeWindow,
        x: Int,
        y: Int,
        check: AccessibleContext.() -> Unit
    ) {
        val context = SwingUtilities.getAccessibleAt(window.findSkiaLayer().canvas, Point(x, y)).accessibleContext
        check(context)
    }

    private fun ComposeWindow.findSkiaLayer(): SkiaLayer {
        // TODO: This function shouldn't use implementation details
        val pane = contentPane.components.filterIsInstance<JLayeredPane>().first()
        return pane.components.filterIsInstance<SkiaLayer>().first()
    }
}