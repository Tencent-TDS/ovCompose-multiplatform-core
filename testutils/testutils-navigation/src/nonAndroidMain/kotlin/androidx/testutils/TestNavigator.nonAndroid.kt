/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.testutils

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import androidx.navigation.Navigator

public actual open class TestNavigator actual constructor(private val hasTransitions: Boolean) :
    Navigator<TestNavigator.Destination>(TEST_NAVIGATOR_NAME) {

    public actual val backStack: List<NavBackStackEntry>
        get() = state.backStack.value

    public actual val current: NavBackStackEntry
        get() = backStack.lastOrNull()
            ?: throw IllegalStateException("Nothing on the back stack")

    public actual fun popCurrent() {
        state.pop(current, false)
    }

    actual override fun createDestination(): Destination {
        return Destination(this)
    }

    override fun navigate(
        entries: List<NavBackStackEntry>,
        navOptions: NavOptions?,
        navigatorExtras: Extras?
    ) {
        entries.forEach { entry ->
            if (hasTransitions) {
                state.pushWithTransition(entry)
            } else {
                state.push(entry)
            }
        }
    }

    override fun popBackStack(popUpTo: NavBackStackEntry, savedState: Boolean) {
        if (hasTransitions) {
            state.popWithTransition(popUpTo, savedState)
        } else {
            super.popBackStack(popUpTo, savedState)
        }
    }

    public actual fun onTransitionComplete(entry: NavBackStackEntry) {
        state.markTransitionComplete(entry)
    }

    public actual open class Destination actual constructor(
        navigator: Navigator<out NavDestination>
    ) : NavDestination(navigator)
}
