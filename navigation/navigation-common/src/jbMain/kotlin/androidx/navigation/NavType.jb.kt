/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.navigation

import androidx.annotation.RestrictTo
import androidx.savedstate.SavedState
import androidx.savedstate.read
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

public actual abstract class NavType<T>
actual constructor(public actual open val isNullableAllowed: Boolean) {

    public actual abstract fun put(bundle: SavedState, key: String, value: T)

    public actual abstract operator fun get(bundle: SavedState, key: String): T?

    public actual abstract fun parseValue(value: String): T

    public actual open fun parseValue(value: String, previousValue: T): T = parseValue(value)

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual fun parseAndPut(bundle: SavedState, key: String, value: String): T {
        val parsedValue = parseValue(value)
        put(bundle, key, parsedValue)
        return parsedValue
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual fun parseAndPut(
        bundle: SavedState,
        key: String,
        value: String?,
        previousValue: T
    ): T {
        if (!bundle.read { contains(key) }) {
            throw IllegalArgumentException("There is no previous value in this savedState.")
        }
        if (value != null) {
            val parsedCombinedValue = parseValue(value, previousValue)
            put(bundle, key, parsedCombinedValue)
            return parsedCombinedValue
        }
        return previousValue
    }

    public actual open fun serializeAsValue(value: T): String {
        return value.toString()
    }

    public actual open val name: String = "nav_type"

    public actual open fun valueEquals(value: T, other: T): Boolean = value == other

    override fun toString(): String {
        return name
    }

    public actual companion object {
        @Suppress("NON_FINAL_MEMBER_IN_OBJECT", "UNCHECKED_CAST") // this needs to be open to
        // maintain api compatibility and type cast are unchecked
        @JvmStatic
        public actual open fun fromArgType(type: String?, packageName: String?): NavType<*> {
            return when (type) {
                IntType.name -> IntType
                IntArrayType.name -> IntArrayType
                IntListType.name -> IntListType
                LongType.name -> LongType
                LongArrayType.name -> LongArrayType
                LongListType.name -> LongListType
                BoolType.name -> BoolType
                BoolArrayType.name -> BoolArrayType
                BoolListType.name -> BoolListType
                StringType.name -> StringType
                StringArrayType.name -> StringArrayType
                StringListType.name -> StringListType
                FloatType.name -> FloatType
                FloatArrayType.name -> FloatArrayType
                FloatListType.name -> FloatListType
                else -> {
                    if (!type.isNullOrEmpty()) {
                        throw IllegalArgumentException(
                            "Object of type $type is not supported for navigation arguments."
                        )
                    }
                    StringType
                }
            }
        }

        @Suppress("UNCHECKED_CAST") // needed for cast to NavType<Any>
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public actual fun inferFromValue(value: String): NavType<Any> {
            // because we allow Long literals without the L suffix at runtime,
            // the order of IntType and LongType parsing has to be reversed compared to Safe Args
            try {
                IntType.parseValue(value)
                return IntType as NavType<Any>
            } catch (e: IllegalArgumentException) {
                // ignored, proceed to check next type
            }
            try {
                LongType.parseValue(value)
                return LongType as NavType<Any>
            } catch (e: IllegalArgumentException) {
                // ignored, proceed to check next type
            }
            try {
                FloatType.parseValue(value)
                return FloatType as NavType<Any>
            } catch (e: IllegalArgumentException) {
                // ignored, proceed to check next type
            }
            try {
                BoolType.parseValue(value)
                return BoolType as NavType<Any>
            } catch (e: IllegalArgumentException) {
                // ignored, proceed to check next type
            }
            return StringType as NavType<Any>
        }

        @Suppress("UNCHECKED_CAST") // needed for cast to NavType<Any>
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public actual fun inferFromValueType(value: Any?): NavType<Any> {
            return when (value) {
                is Int -> IntType as NavType<Any>
                is IntArray -> IntArrayType as NavType<Any>
                is Long -> LongType as NavType<Any>
                is LongArray -> LongArrayType as NavType<Any>
                is Float -> FloatType as NavType<Any>
                is FloatArray -> FloatArrayType as NavType<Any>
                is Boolean -> BoolType as NavType<Any>
                is BooleanArray -> BoolArrayType as NavType<Any>
                is String, null -> StringType as NavType<Any>
                is Array<*> -> StringArrayType as NavType<Any>
                else -> throw IllegalArgumentException(
                    "$value is not supported for navigation arguments."
                )
            }
        }

        @JvmField public actual val IntType: NavType<Int> = IntNavType()
        @JvmField public actual val IntArrayType: NavType<IntArray?> = IntArrayNavType()
        @JvmField public actual val IntListType: NavType<List<Int>?> = IntListNavType()
        @JvmField public actual val LongType: NavType<Long> = LongNavType()
        @JvmField public actual val LongArrayType: NavType<LongArray?> = LongArrayNavType()
        @JvmField public actual val LongListType: NavType<List<Long>?> = LongListNavType()
        @JvmField public actual val FloatType: NavType<Float> = FloatNavType()
        @JvmField public actual val FloatArrayType: NavType<FloatArray?> = FloatArrayNavType()
        @JvmField public actual val FloatListType: NavType<List<Float>?> = FloatListNavType()
        @JvmField public actual val BoolType: NavType<Boolean> = BoolNavType()
        @JvmField public actual val BoolArrayType: NavType<BooleanArray?> = BoolArrayNavType()
        @JvmField public actual val BoolListType: NavType<List<Boolean>?> = BoolListNavType()
        @JvmField public actual val StringType: NavType<String?> = StringNavType()
        @JvmField public actual val StringArrayType: NavType<Array<String>?> = StringArrayNavType()
        @JvmField public actual val StringListType: NavType<List<String>?> = StringListNavType()
    }
}
