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

package androidx.appfunctions.compiler

import androidx.appfunctions.compiler.testings.CompilationTestHelper
import com.google.common.truth.Truth
import java.io.File
import org.junit.Before
import org.junit.Test

class AppFunctionCompilerTest {
    private lateinit var compilationTestHelper: CompilationTestHelper

    @Before
    fun setup() {
        compilationTestHelper =
            CompilationTestHelper(
                testFileSrcDir = File("src/test/test-data/input"),
                goldenFileSrcDir = File("src/test/test-data/output"),
                symbolProcessorProviders = listOf(AppFunctionCompiler.Provider())
            )
    }

    @Test
    fun testEmpty() {
        val report = compilationTestHelper.compileAll(sourceFileNames = emptyList())

        Truth.assertThat(report.isSuccess).isTrue()
    }

    @Test
    fun testSimpleFunction_genAppFunctionIds_success() {
        val report = compilationTestHelper.compileAll(sourceFileNames = listOf("SimpleFunction.KT"))

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "SimpleFunctionIds.kt",
            goldenFileName = "SimpleFunctionIds.KT"
        )
    }

    @Test
    fun testMissingFirstParameter_hasCompileError() {
        val report =
            compilationTestHelper.compileAll(sourceFileNames = listOf("MissingFirstParameter.KT"))

        compilationTestHelper.assertErrorWithMessage(
            report,
            "The first parameter of an app function must be " +
                "androidx.appfunctions.AppFunctionContext\n" +
                "    fun missingFirstParameter() {}\n" +
                "    ^"
        )
    }

    @Test
    fun testIncorrectFirstParameter_hasCompileError() {
        val report =
            compilationTestHelper.compileAll(sourceFileNames = listOf("IncorrectFirstParameter.KT"))

        compilationTestHelper.assertErrorWithMessage(
            report,
            "The first parameter of an app function must be " +
                "androidx.appfunctions.AppFunctionContext\n" +
                "    fun incorrectFirstParameter(x: Int) {}\n" +
                "    ^"
        )
    }

    @Test
    fun testSimpleFunction_genAppFunctionInventoryImpl_success() {
        val report = compilationTestHelper.compileAll(sourceFileNames = listOf("SimpleFunction.KT"))

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "${'$'}SimpleFunction_AppFunctionInventory_Impl.kt",
            goldenFileName = "${'$'}SimpleFunction_AppFunctionInventory_Impl.KT"
        )
    }

    @Test
    fun testAllPrimitiveInputFunctions_genAppFunctionInventoryImpl_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("AllPrimitiveInputFunctions.KT")
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName =
                "${'$'}AllPrimitiveInputFunctions_AppFunctionInventory_Impl.kt",
            goldenFileName = "${'$'}AllPrimitiveInputFunctions_AppFunctionInventory_Impl.KT"
        )
    }

    @Test
    fun testSimpleFunction_genAppFunctionInvokerImpl_success() {
        val report = compilationTestHelper.compileAll(sourceFileNames = listOf("SimpleFunction.KT"))

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "${'$'}SimpleFunction_AppFunctionInvoker.kt",
            goldenFileName = "${'$'}SimpleFunction_AppFunctionInvoker.KT",
        )
    }

    @Test
    fun testAllPrimitiveInputFunctions_genAppFunctionInvokerImpl_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("AllPrimitiveInputFunctions.KT")
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName =
                "${'$'}AllPrimitiveInputFunctions_AppFunctionInvoker.kt",
            goldenFileName = "${'$'}AllPrimitiveInputFunctions_AppFunctionInvoker.KT",
        )
    }

    @Test
    fun testBadInputFunctions_genAppFunctionInventoryImpl_hasCompileError() {
        val reportListPrimitiveArrayInputFunction =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("ListPrimitiveArrayInputFunction.KT")
            )
        val reportArrayNonPrimitiveInputFunction =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("ArrayNonPrimitiveInputFunction.KT")
            )
        val reportAnyTypedInputFunction =
            compilationTestHelper.compileAll(sourceFileNames = listOf("AnyTypedInputFunction.KT"))

        compilationTestHelper.assertErrorWithMessage(
            reportListPrimitiveArrayInputFunction,
            "App function parameters must be one of the following primitive types or a list " +
                "of these types"
        )
        compilationTestHelper.assertErrorWithMessage(
            reportArrayNonPrimitiveInputFunction,
            "App function parameters must be one of the following primitive types or a list " +
                "of these types"
        )
        compilationTestHelper.assertErrorWithMessage(
            reportAnyTypedInputFunction,
            "App function parameters must be one of the following primitive types or a list " +
                "of these types"
        )
    }

    @Test
    fun testFakeNoArgImpl_genLegacyIndexXmlFile_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("FakeNoArgImpl.KT", "FakeSchemas.KT")
            )

        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions.xml",
            goldenFileName = "fakeNoArgImpl_app_function.xml"
        )
    }

    @Test
    fun testFakeNoArgImp_isEnabledTrue_genLegacyIndexXmlFile_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("FakeNoArgImpl_IsEnabled_True.KT", "FakeSchemas.KT")
            )

        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions.xml",
            goldenFileName = "fakeNoArgImpl_isEnabled_true_app_function.xml"
        )
    }

    @Test
    fun testFakeNoArgImp_isEnabledFalse_genLegacyIndexXmlFile_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("FakeNoArgImpl_IsEnabled_False.KT", "FakeSchemas.KT")
            )

        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions.xml",
            goldenFileName = "fakeNoArgImpl_isEnabled_false_app_function.xml"
        )
    }

    @Test
    fun testSimpleFunction_noOverride_hasCompileError() {
        val report = compilationTestHelper.compileAll(sourceFileNames = listOf("SimpleFunction.KT"))

        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions.xml",
            goldenFileName = "simpleFunction_noSchema_app_function.xml"
        )
    }
}
