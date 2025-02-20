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

@file:JvmName("PlatformEventSources")

package androidx.wear.protolayout.expression

import androidx.wear.protolayout.expression.DynamicBuilders.DynamicBool

/**
 * Creates a [DynamicBool] which receives the current visibility status from platform.
 *
 * The visibility status value is `true` when layout is visible, and `false` when invisible.
 */
@RequiresSchemaVersion(major = 1, minor = 500)
public fun platformVisibilityStatus(): DynamicBool =
    DynamicBuilders.StateBoolSource.Builder()
        .setSourceKey(PlatformEventKeys.VISIBILITY_STATUS.key)
        .setSourceNamespace(PlatformEventKeys.VISIBILITY_STATUS.namespace)
        .build()

/** Data sources keys for platform event. */
public object PlatformEventKeys {
    /**
     * The data source key for visibility status from platform sources. The visibility status value
     * is `true` when layout is visible, and `false` when invisible.
     */
    @JvmField
    public val VISIBILITY_STATUS: PlatformDataKey<DynamicBool> =
        PlatformDataKey<DynamicBool>("VisibilityStatus")
}
