package com.testdata

import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionSchemaDefinition

private const val FAKE_CATEGORY = "fake_schema_category"

@AppFunctionSchemaDefinition(name = "noArg", version = 1, category = FAKE_CATEGORY)
interface FakeNoArg {
    fun noArg(appFunctionContext: AppFunctionContext)
}
