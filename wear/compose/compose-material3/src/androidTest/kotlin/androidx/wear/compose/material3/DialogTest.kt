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

package androidx.wear.compose.material3

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class DialogTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun supports_testtag() {
        rule.setContentWithTheme {
            Dialog(visible = true, modifier = Modifier.testTag(TEST_TAG), onDismissRequest = {}) {}
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun dialogContent_composedOnce() {
        var recomposeCounter = 0
        rule.setContentWithTheme {
            var visible by remember { mutableStateOf(false) }
            Button(modifier = Modifier.testTag(SHOW_BUTTON_TAG), onClick = { visible = true }) {}

            Dialog(
                visible = visible,
                modifier = Modifier.testTag(TEST_TAG),
                onDismissRequest = {}
            ) {
                recomposeCounter++
            }
        }
        rule.onNodeWithTag(SHOW_BUTTON_TAG).performClick()
        rule.waitForIdle()
        Assert.assertEquals(1, recomposeCounter)
    }

    @Test
    fun displays_content() {
        rule.setContentWithTheme {
            Dialog(visible = true, onDismissRequest = {}) {
                Text("Text", modifier = Modifier.testTag(TEST_TAG))
            }
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun supports_swipeToDismiss() {
        var dismissCounter = 0
        rule.setContentWithTheme {
            var visible by remember { mutableStateOf(true) }
            Dialog(
                visible = visible,
                modifier = Modifier.testTag(TEST_TAG),
                onDismissRequest = {
                    visible = false
                    dismissCounter++
                }
            ) {}
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput({ swipeRight() })
        rule.onNodeWithTag(TEST_TAG).assertDoesNotExist()
        Assert.assertEquals(1, dismissCounter)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun onDismissRequest_not_called_when_hidden() {
        val visible = mutableStateOf(true)
        var dismissCounter = 0
        rule.setContentWithTheme {
            Dialog(
                modifier = Modifier.testTag(TEST_TAG),
                onDismissRequest = { dismissCounter++ },
                visible = visible.value
            ) {}
        }
        rule.waitForIdle()
        visible.value = false
        rule.waitUntilDoesNotExist(hasTestTag(TEST_TAG))
        Assert.assertEquals(0, dismissCounter)
    }

    @Test
    fun hides_dialog_when_visible_false() {
        rule.setContentWithTheme {
            Dialog(modifier = Modifier.testTag(TEST_TAG), onDismissRequest = {}, visible = false) {}
        }
        rule.onNodeWithTag(TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun shrink_background_when_dialog_is_shown() {
        var scaffoldState = ScaffoldState(appScaffoldPresent = false)
        rule.setContentWithTheme {
            CompositionLocalProvider(
                LocalScaffoldState provides scaffoldState,
            ) {
                var visible by remember { mutableStateOf(false) }
                Button(
                    modifier = Modifier.testTag(SHOW_BUTTON_TAG),
                    onClick = { visible = true }
                ) {}

                Dialog(
                    visible = visible,
                    modifier = Modifier.testTag(TEST_TAG),
                    onDismissRequest = {}
                ) {}
            }
        }
        rule.onNodeWithTag(SHOW_BUTTON_TAG).performClick()
        rule.waitForIdle()
        assert(scaffoldState.parentScale.floatValue < 1f)
    }

    @Test
    fun expand_background_when_dialog_is_hidden() {
        var scaffoldState = ScaffoldState(appScaffoldPresent = false)
        rule.setContentWithTheme {
            CompositionLocalProvider(
                LocalScaffoldState provides scaffoldState,
            ) {
                var visible by remember { mutableStateOf(true) }
                Button(
                    modifier = Modifier.testTag(SHOW_BUTTON_TAG),
                    onClick = { visible = false }
                ) {}

                Dialog(
                    visible = visible,
                    modifier = Modifier.testTag(TEST_TAG),
                    onDismissRequest = {}
                ) {}
            }
        }
        rule.onNodeWithTag(SHOW_BUTTON_TAG).performClick()
        rule.waitForIdle()
        Assert.assertEquals(scaffoldState.parentScale.floatValue, 1f, 0.01f)
    }

    @Test
    fun expand_background_when_dialog_is_removed() {
        var scaffoldState = ScaffoldState(appScaffoldPresent = false)
        rule.setContentWithTheme {
            CompositionLocalProvider(
                LocalScaffoldState provides scaffoldState,
            ) {
                var visible by remember { mutableStateOf(true) }
                Button(
                    modifier = Modifier.testTag(SHOW_BUTTON_TAG),
                    onClick = { visible = false }
                ) {}

                if (visible) {
                    Dialog(
                        visible = visible,
                        modifier = Modifier.testTag(TEST_TAG),
                        onDismissRequest = {}
                    ) {}
                }
            }
        }
        rule.onNodeWithTag(SHOW_BUTTON_TAG).performClick()
        rule.waitForIdle()
        Assert.assertEquals(scaffoldState.parentScale.floatValue, 1f, 0.01f)
    }

    @Test
    fun hidden_dialog_is_not_added_to_fullScreenContent() {
        var scaffoldState = ScaffoldState(appScaffoldPresent = true)
        rule.setContentWithTheme {
            CompositionLocalProvider(
                LocalScaffoldState provides scaffoldState,
            ) {
                var visible by remember { mutableStateOf(false) }
                Button(
                    modifier = Modifier.testTag(SHOW_BUTTON_TAG),
                    onClick = { visible = false }
                ) {}

                if (visible) {
                    Dialog(
                        visible = visible,
                        modifier = Modifier.testTag(TEST_TAG),
                        onDismissRequest = {}
                    ) {}
                }
            }
        }
        rule.waitForIdle()
        Assert.assertTrue(scaffoldState.fullScreenContent.contentItems.isEmpty())
    }

    @Test
    fun single_dialog_added_to_fullScreenContent() {
        var scaffoldState = ScaffoldState(appScaffoldPresent = true)
        rule.setContentWithTheme {
            CompositionLocalProvider(
                LocalScaffoldState provides scaffoldState,
            ) {
                var visible by remember { mutableStateOf(false) }
                Button(
                    modifier = Modifier.testTag(SHOW_BUTTON_TAG),
                    onClick = { visible = true }
                ) {}

                if (visible) {
                    Dialog(
                        visible = visible,
                        modifier = Modifier.testTag(TEST_TAG),
                        onDismissRequest = {}
                    ) {}
                }
            }
        }
        rule.onNodeWithTag(SHOW_BUTTON_TAG).performClick()
        rule.waitForIdle()
        Assert.assertTrue(scaffoldState.fullScreenContent.contentItems.isNotEmpty())
    }

    @Test
    fun dialog_removed_from_fullScreenContent() {
        var scaffoldState = ScaffoldState(appScaffoldPresent = true)
        rule.setContentWithTheme {
            CompositionLocalProvider(
                LocalScaffoldState provides scaffoldState,
            ) {
                var visible by remember { mutableStateOf(true) }
                Button(
                    modifier = Modifier.testTag(HIDE_BUTTON_TAG),
                    onClick = { visible = false }
                ) {}

                if (visible) {
                    Dialog(
                        visible = visible,
                        modifier = Modifier.testTag(TEST_TAG),
                        onDismissRequest = {}
                    ) {}
                }
            }
        }
        rule.onNodeWithTag(HIDE_BUTTON_TAG).performClick()
        rule.waitForIdle()
        Assert.assertTrue(scaffoldState.fullScreenContent.contentItems.isEmpty())
    }

    @Test
    fun multiple_dialogs_added_to_fullScreenContent() {
        var scaffoldState = ScaffoldState(appScaffoldPresent = true)

        rule.setContentWithTheme {
            AppScaffold {
                var visible1 by remember { mutableStateOf(false) }
                var visible2 by remember { mutableStateOf(false) }
                scaffoldState = LocalScaffoldState.current
                Button(
                    modifier = Modifier.testTag(SHOW_BUTTON_TAG),
                    onClick = { visible1 = true }
                ) {}

                if (visible1) {
                    Dialog(
                        visible = true,
                        modifier = Modifier.testTag(TEST_TAG),
                        onDismissRequest = {}
                    ) {
                        Button(
                            modifier = Modifier.testTag(SHOW_SECOND_DIALOG_TAG),
                            onClick = { visible2 = true }
                        ) {}
                    }
                }
                if (visible2) {
                    Dialog(
                        visible = visible2,
                        modifier = Modifier.testTag(TEST_TAG),
                        onDismissRequest = {}
                    ) {
                        Button(
                            modifier = Modifier.testTag(HIDE_SECOND_DIALOG_TAG),
                            onClick = { visible2 = false }
                        ) {}
                    }
                }
            }
        }
        rule.onNodeWithTag(SHOW_BUTTON_TAG).performClick()
        rule.waitForIdle()
        Assert.assertEquals(1, scaffoldState.fullScreenContent.contentItems.size)
        val firstDialog = scaffoldState.fullScreenContent.contentItems.first()

        rule.onNodeWithTag(SHOW_SECOND_DIALOG_TAG).performClick()
        rule.waitForIdle()
        Assert.assertEquals(2, scaffoldState.fullScreenContent.contentItems.size)
        rule.onNodeWithTag(HIDE_SECOND_DIALOG_TAG).performClick()
        rule.waitForIdle()
        Assert.assertEquals(1, scaffoldState.fullScreenContent.contentItems.size)
        // Check that the first dialog is still in the contentItems
        Assert.assertTrue(scaffoldState.fullScreenContent.contentItems.contains(firstDialog))
    }

    @Test
    fun dialog_opened_from_dialog_and_replaced() {
        var scaffoldState = ScaffoldState(appScaffoldPresent = true)

        rule.setContentWithTheme {
            AppScaffold {
                var visible1 by remember { mutableStateOf(false) }
                var visible2 by remember { mutableStateOf(false) }
                scaffoldState = LocalScaffoldState.current
                Button(
                    modifier = Modifier.testTag(SHOW_BUTTON_TAG),
                    onClick = { visible1 = true }
                ) {}

                if (visible1) {
                    Dialog(
                        visible = true,
                        modifier = Modifier.testTag(TEST_TAG),
                        onDismissRequest = {}
                    ) {
                        Button(
                            modifier = Modifier.testTag(SHOW_SECOND_DIALOG_TAG),
                            onClick = {
                                visible1 = false
                                visible2 = true
                            }
                        ) {}
                    }
                }
                if (visible2) {
                    Dialog(
                        visible = visible2,
                        modifier = Modifier.testTag(TEST_TAG),
                        onDismissRequest = {}
                    ) {}
                }
            }
        }
        rule.onNodeWithTag(SHOW_BUTTON_TAG).performClick()
        rule.waitForIdle()
        Assert.assertEquals(1, scaffoldState.fullScreenContent.contentItems.size)
        val firstDialog = scaffoldState.fullScreenContent.contentItems.first()

        rule.onNodeWithTag(SHOW_SECOND_DIALOG_TAG).performClick()
        rule.waitForIdle()
        Assert.assertEquals(1, scaffoldState.fullScreenContent.contentItems.size)
        // Check that the first dialog was removed from contentItems
        Assert.assertFalse(scaffoldState.fullScreenContent.contentItems.contains(firstDialog))
    }
}

private const val SHOW_BUTTON_TAG = "show-button"
private const val HIDE_BUTTON_TAG = "hide-button"
private const val SHOW_SECOND_DIALOG_TAG = "show-second-dialog"
private const val HIDE_SECOND_DIALOG_TAG = "hide-second-dialog"
