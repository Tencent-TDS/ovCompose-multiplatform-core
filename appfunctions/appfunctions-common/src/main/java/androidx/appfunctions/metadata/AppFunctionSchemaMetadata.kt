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
 * Represent a predefined AppFunction schema.
 *
 * A schema defines the input parameters and the output of a function. This class holds the
 * identifying information about a specific schema.
 */
@Document
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AppFunctionSchemaMetadata
internal constructor(
    @Document.Namespace public val namespace: String,
    @Document.Id public val id: String,
    /** The category of the schema, used to group related schemas. */
    @Document.StringProperty public val schemaCategory: String,
    /** The unique name of the schema within its category. */
    @Document.StringProperty public val schemaName: String,
    /** The version of the schema. This is used to track the changes to the schema over time. */
    @Document.LongProperty public val schemaVersion: Long
) {
    /**
     * @param schemaCategory The category of the schema.
     * @param schemaName The unique name of the schema within its category.
     * @param schemaVersion The version of the schema.
     */
    public constructor(
        schemaCategory: String,
        schemaName: String,
        schemaVersion: Long,
    ) : this(
        APP_FUNCTION_NAMESPACE,
        APP_FUNCTION_ID_EMPTY,
        schemaCategory,
        schemaName,
        schemaVersion
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AppFunctionSchemaMetadata) return false

        return this.namespace == other.namespace &&
            this.id == other.id &&
            this.schemaCategory == other.schemaCategory &&
            this.schemaName == other.schemaName &&
            this.schemaVersion == other.schemaVersion
    }

    override fun hashCode(): Int {
        return Objects.hash(namespace, id, schemaCategory, schemaName, schemaVersion)
    }

    override fun toString(): String {
        return "AppFunctionSchemaMetadata(namespace=$namespace, " +
            "id=$id, " +
            "schemaCategory=$schemaCategory, " +
            "schemaName=$schemaName, " +
            "schemaVersion=$schemaVersion)"
    }
}
