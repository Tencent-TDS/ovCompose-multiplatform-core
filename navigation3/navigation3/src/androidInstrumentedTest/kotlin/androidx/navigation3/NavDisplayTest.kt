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
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.window.Dialog
import androidx.kruth.assertThat
import androidx.savedstate.SavedStateRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertWithMessage
import kotlin.test.Test
import kotlin.test.assertFailsWith
import org.junit.Rule
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class NavDisplayTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testContentShown() {
        composeTestRule.setContent {
            NavDisplay(backstack = mutableStateListOf(first)) { NavEntry(first) { Text(first) } }
        }

        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isTrue()
    }

    @Test
    fun testContentChanged() {
        lateinit var backstack: MutableList<Any>
        composeTestRule.setContent {
            backstack = remember { mutableStateListOf(first) }
            NavDisplay(backstack = backstack) {
                when (it) {
                    first -> NavEntry(first) { Text(first) }
                    second -> NavEntry(second) { Text(second) }
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
            var showDialog = remember { mutableStateOf(false) }
            backstack = remember { mutableStateListOf(first) }
            NavDisplay(backstack = backstack) {
                when (it) {
                    first ->
                        NavEntry(first) {
                            Button(onClick = { showDialog.value = true }) { Text(first) }
                        }
                    else -> error("Invalid key passed")
                }
            }
            if (showDialog.value) {
                Dialog(onDismissRequest = {}) { Text(second) }
            }
        }

        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isTrue()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(first).performClick()

        composeTestRule.waitForIdle()
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
            NavDisplay(backstack = backstack) {
                when (it) {
                    first -> NavEntry(first) { Text(first) }
                    second -> NavEntry(second) { Text(second) }
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

    @Test
    fun testStateOfInactiveContentIsRestoredWhenWeGoBackToIt() {
        var increment = 0
        var numberOnScreen1 = -1
        lateinit var backstack: MutableList<Any>
        composeTestRule.setContent {
            backstack = remember { mutableStateListOf(first) }
            NavDisplay(backstack = backstack) {
                when (it) {
                    first -> NavEntry(first) { numberOnScreen1 = rememberSaveable { increment++ } }
                    second -> NavEntry(second) {}
                    else -> error("Invalid key passed")
                }
            }
        }

        composeTestRule.runOnIdle {
            assertWithMessage("Initial number should be 0").that(numberOnScreen1).isEqualTo(0)
            numberOnScreen1 = -1
            assertWithMessage("The number should be -1").that(numberOnScreen1).isEqualTo(-1)
            backstack.add(second)
        }

        composeTestRule.runOnIdle { backstack.removeAt(backstack.size - 1) }

        composeTestRule.runOnIdle {
            assertWithMessage("The number should be restored").that(numberOnScreen1).isEqualTo(0)
        }
    }

    @Test
    fun testIndividualSavedStateRegistries() {
        lateinit var mainRegistry: SavedStateRegistry
        lateinit var registry1: SavedStateRegistry
        lateinit var registry2: SavedStateRegistry
        lateinit var backstack: MutableList<Any>
        composeTestRule.setContent {
            mainRegistry = LocalSavedStateRegistryOwner.current.savedStateRegistry
            backstack = remember { mutableStateListOf(first) }
            NavDisplay(backstack = backstack, localProviders = listOf(SavedStateNavLocalProvider)) {
                when (it) {
                    first ->
                        NavEntry(first) {
                            registry1 = LocalSavedStateRegistryOwner.current.savedStateRegistry
                        }
                    second ->
                        NavEntry(second) {
                            registry2 = LocalSavedStateRegistryOwner.current.savedStateRegistry
                        }
                    else -> error("Invalid key passed")
                }
            }
        }

        composeTestRule.runOnIdle {
            assertWithMessage("Registries should be unique")
                .that(mainRegistry)
                .isNotEqualTo(registry1)
            backstack.add(second)
        }

        composeTestRule.runOnIdle {
            assertWithMessage("Registries should be unique").that(registry2).isNotEqualTo(registry1)
        }
    }

    @Test
    fun swappingOutMultipleBackStacks() {
        lateinit var backStack1: MutableList<String>
        lateinit var backStack2: MutableList<String>
        lateinit var backStack3: MutableList<String>
        lateinit var state: MutableState<Int>

        composeTestRule.setContent {
            backStack1 = remember { mutableStateListOf(first) }
            backStack2 = remember { mutableStateListOf(second) }
            backStack3 = remember { mutableStateListOf(third) }
            state = remember { mutableStateOf(1) }
            NavDisplay(
                backstack =
                    when (state.value) {
                        1 -> backStack1
                        2 -> backStack2
                        else -> backStack3
                    },
                entryProvider =
                    entryProvider {
                        entry(first) { Text(first) }
                        entry(second) { Text(second) }
                        entry(third) { Text(third) }
                        entry(forth) { Text(forth) }
                    }
            )
        }

        composeTestRule.waitForIdle()
        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isTrue()

        composeTestRule.runOnIdle { state.value = 2 }
        composeTestRule.waitForIdle()
        assertThat(composeTestRule.onNodeWithText(second).isDisplayed()).isTrue()

        composeTestRule.runOnIdle { state.value = 3 }
        composeTestRule.waitForIdle()
        assertThat(composeTestRule.onNodeWithText(third).isDisplayed()).isTrue()

        composeTestRule.runOnIdle { backStack3.add(forth) }

        assertThat(backStack3).containsExactly(third, forth)
        assertThat(composeTestRule.onNodeWithText(forth).isDisplayed()).isTrue()
    }

    @Test
    fun testInitEmptyBackstackThrows() {
        lateinit var backstack: MutableList<Any>
        val fail =
            assertFailsWith<IllegalArgumentException> {
                composeTestRule.setContent {
                    backstack = remember { mutableStateListOf() }
                    NavDisplay(backstack = backstack) { NavEntry(first) {} }
                }
            }
        assertThat(fail.message).isEqualTo("NavDisplay backstack cannot be empty")
    }

    @Test
    fun testPopToEmptyBackstackThrows() {
        lateinit var backstack: MutableList<Any>
        composeTestRule.setContent {
            backstack = remember { mutableStateListOf(first) }
            NavDisplay(backstack = backstack) {
                when (it) {
                    first -> NavEntry(first) { Text(first) }
                    second -> NavEntry(second) { Text(second) }
                    else -> error("Invalid key passed")
                }
            }
        }

        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isTrue()

        val fail =
            assertFailsWith<IllegalArgumentException> {
                composeTestRule.runOnIdle { backstack.clear() }
                composeTestRule.waitForIdle()
            }
        assertThat(fail.message).isEqualTo("NavDisplay backstack cannot be empty")
    }
}

private const val first = "first"
private const val second = "second"
private const val third = "third"
private const val forth = "forth"
