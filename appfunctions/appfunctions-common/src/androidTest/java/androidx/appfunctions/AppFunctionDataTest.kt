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

package androidx.appfunctions

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appfunctions.metadata.AppFunctionArrayTypeMetadata
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionObjectTypeMetadata
import androidx.appfunctions.metadata.AppFunctionParameterMetadata
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata.Companion.TYPE_BOOLEAN
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata.Companion.TYPE_DOUBLE
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata.Companion.TYPE_LONG
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata.Companion.TYPE_STRING
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
class AppFunctionDataTest {

    @Test
    fun testReadWrite_asParameters_conformSpec() {
        val builder =
            AppFunctionData.Builder(
                TEST_PARAMETER_METADATA,
                AppFunctionComponentsMetadata(),
            )

        builder.setLong("long", 123L)
        builder.setDouble("double", 50.0)
        builder.setBoolean("boolean", true)
        builder.setString("string", "testString")
        builder.setLongArray("longArray", longArrayOf(1L, 2L, 3L))
        builder.setDoubleArray("doubleArray", doubleArrayOf(4.0, 5.0, 6.0))
        builder.setBooleanArray("booleanArray", booleanArrayOf(false, true, false))
        builder.setStringList("stringList", listOf("1", "2", "3"))
        val data = builder.build()

        assertThat(data.getLong("long")).isEqualTo(123L)
        assertThat(data.getDouble("double")).isEqualTo(50.0)
        assertThat(data.getBoolean("boolean")).isTrue()
        assertThat(data.getString("string")).isEqualTo("testString")
        assertThat(data.getLongArray("longArray")).asList().containsExactly(1L, 2L, 3L)
        assertThat(data.getDoubleArray("doubleArray"))
            .usingExactEquality()
            .containsExactly(4.0, 5.0, 6.0)
        assertThat(data.getBooleanArray("booleanArray"))
            .asList()
            .containsExactly(false, true, false)
        assertThat(data.getStringList("stringList")).containsExactly("1", "2", "3")
    }

    @Test
    fun testWrite_asParameters_notConformSpec() {
        val builder =
            AppFunctionData.Builder(
                TEST_PARAMETER_METADATA,
                AppFunctionComponentsMetadata(),
            )

        assertThrows(IllegalArgumentException::class.java) {
            builder.setLongArray("long", longArrayOf(100, 200))
        }
        assertThrows(IllegalArgumentException::class.java) { builder.setDouble("long", 50.0) }

        assertThrows(IllegalArgumentException::class.java) {
            builder.setDoubleArray("double", doubleArrayOf(50.0, 100.0))
        }
        assertThrows(IllegalArgumentException::class.java) { builder.setBoolean("double", true) }

        assertThrows(IllegalArgumentException::class.java) {
            builder.setBooleanArray("boolean", booleanArrayOf(false, true))
        }
        assertThrows(IllegalArgumentException::class.java) { builder.setLong("boolean", 100) }

        assertThrows(IllegalArgumentException::class.java) {
            builder.setStringList("string", listOf("test"))
        }
        assertThrows(IllegalArgumentException::class.java) { builder.setDouble("string", 100.0) }

        assertThrows(IllegalArgumentException::class.java) { builder.setLong("longArray", 100L) }
        assertThrows(IllegalArgumentException::class.java) {
            builder.setDoubleArray("longArray", doubleArrayOf(2.0))
        }

        assertThrows(IllegalArgumentException::class.java) { builder.setDouble("doubleArray", 1.0) }
        assertThrows(IllegalArgumentException::class.java) {
            builder.setBooleanArray("doubleArray", booleanArrayOf(false))
        }

        assertThrows(IllegalArgumentException::class.java) {
            builder.setBoolean("booleanArray", false)
        }
        assertThrows(IllegalArgumentException::class.java) {
            builder.setStringList("booleanArray", listOf("test1"))
        }

        assertThrows(IllegalArgumentException::class.java) {
            builder.setString("stringList", "test1")
        }
        assertThrows(IllegalArgumentException::class.java) {
            builder.setLongArray("stringList", longArrayOf(1))
        }
    }

