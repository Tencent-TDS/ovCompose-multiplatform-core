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

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.appsearch.annotation.Document
import java.util.Objects

@IntDef(
    AppFunctionDataTypeMetadata.UNIT,
    AppFunctionDataTypeMetadata.BOOLEAN,
    AppFunctionDataTypeMetadata.BYTES,
    AppFunctionDataTypeMetadata.OBJECT,
    AppFunctionDataTypeMetadata.DOUBLE,
    AppFunctionDataTypeMetadata.FLOAT,
    AppFunctionDataTypeMetadata.LONG,
    AppFunctionDataTypeMetadata.INT,
    AppFunctionDataTypeMetadata.STRING,
    AppFunctionDataTypeMetadata.PENDING_INTENT,
    AppFunctionDataTypeMetadata.ARRAY
)
@Retention(AnnotationRetention.SOURCE)
internal annotation class AppFunctionDataType

// TODO: Make it public once API surface is finalize
/**
 * The metadata of a data type used by AppFunction.
 *
 * This class describes the type and the structure of data used by an AppFunction, such as input or
 * output parameters.
 */
@Document
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AppFunctionDataTypeMetadata
internal constructor(
    @Document.Namespace public val namespace: String,
    /** The id of the data type. */
    @Document.Id public val id: String,
    /** The data type such as [AppFunctionDataTypeMetadata.INT]. */
    @Document.LongProperty @AppFunctionDataType public val type: Int,
    /**
     * If the [type] is [AppFunctionDataTypeMetadata.ARRAY], this specifies the array content data
     * type.
     */
    @Document.DocumentProperty public val itemType: AppFunctionItemTypeMetadata?,
    /**
     * If the [type] is [AppFunctionDataTypeMetadata.OBJECT], this specified the object's
     * properties.
     */
    @Document.DocumentProperty public val properties: List<AppFunctionParameterMetadata>?,
    /**
     * If the [type] is [AppFunctionDataTypeMetadata.OBJECT], this specifies the expected schemaType
     * of the AppSearch document.
     */
    @Document.StringProperty public val documentSchemaType: String?,
) {
    /**
     * @param id The id of the data schema.
     * @param type The data type.
     * @param itemType The array item type.
     * @param properties The object's properties.
     * @param documentSchemaType The expected schemaType of the AppSearch document.
     */
    public constructor(
        @AppFunctionDataType type: Int,
        id: String = APP_FUNCTION_ID_EMPTY,
        itemType: AppFunctionItemTypeMetadata? = null,
        properties: List<AppFunctionParameterMetadata>? = null,
        documentSchemaType: String? = null,
    ) : this(APP_FUNCTION_NAMESPACE, id, type, itemType, properties, documentSchemaType)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AppFunctionDataTypeMetadata) return false

        return this.namespace == other.namespace &&
            this.id == other.id &&
            this.type == other.type &&
            this.documentSchemaType == other.documentSchemaType &&
            this.itemType == other.itemType &&
            this.properties == other.properties
    }

    override fun hashCode(): Int {
        return Objects.hash(namespace, id, type, documentSchemaType, itemType, properties)
    }

    override fun toString(): String {
        return "AppFunctionDataSchema(namespace=$namespace, " +
            "id=$id, " +
            "type=$type, " +
            "documentSchemaType=$documentSchemaType, " +
            "itemType=$itemType, " +
            "properties=$properties)"
    }

    public companion object {
        public const val UNIT: Int = 0
        public const val BOOLEAN: Int = 1
        public const val BYTES: Int = 2
        public const val OBJECT: Int = 3
        public const val DOUBLE: Int = 4
        public const val FLOAT: Int = 5
        public const val LONG: Int = 6
        public const val INT: Int = 7
        public const val STRING: Int = 8
        public const val PENDING_INTENT: Int = 9
        public const val ARRAY: Int = 10
    }
}
