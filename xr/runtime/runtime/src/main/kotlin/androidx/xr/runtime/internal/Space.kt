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

package androidx.xr.runtime.internal

import androidx.annotation.RestrictTo

/**
 * Describes a well-known coordinate system that is available for [anchors][Anchor] to be attached
 * to.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface Space : Trackable {
    /** Describes the origin and extents of the well-known coordinate system. */
    public class Type private constructor(private val name: Int) {
        public companion object {
            /**
             * A world-locked origin useful when an application needs to render seated-scale content
             * that is not positioned relative to the physical floor.
             */
            @JvmField public val Local: Type = Type(0)

            /**
             * Similar to [LOCAL] but with a different height/Y coordinate. Matches [STAGE] but with
             * potentially reduced bounds.
             */
            @JvmField public val LocalFloor: Type = Type(1)
        }
    }
}