    @Test
    fun testRead_asParameters_notConformSpec() {
        val builder =
            AppFunctionData.Builder(
                TEST_PARAMETER_METADATA,
                AppFunctionComponentsMetadata(),
            )
        builder.setLong("long", 123L)
        builder.setDouble("double", 50.0)
        builder.setBoolean("boolean", true)
        builder.setString("string", "testString")
        builder.setLongArray("longArray", longArrayOf(1L, 2L, 3L))
        builder.setDoubleArray("doubleArray", doubleArrayOf(4.0, 5.0, 6.0))
        builder.setBooleanArray("booleanArray", booleanArrayOf(false, true, false))
        builder.setStringList("stringList", listOf("1", "2", "3"))
        val data = builder.build()

        assertThrows(IllegalArgumentException::class.java) { data.getDouble("long") }
        assertThrows(IllegalArgumentException::class.java) { data.getLongArray("long") }

        assertThrows(IllegalArgumentException::class.java) { data.getBoolean("double") }
        assertThrows(IllegalArgumentException::class.java) { data.getDoubleArray("double") }

        assertThrows(IllegalArgumentException::class.java) { data.getString("boolean") }
        assertThrows(IllegalArgumentException::class.java) { data.getBooleanArray("boolean") }

        assertThrows(IllegalArgumentException::class.java) { data.getLong("string") }
        assertThrows(IllegalArgumentException::class.java) { data.getStringList("string") }

        assertThrows(IllegalArgumentException::class.java) { data.getDoubleArray("longArray") }
        assertThrows(IllegalArgumentException::class.java) { data.getLong("longArray") }

        assertThrows(IllegalArgumentException::class.java) { data.getBooleanArray("doubleArray") }
        assertThrows(IllegalArgumentException::class.java) { data.getDouble("doubleArray") }

        assertThrows(IllegalArgumentException::class.java) { data.getStringList("booleanArray") }
        assertThrows(IllegalArgumentException::class.java) { data.getBoolean("boolean Array") }

        assertThrows(IllegalArgumentException::class.java) { data.getLongArray("stringList") }
        assertThrows(IllegalArgumentException::class.java) { data.getString("stringList") }
    }

    @Test
    fun testReadWrite_asObject_conformSpec() {
        val builder = AppFunctionData.Builder(TEST_OBJECT_METADATA, AppFunctionComponentsMetadata())

        builder.setLong("long", 123L)
        builder.setDouble("double", 50.0)
        builder.setBoolean("boolean", true)
        builder.setString("string", "testString")
        builder.setLongArray("longArray", longArrayOf(1L, 2L, 3L))
        builder.setDoubleArray("doubleArray", doubleArrayOf(4.0, 5.0, 6.0))
        builder.setBooleanArray("booleanArray", booleanArrayOf(false, true, false))
        builder.setStringList("stringList", listOf("1", "2", "3"))
        val data = builder.build()

        assertThat(data.getLong("long")).isEqualTo(123L)
        assertThat(data.getDouble("double")).isEqualTo(50.0)
        assertThat(data.getBoolean("boolean")).isTrue()
        assertThat(data.getString("string")).isEqualTo("testString")
        assertThat(data.getLongArray("longArray")).asList().containsExactly(1L, 2L, 3L)
        assertThat(data.getDoubleArray("doubleArray"))
            .usingExactEquality()
            .containsExactly(4.0, 5.0, 6.0)
        assertThat(data.getBooleanArray("booleanArray"))
            .asList()
            .containsExactly(false, true, false)
        assertThat(data.getStringList("stringList")).containsExactly("1", "2", "3")
    }

