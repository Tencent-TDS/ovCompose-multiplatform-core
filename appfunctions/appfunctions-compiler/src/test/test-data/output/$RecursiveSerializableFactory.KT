package com.testdata

import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import javax.`annotation`.processing.Generated

@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$RecursiveSerializableFactory` : AppFunctionSerializableFactory<RecursiveSerializable> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): RecursiveSerializable {
    val recursiveSerializableFactory = `$RecursiveSerializableFactory`()

    val nestedData = appFunctionData.getAppFunctionData("nested")
    var nested: RecursiveSerializable? = null
    if (nestedData != null) {
      nested = recursiveSerializableFactory.fromAppFunctionData(nestedData) as RecursiveSerializable?
    }

    return RecursiveSerializable(nested)
  }

  override fun toAppFunctionData(appFunctionSerializable: RecursiveSerializable): AppFunctionData {
    val recursiveSerializableFactory = `$RecursiveSerializableFactory`()

    val builder = AppFunctionData.Builder("com.testdata.RecursiveSerializable")
    if (appFunctionSerializable.nested != null) {
      builder.setAppFunctionData("nested", recursiveSerializableFactory.toAppFunctionData(appFunctionSerializable.nested as RecursiveSerializable))
    }

    return builder.build()
  }
}
