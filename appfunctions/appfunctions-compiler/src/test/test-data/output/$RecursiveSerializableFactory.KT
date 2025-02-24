package com.testdata

import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import javax.`annotation`.processing.Generated

@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$RecursiveSerializableFactory` : AppFunctionSerializableFactory<RecursiveSerializable>
    {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): RecursiveSerializable {
    val recursiveSerializableFactory = `$RecursiveSerializableFactory`()

    val nestedData = appFunctionData.getAppFunctionDataOrNull("nested")
    var nested: RecursiveSerializable? = null
    if (nestedData != null) {
      nested = recursiveSerializableFactory.fromAppFunctionData(nestedData)
    }

    return RecursiveSerializable(nested)
  }

  override fun toAppFunctionData(recursiveSerializable: RecursiveSerializable): AppFunctionData {
    val recursiveSerializableFactory = `$RecursiveSerializableFactory`()

    val builder = AppFunctionData.Builder("com.testdata.RecursiveSerializable")
    if (recursiveSerializable.nested != null) {
      builder.setAppFunctionData("nested",
          recursiveSerializableFactory.toAppFunctionData(recursiveSerializable.nested))
    }

    return builder.build()
  }
}
