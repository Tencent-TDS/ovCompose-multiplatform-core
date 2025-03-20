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

import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.test.filters.MediumTest
import org.junit.After
import org.junit.Before

/**
 * We expect the [SelectionContainerContextMenuFlagFlippedTest] to succeed no matter how the
 * [ComposeFoundationFlags.isNewContextMenuEnabled] is flipped.
 *
 * When the [ComposeFoundationFlags.isNewContextMenuEnabled] flag is removed, this test should be
 * removed and the [SelectionContainerContextMenuFlagFlippedTest] class should have its `open`
 * keyword removed.
 */
@OptIn(ExperimentalFoundationApi::class)
@MediumTest
class SelectionContainerContextMenuFlagFlippedTest : SelectionContainerContextMenuTest() {
    private var initialFlag = ComposeFoundationFlags.isNewContextMenuEnabled

    @Before
    fun setup() {
        ComposeFoundationFlags.isNewContextMenuEnabled = !initialFlag
    }

    @After
    fun cleanup() {
        ComposeFoundationFlags.isNewContextMenuEnabled = initialFlag
    }
}
