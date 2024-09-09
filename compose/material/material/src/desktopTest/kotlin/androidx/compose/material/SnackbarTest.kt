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

package androidx.compose.material

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Test


class SnackbarTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testQueueing() = runComposeUiTest {
        var snackbarsShown = 0
        setContent {
            val state = remember { SnackbarHostState() }

            SnackbarHost(state)

            LaunchedEffect(Unit) {
                repeat(4) {
                    launch {
                        state.showSnackbar(snackbarsShown.toString())
                        snackbarsShown++
                    }
                }
            }
        }

        // The snackbars are shown sequentially. The coroutines calling `showSnackbar` are blocked
        // and released by a `LaunchedEffect` (in the composition) after the snackbar duration
        // completes. That's why each pair of `advanceTimeBy` and `waitForIdle` only shows one
        // snackbar.
        (1..4).forEach {
            mainClock.advanceTimeBy(60_000)
            waitForIdle()
            assertEquals(it, snackbarsShown)
        }
    }
}