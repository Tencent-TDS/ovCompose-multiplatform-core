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
import android.os.OutcomeReceiver
import androidx.annotation.RequiresApi
import androidx.appfunctions.internal.AggregatedAppFunctionInventory
import androidx.appfunctions.internal.AggregatedAppFunctionInvoker
import androidx.appfunctions.internal.AppFunctionInventory
import androidx.appfunctions.internal.AppFunctionInvoker
import androidx.appfunctions.metadata.AppFunctionMetadata
import androidx.appfunctions.metadata.AppFunctionParameterMetadata
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata.Companion.TYPE_LONG
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata.Companion.TYPE_STRING
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata.Companion.TYPE_UNIT
import androidx.appfunctions.metadata.AppFunctionResponseMetadata
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
@OptIn(ExperimentalCoroutinesApi::class)
class AppFunctionServiceDelegateTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var context: Context

    private lateinit var fakeAggregatedInvoker: FakeAggregatedInvoker

    private lateinit var fakeAggregatedInventory: FakeAggregatedInventory

    private lateinit var delegate: AppFunctionServiceDelegate

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context
        fakeAggregatedInvoker = FakeAggregatedInvoker()
        fakeAggregatedInventory = FakeAggregatedInventory()
        delegate =
            AppFunctionServiceDelegate(
                context,
                testDispatcher,
                testDispatcher,
                fakeAggregatedInventory,
                fakeAggregatedInvoker,
            )
    }

    @Test
    fun testOnExecuteFunction_functionNotExist() {
        val request =
            ExecuteAppFunctionRequest(
                targetPackageName = context.packageName,
                functionIdentifier = "fakeFunctionId",
                functionParameters = AppFunctionData.EMPTY
            )

        assertThrows(AppFunctionFunctionNotFoundException::class.java) {
            runBlocking { executeFunctionBlocking(request) }
        }
    }

    @Test
    fun testOnExecuteFunction_invalidParameter() {
        fakeAggregatedInventory.setAppFunctionMetadata(
            AppFunctionMetadata(
                id = "invaliadParameterFunction",
                isEnabledByDefault = true,
                schema = null,
                parameters =
                    listOf(
                        AppFunctionParameterMetadata(
                            name = "requiredLong",
                            isRequired = true,
                            dataType =
                                AppFunctionPrimitiveTypeMetadata(
                                    type = TYPE_LONG,
                                    isNullable = false,
                                )
                        )
                    ),
                response =
                    AppFunctionResponseMetadata(
                        valueType =
                            AppFunctionPrimitiveTypeMetadata(
                                type = TYPE_UNIT,
                                isNullable = false,
                            )
                    )
            )
        )
        val request =
            ExecuteAppFunctionRequest(
                targetPackageName = context.packageName,
                functionIdentifier = "invaliadParameterFunction",
                // Missing requiredLong from the parameter
                functionParameters = AppFunctionData.EMPTY
            )

        assertThrows(AppFunctionInvalidArgumentException::class.java) {
            runBlocking { executeFunctionBlocking(request) }
        }
    }

    @Test
    fun testOnExecuteFunction_buildReturnValueFail() {
        fakeAggregatedInventory.setAppFunctionMetadata(
            AppFunctionMetadata(
                id = "returnIncorrectResultFunction",
                isEnabledByDefault = true,
                schema = null,
                parameters = listOf(),
                response =
                    AppFunctionResponseMetadata(
                        valueType =
                            AppFunctionPrimitiveTypeMetadata(
                                type = TYPE_LONG,
                                isNullable = false,
                            )
                    )
            )
        )
        // Returns String instead of Long
        fakeAggregatedInvoker.setAppFunctionResult("returnIncorrectResultFunction") { "TestString" }
        val request =
            ExecuteAppFunctionRequest(
                targetPackageName = context.packageName,
                functionIdentifier = "returnIncorrectResultFunction",
                functionParameters = AppFunctionData.EMPTY
            )

        assertThrows(AppFunctionAppUnknownException::class.java) {
            runBlocking { executeFunctionBlocking(request) }
        }
    }

    @Test
    fun testOnExecuteFunction_succeed_noArg() {
        fakeAggregatedInventory.setAppFunctionMetadata(
            AppFunctionMetadata(
                id = "succeedFunction",
                isEnabledByDefault = true,
                schema = null,
                parameters = listOf(),
                response =
                    AppFunctionResponseMetadata(
                        valueType =
                            AppFunctionPrimitiveTypeMetadata(
                                type = TYPE_STRING,
                                isNullable = false,
                            )
                    )
            )
        )
        fakeAggregatedInvoker.setAppFunctionResult("succeedFunction") { "TestString" }
        val request =
            ExecuteAppFunctionRequest(
                targetPackageName = context.packageName,
                functionIdentifier = "succeedFunction",
                functionParameters = AppFunctionData.EMPTY
            )

        val response = runBlocking { executeFunctionBlocking(request) }

        assertThat(response).isInstanceOf(ExecuteAppFunctionResponse.Success::class.java)
        assertThat(
                (response as ExecuteAppFunctionResponse.Success)
                    .returnValue
                    .getString(ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE)
            )
            .isEqualTo("TestString")
    }

    @Test
    fun testOnExecuteFunction_succeed_withArg() {
        fakeAggregatedInventory.setAppFunctionMetadata(
            AppFunctionMetadata(
                id = "succeedFunction",
                isEnabledByDefault = true,
                schema = null,
                parameters =
                    listOf(
                        AppFunctionParameterMetadata(
                            name = "testArg",
                            isRequired = true,
                            dataType =
                                AppFunctionPrimitiveTypeMetadata(
                                    type = TYPE_LONG,
                                    isNullable = false,
                                )
                        )
                    ),
                response =
                    AppFunctionResponseMetadata(
                        valueType =
                            AppFunctionPrimitiveTypeMetadata(
                                type = TYPE_STRING,
                                isNullable = false,
                            )
                    )
            )
        )
        fakeAggregatedInvoker.setAppFunctionResult("succeedFunction") { "TestString" }
        val request =
            ExecuteAppFunctionRequest(
                targetPackageName = context.packageName,
                functionIdentifier = "succeedFunction",
                functionParameters = AppFunctionData.Builder("").setLong("testArg", 100L).build()
            )

        val response = runBlocking { executeFunctionBlocking(request) }

        assertThat(response).isInstanceOf(ExecuteAppFunctionResponse.Success::class.java)
        assertThat(
                (response as ExecuteAppFunctionResponse.Success)
                    .returnValue
                    .getString(ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE)
            )
            .isEqualTo("TestString")
    }

    private suspend fun executeFunctionBlocking(
        request: ExecuteAppFunctionRequest,
    ): ExecuteAppFunctionResponse = suspendCancellableCoroutine { cont ->
        delegate.onExecuteFunction(
            request,
            "test",
            object : OutcomeReceiver<ExecuteAppFunctionResponse, AppFunctionException> {
                override fun onResult(result: ExecuteAppFunctionResponse) {
                    cont.resume(result)
                }

                override fun onError(e: AppFunctionException) {
                    cont.resumeWithException(e)
                }
            },
        )
    }

    private class FakeAggregatedInvoker : AggregatedAppFunctionInvoker() {

        private val internalInvoker =
            object : AppFunctionInvoker {

                private val functionResultMap = mutableMapOf<String, () -> Any?>()

                override val supportedFunctionIds: Set<String>
                    get() = functionResultMap.keys

                override suspend fun unsafeInvoke(
                    appFunctionContext: AppFunctionContext,
                    functionIdentifier: String,
                    parameters: Map<String, Any?>
                ): Any? {
                    return functionResultMap[functionIdentifier]?.invoke()
                }

                fun setAppFunctionResult(functionId: String, result: () -> Any?) {
                    functionResultMap[functionId] = result
                }
            }

        override val invokers: List<AppFunctionInvoker>
            get() = listOf(internalInvoker)

        fun setAppFunctionResult(functionId: String, result: () -> Any?) {
            internalInvoker.setAppFunctionResult(functionId, result)
        }
    }

    private class FakeAggregatedInventory : AggregatedAppFunctionInventory() {

        private val internalInventory =
            object : AppFunctionInventory {

                private val internalMap = mutableMapOf<String, AppFunctionMetadata>()

                override val functionIdToMetadataMap: Map<String, AppFunctionMetadata>
                    get() = internalMap

                fun setAppFunctionMetadata(metadata: AppFunctionMetadata) {
                    internalMap[metadata.id] = metadata
                }
            }

        override val inventories: List<AppFunctionInventory>
            get() = listOf(internalInventory)

        fun setAppFunctionMetadata(metadata: AppFunctionMetadata) {
            internalInventory.setAppFunctionMetadata(metadata)
        }
    }
}
