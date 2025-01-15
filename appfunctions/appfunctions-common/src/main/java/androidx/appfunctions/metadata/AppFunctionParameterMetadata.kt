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
/**
 * Represent a parameter's metadata.
 *
 * Example:
 * ```
 * // param1 and param2 are value parameters
 * fun sampleFunction(param1: Int, param2: String) { ... }
 *
 * // property1 is a parameter in constructor
 * class SampleClass(val property1: String)
 * ```
 */
@Document
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AppFunctionParameterMetadata
internal constructor(
    @Document.Namespace public val namespace: String,
    @Document.Id public val id: String,
    /** The name of the parameter. */
    @Document.StringProperty public val name: String,
    /** Indicates whether the parameter is required. */
    @Document.StringProperty public val isRequired: Boolean,
    /** The data type of the parameter. */
    @Document.DocumentProperty public val dataType: AppFunctionDataTypeMetadata? = null,
    /** The reference data type of the parameter. */
    @Document.StringProperty public val referenceDataType: String? = null,
) {
    /**
     * @param name The name of the parameter.
     * @param isRequired Indicates whether the parameter is required.
     * @param dataType The data type of the parameter.
     * @param referenceDataType The reference data type of the parameter.
     */
    public constructor(
        name: String,
        isRequired: Boolean,
        dataType: AppFunctionDataTypeMetadata? = null,
        referenceDataType: String? = null,
    ) : this(
        APP_FUNCTION_NAMESPACE,
        APP_FUNCTION_ID_EMPTY,
        name,
        isRequired,
        dataType,
        referenceDataType
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AppFunctionParameterMetadata) return false

        return this.namespace == other.namespace &&
            this.id == other.id &&
            this.isRequired == other.isRequired &&
            this.dataType == other.dataType &&
            this.referenceDataType == other.referenceDataType
    }

    override fun hashCode(): Int {
        return Objects.hash(namespace, id, isRequired, dataType, referenceDataType)
    }

    override fun toString(): String {
        return "AppFunctionValueParameterMetadata(namespace=$namespace, " +
            "id=$id, " +
            "isRequired=$isRequired, " +
            "dataType=$dataType, " +
            "referenceDataType=$referenceDataType)"
    }
}
