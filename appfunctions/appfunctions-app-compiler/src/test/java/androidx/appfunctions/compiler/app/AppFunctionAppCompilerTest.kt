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

package androidx.appfunctions.compiler.app

import androidx.appfunctions.compiler.testings.CompilationTestHelper
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Before
import org.junit.Test

class AppFunctionAppCompilerTest {
    private lateinit var compilationTestHelper: CompilationTestHelper

    @Before
    fun setup() {
        compilationTestHelper =
            CompilationTestHelper(
                testFileSrcDir = File("src/test/test-data/input"),
                goldenFileSrcDir = File("src/test/test-data/output"),
                symbolProcessorProviders = listOf(AppFunctionAppCompiler.Provider())
            )
    }

    @Test
    fun testEmpty() {
        val report = compilationTestHelper.compileAll(sourceFileNames = emptyList())

        assertThat(report.isSuccess).isTrue()
    }

    @Test
    fun testSimpleFunction() {
        val report = compilationTestHelper.compileAll(sourceFileNames = listOf("SimpleFunction.KT"))

        compilationTestHelper.assertSuccessWithContent(
            report = report,
            expectGeneratedFileName = "SimpleFunctionIds.kt",
            goldenFileName = "SimpleFunctionIds.KT"
        )
    }
}
