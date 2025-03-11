package com.testdata

import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import javax.`annotation`.processing.Generated

@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$DerivedSerializableFactory` : AppFunctionSerializableFactory<DerivedSerializable> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): DerivedSerializable {

    val name = checkNotNull(appFunctionData.getString("name"))
    val age = checkNotNull(appFunctionData.getLongOrNull("age"))

    return DerivedSerializable(name, age)
  }

  override fun toAppFunctionData(appFunctionSerializable: DerivedSerializable): AppFunctionData {

    val builder = AppFunctionData.Builder("com.testdata.DerivedSerializable")
    builder.setString("name", appFunctionSerializable.name)
    builder.setLong("age", appFunctionSerializable.age)

    return builder.build()
  }
}
