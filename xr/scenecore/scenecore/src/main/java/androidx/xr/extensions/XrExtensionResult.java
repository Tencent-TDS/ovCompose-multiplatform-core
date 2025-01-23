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

import static androidx.xr.extensions.XrExtensions.IMAGE_TOO_OLD;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;

/** Represents a result of an asynchronous XR Extension call. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface XrExtensionResult {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @IntDef(
            value = {
                XR_RESULT_SUCCESS,
                XR_RESULT_SUCCESS_NOT_VISIBLE,
                XR_RESULT_IGNORED_ALREADY_APPLIED,
                XR_RESULT_ERROR_INVALID_STATE,
                XR_RESULT_ERROR_NOT_ALLOWED,
                XR_RESULT_ERROR_IGNORED,
                XR_RESULT_ERROR_SYSTEM,
            })
    @Retention(SOURCE)
    public @interface ResultType {}

    /**
     * The asynchronous call has been accepted by the system service, and an immediate state change
     * is expected.
     */
    int XR_RESULT_SUCCESS = 0;

    /**
     * The asynchronous call has been accepted by the system service, but the caller activity's
     * spatial state won't be changed until other condition(s) are met.
     */
    int XR_RESULT_SUCCESS_NOT_VISIBLE = 1;

    /**
     * The asynchronous call has been ignored by the system service because the caller activity is
     * already in the requested state.
     */
    int XR_RESULT_IGNORED_ALREADY_APPLIED = 2;

    /**
     * @deprecated Renamed. Use XR_RESULT_IGNORED_ALREADY_APPLIED.
     */
    @Deprecated int XR_RESULT_ERROR_INVALID_STATE = 2;

    /**
     * The asynchronous call has been rejected by the system service because the caller activity
     * does not have the required capability.
     */
    int XR_RESULT_ERROR_NOT_ALLOWED = 3;

    /**
     * @deprecated Renamed. Use XR_RESULT_ERROR_NOT_ALLOWED.
     */
    @Deprecated int XR_RESULT_ERROR_IGNORED = 3;

    /**
     * The asynchronous call cannot be sent to the system service, or the service cannot properly
     * handle the request. This is not a recoverable error for the client. For example, this error
     * is sent to the client when an asynchronous call attempt has failed with a RemoteException.
     */
    int XR_RESULT_ERROR_SYSTEM = 4;

    /** Returns the result. */
    default @ResultType int getResult() {
        throw new UnsupportedOperationException(IMAGE_TOO_OLD);
    }
}
