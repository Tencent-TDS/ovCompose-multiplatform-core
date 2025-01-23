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
 * Represents an item type metadata.
 * *
 * For examples:
 * ```
 * val property: List<Int?>
 * ```
 *
 * `Int?` is an item type that can be represented as:
 * ```
 * AppFunctionItemTypeMetadata(
 *   isNullable = true,
 *   dataType = AppFunctionDataTypeMetadata(AppFunctionDataTypes.INT),
 * )
 * ```
 */
@Document
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AppFunctionItemTypeMetadata
internal constructor(
    @Document.Namespace public val namespace: String,
    @Document.Id public val id: String,
    /** Indicates whether the parameter is nullable. */
    @Document.BooleanProperty public val isNullable: Boolean,
    /** The data type of the parameter. */
    @Document.DocumentProperty public val dataType: AppFunctionDataTypeMetadata? = null,
    /** The reference data type of the parameter. */
    @Document.StringProperty public val referenceDataType: String? = null,
) {
    /**
     * @param isNullable Indicates whether the parameter is nullable.
     * @param dataType The data type of the parameter.
     * @param referenceDataType The reference data type of the parameter.
     */
    public constructor(
        isNullable: Boolean,
        dataType: AppFunctionDataTypeMetadata? = null,
        referenceDataType: String? = null,
    ) : this(APP_FUNCTION_NAMESPACE, APP_FUNCTION_ID_EMPTY, isNullable, dataType, referenceDataType)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AppFunctionItemTypeMetadata) return false

        return this.namespace == other.namespace &&
            this.id == other.id &&
            this.isNullable == other.isNullable &&
            this.dataType == other.dataType &&
            this.referenceDataType == other.referenceDataType
    }

    override fun hashCode(): Int {
        return Objects.hash(namespace, id, isNullable, dataType, referenceDataType)
    }

    override fun toString(): String {
        return "AppFunctionTypeParameterMetadata(namespace=$namespace, " +
            "id=$id, " +
            "isNullable=$isNullable, " +
            "dataType=$dataType, " +
            "referenceDataType=$referenceDataType)"
    }
}
