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

import androidx.compose.runtime.ComposeTabService
import androidx.compose.runtime.Lock
import androidx.compose.runtime.platformSynchronizedObject
import androidx.compose.runtime.synchronized
import kotlin.native.ref.WeakReference

internal actual class WeakKeysCache<K : Any, V> : Cache<K, V> {
    // TODO Use WeakHashMap once available https://youtrack.jetbrains.com/issue/KT-48075
    private val cache = HashMap<Key<K>, V>()

    // region Tencent Code
    private val lock: Lock? = if (ComposeTabService.textAsyncPaint) platformSynchronizedObject() else null
    // end region

    actual override fun get(key: K, loader: (K) -> V): V {
        // region Tencent Code
        val lock = this.lock
        if (lock != null) {
            return synchronized(lock) {
                clean()
                cache.getOrPut(Key(key)) { loader(key) }
            }
        }
        // end region

        clean()
        return cache.getOrPut(Key(key)) { loader(key) }
    }

    private fun clean() {
        cache.keys
            .filter { !it.isAvailable }
            .forEach {
                cache.remove(it)
            }
    }

    private class Key<K : Any>(key: K) {
        private val ref = WeakReference(key)
        private val hash: Int = key.hashCode()

        val isAvailable get() = ref.value != null

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            other as Key<*>
            return ref.value == other.ref.value
        }

        override fun hashCode(): Int = hash
    }
}