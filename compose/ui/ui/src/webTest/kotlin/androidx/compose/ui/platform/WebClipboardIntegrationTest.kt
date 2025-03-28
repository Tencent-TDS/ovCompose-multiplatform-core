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

package androidx.compose.ui.platform

import androidx.compose.ui.OnCanvasTests
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.browser.window
import kotlinx.coroutines.test.runTest

class WebClipboardIntegrationTest : OnCanvasTests {

    @Test
    fun canGetClipboard() = runTest {
        var clipboard: Clipboard? = null

        createComposeWindow {
            clipboard = LocalClipboard.current
        }

        assertNotNull(clipboard)
        assertEquals<Any>(window.navigator.clipboard, clipboard!!.nativeClipboard)
    }

    // TODO: we can't write or read the clipboard due to permissions requirement:
    // we can't grant the permission in advance using current testing tooling.
    // It could be potentially achieved with Puppeteer.
    // Currently, this test fails with timeout waiting for permission to be granted.
    @Test
    @Ignore
    fun canSetClipboard() = runTest {
        var clipboard: Clipboard? = null

        createComposeWindow {
            clipboard = LocalClipboard.current
        }

        assertNotNull(clipboard)
        assertEquals<Any>(window.navigator.clipboard, clipboard!!.nativeClipboard)

        requestFocus() // focus is required to access the browser Clipboard
        clipboard!!.setClipEntry(null)
        clipboard!!.setClipEntry(ClipEntry.withPlainText("test"))
    }
}