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

package androidx.appfunctions.metadata

import com.google.common.truth.Truth
import org.junit.Test

class AppFunctionMetadataTest {
    @Test
    fun testCreateAppFunctionMetadata_peripheralProperties() {
        val appFunctionMetadata =
            AppFunctionMetadata(
                id = "androidx.appfunctions.common.metadata#empty",
                isEnabledByDefault = true,
                isRestrictToTrustedCaller = false,
                displayNameRes = 100,
                schema =
                    AppFunctionSchemaMetadata(
                        schemaCategory = "exampleCategory",
                        schemaName = "exampleName",
                        schemaVersion = 200L
                    ),
                parameters = listOf<AppFunctionParameterMetadata>(),
                response =
                    AppFunctionResponseMetadata(
                        isNullable = false,
                        dataType =
                            AppFunctionDataTypeMetadata(type = AppFunctionDataTypeMetadata.UNIT)
                    ),
                components = AppFunctionComponentsMetadata(dataTypes = emptyList())
            )

        Truth.assertThat(appFunctionMetadata.id)
            .isEqualTo("androidx.appfunctions.common.metadata#empty")
        Truth.assertThat(appFunctionMetadata.isEnabledByDefault).isTrue()
        Truth.assertThat(appFunctionMetadata.isRestrictToTrustedCaller).isFalse()
        Truth.assertThat(appFunctionMetadata.displayNameRes).isEqualTo(100)
        Truth.assertThat(appFunctionMetadata.schema)
            .isEqualTo(
                AppFunctionSchemaMetadata(
                    schemaCategory = "exampleCategory",
                    schemaName = "exampleName",
                    schemaVersion = 200L
                )
            )
        Truth.assertThat(appFunctionMetadata.parameters).isEmpty()
        Truth.assertThat(appFunctionMetadata.response)
            .isEqualTo(
                AppFunctionResponseMetadata(
                    isNullable = false,
                    dataType = AppFunctionDataTypeMetadata(type = AppFunctionDataTypeMetadata.UNIT)
                )
            )
        Truth.assertThat(appFunctionMetadata.components)
            .isEqualTo(AppFunctionComponentsMetadata(dataTypes = emptyList()))
    }

