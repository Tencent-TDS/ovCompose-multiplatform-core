package com.testdata

import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import javax.`annotation`.processing.Generated

@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$StringBaseSerializableFactory` : AppFunctionSerializableFactory<StringBaseSerializable> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): StringBaseSerializable {

    val name = checkNotNull(appFunctionData.getString("name"))

    return StringBaseSerializable(name)
  }

  override fun toAppFunctionData(appFunctionSerializable: StringBaseSerializable): AppFunctionData {

    val builder = AppFunctionData.Builder("com.testdata.StringBaseSerializable")
    builder.setString("name", appFunctionSerializable.name)

    return builder.build()
  }
}