    @Test
    fun testWrite_asObject_notConformSpec() {
        val builder = AppFunctionData.Builder(TEST_OBJECT_METADATA, AppFunctionComponentsMetadata())

        assertThrows(IllegalArgumentException::class.java) {
            builder.setLongArray("long", longArrayOf(100, 200))
        }
        assertThrows(IllegalArgumentException::class.java) { builder.setDouble("long", 50.0) }

        assertThrows(IllegalArgumentException::class.java) {
            builder.setDoubleArray("double", doubleArrayOf(50.0, 100.0))
        }
        assertThrows(IllegalArgumentException::class.java) { builder.setBoolean("double", true) }

        assertThrows(IllegalArgumentException::class.java) {
            builder.setBooleanArray("boolean", booleanArrayOf(false, true))
        }
        assertThrows(IllegalArgumentException::class.java) { builder.setLong("boolean", 100) }

        assertThrows(IllegalArgumentException::class.java) {
            builder.setStringList("string", listOf("test"))
        }
        assertThrows(IllegalArgumentException::class.java) { builder.setDouble("string", 100.0) }

        assertThrows(IllegalArgumentException::class.java) { builder.setLong("longArray", 100L) }
        assertThrows(IllegalArgumentException::class.java) {
            builder.setDoubleArray("longArray", doubleArrayOf(2.0))
        }

        assertThrows(IllegalArgumentException::class.java) { builder.setDouble("doubleArray", 1.0) }
        assertThrows(IllegalArgumentException::class.java) {
            builder.setBooleanArray("doubleArray", booleanArrayOf(false))
        }

        assertThrows(IllegalArgumentException::class.java) {
            builder.setBoolean("booleanArray", false)
        }
        assertThrows(IllegalArgumentException::class.java) {
            builder.setStringList("booleanArray", listOf("test1"))
        }

        assertThrows(IllegalArgumentException::class.java) {
            builder.setString("stringList", "test1")
        }
        assertThrows(IllegalArgumentException::class.java) {
            builder.setLongArray("stringList", longArrayOf(1))
        }
    }

    @Test
    fun testRead_asObject_notConformSpec() {
        val builder = AppFunctionData.Builder(TEST_OBJECT_METADATA, AppFunctionComponentsMetadata())
        builder.setLong("long", 123L)
        builder.setDouble("double", 50.0)
        builder.setBoolean("boolean", true)
        builder.setString("string", "testString")
        builder.setLongArray("longArray", longArrayOf(1L, 2L, 3L))
        builder.setDoubleArray("doubleArray", doubleArrayOf(4.0, 5.0, 6.0))
        builder.setBooleanArray("booleanArray", booleanArrayOf(false, true, false))
        builder.setStringList("stringList", listOf("1", "2", "3"))
        val data = builder.build()

        assertThrows(IllegalArgumentException::class.java) { data.getDouble("long") }
        assertThrows(IllegalArgumentException::class.java) { data.getLongArray("long") }

        assertThrows(IllegalArgumentException::class.java) { data.getBoolean("double") }
        assertThrows(IllegalArgumentException::class.java) { data.getDoubleArray("double") }

        assertThrows(IllegalArgumentException::class.java) { data.getString("boolean") }
        assertThrows(IllegalArgumentException::class.java) { data.getBooleanArray("boolean") }

        assertThrows(IllegalArgumentException::class.java) { data.getLong("string") }
        assertThrows(IllegalArgumentException::class.java) { data.getStringList("string") }

        assertThrows(IllegalArgumentException::class.java) { data.getDoubleArray("longArray") }
        assertThrows(IllegalArgumentException::class.java) { data.getLong("longArray") }

        assertThrows(IllegalArgumentException::class.java) { data.getBooleanArray("doubleArray") }
        assertThrows(IllegalArgumentException::class.java) { data.getDouble("doubleArray") }

        assertThrows(IllegalArgumentException::class.java) { data.getStringList("booleanArray") }
        assertThrows(IllegalArgumentException::class.java) { data.getBoolean("boolean Array") }

        assertThrows(IllegalArgumentException::class.java) { data.getLongArray("stringList") }
        assertThrows(IllegalArgumentException::class.java) { data.getString("stringList") }
    }

