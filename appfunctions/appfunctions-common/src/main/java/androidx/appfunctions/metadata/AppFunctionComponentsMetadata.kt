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

package androidx.appfunctions.metadata

import androidx.annotation.RestrictTo
import androidx.appsearch.annotation.Document
import java.util.Objects

// TODO: Make it public once API surface is finalize
/** Represent the reusable components for a function specification */
@Document
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AppFunctionComponentsMetadata
internal constructor(
    @Document.Namespace public val namespace: String,
    @Document.Id public val id: String,
    /** The list of common [AppFunctionDataTypeMetadata] that can be used to call an AppFunction. */
    @Document.DocumentProperty public val dataTypes: List<AppFunctionDataTypeMetadata>,
) {
    /**
     * @param dataTypes The list of common [AppFunctionDataTypeMetadata] that can be used for
     *   function calling.
     */
    public constructor(
        dataTypes: List<AppFunctionDataTypeMetadata>
    ) : this(APP_FUNCTION_NAMESPACE, APP_FUNCTION_ID_EMPTY, dataTypes)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AppFunctionComponentsMetadata) return false

        return this.namespace == other.namespace &&
            this.id == other.id &&
            this.dataTypes == other.dataTypes
    }

    override fun hashCode(): Int {
        return Objects.hash(namespace, id, dataTypes)
    }

    override fun toString(): String {
        return "AppFunctionComponentsMetadata(namespace=$namespace, " +
            "id=$id, " +
            "dataTypes=$dataTypes)"
    }
}