    @Test
    fun testCreateAppFunctionMetadata_primitiveParameters() {
        val appFunctionMetadata =
            createTestAppFunctionMetadata(
                parameters =
                    listOf<AppFunctionParameterMetadata>(
                        AppFunctionParameterMetadata(
                            name = "requiredInt",
                            isRequired = true,
                            dataType =
                                AppFunctionDataTypeMetadata(type = AppFunctionDataTypeMetadata.INT)
                        ),
                        AppFunctionParameterMetadata(
                            name = "optionalInt",
                            isRequired = false,
                            dataType =
                                AppFunctionDataTypeMetadata(type = AppFunctionDataTypeMetadata.INT)
                        ),
                        AppFunctionParameterMetadata(
                            name = "requiredLong",
                            isRequired = true,
                            dataType =
                                AppFunctionDataTypeMetadata(type = AppFunctionDataTypeMetadata.LONG)
                        ),
                        AppFunctionParameterMetadata(
                            name = "optionalLong",
                            isRequired = false,
                            dataType =
                                AppFunctionDataTypeMetadata(type = AppFunctionDataTypeMetadata.LONG)
                        ),
                        AppFunctionParameterMetadata(
                            name = "requiredDouble",
                            isRequired = true,
                            dataType =
                                AppFunctionDataTypeMetadata(
                                    type = AppFunctionDataTypeMetadata.DOUBLE
                                )
                        ),
                        AppFunctionParameterMetadata(
                            name = "optionalDouble",
                            isRequired = false,
                            dataType =
                                AppFunctionDataTypeMetadata(
                                    type = AppFunctionDataTypeMetadata.DOUBLE
                                )
                        ),
                        AppFunctionParameterMetadata(
                            name = "requiredFloat",
                            isRequired = true,
                            dataType =
                                AppFunctionDataTypeMetadata(
                                    type = AppFunctionDataTypeMetadata.FLOAT
                                )
                        ),
                        AppFunctionParameterMetadata(
                            name = "optionalFloat",
                            isRequired = false,
                            dataType =
                                AppFunctionDataTypeMetadata(
                                    type = AppFunctionDataTypeMetadata.FLOAT
                                )
                        ),
                        AppFunctionParameterMetadata(
                            name = "requiredBoolean",
                            isRequired = true,
                            dataType =
                                AppFunctionDataTypeMetadata(
                                    type = AppFunctionDataTypeMetadata.BOOLEAN
                                )
                        ),
                        AppFunctionParameterMetadata(
                            name = "optionalBoolean",
                            isRequired = false,
                            dataType =
                                AppFunctionDataTypeMetadata(
                                    type = AppFunctionDataTypeMetadata.BOOLEAN
                                )
                        ),
                        AppFunctionParameterMetadata(
                            name = "requiredString",
                            isRequired = true,
                            dataType =
                                AppFunctionDataTypeMetadata(
                                    type = AppFunctionDataTypeMetadata.STRING
                                )
                        ),
                        AppFunctionParameterMetadata(
                            name = "optionalString",
                            isRequired = false,
                            dataType =
                                AppFunctionDataTypeMetadata(
                                    type = AppFunctionDataTypeMetadata.STRING
                                )
                        ),
                        AppFunctionParameterMetadata(
                            name = "requiredBytesArray",
                            isRequired = true,
                            dataType =
                                AppFunctionDataTypeMetadata(
                                    type = AppFunctionDataTypeMetadata.BYTES
                                )
                        ),
                        AppFunctionParameterMetadata(
                            name = "optionalBytesArray",
                            isRequired = false,
                            dataType =
                                AppFunctionDataTypeMetadata(
                                    type = AppFunctionDataTypeMetadata.BYTES
                                )
                        ),
                    ),
            )

        Truth.assertThat(appFunctionMetadata.parameters)
            .containsExactly(
                AppFunctionParameterMetadata(
                    name = "requiredInt",
                    isRequired = true,
                    dataType = AppFunctionDataTypeMetadata(type = AppFunctionDataTypeMetadata.INT)
                ),
                AppFunctionParameterMetadata(
                    name = "optionalInt",
                    isRequired = false,
                    dataType = AppFunctionDataTypeMetadata(type = AppFunctionDataTypeMetadata.INT)
                ),
                AppFunctionParameterMetadata(
                    name = "requiredLong",
                    isRequired = true,
                    dataType = AppFunctionDataTypeMetadata(type = AppFunctionDataTypeMetadata.LONG)
                ),
                AppFunctionParameterMetadata(
                    name = "optionalLong",
                    isRequired = false,
                    dataType = AppFunctionDataTypeMetadata(type = AppFunctionDataTypeMetadata.LONG)
                ),
                AppFunctionParameterMetadata(
                    name = "requiredDouble",
                    isRequired = true,
                    dataType =
                        AppFunctionDataTypeMetadata(type = AppFunctionDataTypeMetadata.DOUBLE)
                ),
                AppFunctionParameterMetadata(
                    name = "optionalDouble",
                    isRequired = false,
                    dataType =
                        AppFunctionDataTypeMetadata(type = AppFunctionDataTypeMetadata.DOUBLE)
                ),
                AppFunctionParameterMetadata(
                    name = "requiredFloat",
                    isRequired = true,
                    dataType = AppFunctionDataTypeMetadata(type = AppFunctionDataTypeMetadata.FLOAT)
                ),
                AppFunctionParameterMetadata(
                    name = "optionalFloat",
                    isRequired = false,
                    dataType = AppFunctionDataTypeMetadata(type = AppFunctionDataTypeMetadata.FLOAT)
                ),
                AppFunctionParameterMetadata(
                    name = "requiredBoolean",
                    isRequired = true,
                    dataType =
                        AppFunctionDataTypeMetadata(type = AppFunctionDataTypeMetadata.BOOLEAN)
                ),
                AppFunctionParameterMetadata(
                    name = "optionalBoolean",
                    isRequired = false,
                    dataType =
                        AppFunctionDataTypeMetadata(type = AppFunctionDataTypeMetadata.BOOLEAN)
                ),
                AppFunctionParameterMetadata(
                    name = "requiredString",
                    isRequired = true,
                    dataType =
                        AppFunctionDataTypeMetadata(type = AppFunctionDataTypeMetadata.STRING)
                ),
                AppFunctionParameterMetadata(
                    name = "optionalString",
                    isRequired = false,
                    dataType =
                        AppFunctionDataTypeMetadata(type = AppFunctionDataTypeMetadata.STRING)
                ),
                AppFunctionParameterMetadata(
                    name = "requiredBytesArray",
                    isRequired = true,
                    dataType = AppFunctionDataTypeMetadata(type = AppFunctionDataTypeMetadata.BYTES)
                ),
                AppFunctionParameterMetadata(
                    name = "optionalBytesArray",
                    isRequired = false,
                    dataType = AppFunctionDataTypeMetadata(type = AppFunctionDataTypeMetadata.BYTES)
                ),
            )
    }

