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

import androidx.annotation.VisibleForTesting
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.compiler.AppFunctionCompiler
import androidx.appfunctions.compiler.core.AnnotatedAppFunctionSerializable
import androidx.appfunctions.compiler.core.AnnotatedAppFunctionSerializableProxy
import androidx.appfunctions.compiler.core.AppFunctionSymbolResolver
import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionSerializableFactoryClass
import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionSerializableFactoryClass.FromAppFunctionDataMethod.APP_FUNCTION_DATA_PARAM_NAME
import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionSerializableFactoryClass.ToAppFunctionDataMethod.APP_FUNCTION_SERIALIZABLE_PARAM_NAME
import androidx.appfunctions.compiler.core.ProcessingException
import androidx.appfunctions.compiler.core.logException
import androidx.appfunctions.compiler.core.toClassName
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName

/**
 * Generates a factory class with methods to convert classes annotated with
 * [androidx.appfunctions.AppFunctionSerializable] or
 * [androidx.appfunctions.AppFunctionSerializableProxy] to [androidx.appfunctions.AppFunctionData],
 * and vice-versa.
 *
 * **Example:**
 *
 * ```
 * @AppFunctionSerializable
 * class Location(val latitude: Double, val longitude: Double)
 * ```
 *
 * A corresponding `LocationFactory` class will be generated:
 * ```
 * @Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
 * public class LocationFactory : AppFunctionSerializableFactory<Location> {
 *   override fun fromAppFunctionData(appFunctionData: AppFunctionData): Location {
 *     val latitude = appFunctionData.getDouble("latitude")
 *     val longitude = appFunctionData.getDouble("longitude")
 *
 *     return Location(latitude, longitude)
 *   }
 *
 *   override fun toAppFunctionData(appFunctionSerializable: Location): AppFunctionData {
 *     val builder = AppFunctionData.Builder("")
 *
 *     builder.setDouble("latitude", location.latitude)
 *     builder.setDouble("longitude", location.longitude)
 *
 *     return builder.build()
 *   }
 * }
 * ```
 */
class AppFunctionSerializableProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {
    private var hasProcessed = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (hasProcessed) return emptyList()
        hasProcessed = true

