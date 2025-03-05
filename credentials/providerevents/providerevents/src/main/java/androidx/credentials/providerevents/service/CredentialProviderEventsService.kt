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

package androidx.credentials.providerevents.service

import android.app.Service
import android.content.Intent
import android.os.CancellationSignal
import android.os.IBinder
import androidx.annotation.RestrictTo
import androidx.core.os.OutcomeReceiverCompat
import androidx.credentials.CreateCredentialRequest
import androidx.credentials.CreateCredentialResponse
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.provider.CallingAppInfo
import androidx.credentials.providerevents.internal.CredentialEventsProviderFactory

/** Service to be extended by credential providers to receive credential updates and events */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public abstract class CredentialProviderEventsService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        if (intent == null) {
            return null
        }
        val provider =
            CredentialEventsProviderFactory(applicationContext).getBestAvailableProvider(intent)
        if (provider == null) {
            return null
        }
        return provider.getStubImplementation(this)
    }

    /**
     * Credential provider must extend this method in order to receive credential creation requests
     */
    public open fun onCreateCredentialRequest(
        request: CreateCredentialRequest,
        callingAppInfo: CallingAppInfo,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiverCompat<CreateCredentialResponse, CreateCredentialException>
    ) {}
}
