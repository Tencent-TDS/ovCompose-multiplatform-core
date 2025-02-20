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

import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.compiler.core.AnnotatedAppFunctionSerializable
import androidx.appfunctions.compiler.core.AppFunctionTypeReference
import androidx.appfunctions.compiler.core.AppFunctionTypeReference.AppFunctionSupportedTypeCategory.PRIMITIVE_ARRAY
import androidx.appfunctions.compiler.core.AppFunctionTypeReference.AppFunctionSupportedTypeCategory.PRIMITIVE_LIST
import androidx.appfunctions.compiler.core.AppFunctionTypeReference.AppFunctionSupportedTypeCategory.PRIMITIVE_SINGULAR
import androidx.appfunctions.compiler.core.AppFunctionTypeReference.AppFunctionSupportedTypeCategory.SERIALIZABLE_LIST
import androidx.appfunctions.compiler.core.AppFunctionTypeReference.AppFunctionSupportedTypeCategory.SERIALIZABLE_SINGULAR
import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionSerializableFactoryClass.FromAppFunctionDataMethod
import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionSerializableFactoryClass.FromAppFunctionDataMethod.APP_FUNCTION_DATA_PARAM_NAME
import androidx.appfunctions.compiler.core.ensureQualifiedTypeName
import androidx.appfunctions.compiler.core.toTypeName
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.buildCodeBlock

/**
 * Wraps methods to build the [CodeBlock]s that make up the method bodies of the generated
 * AppFunctionSerializableFactory.
 */
