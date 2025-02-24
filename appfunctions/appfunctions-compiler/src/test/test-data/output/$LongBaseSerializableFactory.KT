package com.testdata

import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import javax.`annotation`.processing.Generated

@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$LongBaseSerializableFactory` : AppFunctionSerializableFactory<LongBaseSerializable> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): LongBaseSerializable {

    val age = appFunctionData.getLong("age")
    val name = appFunctionData.getString("name")

    return LongBaseSerializable(age, name)
  }

  override fun toAppFunctionData(longBaseSerializable: LongBaseSerializable): AppFunctionData {

    val builder = AppFunctionData.Builder("com.testdata.LongBaseSerializable")
    builder.setLong("age", longBaseSerializable.age)
    builder.setString("name", longBaseSerializable.name)

    return builder.build()
  }
}
