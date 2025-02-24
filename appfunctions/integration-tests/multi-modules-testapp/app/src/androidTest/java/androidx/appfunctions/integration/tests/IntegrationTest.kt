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
import androidx.appfunctions.integration.tests.AppSearchMetadataHelper
import androidx.appfunctions.integration.tests.TestUtil.doBlocking
import androidx.appfunctions.integration.tests.TestUtil.retryAssert
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertIs
import kotlinx.coroutines.delay
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
        delay(5)
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

    private suspend fun awaitAppFunctionsIndexed(expectedFunctionIds: Set<String>) {
        retryAssert {
            val functionIds = AppSearchMetadataHelper.collectSelfFunctionIds(targetContext)
            assertThat(functionIds).containsAtLeastElementsIn(expectedFunctionIds)
        }
    }

    private companion object {
        const val APP_FUNCTION_ID = "androidx.appfunctions.integration.testapp.TestFunctions#add"
        const val DO_THROW_FUNCTION_ID =
            "androidx.appfunctions.integration.testapp.TestFunctions#doThrow"
        val FUNCTION_IDS = setOf(APP_FUNCTION_ID, DO_THROW_FUNCTION_ID)
    }
}
