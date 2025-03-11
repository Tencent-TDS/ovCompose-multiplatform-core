package com.testdata

import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import javax.`annotation`.processing.Generated

@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$NestedDerivedSerializableFactory` : AppFunctionSerializableFactory<NestedDerivedSerializable> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): NestedDerivedSerializable {
    val nestedBaseSerializableFactory = `$NestedBaseSerializableFactory`()
    val nestedDerivedSerializableFactory = `$NestedDerivedSerializableFactory`()

    val stringBaseName = checkNotNull(appFunctionData.getString("stringBaseName"))
    val nestedBaseData = appFunctionData.getAppFunctionData("nestedBase")
    var nestedBase: NestedBaseSerializable? = null
    if (nestedBaseData != null) {
      nestedBase = nestedBaseSerializableFactory.fromAppFunctionData(nestedBaseData) as NestedBaseSerializable?
    }
    val longBaseValue = checkNotNull(appFunctionData.getLongOrNull("longBaseValue"))
    val nestedDerivedData = appFunctionData.getAppFunctionData("nestedDerived")
    var nestedDerived: NestedDerivedSerializable? = null
    if (nestedDerivedData != null) {
      nestedDerived = nestedDerivedSerializableFactory.fromAppFunctionData(nestedDerivedData) as NestedDerivedSerializable?
    }

    return NestedDerivedSerializable(stringBaseName, nestedBase, longBaseValue, nestedDerived)
  }

  override fun toAppFunctionData(appFunctionSerializable: NestedDerivedSerializable): AppFunctionData {
    val nestedBaseSerializableFactory = `$NestedBaseSerializableFactory`()
    val nestedDerivedSerializableFactory = `$NestedDerivedSerializableFactory`()

    val builder = AppFunctionData.Builder("com.testdata.NestedDerivedSerializable")
    builder.setString("stringBaseName", appFunctionSerializable.stringBaseName)
    if (appFunctionSerializable.nestedBase != null) {
      builder.setAppFunctionData("nestedBase", nestedBaseSerializableFactory.toAppFunctionData(appFunctionSerializable.nestedBase as NestedBaseSerializable))
    }
    builder.setLong("longBaseValue", appFunctionSerializable.longBaseValue)
    if (appFunctionSerializable.nestedDerived != null) {
      builder.setAppFunctionData("nestedDerived", nestedDerivedSerializableFactory.toAppFunctionData(appFunctionSerializable.nestedDerived as NestedDerivedSerializable))
    }

    return builder.build()
  }
}
