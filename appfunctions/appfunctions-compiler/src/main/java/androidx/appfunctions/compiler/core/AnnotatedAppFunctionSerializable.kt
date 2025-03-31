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

import androidx.appfunctions.compiler.core.AppFunctionTypeReference.AppFunctionSupportedTypeCategory.SERIALIZABLE_LIST
import androidx.appfunctions.compiler.core.AppFunctionTypeReference.AppFunctionSupportedTypeCategory.SERIALIZABLE_PROXY_SINGULAR
import androidx.appfunctions.compiler.core.AppFunctionTypeReference.AppFunctionSupportedTypeCategory.SERIALIZABLE_SINGULAR
import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionSerializableAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.ClassName

/**
 * Represents a class annotated with [androidx.appfunctions.AppFunctionSerializable].
 *
 * When the serializable has type parameter (e.g. `SetField<T>`), the type arguments must be
 * provided as [arguments] to resolve the actual type reference.
 */
open class AnnotatedAppFunctionSerializable(
    private val appFunctionSerializableClass: KSClassDeclaration,
    private val arguments: List<KSTypeArgument> = emptyList()
) {
    /** A map of type parameter name to its parameterized type. */
    private val typeParameterMap: Map<String, KSTypeReference> = buildMap {
        for ((index, typeParameter) in appFunctionSerializableClass.typeParameters.withIndex()) {
            val typeParameterName = typeParameter.name.asString()
            val actualType =
                arguments.getOrNull(index)?.type
                    ?: throw ProcessingException(
                        "Missing type argument for $typeParameterName",
                        typeParameter
                    )
            this[typeParameterName] = actualType
        }
    }

    /**
     * The qualified name of the class being annotated with AppFunctionSerializable.
     *
     * When the AppFunctionSerializable contains type parameters, the parameterized type information
     * would be included as a suffix.
     */
    val qualifiedName: String by lazy {
        buildString {
            append(appFunctionSerializableClass.toClassName().canonicalName)
            for ((index, entry) in typeParameterMap.entries.withIndex()) {
                if (index == 0) {
                    append("<")
                }

                val (_, typeRef) = entry
                append(typeRef.toTypeName().ignoreNullable().toString())

                if (index != typeParameterMap.size - 1) {
                    append(",")
                } else {
                    append(">")
                }
            }
        }
    }

    /** The super type of the class being annotated with AppFunctionSerializable */
    val superTypes: Sequence<KSTypeReference> by lazy { appFunctionSerializableClass.superTypes }

    /** The modifier of the class being annotated with AppFunctionSerializable. */
    val modifiers: Set<Modifier> by lazy { appFunctionSerializableClass.modifiers }

    /** The primary constructor if available. */
    val primaryConstructor: KSFunctionDeclaration? by lazy {
        appFunctionSerializableClass.primaryConstructor
    }

    /** The [KSNode] to which the processing error is attributed. */
    val attributeNode: KSNode by lazy { appFunctionSerializableClass }

    // TODO(b/392587953): throw an error if a property has the same name as one of the factory
    //  method parameters
    /**
     * Validates that the class annotated with AppFunctionSerializable follows app function's spec.
     *
     * @throws ProcessingException if the class does not adhere to the requirements
     */
    open fun validate(): AnnotatedAppFunctionSerializable {
        val validateHelper = AppFunctionSerializableValidateHelper(this)
        validateHelper.validatePrimaryConstructor()
        validateHelper.validateParameters()
        return this
    }

    /**
     * Finds all super types of the serializable [appFunctionSerializableClass] that are annotated
     * with the [androidx.appfunctions.AppFunctionSchemaCapability] annotation.
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
     * @return a set of [KSClassDeclaration] for all super types of the
     *   [appFunctionSerializableClass] that are annotated with
     *   [androidx.appfunctions.AppFunctionSchemaCapability].
     */
    fun findSuperTypesWithCapabilityAnnotation(): Set<KSClassDeclaration> {
        return buildSet {
            val unvisitedSuperTypes: MutableList<KSTypeReference> =
                appFunctionSerializableClass.superTypes.toMutableList()

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
     * Finds all super types of the serializable [appFunctionSerializableClass] that are annotated
     * with the [androidx.appfunctions.AppFunctionSerializable] annotation.
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
     * @return a set of [KSClassDeclaration] for all super types of the
     *   [appFunctionSerializableClass] that are annotated with
     *   [androidx.appfunctions.AppFunctionSerializable].
     */
    fun findSuperTypesWithSerializableAnnotation(): Set<KSClassDeclaration> {
        return appFunctionSerializableClass.superTypes
            .map { it.resolve().declaration as KSClassDeclaration }
            .filter {
                it.annotations.findAnnotation(AppFunctionSerializableAnnotation.CLASS_NAME) != null
            }
            .toSet()
    }

    /**
     * Returns the annotated class's properties as defined in its primary constructor.
     *
     * When the property is generic type, it will try to resolve the actual type reference from
     * [arguments].
     */
    fun getProperties(): List<AppFunctionPropertyDeclaration> {
        return checkNotNull(appFunctionSerializableClass.primaryConstructor).parameters.map {
            valueParameter ->
            val valueTypeDeclaration = valueParameter.type.resolve().declaration
            if (valueTypeDeclaration is KSTypeParameter) {
                val actualType =
                    typeParameterMap[valueTypeDeclaration.name.asString()]
                        ?: throw ProcessingException(
                            "Unable to resolve actual type",
                            valueParameter
                        )
                AppFunctionPropertyDeclaration(
                    checkNotNull(valueParameter.name).asString(),
                    actualType
                )
            } else {
                AppFunctionPropertyDeclaration(valueParameter)
            }
        }
    }

    /** Returns the properties that have @AppFunctionSerializable class types. */
    fun getSerializablePropertyTypeReferences(): Set<AppFunctionTypeReference> {
        return getProperties()
            .map { property -> AppFunctionTypeReference(property.type) }
            .filter { afType ->
                afType.isOfTypeCategory(SERIALIZABLE_SINGULAR) ||
                    afType.isOfTypeCategory(SERIALIZABLE_LIST)
            }
            .toSet()
    }

    /** Returns the properties that have @AppFunctionSerializableProxy class types. */
    fun getSerializableProxyPropertyTypeReferences(): Set<AppFunctionTypeReference> {
        return getProperties()
            .map { it -> AppFunctionTypeReference(it.type) }
            .filter { afType -> afType.isOfTypeCategory(SERIALIZABLE_PROXY_SINGULAR) }
            .toSet()
    }

    /**
     * Returns the set of source files that contain the definition of [appFunctionSerializableClass]
     * and all @AppFunctionSerializable classes directly reachable through its fields. This method
     * differs from [getTransitiveSerializableSourceFiles] by excluding transitively
     * nested @AppFunctionSerializable classes.
     */
    fun getSerializableSourceFiles(): Set<KSFile> {
        val sourceFileSet: MutableSet<KSFile> = mutableSetOf()
        appFunctionSerializableClass.containingFile?.let { sourceFileSet.add(it) }
        for (serializableAfType in getSerializablePropertyTypeReferences()) {
            val appFunctionSerializableDefinition =
                serializableAfType.selfOrItemTypeReference.resolve().declaration
                    as KSClassDeclaration
            appFunctionSerializableDefinition.containingFile?.let { sourceFileSet.add(it) }
        }
        return sourceFileSet
    }

    /**
     * Returns the set of source files that contain the definition of [appFunctionSerializableClass]
     * and all @AppFunctionSerializable classes transitively reachable through its fields or nested
     * classes.
     */
    fun getTransitiveSerializableSourceFiles(): Set<KSFile> {
        val sourceFileSet: MutableSet<KSFile> = mutableSetOf()
        val visitedSerializableSet: MutableSet<ClassName> = mutableSetOf()

        // Add the file containing the AppFunctionSerializable class definition immediately it's
        // seen
        appFunctionSerializableClass.containingFile?.let { sourceFileSet.add(it) }
        visitedSerializableSet.add(originalClassName)
        traverseSerializableClassSourceFiles(sourceFileSet, visitedSerializableSet)
        return sourceFileSet
    }

    private fun traverseSerializableClassSourceFiles(
        sourceFileSet: MutableSet<KSFile>,
        visitedSerializableSet: MutableSet<ClassName>
    ) {
        for (serializableAfType in getSerializablePropertyTypeReferences()) {
            val appFunctionSerializableDefinition =
                serializableAfType.selfOrItemTypeReference.resolve().declaration
                    as KSClassDeclaration
            // Skip serializable that have been seen before
            if (visitedSerializableSet.contains(originalClassName)) {
                continue
            }
            // Process newly found serializable
            sourceFileSet.addAll(
                AnnotatedAppFunctionSerializable(
                        appFunctionSerializableDefinition,
                        serializableAfType.selfOrItemTypeReference.resolve().arguments
                    )
                    .getTransitiveSerializableSourceFiles()
            )
        }
    }

    val originalClassName: ClassName by lazy {
        ClassName(
            appFunctionSerializableClass.packageName.asString(),
            appFunctionSerializableClass.simpleName.asString()
        )
    }
}
