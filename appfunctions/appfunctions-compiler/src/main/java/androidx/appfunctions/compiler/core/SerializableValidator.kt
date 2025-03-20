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

package androidx.appfunctions.compiler.core

import androidx.appfunctions.compiler.core.AppFunctionTypeReference.Companion.SUPPORTED_TYPES_STRING
import androidx.appfunctions.compiler.core.AppFunctionTypeReference.Companion.isSupportedType
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Visibility

/**
 * Validates if the provided [classToValidate] conforms to the AppFunctions SDK's serializable
 * specification.
 */
class SerializableValidator(private val classToValidate: KSClassDeclaration) {

    /**
     * Validates if the provided [classToValidate] conforms to the AppFunctions SDK's serializable
     * specification.
     *
     * A valid serializable class must:
     * 1. Have a public primary constructor with at least one parameter.
     * 2. All primary constructor parameters must be `val` properties.
     * 3. Parameters must be one of the following types:
     * - Primitive singles: `String`, `Int`, `Long`, `Float`, `Double`, `Boolean`, `PendingIntent`
     * - Primitive arrays: `IntArray`, `LongArray`, `ShortArray`, `FloatArray`, `DoubleArray`,
     *   `BooleanArray`
     * - `List<String>`
     * - `List<@AppFunctionSerializable>` (other serializable classes)
     * 4. If the class implements `@AppFunctionSerializable` or `AppFunctionSchemaCapability`, all
     *    properties defined in those interfaces/classes must be present as constructor parameters.
     */
    fun validate() {
        val validatedPrimaryConstructor = classToValidate.validateSerializablePrimaryConstructor()
        validateParameters(
            validatedPrimaryConstructor.parameters
                .associateBy { checkNotNull(it.name).toString() }
                .toMutableMap()
        )
    }

    /**
     * Finds all super types of the serializable [classToValidate] that are annotated with the
     * [androidx.appfunctions.AppFunctionSchemaCapability] annotation.
     *
     * For example, consider the following classes:
     * ```
     * @AppFunctionSchemaCapability
     * public interface AppFunctionOpenable {
     *     public val intentToOpen: PendingIntent
     * }
     *
     * public interface OpenableResponse : AppFunctionOpenable {
     *     override val intentToOpen: PendingIntent
     * }
     *
     * @AppFunctionSerializable
     * class MySerializableClass(
     *   override val intentToOpen: PendingIntent
     * ) : OpenableResponse
     * ```
     *
     * This method will return the [KSClassDeclaration] of `AppFunctionOpenable` since it is a super
     * type of `MySerializableClass` and is annotated with the
     * [androidx.appfunctions.AppFunctionSchemaCapability] annotation.
     *
     * @return a set of [KSClassDeclaration] for all super types of the [classToValidate] that are
     *   annotated with [androidx.appfunctions.AppFunctionSchemaCapability].
     */
    fun findSuperTypesWithCapabilityAnnotation(): Set<KSClassDeclaration> {
        return buildSet {
            val unvisitedSuperTypes: MutableList<KSTypeReference> =
                classToValidate.superTypes.toMutableList()

            while (!unvisitedSuperTypes.isEmpty()) {
                val superTypeClassDeclaration =
                    unvisitedSuperTypes.removeLast().resolve().declaration as KSClassDeclaration
                if (
                    superTypeClassDeclaration.annotations.findAnnotation(
                        IntrospectionHelper.AppFunctionSchemaCapability.CLASS_NAME
                    ) != null
                ) {
                    add(superTypeClassDeclaration)
                }
                if (
                    superTypeClassDeclaration.annotations.findAnnotation(
                        IntrospectionHelper.AppFunctionSerializableAnnotation.CLASS_NAME
                    ) == null
                ) {
                    // Only consider non serializable super types since serializable super types
                    // are already handled separately
                    unvisitedSuperTypes.addAll(superTypeClassDeclaration.superTypes)
                }
            }
        }
    }

    /**
     * Finds all super types of the serializable [classToValidate] that are annotated with the
     * [androidx.appfunctions.AppFunctionSerializable] annotation.
     *
     * For example, consider the following classes:
     * ```
     * @AppFunctionSerializable
     * open class Address (
     *     open val street: String,
     *     open val city: String,
     *     open val state: String,
     *     open val zipCode: String,
     * )
     *
     * @AppFunctionSerializable
     * class MySerializableClass(
     *     override val street: String,
     *     override val city: String,
     *     override val state: String,
     *     override val zipCode: String,
     * ) : Address
     * ```
     *
     * This method will return the [KSClassDeclaration] of `Address` since it is a super type of
     * `MySerializableClass` and is annotated with the
     * [androidx.appfunctions.AppFunctionSerializable] annotation.
     *
     * @return a set of [KSClassDeclaration] for all super types of the [classToValidate] that are
     *   annotated with [androidx.appfunctions.AppFunctionSerializable].
     */
    fun findSuperTypesWithSerializableAnnotation(): Set<KSClassDeclaration> {
        return classToValidate.superTypes
            .map { it.resolve().declaration as KSClassDeclaration }
            .filter {
                it.annotations.findAnnotation(
                    IntrospectionHelper.AppFunctionSerializableAnnotation.CLASS_NAME
                ) != null
            }
            .toSet()
    }

