package com.testdata

import androidx.appfunctions.AppFunctionSerializable

@AppFunctionSerializable open class StringBaseSerializable(open val name: String) {}

@AppFunctionSerializable
open class LongBaseSerializable(
    open val age: Long,
    override val name: String,
) : StringBaseSerializable(name) {}

@AppFunctionSerializable
class DerivedSerializable(
    override val name: String,
    override val age: Long,
) : LongBaseSerializable(age, name)
