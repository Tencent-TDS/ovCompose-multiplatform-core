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

import androidx.annotation.CallSuper
import androidx.savedstate.SavedState

public actual abstract class Navigator<D : NavDestination> {
    protected actual val state: NavigatorState
        get() = implementedInJetBrainsFork()

    public actual var isAttached: Boolean
        get() = implementedInJetBrainsFork()
        set(_) {
            implementedInJetBrainsFork()
        }

    @CallSuper
    public actual open fun onAttach(state: NavigatorState) {
        implementedInJetBrainsFork()
    }

    public actual abstract fun createDestination(): D

    @Suppress("UNCHECKED_CAST")
    public actual open fun navigate(
        entries: List<NavBackStackEntry>,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras?
    ) {
        implementedInJetBrainsFork()
    }

    @Suppress("UNUSED_PARAMETER", "RedundantNullableReturnType")
    public actual open fun navigate(
        destination: D,
        args: SavedState?,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras?
    ): NavDestination? {
        implementedInJetBrainsFork()
    }

    @Suppress("UNCHECKED_CAST")
    public actual open fun onLaunchSingleTop(backStackEntry: NavBackStackEntry) {
        implementedInJetBrainsFork()
    }

    @Suppress("UNUSED_PARAMETER")
    public actual open fun popBackStack(popUpTo: NavBackStackEntry, savedState: Boolean) {
        implementedInJetBrainsFork()
    }

    public actual open fun popBackStack(): Boolean {
        implementedInJetBrainsFork()
    }

    public actual open fun onSaveState(): SavedState? {
        implementedInJetBrainsFork()
    }

    public actual open fun onRestoreState(savedState: SavedState) {
        implementedInJetBrainsFork()
    }

    public actual interface Extras
}
