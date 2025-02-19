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
import androidx.collection.SparseArrayCompat
import kotlin.jvm.JvmStatic
import kotlin.jvm.JvmSynthetic
import kotlin.reflect.KClass
import kotlinx.serialization.KSerializer

public actual open class NavGraph actual constructor(navGraphNavigator: Navigator<out NavGraph>) :
    NavDestination(navGraphNavigator), Iterable<NavDestination> {
    public actual val nodes: SparseArrayCompat<NavDestination>
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) get() = implementedInJetBrainsFork()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual fun matchRouteComprehensive(
        route: String,
        searchChildren: Boolean,
        searchParent: Boolean,
        lastVisited: NavDestination
    ): DeepLinkMatch? {
        implementedInJetBrainsFork()
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual fun matchDeepLinkComprehensive(
        navDeepLinkRequest: NavDeepLinkRequest,
        searchChildren: Boolean,
        searchParent: Boolean,
        lastVisited: NavDestination
    ): DeepLinkMatch? {
        implementedInJetBrainsFork()
    }

    public actual fun addDestination(node: NavDestination) {
        implementedInJetBrainsFork()
    }

    public actual fun addDestinations(nodes: Collection<NavDestination?>) {
        implementedInJetBrainsFork()
    }

    public actual fun addDestinations(vararg nodes: NavDestination) {
        implementedInJetBrainsFork()
    }

    public actual fun findNode(route: String?): NavDestination? {
        implementedInJetBrainsFork()
    }

    public actual inline fun <reified T> findNode(): NavDestination? {
        implementedInJetBrainsFork()
    }

    public actual fun findNode(route: KClass<*>): NavDestination? {
        implementedInJetBrainsFork()
    }

    public actual fun <T> findNode(route: T?): NavDestination? {
        implementedInJetBrainsFork()
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual fun findNode(route: String, searchParents: Boolean): NavDestination? {
        implementedInJetBrainsFork()
    }

    actual override fun iterator(): MutableIterator<NavDestination> {
        implementedInJetBrainsFork()
    }

    public actual fun addAll(other: NavGraph) {
        implementedInJetBrainsFork()
    }

    public actual fun remove(node: NavDestination) {
        implementedInJetBrainsFork()
    }

    public actual fun clear() {
        implementedInJetBrainsFork()
    }

    public actual fun setStartDestination(startDestRoute: String) {
        implementedInJetBrainsFork()
    }

    public actual inline fun <reified T : Any> setStartDestination() {
        implementedInJetBrainsFork()
    }

    @JvmSynthetic
    public actual fun <T : Any> setStartDestination(startDestRoute: KClass<T>) {
        implementedInJetBrainsFork()
    }

    public actual fun <T : Any> setStartDestination(startDestRoute: T) {
        implementedInJetBrainsFork()
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual fun <T> setStartDestination(
        serializer: KSerializer<T>,
        parseRoute: (NavDestination) -> String
    ) {
        implementedInJetBrainsFork()
    }

    public actual var startDestinationRoute: String?
        get() = implementedInJetBrainsFork()
        set(_) {
            implementedInJetBrainsFork()
        }

    public actual val startDestDisplayName: String
        get() = implementedInJetBrainsFork()

    public actual companion object {
        @JvmStatic
        public actual fun NavGraph.findStartDestination(): NavDestination {
            implementedInJetBrainsFork()
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public actual fun NavGraph.childHierarchy(): Sequence<NavDestination> {
            implementedInJetBrainsFork()
        }
    }
}
