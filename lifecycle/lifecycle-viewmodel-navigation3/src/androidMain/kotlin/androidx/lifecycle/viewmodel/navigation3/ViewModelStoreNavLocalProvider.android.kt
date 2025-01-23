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

package androidx.lifecycle.viewmodel.navigation3

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.remember
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.enableSavedStateHandles
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.NavEntry
import androidx.navigation3.NavLocalProvider
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.compose.LocalSavedStateRegistryOwner

/**
 * Provides the content of a [NavEntry] with a [ViewModelStoreOwner] and provides that
 * [ViewModelStoreOwner] as a [LocalViewModelStoreOwner] so that it is available within the content.
 *
 * This requires that usage of the [SavedStateNavLocalProvider] to ensure that the [NavEntry] scoped
 * [ViewModel]s can properly provide access to [SavedStateHandle]s
 */
public object ViewModelStoreNavLocalProvider : NavLocalProvider {

    @Composable
    override fun ProvideToBackStack(backStack: List<Any>, content: @Composable () -> Unit) {
        val entryViewModelStoreProvider = viewModel { EntryViewModel() }
        entryViewModelStoreProvider.ownerInBackStack.clear()
        entryViewModelStoreProvider.ownerInBackStack.addAll(backStack)
        content.invoke()
    }

    @Composable
    override fun <T : Any> ProvideToEntry(entry: NavEntry<T>) {
        val key = entry.key
        val entryViewModelStoreProvider = viewModel { EntryViewModel() }
        val viewModelStore = entryViewModelStoreProvider.viewModelStoreForKey(key)
        // This ensures we always keep viewModels on config changes.
        val activity = LocalActivity.current
        remember(key, viewModelStore) {
            object : RememberObserver {
                override fun onAbandoned() {
                    if (!entryViewModelStoreProvider.ownerInBackStack.contains(key)) {
                        disposeIfNotChangingConfiguration()
                    }
                }

                override fun onForgotten() {
                    if (!entryViewModelStoreProvider.ownerInBackStack.contains(key)) {
                        disposeIfNotChangingConfiguration()
                    }
                }

                override fun onRemembered() {}

                fun disposeIfNotChangingConfiguration() {
                    if (activity?.isChangingConfigurations != true) {
                        entryViewModelStoreProvider.removeViewModelStoreOwnerForKey(key)?.clear()
                    }
                }
            }
        }

        val savedStateRegistryOwner = LocalSavedStateRegistryOwner.current
        val childViewModelOwner = remember {
            object :
                ViewModelStoreOwner,
                SavedStateRegistryOwner by savedStateRegistryOwner,
                HasDefaultViewModelProviderFactory {
                override val viewModelStore: ViewModelStore
                    get() = viewModelStore

                override val defaultViewModelProviderFactory: ViewModelProvider.Factory
                    get() = SavedStateViewModelFactory(null, savedStateRegistryOwner)

                override val defaultViewModelCreationExtras: CreationExtras
                    get() =
                        MutableCreationExtras().also {
                            it[SAVED_STATE_REGISTRY_OWNER_KEY] = savedStateRegistryOwner
                            it[VIEW_MODEL_STORE_OWNER_KEY] = this
                        }

                init {
                    require(this.lifecycle.currentState == Lifecycle.State.INITIALIZED) {
                        "The Lifecycle state is already beyond INITIALIZED. The " +
                            "ViewModelStoreNavLocalProvider requires adding the " +
                            "SavedStateNavLocalProvider to ensure support for " +
                            "SavedStateHandles."
                    }
                    enableSavedStateHandles()
                }
            }
        }
        CompositionLocalProvider(LocalViewModelStoreOwner provides childViewModelOwner) {
            entry.content.invoke(key)
        }
    }
}

private class EntryViewModel : ViewModel() {
    private val owners = mutableMapOf<Any, ViewModelStore>()
    val ownerInBackStack = mutableListOf<Any>()

    fun viewModelStoreForKey(key: Any): ViewModelStore = owners.getOrPut(key) { ViewModelStore() }

    fun removeViewModelStoreOwnerForKey(key: Any): ViewModelStore? = owners.remove(key)

    override fun onCleared() {
        owners.forEach { (_, store) -> store.clear() }
    }
}
