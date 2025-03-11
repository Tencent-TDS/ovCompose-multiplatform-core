package com.testdata

import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import javax.`annotation`.processing.Generated

@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$NonNestedChildSerializableFactory` : AppFunctionSerializableFactory<NonNestedChildSerializable> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): NonNestedChildSerializable {
    val nestedBaseSerializableFactory = `$NestedBaseSerializableFactory`()

    val stringBaseName = checkNotNull(appFunctionData.getString("stringBaseName"))
    val nestedBaseData = appFunctionData.getAppFunctionData("nestedBase")
    var nestedBase: NestedBaseSerializable? = null
    if (nestedBaseData != null) {
      nestedBase = nestedBaseSerializableFactory.fromAppFunctionData(nestedBaseData) as NestedBaseSerializable?
    }
    val longBaseValue = checkNotNull(appFunctionData.getLongOrNull("longBaseValue"))

    return NonNestedChildSerializable(stringBaseName, nestedBase, longBaseValue)
  }

  override fun toAppFunctionData(appFunctionSerializable: NonNestedChildSerializable): AppFunctionData {
    val nestedBaseSerializableFactory = `$NestedBaseSerializableFactory`()

    val builder = AppFunctionData.Builder("com.testdata.NonNestedChildSerializable")
    builder.setString("stringBaseName", appFunctionSerializable.stringBaseName)
    if (appFunctionSerializable.nestedBase != null) {
      builder.setAppFunctionData("nestedBase", nestedBaseSerializableFactory.toAppFunctionData(appFunctionSerializable.nestedBase as NestedBaseSerializable))
    }
    builder.setLong("longBaseValue", appFunctionSerializable.longBaseValue)

    return builder.build()
  }
}
