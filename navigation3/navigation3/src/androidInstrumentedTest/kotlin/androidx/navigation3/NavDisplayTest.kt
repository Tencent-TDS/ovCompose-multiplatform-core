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

package androidx.navigation3

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.kruth.assertThat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kotlin.test.Test
import org.junit.Rule
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class NavDisplayTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testContentShown() {
        composeTestRule.setContent {
            val manager = rememberNavWrapperManager(emptyList())
            NavDisplay(backstack = mutableStateListOf(first), wrapperManager = manager) {
                Record(first) { Text(first) }
            }
        }

        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isTrue()
    }

    @Test
    fun testContentChanged() {
        lateinit var backstack: MutableList<Any>
        composeTestRule.setContent {
            backstack = remember { mutableStateListOf(first) }
            val manager = rememberNavWrapperManager(emptyList())
            NavDisplay(backstack = backstack, wrapperManager = manager) {
                when (it) {
                    first -> Record(first) { Text(first) }
                    second -> Record(second) { Text(second) }
                    else -> error("Invalid key passed")
                }
            }
        }

        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isTrue()

        composeTestRule.runOnIdle { backstack.add(second) }

        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isFalse()
        assertThat(composeTestRule.onNodeWithText(second).isDisplayed()).isTrue()
    }

    @Test
    fun testDialog() {
        lateinit var backstack: MutableList<Any>
        composeTestRule.setContent {
            backstack = remember { mutableStateListOf(first) }
            val manager = rememberNavWrapperManager(emptyList())
            NavDisplay(
                backstack = backstack,
                wrapperManager = manager,
                onBack = {
                    // removeLast requires API 35
                    backstack.removeAt(backstack.size - 1)
                }
            ) {
                when (it) {
                    first -> Record(first) { Text(first) }
                    second -> Record(second, NavDisplay.isDialog(true)) { Text(second) }
                    else -> error("Invalid key passed")
                }
            }
        }

        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isTrue()

        composeTestRule.runOnIdle { backstack.add(second) }

        // Both first and second should be showing if we are on a dialog.
        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText(second).isDisplayed()).isTrue()
    }

    @Test
    fun testOnBack() {
        lateinit var onBackDispatcher: OnBackPressedDispatcher
        lateinit var backstack: MutableList<Any>
        composeTestRule.setContent {
            onBackDispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            backstack = remember { mutableStateListOf(first) }
            val manager = rememberNavWrapperManager(emptyList())
            NavDisplay(
                backstack = backstack,
                wrapperManager = manager,
                onBack = {
                    // removeLast requires API 35
                    backstack.removeAt(backstack.size - 1)
                }
            ) {
                when (it) {
                    first -> Record(first) { Text(first) }
                    second -> Record(second) { Text(second) }
                    else -> error("Invalid key passed")
                }
            }
        }

        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isTrue()

        composeTestRule.runOnIdle { backstack.add(second) }

        assertThat(composeTestRule.onNodeWithText(second).isDisplayed()).isTrue()

        composeTestRule.runOnIdle { onBackDispatcher.onBackPressed() }

        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isTrue()
    }
}

private const val first = "first"
private const val second = "second"
