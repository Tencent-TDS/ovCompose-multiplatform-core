/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.foundation

import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class AssertThat<T>(val t: T?)

internal fun <T> AssertThat<T>.isEqualTo(a: Any?) {
    assertEquals(a, t)
}

internal fun AssertThat<Boolean>.isTrue() = assertTrue(t == true)

internal fun AssertThat<Boolean>.isFalse() = assertTrue(t == false)

internal fun <T> AssertThat<Iterable<T>>.containsExactly(vararg any: T) {
    require(t != null)
    assertContentEquals(t, any.toList())
}

internal fun AssertThat<Float>.isGreaterThan(n: Int) {
    assertTrue(t!! > n, "$t is not greater than $n")
}

internal fun AssertThat<Float>.isGreaterThan(n: Float) {
    assertTrue(t!! > n, "$t is not greater than $n")
}
internal fun AssertThat<Float>.isLessThan(n: Float) {
    assertTrue(t!! < n, "$t is not less than $n")
}

internal fun <T : Collection<*>> AssertThat<T>.isEmpty() {
    assertTrue(t!!.isEmpty(), "$t is not empty")
}

internal fun <T : Collection<*>> AssertThat<T>.hasSize(size: Int) {
    assertEquals(t!!.size, size, "$t has ${t.size} items, but $size expected")
}

internal fun AssertThat<*>.isNull() {
    assertEquals(null, t, "$t expected to be null")
}

internal fun <T> assertThat(t: T?): AssertThat<T> {
    return AssertThat(t)
}
