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

package androidx.appfunction.integration.tests

import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.AppFunctionFunctionNotFoundException
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionManagerCompat
import androidx.appfunctions.ExecuteAppFunctionRequest
import androidx.appfunctions.ExecuteAppFunctionResponse
import androidx.appfunctions.integration.testapp.CreateNoteParams
import androidx.appfunctions.integration.testapp.Note
import androidx.appfunctions.integration.tests.AppSearchMetadataHelper
import androidx.appfunctions.integration.tests.TestUtil.doBlocking
import androidx.appfunctions.integration.tests.TestUtil.retryAssert
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertIs
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

@LargeTest
class IntegrationTest {
    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val appFunctionManager = AppFunctionManagerCompat(targetContext)

    @Before
    fun setup() = doBlocking {
        assumeTrue(appFunctionManager.isSupported())

        awaitAppFunctionsIndexed(FUNCTION_IDS)
    }

    @Test
    fun executeAppFunction_success() = doBlocking {
        val response =
            appFunctionManager.executeAppFunction(
                request =
                    ExecuteAppFunctionRequest(
                        targetContext.packageName,
                        "androidx.appfunctions.integration.testapp.TestFunctions#add",
                        AppFunctionData.Builder("").setLong("num1", 1).setLong("num2", 2).build()
                    )
            )

        val successResponse = assertIs<ExecuteAppFunctionResponse.Success>(response)
        assertThat(
                successResponse.returnValue.getLong(
                    ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE
                )
            )
            .isEqualTo(3)
    }

    @Test
    fun executeAppFunction_voidReturnType_success() = doBlocking {
        val response =
            appFunctionManager.executeAppFunction(
                request =
                    ExecuteAppFunctionRequest(
                        targetContext.packageName,
                        VOID_FUNCTION_ID,
                        AppFunctionData.Builder("").build()
                    )
            )

        assertThat(response).isInstanceOf(ExecuteAppFunctionResponse.Success::class.java)
    }

    @Test
    fun executeAppFunction_setFactory_success() = doBlocking {
        // A factory is set to create the enclosing class of the function.
        // See [TestApplication.appFunctionConfiguration].
        var response =
            appFunctionManager.executeAppFunction(
                request =
                    ExecuteAppFunctionRequest(
                        targetContext.packageName,
                        IS_CREATED_BY_FACTORY_FUNCTION_ID,
                        AppFunctionData.Builder("").build()
                    )
            )

        // If the enclosing class was created by the provided factory, the secondary constructor
        // should be called and so the return value would be `true`.
        val successResponse = assertIs<ExecuteAppFunctionResponse.Success>(response)
        assertThat(
                successResponse.returnValue.getBoolean(
                    ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE
                )
            )
            .isEqualTo(true)
    }

    @Test
    fun executeAppFunction_functionInLibraryModule_success() = doBlocking {
        var response =
            appFunctionManager.executeAppFunction(
                request =
                    ExecuteAppFunctionRequest(
                        targetContext.packageName,
                        CONCAT_FUNCTION_ID,
                        AppFunctionData.Builder("")
                            .setString("str1", "log")
                            .setString("str2", "cat")
                            .build()
                    )
            )

        val successResponse = assertIs<ExecuteAppFunctionResponse.Success>(response)
        assertThat(
                successResponse.returnValue.getString(
                    ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE
                )
            )
            .isEqualTo("logcat")
    }

    @Test
    fun executeAppFunction_functionNotFound_fail() = doBlocking {
        val response =
            appFunctionManager.executeAppFunction(
                request =
                    ExecuteAppFunctionRequest(
                        targetContext.packageName,
                        "androidx.appfunctions.integration.testapp.TestFunctions#notExist",
                        AppFunctionData.Builder("").build()
                    )
            )

        val errorResponse = assertIs<ExecuteAppFunctionResponse.Error>(response)
        assertThat(errorResponse.error)
            .isInstanceOf(AppFunctionFunctionNotFoundException::class.java)
    }

    @Test
    fun executeAppFunction_appThrows_fail() = doBlocking {
        val response =
            appFunctionManager.executeAppFunction(
                request =
                    ExecuteAppFunctionRequest(
                        targetContext.packageName,
                        "androidx.appfunctions.integration.testapp.TestFunctions#doThrow",
                        AppFunctionData.Builder("").build()
                    )
            )

        assertThat(response).isInstanceOf(ExecuteAppFunctionResponse.Error::class.java)
        val errorResponse = response as ExecuteAppFunctionResponse.Error
        assertThat(errorResponse.error)
            .isInstanceOf(AppFunctionInvalidArgumentException::class.java)
    }

    @Test
    fun executeAppFunction_createNote() = doBlocking {
        val response =
            appFunctionManager.executeAppFunction(
                request =
                    ExecuteAppFunctionRequest(
                        targetContext.packageName,
                        CREATE_NOTE_FUNCTION_ID,
                        AppFunctionData.Builder("")
                            .setAppFunctionData(
                                "createNoteParams",
                                AppFunctionData.serialize(
                                    CreateNoteParams(
                                        title = "Test Title",
                                    ),
                                    CreateNoteParams::class.java
                                )
                            )
                            .build()
                    )
            )

        val successResponse = assertIs<ExecuteAppFunctionResponse.Success>(response)
        val expectedNote =
            Note(
                title = "Test Title",
            )
        assertThat(
                successResponse.returnValue
                    .getAppFunctionData(ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE)
                    ?.deserialize(Note::class.java)
            )
            .isEqualTo(expectedNote)
    }

    private suspend fun awaitAppFunctionsIndexed(expectedFunctionIds: Set<String>) {
        retryAssert {
            val functionIds = AppSearchMetadataHelper.collectSelfFunctionIds(targetContext)
            assertThat(functionIds).containsAtLeastElementsIn(expectedFunctionIds)
        }
    }

    private companion object {
        // AppFunctions that are defined in the top-level module.
        const val APP_FUNCTION_ID = "androidx.appfunctions.integration.testapp.TestFunctions#add"
        const val DO_THROW_FUNCTION_ID =
            "androidx.appfunctions.integration.testapp.TestFunctions#doThrow"
        const val VOID_FUNCTION_ID =
            "androidx.appfunctions.integration.testapp.TestFunctions#voidFunction"
        const val CREATE_NOTE_FUNCTION_ID =
            "androidx.appfunctions.integration.testapp.TestFunctions#createNote"
        const val IS_CREATED_BY_FACTORY_FUNCTION_ID =
            "androidx.appfunctions.integration.testapp.TestFactory#isCreatedByFactory"

        // AppFunctions that are defined in a library module.
        const val CONCAT_FUNCTION_ID =
            "androidx.appfunctions.integration.testapp.library.TestFunctions2#concat"
        val FUNCTION_IDS =
            setOf(
                APP_FUNCTION_ID,
                DO_THROW_FUNCTION_ID,
                VOID_FUNCTION_ID,
                CREATE_NOTE_FUNCTION_ID,
                IS_CREATED_BY_FACTORY_FUNCTION_ID,
                CONCAT_FUNCTION_ID
            )
    }
}
