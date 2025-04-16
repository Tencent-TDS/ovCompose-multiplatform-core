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

package androidx.compose.material3

import androidx.compose.material3.internal.formatString
import kotlin.test.Test
import kotlin.test.assertEquals

class StringFormatTest {

    @Test
    fun no_args() {
        val result = formatString("str")
        assertEquals("str", result)
    }

    @Test
    fun one_number_arg() {
        val result = formatString("str %1\$d", 1)
        assertEquals("str 1", result)
    }

    @Test
    fun one_string_arg() {
        val result = formatString("str %1\$s", "1")
        assertEquals("str 1", result)
    }

    @Test
    fun two_number_args() {
        val result = formatString("str %1\$d %2\$d", 1, 2)
        assertEquals("str 1 2", result)
    }

    @Test
    fun two_string_args() {
        val result = formatString("str %1\$s %2\$s", "1", "2")
        assertEquals("str 1 2", result)
    }

    @Test
    fun string_and_number_args() {
        val result = formatString("str %1\$s %2\$d", "1", 2)
        assertEquals("str 1 2", result)
    }

    @Test
    fun too_many_args() {
        val result = formatString("str %1\$d", 1, 2)
        assertEquals("str 1", result)
    }

    @Test
    fun not_enough_args() {
        // Behavior is not specified. We can consider different variants.
        val result = formatString("str %1\$d", )
        assertEquals("str %1\$d", result)
    }

}