        try {
            val entitySymbolResolver = AppFunctionSymbolResolver(resolver)
            val entityClasses = entitySymbolResolver.resolveAnnotatedAppFunctionSerializables()
            val entityProxyClasses =
                entitySymbolResolver.resolveAnnotatedAppFunctionSerializableProxies()
            val proxyTargetToSerializableProxy =
                entityProxyClasses.associateBy { it.targetClassDeclaration.toClassName() }
            for (entity in entityClasses) {
                buildAppFunctionSerializableFactoryClass(entity, proxyTargetToSerializableProxy)
            }
            for (entityProxy in entityProxyClasses) {
                buildAppFunctionSerializableProxyFactoryClass(
                    entityProxy,
                    proxyTargetToSerializableProxy
                )
            }
        } catch (e: ProcessingException) {
            logger.logException(e)
        }
        return emptyList()
    }

    private fun buildAppFunctionSerializableFactoryClass(
        annotatedClass: AnnotatedAppFunctionSerializable,
        proxyTargetToSerializableProxy: Map<ClassName, AnnotatedAppFunctionSerializableProxy>
    ) {
        val superInterfaceClass =
            AppFunctionSerializableFactoryClass.CLASS_NAME.parameterizedBy(
                listOf(annotatedClass.originalClassName)
            )

        val factoryCodeBuilder =
            AppFunctionSerializableFactoryCodeBuilder(
                annotatedClass,
                proxyTargetToSerializableProxy
            )
        val generatedFactoryClassName = "\$${annotatedClass.originalClassName.simpleName}Factory"
        val fileSpec =
            FileSpec.builder(
                    annotatedClass.originalClassName.packageName,
                    generatedFactoryClassName
                )
                .addType(
                    TypeSpec.classBuilder(generatedFactoryClassName)
                        .addAnnotation(AppFunctionCompiler.GENERATED_ANNOTATION)
                        .addSuperinterface(superInterfaceClass)
                        .addFunction(
                            buildFromAppFunctionDataFunction(annotatedClass, factoryCodeBuilder)
                        )
                        .addFunction(
                            buildToAppFunctionDataFunction(annotatedClass, factoryCodeBuilder)
                        )
                        .build()
                )
                .build()
        codeGenerator
            .createNewFile(
                Dependencies(
                    aggregating = true,
                    *annotatedClass.getSerializableSourceFiles().toTypedArray()
                ),
                annotatedClass.originalClassName.packageName,
                generatedFactoryClassName
            )
            .bufferedWriter()
            .use { fileSpec.writeTo(it) }
    }

    private fun buildAppFunctionSerializableProxyFactoryClass(
        annotatedProxyClass: AnnotatedAppFunctionSerializableProxy,
        proxyTargetToSerializableProxy: Map<ClassName, AnnotatedAppFunctionSerializableProxy>
    ) {
        val proxySuperInterfaceClass =
            AppFunctionSerializableFactoryClass.CLASS_NAME.parameterizedBy(
                annotatedProxyClass.targetClassDeclaration.toClassName()
            )
        val generatedSerializableProxyFactoryClassName =
            "\$${checkNotNull(
                annotatedProxyClass.targetClassDeclaration.simpleName).asString()}Factory"
        val serializableProxyClassBuilder =
            TypeSpec.classBuilder(generatedSerializableProxyFactoryClassName)
        val factoryCodeBuilder =
            AppFunctionSerializableFactoryCodeBuilder(
                annotatedProxyClass,
                proxyTargetToSerializableProxy
            )
        serializableProxyClassBuilder.addAnnotation(AppFunctionCompiler.GENERATED_ANNOTATION)
        serializableProxyClassBuilder.addSuperinterface(proxySuperInterfaceClass)
        serializableProxyClassBuilder.addFunction(
            buildProxyFromAppFunctionDataFunction(annotatedProxyClass, factoryCodeBuilder)
        )
        serializableProxyClassBuilder.addFunction(
            buildProxyToAppFunctionDataFunction(annotatedProxyClass, factoryCodeBuilder)
        )
        val fileSpec =
            FileSpec.builder(
                    annotatedProxyClass.originalClassName.packageName,
                    generatedSerializableProxyFactoryClassName
                )
                .addType(serializableProxyClassBuilder.build())
                .build()
        codeGenerator
            .createNewFile(
                Dependencies(
                    aggregating = true,
                    *annotatedProxyClass.getSerializableSourceFiles().toTypedArray()
                ),
                annotatedProxyClass.originalClassName.packageName,
                generatedSerializableProxyFactoryClassName
            )
            .bufferedWriter()
            .use { fileSpec.writeTo(it) }
    }

    private fun buildFromAppFunctionDataFunction(
        annotatedClass: AnnotatedAppFunctionSerializable,
        factoryCodeBuilder: AppFunctionSerializableFactoryCodeBuilder,
    ): FunSpec {
        return FunSpec.builder(
                AppFunctionSerializableFactoryClass.FromAppFunctionDataMethod.METHOD_NAME
            )
            .addModifiers(KModifier.OVERRIDE)
            .addParameter(
                ParameterSpec.builder(APP_FUNCTION_DATA_PARAM_NAME, AppFunctionData::class).build()
            )
            .addCode(factoryCodeBuilder.appendFromAppFunctionDataMethodBody())
            .returns(annotatedClass.originalClassName)
            .build()
    }

    // Todo(b/403199251): Remove temp method
    private fun buildProxyFromAppFunctionDataFunction(
        annotatedProxyClass: AnnotatedAppFunctionSerializableProxy,
        factoryCodeBuilder: AppFunctionSerializableFactoryCodeBuilder,
    ): FunSpec {
        return FunSpec.builder(
                AppFunctionSerializableFactoryClass.FromAppFunctionDataMethod.METHOD_NAME
            )
            .addModifiers(KModifier.OVERRIDE)
            .addParameter(
                ParameterSpec.builder(APP_FUNCTION_DATA_PARAM_NAME, AppFunctionData::class).build()
            )
            .addCode(factoryCodeBuilder.appendFromAppFunctionDataMethodBodyForProxy())
            .returns(annotatedProxyClass.targetClassDeclaration.toClassName())
            .build()
    }

    private fun buildToAppFunctionDataFunction(
        annotatedClass: AnnotatedAppFunctionSerializable,
        factoryCodeBuilder: AppFunctionSerializableFactoryCodeBuilder
    ): FunSpec {
        return FunSpec.builder(
                AppFunctionSerializableFactoryClass.ToAppFunctionDataMethod.METHOD_NAME
            )
            .addModifiers(KModifier.OVERRIDE)
            .addParameter(
                ParameterSpec.builder(
                        APP_FUNCTION_SERIALIZABLE_PARAM_NAME,
                        annotatedClass.originalClassName
                    )
                    .build()
            )
            .addCode(factoryCodeBuilder.appendToAppFunctionDataMethodBody())
            .returns(AppFunctionData::class.asTypeName())
            .build()
    }

    // Todo(b/403199251): Remove temp method
    private fun buildProxyToAppFunctionDataFunction(
        annotatedProxyClass: AnnotatedAppFunctionSerializableProxy,
        factoryCodeBuilder: AppFunctionSerializableFactoryCodeBuilder
    ): FunSpec {
        return FunSpec.builder(
                AppFunctionSerializableFactoryClass.ToAppFunctionDataMethod.METHOD_NAME
            )
            .addModifiers(KModifier.OVERRIDE)
            .addParameter(
                ParameterSpec.builder(
                        APP_FUNCTION_SERIALIZABLE_PARAM_NAME,
                        annotatedProxyClass.targetClassDeclaration.toClassName()
                    )
                    .build()
            )
            .addCode(factoryCodeBuilder.appendToAppFunctionDataMethodBodyForProxy())
            .returns(AppFunctionData::class.asTypeName())
            .build()
    }

    @VisibleForTesting
    class Provider : SymbolProcessorProvider {
        override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
            return AppFunctionSerializableProcessor(environment.codeGenerator, environment.logger)
        }
    }
}
