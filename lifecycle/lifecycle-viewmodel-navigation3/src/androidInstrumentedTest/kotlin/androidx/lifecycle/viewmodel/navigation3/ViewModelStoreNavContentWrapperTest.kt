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

package androidx.lifecycle.viewmodel.navigation3

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.kruth.assertThat
import androidx.kruth.assertWithMessage
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.NavDisplay
import androidx.navigation3.NavRecord
import androidx.navigation3.SavedStateNavLocalProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kotlin.test.Test
import org.junit.Rule
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class ViewModelStoreNavLocalProviderTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testViewModelProvided() {
        val savedStateWrapper = SavedStateNavLocalProvider
        val viewModelWrapper = ViewModelStoreNavLocalProvider
        lateinit var viewModel1: MyViewModel
        lateinit var viewModel2: MyViewModel
        val record1Arg = "record1 Arg"
        val record2Arg = "record2 Arg"
        val record1 =
            NavRecord("key1") {
                viewModel1 = viewModel<MyViewModel>()
                viewModel1.myArg = record1Arg
            }
        val record2 =
            NavRecord("key2") {
                viewModel2 = viewModel<MyViewModel>()
                viewModel2.myArg = record2Arg
            }
        composeTestRule.setContent {
            savedStateWrapper.ProvideToRecord(
                NavRecord(record1.key) { viewModelWrapper.ProvideToRecord(record1) }
            )
            savedStateWrapper.ProvideToRecord(
                NavRecord(record2.key) { viewModelWrapper.ProvideToRecord(record2) }
            )
        }

        composeTestRule.runOnIdle {
            assertWithMessage("Incorrect arg for record 1")
                .that(viewModel1.myArg)
                .isEqualTo(record1Arg)
            assertWithMessage("Incorrect arg for record 2")
                .that(viewModel2.myArg)
                .isEqualTo(record2Arg)
        }
    }

    @Test
    fun testViewModelNoSavedStateNavLocalProvider() {
        val viewModelWrapper = ViewModelStoreNavLocalProvider
        lateinit var viewModel1: MyViewModel
        val record1Arg = "record1 Arg"
        val record1 =
            NavRecord("key1") {
                viewModel1 = viewModel<MyViewModel>()
                viewModel1.myArg = record1Arg
            }
        try {
            composeTestRule.setContent { viewModelWrapper.ProvideToRecord(record1) }
        } catch (e: Exception) {
            assertThat(e)
                .hasMessageThat()
                .isEqualTo(
                    "The Lifecycle state is already beyond INITIALIZED. The " +
                        "ViewModelStoreNavLocalProvider requires adding the " +
                        "SavedStateNavLocalProvider to ensure support for " +
                        "SavedStateHandles."
                )
        }
    }

    @Test
    fun testViewModelSaved() {
        lateinit var backStack: MutableList<Any>
        composeTestRule.setContent {
            backStack = remember { mutableStateListOf("Home") }
            NavDisplay(
                backstack = backStack,
                localProviders = listOf(SavedStateNavLocalProvider, ViewModelStoreNavLocalProvider),
                onBack = { backStack.removeAt(backStack.lastIndex) },
            ) { key ->
                when (key) {
                    "Home" -> {
                        NavRecord(key) { viewModel<HomeViewModel>() }
                    }
                    "AnotherScreen" -> {
                        NavRecord(key) { viewModel<HomeViewModel>() }
                    }
                    else -> error("Unknown key: $key")
                }
            }
        }

        composeTestRule.waitForIdle()
        assertWithMessage("Only one viewModel should be created")
            .that(globalViewModelCount)
            .isEqualTo(1)

        composeTestRule.runOnIdle { backStack.add("AnotherScreen") }

        composeTestRule.waitForIdle()
        assertWithMessage("Another viewModel should be created")
            .that(globalViewModelCount)
            .isEqualTo(2)

        composeTestRule.runOnIdle { backStack.removeAt(backStack.lastIndex) }

        composeTestRule.waitForIdle()
        assertWithMessage("We should be reusing the old ViewModel since we popped")
            .that(globalViewModelCount)
            .isEqualTo(2)

        composeTestRule.runOnIdle { backStack.add("AnotherScreen") }

        composeTestRule.waitForIdle()
        assertWithMessage("Another viewModel should be created")
            .that(globalViewModelCount)
            .isEqualTo(3)
    }

    @Test
    fun testViewModelSavedStateHandle() {
        lateinit var backStack: MutableList<Any>
        lateinit var viewModel: SavedStateViewModel
        composeTestRule.setContent {
            backStack = remember { mutableStateListOf("Home") }
            NavDisplay(
                backstack = backStack,
                localProviders = listOf(SavedStateNavLocalProvider, ViewModelStoreNavLocalProvider),
                onBack = { backStack.removeAt(backStack.lastIndex) },
            ) { key ->
                when (key) {
                    "Home" -> {
                        NavRecord(key) {
                            viewModel =
                                viewModel<SavedStateViewModel> {
                                    val handle = createSavedStateHandle()
                                    SavedStateViewModel(handle)
                                }
                        }
                    }
                    else -> error("Unknown key: $key")
                }
            }
        }

        composeTestRule.waitForIdle()
        assertThat(viewModel.savedStateHandle).isNotNull()
    }
}

class MyViewModel : ViewModel() {
    var myArg = "default"
}

var globalViewModelCount = 0

class HomeViewModel : ViewModel() {
    init {
        globalViewModelCount++
    }
}

class SavedStateViewModel(val savedStateHandle: SavedStateHandle) : ViewModel()
