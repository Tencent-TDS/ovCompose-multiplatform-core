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

import androidx.annotation.RestrictTo
import androidx.savedstate.SavedState
import kotlin.jvm.JvmStatic
import kotlin.reflect.KClass

public actual open class NavDestination actual constructor(navigatorName: String) {
    public actual val navigatorName: String = implementedInJetBrainsFork()

    public actual constructor(navigator: Navigator<out NavDestination>) : this("") {
        implementedInJetBrainsFork()
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual class DeepLinkMatch : Comparable<DeepLinkMatch> {
        public actual val destination: NavDestination = implementedInJetBrainsFork()

        public actual val matchingArgs: SavedState? = implementedInJetBrainsFork()

        public actual fun hasMatchingArgs(arguments: SavedState?): Boolean {
            implementedInJetBrainsFork()
        }

        override fun compareTo(other: DeepLinkMatch): Int {
            implementedInJetBrainsFork()
        }
    }

    public actual var parent: NavGraph? = implementedInJetBrainsFork()

    public actual var label: CharSequence? = implementedInJetBrainsFork()

    public actual val arguments: Map<String, NavArgument> = implementedInJetBrainsFork()

    public actual var route: String? = implementedInJetBrainsFork()
        set(_) {
            implementedInJetBrainsFork()
        }

    public actual open val displayName: String = implementedInJetBrainsFork()

    public actual open fun hasDeepLink(deepLink: NavUri): Boolean {
        implementedInJetBrainsFork()
    }

    public actual open fun hasDeepLink(deepLinkRequest: NavDeepLinkRequest): Boolean {
        implementedInJetBrainsFork()
    }

    public actual fun addDeepLink(uriPattern: String) {
        implementedInJetBrainsFork()
    }

    public actual fun addDeepLink(navDeepLink: NavDeepLink) {
        implementedInJetBrainsFork()
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual fun matchRoute(route: String): DeepLinkMatch? {
        implementedInJetBrainsFork()
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual open fun matchDeepLink(navDeepLinkRequest: NavDeepLinkRequest): DeepLinkMatch? {
        implementedInJetBrainsFork()
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual fun hasRoute(route: String, arguments: SavedState?): Boolean {
        implementedInJetBrainsFork()
    }

    public actual fun addArgument(argumentName: String, argument: NavArgument) {
        implementedInJetBrainsFork()
    }

    public actual fun removeArgument(argumentName: String) {
        implementedInJetBrainsFork()
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Suppress("NullableCollection")
    public actual fun addInDefaultArgs(args: SavedState?): SavedState? {
        implementedInJetBrainsFork()
    }

    public actual companion object {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public actual fun createRoute(route: String?): String {
            implementedInJetBrainsFork()
        }

        @JvmStatic
        public actual val NavDestination.hierarchy: Sequence<NavDestination>
            get() = implementedInJetBrainsFork()

        @JvmStatic
        public actual inline fun <reified T : Any> NavDestination.hasRoute(): Boolean {
            implementedInJetBrainsFork()
        }

        @JvmStatic
        public actual fun <T : Any> NavDestination.hasRoute(route: KClass<T>): Boolean {
            implementedInJetBrainsFork()
        }
    }
}
