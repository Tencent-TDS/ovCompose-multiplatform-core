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

package androidx.appfunctions.compiler.processors

import androidx.appfunctions.compiler.AppFunctionCompiler
import androidx.appfunctions.compiler.core.AppFunctionSymbolResolver
import androidx.appfunctions.compiler.core.AppFunctionSymbolResolver.AnnotatedAppFunctions
import androidx.appfunctions.compiler.core.IntrospectionHelper
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.buildCodeBlock

/**
 * Generates implementations for the AppFunctionInventory interface.
 *
 * It resolves all functions in a class annotated with `@AppFunction`, and generates the
 * corresponding metadata for those functions.
 */
class AppFunctionInventoryProcessor(
    private val codeGenerator: CodeGenerator,
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val appFunctionSymbolResolver = AppFunctionSymbolResolver(resolver)
        val appFunctionClasses = appFunctionSymbolResolver.resolveAnnotatedAppFunctions()
        for (appFunctionClass in appFunctionClasses) {
            generateAppFunctionInventoryClass(appFunctionClass)
        }
        return emptyList()
    }

    private fun generateAppFunctionInventoryClass(appFunctionClass: AnnotatedAppFunctions) {
        val originalPackageName = appFunctionClass.classDeclaration.packageName.asString()
        val originalClassName = appFunctionClass.classDeclaration.simpleName.asString()

        val inventoryClassName = getAppFunctionInventoryClassName(originalClassName)
        val inventoryClassBuilder = TypeSpec.classBuilder(inventoryClassName)
        inventoryClassBuilder.addSuperinterface(IntrospectionHelper.APP_FUNCTION_INVENTORY_CLASS)
        inventoryClassBuilder.addAnnotation(AppFunctionCompiler.GENERATED_ANNOTATION)
        inventoryClassBuilder.addProperty(buildFunctionIdToMetadataMapProperty())

        val fileSpec =
            FileSpec.builder(originalPackageName, inventoryClassName)
                .addType(inventoryClassBuilder.build())
                .build()
        codeGenerator
            .createNewFile(
                Dependencies(
                    aggregating = false,
                    checkNotNull(appFunctionClass.classDeclaration.containingFile)
                ),
                originalPackageName,
                inventoryClassName
            )
            .bufferedWriter()
            .use { fileSpec.writeTo(it) }
    }

    /** Creates the `functionIdToMetadataMap` property of the `AppFunctionInventory`. */
    private fun buildFunctionIdToMetadataMapProperty(): PropertySpec {
        return PropertySpec.builder(
                "functionIdToMetadataMap",
                Map::class.asClassName()
                    .parameterizedBy(
                        String::class.asClassName(),
                        IntrospectionHelper.APP_FUNCTION_METADATA_CLASS
                    ),
            )
            .addModifiers(KModifier.OVERRIDE)
            // TODO: Actually build map properties
            .initializer(buildCodeBlock { addStatement("mapOf()") })
            .build()
    }

    private fun getAppFunctionInventoryClassName(functionClassName: String): String {
        return "$%s_AppFunctionInventory_Impl".format(functionClassName)
    }
}
