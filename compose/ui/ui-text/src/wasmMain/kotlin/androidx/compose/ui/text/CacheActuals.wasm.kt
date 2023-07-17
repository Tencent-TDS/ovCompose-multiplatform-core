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


internal actual fun newWeakRef(obj: Any): InternalWeakRef = object : InternalWeakRef {
    val jsWeakRef = newWeakRef(obj.toJsReference())

    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    override fun get(): Any? {
        return (jsWeakRef.deref() as? JsReference<*>)?.get()
    }
}

private fun newWeakRef(obj: JsAny): WeakRef = js("new WeakRef(obj)")

private external interface WeakRef {
    fun deref(): JsAny?
}

internal actual fun newWeakHashMapCleaner(cleanKey: (Key<*>) -> Unit): WeakHashMapCleaner =
    object : WeakHashMapCleaner {
        val registry = FinalizationRegistry {
            cleanKey(it.get())
        }

        override fun register(obj: Any, handle: Key<*>) {
            registry.register(obj.toJsReference(), handle.toJsReference())
        }
    }

internal external class FinalizationRegistry(cleanup: (JsReference<Key<*>>) -> Unit) {
    fun register(obj: JsReference<Any>, handle: JsReference<Key<*>>)
}