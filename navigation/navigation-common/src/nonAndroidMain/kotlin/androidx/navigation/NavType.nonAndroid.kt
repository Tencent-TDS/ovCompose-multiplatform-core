/*
 * Copyright 2024 The Android Open Source Project
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
import kotlin.jvm.JvmStatic

public actual abstract class NavType<T>
actual constructor(public actual open val isNullableAllowed: Boolean) {
    public actual abstract fun put(bundle: SavedState, key: String, value: T)

    public actual abstract operator fun get(bundle: SavedState, key: String): T?

    public actual abstract fun parseValue(value: String): T

    public actual open fun parseValue(value: String, previousValue: T): T {
        implementedInJetBrainsFork()
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual fun parseAndPut(bundle: SavedState, key: String, value: String): T {
        implementedInJetBrainsFork()
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual fun parseAndPut(
        bundle: SavedState,
        key: String,
        value: String?,
        previousValue: T
    ): T {
        implementedInJetBrainsFork()
    }

    public actual open fun serializeAsValue(value: T): String {
        implementedInJetBrainsFork()
    }

    public actual open val name: String
        get() = implementedInJetBrainsFork()

    public actual open fun valueEquals(value: T, other: T): Boolean {
        implementedInJetBrainsFork()
    }

    public actual companion object {
        @JvmStatic
        @Suppress("NON_FINAL_MEMBER_IN_OBJECT")
        public actual open fun fromArgType(type: String?, packageName: String?): NavType<*> {
            implementedInJetBrainsFork()
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @JvmStatic
        public actual fun inferFromValue(value: String): NavType<Any> {
            implementedInJetBrainsFork()
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @JvmStatic
        public actual fun inferFromValueType(value: Any?): NavType<Any> {
            implementedInJetBrainsFork()
        }

        public actual val IntType: NavType<Int>
            get() = implementedInJetBrainsFork()

        public actual val IntArrayType: NavType<IntArray?>
            get() = implementedInJetBrainsFork()

        public actual val IntListType: NavType<List<Int>?>
            get() = implementedInJetBrainsFork()

        public actual val LongType: NavType<Long>
            get() = implementedInJetBrainsFork()

        public actual val LongArrayType: NavType<LongArray?>
            get() = implementedInJetBrainsFork()

        public actual val LongListType: NavType<List<Long>?>
            get() = implementedInJetBrainsFork()

        public actual val FloatType: NavType<Float>
            get() = implementedInJetBrainsFork()

        public actual val FloatArrayType: NavType<FloatArray?>
            get() = implementedInJetBrainsFork()

        public actual val FloatListType: NavType<List<Float>?>
            get() = implementedInJetBrainsFork()

        public actual val BoolType: NavType<Boolean>
            get() = implementedInJetBrainsFork()

        public actual val BoolArrayType: NavType<BooleanArray?>
            get() = implementedInJetBrainsFork()

        public actual val BoolListType: NavType<List<Boolean>?>
            get() = implementedInJetBrainsFork()

        public actual val StringType: NavType<String?>
            get() = implementedInJetBrainsFork()

        public actual val StringArrayType: NavType<Array<String>?>
            get() = implementedInJetBrainsFork()

        public actual val StringListType: NavType<List<String>?>
            get() = implementedInJetBrainsFork()
    }
}
