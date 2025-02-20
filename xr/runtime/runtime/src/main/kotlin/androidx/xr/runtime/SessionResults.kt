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

package androidx.xr.runtime

import androidx.annotation.RestrictTo

/** Result of a [Session.create] call. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) public sealed class SessionCreateResult

/**
 * Result of a successful [Session.create] call.
 *
 * @property session the [Session] that was created.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class SessionCreateSuccess(public val session: Session) : SessionCreateResult()

/**
 * Result of an unsuccessful [Session.create] call. The session was not created due to the required
 * [permissions] not being granted.
 *
 * @property permissions the permissions that were not granted.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class SessionCreatePermissionsNotGranted(public val permissions: List<String>) :
    SessionCreateResult()

/** Result of a [Session.configure] call. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) public sealed class SessionConfigureResult

/** Result of a successful [Session.configure] call. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class SessionConfigureSuccess() : SessionConfigureResult()

/**
 * Result of an unsuccessful [Session.configure] call. The session was not configured due to the
 * given [SessionConfig] not being supported.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class SessionConfigureConfigurationNotSupported() : SessionConfigureResult()

/** Result of a [Session.resume] call. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) public sealed class SessionResumeResult

/** Result of a successful [Session.resume] call. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class SessionResumeSuccess() : SessionResumeResult()

/**
 * Result of an unsuccessful [Session.resume] call. The session was not resumed due to the required
 * [permissions] not being granted.
 *
 * @property permissions the permissions that were not granted.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class SessionResumePermissionsNotGranted(public val permissions: List<String>) :
    SessionResumeResult()
