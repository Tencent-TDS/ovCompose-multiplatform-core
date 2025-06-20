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

package androidx.compose.ui.text

import kotlin.native.ref.WeakReference

internal actual class WeakKeysCache<K : Any, V> : Cache<K, V> {
    // TODO Use WeakHashMap once available https://youtrack.jetbrains.com/issue/KT-48075
    private val cache = HashMap<Key<K>, V>()

    actual override fun get(key: K, loader: (K) -> V): V {
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