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

package androidx.appfunctions.internal

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunctionData

/** Translates the request and response between formats. */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal interface Translator {
    /** Upgrades the request from old format to the jetpack format. */
    fun upgradeRequest(request: AppFunctionData): AppFunctionData

    /** Upgrades the response from the jetpack format to the old format. */
    fun upgradeResponse(response: AppFunctionData): AppFunctionData

    /** Downgrades the request from the jetpack format to the old format. */
    fun downgradeRequest(request: AppFunctionData): AppFunctionData

    /** Downgrades the response from old format to the jetpack format. */
    fun downgradeResponse(response: AppFunctionData): AppFunctionData
}
