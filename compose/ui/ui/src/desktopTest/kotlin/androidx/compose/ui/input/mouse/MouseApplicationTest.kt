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

package androidx.compose.ui.input.mouse

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.sendMouseEvent
import androidx.compose.ui.sendMouseWheelEvent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.window.runApplicationTest
import com.google.common.truth.Truth.assertThat
import java.awt.event.MouseEvent
import javax.swing.JPanel
import org.junit.Test

class MouseApplicationTest {

    @Test
    fun `interop in lazy list`() = runApplicationTest {
        lateinit var window: ComposeWindow

        val currentlyVisible = mutableSetOf<Int>()
        launchTestApplication {
            Window(
                onCloseRequest = ::exitApplication,
                state = rememberWindowState(width = 250.dp, height = 250.dp)
            ) {
                window = this.window

                LazyColumn(Modifier.fillMaxSize()) {
                    items(1000) { index ->
                        SwingPanel(
                            factory = {
                                object : JPanel() {
                                    override fun addNotify() {
                                        super.addNotify()
                                        currentlyVisible += index
                                    }

                                    override fun removeNotify() {
                                        super.removeNotify()
                                        currentlyVisible -= index
                                    }
                                }
                            },
                            modifier = Modifier.size(100.dp)
                        )
                    }
                }
            }
        }

        awaitIdle()
        assertThat(currentlyVisible).containsExactly(0, 1, 2)

        window.sendMouseEvent(MouseEvent.MOUSE_ENTERED, 5, 5)
        window.sendMouseWheelEvent(5, 5, wheelRotation = 1000.0)

        awaitIdle()
        assertThat(currentlyVisible).containsExactly(100, 101, 102)

        window.sendMouseWheelEvent(5, 5, wheelRotation = -1000.0)

        awaitIdle()
        assertThat(currentlyVisible).containsExactly(0, 1, 2)
    }
}
