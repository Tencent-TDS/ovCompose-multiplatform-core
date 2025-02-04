/*
 * Copyright 2019 The Android Open Source Project
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

@file:JvmName("NavBackStackEntryKt")
@file:JvmMultifileClass

package androidx.navigation

import android.app.Application
import android.content.Context
import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import androidx.lifecycle.DEFAULT_ARGS_KEY
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.enableSavedStateHandles
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.savedstate.SavedState
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.savedState
import java.util.UUID

public actual class NavBackStackEntry
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
private constructor(
    private val context: Context?,
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public actual var destination: NavDestination,
    private val immutableArgs: SavedState? = null,
    private var hostLifecycleState: Lifecycle.State = Lifecycle.State.CREATED,
    private val viewModelStoreProvider: NavViewModelStoreProvider? = null,
    public actual val id: String = UUID.randomUUID().toString(),
    private val savedState: SavedState? = null
) :
    LifecycleOwner,
    ViewModelStoreOwner,
    HasDefaultViewModelProviderFactory,
    SavedStateRegistryOwner {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual constructor(
        entry: NavBackStackEntry,
        arguments: SavedState?
    ) : this(
        entry.context,
        entry.destination,
        arguments,
        entry.hostLifecycleState,
        entry.viewModelStoreProvider,
        entry.id,
        entry.savedState
    ) {
        hostLifecycleState = entry.hostLifecycleState
        maxLifecycle = entry.maxLifecycle
    }

    public companion object {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun create(
            context: Context?,
            destination: NavDestination,
            arguments: SavedState? = null,
            hostLifecycleState: Lifecycle.State = Lifecycle.State.CREATED,
            viewModelStoreProvider: NavViewModelStoreProvider? = null,
            id: String = UUID.randomUUID().toString(),
            savedState: SavedState? = null
        ): NavBackStackEntry =
            NavBackStackEntry(
                context,
                destination,
                arguments,
                hostLifecycleState,
                viewModelStoreProvider,
                id,
                savedState
            )
    }

    private var _lifecycle = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private var savedStateRegistryAttached = false
    private val defaultFactory by lazy {
        SavedStateViewModelFactory((context?.applicationContext as? Application), this, arguments)
    }

    public actual val arguments: SavedState?
        get() =
            if (immutableArgs == null) {
                null
            } else {
                savedState { putAll(immutableArgs) }
            }

    @get:MainThread
    public actual val savedStateHandle: SavedStateHandle by lazy {
        check(savedStateRegistryAttached) {
            "You cannot access the NavBackStackEntry's SavedStateHandle until it is added to " +
                "the NavController's back stack (i.e., the Lifecycle of the NavBackStackEntry " +
                "reaches the CREATED state)."
        }
        check(lifecycle.currentState != Lifecycle.State.DESTROYED) {
            "You cannot access the NavBackStackEntry's SavedStateHandle after the " +
                "NavBackStackEntry is destroyed."
        }
        ViewModelProvider(this, navResultSavedStateFactory)
            .get(SavedStateViewModel::class.java)
            .handle
    }

    actual override val lifecycle: Lifecycle
        get() = _lifecycle

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual var maxLifecycle: Lifecycle.State = Lifecycle.State.INITIALIZED
        set(maxState) {
            field = maxState
            updateState()
        }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual fun handleLifecycleEvent(event: Lifecycle.Event) {
        hostLifecycleState = event.targetState
        updateState()
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual fun updateState() {
        if (!savedStateRegistryAttached) {
            savedStateRegistryController.performAttach()
            savedStateRegistryAttached = true
            if (viewModelStoreProvider != null) {
                enableSavedStateHandles()
            }
            // Perform the restore just once, the first time updateState() is called
            // and specifically *before* we move up the Lifecycle
            savedStateRegistryController.performRestore(savedState)
        }
        if (hostLifecycleState.ordinal < maxLifecycle.ordinal) {
            _lifecycle.currentState = hostLifecycleState
        } else {
            _lifecycle.currentState = maxLifecycle
        }
    }

    public actual override val viewModelStore: ViewModelStore
        get() {
            check(savedStateRegistryAttached) {
                "You cannot access the NavBackStackEntry's ViewModels until it is added to " +
                    "the NavController's back stack (i.e., the Lifecycle of the " +
                    "NavBackStackEntry reaches the CREATED state)."
            }
            check(lifecycle.currentState != Lifecycle.State.DESTROYED) {
                "You cannot access the NavBackStackEntry's ViewModels after the " +
                    "NavBackStackEntry is destroyed."
            }
            checkNotNull(viewModelStoreProvider) {
                "You must call setViewModelStore() on your NavHostController before " +
                    "accessing the ViewModelStore of a navigation graph."
            }
            return viewModelStoreProvider.getViewModelStore(id)
        }

    override actual val defaultViewModelProviderFactory: ViewModelProvider.Factory = defaultFactory

    override actual val defaultViewModelCreationExtras: CreationExtras
        get() {
            val extras = MutableCreationExtras()
            (context?.applicationContext as? Application)?.let { application ->
                extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] = application
            }
            extras[SAVED_STATE_REGISTRY_OWNER_KEY] = this
            extras[VIEW_MODEL_STORE_OWNER_KEY] = this
            arguments?.let { args -> extras[DEFAULT_ARGS_KEY] = args }
            return extras
        }

    override actual val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual fun saveState(outBundle: SavedState) {
        savedStateRegistryController.performSave(outBundle)
    }

    @Suppress("DEPRECATION")
    override fun equals(other: Any?): Boolean {
        if (other == null || other !is NavBackStackEntry) return false
        return id == other.id &&
            destination == other.destination &&
            lifecycle == other.lifecycle &&
            savedStateRegistry == other.savedStateRegistry &&
            (immutableArgs == other.immutableArgs ||
                immutableArgs?.keySet()?.all {
                    immutableArgs.get(it) == other.immutableArgs?.get(it)
                } == true)
    }

    @Suppress("DEPRECATION")
    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + destination.hashCode()
        immutableArgs?.keySet()?.forEach { result = 31 * result + immutableArgs.get(it).hashCode() }
        result = 31 * result + lifecycle.hashCode()
        result = 31 * result + savedStateRegistry.hashCode()
        return result
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append(javaClass.simpleName)
        sb.append("($id)")
        sb.append(" destination=")
        sb.append(destination)
        return sb.toString()
    }

    /** Used to create the {SavedStateViewModel} */
    private val navResultSavedStateFactory by lazy {
        viewModelFactory { initializer { SavedStateViewModel(createSavedStateHandle()) } }
    }

    private class SavedStateViewModel(val handle: SavedStateHandle) : ViewModel()
}
