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

import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedState
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner

public actual class NavBackStackEntry :
    LifecycleOwner,
    ViewModelStoreOwner,
    HasDefaultViewModelProviderFactory,
    SavedStateRegistryOwner {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual constructor(entry: NavBackStackEntry, arguments: SavedState?)

    actual override val savedStateRegistry: SavedStateRegistry
        get() = implementedInJetBrainsFork()

    actual override val lifecycle: Lifecycle
        get() = implementedInJetBrainsFork()

    actual override val viewModelStore: ViewModelStore
        get() = implementedInJetBrainsFork()

    actual override val defaultViewModelProviderFactory: ViewModelProvider.Factory
        get() = implementedInJetBrainsFork()

    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual var destination: NavDestination
        get() = implementedInJetBrainsFork()
        set(_) {
            implementedInJetBrainsFork()
        }

    public actual val id: String
        get() = implementedInJetBrainsFork()

    public actual val arguments: SavedState?
        get() = implementedInJetBrainsFork()

    @get:MainThread
    public actual val savedStateHandle: SavedStateHandle
        get() = implementedInJetBrainsFork()

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual var maxLifecycle: Lifecycle.State
        get() = implementedInJetBrainsFork()
        set(_) {
            implementedInJetBrainsFork()
        }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual fun handleLifecycleEvent(event: Lifecycle.Event) {
        implementedInJetBrainsFork()
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual fun updateState() {
        implementedInJetBrainsFork()
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual fun saveState(outBundle: SavedState) {
        implementedInJetBrainsFork()
    }
}
