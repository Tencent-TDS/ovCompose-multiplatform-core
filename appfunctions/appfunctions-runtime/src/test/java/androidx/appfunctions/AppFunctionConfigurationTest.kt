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

package androidx.appfunctions

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppFunctionConfigurationTest {
    @Test
    fun testEmpty() {
        val configuration = AppFunctionConfiguration.Builder().build()

        assertThat(configuration.factories).isEmpty()
    }

    @Test
    fun testUniqueFactories() {
        val factory1 = TestAppFunctionClass1.Factory()
        val factory2 = TestAppFunctionClass2.Factory()

        val configuration =
            AppFunctionConfiguration.Builder()
                .addFactory(TestAppFunctionClass1::class.java, factory1)
                .addFactory(TestAppFunctionClass2::class.java, factory2)
                .build()

        assertThat(configuration.factories).hasSize(2)
        assertThat(configuration.factories[TestAppFunctionClass1::class.java]).isEqualTo(factory1)
        assertThat(configuration.factories[TestAppFunctionClass2::class.java]).isEqualTo(factory2)
    }

    @Test
    fun testDuplicatedFactories() {
        val factory1 = TestAppFunctionClass1.Factory()

        val configuration =
            AppFunctionConfiguration.Builder()
                .addFactory(TestAppFunctionClass1::class.java, factory1)
                .addFactory(TestAppFunctionClass1::class.java, factory1)
                .build()

        assertThat(configuration.factories).hasSize(1)
        assertThat(configuration.factories[TestAppFunctionClass1::class.java]).isEqualTo(factory1)
    }

    internal class TestAppFunctionClass1 {
        internal class Factory : AppFunctionFactory<TestAppFunctionClass1> {
            override fun createEnclosingClass(
                enclosingClass: Class<TestAppFunctionClass1>
            ): TestAppFunctionClass1 {
                return TestAppFunctionClass1()
            }
        }
    }

    internal class TestAppFunctionClass2 {
        internal class Factory : AppFunctionFactory<TestAppFunctionClass2> {
            override fun createEnclosingClass(
                enclosingClass: Class<TestAppFunctionClass2>
            ): TestAppFunctionClass2 {
                return TestAppFunctionClass2()
            }
        }
    }
}
