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

package androidx.compose.ui.test

import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import kotlin.test.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalTestApi::class)
class IdlingResourceTest {

    @Test
    fun testIdlingResource() = runDesktopComposeUiTest {
        var text by mutableStateOf("")
        setContent {
            Text(
                text = text,
                modifier = Modifier.testTag("text")
            )
        }

        var isIdle = true
        val idlingResource = object : IdlingResource {
            override val isIdleNow: Boolean
                get() = isIdle
        }

        fun test(expectedValue: String) {
            text = "first"
            isIdle = false
            val job = CoroutineScope(Dispatchers.Default).launch {
                delay(1000)
                text = "second"
                isIdle = true
            }
            try {
                onNodeWithTag("text").assertTextEquals(expectedValue)
            } finally {
                job.cancel()
            }
        }

        // With the idling resource registered, we expect the test to wait until the second value
        // has been set.
        registerIdlingResource(idlingResource)
        test(expectedValue = "second")

        // Without the idling resource registered, we expect the test to see the first value
        unregisterIdlingResource(idlingResource)
        test(expectedValue = "first")
    }
}