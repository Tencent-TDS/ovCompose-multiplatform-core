/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.window.window

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowTestScope
import androidx.compose.ui.window.runApplicationTest
import com.google.common.truth.Truth.assertThat
import org.junit.experimental.theories.DataPoint

open class BaseWindowTextFieldTest {
    internal interface TextFieldTestScope {
        val window: ComposeWindow
        val text: String

        @Composable
        fun TextField()

        suspend fun awaitIdle()

        suspend fun assertStateEquals(actual: String, selection: TextRange, composition: TextRange?)
    }

    internal fun runTextFieldTest(
        textFieldKind: TextFieldKind,
        name: String,
        body: suspend TextFieldTestScope.() -> Unit
    ) = runApplicationTest(
        hasAnimations = true,
        animationsDelayMillis = 100
    ) {
        var scope: TextFieldTestScope? = null
        launchTestApplication {
            Window(onCloseRequest = ::exitApplication) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (scope == null) {
                        scope = textFieldKind.createScope(this@runApplicationTest, window)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$name ($scope)")
                        Box(Modifier.border(1.dp, Color.Black).padding(8.dp)) {
                            scope!!.TextField()
                        }
                    }
                }
            }
        }

        awaitIdle()
        scope!!.body()
    }

    internal fun interface TextFieldKind {
        fun createScope(windowTestScope: WindowTestScope, window: ComposeWindow): TextFieldTestScope
    }

    companion object {
        @JvmField
        @DataPoint
        internal val TextField1: TextFieldKind = TextFieldKind { windowTestScope, window ->
            object : TextFieldTestScope {
                override val window: ComposeWindow
                    get() = window

                private var textFieldValue by mutableStateOf(TextFieldValue())

                override val text: String
                    get() = textFieldValue.text

                override suspend fun awaitIdle() {
                    windowTestScope.awaitIdle()
                }

                @Composable
                override fun TextField() {
                    val focusRequester = FocusRequester()
                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = { textFieldValue = it },
                        modifier = Modifier.focusRequester(focusRequester)
                    )

                    LaunchedEffect(focusRequester) {
                        focusRequester.requestFocus()
                    }
                }

                override suspend fun assertStateEquals(
                    actual: String,
                    selection: TextRange,
                    composition: TextRange?
                ) {
                    windowTestScope.awaitIdle()
                    assertThat(textFieldValue.text).isEqualTo(actual)
                    assertThat(textFieldValue.selection).isEqualTo(selection)
                    assertThat(textFieldValue.composition).isEqualTo(composition)
                }

                override fun toString() = "TextField1"
            }
        }

        @JvmField
        @DataPoint
        internal val TextField2: TextFieldKind = TextFieldKind { windowTestScope, window ->
            object : TextFieldTestScope {
                override val window: ComposeWindow
                    get() = window

                private val textFieldState = TextFieldState()

                override val text: String
                    get() = textFieldState.text.toString()

                override suspend fun awaitIdle() {
                    windowTestScope.awaitIdle()
                }

                @Composable
                override fun TextField() {
                    val focusRequester = FocusRequester()
                    BasicTextField(
                        state = textFieldState,
                        modifier = Modifier.focusRequester(focusRequester)
                    )

                    LaunchedEffect(focusRequester) {
                        focusRequester.requestFocus()
                    }
                }

                override suspend fun assertStateEquals(
                    actual: String,
                    selection: TextRange,
                    composition: TextRange?
                ) {
                    windowTestScope.awaitIdle()
                    assertThat(textFieldState.text.toString()).isEqualTo(actual)
                    assertThat(textFieldState.selection).isEqualTo(selection)
                    assertThat(textFieldState.composition).isEqualTo(composition)

                }

                override fun toString() = "TextField2"
            }
        }
    }
}