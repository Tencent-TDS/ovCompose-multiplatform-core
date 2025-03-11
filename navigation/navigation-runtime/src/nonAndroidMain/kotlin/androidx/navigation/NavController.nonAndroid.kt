/*
 * Copyright (C) 2017 The Android Open Source Project
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

@file:JvmName("NavControllerKt")
@file:JvmMultifileClass

package androidx.navigation

import androidx.annotation.CallSuper
import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStore
import androidx.savedstate.SavedState
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.InternalSerializationApi

public actual open class NavController {

    public actual open var graph: NavGraph
        @MainThread get() = implementedInJetBrainsFork()
        @MainThread
        @CallSuper
        set(_) {
            implementedInJetBrainsFork()
        }

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual val currentBackStack: StateFlow<List<NavBackStackEntry>> =
        implementedInJetBrainsFork()

    public actual val visibleEntries: StateFlow<List<NavBackStackEntry>>

    internal actual fun unlinkChildFromParent(child: NavBackStackEntry): NavBackStackEntry? =
        implementedInJetBrainsFork()

    internal actual var hostLifecycleState: Lifecycle.State = implementedInJetBrainsFork()

    public actual fun interface OnDestinationChangedListener {
        public actual fun onDestinationChanged(
            controller: NavController,
            destination: NavDestination,
            arguments: SavedState?
        )
    }

    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual open var navigatorProvider: NavigatorProvider

    public actual open fun addOnDestinationChangedListener(listener: OnDestinationChangedListener) {
        implementedInJetBrainsFork()
    }

    public actual open fun removeOnDestinationChangedListener(
        listener: OnDestinationChangedListener
    ) {
        implementedInJetBrainsFork()
    }

    @MainThread public actual open fun popBackStack(): Boolean = implementedInJetBrainsFork()

    @MainThread
    @JvmOverloads
    public actual fun popBackStack(route: String, inclusive: Boolean, saveState: Boolean): Boolean =
        implementedInJetBrainsFork()

    @MainThread
    @JvmOverloads
    public actual inline fun <reified T : Any> popBackStack(
        inclusive: Boolean,
        saveState: Boolean
    ): Boolean = implementedInJetBrainsFork()

    @MainThread
    @JvmOverloads
    public actual fun <T : Any> popBackStack(
        route: KClass<T>,
        inclusive: Boolean,
        saveState: Boolean
    ): Boolean = implementedInJetBrainsFork()

    @MainThread
    @JvmOverloads
    public actual fun <T : Any> popBackStack(
        route: T,
        inclusive: Boolean,
        saveState: Boolean
    ): Boolean = implementedInJetBrainsFork()

    internal actual fun popBackStackFromNavigator(
        popUpTo: NavBackStackEntry,
        onComplete: () -> Unit
    ) {
        implementedInJetBrainsFork()
    }

    @MainThread
    public actual fun clearBackStack(route: String): Boolean = implementedInJetBrainsFork()

    @MainThread
    public actual inline fun <reified T : Any> clearBackStack(): Boolean =
        implementedInJetBrainsFork()

    @MainThread
    public actual fun <T : Any> clearBackStack(route: KClass<T>): Boolean =
        implementedInJetBrainsFork()

    @MainThread
    public actual fun <T : Any> clearBackStack(route: T): Boolean = implementedInJetBrainsFork()

    @MainThread public actual open fun navigateUp(): Boolean = implementedInJetBrainsFork()

    internal actual fun updateBackStackLifecycle() {
        implementedInJetBrainsFork()
    }

    internal actual fun populateVisibleEntries(): List<NavBackStackEntry> =
        implementedInJetBrainsFork()

    @MainThread
    @CallSuper
    public actual open fun setGraph(graph: NavGraph, startDestinationArgs: SavedState?) {
        implementedInJetBrainsFork()
    }

    @MainThread
    public actual fun handleDeepLink(request: NavDeepLinkRequest): Boolean =
        implementedInJetBrainsFork()

    public actual open val currentDestination: NavDestination? = implementedInJetBrainsFork()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual fun findDestination(route: String): NavDestination? = implementedInJetBrainsFork()

    @MainThread
    public actual open fun navigate(deepLink: NavUri) {
        implementedInJetBrainsFork()
    }

    @MainThread
    public actual open fun navigate(deepLink: NavUri, navOptions: NavOptions?) {
        implementedInJetBrainsFork()
    }

    @MainThread
    public actual open fun navigate(
        deepLink: NavUri,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras?
    ) {
        implementedInJetBrainsFork()
    }

    @MainThread
    public actual open fun navigate(request: NavDeepLinkRequest) {
        implementedInJetBrainsFork()
    }

    @MainThread
    public actual open fun navigate(request: NavDeepLinkRequest, navOptions: NavOptions?) {
        implementedInJetBrainsFork()
    }

    @MainThread
    public actual open fun navigate(
        request: NavDeepLinkRequest,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras?
    ) {
        implementedInJetBrainsFork()
    }

    @MainThread
    public actual fun navigate(route: String, builder: NavOptionsBuilder.() -> Unit) {
        implementedInJetBrainsFork()
    }

    @MainThread
    @JvmOverloads
    public actual fun navigate(
        route: String,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras?
    ) {
        implementedInJetBrainsFork()
    }

    @MainThread
    public actual fun <T : Any> navigate(route: T, builder: NavOptionsBuilder.() -> Unit) {
        implementedInJetBrainsFork()
    }

    @MainThread
    @JvmOverloads
    public actual fun <T : Any> navigate(
        route: T,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras?
    ) {
        implementedInJetBrainsFork()
    }

    @CallSuper public actual open fun saveState(): SavedState? = implementedInJetBrainsFork()

    @CallSuper
    public actual open fun restoreState(navState: SavedState?) {
        implementedInJetBrainsFork()
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual open fun setLifecycleOwner(owner: LifecycleOwner) {
        implementedInJetBrainsFork()
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual open fun setViewModelStore(viewModelStore: ViewModelStore) {
        implementedInJetBrainsFork()
    }

    public actual fun getBackStackEntry(route: String): NavBackStackEntry =
        implementedInJetBrainsFork()

    public actual inline fun <reified T : Any> getBackStackEntry(): NavBackStackEntry =
        implementedInJetBrainsFork()

    @OptIn(InternalSerializationApi::class)
    public actual fun <T : Any> getBackStackEntry(route: KClass<T>): NavBackStackEntry =
        implementedInJetBrainsFork()

    public actual fun <T : Any> getBackStackEntry(route: T): NavBackStackEntry =
        implementedInJetBrainsFork()

    public actual open val currentBackStackEntry: NavBackStackEntry? = implementedInJetBrainsFork()

    public actual val currentBackStackEntryFlow: Flow<NavBackStackEntry> =
        implementedInJetBrainsFork()

    public actual open val previousBackStackEntry: NavBackStackEntry? = implementedInJetBrainsFork()

    public actual companion object {
        @JvmStatic
        @NavDeepLinkSaveStateControl
        public actual fun enableDeepLinkSaveState(saveState: Boolean) {
            implementedInJetBrainsFork()
        }
    }
}
