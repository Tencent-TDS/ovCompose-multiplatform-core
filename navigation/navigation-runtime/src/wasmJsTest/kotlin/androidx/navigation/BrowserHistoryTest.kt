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

package androidx.navigation

import androidx.kruth.assertThat
import androidx.testutils.TestNavigator
import androidx.testutils.test
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.test.Test
import kotlinx.browser.window
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.w3c.dom.AddEventListenerOptions

class BrowserHistoryTest {

    @Test
    fun checkBrowserHistoryStateSynchronizedWithNavigation() = runTest {
        cleanWindowHistory()
        val navController = NavHostController().apply {
            navigatorProvider.addNavigator(TestNavigator())
        }

        val bind = launch { window.bindToNavigation(navController) }

        val graph = navController.createGraph(route = "graph", startDestination = "screen_1") {
            test("screen_1")
            test("screen_2")
            navigation(route = "nested1", startDestination = "nested2") {
                navigation(route = "nested2", startDestination = "nested3") {
                    navigation(route = "nested3", startDestination = "screen_3") {
                        test("screen_3")
                        test("screen_4")
                    }
                }
                test("screen_5")
            }
        }
        navController.setGraph(graph, null)

        advanceUntilIdle()
        assertThat(window.history.length).isEqualTo(1)
        assertThat(window.history.state.toString()).isEqualTo("screen_1")

        navController.navigate("screen_2")
        navController.navigate("screen_4")

        advanceUntilIdle()
        assertThat(window.history.length).isEqualTo(2)
        assertThat(window.history.state.toString().lines())
            .containsExactly("screen_1", "screen_2", "screen_4")
            .inOrder()

        navController.navigate("screen_5") {
            popUpTo("screen_1") { inclusive = true }
        }
        navController.navigate("screen_2")

        advanceUntilIdle()
        assertThat(window.history.length).isEqualTo(3)
        assertThat(window.history.state.toString().lines())
            .containsExactly("screen_5", "screen_2")
            .inOrder()

        bind.cancel()
    }

    @Test
    fun checkNavigationSynchronizedWithBrowserHistoryState() = runTest {
        cleanWindowHistory()
        val navController = NavHostController().apply {
            navigatorProvider.addNavigator(TestNavigator())
        }

        val bind = launch { window.bindToNavigation(navController) }

        val graph = navController.createGraph(route = "graph", startDestination = "screen_1") {
            test("screen_1")
            test("screen_2")
            navigation(route = "nested1", startDestination = "nested2") {
                navigation(route = "nested2", startDestination = "nested3") {
                    navigation(route = "nested3", startDestination = "screen_3") {
                        test("screen_3")
                        test("screen_4")
                    }
                }
                test("screen_5")
            }
        }
        navController.setGraph(graph, null)
        advanceUntilIdle()

        navController.navigate("screen_2")
        advanceUntilIdle()

        navController.navigate("screen_4")
        advanceUntilIdle()

        navController.navigate("screen_5") {
            popUpTo("screen_1") { inclusive = true }
        }
        advanceUntilIdle()

        navController.navigate("screen_2")
        advanceUntilIdle()

        assertThat(window.history.length).isEqualTo(5)
        assertThat(window.history.state.toString().lines())
            .containsExactly("screen_5", "screen_2")
            .inOrder()

        window.history.back()
        waitHistoryStateUpdate()

        assertThat(window.history.length).isEqualTo(5)
        assertThat(window.history.state.toString().lines())
            .containsExactly("screen_5")
            .inOrder()

        window.history.back()
        waitHistoryStateUpdate()

        assertThat(window.history.length).isEqualTo(5)
        assertThat(window.history.state.toString().lines())
            .containsExactly("screen_1", "screen_2", "screen_4")
            .inOrder()

        window.history.back()
        waitHistoryStateUpdate()

        assertThat(window.history.length).isEqualTo(5)
        assertThat(window.history.state.toString().lines())
            .containsExactly("screen_1", "screen_2")
            .inOrder()

        navController.navigate("screen_2")
        advanceUntilIdle()

        assertThat(window.history.length).isEqualTo(3)
        assertThat(window.history.state.toString().lines())
            .containsExactly("screen_1", "screen_2", "screen_2")
            .inOrder()

        window.history.back()
        waitHistoryStateUpdate()

        assertThat(window.history.length).isEqualTo(3)
        assertThat(window.history.state.toString().lines())
            .containsExactly("screen_1", "screen_2")
            .inOrder()

        window.history.forward()
        waitHistoryStateUpdate()

        assertThat(window.history.length).isEqualTo(3)
        assertThat(window.history.state.toString().lines())
            .containsExactly("screen_1", "screen_2", "screen_2")
            .inOrder()

        bind.cancel()
    }

    private suspend fun cleanWindowHistory() {
        with(window.history) {
            if (length > 1) {
                val size = length - 1
                go(-size)
                waitHistoryStateUpdate()
            }
        }
    }

    private suspend fun waitHistoryStateUpdate() = suspendCoroutine { cont ->
        window.addEventListener(
            type = "popstate",
            callback = { cont.resume(Unit) },
            options = AddEventListenerOptions(passive = false, once = true)
        )
    }
}