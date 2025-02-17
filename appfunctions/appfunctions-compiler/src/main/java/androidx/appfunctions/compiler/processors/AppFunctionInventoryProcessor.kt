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
import androidx.appfunctions.compiler.core.AnnotatedAppFunctions
import androidx.appfunctions.compiler.core.AppFunctionComponentRegistryGenerator
import androidx.appfunctions.compiler.core.AppFunctionComponentRegistryGenerator.AppFunctionComponent
import androidx.appfunctions.compiler.core.AppFunctionSymbolResolver
import androidx.appfunctions.compiler.core.IntrospectionHelper
import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionComponentRegistryAnnotation
import androidx.appfunctions.compiler.core.ProcessingException
import androidx.appfunctions.metadata.AppFunctionArrayTypeMetadata
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionDataTypeMetadata
import androidx.appfunctions.metadata.AppFunctionMetadata
import androidx.appfunctions.metadata.AppFunctionObjectTypeMetadata
import androidx.appfunctions.metadata.AppFunctionParameterMetadata
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata
import androidx.appfunctions.metadata.AppFunctionReferenceTypeMetadata
import androidx.appfunctions.metadata.AppFunctionResponseMetadata
import androidx.appfunctions.metadata.AppFunctionSchemaMetadata
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.squareup.kotlinpoet.CodeBlock
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
 *
 * **Important:** [androidx.appfunctions.compiler.processors.AppFunctionInventoryProcessor] will
 * process exactly once for each compilation unit to generate a single registry for looking up all
 * generated inventories within the compilation unit.
 */
