/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.FloatingWindow
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import androidx.navigation.compose.DialogNavigator.Destination
import androidx.navigation.compose.internal.implementedInJetBrainsFork
import kotlinx.coroutines.flow.StateFlow

public actual class DialogNavigator actual constructor() : Navigator<Destination>() {
    internal actual val backStack: StateFlow<List<NavBackStackEntry>>
        get() = implementedInJetBrainsFork()

    internal actual val transitionInProgress: StateFlow<Set<NavBackStackEntry>>
        get() = implementedInJetBrainsFork()

    internal actual fun dismiss(backStackEntry: NavBackStackEntry) {
        implementedInJetBrainsFork()
    }

    actual override fun navigate(
        entries: List<NavBackStackEntry>,
        navOptions: NavOptions?,
        navigatorExtras: Extras?
    ) {
        implementedInJetBrainsFork()
    }

    actual override fun createDestination(): Destination = implementedInJetBrainsFork()

    actual override fun popBackStack(popUpTo: NavBackStackEntry, savedState: Boolean) {
        implementedInJetBrainsFork()
    }

    internal actual fun onTransitionComplete(entry: NavBackStackEntry) {
        implementedInJetBrainsFork()
    }

    public actual class Destination
    actual constructor(
        navigator: DialogNavigator,
        internal actual val dialogProperties: DialogProperties,
        internal actual val content: @Composable (NavBackStackEntry) -> Unit
    ) : NavDestination(navigator), FloatingWindow

    internal actual companion object {
        internal actual const val NAME = "dialog"
    }
}
