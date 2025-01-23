/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.xr.extensions;

import android.util.Log;

class XrExtensionResultImpl implements XrExtensionResult {
    private static final String TAG = "XrExtensionResultImpl";
    private final @ResultType int result;

    XrExtensionResultImpl(com.android.extensions.xr.XrExtensionResult result) {
        switch (result.getResult()) {
            case com.android.extensions.xr.XrExtensionResult.XR_RESULT_SUCCESS:
                this.result = XrExtensionResult.XR_RESULT_SUCCESS;
                break;
            case com.android.extensions.xr.XrExtensionResult.XR_RESULT_SUCCESS_NOT_VISIBLE:
                this.result = XrExtensionResult.XR_RESULT_SUCCESS_NOT_VISIBLE;
                break;
            case com.android.extensions.xr.XrExtensionResult.XR_RESULT_IGNORED_ALREADY_APPLIED:
                this.result = XrExtensionResult.XR_RESULT_IGNORED_ALREADY_APPLIED;
                break;
            case com.android.extensions.xr.XrExtensionResult.XR_RESULT_ERROR_NOT_ALLOWED:
                this.result = XrExtensionResult.XR_RESULT_ERROR_NOT_ALLOWED;
                break;
            case com.android.extensions.xr.XrExtensionResult.XR_RESULT_ERROR_SYSTEM:
                this.result = XrExtensionResult.XR_RESULT_ERROR_SYSTEM;
                break;
            default:
                // This path should never be taken.
                Log.wtf(TAG, "Unknown result: " + result);
                this.result = XrExtensionResult.XR_RESULT_ERROR_SYSTEM;
                break;
        }
    }

    @Override
    public @ResultType int getResult() {
        return result;
    }
}
