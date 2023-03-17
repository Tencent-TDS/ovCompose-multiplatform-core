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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.window.WindowTestScope
import androidx.compose.ui.window.runApplicationTest
import java.awt.Point
import java.awt.Window
import java.awt.event.MouseEvent
import javax.accessibility.AccessibleComponent
import javax.accessibility.AccessibleContext
import kotlin.test.assertNotEquals
import org.junit.Ignore
import org.junit.Test

class ApplicationAccessibilityTest {
    @Test
    fun `popup text is accessible on hover`() = runApplicationTest {
        lateinit var window: ComposeWindow
        val showPopup = mutableStateOf(false)

        launchTestApplication {
            Window(onCloseRequest = {}) {
                window = this.window
                Button(
                    modifier = Modifier.size(100.dp),
                    onClick = {
                        showPopup.value = true
                    }
                ) {
                    Text("Accessible button")
                }

                if (showPopup.value) {
                    // show popup on top of the accessible button
                    val position = object : PopupPositionProvider {
                        override fun calculatePosition(
                            anchorBounds: IntRect,
                            windowSize: IntSize,
                            layoutDirection: LayoutDirection,
                            popupContentSize: IntSize
                        ): IntOffset = IntOffset(0, 25)
                    }
                    Popup(position, focusable = false) {
                        Button(
                            onClick = {},
                            modifier = Modifier.size(100.dp)
                        ) {
                            Text("Accessible popup button")
                        }
                    }
                }
            }
        }
        awaitIdle()

        checkAccessibleOnPoint(window, 20, 20) {
            assertThat(accessibleName).isEqualTo("Accessible button")
        }

        // open popup
        clickOnPoint(window, 20, 20)
        assertThat(showPopup.value).isEqualTo(true)

        checkAccessibleOnPoint(window, 5, 5) {
            assertThat(accessibleName).isEqualTo("Accessible button")
        }

        checkAccessibleOnPoint(window, 5, 50) {
            assertThat(accessibleName).isEqualTo("Accessible popup button")
        }
    }

    @Test
    fun `accessibility of multiple components`() = runApplicationTest {
        lateinit var window: ComposeWindow

        launchTestApplication {
            Window(onCloseRequest = {}) {
                window = this.window
                Column {
                    Button(
                        onClick = {},
                        modifier = Modifier.size(20.dp),
                    ) {
                        Text("Accessible button 1")
                    }
                    Button(
                        onClick = {},
                        modifier = Modifier.size(20.dp),
                    ) {
                        Text("Accessible button 2")
                    }
                }
            }
        }
        awaitIdle()

        checkAccessibleOnPoint(window, 10, 10) {
            assertThat(accessibleName).isEqualTo("Accessible button 1")
        }

        checkAccessibleOnPoint(window, 5, 22) {
            assertThat(accessibleName).isEqualTo("Accessible button 2")
        }
    }

    // TODO: component under popup shouldn't be read by screen reader
    //  but current implementation does it
    //  (see ComposeSceneAccessible.ComposeSceneAccessibleContext.getAccessibleAt)
    @Ignore
    @Test
    fun `hover popup when there is a component under it`() = runApplicationTest {
        lateinit var window: ComposeWindow

        launchTestApplication {
            Window(onCloseRequest = {}) {
                window = this.window
                Column {
                    Button(
                        onClick = {},
                        modifier = Modifier.size(20.dp),
                    ) {
                        Text("button under popup")
                    }
                    val popupPosition = object : PopupPositionProvider {
                        override fun calculatePosition(
                            anchorBounds: IntRect,
                            windowSize: IntSize,
                            layoutDirection: LayoutDirection,
                            popupContentSize: IntSize
                        ): IntOffset = IntOffset.Zero
                    }
                    Popup(popupPosition) {
                        Column {
                            Spacer(Modifier.height(30.dp))
                            Button(
                                onClick = {},
                                modifier = Modifier.size(20.dp)
                            ) {
                                Text("popup button")
                            }
                        }
                    }
                }
            }
        }
        awaitIdle()

        checkAccessibleOnPoint(window, 5, 32) {
            assertThat(accessibleName).isEqualTo("popup button")
        }

        checkAccessibleOnPoint(window, 5, 5) {
            assertNotEquals("button under popup", accessibleName)
        }
    }

    private inline fun checkAccessibleOnPoint(
        window: ComposeWindow,
        x: Int,
        y: Int,
        check: AccessibleContext.() -> Unit
    ) {
        val sceneAccessible = window.delegate.layer.sceneAccessible
        val accessibleComponent = sceneAccessible.accessibleContext as AccessibleComponent
        val accessibleComponentAtPoint = accessibleComponent.getAccessibleAt(Point(x, y))

        check(accessibleComponentAtPoint.accessibleContext)
    }

    private suspend fun WindowTestScope.clickOnPoint(window: Window, x: Int, y: Int) {
        window.sendMouseEvent(MouseEvent.MOUSE_PRESSED, x, y, MouseEvent.BUTTON1_DOWN_MASK)
        window.sendMouseEvent(MouseEvent.MOUSE_RELEASED, x, y)
        awaitIdle()
    }
}