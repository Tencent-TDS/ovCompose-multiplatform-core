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
                symbolProcessorProviders = listOf(AppFunctionCompiler.Provider()),
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
            goldenFileName = "SimpleFunctionIds.KT",
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
                "    ^",
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
                "    ^",
        )
    }

    @Test
    fun testSimpleFunction_genAppFunctionInventoryImpl_success() {
        val report = compilationTestHelper.compileAll(sourceFileNames = listOf("SimpleFunction.KT"))

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "${'$'}SimpleFunction_AppFunctionInventory.kt",
            goldenFileName = "${'$'}SimpleFunction_AppFunctionInventory.KT",
        )
    }

    @Test
    fun testAllPrimitiveInputFunctions_genAppFunctionInventory_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("AllPrimitiveInputFunctions.KT")
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName =
                "${'$'}AllPrimitiveInputFunctions_AppFunctionInventory.kt",
            goldenFileName = "${'$'}AllPrimitiveInputFunctions_AppFunctionInventory.KT",
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
    fun testSerializableInputFunctions_genAppFunctionInventory_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("SerializableInputFunctions.KT"),
                processorOptions = mapOf("appfunctions:aggregateAppFunctions" to "true"),
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName =
                "${'$'}SerializableInputFunctions_AppFunctionInventory.kt",
            goldenFileName = "${'$'}SerializableInputFunctions_AppFunctionInventory.KT",
        )
        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions_dynamic_schema.xml",
            goldenFileName = "serializableInputFunctions_app_function_dynamic_schema.xml",
        )
    }

    @Test
    fun testDiffPackageSerializableInputFunction_genAppFunctionInventory_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames =
                    listOf(
                        "AppFunctionWithInputFromDifferentPackage.KT",
                        "DiffPackageSerializable.KT",
                        "DiffPackageSchemas.KT",
                        "AnotherDiffPackageSerializable.KT",
                    ),
                processorOptions = mapOf("appfunctions:aggregateAppFunctions" to "true"),
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName =
                "${'$'}AppFunctionWithInputFromDifferentPackage_AppFunctionInventory.kt",
            goldenFileName =
                "${'$'}AppFunctionWithInputFromDifferentPackage_AppFunctionInventory.KT",
        )
        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions_dynamic_schema.xml",
            goldenFileName =
                "appFunctionWithInputFromDifferentPackage_app_function_dynamic_schema.xml",
        )
    }

    @Test
    fun testFakeAllPrimitiveParamsImpl_genAppFunctionInventory_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("FakeAllPrimitiveParamsImpl.KT", "FakeSchemas.KT")
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName =
                "${'$'}FakeAllPrimitiveParamsImpl_AppFunctionInventory.kt",
            goldenFileName = "${'$'}FakeAllPrimitiveParamsImpl_AppFunctionInventory.KT",
        )
    }

    @Test
    fun testFakeAllPrimitiveReturnsImpl_genAppFunctionInventory_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("FakeAllPrimitiveReturnsImpl.KT", "FakeSchemas.KT")
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName =
                "${'$'}FakeAllPrimitiveReturnsImpl_AppFunctionInventory.kt",
            goldenFileName = "${'$'}FakeAllPrimitiveReturnsImpl_AppFunctionInventory.KT",
        )
    }

    @Test
    fun testSerializableOutputFunctions_genAppFunctionInventory_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("SerializableOutputFunctions.KT"),
                processorOptions = mapOf("appfunctions:aggregateAppFunctions" to "true"),
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName =
                "${'$'}SerializableOutputFunctions_AppFunctionInventory.kt",
            goldenFileName = "${'$'}SerializableOutputFunctions_AppFunctionInventory.KT",
        )
        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions_dynamic_schema.xml",
            goldenFileName = "serializableOutputFunctions_app_function_dynamic_schema.xml",
        )
    }

    @Test
    fun testDiffPackageSerializableOutputFunction_genAppFunctionInventoryAndIndexXml_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames =
                    listOf(
                        "AppFunctionWithOutputFromDifferentPackage.KT",
                        "DiffPackageSerializable.KT",
                        "DiffPackageSchemas.KT",
                        "AnotherDiffPackageSerializable.KT",
                    ),
                processorOptions = mapOf("appfunctions:aggregateAppFunctions" to "true"),
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName =
                "${'$'}AppFunctionWithOutputFromDifferentPackage_AppFunctionInventory.kt",
            goldenFileName =
                "${'$'}AppFunctionWithOutputFromDifferentPackage_AppFunctionInventory.KT",
        )
        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions_dynamic_schema.xml",
            goldenFileName =
                "appFunctionWithOutputFromDifferentPackage_app_function_dynamic_schema.xml",
        )
    }

    @Test
    fun testRecursiveSerializableInputFunctions_genAppFunctionInventoryImplAndIndexXml_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("RecursiveSerializableInputFunctions.KT"),
                processorOptions = mapOf("appfunctions:aggregateAppFunctions" to "true"),
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName =
                "${'$'}RecursiveSerializableInputFunctions_AppFunctionInventory.kt",
            goldenFileName = "${'$'}RecursiveSerializableInputFunctions_AppFunctionInventory.KT",
        )
        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions_dynamic_schema.xml",
            goldenFileName = "recursiveSerializableInputFunctions_app_function_dynamic_schema.xml",
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
            "App function parameters must be a supported type, or a type annotated as" +
                " @AppFunctionSerializable. See list of supported types",
        )
        compilationTestHelper.assertErrorWithMessage(
            reportArrayNonPrimitiveInputFunction,
            "App function parameters must be a supported type, or a type annotated as" +
                " @AppFunctionSerializable. See list of supported types",
        )
        compilationTestHelper.assertErrorWithMessage(
            reportAnyTypedInputFunction,
            "App function parameters must be a supported type, or a type annotated as" +
                " @AppFunctionSerializable. See list of supported types",
        )
    }

    @Test
    fun testFakeNoArgImpl_genLegacyIndexXmlFile_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("FakeNoArgImpl.KT", "FakeSchemas.KT"),
                processorOptions = mapOf("appfunctions:aggregateAppFunctions" to "true"),
            )

        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions.xml",
            goldenFileName = "fakeNoArgImpl_app_function.xml",
        )
    }

    @Test
    fun testFakeNoArgImp_isEnabledTrue_genLegacyIndexXmlFile_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("FakeNoArgImpl_IsEnabled_True.KT", "FakeSchemas.KT"),
                processorOptions = mapOf("appfunctions:aggregateAppFunctions" to "true"),
            )

        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions.xml",
            goldenFileName = "fakeNoArgImpl_isEnabled_true_app_function.xml",
        )
    }

    @Test
    fun testFakeNoArgImp_isEnabledFalse_genLegacyIndexXmlFile_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("FakeNoArgImpl_IsEnabled_False.KT", "FakeSchemas.KT"),
                processorOptions = mapOf("appfunctions:aggregateAppFunctions" to "true"),
            )

        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions.xml",
            goldenFileName = "fakeNoArgImpl_isEnabled_false_app_function.xml",
        )
    }

    @Test
    fun testFakeNoArg_freeForm_genLegacyIndexXmlFile_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("FakeNoArg_FreeForm_Function.KT"),
                processorOptions = mapOf("appfunctions:aggregateAppFunctions" to "true"),
            )

        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions.xml",
            goldenFileName = "fakeNoArg_freeForm_function_app_function.xml",
        )
    }

    @Test
    fun testFakeNoArgImpl_genIndexXmlFile_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("FakeNoArgImpl.KT", "FakeSchemas.KT"),
                processorOptions = mapOf("appfunctions:aggregateAppFunctions" to "true"),
            )

        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions_dynamic_schema.xml",
            goldenFileName = "fakeNoArgImpl_app_function_dynamic_schema.xml",
        )
    }

    @Test
    fun testFakeNoArgImp_isEnabledTrue_genIndexXmlFile_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("FakeNoArgImpl_IsEnabled_True.KT", "FakeSchemas.KT"),
                processorOptions = mapOf("appfunctions:aggregateAppFunctions" to "true"),
            )

        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions_dynamic_schema.xml",
            goldenFileName = "fakeNoArgImpl_isEnabled_true_app_function_dynamic_schema.xml",
        )
    }

    @Test
    fun testFakeNoArgImp_isEnabledFalse_genIndexXmlFile_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("FakeNoArgImpl_IsEnabled_False.KT", "FakeSchemas.KT"),
                processorOptions = mapOf("appfunctions:aggregateAppFunctions" to "true"),
            )

        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions_dynamic_schema.xml",
            goldenFileName = "fakeNoArgImpl_isEnabled_false_app_function_dynamic_schema.xml",
        )
    }

    @Test
    fun testFakeNoArg_freeForm_genIndexXmlFile_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("FakeNoArg_FreeForm_Function.KT"),
                processorOptions = mapOf("appfunctions:aggregateAppFunctions" to "true"),
            )

        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions_dynamic_schema.xml",
            goldenFileName = "fakeNoArg_freeForm_function_app_function_dynamic_schema.xml",
        )
    }

    @Test
    fun testFakeAllPrimitiveParams_genIndexXmlFile_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("FakeAllPrimitiveParamsImpl.KT", "FakeSchemas.KT"),
                processorOptions = mapOf("appfunctions:aggregateAppFunctions" to "true"),
            )

        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions_dynamic_schema.xml",
            goldenFileName = "fakeAllPrimitiveParams_app_function_dynamic_schema.xml",
        )
    }

    @Test
    fun testFakeAllPrimitiveReturns_genIndexXmlFile_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("FakeAllPrimitiveReturnsImpl.KT", "FakeSchemas.KT"),
                processorOptions = mapOf("appfunctions:aggregateAppFunctions" to "true"),
            )

        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions_dynamic_schema.xml",
            goldenFileName = "fakeAllPrimitiveReturns_app_function_dynamic_schema.xml",
        )
    }

    @Test
    fun testFakeAllNullablePrimitiveParamsWithDefault_genIndexXmlFile_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames =
                    listOf(
                        "FakeAllNullablePrimitiveParamsWithDefaultValuesImpl.KT",
                        "FakeSchemas.KT",
                    ),
                processorOptions = mapOf("appfunctions:aggregateAppFunctions" to "true"),
            )

        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions_dynamic_schema.xml",
            goldenFileName =
                "fakeAllNullablePrimitiveParamsWithDefault_app_function_dynamic_schema.xml",
        )
    }

    @Test
    fun testGenerateInventoryComponentRegistry() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames =
                    listOf(
                        "AllPrimitiveInputFunctions.KT",
                        "SimpleFunction.KT",
                        "SimpleFunctionDiffPackage.KT",
                    )
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "${'$'}Main_InventoryComponentRegistry.kt",
            goldenFileName = "${'$'}Main_InventoryComponentRegistry.KT",
        )
    }

    @Test
    fun testGenerateInvokerComponentRegistry() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames =
                    listOf(
                        "AllPrimitiveInputFunctions.KT",
                        "SimpleFunction.KT",
                        "SimpleFunctionDiffPackage.KT",
                    )
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "${'$'}Main_InvokerComponentRegistry.kt",
            goldenFileName = "${'$'}Main_InvokerComponentRegistry.KT",
        )
    }

    @Test
    fun testGenerateFunctionComponentRegistry() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames =
                    listOf(
                        "AllPrimitiveInputFunctions.KT",
                        "SimpleFunction.KT",
                        "SimpleFunctionDiffPackage.KT",
                    )
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "${'$'}Main_FunctionComponentRegistry.kt",
            goldenFileName = "${'$'}Main_FunctionComponentRegistry.KT",
        )
    }

    @Test
    fun testGenerateAggregateInventoryImpl() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames =
                    listOf(
                        "AllPrimitiveInputFunctions.KT",
                        "SimpleFunction.KT",
                        // TODO(b/395812003): Fix naming conflict issue
                        //                        "SimpleFunctionDiffPackage.KT",
                    ),
                processorOptions = mapOf("appfunctions:aggregateAppFunctions" to "true"),
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "${'$'}AggregatedAppFunctionInventory_Impl.kt",
            goldenFileName = "${'$'}AggregatedAppFunctionInventory_Impl.KT",
        )
    }

    @Test
    fun testGenerateAggregateInvokerImpl() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames =
                    listOf(
                        "AllPrimitiveInputFunctions.KT",
                        "SimpleFunction.KT",
                        // TODO(b/395812003): Fix naming conflict issue
                        //                        "SimpleFunctionDiffPackage.KT",
                    ),
                processorOptions = mapOf("appfunctions:aggregateAppFunctions" to "true"),
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "${'$'}AggregatedAppFunctionInvoker_Impl.kt",
            goldenFileName = "${'$'}AggregatedAppFunctionInvoker_Impl.KT",
        )
    }
}
