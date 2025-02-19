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

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appfunctions.core.AppFunctionMetadataTestHelper
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
class AppFunctionManagerCompatTest {

    private lateinit var context: Context

    private lateinit var metadataTestHelper: AppFunctionMetadataTestHelper

    private lateinit var appFunctionManagerCompat: AppFunctionManagerCompat

    private val testFunctionIds =
        setOf(
            AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_ENABLED_BY_DEFAULT,
            AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_DISABLED_BY_DEFAULT,
        )

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        appFunctionManagerCompat = AppFunctionManagerCompat(context)
        metadataTestHelper = AppFunctionMetadataTestHelper(context)

        assumeTrue(appFunctionManagerCompat.isSupported())

        runBlocking {
            metadataTestHelper.awaitAppFunctionIndexed(testFunctionIds)

            // Reset all test ids
            for (functionIds in testFunctionIds) {
                appFunctionManagerCompat.setAppFunctionEnabled(
                    functionIds,
                    AppFunctionManagerCompat.Companion.APP_FUNCTION_STATE_DEFAULT
                )
            }
        }
    }

    @Test
    fun testSelfIsAppFunctionEnabled_defaultEnabledState() {
        val isEnabled = runBlocking {
            appFunctionManagerCompat.isAppFunctionEnabled(
                AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_ENABLED_BY_DEFAULT
            )
        }

        assertThat(isEnabled).isTrue()
    }

    @Test
    fun testSelfIsAppFunctionEnabled_defaultDisabledState() {
        val isEnabled = runBlocking {
            appFunctionManagerCompat.isAppFunctionEnabled(
                AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_DISABLED_BY_DEFAULT
            )
        }

        assertThat(isEnabled).isFalse()
    }

    @Test
    fun testIsAppFunctionEnabled_defaultEnabledState() {
        val isEnabled = runBlocking {
            appFunctionManagerCompat.isAppFunctionEnabled(
                context.packageName,
                AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_ENABLED_BY_DEFAULT
            )
        }

        assertThat(isEnabled).isTrue()
    }

    @Test
    fun testIsAppFunctionEnabled_defaultDisabledState() {
        val isEnabled = runBlocking {
            appFunctionManagerCompat.isAppFunctionEnabled(
                context.packageName,
                AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_DISABLED_BY_DEFAULT
            )
        }

        assertThat(isEnabled).isFalse()
    }

    @Test
    fun testSetAppFunctionEnabled_overrideToDisable() {
        val isEnabled = runBlocking {
            appFunctionManagerCompat.setAppFunctionEnabled(
                AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_ENABLED_BY_DEFAULT,
                AppFunctionManagerCompat.APP_FUNCTION_STATE_DISABLED
            )
            appFunctionManagerCompat.isAppFunctionEnabled(
                context.packageName,
                AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_ENABLED_BY_DEFAULT
            )
        }

        assertThat(isEnabled).isFalse()
    }

    @Test
    fun testSetAppFunctionEnabled_overrideToEnabled() {
        val isEnabled = runBlocking {
            appFunctionManagerCompat.setAppFunctionEnabled(
                AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_DISABLED_BY_DEFAULT,
                AppFunctionManagerCompat.APP_FUNCTION_STATE_ENABLED
            )
            appFunctionManagerCompat.isAppFunctionEnabled(
                context.packageName,
                AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_DISABLED_BY_DEFAULT
            )
        }

        assertThat(isEnabled).isTrue()
    }

    @Test
    fun testSetAppFunctionEnabled_resetToEnabled() {
        val isEnabled = runBlocking {
            appFunctionManagerCompat.setAppFunctionEnabled(
                AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_ENABLED_BY_DEFAULT,
                AppFunctionManagerCompat.APP_FUNCTION_STATE_DISABLED
            )
            appFunctionManagerCompat.setAppFunctionEnabled(
                AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_ENABLED_BY_DEFAULT,
                AppFunctionManagerCompat.APP_FUNCTION_STATE_DEFAULT
            )
            appFunctionManagerCompat.isAppFunctionEnabled(
                context.packageName,
                AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_ENABLED_BY_DEFAULT
            )
        }

        assertThat(isEnabled).isTrue()
    }

    @Test
    fun testSetAppFunctionEnabled_resetToDisabled() {
        val isEnabled = runBlocking {
            appFunctionManagerCompat.setAppFunctionEnabled(
                AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_DISABLED_BY_DEFAULT,
                AppFunctionManagerCompat.APP_FUNCTION_STATE_ENABLED
            )
            appFunctionManagerCompat.setAppFunctionEnabled(
                AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_DISABLED_BY_DEFAULT,
                AppFunctionManagerCompat.APP_FUNCTION_STATE_DEFAULT
            )
            appFunctionManagerCompat.isAppFunctionEnabled(
                context.packageName,
                AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_DISABLED_BY_DEFAULT
            )
        }

        assertThat(isEnabled).isFalse()
    }

    @Test
    fun testExecuteAppFunction_functionNotExist() {
        val request =
            ExecuteAppFunctionRequest(
                targetPackageName = context.packageName,
                functionIdentifier = "fakeFunctionId",
                functionParameters = AppFunctionData.EMPTY,
            )

        val response = runBlocking { appFunctionManagerCompat.executeAppFunction(request) }

        assertThat(response).isInstanceOf(ExecuteAppFunctionResponse.Error::class.java)
        assertThat((response as ExecuteAppFunctionResponse.Error).error)
            .isInstanceOf(AppFunctionFunctionNotFoundException::class.java)
    }

    @Test
    fun testExecuteAppFunction_functionSucceed() {
        val request =
            ExecuteAppFunctionRequest(
                targetPackageName = context.packageName,
                functionIdentifier =
                    AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_EXECUTION_SUCCEED,
                functionParameters = AppFunctionData.EMPTY,
            )

        val response = runBlocking { appFunctionManagerCompat.executeAppFunction(request) }

        assertThat(response).isInstanceOf(ExecuteAppFunctionResponse.Success::class.java)
        assertThat(
                (response as ExecuteAppFunctionResponse.Success)
                    .returnValue
                    .getString(ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE)
            )
            .isEqualTo("result")
    }

    @Test
    fun testExecuteAppFunction_functionFail() {
        val request =
            ExecuteAppFunctionRequest(
                targetPackageName = context.packageName,
                functionIdentifier =
                    AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_EXECUTION_FAIL,
                functionParameters = AppFunctionData.EMPTY,
            )

        val response = runBlocking { appFunctionManagerCompat.executeAppFunction(request) }

        assertThat(response).isInstanceOf(ExecuteAppFunctionResponse.Error::class.java)
        assertThat((response as ExecuteAppFunctionResponse.Error).error)
            .isInstanceOf(AppFunctionInvalidArgumentException::class.java)
    }
}
