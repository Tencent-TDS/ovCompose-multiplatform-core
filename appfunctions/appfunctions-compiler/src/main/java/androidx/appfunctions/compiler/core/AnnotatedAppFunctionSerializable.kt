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
import androidx.appfunctions.compiler.core.AppFunctionTypeReference.AppFunctionSupportedTypeCategory.SERIALIZABLE_SINGULAR
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSTypeReference
import com.squareup.kotlinpoet.ClassName

/** Represents a class annotated with [androidx.appfunctions.AppFunctionSerializable]. */
open class AnnotatedAppFunctionSerializable(
    private val appFunctionSerializableClass: KSClassDeclaration
) {
    /** The qualified name of the class being annotated with AppFunctionSerializable. */
    val qualifiedName: String by lazy { appFunctionSerializableClass.toClassName().canonicalName }

    /** The super type of the class being annotated with AppFunctionSerializable */
    val superTypes: Sequence<KSTypeReference> by lazy { appFunctionSerializableClass.superTypes }

    /**
     * The validator that can be used to validate the class annotated with AppFunctionSerializable.
     */
    private val validator: SerializableValidator by lazy {
        SerializableValidator(classToValidate = appFunctionSerializableClass)
    }

    // TODO(b/392587953): throw an error if a property has the same name as one of the factory
    //  method parameters
    /**
     * Validates that the class annotated with AppFunctionSerializable follows app function's spec.
     *
     * @throws ProcessingException if the class does not adhere to the requirements
     */
    open fun validate(): AnnotatedAppFunctionSerializable {
        validator.validate()
        return this
    }

    /**
     * Returns the set of super types of [appFunctionSerializableClass] that are annotated with
     * [androidx.appfunctions.AppFunctionSchemaCapability].
     */
    fun findSuperTypesWithCapabilityAnnotation(): Set<KSClassDeclaration> {
        return validator.findSuperTypesWithCapabilityAnnotation()
    }

    /** Returns the annotated class's properties as defined in its primary constructor. */
    fun getProperties(): List<AppFunctionPropertyDeclaration> {
        return checkNotNull(appFunctionSerializableClass.primaryConstructor).parameters.map {
            AppFunctionPropertyDeclaration(it)
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
                AnnotatedAppFunctionSerializable(appFunctionSerializableDefinition)
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
