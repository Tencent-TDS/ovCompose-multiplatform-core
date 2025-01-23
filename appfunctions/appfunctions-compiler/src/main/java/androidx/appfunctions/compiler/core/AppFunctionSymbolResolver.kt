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

import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionAnnotation
import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionContextClass
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.LIST

/** The helper class to resolve AppFunction related symbols. */
class AppFunctionSymbolResolver(private val resolver: Resolver) {

    /** Resolves valid functions annotated with @AppFunction annotation. */
    fun resolveAnnotatedAppFunctions(): List<AnnotatedAppFunctions> {
        return buildMap<KSClassDeclaration, MutableList<KSFunctionDeclaration>>() {
                val annotatedAppFunctions =
                    resolver.getSymbolsWithAnnotation(
                        AppFunctionAnnotation.CLASS_NAME.canonicalName
                    )
                for (symbol in annotatedAppFunctions) {
                    if (symbol !is KSFunctionDeclaration) {
                        throw ProcessingException(
                            "Only functions can be annotated with @AppFunction",
                            symbol
                        )
                    }
                    val functionClass = symbol.parentDeclaration as? KSClassDeclaration
                    if (functionClass == null) {
                        throw ProcessingException(
                            "Top level functions cannot be annotated with @AppFunction ",
                            symbol
                        )
                    }

                    this.getOrPut(functionClass) { mutableListOf<KSFunctionDeclaration>() }
                        .add(symbol)
                }
            }
            .map { (classDeclaration, appFunctionsDeclarations) ->
                AnnotatedAppFunctions(classDeclaration, appFunctionsDeclarations).validate()
            }
    }

    /**
     * Represents a collection of functions within a specific class that are annotated as app
     * functions.
     */
    data class AnnotatedAppFunctions(
        /**
         * The [com.google.devtools.ksp.symbol.KSClassDeclaration] of the class that contains the
         * annotated app functions.
         */
        val classDeclaration: KSClassDeclaration,
        /**
         * The list of [com.google.devtools.ksp.symbol.KSFunctionDeclaration] that annotated as app
         * function.
         */
        val appFunctionDeclarations: List<KSFunctionDeclaration>
    ) {
        fun validate(): AnnotatedAppFunctions {
            validateFirstParameter()
            validateParameterTypes()
            return this
        }

        private fun validateFirstParameter() {
            for (appFunctionDeclaration in appFunctionDeclarations) {
                val firstParam = appFunctionDeclaration.parameters.firstOrNull()
                if (firstParam == null) {
                    throw ProcessingException(
                        "The first parameter of an app function must be " +
                            "${AppFunctionContextClass.CLASS_NAME}",
                        appFunctionDeclaration
                    )
                }
                if (!firstParam.type.isOfType(AppFunctionContextClass.CLASS_NAME)) {
                    throw ProcessingException(
                        "The first parameter of an app function must be " +
                            "${AppFunctionContextClass.CLASS_NAME}",
                        firstParam
                    )
                }
            }
        }

        private fun validateParameterTypes() {
            for (appFunctionDeclaration in appFunctionDeclarations) {
                for ((paramIndex, ksValueParameter) in
                    appFunctionDeclaration.parameters.withIndex()) {
                    if (paramIndex == 0) {
                        // Skip the first parameter which is always the `AppFunctionContext`.
                        continue
                    }

                    if (!ksValueParameter.validateAppFunctionParameterType()) {
                        throw ProcessingException(
                            "App function parameters must be one of the following " +
                                "primitive types or a list of these types:\n${
                                SUPPORTED_RAW_PRIMITIVE_TYPES.joinToString(
                                    ",\n"
                                )
                            }, but found ${
                                    ksValueParameter.resolveTypeReference().ensureQualifiedTypeName()
                                        .asString()
                            }",
                            ksValueParameter
                        )
                    }
                }
            }
        }

        /**
         * Gets the identifier of an app functions.
         *
         * The format of the identifier is `packageName.className#methodName`.
         */
        fun getAppFunctionIdentifier(functionDeclaration: KSFunctionDeclaration): String {
            val packageName = classDeclaration.packageName.asString()
            val className = classDeclaration.simpleName.asString()
            val methodName = functionDeclaration.simpleName.asString()
            return "${packageName}.${className}#${methodName}"
        }

        /** Gets the [classDeclaration]'s [ClassName]. */
        fun getEnclosingClassName(): ClassName {
            return ClassName(
                classDeclaration.packageName.asString(),
                classDeclaration.simpleName.asString()
            )
        }

        /** Returns the file containing the class declaration and app functions. */
        fun getSourceFile(): KSFile? = classDeclaration.containingFile

        private fun KSValueParameter.validateAppFunctionParameterType(): Boolean {
            // Todo(b/391342300): Allow AppFunctionSerializable type too.
            if (type.isOfType(LIST)) {
                val typeReferenceArgument = type.resolveListParameterizedType()
                // List types only support raw primitive types
                return SUPPORTED_RAW_PRIMITIVE_TYPES.contains(
                    typeReferenceArgument.ensureQualifiedTypeName().asString()
                )
            }
            return SUPPORTED_RAW_PRIMITIVE_TYPES.contains(
                type.ensureQualifiedTypeName().asString()
            ) || SUPPORTED_ARRAY_PRIMITIVE_TYPES.contains(type.ensureQualifiedTypeName().asString())
        }

        /**
         * Resolves the type reference of a parameter.
         *
         * If the parameter type is a list, it will resolve the type reference of the list element.
         */
        private fun KSValueParameter.resolveTypeReference(): KSTypeReference {
            return if (type.isOfType(LIST)) {
                type.resolveListParameterizedType()
            } else {
                type
            }
        }

        private companion object {
            val SUPPORTED_RAW_PRIMITIVE_TYPES: Set<String> =
                setOf(
                    Int::class.qualifiedName!!,
                    Long::class.qualifiedName!!,
                    Float::class.qualifiedName!!,
                    Double::class.qualifiedName!!,
                    Boolean::class.qualifiedName!!,
                    String::class.qualifiedName!!,
                )

            val SUPPORTED_ARRAY_PRIMITIVE_TYPES: Set<String> =
                setOf(
                    IntArray::class.qualifiedName!!,
                    LongArray::class.qualifiedName!!,
                    FloatArray::class.qualifiedName!!,
                    DoubleArray::class.qualifiedName!!,
                    BooleanArray::class.qualifiedName!!,
                )
        }
    }
}
