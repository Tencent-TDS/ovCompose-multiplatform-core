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

package androidx.appfunctions.integration.tests

import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.AppFunctionFunctionNotFoundException
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionManagerCompat
import androidx.appfunctions.ExecuteAppFunctionRequest
import androidx.appfunctions.ExecuteAppFunctionResponse
import androidx.appfunctions.integration.testapp.library.TestFunctions2Ids
import androidx.appfunctions.integration.tests.TestUtil.doBlocking
import androidx.appfunctions.integration.tests.TestUtil.retryAssert
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertIs
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

@LargeTest
class IntegrationTest {
    private val targetContext = InstrumentationRegistry.getInstrumentation().context
    private val appFunctionManager =
        AppFunctionManagerCompat(InstrumentationRegistry.getInstrumentation().targetContext)
    private val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation

    @Before
    fun setup() = doBlocking {
        assumeTrue(appFunctionManager.isSupported())
        uiAutomation.apply {
            // This is needed because the test is running under the UID of
            // "androidx.appfunctions.integration.testapp",
            // while the app functions are defined under
            // "androidx.appfunctions.integration.testapp.test"
            adoptShellPermissionIdentity("android.permission.EXECUTE_APP_FUNCTIONS")
            executeShellCommand(
                "device_config put appsearch max_allowed_app_function_doc_size_in_bytes $TEST_APP_FUNCTION_DOC_SIZE_LIMIT"
            )
        }
        awaitAppFunctionsIndexed(FUNCTION_IDS)
    }

    @After
    fun tearDown() {
        uiAutomation.dropShellPermissionIdentity()
    }

    @Test
    fun executeAppFunction_success() = doBlocking {
        val response =
            appFunctionManager.executeAppFunction(
                request =
                    ExecuteAppFunctionRequest(
                        targetContext.packageName,
                        TestFunctionsIds.ADD_ID,
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
                        TestFunctionsIds.VOID_FUNCTION_ID,
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
                        TestFactoryIds.IS_CREATED_BY_FACTORY_ID,
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
                        TestFunctions2Ids.CONCAT_ID,
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
                        TestFunctionsIds.DO_THROW_ID,
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
                        TestFunctionsIds.CREATE_NOTE_ID,
                        AppFunctionData.Builder("")
                            .setAppFunctionData(
                                "createNoteParams",
                                AppFunctionData.serialize(
                                    CreateNoteParams(
                                        title = "Test Title",
                                        content = listOf("1", "2"),
                                        owner = Owner("test"),
                                        attachments =
                                            listOf(Attachment("Uri1", Attachment("nested")))
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
                content = listOf("1", "2"),
                owner = Owner("test"),
                attachments = listOf(Attachment("Uri1", Attachment("nested")))
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
        const val TEST_APP_FUNCTION_DOC_SIZE_LIMIT = 512 * 1024 // 512kb

        val FUNCTION_IDS =
            setOf(
                TestFunctionsIds.ADD_ID,
                TestFunctionsIds.DO_THROW_ID,
                TestFunctionsIds.VOID_FUNCTION_ID,
                TestFunctionsIds.CREATE_NOTE_ID,
                TestFactoryIds.IS_CREATED_BY_FACTORY_ID,
                TestFunctions2Ids.CONCAT_ID
            )
    }
}