    @Test
    fun testReadWrite_nestedObjectParameter() {
        val data =
            AppFunctionData.Builder(TEST_NESTED_PARAMETER_METADATA, AppFunctionComponentsMetadata())
                .setAppFunctionData(
                    "data",
                    AppFunctionData.Builder(TEST_OBJECT_METADATA, AppFunctionComponentsMetadata())
                        .setLong("long", 100)
                        .build()
                )
                .setAppFunctionDataList(
                    "dataList",
                    listOf(
                        AppFunctionData.Builder(
                                TEST_OBJECT_METADATA,
                                AppFunctionComponentsMetadata()
                            )
                            .setDouble("double", 20.0)
                            .build(),
                        AppFunctionData.Builder(
                                TEST_OBJECT_METADATA,
                                AppFunctionComponentsMetadata()
                            )
                            .setString("string", "testString")
                            .build()
                    )
                )
                .build()

        assertThat(data.getAppFunctionData("data")?.getLong("long")).isEqualTo(100)
        assertThat(data.getAppFunctionDataList("dataList")).hasSize(2)
        assertThat(data.getAppFunctionDataList("dataList")?.get(0)?.getDouble("double"))
            .isEqualTo(20.0)
        assertThat(data.getAppFunctionDataList("dataList")?.get(1)?.getString("string"))
            .isEqualTo("testString")
    }

    @Test
    fun testReadWrite_nestedAppFunctionData_conformSpec() {
        val innerObjectType =
            AppFunctionObjectTypeMetadata(
                properties =
                    mapOf(
                        "innerDouble" to
                            AppFunctionPrimitiveTypeMetadata(
                                type = TYPE_DOUBLE,
                                isNullable = false,
                            )
                    ),
                required = emptyList(),
                qualifiedName = "innerData",
                isNullable = false,
            )
        val outerObjectType =
            AppFunctionObjectTypeMetadata(
                properties = mapOf("nestedData" to innerObjectType),
                required = emptyList(),
                qualifiedName = "outerData",
                isNullable = false,
            )
        val innerDataBuilder =
            AppFunctionData.Builder(innerObjectType, AppFunctionComponentsMetadata())
        val outerDataBuilder =
            AppFunctionData.Builder(
                outerObjectType,
                AppFunctionComponentsMetadata(),
            )

        innerDataBuilder.setDouble("innerDouble", 500.0)
        outerDataBuilder.setAppFunctionData("nestedData", innerDataBuilder.build())
        val outerData = outerDataBuilder.build()

        assertThat(outerData.getAppFunctionData("nestedData")?.getDouble("innerDouble"))
            .isEqualTo(500.0)
    }

    @Test
    fun testWrite_nestedAppFunctionData_notConformSpec() {
        val innerObjectType =
            AppFunctionObjectTypeMetadata(
                properties =
                    mapOf(
                        "innerDouble" to
                            AppFunctionPrimitiveTypeMetadata(
                                type = TYPE_DOUBLE,
                                isNullable = false,
                            )
                    ),
                required = emptyList(),
                qualifiedName = "innerData",
                isNullable = false,
            )
        val incorrectInnerObjectType =
            AppFunctionObjectTypeMetadata(
                properties =
                    mapOf(
                        "innerDouble" to
                            AppFunctionPrimitiveTypeMetadata(
                                type = TYPE_LONG,
                                isNullable = false,
                            )
                    ),
                required = emptyList(),
                qualifiedName = "innerData",
                isNullable = false,
            )
        val outerObjectType =
            AppFunctionObjectTypeMetadata(
                properties = mapOf("nestedData" to innerObjectType),
                required = emptyList(),
                qualifiedName = "outerData",
                isNullable = false,
            )
        val incorrectInnerDataBuilder =
            AppFunctionData.Builder(incorrectInnerObjectType, AppFunctionComponentsMetadata())
        val outerDataBuilder =
            AppFunctionData.Builder(
                outerObjectType,
                AppFunctionComponentsMetadata(),
            )

        incorrectInnerDataBuilder.setLong("innerDouble", 500)
        assertThrows(IllegalArgumentException::class.java) {
            outerDataBuilder.setAppFunctionData("nestedData", incorrectInnerDataBuilder.build())
        }
    }

