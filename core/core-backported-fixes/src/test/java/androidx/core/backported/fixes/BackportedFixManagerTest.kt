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

package androidx.core.backported.fixes

import androidx.core.backported.fixes.KnownIssue.KI_350037023
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Unit tests for [BackportedFixManager]. */
@RunWith(RobolectricTestRunner::class)
class BackportedFixManagerTest {

    @Test
    fun isFixed_ki350037023() {
        val fixManager = BackportedFixManager()
        val result = fixManager.isFixed(KI_350037023)
        assertThat(result).isFalse()
    }
}
