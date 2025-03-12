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

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import androidx.navigation.compose.ComposeNavigator.Destination
import androidx.navigation.implementedInJetBrainsFork
import kotlin.jvm.JvmSuppressWildcards
import kotlinx.coroutines.flow.StateFlow

public actual class ComposeNavigator actual constructor() : Navigator<Destination>() {

    internal actual val transitionsInProgress: StateFlow<Set<NavBackStackEntry>>
        get() = implementedInJetBrainsFork()

    public actual val backStack: StateFlow<List<NavBackStackEntry>>
        get() = implementedInJetBrainsFork()

    internal actual val isPop: MutableState<Boolean> = implementedInJetBrainsFork()

    actual override fun navigate(
        entries: List<NavBackStackEntry>,
        navOptions: NavOptions?,
        navigatorExtras: Extras?
    ) {
        implementedInJetBrainsFork()
    }

    actual override fun createDestination(): Destination {
        implementedInJetBrainsFork()
    }

    actual override fun popBackStack(popUpTo: NavBackStackEntry, savedState: Boolean) {
        implementedInJetBrainsFork()
    }

    public actual fun prepareForTransition(entry: NavBackStackEntry) {
        implementedInJetBrainsFork()
    }

    public actual fun onTransitionComplete(entry: NavBackStackEntry) {
        implementedInJetBrainsFork()
    }

    /** NavDestination specific to [ComposeNavigator] */
    public actual class Destination
    actual constructor(
        navigator: ComposeNavigator,
        internal actual val content:
            @Composable
            AnimatedContentScope.(@JvmSuppressWildcards NavBackStackEntry) -> Unit
    ) : NavDestination(navigator) {

        @Deprecated(
            message = "Deprecated in favor of Destination that supports AnimatedContent",
            level = DeprecationLevel.HIDDEN,
        )
        public constructor(
            navigator: ComposeNavigator,
            content: @Composable (NavBackStackEntry) -> @JvmSuppressWildcards Unit
        ) : this(navigator, content = { entry -> content(entry) })

        internal actual var enterTransition:
            (@JvmSuppressWildcards
            AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?)? =
            null

        internal actual var exitTransition:
            (@JvmSuppressWildcards
            AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?)? =
            null

        internal actual var popEnterTransition:
            (@JvmSuppressWildcards
            AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?)? =
            null

        internal actual var popExitTransition:
            (@JvmSuppressWildcards
            AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?)? =
            null

        internal actual var sizeTransform:
            (@JvmSuppressWildcards
            AnimatedContentTransitionScope<NavBackStackEntry>.() -> SizeTransform?)? =
            null
    }

    internal actual companion object {
        internal actual const val NAME = "composable"
    }
}