class AppFunctionInventoryProcessor(
    private val codeGenerator: CodeGenerator,
) : SymbolProcessor {

    private var hasProcessed = false

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (hasProcessed) return emptyList()
        hasProcessed = true

        val appFunctionSymbolResolver = AppFunctionSymbolResolver(resolver)
        val appFunctionClasses = appFunctionSymbolResolver.resolveAnnotatedAppFunctions()
        val generatedInventoryComponents =
            buildList<AppFunctionComponent> {
                for (appFunctionClass in appFunctionClasses) {
                    val inventoryQualifiedName = generateAppFunctionInventoryClass(appFunctionClass)
                    add(
                        AppFunctionComponent(
                            qualifiedName = inventoryQualifiedName,
                            sourceFiles = appFunctionClass.getSourceFiles(),
                        )
                    )
                }
            }

        AppFunctionComponentRegistryGenerator(codeGenerator)
            .generateRegistry(
                resolver.getModuleName().asString(),
                AppFunctionComponentRegistryAnnotation.Category.INVENTORY,
                generatedInventoryComponents,
            )
        return emptyList()
    }

    /**
     * Generates an implementation of AppFunctionInventory for [appFunctionClass].
     *
     * @return fully qualified name of the generated inventory implementation class.
     */
    private fun generateAppFunctionInventoryClass(appFunctionClass: AnnotatedAppFunctions): String {
        val originalPackageName = appFunctionClass.classDeclaration.packageName.asString()
        val originalClassName = appFunctionClass.classDeclaration.simpleName.asString()

        val inventoryClassName = getAppFunctionInventoryClassName(originalClassName)
        val inventoryClassBuilder = TypeSpec.classBuilder(inventoryClassName)
        inventoryClassBuilder.addSuperinterface(IntrospectionHelper.APP_FUNCTION_INVENTORY_CLASS)
        inventoryClassBuilder.addAnnotation(AppFunctionCompiler.GENERATED_ANNOTATION)
        inventoryClassBuilder.addKdoc(buildSourceFilesKdoc(appFunctionClass))
        addFunctionMetadataProperties(inventoryClassBuilder, appFunctionClass)

        val fileSpec =
            FileSpec.builder(originalPackageName, inventoryClassName)
                .addType(inventoryClassBuilder.build())
                .build()
        codeGenerator
            .createNewFile(
                Dependencies(aggregating = true, *appFunctionClass.getSourceFiles().toTypedArray()),
                originalPackageName,
                inventoryClassName
            )
            .bufferedWriter()
            .use { fileSpec.writeTo(it) }

        return "${originalPackageName}.$inventoryClassName"
    }

    /**
     * Adds properties to the `AppFunctionInventory` class for each function in the class.
     *
     * @param inventoryClassBuilder The builder for the `AppFunctionInventory` class.
     * @param appFunctionClass The class annotated with `@AppFunction`.
     */
    private fun addFunctionMetadataProperties(
        inventoryClassBuilder: TypeSpec.Builder,
        appFunctionClass: AnnotatedAppFunctions
    ) {
        val appFunctionMetadataList = appFunctionClass.createAppFunctionMetadataList()

        for (functionMetadata in appFunctionMetadataList) {
            val functionMetadataObjectClassBuilder =
                TypeSpec.objectBuilder(getFunctionMetadataObjectClassName(functionMetadata.id))
                    .addModifiers(KModifier.PRIVATE)
            addSchemaMetadataPropertyForFunction(
                functionMetadataObjectClassBuilder,
                functionMetadata.schema
            )
            addPropertiesForParameterMetadataList(
                functionMetadataObjectClassBuilder,
                functionMetadata.parameters
            )
            addPropertyForResponseMetadata(
                functionMetadataObjectClassBuilder,
                functionMetadata.response
            )
            addPropertyForComponentsMetadata(
                functionMetadataObjectClassBuilder,
                functionMetadata.components
            )
            addPropertyForAppFunctionMetadata(functionMetadataObjectClassBuilder, functionMetadata)
            inventoryClassBuilder.addType(functionMetadataObjectClassBuilder.build())
        }
        addFunctionIdToMetadataMapProperty(inventoryClassBuilder, appFunctionMetadataList)
    }

    private fun addPropertyForAppFunctionMetadata(
        functionMetadataObjectClassBuilder: TypeSpec.Builder,
        functionMetadata: AppFunctionMetadata
    ) {
        functionMetadataObjectClassBuilder.addProperty(
            PropertySpec.builder(
                    APP_FUNCTION_METADATA_PROPERTY_NAME,
                    IntrospectionHelper.APP_FUNCTION_METADATA_CLASS
                )
                .addModifiers(KModifier.PUBLIC)
                .initializer(
                    buildCodeBlock {
                        addStatement(
                            """
                            %T(
                                id = %S,
                                isEnabledByDefault = %L,
                                schema =  %L,
                                parameters = %L,
                                response = %L,
                                components = %L
                            )
                            """
                                .trimIndent(),
                            IntrospectionHelper.APP_FUNCTION_METADATA_CLASS,
                            functionMetadata.id,
                            functionMetadata.isEnabledByDefault,
                            SCHEMA_METADATA_PROPERTY_NAME,
                            PARAMETER_METADATA_LIST_PROPERTY_NAME,
                            RESPONSE_METADATA_PROPERTY_NAME,
                            COMPONENT_METADATA_PROPERTY_NAME
                        )
                    }
                )
                .build()
        )
    }

    private fun addPropertyForComponentsMetadata(
        functionMetadataObjectClassBuilder: TypeSpec.Builder,
        appFunctionComponentsMetadata: AppFunctionComponentsMetadata
    ) {
        val componentDataTypesPropertyName = COMPONENT_METADATA_PROPERTY_NAME + "_DATA_TYPES_MAP"
        addPropertyForComponentsDataTypes(
            componentDataTypesPropertyName,
            functionMetadataObjectClassBuilder,
            appFunctionComponentsMetadata.dataTypes
        )
        functionMetadataObjectClassBuilder.addProperty(
            PropertySpec.builder(
                    COMPONENT_METADATA_PROPERTY_NAME,
                    IntrospectionHelper.APP_FUNCTION_COMPONENTS_METADATA_CLASS
                )
                .addModifiers(KModifier.PRIVATE)
                .initializer(
                    buildCodeBlock {
                        addStatement(
                            """
                            %T(
                                dataTypes = %L
                            )
                            """
                                .trimIndent(),
                            IntrospectionHelper.APP_FUNCTION_COMPONENTS_METADATA_CLASS,
                            componentDataTypesPropertyName
                        )
                    }
                )
                .build()
        )
    }

    private fun addPropertyForComponentsDataTypes(
        propertyName: String,
        functionMetadataObjectClassBuilder: TypeSpec.Builder,
        dataTypes: Map<String, AppFunctionDataTypeMetadata>
    ) {
        functionMetadataObjectClassBuilder.addProperty(
            PropertySpec.builder(
                    propertyName,
                    Map::class.asClassName()
                        .parameterizedBy(
                            String::class.asClassName(),
                            IntrospectionHelper.APP_FUNCTION_DATA_TYPE_METADATA
                        ),
                )
                .addModifiers(KModifier.PRIVATE)
                .initializer(
                    buildCodeBlock {
                        addStatement("mapOf(")
                        indent()
                        for ((componentReferenceKey, componentReferenceTypeMetadata) in dataTypes) {
                            val datatypeVariableName =
                                when (componentReferenceTypeMetadata) {
                                    is AppFunctionObjectTypeMetadata -> {
                                        val objectTypeMetadataPropertyName =
                                            getObjectTypeMetadataPropertyNameForComponent(
                                                componentReferenceKey
                                            )
                                        addPropertyForObjectTypeMetadata(
                                            objectTypeMetadataPropertyName,
                                            functionMetadataObjectClassBuilder,
                                            componentReferenceTypeMetadata
                                        )
                                        objectTypeMetadataPropertyName
                                    }
                                    else -> {
                                        // TODO provide KSNode to improve error message
                                        throw ProcessingException(
                                            "Component types contains unsupported datatype: " +
                                                componentReferenceTypeMetadata,
                                            null
                                        )
                                    }
                                }
                            addStatement(
                                """
                                %S to %L,
                                """
                                    .trimIndent(),
                                componentReferenceKey,
                                datatypeVariableName
                            )
                        }
                        addStatement(")")
                        unindent()
                    }
                )
                .build()
        )
    }

    private fun addPropertyForResponseMetadata(
        functionMetadataObjectClassBuilder: TypeSpec.Builder,
        appFunctionResponseMetadata: AppFunctionResponseMetadata
    ) {
        val responseMetadataValueTypeName =
            when (val castDataType = appFunctionResponseMetadata.valueType) {
                is AppFunctionPrimitiveTypeMetadata -> {
                    val primitiveReturnTypeMetadataPropertyName = "PRIMITIVE_RESPONSE_VALUE_TYPE"
                    addPropertyForPrimitiveTypeMetadata(
                        primitiveReturnTypeMetadataPropertyName,
                        functionMetadataObjectClassBuilder,
                        castDataType
                    )
                    primitiveReturnTypeMetadataPropertyName
                }
                is AppFunctionArrayTypeMetadata -> {
                    val arrayReturnTypeMetadataPropertyName = "ARRAY_RESPONSE_VALUE_TYPE"
                    addPropertyForArrayTypeMetadata(
                        arrayReturnTypeMetadataPropertyName,
                        functionMetadataObjectClassBuilder,
                        castDataType
                    )
                    arrayReturnTypeMetadataPropertyName
                }
                is AppFunctionObjectTypeMetadata -> {
                    val objectReturnTypeMetadataPropertyName = "OBJECT_RESPONSE_VALUE_TYPE"
                    addPropertyForObjectTypeMetadata(
                        objectReturnTypeMetadataPropertyName,
                        functionMetadataObjectClassBuilder,
                        castDataType
                    )
                    objectReturnTypeMetadataPropertyName
                }
                is AppFunctionReferenceTypeMetadata -> {
                    val referenceReturnTypeMetadataPropertyName = "REFERENCE_RESPONSE_VALUE_TYPE"
                    addPropertyForReferenceTypeMetadata(
                        referenceReturnTypeMetadataPropertyName,
                        functionMetadataObjectClassBuilder,
                        castDataType
                    )
                    referenceReturnTypeMetadataPropertyName
                }
                else -> {
                    // TODO provide KSNode to improve error message
                    throw ProcessingException(
                        "Unable to build parameter metadata for unknown datatype: $castDataType",
                        null
                    )
                }
            }
        functionMetadataObjectClassBuilder.addProperty(
            PropertySpec.builder(
                    RESPONSE_METADATA_PROPERTY_NAME,
                    IntrospectionHelper.APP_FUNCTION_RESPONSE_METADATA_CLASS
                )
                .addModifiers(KModifier.PRIVATE)
                .initializer(
                    buildCodeBlock {
                        addStatement(
                            """
                            %T(
                                valueType = %L
                            )
                            """
                                .trimIndent(),
                            IntrospectionHelper.APP_FUNCTION_RESPONSE_METADATA_CLASS,
                            responseMetadataValueTypeName
                        )
                    }
                )
                .build()
        )
    }

    private fun addPropertiesForParameterMetadataList(
        functionMetadataObjectClassBuilder: TypeSpec.Builder,
        parameterMetadataList: List<AppFunctionParameterMetadata>
    ) {
        functionMetadataObjectClassBuilder.addProperty(
            PropertySpec.builder(
                    PARAMETER_METADATA_LIST_PROPERTY_NAME,
                    List::class.asClassName()
                        .parameterizedBy(IntrospectionHelper.APP_FUNCTION_PARAMETER_METADATA_CLASS)
                )
                .addModifiers(KModifier.PRIVATE)
                .initializer(
                    buildCodeBlock {
                        addStatement("listOf(")
                        indent()
                        for (parameterMetadata in parameterMetadataList) {
                            addPropertiesForParameterMetadata(
                                parameterMetadata,
                                functionMetadataObjectClassBuilder
                            )
                            addStatement(
                                "%L,",
                                "${parameterMetadata.name.uppercase()}_PARAMETER_METADATA"
                            )
                        }
                        unindent()
                        addStatement(")")
                    }
                )
                .build()
        )
    }

    private fun addPropertiesForParameterMetadata(
        parameterMetadata: AppFunctionParameterMetadata,
        functionMetadataObjectClassBuilder: TypeSpec.Builder,
    ) {
        val parameterMetadataPropertyName =
            "${parameterMetadata.name.uppercase()}_PARAMETER_METADATA"
        val datatypeVariableName =
            when (val castDataType = parameterMetadata.dataType) {
                is AppFunctionPrimitiveTypeMetadata -> {
                    val primitiveTypeMetadataPropertyName =
                        getPrimitiveTypeMetadataPropertyNameForParameter(parameterMetadata)
                    addPropertyForPrimitiveTypeMetadata(
                        primitiveTypeMetadataPropertyName,
                        functionMetadataObjectClassBuilder,
                        castDataType
                    )
                    primitiveTypeMetadataPropertyName
                }
                is AppFunctionArrayTypeMetadata -> {
                    val arrayTypeMetadataPropertyName =
                        getArrayTypeMetadataPropertyNameForParameter(parameterMetadata)
                    addPropertyForArrayTypeMetadata(
                        arrayTypeMetadataPropertyName,
                        functionMetadataObjectClassBuilder,
                        castDataType
                    )
                    arrayTypeMetadataPropertyName
                }
                is AppFunctionObjectTypeMetadata -> {
                    val objectTypeMetadataPropertyName =
                        getObjectTypeMetadataPropertyNameForParameter(parameterMetadata)
                    addPropertyForObjectTypeMetadata(
                        objectTypeMetadataPropertyName,
                        functionMetadataObjectClassBuilder,
                        castDataType
                    )
                    objectTypeMetadataPropertyName
                }
                is AppFunctionReferenceTypeMetadata -> {
                    val referenceTypeMetadataPropertyName =
                        getReferenceTypeMetadataPropertyNameForParameter(parameterMetadata)
                    addPropertyForReferenceTypeMetadata(
                        referenceTypeMetadataPropertyName,
                        functionMetadataObjectClassBuilder,
                        castDataType
                    )
                    referenceTypeMetadataPropertyName
                }
                else -> {
                    // TODO provide KSNode to improve error message
                    throw ProcessingException(
                        "Unable to build parameter metadata for unknown datatype: $castDataType",
                        null
                    )
                }
            }
        functionMetadataObjectClassBuilder.addProperty(
            PropertySpec.builder(
                    parameterMetadataPropertyName,
                    IntrospectionHelper.APP_FUNCTION_PARAMETER_METADATA_CLASS
                )
                .addModifiers(KModifier.PRIVATE)
                .initializer(
                    buildCodeBlock {
                        addStatement(
                            """
                            %T(
                                name = %S,
                                isRequired = %L,
                                dataType = %L
                            )
                            """
                                .trimIndent(),
                            IntrospectionHelper.APP_FUNCTION_PARAMETER_METADATA_CLASS,
                            parameterMetadata.name,
                            parameterMetadata.isRequired,
                            datatypeVariableName
                        )
                    }
                )
                .build()
        )
    }

    private fun addPropertyForPrimitiveTypeMetadata(
        propertyName: String,
        functionMetadataObjectClassBuilder: TypeSpec.Builder,
        primitiveTypeMetadata: AppFunctionPrimitiveTypeMetadata
    ) {
        functionMetadataObjectClassBuilder.addProperty(
            PropertySpec.builder(
                    propertyName,
                    IntrospectionHelper.APP_FUNCTION_PRIMITIVE_TYPE_METADATA_CLASS
                )
                .addModifiers(KModifier.PRIVATE)
                .initializer(
                    buildCodeBlock {
                        addStatement(
                            """
                            %T(
                                type = %L,
                                isNullable = %L
                            )
                            """
                                .trimIndent(),
                            IntrospectionHelper.APP_FUNCTION_PRIMITIVE_TYPE_METADATA_CLASS,
                            primitiveTypeMetadata.type,
                            primitiveTypeMetadata.isNullable
                        )
                    }
                )
                .build()
        )
    }

    private fun addPropertyForArrayTypeMetadata(
        propertyName: String,
        functionMetadataObjectClassBuilder: TypeSpec.Builder,
        arrayTypeMetadata: AppFunctionArrayTypeMetadata
    ) {
        val itemTypeVariableName =
            when (val castItemType = arrayTypeMetadata.itemType) {
                is AppFunctionPrimitiveTypeMetadata -> {
                    val primitiveItemTypeVariableName = propertyName + "_PRIMITIVE_ITEM_TYPE"
                    addPropertyForPrimitiveTypeMetadata(
                        primitiveItemTypeVariableName,
                        functionMetadataObjectClassBuilder,
                        castItemType
                    )
                    primitiveItemTypeVariableName
                }
                is AppFunctionObjectTypeMetadata -> {
                    val objectItemTypeVariableName = propertyName + "_OBJECT_ITEM_TYPE"
                    addPropertyForObjectTypeMetadata(
                        objectItemTypeVariableName,
                        functionMetadataObjectClassBuilder,
                        castItemType
                    )
                    objectItemTypeVariableName
                }
                is AppFunctionReferenceTypeMetadata -> {
                    val referenceItemTypeVariableName = propertyName + "_REFERENCE_ITEM_TYPE"
                    addPropertyForReferenceTypeMetadata(
                        referenceItemTypeVariableName,
                        functionMetadataObjectClassBuilder,
                        castItemType
                    )
                    referenceItemTypeVariableName
                }
                else -> {
                    // TODO provide KSNode to improve error message
                    throw ProcessingException(
                        "Unable to build parameter item type metadata for unknown itemType: " +
                            "$castItemType",
                        null
                    )
                }
            }
        functionMetadataObjectClassBuilder.addProperty(
            PropertySpec.builder(
                    propertyName,
                    IntrospectionHelper.APP_FUNCTION_ARRAY_TYPE_METADATA_CLASS
                )
                .addModifiers(KModifier.PRIVATE)
                .initializer(
                    buildCodeBlock {
                        addStatement(
                            """
                            %T(
                                itemType = %L,
                                isNullable = %L
                            )
                            """
                                .trimIndent(),
                            IntrospectionHelper.APP_FUNCTION_ARRAY_TYPE_METADATA_CLASS,
                            itemTypeVariableName,
                            arrayTypeMetadata.isNullable
                        )
                    }
                )
                .build()
        )
    }

    private fun addPropertyForReferenceTypeMetadata(
        propertyName: String,
        functionMetadataObjectClassBuilder: TypeSpec.Builder,
        referenceTypeMetadata: AppFunctionReferenceTypeMetadata,
    ) {
        functionMetadataObjectClassBuilder.addProperty(
            PropertySpec.builder(
                    propertyName,
                    IntrospectionHelper.APP_FUNCTION_REFERENCE_TYPE_METADATA_CLASS
                )
                .addModifiers(KModifier.PRIVATE)
                .initializer(
                    buildCodeBlock {
                        addStatement(
                            """
                            %T(
                                referenceDataType = %S,
                                isNullable = %L
                            )
                            """
                                .trimIndent(),
                            IntrospectionHelper.APP_FUNCTION_REFERENCE_TYPE_METADATA_CLASS,
                            referenceTypeMetadata.referenceDataType,
                            referenceTypeMetadata.isNullable
                        )
                    }
                )
                .build()
        )
    }

    private fun addPropertyForObjectTypeMetadata(
        propertyName: String,
        functionMetadataObjectClassBuilder: TypeSpec.Builder,
        objectTypeMetadata: AppFunctionObjectTypeMetadata,
    ) {
        val objectPropertiesMapPropertyName = propertyName + "_PROPERTIES_MAP"
        addPropertyForObjectPropertiesMap(
            objectPropertiesMapPropertyName,
            functionMetadataObjectClassBuilder,
            objectTypeMetadata.properties
        )
        val requiredPropertiesListPropertyName = propertyName + "_REQUIRED_PROPERTIES_LIST"
        addPropertyForListOfRequiredObjectProperties(
            requiredPropertiesListPropertyName,
            functionMetadataObjectClassBuilder,
            objectTypeMetadata.required
        )
        functionMetadataObjectClassBuilder.addProperty(
            PropertySpec.builder(
                    propertyName,
                    IntrospectionHelper.APP_FUNCTION_OBJECT_TYPE_METADATA_CLASS
                )
                .addModifiers(KModifier.PRIVATE)
                .initializer(
                    buildCodeBlock {
                        addStatement(
                            """
                            %T(
                                properties = %L,
                                required = %L,
                                qualifiedName = %S,
                                isNullable = %L
                            )
                            """
                                .trimIndent(),
                            IntrospectionHelper.APP_FUNCTION_OBJECT_TYPE_METADATA_CLASS,
                            objectPropertiesMapPropertyName,
                            requiredPropertiesListPropertyName,
                            objectTypeMetadata.qualifiedName,
                            objectTypeMetadata.isNullable,
                        )
                    }
                )
                .build()
        )
    }

    private fun addPropertyForListOfRequiredObjectProperties(
        propertyName: String,
        functionMetadataObjectClassBuilder: TypeSpec.Builder,
        requiredProperties: List<String>
    ) {
        functionMetadataObjectClassBuilder.addProperty(
            PropertySpec.builder(
                    propertyName,
                    List::class.asClassName().parameterizedBy(String::class.asClassName())
                )
                .addModifiers(KModifier.PRIVATE)
                .initializer(
                    buildCodeBlock {
                        addStatement("listOf(")
                        indent()
                        for (requiredProperty in requiredProperties) {
                            addStatement("%S,", requiredProperty)
                        }
                        unindent()
                        addStatement(")")
                    }
                )
                .build()
        )
    }

    private fun addPropertyForObjectPropertiesMap(
        propertyName: String,
        functionMetadataObjectClassBuilder: TypeSpec.Builder,
        propertiesMap: Map<String, AppFunctionDataTypeMetadata>,
    ) {
        functionMetadataObjectClassBuilder.addProperty(
            PropertySpec.builder(
                    propertyName,
                    Map::class.asClassName()
                        .parameterizedBy(
                            String::class.asClassName(),
                            IntrospectionHelper.APP_FUNCTION_DATA_TYPE_METADATA
                        ),
                )
                .addModifiers(KModifier.PRIVATE)
                .initializer(
                    buildCodeBlock {
                        addStatement("mapOf(")
                        indent()
                        for ((objectPropertyName, objectPropertyTypeMetadata) in propertiesMap) {
                            val dataTypeVariableName =
                                propertyName + "_${objectPropertyName.uppercase()}"
                            when (objectPropertyTypeMetadata) {
                                is AppFunctionPrimitiveTypeMetadata ->
                                    addPropertyForPrimitiveTypeMetadata(
                                        dataTypeVariableName,
                                        functionMetadataObjectClassBuilder,
                                        objectPropertyTypeMetadata
                                    )
                                is AppFunctionArrayTypeMetadata ->
                                    addPropertyForArrayTypeMetadata(
                                        dataTypeVariableName,
                                        functionMetadataObjectClassBuilder,
                                        objectPropertyTypeMetadata
                                    )
                                is AppFunctionObjectTypeMetadata ->
                                    addPropertyForObjectTypeMetadata(
                                        dataTypeVariableName,
                                        functionMetadataObjectClassBuilder,
                                        objectPropertyTypeMetadata
                                    )
                                is AppFunctionReferenceTypeMetadata ->
                                    addPropertyForReferenceTypeMetadata(
                                        dataTypeVariableName,
                                        functionMetadataObjectClassBuilder,
                                        objectPropertyTypeMetadata
                                    )
                                else -> {
                                    // TODO provide KSNode to improve error message
                                    throw ProcessingException(
                                        "Unable to build metadata for unknown object property " +
                                            "datatype: $objectPropertyTypeMetadata",
                                        null
                                    )
                                }
                            }
                            addStatement(
                                """
                                %S to %L,
                                """
                                    .trimIndent(),
                                objectPropertyName,
                                dataTypeVariableName
                            )
                        }
                        unindent()
                        addStatement(")")
                    }
                )
                .build()
        )
    }

    /** Creates the `functionIdToMetadataMap` property of the `AppFunctionInventory`. */
    private fun addFunctionIdToMetadataMapProperty(
        inventoryClassBuilder: TypeSpec.Builder,
        appFunctionMetadataList: List<AppFunctionMetadata>
    ) {
        inventoryClassBuilder.addProperty(
            PropertySpec.builder(
                    FUNCTION_ID_TO_METADATA_MAP_PROPERTY_NAME,
                    Map::class.asClassName()
                        .parameterizedBy(
                            String::class.asClassName(),
                            IntrospectionHelper.APP_FUNCTION_METADATA_CLASS
                        ),
                )
                .addModifiers(KModifier.OVERRIDE)
                .initializer(
                    buildCodeBlock {
                        addStatement("mapOf(")
                        indent()
                        for (appFunctionMetadata in appFunctionMetadataList) {
                            addStatement(
                                """
                                %S to %L.%L,
                                """
                                    .trimIndent(),
                                appFunctionMetadata.id,
                                getFunctionMetadataObjectClassName(appFunctionMetadata.id),
                                APP_FUNCTION_METADATA_PROPERTY_NAME
                            )
                        }
                        unindent()
                        addStatement(")")
                    }
                )
                .build()
        )
    }

    private fun addSchemaMetadataPropertyForFunction(
        functionMetadataObjectClassBuilder: TypeSpec.Builder,
        schemaMetadata: AppFunctionSchemaMetadata?
    ) {
        functionMetadataObjectClassBuilder.addProperty(
            PropertySpec.builder(
                    SCHEMA_METADATA_PROPERTY_NAME,
                    IntrospectionHelper.APP_FUNCTION_SCHEMA_METADATA_CLASS.copy(nullable = true)
                )
                .addModifiers(KModifier.PRIVATE)
                .initializer(
                    buildCodeBlock {
                        if (schemaMetadata == null) {
                            addStatement("%L", null)
                        } else {
                            addStatement(
                                "%T(category= %S, name=%S, version=%L)",
                                IntrospectionHelper.APP_FUNCTION_SCHEMA_METADATA_CLASS,
                                schemaMetadata.category,
                                schemaMetadata.name,
                                schemaMetadata.version
                            )
                        }
                    }
                )
                .build()
        )
    }

    // TODO: Remove doc once done with impl
    private fun buildSourceFilesKdoc(appFunctionClass: AnnotatedAppFunctions): CodeBlock {
        return buildCodeBlock {
            addStatement("Source Files:")
            for (file in appFunctionClass.getSourceFiles()) {
                addStatement(file.fileName)
            }
        }
    }

    private fun getAppFunctionInventoryClassName(functionClassName: String): String {
        return "$%s_AppFunctionInventory".format(functionClassName)
    }

    /**
     * Generates the name of the class for the metadata object of a function.
     *
     * @param functionId The ID of the function.
     * @return The name of the class.
     */
    private fun getFunctionMetadataObjectClassName(functionId: String): String {
        return functionId.replace("[^A-Za-z0-9]".toRegex(), "_").split("_").joinToString("") {
            it.replaceFirstChar { it.uppercase() }
        } + "MetadataObject"
    }

    /**
     * Generates the name of the property for the primitive type metadata of a parameter.
     *
     * @param parameterMetadata The metadata of the parameter.
     * @return The name of the property.
     */
    private fun getPrimitiveTypeMetadataPropertyNameForParameter(
        parameterMetadata: AppFunctionParameterMetadata
    ): String {
        return "PARAMETER_METADATA_${parameterMetadata.name.uppercase()}_PRIMITIVE_DATA_TYPE"
    }

    /**
     * Generates the name of the property for the array type metadata of a parameter.
     *
     * @param parameterMetadata The metadata of the parameter.
     * @return The name of the property.
     */
    private fun getArrayTypeMetadataPropertyNameForParameter(
        parameterMetadata: AppFunctionParameterMetadata
    ): String {
        return "PARAMETER_METADATA_${parameterMetadata.name.uppercase()}_ARRAY_DATA_TYPE"
    }

    /**
     * Generates the name of the property for the object type metadata of a parameter.
     *
     * @param parameterMetadata The metadata of the parameter.
     * @return The name of the property.
     */
    private fun getObjectTypeMetadataPropertyNameForParameter(
        parameterMetadata: AppFunctionParameterMetadata
    ): String {
        return "PARAMETER_METADATA_${parameterMetadata.name.uppercase()}_OBJECT_DATA_TYPE"
    }

    /**
     * Generates the name of the property for the reference type metadata of a parameter.
     *
     * @param parameterMetadata The metadata of the parameter.
     * @return The name of the property.
     */
    private fun getReferenceTypeMetadataPropertyNameForParameter(
        parameterMetadata: AppFunctionParameterMetadata
    ): String {
        return "PARAMETER_METADATA_${parameterMetadata.name.uppercase()}_REFERENCE_DATA_TYPE"
    }

    /**
     * Generates the name of the property for the object type metadata of a component.
     *
     * @param componentName The name of the component.
     * @return The name of the property.
     */
    private fun getObjectTypeMetadataPropertyNameForComponent(componentName: String): String {
        return "${componentName.uppercase().replace(".", "_")}_OBJECT_DATA_TYPE"
    }

    companion object {
        const val APP_FUNCTION_METADATA_PROPERTY_NAME = "APP_FUNCTION_METADATA"
        const val SCHEMA_METADATA_PROPERTY_NAME = "SCHEMA_METADATA"
        const val PARAMETER_METADATA_LIST_PROPERTY_NAME = "PARAMETER_METADATA_LIST"
        const val RESPONSE_METADATA_PROPERTY_NAME = "RESPONSE_METADATA"
        const val COMPONENT_METADATA_PROPERTY_NAME = "COMPONENTS_METADATA"
        const val FUNCTION_ID_TO_METADATA_MAP_PROPERTY_NAME = "functionIdToMetadataMap"
    }
}