    @Test
    fun testWrite_nestedListAppFunctionData_notConformSpec() {
        val innerObjectType =
            AppFunctionObjectTypeMetadata(
                properties =
                    mapOf(
                        "innerDouble" to
                            AppFunctionPrimitiveTypeMetadata(
                                type = TYPE_DOUBLE,
                                isNullable = false,
                            )
                    ),
                required = emptyList(),
                qualifiedName = "innerData",
                isNullable = false,
            )
        val incorrectInnerObjectType =
            AppFunctionObjectTypeMetadata(
                properties =
                    mapOf(
                        "innerDouble" to
                            AppFunctionPrimitiveTypeMetadata(
                                type = TYPE_LONG,
                                isNullable = false,
                            )
                    ),
                required = emptyList(),
                qualifiedName = "innerData",
                isNullable = false,
            )
        val outerObjectType =
            AppFunctionObjectTypeMetadata(
                properties =
                    mapOf("nestedDataList" to AppFunctionArrayTypeMetadata(innerObjectType, false)),
                required = emptyList(),
                qualifiedName = "outerData",
                isNullable = false,
            )
        val correctInnerDataBuilder =
            AppFunctionData.Builder(innerObjectType, AppFunctionComponentsMetadata())
        val incorrectInnerDataBuilder =
            AppFunctionData.Builder(incorrectInnerObjectType, AppFunctionComponentsMetadata())
        val outerDataBuilder =
            AppFunctionData.Builder(
                outerObjectType,
                AppFunctionComponentsMetadata(),
            )

        correctInnerDataBuilder.setDouble("innerDouble", 500.0)
        incorrectInnerDataBuilder.setLong("innerDouble", 500)

        assertThrows(IllegalArgumentException::class.java) {
            outerDataBuilder.setAppFunctionDataList(
                "nestedDataList",
                listOf(correctInnerDataBuilder.build(), incorrectInnerDataBuilder.build())
            )
        }
    }

    @Test
    fun testSerialize() {
        val note = Note(title = "Test Title", attachment = Attachment(uri = "Test Uri"))

        val data = AppFunctionData.serialize(note, Note::class.java)

        assertThat(data.getString("title")).isEqualTo("Test Title")
        assertThat(data.getAppFunctionData("attachment")?.getString("uri")).isEqualTo("Test Uri")
    }

    @Test
    fun testSerialize_withQualifiedName() {
        val note = Note(title = "Test Title", attachment = Attachment(uri = "Test Uri"))

        val data = AppFunctionData.serialize(note, "androidx.appfunctions.Note")

        assertThat(data.getString("title")).isEqualTo("Test Title")
        assertThat(data.getAppFunctionData("attachment")?.getString("uri")).isEqualTo("Test Uri")
    }

    @Test
    fun testDeserialize() {
        val data =
            AppFunctionData.Builder("androidx.appfunctions.Note")
                .setString("title", "Test Title")
                .setAppFunctionData(
                    "attachment",
                    AppFunctionData.Builder("androidx.appfunctions.Attachment")
                        .setString("uri", "Test Uri")
                        .build()
                )
                .build()

        val note = data.deserialize(Note::class.java)

        assertThat(note.title).isEqualTo("Test Title")
        assertThat(note.attachment.uri).isEqualTo("Test Uri")
    }

    @Test
    fun testDeserialize_withQualifiedName() {
        val data =
            AppFunctionData.Builder("androidx.appfunctions.Note")
                .setString("title", "Test Title")
                .setAppFunctionData(
                    "attachment",
                    AppFunctionData.Builder("androidx.appfunctions.Attachment")
                        .setString("uri", "Test Uri")
                        .build()
                )
                .build()

        val note = data.deserialize<Note>("androidx.appfunctions.Note")

        assertThat(note.title).isEqualTo("Test Title")
        assertThat(note.attachment.uri).isEqualTo("Test Uri")
    }

    @Test
    fun testSerialize_missingFactory() {
        val missingFactoryClass = MissingFactoryClass("test")

        assertThrows(IllegalArgumentException::class.java) {
            AppFunctionData.serialize(missingFactoryClass, MissingFactoryClass::class.java)
        }
    }

    @Test
    fun testDeserialize_missingFactory() {
        val data =
            AppFunctionData.Builder("androidx.appfunctions-MissingFactoryClass")
                .setString("item", "test")
                .build()

        assertThrows(IllegalArgumentException::class.java) {
            data.deserialize(MissingFactoryClass::class.java)
        }
    }

