package com.testdata

import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import javax.`annotation`.processing.Generated

@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$EntityWithValidNullablePropertiesFactory` : AppFunctionSerializableFactory<EntityWithValidNullableProperties> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): EntityWithValidNullableProperties {
    val inputSerializableFactory = `$InputSerializableFactory`()

    val longParam = appFunctionData.getLongOrNull("longParam")
    val doubleParam = appFunctionData.getDoubleOrNull("doubleParam")
    val boolParam = appFunctionData.getBooleanOrNull("boolParam")
    val stringParam = appFunctionData.getString("stringParam")
    val longArray = appFunctionData.getLongArray("longArray")
    val doubleArray = appFunctionData.getDoubleArray("doubleArray")
    val boolArray = appFunctionData.getBooleanArray("boolArray")
    val stringList = appFunctionData.getStringList("stringList")
    val inputSerializableData = appFunctionData.getAppFunctionData("inputSerializable")
    var inputSerializable: InputSerializable? = null
    if (inputSerializableData != null) {
      inputSerializable = inputSerializableFactory.fromAppFunctionData(inputSerializableData) as InputSerializable?
    }
    val serializableListData = appFunctionData.getAppFunctionDataList("serializableList")
    val serializableList = serializableListData?.map { data ->
      inputSerializableFactory.fromAppFunctionData(data)
    }

    return EntityWithValidNullableProperties(longParam, doubleParam, boolParam, stringParam, longArray, doubleArray, boolArray, stringList, inputSerializable, serializableList)
  }

  override fun toAppFunctionData(appFunctionSerializable: EntityWithValidNullableProperties): AppFunctionData {
    val inputSerializableFactory = `$InputSerializableFactory`()

    val builder = AppFunctionData.Builder("com.testdata.EntityWithValidNullableProperties")
    if (appFunctionSerializable.longParam != null) {
      builder.setLong("longParam", appFunctionSerializable.longParam)
    }
    if (appFunctionSerializable.doubleParam != null) {
      builder.setDouble("doubleParam", appFunctionSerializable.doubleParam)
    }
    if (appFunctionSerializable.boolParam != null) {
      builder.setBoolean("boolParam", appFunctionSerializable.boolParam)
    }
    if (appFunctionSerializable.stringParam != null) {
      builder.setString("stringParam", appFunctionSerializable.stringParam)
    }
    if (appFunctionSerializable.longArray != null) {
      builder.setLongArray("longArray", appFunctionSerializable.longArray)
    }
    if (appFunctionSerializable.doubleArray != null) {
      builder.setDoubleArray("doubleArray", appFunctionSerializable.doubleArray)
    }
    if (appFunctionSerializable.boolArray != null) {
      builder.setBooleanArray("boolArray", appFunctionSerializable.boolArray)
    }
    if (appFunctionSerializable.stringList != null) {
      builder.setStringList("stringList", appFunctionSerializable.stringList)
    }
    if (appFunctionSerializable.inputSerializable != null) {
      builder.setAppFunctionData("inputSerializable", inputSerializableFactory.toAppFunctionData(appFunctionSerializable.inputSerializable as InputSerializable))
    }
    if (appFunctionSerializable.serializableList != null) {
      builder.setAppFunctionDataList("serializableList", appFunctionSerializable.serializableList.map{ inputSerializable ->
        inputSerializableFactory.toAppFunctionData(inputSerializable)
      })
    }

    return builder.build()
  }
}
