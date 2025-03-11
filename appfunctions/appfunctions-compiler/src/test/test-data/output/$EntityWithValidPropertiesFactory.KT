package com.testdata

import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import javax.`annotation`.processing.Generated

@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$EntityWithValidPropertiesFactory` : AppFunctionSerializableFactory<EntityWithValidProperties> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): EntityWithValidProperties {
    val inputSerializableFactory = `$InputSerializableFactory`()

    val longParam = checkNotNull(appFunctionData.getLongOrNull("longParam"))
    val doubleParam = checkNotNull(appFunctionData.getDoubleOrNull("doubleParam"))
    val boolParam = checkNotNull(appFunctionData.getBooleanOrNull("boolParam"))
    val stringParam = checkNotNull(appFunctionData.getString("stringParam"))
    val longArray = checkNotNull(appFunctionData.getLongArray("longArray") ?: LongArray(0))
    val doubleArray = checkNotNull(appFunctionData.getDoubleArray("doubleArray") ?: DoubleArray(0))
    val boolArray = checkNotNull(appFunctionData.getBooleanArray("boolArray") ?: BooleanArray(0))
    val stringList = checkNotNull(appFunctionData.getStringList("stringList") ?: emptyList())
    val inputSerializableData = checkNotNull(appFunctionData.getAppFunctionData("inputSerializable"))
    val inputSerializable = inputSerializableFactory.fromAppFunctionData(inputSerializableData)
    val serializableListData = appFunctionData.getAppFunctionDataList("serializableList") ?: emptyList()
    val serializableList = serializableListData.map { data ->
      inputSerializableFactory.fromAppFunctionData(data)
    }

    return EntityWithValidProperties(longParam, doubleParam, boolParam, stringParam, longArray, doubleArray, boolArray, stringList, inputSerializable, serializableList)
  }

  override fun toAppFunctionData(appFunctionSerializable: EntityWithValidProperties): AppFunctionData {
    val inputSerializableFactory = `$InputSerializableFactory`()

    val builder = AppFunctionData.Builder("com.testdata.EntityWithValidProperties")
    builder.setLong("longParam", appFunctionSerializable.longParam)
    builder.setDouble("doubleParam", appFunctionSerializable.doubleParam)
    builder.setBoolean("boolParam", appFunctionSerializable.boolParam)
    builder.setString("stringParam", appFunctionSerializable.stringParam)
    builder.setLongArray("longArray", appFunctionSerializable.longArray)
    builder.setDoubleArray("doubleArray", appFunctionSerializable.doubleArray)
    builder.setBooleanArray("boolArray", appFunctionSerializable.boolArray)
    builder.setStringList("stringList", appFunctionSerializable.stringList)
    builder.setAppFunctionData("inputSerializable", inputSerializableFactory.toAppFunctionData(appFunctionSerializable.inputSerializable as InputSerializable))
    builder.setAppFunctionDataList("serializableList", appFunctionSerializable.serializableList.map{ inputSerializable ->
      inputSerializableFactory.toAppFunctionData(inputSerializable)
    })

    return builder.build()
  }
}
