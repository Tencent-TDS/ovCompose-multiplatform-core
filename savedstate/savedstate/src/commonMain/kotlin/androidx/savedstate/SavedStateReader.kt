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

@file:JvmName("SavedStateReaderKt")
@file:JvmMultifileClass

package androidx.savedstate

import kotlin.jvm.JvmInline
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

/**
 * An inline class that encapsulates an opaque [SavedState], and provides an API for reading the
 * platform specific state.
 *
 * @see SavedState.read
 */
@JvmInline
public expect value class SavedStateReader
@PublishedApi
internal constructor(
    private val source: SavedState,
) {

    /**
     * Retrieves a [Boolean] value associated with the specified [key].
     *
     * @param key The [key] to retrieve the value for.
     * @return The value associated with the [key].
     * @throws IllegalArgumentException If the [key] is not found.
     * @throws IllegalArgumentException if associated value has wrong type.
     */
    public fun getBoolean(key: String): Boolean

    /** Returns the value corresponding to the given [key], or null if such a key is not present. */
    public fun getBooleanOrNull(key: String): Boolean?

    /**
     * Retrieves a [Char] value associated with the specified [key].
     *
     * @param key The [key] to retrieve the value for.
     * @return The value associated with the [key].
     * @throws IllegalArgumentException If the [key] is not found.
     * @throws IllegalArgumentException if associated value has wrong type.
     */
    public fun getChar(key: String): Char

    /** Returns the value corresponding to the given [key], or null if such a key is not present. */
    public fun getCharOrNull(key: String): Char?

    /**
     * Retrieves a [CharSequence] value associated with the specified [key].
     *
     * @param key The [key] to retrieve the value for.
     * @return The value associated with the [key].
     * @throws IllegalArgumentException If the [key] is not found.
     * @throws IllegalArgumentException if associated value has wrong type.
     */
    public fun getCharSequence(key: String): CharSequence

    /** Returns the value corresponding to the given [key], or null if such a key is not present. */
    public fun getCharSequenceOrNull(key: String): CharSequence?

    /**
     * Retrieves a [Double] value associated with the specified [key].
     *
     * @param key The [key] to retrieve the value for.
     * @return The value associated with the [key].
     * @throws IllegalArgumentException If the [key] is not found.
     * @throws IllegalArgumentException if associated value has wrong type.
     */
    public fun getDouble(key: String): Double

    /** Returns the value corresponding to the given [key], or null if such a key is not present. */
    public fun getDoubleOrNull(key: String): Double?

    /**
     * Retrieves a [Float] value associated with the specified [key].
     *
     * @param key The [key] to retrieve the value for.
     * @return The value associated with the [key].
     * @throws IllegalArgumentException If the [key] is not found.
     * @throws IllegalArgumentException if associated value has wrong type.
     */
    public fun getFloat(key: String): Float

    /** Returns the value corresponding to the given [key], or null if such a key is not present. */
    public fun getFloatOrNull(key: String): Float?

    /**
     * Retrieves an [Int] value associated with the specified [key].
     *
     * @param key The [key] to retrieve the value for.
     * @return The value associated with the [key].
     * @throws IllegalArgumentException If the [key] is not found.
     * @throws IllegalArgumentException if associated value has wrong type.
     */
    public fun getInt(key: String): Int

    /** Returns the value corresponding to the given [key], or null if such a key is not present. */
    public fun getIntOrNull(key: String): Int?

    /**
     * Retrieves an [Long] value associated with the specified [key].
     *
     * @param key The [key] to retrieve the value for.
     * @return The value associated with the [key].
     * @throws IllegalArgumentException If the [key] is not found.
     * @throws IllegalArgumentException if associated value has wrong type.
     */
    public fun getLong(key: String): Long

    /** Returns the value corresponding to the given [key], or null if such a key is not present. */
    public fun getLongOrNull(key: String): Long?

    /**
     * Retrieves a [String] value associated with the specified [key].
     *
     * @param key The [key] to retrieve the value for.
     * @return The value associated with the [key].
     * @throws IllegalArgumentException If the [key] is not found.
     * @throws IllegalArgumentException if associated value has wrong type.
     */
    public fun getString(key: String): String

    /** Returns the value corresponding to the given [key], or null if such a key is not present. */
    public fun getStringOrNull(key: String): String?

    /**
     * Retrieves a [List] of elements of [Int] associated with the specified [key].
     *
     * @param key The [key] to retrieve the value for.
     * @return The value associated with the [key].
     * @throws IllegalArgumentException If the [key] is not found.
     * @throws IllegalArgumentException if associated value has wrong type.
     */
    public fun getIntList(key: String): List<Int>

    /** Returns the value corresponding to the given [key], or null if such a key is not present. */
    public fun getIntListOrNull(key: String): List<Int>?

    /**
     * Retrieves a [List] of elements of [SavedState] associated with the specified [key].
     *
     * @param key The [key] to retrieve the value for.
     * @return The value associated with the [key].
     * @throws IllegalArgumentException If the [key] is not found.
     * @throws IllegalArgumentException if associated value has wrong type.
     */
    public fun getSavedStateList(key: String): List<SavedState>

    /** Returns the value corresponding to the given [key], or null if such a key is not present. */
    public fun getSavedStateListOrNull(key: String): List<SavedState>?

    /**
     * Retrieves a [List] of elements of [String] associated with the specified [key].
     *
     * @param key The [key] to retrieve the value for.
     * @return The value associated with the [key].
     * @throws IllegalArgumentException If the [key] is not found.
     * @throws IllegalArgumentException if associated value has wrong type.
     */
    public fun getStringList(key: String): List<String>

    /** Returns the value corresponding to the given [key], or null if such a key is not present. */
    public fun getStringListOrNull(key: String): List<String>?

    /**
     * Retrieves a [List] of elements of [CharArray] associated with the specified [key].
     *
     * @param key The [key] to retrieve the value for.
     * @return The value associated with the [key].
     * @throws IllegalArgumentException If the [key] is not found.
     * @throws IllegalArgumentException if associated value has wrong type.
     */
    public fun getCharSequenceList(key: String): List<CharSequence>

    /** Returns the value corresponding to the given [key], or null if such a key is not present. */
    public fun getCharSequenceListOrNull(key: String): List<CharSequence>?

    /**
     * Retrieves a [BooleanArray] value associated with the specified [key].
     *
     * @param key The [key] to retrieve the value for.
     * @return The value associated with the [key].
     * @throws IllegalArgumentException If the [key] is not found.
     * @throws IllegalArgumentException if associated value has wrong type.
     */
    public fun getBooleanArray(key: String): BooleanArray

    /** Returns the value corresponding to the given [key], or null if such a key is not present. */
    public fun getBooleanArrayOrNull(key: String): BooleanArray?

    /**
     * Retrieves a [CharArray] value associated with the specified [key].
     *
     * @param key The [key] to retrieve the value for.
     * @return The value associated with the [key].
     * @throws IllegalArgumentException If the [key] is not found.
     * @throws IllegalArgumentException if associated value has wrong type.
     */
    public fun getCharArray(key: String): CharArray

    /** Returns the value corresponding to the given [key], or null if such a key is not present. */
    public fun getCharArrayOrNull(key: String): CharArray?

    /**
     * Retrieves a [CharArray] value associated with the specified [key].
     *
     * @param key The [key] to retrieve the value for.
     * @return The value associated with the [key].
     * @throws IllegalArgumentException If the [key] is not found.
     * @throws IllegalArgumentException if associated value has wrong type.
     */
    public fun getCharSequenceArray(key: String): Array<CharSequence>

    /** Returns the value corresponding to the given [key], or null if such a key is not present. */
    public fun getCharSequenceArrayOrNull(key: String): Array<CharSequence>?

    /**
     * Retrieves a [DoubleArray] value associated with the specified [key].
     *
     * @param key The [key] to retrieve the value for.
     * @return The value associated with the [key].
     * @throws IllegalArgumentException If the [key] is not found.
     * @throws IllegalArgumentException if associated value has wrong type.
     */
    public fun getDoubleArray(key: String): DoubleArray

    /** Returns the value corresponding to the given [key], or null if such a key is not present. */
    public fun getDoubleArrayOrNull(key: String): DoubleArray?

    /**
     * Retrieves a [FloatArray] value associated with the specified [key].
     *
     * @param key The [key] to retrieve the value for.
     * @return The value associated with the [key].
     * @throws IllegalArgumentException If the [key] is not found.
     * @throws IllegalArgumentException if associated value has wrong type.
     */
    public fun getFloatArray(key: String): FloatArray

    /** Returns the value corresponding to the given [key], or null if such a key is not present. */
    public fun getFloatArrayOrNull(key: String): FloatArray?

    /**
     * Retrieves a [IntArray] value associated with the specified [key].
     *
     * @param key The [key] to retrieve the value for.
     * @return The value associated with the [key].
     * @throws IllegalArgumentException If the [key] is not found.
     * @throws IllegalArgumentException if associated value has wrong type.
     */
    public fun getIntArray(key: String): IntArray

    /** Returns the value corresponding to the given [key], or null if such a key is not present. */
    public fun getIntArrayOrNull(key: String): IntArray?

    /**
     * Retrieves a [LongArray] value associated with the specified [key].
     *
     * @param key The [key] to retrieve the value for.
     * @return The value associated with the [key].
     * @throws IllegalArgumentException If the [key] is not found.
     * @throws IllegalArgumentException if associated value has wrong type.
     */
    public fun getLongArray(key: String): LongArray

    /** Returns the value corresponding to the given [key], or null if such a key is not present. */
    public fun getLongArrayOrNull(key: String): LongArray?

    /**
     * Retrieves a [Array] of [SavedState] value associated with the specified [key].
     *
     * @param key The [key] to retrieve the value for.
     * @return The value associated with the [key].
     * @throws IllegalArgumentException If the [key] is not found.
     * @throws IllegalArgumentException if associated value has wrong type.
     */
    public fun getSavedStateArray(key: String): Array<SavedState>

    /** Returns the value corresponding to the given [key], or null if such a key is not present. */
    public fun getSavedStateArrayOrNull(key: String): Array<SavedState>?

    /**
     * Retrieves a [Array] of [String] value associated with the specified [key].
     *
     * @param key The [key] to retrieve the value for.
     * @return The value associated with the [key].
     * @throws IllegalArgumentException If the [key] is not found.
     * @throws IllegalArgumentException if associated value has wrong type.
     */
    public fun getStringArray(key: String): Array<String>

    /** Returns the value corresponding to the given [key], or null if such a key is not present. */
    public fun getStringArrayOrNull(key: String): Array<String>?

    /**
     * Retrieves a [SavedState] object associated with the specified [key].
     *
     * @param key The [key] to retrieve the value for.
     * @return The value associated with the [key].
     * @throws IllegalArgumentException If the [key] is not found.
     * @throws IllegalArgumentException if associated value has wrong type.
     */
    public fun getSavedState(key: String): SavedState

    /** Returns the value corresponding to the given [key], or null if such a key is not present. */
    public fun getSavedStateOrNull(key: String): SavedState?

    /**
     * Returns the number of key-value pairs in the [SavedState].
     *
     * @return The size of the [SavedState].
     */
    public fun size(): Int

    /**
     * Checks if the [SavedState] is empty (contains no key-value pairs).
     *
     * @return `true` if the [SavedState] is empty, `false` otherwise.
     */
    public fun isEmpty(): Boolean

    /**
     * Checks if the [SavedState] contains a null reference for the specified [key].
     *
     * @param key The [key] to check for.
     * @return `true` if the [SavedState] contains a null reference for the [key], `false`
     *   otherwise.
     */
    public fun isNull(key: String): Boolean

    /**
     * Checks if the [SavedState] contains the specified [key].
     *
     * @param key The [key] to check for.
     * @return `true` if the [SavedState] contains the [key], `false` otherwise.
     */
    public operator fun contains(key: String): Boolean

    /**
     * Checks if the two specified [SavedState] are *deeply* equal to one another.
     *
     * Two [SavedState] are considered deeply equal if they have the same size, and elements at
     * corresponding keys are deeply equal. That is, if two corresponding elements are nested
     * [SavedState], they are also compared deeply.
     *
     * If any of [SavedState] contains itself on any nesting level the behavior is undefined.
     *
     * @param other the object to compare deeply with this.
     * @return `true` if the two are deeply equal, `false` otherwise.
     */
    public fun contentDeepEquals(other: SavedState): Boolean

    /**
     * Returns a hash code based on the "deep contents" of specified [SavedState]. If the
     * [SavedState] contains other [SavedState] as elements, the hash code is based on their
     * contents and so on.
     *
     * The computation of the hash code returned is as if the [SavedState] is a [List]. Nested
     * [SavedState] are treated as lists too.
     *
     * If any of [SavedState] contains itself on any nesting level the behavior is undefined.
     *
     * @return a deep-content-based hash code for [SavedState].
     */
    public fun contentDeepHashCode(): Int

    /**
     * Returns a string representation of the contents of this [SavedState] as if it is a [List].
     * Nested [SavedState] are treated as lists too.
     *
     * If any of [SavedState] contains itself on any nesting level that reference is rendered as
     * `"[...]"` to prevent recursion.
     */
    public fun contentDeepToString(): String

    /**
     * Returns a new [Map] containing all key-value pairs from the [SavedState].
     *
     * The returned [Map] does not preserve the entry iteration order of the [SavedState].
     *
     * **IMPORTANT:** All values will be copied by reference, and values within the [SavedState]
     * that are also a [SavedState] will **NOT** be converted to a [Map].
     *
     * @return A [Map] containing all key-value pairs from the [SavedState].
     */
    public fun toMap(): Map<String, Any?>
}

@PublishedApi
internal fun keyOrValueNotFoundError(key: String): Nothing {
    throw IllegalArgumentException(
        "No valid saved state was found for the key '$key'. It may be missing, null, or not of " +
            "the expected type. This can occur if the value was saved with a different type or " +
            "if the saved state was modified unexpectedly."
    )
}
