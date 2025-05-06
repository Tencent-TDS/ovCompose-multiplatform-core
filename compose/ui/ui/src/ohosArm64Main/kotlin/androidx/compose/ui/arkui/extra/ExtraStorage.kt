/*
 * Tencent is pleased to support the open source community by making ovCompose available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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

package androidx.compose.ui.arkui.extra

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Interfaces that support additional data storage
 *
 * Classes implementing this interface can store/retrieve extra data through [extras] property.
 */
interface ExtraStorageOwner {

    /**
     * Gets the associated extra storage
     */
    val extras: ExtraStorage
}

/**
 * Type-safe key-value storage interface
 *
 * Type-safe storage operations via [Key] to prevent type casting errors.
 */
interface ExtraStorage {

    /**
     * Store [value] with [key]
     *
     * Example:
     * ```
     * private val IntegerKey by key<Int>()
     *
     * fun example() {
     *     extras[IntegerKey] = 123
     *     extras[IntegerKey] = "abc"  // error: type mismatch
     * }
     * ```
     */
    operator fun <T> set(key: Key<T>, value: T?)

    /**
     * Retrieve value with [key]
     *
     * Example:
     * ```
     * private val IntegerKey by key<Int>()
     *
     * fun example() {
     *     val result: Int? = extras[IntegerKey]
     *     val result: String? = extras[IntegerKey]  // error: type mismatch
     * }
     * ```
     */
    operator fun <T> get(key: Key<T>): T?
}

/**
 * Returns the value for the given [key] if the value is present and not `null`.
 * Otherwise, calls the [defaultValue] function, puts its result into the map under the given key and returns the call result.
 *
 * Note that the operation is not guaranteed to be atomic if the storage is being modified concurrently.
 */
fun <T> ExtraStorage.getOrPut(key: Key<T>, defaultValue: () -> T): T =
    get(key) ?: defaultValue().also { set(key, it) }

internal class DefaultExtraStorage : ExtraStorage {

    private val map: MutableMap<Key<*>, Any?> = HashMap(INITIAL_CAPACITY)

    override fun <T> set(key: Key<T>, value: T?) {
        if (value == null) map.remove(key) else map[key] = value
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(key: Key<T>): T? =
        map[key] as? T // Returns null on type mismatch caused by improper cast key usage

    override fun toString(): String = "DefaultExtraStorage(map=$map)"

    companion object {
        // Reference Android View KeyedTags Initial Capacity
        private const val INITIAL_CAPACITY = 2
    }
}

/**
 * Type-safe storage key interface
 */
interface Key<T>

/**
 * Create an instance of the key and automatically collect the type of value
 *
 * Example:
 * ```
 * val stringKey = Key<String>()
 * ```
 *
 * Notice:
 * 1. Type parameter [T] is only used for type checking during compilation period to ensure type safety of access values.
 * 2. Each created Key instance is unique, their equality is based on the instance itself and has no contact with the type parameter.
 *
 * Example:
 * ```
 * val stringKey1 = Key<String>()
 * val stringKey2 = Key<String>()
 *
 * extras[stringKey1] = "string1"
 * extras[stringKey2] = " string2"
 *
 * println(extras[stringKey1]) // string1
 * println(extras[stringKey2]) // string2
 * ```
 */
inline fun <reified T> Key(): Key<T> = TypedKey(T::class.simpleName.orEmpty())


/**
 * Create an instance of the key and automatically collect the name of key and type of value
 *
 * Example:
 * ```
 * val stringKey by key<String>()
 * ```
 *
 * Notice:
 * 1. Type parameter [T] is only used for type checking during compilation period to ensure type safety of access values.
 * 2. Each created Key instance is unique, their equality is based on the instance itself and has no contact with the type parameter.
 *
 * Example:
 * ```
 * val stringKey1 by key<String>()
 * val stringKey2 by key<String>()
 *
 * extras[stringKey1] = "string1"
 * extras[stringKey2] = " string2"
 *
 * println(extras[stringKey1]) // string1
 * println(extras[stringKey2]) // string2
 * ```
 */
inline fun <reified T> key(): ReadOnlyProperty<Any?, Key<T>> =
    object : ReadOnlyProperty<Any?, Key<T>> {
        private var key: Key<T>? = null
        override fun getValue(thisRef: Any?, property: KProperty<*>): Key<T> =
            key ?: NamedKey<T>(property.name, T::class.simpleName.orEmpty()).also { key = it }
    }

@PublishedApi
internal class TypedKey<T>(private val type: String) : Key<T> {
    @OptIn(ExperimentalStdlibApi::class)
    override fun toString(): String =
        "Key(hash=${hashCode().toHexString(HexFormat.UpperCase)}, type='$type')"
}

@PublishedApi
internal class NamedKey<T>(private val name: String, private val type: String) : Key<T> {
    @OptIn(ExperimentalStdlibApi::class)
    override fun toString(): String =
        "Key(hash=${hashCode().toHexString(HexFormat.UpperCase)}, name='$name', type='$type')"
}