    @Test
    fun testCreateAppFunctionMetadata_primitiveArrayParameters() {
        val appFunctionMetadata =
            createTestAppFunctionMetadata(
                parameters =
                    listOf(
                        AppFunctionParameterMetadata(
                            name = "nonNullIntList",
                            isRequired = true,
                            dataType =
                                AppFunctionDataTypeMetadata(
                                    type = AppFunctionDataTypeMetadata.ARRAY,
                                    itemType =
                                        AppFunctionItemTypeMetadata(
                                            isNullable = false,
                                            dataType =
                                                AppFunctionDataTypeMetadata(
                                                    type = AppFunctionDataTypeMetadata.INT
                                                )
                                        )
                                ),
                        ),
                        AppFunctionParameterMetadata(
                            name = "nullableIntList",
                            isRequired = true,
                            dataType =
                                AppFunctionDataTypeMetadata(
                                    type = AppFunctionDataTypeMetadata.ARRAY,
                                    itemType =
                                        AppFunctionItemTypeMetadata(
                                            isNullable = true,
                                            dataType =
                                                AppFunctionDataTypeMetadata(
                                                    type = AppFunctionDataTypeMetadata.INT
                                                )
                                        )
                                ),
                        ),
                        AppFunctionParameterMetadata(
                            name = "nonNullLongList",
                            isRequired = true,
                            dataType =
                                AppFunctionDataTypeMetadata(
                                    type = AppFunctionDataTypeMetadata.ARRAY,
                                    itemType =
                                        AppFunctionItemTypeMetadata(
                                            isNullable = false,
                                            dataType =
                                                AppFunctionDataTypeMetadata(
                                                    type = AppFunctionDataTypeMetadata.LONG
                                                )
                                        )
                                ),
                        ),
                        AppFunctionParameterMetadata(
                            name = "nullableLongList",
                            isRequired = true,
                            dataType =
                                AppFunctionDataTypeMetadata(
                                    type = AppFunctionDataTypeMetadata.ARRAY,
                                    itemType =
                                        AppFunctionItemTypeMetadata(
                                            isNullable = true,
                                            dataType =
                                                AppFunctionDataTypeMetadata(
                                                    type = AppFunctionDataTypeMetadata.LONG
                                                )
                                        )
                                ),
                        ),
                        AppFunctionParameterMetadata(
                            name = "nonNullDoubleList",
                            isRequired = true,
                            dataType =
                                AppFunctionDataTypeMetadata(
                                    type = AppFunctionDataTypeMetadata.ARRAY,
                                    itemType =
                                        AppFunctionItemTypeMetadata(
                                            isNullable = false,
                                            dataType =
                                                AppFunctionDataTypeMetadata(
                                                    type = AppFunctionDataTypeMetadata.DOUBLE
                                                )
                                        )
                                ),
                        ),
                        AppFunctionParameterMetadata(
                            name = "nullableDoubleList",
                            isRequired = true,
                            dataType =
                                AppFunctionDataTypeMetadata(
                                    type = AppFunctionDataTypeMetadata.ARRAY,
                                    itemType =
                                        AppFunctionItemTypeMetadata(
                                            isNullable = true,
                                            dataType =
                                                AppFunctionDataTypeMetadata(
                                                    type = AppFunctionDataTypeMetadata.DOUBLE
                                                )
                                        )
                                ),
                        ),
                        AppFunctionParameterMetadata(
                            name = "nonNullFloatList",
                            isRequired = true,
                            dataType =
                                AppFunctionDataTypeMetadata(
                                    type = AppFunctionDataTypeMetadata.ARRAY,
                                    itemType =
                                        AppFunctionItemTypeMetadata(
                                            isNullable = false,
                                            dataType =
                                                AppFunctionDataTypeMetadata(
                                                    type = AppFunctionDataTypeMetadata.FLOAT
                                                )
                                        )
                                ),
                        ),
                        AppFunctionParameterMetadata(
                            name = "nullableFloatList",
                            isRequired = true,
                            dataType =
                                AppFunctionDataTypeMetadata(
                                    type = AppFunctionDataTypeMetadata.ARRAY,
                                    itemType =
                                        AppFunctionItemTypeMetadata(
                                            isNullable = true,
                                            dataType =
                                                AppFunctionDataTypeMetadata(
                                                    type = AppFunctionDataTypeMetadata.FLOAT
                                                )
                                        )
                                ),
                        ),
                        AppFunctionParameterMetadata(
                            name = "nonNullBooleanList",
                            isRequired = true,
                            dataType =
                                AppFunctionDataTypeMetadata(
                                    type = AppFunctionDataTypeMetadata.ARRAY,
                                    itemType =
                                        AppFunctionItemTypeMetadata(
                                            isNullable = false,
                                            dataType =
                                                AppFunctionDataTypeMetadata(
                                                    type = AppFunctionDataTypeMetadata.BOOLEAN
                                                )
                                        )
                                ),
                        ),
                        AppFunctionParameterMetadata(
                            name = "nullableBooleanList",
                            isRequired = true,
                            dataType =
                                AppFunctionDataTypeMetadata(
                                    type = AppFunctionDataTypeMetadata.ARRAY,
                                    itemType =
                                        AppFunctionItemTypeMetadata(
                                            isNullable = true,
                                            dataType =
                                                AppFunctionDataTypeMetadata(
                                                    type = AppFunctionDataTypeMetadata.BOOLEAN
                                                )
                                        )
                                ),
                        ),
                        AppFunctionParameterMetadata(
                            name = "nonNullStringList",
                            isRequired = true,
                            dataType =
                                AppFunctionDataTypeMetadata(
                                    type = AppFunctionDataTypeMetadata.ARRAY,
                                    itemType =
                                        AppFunctionItemTypeMetadata(
                                            isNullable = false,
                                            dataType =
                                                AppFunctionDataTypeMetadata(
                                                    type = AppFunctionDataTypeMetadata.STRING
                                                )
                                        )
                                ),
                        ),
                        AppFunctionParameterMetadata(
                            name = "nullableStringList",
                            isRequired = true,
                            dataType =
                                AppFunctionDataTypeMetadata(
                                    type = AppFunctionDataTypeMetadata.ARRAY,
                                    itemType =
                                        AppFunctionItemTypeMetadata(
                                            isNullable = true,
                                            dataType =
                                                AppFunctionDataTypeMetadata(
                                                    type = AppFunctionDataTypeMetadata.STRING
                                                )
                                        )
                                ),
                        ),
                        AppFunctionParameterMetadata(
                            name = "nonNullByteArrayList",
                            isRequired = true,
                            dataType =
                                AppFunctionDataTypeMetadata(
                                    type = AppFunctionDataTypeMetadata.ARRAY,
                                    itemType =
                                        AppFunctionItemTypeMetadata(
                                            isNullable = false,
                                            dataType =
                                                AppFunctionDataTypeMetadata(
                                                    type = AppFunctionDataTypeMetadata.BYTES
                                                )
                                        )
                                ),
                        ),
                        AppFunctionParameterMetadata(
                            name = "nullableByteArrayList",
                            isRequired = true,
                            dataType =
                                AppFunctionDataTypeMetadata(
                                    type = AppFunctionDataTypeMetadata.ARRAY,
                                    itemType =
                                        AppFunctionItemTypeMetadata(
                                            isNullable = true,
                                            dataType =
                                                AppFunctionDataTypeMetadata(
                                                    type = AppFunctionDataTypeMetadata.BYTES
                                                )
                                        )
                                ),
                        ),
                    )
            )

        Truth.assertThat(appFunctionMetadata.parameters)
            .containsExactly(
                AppFunctionParameterMetadata(
                    name = "nonNullIntList",
                    isRequired = true,
                    dataType =
                        AppFunctionDataTypeMetadata(
                            type = AppFunctionDataTypeMetadata.ARRAY,
                            itemType =
                                AppFunctionItemTypeMetadata(
                                    isNullable = false,
                                    dataType =
                                        AppFunctionDataTypeMetadata(
                                            type = AppFunctionDataTypeMetadata.INT
                                        )
                                )
                        ),
                ),
                AppFunctionParameterMetadata(
                    name = "nullableIntList",
                    isRequired = true,
                    dataType =
                        AppFunctionDataTypeMetadata(
                            type = AppFunctionDataTypeMetadata.ARRAY,
                            itemType =
                                AppFunctionItemTypeMetadata(
                                    isNullable = true,
                                    dataType =
                                        AppFunctionDataTypeMetadata(
                                            type = AppFunctionDataTypeMetadata.INT
                                        )
                                )
                        ),
                ),
                AppFunctionParameterMetadata(
                    name = "nonNullLongList",
                    isRequired = true,
                    dataType =
                        AppFunctionDataTypeMetadata(
                            type = AppFunctionDataTypeMetadata.ARRAY,
                            itemType =
                                AppFunctionItemTypeMetadata(
                                    isNullable = false,
                                    dataType =
                                        AppFunctionDataTypeMetadata(
                                            type = AppFunctionDataTypeMetadata.LONG
                                        )
                                )
                        ),
                ),
                AppFunctionParameterMetadata(
                    name = "nullableLongList",
                    isRequired = true,
                    dataType =
                        AppFunctionDataTypeMetadata(
                            type = AppFunctionDataTypeMetadata.ARRAY,
                            itemType =
                                AppFunctionItemTypeMetadata(
                                    isNullable = true,
                                    dataType =
                                        AppFunctionDataTypeMetadata(
                                            type = AppFunctionDataTypeMetadata.LONG
                                        )
                                )
                        ),
                ),
                AppFunctionParameterMetadata(
                    name = "nonNullDoubleList",
                    isRequired = true,
                    dataType =
                        AppFunctionDataTypeMetadata(
                            type = AppFunctionDataTypeMetadata.ARRAY,
                            itemType =
                                AppFunctionItemTypeMetadata(
                                    isNullable = false,
                                    dataType =
                                        AppFunctionDataTypeMetadata(
                                            type = AppFunctionDataTypeMetadata.DOUBLE
                                        )
                                )
                        ),
                ),
                AppFunctionParameterMetadata(
                    name = "nullableDoubleList",
                    isRequired = true,
                    dataType =
                        AppFunctionDataTypeMetadata(
                            type = AppFunctionDataTypeMetadata.ARRAY,
                            itemType =
                                AppFunctionItemTypeMetadata(
                                    isNullable = true,
                                    dataType =
                                        AppFunctionDataTypeMetadata(
                                            type = AppFunctionDataTypeMetadata.DOUBLE
                                        )
                                )
                        ),
                ),
                AppFunctionParameterMetadata(
                    name = "nonNullFloatList",
                    isRequired = true,
                    dataType =
                        AppFunctionDataTypeMetadata(
                            type = AppFunctionDataTypeMetadata.ARRAY,
                            itemType =
                                AppFunctionItemTypeMetadata(
                                    isNullable = false,
                                    dataType =
                                        AppFunctionDataTypeMetadata(
                                            type = AppFunctionDataTypeMetadata.FLOAT
                                        )
                                )
                        ),
                ),
                AppFunctionParameterMetadata(
                    name = "nullableFloatList",
                    isRequired = true,
                    dataType =
                        AppFunctionDataTypeMetadata(
                            type = AppFunctionDataTypeMetadata.ARRAY,
                            itemType =
                                AppFunctionItemTypeMetadata(
                                    isNullable = true,
                                    dataType =
                                        AppFunctionDataTypeMetadata(
                                            type = AppFunctionDataTypeMetadata.FLOAT
                                        )
                                )
                        ),
                ),
                AppFunctionParameterMetadata(
                    name = "nonNullBooleanList",
                    isRequired = true,
                    dataType =
                        AppFunctionDataTypeMetadata(
                            type = AppFunctionDataTypeMetadata.ARRAY,
                            itemType =
                                AppFunctionItemTypeMetadata(
                                    isNullable = false,
                                    dataType =
                                        AppFunctionDataTypeMetadata(
                                            type = AppFunctionDataTypeMetadata.BOOLEAN
                                        )
                                )
                        ),
                ),
                AppFunctionParameterMetadata(
                    name = "nullableBooleanList",
                    isRequired = true,
                    dataType =
                        AppFunctionDataTypeMetadata(
                            type = AppFunctionDataTypeMetadata.ARRAY,
                            itemType =
                                AppFunctionItemTypeMetadata(
                                    isNullable = true,
                                    dataType =
                                        AppFunctionDataTypeMetadata(
                                            type = AppFunctionDataTypeMetadata.BOOLEAN
                                        )
                                )
                        ),
                ),
                AppFunctionParameterMetadata(
                    name = "nonNullStringList",
                    isRequired = true,
                    dataType =
                        AppFunctionDataTypeMetadata(
                            type = AppFunctionDataTypeMetadata.ARRAY,
                            itemType =
                                AppFunctionItemTypeMetadata(
                                    isNullable = false,
                                    dataType =
                                        AppFunctionDataTypeMetadata(
                                            type = AppFunctionDataTypeMetadata.STRING
                                        )
                                )
                        ),
                ),
                AppFunctionParameterMetadata(
                    name = "nullableStringList",
                    isRequired = true,
                    dataType =
                        AppFunctionDataTypeMetadata(
                            type = AppFunctionDataTypeMetadata.ARRAY,
                            itemType =
                                AppFunctionItemTypeMetadata(
                                    isNullable = true,
                                    dataType =
                                        AppFunctionDataTypeMetadata(
                                            type = AppFunctionDataTypeMetadata.STRING
                                        )
                                )
                        ),
                ),
                AppFunctionParameterMetadata(
                    name = "nonNullByteArrayList",
                    isRequired = true,
                    dataType =
                        AppFunctionDataTypeMetadata(
                            type = AppFunctionDataTypeMetadata.ARRAY,
                            itemType =
                                AppFunctionItemTypeMetadata(
                                    isNullable = false,
                                    dataType =
                                        AppFunctionDataTypeMetadata(
                                            type = AppFunctionDataTypeMetadata.BYTES
                                        )
                                )
                        ),
                ),
                AppFunctionParameterMetadata(
                    name = "nullableByteArrayList",
                    isRequired = true,
                    dataType =
                        AppFunctionDataTypeMetadata(
                            type = AppFunctionDataTypeMetadata.ARRAY,
                            itemType =
                                AppFunctionItemTypeMetadata(
                                    isNullable = true,
                                    dataType =
                                        AppFunctionDataTypeMetadata(
                                            type = AppFunctionDataTypeMetadata.BYTES
                                        )
                                )
                        ),
                ),
            )
    }