    /**
     * Validates that the super types of the serializable [classToValidate] are valid.
     *
     * A super type of a serializable class must be annotated with either
     * [androidx.appfunctions.AppFunctionSchemaCapability] or
     * [androidx.appfunctions.AppFunctionSerializable]. A class cannot be annotated with both
     * annotations.
     *
     * @param superTypesWithSerializableAnnotation a set of [KSClassDeclaration] for all super types
     *   of the [classToValidate] that are annotated with
     *   [androidx.appfunctions.AppFunctionSerializable].
     * @param superTypesWithCapabilityAnnotation a set of [KSClassDeclaration] for all super types
     *   of the [classToValidate] that are annotated with
     *   [androidx.appfunctions.AppFunctionSchemaCapability].
     */
    private fun validateSuperTypes(
        superTypesWithSerializableAnnotation: Set<KSClassDeclaration>,
        superTypesWithCapabilityAnnotation: Set<KSClassDeclaration>
    ) {
        if (
            superTypesWithSerializableAnnotation
                .intersect(superTypesWithCapabilityAnnotation)
                .isNotEmpty()
        ) {
            throw ProcessingException(
                "A class cannot be annotated with both @AppFunctionSerializable and " +
                    "@AppFunctionSchemaCapability.",
                classToValidate
            )
        }
    }

    private fun validateParameters(
        parametersToValidate: MutableMap<String, KSValueParameter>,
    ) {
        val superTypesWithSerializableAnnotation = findSuperTypesWithSerializableAnnotation()
        val superTypesWithCapabilityAnnotation = findSuperTypesWithCapabilityAnnotation()
        validateSuperTypes(superTypesWithSerializableAnnotation, superTypesWithCapabilityAnnotation)
        for (superType in superTypesWithSerializableAnnotation) {
            if (
                superType.annotations.findAnnotation(
                    IntrospectionHelper.AppFunctionSerializableAnnotation.CLASS_NAME
                ) == null
            ) {
                throw ProcessingException(
                    "Expected supertype with @AppFunctionSerializable annotation.",
                    superType
                )
            }
            val superTypePrimaryConstructor = superType.validateSerializablePrimaryConstructor()

            for (superTypeParameter in superTypePrimaryConstructor.parameters) {
                // Parameter has now been visited
                val parameterInSuperType =
                    parametersToValidate.remove(superTypeParameter.name.toString())
                if (parameterInSuperType == null) {
                    throw ProcessingException(
                        "All parameters in @AppFunctionSerializable " +
                            "supertypes must be present in subtype",
                        superTypeParameter
                    )
                }
                validateSerializableParameter(parameterInSuperType)
            }
        }

        for (superType in superTypesWithCapabilityAnnotation) {
            if (
                superType.annotations.findAnnotation(
                    IntrospectionHelper.AppFunctionSchemaCapability.CLASS_NAME
                ) == null
            ) {
                throw ProcessingException(
                    "Expected supertype with @AppFunctionSchemaCapability annotation.",
                    superType
                )
            }
            val capabilityProperties = superType.getDeclaredProperties()

            for (superTypeProperty in capabilityProperties) {
                // Parameter has now been visited
                val parameterInSuperType =
                    parametersToValidate.remove(superTypeProperty.simpleName.toString())
                if (parameterInSuperType == null) {
                    throw ProcessingException(
                        "All Properties in @AppFunctionSchemaCapability " +
                            "supertypes must be present in subtype",
                        superTypeProperty
                    )
                }
                validateSerializableParameter(parameterInSuperType)
            }
        }

        // Validate the remaining parameters
        if (parametersToValidate.isNotEmpty()) {
            for ((_, parameterToValidate) in parametersToValidate) {
                validateSerializableParameter(parameterToValidate)
            }
        }
    }

    private fun KSClassDeclaration.validateSerializablePrimaryConstructor(): KSFunctionDeclaration {
        if (primaryConstructor == null) {
            throw ProcessingException(
                "A valid AppFunctionSerializable must have a primary constructor.",
                this
            )
        }
        val primaryConstructorDeclaration = checkNotNull(primaryConstructor)
        if (primaryConstructorDeclaration.parameters.isEmpty()) {
            throw ProcessingException(
                "A valid AppFunctionSerializable must have a non-empty primary constructor.",
                this
            )
        }

        if (primaryConstructorDeclaration.getVisibility() != Visibility.PUBLIC) {
            throw ProcessingException(
                "A valid AppFunctionSerializable must have  a public primary constructor.",
                this
            )
        }
        return primaryConstructorDeclaration
    }

    private fun validateSerializableParameter(ksValueParameter: KSValueParameter) {
        if (!ksValueParameter.isVal) {
            throw ProcessingException(
                "All parameters in @AppFunctionSerializable primary constructor must have getters",
                ksValueParameter
            )
        }

        if (!isSupportedType(ksValueParameter.type)) {
            throw ProcessingException(
                "AppFunctionSerializable properties must be one of the following types:\n" +
                    SUPPORTED_TYPES_STRING +
                    ", an @AppFunctionSerializable or a list of @AppFunctionSerializable\nbut found " +
                    ksValueParameter.type.toTypeName(),
                ksValueParameter
            )
        }
    }
}