// TODO(b/392587953): extract common format maps
class AppFunctionSerializableFactoryCodeBuilder(
    val annotatedClass: AnnotatedAppFunctionSerializable
) {
    /** Builds and appends the method body of fromAppFunctionData to the given code block. */
    fun appendFromAppFunctionDataMethodBody(): CodeBlock {
        return buildCodeBlock {
            add(factoryInitStatements)
            for (property in annotatedClass.getProperties()) {
                appendGetterStatement(property)
            }
            appendGetterReturnStatement(
                annotatedClass.originalClassName,
                annotatedClass.getProperties()
            )
        }
    }

    /** Builds and appends the method body of fromAppFunctionData to the given code block. */
    fun appendToAppFunctionDataMethodBody(): CodeBlock {
        return buildCodeBlock {
            add(factoryInitStatements)
            val qualifiedClassName =
                checkNotNull(annotatedClass.appFunctionSerializableClass.qualifiedName).asString()
            addStatement("val builder = %T(%S)", AppFunctionData.Builder::class, qualifiedClassName)
            for (property in annotatedClass.getProperties()) {
                val afType = AppFunctionTypeReference(property.type)
                if (afType.isNullable) {
                    appendNullableSetterStatement(property, afType)
                } else {
                    appendSetterStatement(property, afType)
                }
            }
            add("\nreturn builder.build()")
        }
    }

    private fun CodeBlock.Builder.appendGetterStatement(
        param: KSValueParameter
    ): CodeBlock.Builder {
        val afType = AppFunctionTypeReference(param.type)
        return when (afType.typeCategory) {
            PRIMITIVE_SINGULAR,
            PRIMITIVE_ARRAY,
            PRIMITIVE_LIST -> appendPrimitiveGetterStatement(param, afType)
            SERIALIZABLE_SINGULAR -> appendSerializableGetterStatement(param, afType)
            SERIALIZABLE_LIST -> appendSerializableListGetterStatement(param, afType)
        }
    }

    private fun CodeBlock.Builder.appendPrimitiveGetterStatement(
        param: KSValueParameter,
        afType: AppFunctionTypeReference
    ): CodeBlock.Builder {
        val formatStringMap =
            mapOf<String, Any>(
                "param_name" to checkNotNull(param.name).asString(),
                "app_function_data_param_name" to APP_FUNCTION_DATA_PARAM_NAME,
                "getter_name" to getAppFunctionDataGetterName(afType),
                "default_value_postfix" to getGetterDefaultValuePostfix(afType)
            )
        addNamed(
            "val %param_name:L = %app_function_data_param_name:L.%getter_name:L(\"%param_name:L\")%default_value_postfix:L\n",
            formatStringMap
        )
        return this
    }

    private fun CodeBlock.Builder.appendSerializableGetterStatement(
        param: KSValueParameter,
        afType: AppFunctionTypeReference
    ): CodeBlock.Builder {
        val paramName = checkNotNull(param.name).asString()
        val typeName = afType.selfTypeReference.getTypeShortName()
        val formatStringMap =
            mapOf<String, Any>(
                "param_name" to paramName,
                "param_type" to param.type.toTypeName(),
                "factory_name" to "${typeName}Factory".lowerFirstChar(),
                "app_function_data_param_name" to APP_FUNCTION_DATA_PARAM_NAME,
                "getter_name" to getAppFunctionDataGetterName(afType),
                "from_app_function_data_method_name" to FromAppFunctionDataMethod.METHOD_NAME,
                "serializable_data_val_name" to "${paramName}Data"
            )

        addNamed(
            "val %serializable_data_val_name:L = %app_function_data_param_name:L.%getter_name:L(%param_name:S)\n",
            formatStringMap
        )
        if (afType.isNullable) {
            return addNamed("var %param_name:L: %param_type:T = null\n", formatStringMap)
                .addNamed("if (%serializable_data_val_name:L != null) {\n", formatStringMap)
                .indent()
                .addNamed(
                    "%param_name:L = %factory_name:L.%from_app_function_data_method_name:L(%serializable_data_val_name:L)\n",
                    formatStringMap
                )
                .unindent()
                .addStatement("}")
        } else {
            addNamed(
                "val %param_name:L = %factory_name:L.%from_app_function_data_method_name:L(%serializable_data_val_name:L)\n",
                formatStringMap
            )
        }
        return this
    }

    private fun CodeBlock.Builder.appendSerializableListGetterStatement(
        param: KSValueParameter,
        afType: AppFunctionTypeReference
    ): CodeBlock.Builder {
        val parametrizedTypeName = afType.itemTypeReference.getTypeShortName()
        val factoryName = parametrizedTypeName + "Factory"
        val factoryInstanceName = factoryName.lowerFirstChar()
        val formatStringMap =
            mapOf<String, Any>(
                "param_name" to checkNotNull(param.name).asString(),
                "temp_list_name" to checkNotNull(param.name).asString() + "Data",
                "app_function_data_param_name" to APP_FUNCTION_DATA_PARAM_NAME,
                "factory_instance_name" to factoryInstanceName,
                "getter_name" to getAppFunctionDataGetterName(afType),
                "default_value_postfix" to getGetterDefaultValuePostfix(afType),
                "null_safe_op" to if (afType.isNullable) "?" else ""
            )

        addNamed(
                "val %temp_list_name:L = %app_function_data_param_name:L.%getter_name:L(\"%param_name:L\")%default_value_postfix:L\n",
                formatStringMap
            )
            .addNamed(
                "val %param_name:L = %temp_list_name:L%null_safe_op:L.map { data ->\n",
                formatStringMap
            )
            .indent()
            .addNamed("%factory_instance_name:L.fromAppFunctionData(data)\n", formatStringMap)
            .unindent()
            .addStatement("}")
        return this
    }

    private fun CodeBlock.Builder.appendGetterReturnStatement(
        originalClassName: ClassName,
        params: List<KSValueParameter>
    ): CodeBlock.Builder {
        val formatStringMap =
            mapOf<String, Any>(
                "original_class_name" to originalClassName,
                "params_list" to
                    params.joinToString(", ") { param -> checkNotNull(param.name).asString() }
            )

        addNamed("\nreturn %original_class_name:T(%params_list:L)", formatStringMap)
        return this
    }

    private fun CodeBlock.Builder.appendNullableSetterStatement(
        param: KSValueParameter,
        type: AppFunctionTypeReference
    ): CodeBlock.Builder {
        val formatStringMap =
            mapOf<String, Any>(
                "param_name" to checkNotNull(param.name).asString(),
                "annotated_class_instance" to
                    annotatedClass.originalClassName.simpleName.lowerFirstChar()
            )

        return addNamed(
                "if (%annotated_class_instance:L.%param_name:L != null) {\n",
                formatStringMap
            )
            .indent()
            .appendSetterStatement(param, type)
            .unindent()
            .addStatement("}")
    }

    private fun CodeBlock.Builder.appendSetterStatement(
        param: KSValueParameter,
        afType: AppFunctionTypeReference
    ): CodeBlock.Builder {
        return when (afType.typeCategory) {
            PRIMITIVE_SINGULAR,
            PRIMITIVE_ARRAY,
            PRIMITIVE_LIST -> appendPrimitiveSetterStatement(param, afType)
            SERIALIZABLE_SINGULAR -> appendSerializableSetterStatement(param, afType)
            SERIALIZABLE_LIST -> appendSerializableListSetterStatement(param, afType)
        }
    }

    private fun CodeBlock.Builder.appendPrimitiveSetterStatement(
        param: KSValueParameter,
        afType: AppFunctionTypeReference
    ): CodeBlock.Builder {
        val formatStringMap =
            mapOf<String, Any>(
                "param_name" to checkNotNull(param.name).asString(),
                "setter_name" to getAppFunctionDataSetterName(afType),
                "annotated_class_instance" to
                    annotatedClass.originalClassName.simpleName.lowerFirstChar()
            )
        addNamed(
            "builder.%setter_name:L(\"%param_name:L\", %annotated_class_instance:L.%param_name:L)\n",
            formatStringMap
        )
        return this
    }

    private fun CodeBlock.Builder.appendSerializableSetterStatement(
        param: KSValueParameter,
        afType: AppFunctionTypeReference
    ): CodeBlock.Builder {
        val typeName = afType.selfTypeReference.getTypeShortName()
        val formatStringMap =
            mapOf<String, Any>(
                "param_name" to checkNotNull(param.name).asString(),
                "factory_name" to "${typeName}Factory".lowerFirstChar(),
                "setter_name" to getAppFunctionDataSetterName(afType),
                "annotated_class_instance" to
                    annotatedClass.originalClassName.simpleName.lowerFirstChar()
            )

        addNamed(
            "builder.%setter_name:L(\"%param_name:L\", %factory_name:L.toAppFunctionData(%annotated_class_instance:L.%param_name:L))\n",
            formatStringMap
        )
        return this
    }

    private fun CodeBlock.Builder.appendSerializableListSetterStatement(
        param: KSValueParameter,
        afType: AppFunctionTypeReference
    ): CodeBlock.Builder {
        val parametrizedTypeName = afType.selfOrItemTypeReference.getTypeShortName()

        val formatStringMap =
            mapOf<String, Any>(
                "param_name" to checkNotNull(param.name).asString(),
                "factory_name" to "${parametrizedTypeName}Factory".lowerFirstChar(),
                "setter_name" to getAppFunctionDataSetterName(afType),
                "annotated_class_instance" to
                    annotatedClass.originalClassName.simpleName.lowerFirstChar(),
                "lambda_param_name" to parametrizedTypeName.lowerFirstChar()
            )

        addNamed(
                "builder.%setter_name:L(\"%param_name:L\", " +
                    "%annotated_class_instance:L.%param_name:L" +
                    ".map{ %lambda_param_name:L ->\n",
                formatStringMap
            )
            .indent()
            .addNamed("%factory_name:L.toAppFunctionData(%lambda_param_name:L)\n", formatStringMap)
            .unindent()
            .addStatement("})")
        return this
    }

    private fun getAppFunctionDataGetterName(afType: AppFunctionTypeReference): String {
        val shortTypeName = afType.selfOrItemTypeReference.getTypeShortName()
        return when (afType.typeCategory) {
            PRIMITIVE_SINGULAR -> "get$shortTypeName${if (afType.isNullable) "OrNull" else ""}"
            PRIMITIVE_ARRAY -> "get$shortTypeName"
            SERIALIZABLE_SINGULAR -> "getAppFunctionData${if (afType.isNullable) "OrNull" else ""}"
            SERIALIZABLE_LIST -> "getAppFunctionDataList"
            PRIMITIVE_LIST -> "get${shortTypeName}List"
        }
    }

    // Missing list/array types default to an empty list/array; missing singular properties throw an
    // error; all nullable properties default to null.
    private fun getGetterDefaultValuePostfix(afType: AppFunctionTypeReference): String {
        return when (afType.typeCategory) {
            PRIMITIVE_SINGULAR,
            SERIALIZABLE_SINGULAR -> ""
            PRIMITIVE_ARRAY ->
                if (afType.isNullable) {
                    ""
                } else {
                    " ?: ${afType.selfOrItemTypeReference.getTypeShortName()}(0)"
                }
            PRIMITIVE_LIST,
            SERIALIZABLE_LIST -> if (afType.isNullable) "" else " ?: emptyList()"
        }
    }

    private fun getAppFunctionDataSetterName(afType: AppFunctionTypeReference): String {
        return when (afType.typeCategory) {
            PRIMITIVE_SINGULAR,
            PRIMITIVE_ARRAY -> "set${afType.selfOrItemTypeReference.getTypeShortName()}"
            PRIMITIVE_LIST -> "set${afType.selfOrItemTypeReference.getTypeShortName()}List"
            SERIALIZABLE_SINGULAR -> "setAppFunctionData"
            SERIALIZABLE_LIST -> "setAppFunctionDataList"
        }
    }

    private val factoryInitStatements = buildCodeBlock {
        val factoryInstanceNameToClassMap: Map<String, ClassName> = buildMap {
            for (serializableType in annotatedClass.getSerializablePropertyTypeReferences()) {
                val qualifiedName =
                    serializableType.selfOrItemTypeReference.ensureQualifiedTypeName()
                put(
                    "${qualifiedName.getShortName().lowerFirstChar()}Factory",
                    ClassName(
                        qualifiedName.getQualifier(),
                        "`\$${qualifiedName.getShortName()}Factory`"
                    )
                )
            }
        }
        for (entry in factoryInstanceNameToClassMap) {
            addStatement("val %L = %T()", entry.key, entry.value)
        }
        add("\n")
    }

    private fun KSTypeReference.getTypeShortName(): String {
        return this.ensureQualifiedTypeName().getShortName()
    }

    private fun String.lowerFirstChar(): String {
        return replaceFirstChar { it -> it.lowercase() }
    }
}
