package com.testdata

import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import javax.`annotation`.processing.Generated

@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$DerivedSerializableFactory` : AppFunctionSerializableFactory<DerivedSerializable> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): DerivedSerializable {

    val name = appFunctionData.getString("name")
    val age = appFunctionData.getLong("age")

    return DerivedSerializable(name, age)
  }

  override fun toAppFunctionData(derivedSerializable: DerivedSerializable): AppFunctionData {

    val builder = AppFunctionData.Builder("com.testdata.DerivedSerializable")
    builder.setString("name", derivedSerializable.name)
    builder.setLong("age", derivedSerializable.age)

    return builder.build()
  }
}
