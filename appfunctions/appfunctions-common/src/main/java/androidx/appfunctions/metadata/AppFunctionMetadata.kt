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

internal const val APP_FUNCTION_NAMESPACE = "appfunctions"
internal const val APP_FUNCTION_ID_EMPTY = "unused"

// TODO: Make it public once API surface is finalize
/**
 * Represents an AppFunction's metadata.
 *
 * The class provides the essential information to call an AppFunction. The caller has two options
 * to invoke a function:
 * * Using function schema to identify input/output: The function schema defines the input and
 *   output of a function. If [schema] is not null, the caller can look up the input/output
 *   information based on the schema definition, and call the function accordingly.
 * * Examine [parameters] and [response]: A function metadata also has parameters and response
 *   properties describe the input and output of a function. The caller can examine these fields to
 *   obtain the input/output information, and call the function accordingly.
 */
@Document
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AppFunctionMetadata
internal constructor(
    @Document.Namespace public val namespace: String,
    /** The id of the AppFunction. */
    @Document.Id public val id: String,
    /**
     * Indicates whether the function is enabled by default.
     *
     * This represents the initial configuration and might not represent the current enabled state,
     * as it could be modified at runtime.
     */
    @Document.BooleanProperty public val isEnabledByDefault: Boolean,
    /**
     * Restricts the function to trusted callers only.
     *
     * Trusted callers are the callers with [android.permission.EXECUTE_APPFUNCTIONS_TRUSTED]
     * permission. Only the applications with privacy guarantees from the system can hold the
     * permission.
     *
     * @see android.permission.EXECUTE_APP_FUNCTIONS_TRUSTED
     */
    @Document.BooleanProperty public val isRestrictToTrustedCaller: Boolean,
    /** The display name of the AppFunction. */
    @Document.LongProperty public val displayNameRes: Long?,
    /** The predefined schema of the AppFunction. */
    @Document.DocumentProperty public val schema: AppFunctionSchemaMetadata?,
    /** The parameters of the AppFunction. */
    @Document.DocumentProperty public val parameters: List<AppFunctionParameterMetadata>,
    /** The response of the AppFunction. */
    @Document.DocumentProperty public val response: AppFunctionResponseMetadata,
    /** The reusable components for the AppFunction. */
    @Document.DocumentProperty public val components: AppFunctionComponentsMetadata,
) {
    /**
     * @param id The id of the AppFunction.
     * @param isEnabledByDefault Indicates whether the function is enabled by default.
     * @param isRestrictToTrustedCaller Indicates whether the function is restricted to trusted
     *   caller.
     * @param displayNameRes The string resource for function's display name.
     * @param schema The schema of the AppFunction.
     * @param parameters The parameters of the AppFunction.
     * @param response The response of the AppFunction.
     * @param components The reusable components for the AppFunction.
     */
    public constructor(
        id: String,
        isEnabledByDefault: Boolean,
        isRestrictToTrustedCaller: Boolean,
        displayNameRes: Long?,
        schema: AppFunctionSchemaMetadata?,
        parameters: List<AppFunctionParameterMetadata>,
        response: AppFunctionResponseMetadata,
        components: AppFunctionComponentsMetadata
    ) : this(
        APP_FUNCTION_NAMESPACE,
        id,
        isEnabledByDefault,
        isRestrictToTrustedCaller,
        displayNameRes,
        schema,
        parameters,
        response,
        components
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AppFunctionMetadata) return false

        return this.namespace == other.namespace &&
            this.id == other.id &&
            this.isEnabledByDefault == other.isEnabledByDefault &&
            this.isRestrictToTrustedCaller == other.isRestrictToTrustedCaller &&
            this.displayNameRes == other.displayNameRes &&
            this.schema == other.schema &&
            this.parameters == other.parameters &&
            this.response == other.response &&
            this.components == other.components
    }

    override fun hashCode(): Int {
        return Objects.hash(
            namespace,
            id,
            isEnabledByDefault,
            isRestrictToTrustedCaller,
            displayNameRes,
            schema,
            parameters,
            response,
            components,
        )
    }

    override fun toString(): String {
        return "AppFunctionMetadata(namespace=$namespace, " +
            "id=$id, " +
            "isEnabledByDefault=$isEnabledByDefault, " +
            "isRestrictToTrustedCaller=$isRestrictToTrustedCaller, " +
            "displayNameRes=$displayNameRes, " +
            "schema=$schema, " +
            "parameters=$parameters, " +
            "response=$response, " +
            "components=$components)"
    }
}
