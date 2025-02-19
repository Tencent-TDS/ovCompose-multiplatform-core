package com.testdata

import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import javax.`annotation`.processing.Generated

@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$EntityWithValidNullablePropertiesFactory` :
    AppFunctionSerializableFactory<EntityWithValidNullableProperties> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData):
      EntityWithValidNullableProperties {
    val inputSerializableFactory = `$InputSerializableFactory`()

    val longParam = appFunctionData.getLongOrNull("longParam")
    val doubleParam = appFunctionData.getDoubleOrNull("doubleParam")
    val boolParam = appFunctionData.getBooleanOrNull("boolParam")
    val stringParam = appFunctionData.getStringOrNull("stringParam")
    val longArray = appFunctionData.getLongArray("longArray")
    val doubleArray = appFunctionData.getDoubleArray("doubleArray")
    val boolArray = appFunctionData.getBooleanArray("boolArray")
    val stringList = appFunctionData.getStringList("stringList")
    val inputSerializableData = appFunctionData.getAppFunctionDataOrNull("inputSerializable")
    var inputSerializable: InputSerializable? = null
    if (inputSerializableData != null) {
      inputSerializable = inputSerializableFactory.fromAppFunctionData(inputSerializableData)
    }
    val serializableListData = appFunctionData.getAppFunctionDataList("serializableList")
    val serializableList = serializableListData?.map { data ->
      inputSerializableFactory.fromAppFunctionData(data)
    }

    return EntityWithValidNullableProperties(longParam, doubleParam, boolParam, stringParam,
        longArray, doubleArray, boolArray, stringList, inputSerializable, serializableList)
  }

  override
      fun toAppFunctionData(entityWithValidNullableProperties: EntityWithValidNullableProperties):
      AppFunctionData {
    val inputSerializableFactory = `$InputSerializableFactory`()

    val builder = AppFunctionData.Builder("com.testdata.EntityWithValidNullableProperties")
    if (entityWithValidNullableProperties.longParam != null) {
      builder.setLong("longParam", entityWithValidNullableProperties.longParam)
    }
    if (entityWithValidNullableProperties.doubleParam != null) {
      builder.setDouble("doubleParam", entityWithValidNullableProperties.doubleParam)
    }
    if (entityWithValidNullableProperties.boolParam != null) {
      builder.setBoolean("boolParam", entityWithValidNullableProperties.boolParam)
    }
    if (entityWithValidNullableProperties.stringParam != null) {
      builder.setString("stringParam", entityWithValidNullableProperties.stringParam)
    }
    if (entityWithValidNullableProperties.longArray != null) {
      builder.setLongArray("longArray", entityWithValidNullableProperties.longArray)
    }
    if (entityWithValidNullableProperties.doubleArray != null) {
      builder.setDoubleArray("doubleArray", entityWithValidNullableProperties.doubleArray)
    }
    if (entityWithValidNullableProperties.boolArray != null) {
      builder.setBooleanArray("boolArray", entityWithValidNullableProperties.boolArray)
    }
    if (entityWithValidNullableProperties.stringList != null) {
      builder.setStringList("stringList", entityWithValidNullableProperties.stringList)
    }
    if (entityWithValidNullableProperties.inputSerializable != null) {
      builder.setAppFunctionData("inputSerializable",
          inputSerializableFactory.toAppFunctionData(entityWithValidNullableProperties.inputSerializable))
    }
    if (entityWithValidNullableProperties.serializableList != null) {
      builder.setAppFunctionDataList("serializableList",
          entityWithValidNullableProperties.serializableList.map{ inputSerializable ->
        inputSerializableFactory.toAppFunctionData(inputSerializable)
      })
    }

    return builder.build()
  }
}
