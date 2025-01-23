/*
 * Copyright 2024 The Android Open Source Project
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

package com.android.extensions.xr;

import androidx.annotation.RestrictTo;

/** Represents a result of an asynchronous XR Extension call. */
@SuppressWarnings({"unchecked", "deprecation", "all"})
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class XrExtensionResult {

    XrExtensionResult() {
        throw new RuntimeException("Stub!");
    }

    /** Returns the result. */
    public int getResult() {
        throw new RuntimeException("Stub!");
    }

    /**
     * @deprecated Renamed. Use XR_RESULT_ERROR_NOT_ALLOWED.
     */
    @Deprecated public static final int XR_RESULT_ERROR_IGNORED = 3; // 0x3

    /**
     * @deprecated Renamed. Use XR_RESULT_IGNORED_ALREADY_APPLIED.
     */
    @Deprecated public static final int XR_RESULT_ERROR_INVALID_STATE = 2; // 0x2

    /**
     * The asynchronous call has been rejected by the system service because the caller activity
     * does not have the required capability.
     */
    public static final int XR_RESULT_ERROR_NOT_ALLOWED = 3; // 0x3

    /**
     * The asynchronous call cannot be sent to the system service, or the service cannot properly
     * handle the request. This is not a recoverable error for the client. For example, this error
     * is sent to the client when an asynchronous call attempt has failed with a RemoteException.
     */
    public static final int XR_RESULT_ERROR_SYSTEM = 4; // 0x4

    /**
     * The asynchronous call has been ignored by the system service because the caller activity is
     * already in the requested state.
     */
    public static final int XR_RESULT_IGNORED_ALREADY_APPLIED = 2; // 0x2

    /**
     * The asynchronous call has been accepted by the system service, and an immediate state change
     * is expected.
     */
    public static final int XR_RESULT_SUCCESS = 0; // 0x0

    /**
     * The asynchronous call has been accepted by the system service, but the caller activity's
     * spatial state won't be changed until other condition(s) are met.
     */
    public static final int XR_RESULT_SUCCESS_NOT_VISIBLE = 1; // 0x1
}
