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

package androidx.privacysandbox.ondevicepersonalization.client

import android.adservices.ondevicepersonalization.SurfacePackageToken

@Suppress("DataClassDefinition")
data class ExecuteInIsolatedServiceResponse
internal constructor(
    /**
     * Returns a {@link SurfacePackageToken}, which is an opaque reference to content that can be
     * displayed in a {@link android.view.SurfaceView}. This may be {@code null} if the {@link
     * IsolatedService} has not generated any content to be displayed within the calling app.
     */
    val surfacePackageToken: SurfacePackageToken?,
)
