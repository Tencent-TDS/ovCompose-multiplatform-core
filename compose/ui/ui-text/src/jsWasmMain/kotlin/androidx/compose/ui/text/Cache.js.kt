/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.text

internal actual typealias WeakKeysCache<K, V> = WeakHashMap<K, V>

internal class WeakHashMap<K : Any, V> : Cache<K, V> {
    private val cache = HashMap<Key<K>, V>()

    // Based on https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/FinalizationRegistry
    // It should eventually clean up all entries with unreachable keys
    private val cleaner = newWeakHashMapCleaner { handle ->
        cache.remove(handle)
    }

    override fun get(key: K, loader: (K) -> V): V {
        val wrappedKey = Key(key)
        return cache.getOrPut(wrappedKey) {
            cleaner.register(key, wrappedKey)
            loader(key)
        }
    }
}

internal interface WeakHashMapCleaner {
    fun register(obj: Any, handle: Key<*>)
}

internal expect fun newWeakHashMapCleaner(cleanKey: (Key<*>) -> Unit): WeakHashMapCleaner

internal interface InternalWeakRef {
    fun get(): Any?
}

internal expect fun newWeakRef(obj: Any): InternalWeakRef

internal class Key<K : Any>(key: K) {
    private val ref = newWeakRef(key)
    private val hash: Int = key.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        other as Key<*>
        return ref.get() == other.ref.get()
    }

    override fun hashCode(): Int = hash
}