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
import android.content.pm.SigningInfo
import android.os.Build
import android.os.OutcomeReceiver
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appfunctions.internal.AggregateAppFunctionInventory
import androidx.appfunctions.internal.AggregateAppFunctionInvoker
import androidx.appfunctions.internal.Constants.APP_FUNCTIONS_TAG
import androidx.appfunctions.internal.unsafeBuildReturnValue
import androidx.appfunctions.internal.unsafeGetParameterValue
import androidx.appfunctions.metadata.AppFunctionMetadata
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// TODO: Update the RequiresApi in AppFunctionData to be T.
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
internal class AppFunctionServiceDelegate(
    context: Context,
    workerCoroutineContext: CoroutineContext,
    private val mainCoroutineContext: CoroutineContext,
    private val aggregateInventory: AggregateAppFunctionInventory,
    private val aggregateInvoker: AggregateAppFunctionInvoker,
) {

    private val job = Job()
    private val workerCoroutineScope = CoroutineScope(workerCoroutineContext + job)
    private val appContext = context.applicationContext

    internal fun onExecuteFunction(
        executeAppFunctionRequest: ExecuteAppFunctionRequest,
        callingPackageName: String,
        callback: OutcomeReceiver<ExecuteAppFunctionResponse, AppFunctionException>,
    ): Job =
        workerCoroutineScope.launch {
            try {
                val appFunctionMetadata =
                    aggregateInventory.functionIdToMetadataMap[
                            executeAppFunctionRequest.functionIdentifier]
                if (appFunctionMetadata == null) {
                    Log.d(
                        APP_FUNCTIONS_TAG,
                        "${executeAppFunctionRequest.functionIdentifier} is not available"
                    )
                    callback.onError(
                        AppFunctionFunctionNotFoundException(
                            "${executeAppFunctionRequest.functionIdentifier} is not available"
                        )
                    )
                    return@launch
                }

                val parameters = extractParameters(executeAppFunctionRequest, appFunctionMetadata)
                callback.onResult(
                    unsafeInvokeFunction(
                        executeAppFunctionRequest,
                        callingPackageName,
                        appFunctionMetadata,
                        parameters,
                    )
                )
            } catch (e: CancellationException) {
                callback.onError(AppFunctionCancelledException(e.message))
            } catch (e: AppFunctionException) {
                callback.onError(e)
            } catch (e: Exception) {
                callback.onError(AppFunctionAppUnknownException(e.message))
            }
        }

    internal fun onDestroy() {
        job.cancel()
    }

    private fun extractParameters(
        request: ExecuteAppFunctionRequest,
        appFunctionMetadata: AppFunctionMetadata,
    ): Map<String, Any?> {
        return buildMap {
            for (parameterMetadata in appFunctionMetadata.parameters) {
                this[parameterMetadata.name] =
                    request.functionParameters.unsafeGetParameterValue(parameterMetadata)
            }
        }
    }

    private suspend fun unsafeInvokeFunction(
        request: ExecuteAppFunctionRequest,
        callingPackageName: String,
        appFunctionMetadata: AppFunctionMetadata,
        parameters: Map<String, Any?>
    ): ExecuteAppFunctionResponse {
        val result =
            withContext(mainCoroutineContext) {
                aggregateInvoker.unsafeInvoke(
                    buildAppFunctionContext(callingPackageName),
                    request.functionIdentifier,
                    parameters
                )
            }
        val returnValue = appFunctionMetadata.response.unsafeBuildReturnValue(result)
        return ExecuteAppFunctionResponse.Success(returnValue)
    }

    private fun buildAppFunctionContext(callingPackageName: String): AppFunctionContext {
        return object : AppFunctionContext {
            override val context: Context
                get() = appContext

            override val callingPackageName: String
                get() = callingPackageName

            override val callingPackageSigningInfo: SigningInfo
                get() = TODO("Not yet implemented")
        }
    }
}
