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

package androidx.compose.foundation.text.selection

import androidx.compose.foundation.assertThat
import androidx.compose.foundation.isEqualTo
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyMapping
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.test.Test

internal abstract class CommonSelectionTests(
    val keyboardActions: KeyboardActions,
    private val keyMapping: KeyMapping
) : KeyboardActions by keyboardActions {

    abstract fun setPlatformDefaultKeyMapping(value: KeyMapping)

    @OptIn(ExperimentalTestApi::class)
    private fun textFieldSemanticInteraction(
        initialValue: String = "",
        runAction: ComposeUiTest.(node: SemanticsNodeInteraction, state: MutableState<TextFieldValue>) -> Unit
    ) =
        runComposeUiTest {
            setPlatformDefaultKeyMapping(keyMapping)
            val state = mutableStateOf(TextFieldValue(initialValue))

            waitForIdle()

            setContent {
                BasicTextField(
                    value = state.value,
                    onValueChange = { state.value = it },
                    modifier = Modifier.testTag("textField")
                )
            }

            onNodeWithTag("textField").apply {
                performMouseInput {
                    click(Offset(0f, 0f))
                }

                assertIsFocused()

                assertThat(state.value.selection).isEqualTo(TextRange(0, 0))

                runAction(this, state)
            }
        }


    @OptIn(ExperimentalTestApi::class)
    @Test
    fun selectLineStart() {
        textFieldSemanticInteraction("line 1\nline 2\nline 3\nline 4\nline 5") { node, state ->
            node.performKeyInput {
                pressKey(Key.DirectionRight)
                pressKey(Key.DirectionDown)
            }
            assertThat(state.value.selection).isEqualTo(TextRange(8, 8))

            node.performKeyInput { this.selectLineStart() }

            assertThat(state.value.selection).isEqualTo(TextRange(8, 7))
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun selectTextStart() {
        textFieldSemanticInteraction("line 1\nline 2\nline 3\nline 4\nline 5") { node, state ->
            node.performKeyInput {
                pressKey(Key.DirectionRight)
                pressKey(Key.DirectionDown)
            }

            assertThat(state.value.selection).isEqualTo(TextRange(8, 8))

            node.performKeyInput { this.selectTextStart() }

            assertThat(state.value.selection).isEqualTo(TextRange(8, 0))
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun selectTextEnd() {
        textFieldSemanticInteraction("line 1\nline 2\nline 3\nline 4\nline 5") { node, state ->
            node.performKeyInput {
                pressKey(Key.DirectionRight)
                pressKey(Key.DirectionDown)
            }
            assertThat(state.value.selection).isEqualTo(TextRange(8, 8))

            node.performKeyInput { this.selectTextEnd() }
            assertThat(state.value.selection).isEqualTo(TextRange(8, 34))
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun selectLineEnd() {
        textFieldSemanticInteraction("line 1\nline 2\nline 3\nline 4\nline 5") { node, state ->
            node.performKeyInput {
                pressKey(Key.DirectionRight)
                pressKey(Key.DirectionDown)
            }

            assertThat(state.value.selection).isEqualTo(TextRange(8, 8))

            node.performKeyInput { this.selectLineEnd() }
            assertThat(state.value.selection).isEqualTo(TextRange(8, 13))
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun deleteAll() {
        textFieldSemanticInteraction("") { node, state ->
            node.performKeyInput { this.deleteAll() }
            assertThat(state.value.text).isEqualTo("")
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun selectAll() {
        textFieldSemanticInteraction("Select this text") { node, state ->
            node.performKeyInput { this.selectAll() }
            assertThat(state.value.selection).isEqualTo(TextRange(0, 16))

            node.performKeyInput { keyDown(Key.Delete) }
            assertThat(state.value.selection).isEqualTo(TextRange(0, 0))
            assertThat(state.value.text).isEqualTo("")
        }
    }
}