    @Test
    fun testCreateAppFunctionMetadata_objectParameters() {
        val appFunctionMetadata =
            createTestAppFunctionMetadata(
                components =
                    AppFunctionComponentsMetadata(
                        dataTypes =
                            listOf(
                                AppFunctionDataTypeMetadata(
                                    id = "androidx.appfunctions.common.TestClass",
                                    type = AppFunctionDataTypeMetadata.OBJECT,
                                    properties =
                                        listOf(
                                            AppFunctionParameterMetadata(
                                                name = "testProperty",
                                                isRequired = true,
                                                dataType =
                                                    AppFunctionDataTypeMetadata(
                                                        type = AppFunctionDataTypeMetadata.INT
                                                    )
                                            )
                                        )
                                )
                            )
                    ),
                parameters =
                    listOf(
                        AppFunctionParameterMetadata(
                            name = "testClass",
                            isRequired = false,
                            referenceDataType =
                                "#components/dataSchemas/androidx.appfunctions.common.TestClass"
                        )
                    )
            )

        Truth.assertThat(appFunctionMetadata.components.dataTypes)
            .containsExactly(
                AppFunctionDataTypeMetadata(
                    id = "androidx.appfunctions.common.TestClass",
                    type = AppFunctionDataTypeMetadata.OBJECT,
                    properties =
                        listOf(
                            AppFunctionParameterMetadata(
                                name = "testProperty",
                                isRequired = true,
                                dataType =
                                    AppFunctionDataTypeMetadata(
                                        type = AppFunctionDataTypeMetadata.INT
                                    )
                            )
                        )
                )
            )
        Truth.assertThat(appFunctionMetadata.parameters)
            .containsExactly(
                AppFunctionParameterMetadata(
                    name = "testClass",
                    isRequired = false,
                    referenceDataType =
                        "#components/dataSchemas/androidx.appfunctions.common.TestClass"
                )
            )
    }