    companion object {
        val TEST_OBJECT_METADATA =
            AppFunctionObjectTypeMetadata(
                properties =
                    mapOf(
                        "long" to AppFunctionPrimitiveTypeMetadata(TYPE_LONG, false),
                        "double" to AppFunctionPrimitiveTypeMetadata(TYPE_DOUBLE, false),
                        "boolean" to AppFunctionPrimitiveTypeMetadata(TYPE_BOOLEAN, false),
                        "string" to AppFunctionPrimitiveTypeMetadata(TYPE_STRING, false),
                        "longArray" to
                            AppFunctionArrayTypeMetadata(
                                itemType = AppFunctionPrimitiveTypeMetadata(TYPE_LONG, false),
                                isNullable = false,
                            ),
                        "doubleArray" to
                            AppFunctionArrayTypeMetadata(
                                itemType = AppFunctionPrimitiveTypeMetadata(TYPE_DOUBLE, false),
                                isNullable = false,
                            ),
                        "booleanArray" to
                            AppFunctionArrayTypeMetadata(
                                itemType = AppFunctionPrimitiveTypeMetadata(TYPE_BOOLEAN, false),
                                isNullable = false,
                            ),
                        "stringList" to
                            AppFunctionArrayTypeMetadata(
                                itemType = AppFunctionPrimitiveTypeMetadata(TYPE_STRING, false),
                                isNullable = false,
                            ),
                    ),
                required = emptyList(),
                qualifiedName = "test",
                isNullable = false,
            )

        val TEST_PARAMETER_METADATA =
            listOf(
                AppFunctionParameterMetadata(
                    name = "long",
                    isRequired = true,
                    dataType =
                        AppFunctionPrimitiveTypeMetadata(
                            type = TYPE_LONG,
                            isNullable = false,
                        ),
                ),
                AppFunctionParameterMetadata(
                    name = "double",
                    isRequired = true,
                    dataType =
                        AppFunctionPrimitiveTypeMetadata(
                            type = TYPE_DOUBLE,
                            isNullable = false,
                        ),
                ),
                AppFunctionParameterMetadata(
                    name = "boolean",
                    isRequired = true,
                    dataType =
                        AppFunctionPrimitiveTypeMetadata(
                            type = TYPE_BOOLEAN,
                            isNullable = false,
                        ),
                ),
                AppFunctionParameterMetadata(
                    name = "string",
                    isRequired = true,
                    dataType =
                        AppFunctionPrimitiveTypeMetadata(
                            type = TYPE_STRING,
                            isNullable = false,
                        ),
                ),
                AppFunctionParameterMetadata(
                    name = "longArray",
                    isRequired = true,
                    dataType =
                        AppFunctionArrayTypeMetadata(
                            itemType =
                                AppFunctionPrimitiveTypeMetadata(
                                    type = TYPE_LONG,
                                    isNullable = false,
                                ),
                            isNullable = false,
                        ),
                ),
                AppFunctionParameterMetadata(
                    name = "doubleArray",
                    isRequired = true,
                    dataType =
                        AppFunctionArrayTypeMetadata(
                            itemType =
                                AppFunctionPrimitiveTypeMetadata(
                                    type = TYPE_DOUBLE,
                                    isNullable = false,
                                ),
                            isNullable = false,
                        ),
                ),
                AppFunctionParameterMetadata(
                    name = "booleanArray",
                    isRequired = true,
                    dataType =
                        AppFunctionArrayTypeMetadata(
                            itemType =
                                AppFunctionPrimitiveTypeMetadata(
                                    type = TYPE_BOOLEAN,
                                    isNullable = false,
                                ),
                            isNullable = false,
                        ),
                ),
                AppFunctionParameterMetadata(
                    name = "stringList",
                    isRequired = true,
                    dataType =
                        AppFunctionArrayTypeMetadata(
                            itemType =
                                AppFunctionPrimitiveTypeMetadata(
                                    type = TYPE_STRING,
                                    isNullable = false,
                                ),
                            isNullable = false,
                        ),
                ),
            )
        val TEST_NESTED_PARAMETER_METADATA =
            listOf(
                AppFunctionParameterMetadata(
                    name = "data",
                    isRequired = true,
                    dataType = TEST_OBJECT_METADATA
                ),
                AppFunctionParameterMetadata(
                    name = "dataList",
                    isRequired = true,
                    dataType =
                        AppFunctionArrayTypeMetadata(
                            itemType = TEST_OBJECT_METADATA,
                            isNullable = false
                        )
                )
            )
    }
}
