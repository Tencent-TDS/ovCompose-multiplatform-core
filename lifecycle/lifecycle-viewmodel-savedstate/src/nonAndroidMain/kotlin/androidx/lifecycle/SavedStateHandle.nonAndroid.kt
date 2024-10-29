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

package androidx.lifecycle

import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import androidx.lifecycle.internal.SavedStateHandleImpl
import androidx.lifecycle.internal.isAcceptableType
import androidx.savedstate.SavedState
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.read
import kotlin.jvm.JvmStatic
import kotlinx.coroutines.flow.StateFlow

actual class SavedStateHandle {

    private var impl: SavedStateHandleImpl

    actual constructor(initialState: Map<String, Any?>) {
        impl = SavedStateHandleImpl(initialState)
    }

    actual constructor() {
        impl = SavedStateHandleImpl()
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    actual fun savedStateProvider(): SavedStateRegistry.SavedStateProvider =
        impl.savedStateProvider()

    @MainThread actual operator fun contains(key: String): Boolean = key in impl

    @MainThread
    actual fun <T> getStateFlow(key: String, initialValue: T): StateFlow<T> =
        impl.getStateFlow(key, initialValue)

    @MainThread actual fun keys(): Set<String> = impl.keys()

    @MainThread actual operator fun <T> get(key: String): T? = impl.get(key)

    @MainThread actual operator fun <T> set(key: String, value: T?) = impl.set(key, value)

    @MainThread actual fun <T> remove(key: String): T? = impl.remove(key)

    @MainThread
    actual fun setSavedStateProvider(key: String, provider: SavedStateRegistry.SavedStateProvider) =
        impl.setSavedStateProvider(key, provider)

    @MainThread
    actual fun clearSavedStateProvider(key: String) {
        impl.clearSavedStateProvider(key)
    }

    actual companion object {

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @JvmStatic
        @Suppress("DEPRECATION")
        actual fun createHandle(
            restoredState: SavedState?,
            defaultState: SavedState?,
        ): SavedStateHandle {
            val initialState = restoredState ?: defaultState

            // If there is no restored state or default state, an empty SavedStateHandle is created.
            if (initialState == null) return SavedStateHandle()

            return SavedStateHandle(initialState = initialState.read { toMap() })
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        actual fun validateValue(value: Any?): Boolean = isAcceptableType(value)
    }
}
