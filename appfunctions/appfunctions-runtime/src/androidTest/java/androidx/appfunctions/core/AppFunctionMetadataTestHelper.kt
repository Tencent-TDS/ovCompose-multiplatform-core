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

package androidx.appfunctions.core

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appsearch.app.GlobalSearchSession
import androidx.appsearch.app.SearchSpec
import androidx.appsearch.platformstorage.PlatformStorage
import androidx.concurrent.futures.await
import kotlinx.coroutines.delay

@RequiresApi(Build.VERSION_CODES.S)
internal class AppFunctionMetadataTestHelper(private val context: Context) {
    suspend fun awaitAppFunctionIndexed(functionIds: Set<String>) {
        awaitRuntimeMetadataAvailable(functionIds)
    }

    private suspend fun awaitRuntimeMetadataAvailable(functionIds: Set<String>) {
        val notFoundIds = mutableSetOf<String>().apply { addAll(functionIds) }

        createSearchSession().use { session ->
            var retry = 0
            while (retry < RETRY_LIMIT) {
                val searchResults =
                    session.search(
                        "",
                        SearchSpec.Builder()
                            .addFilterNamespaces("app_functions_runtime")
                            .addFilterPackageNames("android")
                            .addFilterSchemas("AppFunctionRuntimeMetadata")
                            .build(),
                    )
                var nextPage = searchResults.nextPageAsync.await()
                while (notFoundIds.isNotEmpty() && nextPage.isNotEmpty()) {
                    for (result in nextPage) {
                        val functionId = result.genericDocument.getPropertyString("functionId")
                        if (notFoundIds.contains(functionId)) {
                            notFoundIds.remove(functionId)
                        }
                    }
                    nextPage = searchResults.nextPageAsync.await()
                }
                if (notFoundIds.isEmpty()) {
                    return
                }

                delay(RETRY_DELAY_MS)
                retry += 1
            }
        }
        throw IllegalStateException("AppSearch indexer fail")
    }

    private suspend fun createSearchSession(): GlobalSearchSession {
        return PlatformStorage.createGlobalSearchSessionAsync(
                PlatformStorage.GlobalSearchContext.Builder(context).build()
            )
            .await()
    }

    /** List of function ids defined in androidTest/res/xml/app_functions.xml */
    object FunctionIds {
        const val NO_SCHEMA_ENABLED_BY_DEFAULT =
            "androidx.appfunctions.test#noSchema_enabledByDefault"
        const val NO_SCHEMA_DISABLED_BY_DEFAULT =
            "androidx.appfunctions.test#noSchema_disabledByDefault"
        const val NO_SCHEMA_EXECUTION_SUCCEED =
            "androidx.appfunctions.test#noSchema_executionSucceed"
        const val NO_SCHEMA_EXECUTION_FAIL = "androidx.appfunctions.test#noSchema_executionFail"
    }

    companion object {
        private const val RETRY_LIMIT = 5
        private const val RETRY_DELAY_MS = 500L
    }
}
