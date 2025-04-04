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

package androidx.compose.foundation.text

import androidx.compose.foundation.assertThat
import androidx.compose.foundation.isEqualTo
import androidx.compose.foundation.isFalse
import androidx.compose.foundation.isNotNull
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.CommitTextCommand
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.TextEditorState
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.test.Test
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class LegacyPlatformTextInputServiceAdapterTest {
    @OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
    @Test
    fun testRequestValuesUpdate() = runComposeUiTest {
        var value: TextFieldValue? = null
        var state: TextEditorState? = null
        var imeOptions: ImeOptions? = null
        var outputValue: TextFieldValue? = null
        var textLayoutResult: TextLayoutResult? = null
        var focusedRectInRoot: Rect? = null
        var textFieldRectInRoot: Rect? = null
        var textClippingRectInRoot: Rect? = null

        setContent {
            InterceptPlatformTextInput({ request, nextHandler ->
                coroutineScope {
                    launch {
                        snapshotFlow { request.value() }.collect { value = it }
                    }
                    launch {
                        snapshotFlow { request.state }.collect { state = it }
                    }
                    launch {
                        snapshotFlow { request.imeOptions }.collect { imeOptions = it }
                    }
                    launch {
                        request.outputValue.collect { outputValue = it }
                    }
                    launch {
                        request.textLayoutResult.collect { textLayoutResult = it }
                    }
                    launch {
                        request.focusedRectInRoot.collect { focusedRectInRoot = it }
                    }
                    launch {
                        request.textFieldRectInRoot.collect { textFieldRectInRoot = it }
                    }
                    launch {
                        request.textClippingRectInRoot.collect { textClippingRectInRoot = it }
                    }
                }
                awaitCancellation()
            }) {
                var text by remember { mutableStateOf("") }
                BasicTextField(
                    modifier = Modifier.testTag("input"),
                    value = text,
                    onValueChange = {
                        text = it
                    }
                )
            }
        }

        onNodeWithTag("input").performTextInput("abc")

        waitForIdle()

        assertThat(value?.text).isEqualTo("abc")
        assertThat(state?.length).isEqualTo(3)
        assertThat(state?.selection).isEqualTo(TextRange(3, 3))
        assertThat(state?.let { it.substring(startIndex = 0, it.length) }).isEqualTo("abc")
        assertThat(imeOptions).isNotNull()
        assertThat(outputValue?.text).isEqualTo("abc")
        assertThat(outputValue?.selection).isEqualTo(TextRange(3, 3))
        assertThat(textLayoutResult?.layoutInput?.text?.text).isEqualTo("abc")
        assertThat(focusedRectInRoot?.isEmpty).isFalse()
        assertThat(textFieldRectInRoot?.isEmpty).isFalse()
        assertThat(textClippingRectInRoot?.isEmpty).isFalse()
    }

    @OptIn(ExperimentalComposeUiApi::class, ExperimentalTestApi::class)
    @Test
    fun testTextEditingWithEditText() = runComposeUiTest {
        var text by mutableStateOf(TextFieldValue("abc"))
        setContent {
            InterceptPlatformTextInput({ request, nextHandler ->
                coroutineScope {
                    request.editText {
                        commitText("abc", 1)
                    }
                    request.onEditCommand(
                        listOf(CommitTextCommand("def", 1))
                    )
                    request.editText {
                        setComposingText("gh", 1)
                    }
                }
                awaitCancellation()
            }) {
                BasicTextField(
                    modifier = Modifier.testTag("input"),
                    value = text,
                    onValueChange = { text = it }
                )
            }
        }
        onNodeWithTag("input").requestFocus()

        waitForIdle()

        assertThat(text.text).isEqualTo("abcdefgh")
        assertThat(text.composition).isEqualTo("abcdef")
    }
}