    @Test
    fun testCreateAppFunctionMetadata_objectArrayParameter() {
        val appFunctionMetadata =
            createTestAppFunctionMetadata(
                components =
                    AppFunctionComponentsMetadata(
                        dataTypes =
                            listOf(
                                AppFunctionDataTypeMetadata(
                                    id = "androidx.appfunctions.common.TestClass",
                                    type = AppFunctionDataTypeMetadata.OBJECT,
                                    properties =
                                        listOf(
                                            AppFunctionParameterMetadata(
                                                name = "testProperty",
                                                isRequired = true,
                                                dataType =
                                                    AppFunctionDataTypeMetadata(
                                                        type = AppFunctionDataTypeMetadata.INT
                                                    )
                                            )
                                        )
                                )
                            )
                    ),
                parameters =
                    listOf(
                        AppFunctionParameterMetadata(
                            name = "testClass",
                            isRequired = false,
                            dataType =
                                AppFunctionDataTypeMetadata(
                                    type = AppFunctionDataTypeMetadata.ARRAY,
                                    itemType =
                                        AppFunctionItemTypeMetadata(
                                            isNullable = false,
                                            referenceDataType =
                                                "#components/dataSchemas/" +
                                                    "androidx.appfunctions.common.TestClass"
                                        )
                                )
                        )
                    )
            )

        Truth.assertThat(appFunctionMetadata.components.dataTypes)
            .containsExactly(
                AppFunctionDataTypeMetadata(
                    id = "androidx.appfunctions.common.TestClass",
                    type = AppFunctionDataTypeMetadata.OBJECT,
                    properties =
                        listOf(
                            AppFunctionParameterMetadata(
                                name = "testProperty",
                                isRequired = true,
                                dataType =
                                    AppFunctionDataTypeMetadata(
                                        type = AppFunctionDataTypeMetadata.INT
                                    )
                            )
                        )
                )
            )
        Truth.assertThat(appFunctionMetadata.parameters)
            .containsExactly(
                AppFunctionParameterMetadata(
                    name = "testClass",
                    isRequired = false,
                    dataType =
                        AppFunctionDataTypeMetadata(
                            type = AppFunctionDataTypeMetadata.ARRAY,
                            itemType =
                                AppFunctionItemTypeMetadata(
                                    isNullable = false,
                                    referenceDataType =
                                        "#components/dataSchemas/" +
                                            "androidx.appfunctions.common.TestClass"
                                )
                        )
                )
            )
    }

    private fun createTestAppFunctionMetadata(
        id: String = "androidx.appfunctions.common.metadata#defaultId",
        isEnabledByDefault: Boolean = true,
        isRestrictToTrustedCaller: Boolean = false,
        displayNameRes: Long = 0L,
        schemaMetadata: AppFunctionSchemaMetadata? = null,
        parameters: List<AppFunctionParameterMetadata> = emptyList(),
        response: AppFunctionResponseMetadata =
            AppFunctionResponseMetadata(
                isNullable = false,
                dataType = AppFunctionDataTypeMetadata(type = AppFunctionDataTypeMetadata.UNIT)
            ),
        components: AppFunctionComponentsMetadata =
            AppFunctionComponentsMetadata(dataTypes = emptyList())
    ): AppFunctionMetadata {
        return AppFunctionMetadata(
            id = id,
            isEnabledByDefault = isEnabledByDefault,
            isRestrictToTrustedCaller = isRestrictToTrustedCaller,
            displayNameRes = displayNameRes,
            schema = schemaMetadata,
            parameters = parameters,
            response = response,
            components = components
        )
    }
}
