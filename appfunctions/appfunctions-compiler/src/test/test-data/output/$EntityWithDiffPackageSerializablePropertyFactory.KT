package com.testdata

import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import com.testdata.differentPackage.DiffPackageSerializable
import com.testdata.differentPackage.`$DiffPackageSerializableFactory`
import javax.`annotation`.processing.Generated

@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$EntityWithDiffPackageSerializablePropertyFactory` : AppFunctionSerializableFactory<EntityWithDiffPackageSerializableProperty> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): EntityWithDiffPackageSerializableProperty {
    val diffPackageSerializableFactory = `$DiffPackageSerializableFactory`()

    val inputSerializableData = checkNotNull(appFunctionData.getAppFunctionData("inputSerializable"))
    val inputSerializable = diffPackageSerializableFactory.fromAppFunctionData(inputSerializableData)
    val serializableListData = appFunctionData.getAppFunctionDataList("serializableList") ?: emptyList()
    val serializableList = serializableListData.map { data ->
      diffPackageSerializableFactory.fromAppFunctionData(data)
    }

    return EntityWithDiffPackageSerializableProperty(inputSerializable, serializableList)
  }

  override fun toAppFunctionData(appFunctionSerializable: EntityWithDiffPackageSerializableProperty): AppFunctionData {
    val diffPackageSerializableFactory = `$DiffPackageSerializableFactory`()

    val builder = AppFunctionData.Builder("com.testdata.EntityWithDiffPackageSerializableProperty")
    builder.setAppFunctionData("inputSerializable", diffPackageSerializableFactory.toAppFunctionData(appFunctionSerializable.inputSerializable as DiffPackageSerializable))
    builder.setAppFunctionDataList("serializableList", appFunctionSerializable.serializableList.map{ diffPackageSerializable ->
      diffPackageSerializableFactory.toAppFunctionData(diffPackageSerializable)
    })

    return builder.build()
  }
}
