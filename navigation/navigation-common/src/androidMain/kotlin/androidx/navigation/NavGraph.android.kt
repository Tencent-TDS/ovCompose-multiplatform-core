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

@file:JvmName("NavGraphKt")
@file:JvmMultifileClass

package androidx.navigation

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.IdRes
import androidx.annotation.RestrictTo
import androidx.collection.SparseArrayCompat
import androidx.collection.forEach
import androidx.collection.size
import androidx.collection.valueIterator
import androidx.core.content.res.use
import androidx.navigation.common.R
import androidx.navigation.serialization.generateHashCode
import androidx.navigation.serialization.generateRouteWithArgs
import kotlin.reflect.KClass
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

public actual open class NavGraph actual constructor(navGraphNavigator: Navigator<out NavGraph>) :
    NavDestination(navGraphNavigator), Iterable<NavDestination> {

    public actual val nodes: SparseArrayCompat<NavDestination> = SparseArrayCompat<NavDestination>()
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) get

    private var startDestId = 0
    private var startDestIdName: String? = null

    override fun onInflate(context: Context, attrs: AttributeSet) {
        super.onInflate(context, attrs)
        context.resources.obtainAttributes(attrs, R.styleable.NavGraphNavigator).use {
            startDestinationId = it.getResourceId(R.styleable.NavGraphNavigator_startDestination, 0)
            startDestIdName = getDisplayName(context, startDestId)
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual fun matchRouteComprehensive(
        route: String,
        searchChildren: Boolean,
        searchParent: Boolean,
        lastVisited: NavDestination
    ): DeepLinkMatch? {
        // First try to match with this graph's route
        val bestMatch = matchRoute(route)
        // If searchChildren is true, search through all child destinations for a matching route
        val bestChildMatch =
            if (searchChildren) {
                mapNotNull { child ->
                        when (child) {
                            lastVisited -> null
                            is NavGraph ->
                                child.matchRouteComprehensive(
                                    route,
                                    searchChildren = true,
                                    searchParent = false,
                                    lastVisited = this
                                )
                            else -> child.matchRoute(route)
                        }
                    }
                    .maxOrNull()
            } else null

        // If searchParent is true, search through all parents (and their children) destinations
        // for a matching route
        val bestParentMatch =
            parent?.let {
                if (searchParent && it != lastVisited)
                    it.matchRouteComprehensive(route, searchChildren, true, this)
                else null
            }
        return listOfNotNull(bestMatch, bestChildMatch, bestParentMatch).maxOrNull()
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual fun matchDeepLinkComprehensive(
        navDeepLinkRequest: NavDeepLinkRequest,
        searchChildren: Boolean,
        searchParent: Boolean,
        lastVisited: NavDestination
    ): DeepLinkMatch? {
        // First search through any deep links directly added to this NavGraph
        val bestMatch = super.matchDeepLink(navDeepLinkRequest)

        // If searchChildren is true, search through all child destinations for a matching deeplink
        val bestChildMatch =
            if (searchChildren) {
                mapNotNull { child ->
                        if (child != lastVisited) child.matchDeepLink(navDeepLinkRequest) else null
                    }
                    .maxOrNull()
            } else null

        // If searchParent is true, search through all parents (and their children) destinations
        // for a matching deeplink
        val bestParentMatch =
            parent?.let {
                if (searchParent && it != lastVisited)
                    it.matchDeepLinkComprehensive(navDeepLinkRequest, searchChildren, true, this)
                else null
            }
        return listOfNotNull(bestMatch, bestChildMatch, bestParentMatch).maxOrNull()
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual override fun matchDeepLink(
        navDeepLinkRequest: NavDeepLinkRequest
    ): DeepLinkMatch? =
        matchDeepLinkComprehensive(
            navDeepLinkRequest,
            searchChildren = true,
            searchParent = false,
            lastVisited = this
        )

    public actual fun addDestination(node: NavDestination) {
        val id = node.id
        val innerRoute = node.route
        require(id != 0 || innerRoute != null) {
            "Destinations must have an id or route. Call setId(), setRoute(), or include an " +
                "android:id or app:route in your navigation XML."
        }
        if (route != null) {
            require(innerRoute != route) {
                "Destination $node cannot have the same route as graph $this"
            }
        }
        require(id != this.id) { "Destination $node cannot have the same id as graph $this" }
        val existingDestination = nodes[id]
        if (existingDestination === node) {
            return
        }
        check(node.parent == null) {
            "Destination already has a parent set. Call NavGraph.remove() to remove the previous " +
                "parent."
        }
        if (existingDestination != null) {
            existingDestination.parent = null
        }
        node.parent = this
        nodes.put(node.id, node)
    }

    public actual fun addDestinations(nodes: Collection<NavDestination?>) {
        for (node in nodes) {
            if (node == null) {
                continue
            }
            addDestination(node)
        }
    }

    public actual fun addDestinations(vararg nodes: NavDestination) {
        for (node in nodes) {
            addDestination(node)
        }
    }

    /**
     * Finds a destination in the collection by ID. This will recursively check the [parent][parent]
     * of this navigation graph if node is not found in this navigation graph.
     *
     * @param resId ID to locate
     * @return the node with ID resId
     */
    public fun findNode(@IdRes resId: Int): NavDestination? =
        findNodeComprehensive(resId, this, false)

    /**
     * Searches all children and parents recursively.
     *
     * Does not revisit graphs (whether it's a child or parent) if it has already been visited.
     *
     * @param resId the [NavDestination.id]
     * @param lastVisited the previously visited node
     * @param searchChildren searches the graph's children for the node when true
     * @param matchingDest an optional NavDestination that the node should match with. This is
     *   because [resId] is only unique to a local graph. Nodes in sibling graphs can have the same
     *   id.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun findNodeComprehensive(
        @IdRes resId: Int,
        lastVisited: NavDestination?,
        searchChildren: Boolean,
        matchingDest: NavDestination? = null,
    ): NavDestination? {
        // first search direct children
        var destination = nodes[resId]
        when {
            matchingDest != null ->
                // check parent in case of duplicated destinations to ensure it finds the correct
                // nested destination
                if (destination == matchingDest && destination.parent == matchingDest.parent)
                    return destination
                else destination = null
            else -> if (destination != null) return destination
        }

        if (searchChildren) {
            // then dfs through children. Avoid re-visiting children that were recursing up this
            // way.
            destination =
                nodes.valueIterator().asSequence().firstNotNullOfOrNull { child ->
                    if (child is NavGraph && child != lastVisited) {
                        child.findNodeComprehensive(resId, this, true, matchingDest)
                    } else null
                }
        }

        // lastly search through parents. Avoid re-visiting parents that were recursing down
        // this way.
        return destination
            ?: if (parent != null && parent != lastVisited) {
                parent!!.findNodeComprehensive(resId, this, searchChildren, matchingDest)
            } else null
    }

    public actual fun findNode(route: String?): NavDestination? {
        return if (!route.isNullOrBlank()) findNode(route, true) else null
    }

    public actual inline fun <reified T> findNode(): NavDestination? = findNode(T::class)

    @OptIn(InternalSerializationApi::class)
    public actual fun findNode(route: KClass<*>): NavDestination? =
        findNode(route.serializer().generateHashCode())

    @OptIn(InternalSerializationApi::class)
    public actual fun <T> findNode(route: T?): NavDestination? =
        route?.let { findNode(it::class.serializer().generateHashCode()) }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual fun findNode(route: String, searchParents: Boolean): NavDestination? {
        val destination =
            nodes.valueIterator().asSequence().firstOrNull {
                // first try matching with routePattern
                // if not found with routePattern, try matching with route args
                it.route.equals(route) || it.matchRoute(route) != null
            }

        // Search the parent for the NavDestination if it is not a child of this navigation graph
        // and searchParents is true
        return destination
            ?: if (searchParents && parent != null) parent!!.findNode(route) else null
    }

    public actual final override fun iterator(): MutableIterator<NavDestination> {
        return object : MutableIterator<NavDestination> {
            private var index = -1
            private var wentToNext = false

            override fun hasNext(): Boolean {
                return index + 1 < nodes.size()
            }

            override fun next(): NavDestination {
                if (!hasNext()) {
                    throw NoSuchElementException()
                }
                wentToNext = true
                return nodes.valueAt(++index)
            }

            override fun remove() {
                check(wentToNext) { "You must call next() before you can remove an element" }
                with(nodes) {
                    valueAt(index).parent = null
                    removeAt(index)
                }
                index--
                wentToNext = false
            }
        }
    }

    public actual fun addAll(other: NavGraph) {
        val iterator = other.iterator()
        while (iterator.hasNext()) {
            val destination = iterator.next()
            iterator.remove()
            addDestination(destination)
        }
    }

    public actual fun remove(node: NavDestination) {
        val index = nodes.indexOfKey(node.id)
        if (index >= 0) {
            nodes.valueAt(index).parent = null
            nodes.removeAt(index)
        }
    }

    public actual fun clear() {
        val iterator = iterator()
        while (iterator.hasNext()) {
            iterator.next()
            iterator.remove()
        }
    }

    actual override val displayName: String
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        get() = if (id != 0) super.displayName else "the root navigation"

    /**
     * Gets the starting destination for this NavGraph. When navigating to the NavGraph, this
     * destination is the one the user will initially see.
     *
     * @return the start destination
     */
    @IdRes
    @Deprecated("Use getStartDestinationId instead.", ReplaceWith("startDestinationId"))
    public fun getStartDestination(): Int = startDestinationId

    /**
     * The starting destination id for this NavGraph. When navigating to the NavGraph, the
     * destination represented by this id is the one the user will initially see.
     */
    @get:IdRes
    public var startDestinationId: Int
        get() = startDestId
        private set(startDestId) {
            require(startDestId != id) {
                "Start destination $startDestId cannot use the same id as the graph $this"
            }
            if (startDestinationRoute != null) {
                startDestinationRoute = null
            }
            this.startDestId = startDestId
            startDestIdName = null
        }

    /**
     * Sets the starting destination for this NavGraph.
     *
     * This will clear any previously set [startDestinationRoute].
     *
     * @param startDestId The id of the destination to be shown when navigating to this NavGraph.
     */
    public fun setStartDestination(startDestId: Int) {
        startDestinationId = startDestId
    }

    public actual fun setStartDestination(startDestRoute: String) {
        startDestinationRoute = startDestRoute
    }

    public actual inline fun <reified T : Any> setStartDestination() {
        setStartDestination(T::class)
    }

    /**
     * Sets the starting destination for this NavGraph.
     *
     * This will override any previously set [startDestinationId]
     *
     * @param startDestRoute The [KClass] of route [T] to be shown when navigating to this NavGraph.
     */
    @JvmSynthetic
    @OptIn(InternalSerializationApi::class)
    public actual fun <T : Any> setStartDestination(startDestRoute: KClass<T>) {
        setStartDestination(startDestRoute.serializer()) { startDestination ->
            startDestination.route!!
        }
    }

    @JvmSynthetic
    @OptIn(InternalSerializationApi::class)
    public actual fun <T : Any> setStartDestination(startDestRoute: T) {
        setStartDestination(startDestRoute::class.serializer()) { startDestination ->
            val args = startDestination.arguments.mapValues { it.value.type }
            generateRouteWithArgs(startDestRoute, args)
        }
    }

    // unfortunately needs to be public so reified setStartDestination can access this
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @OptIn(ExperimentalSerializationApi::class)
    public actual fun <T> setStartDestination(
        serializer: KSerializer<T>,
        parseRoute: (NavDestination) -> String,
    ) {
        val id = serializer.generateHashCode()
        val startDest = findNode(id)
        checkNotNull(startDest) {
            "Cannot find startDestination ${serializer.descriptor.serialName} from NavGraph. " +
                "Ensure the starting NavDestination was added with route from KClass."
        }
        // when dest id is based on serializer, we expect the dest route to have been generated
        // and set
        startDestinationRoute = parseRoute(startDest)
        // bypass startDestinationId setter so we don't set route back to null
        this.startDestId = id
    }

    public actual var startDestinationRoute: String? = null
        private set(startDestRoute) {
            startDestId =
                if (startDestRoute == null) {
                    0
                } else {
                    require(startDestRoute != route) {
                        "Start destination $startDestRoute cannot use the same route as the graph $this"
                    }
                    require(startDestRoute.isNotBlank()) {
                        "Cannot have an empty start destination route"
                    }
                    val internalRoute = createRoute(startDestRoute)
                    internalRoute.hashCode()
                }
            field = startDestRoute
        }

    public actual val startDestDisplayName: String
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        get() {
            if (startDestIdName == null) {
                startDestIdName = startDestinationRoute ?: startDestId.toString()
            }
            return startDestIdName!!
        }

    public override fun toString(): String {
        val sb = StringBuilder()
        sb.append(super.toString())
        val startDestination = findNode(startDestinationRoute) ?: findNode(startDestinationId)
        sb.append(" startDestination=")
        if (startDestination == null) {
            when {
                startDestinationRoute != null -> sb.append(startDestinationRoute)
                startDestIdName != null -> sb.append(startDestIdName)
                else -> sb.append("0x${Integer.toHexString(startDestId)}")
            }
        } else {
            sb.append("{")
            sb.append(startDestination.toString())
            sb.append("}")
        }
        return sb.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is NavGraph) return false
        return super.equals(other) &&
            nodes.size == other.nodes.size &&
            startDestinationId == other.startDestinationId &&
            nodes.valueIterator().asSequence().all { it == other.nodes.get(it.id) }
    }

    override fun hashCode(): Int {
        var result = startDestinationId
        nodes.forEach { key, value ->
            result = 31 * result + key
            result = 31 * result + value.hashCode()
        }
        return result
    }

    public actual companion object {
        @JvmStatic
        public actual fun NavGraph.findStartDestination(): NavDestination = childHierarchy().last()

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public actual fun NavGraph.childHierarchy(): Sequence<NavDestination> =
            generateSequence(this as NavDestination) {
                if (it is NavGraph) {
                    it.findNode(it.startDestinationId)
                } else {
                    null
                }
            }
    }
}

/**
 * Returns the destination with `id`.
 *
 * @throws IllegalArgumentException if no destination is found with that id.
 */
@Suppress("NOTHING_TO_INLINE")
public inline operator fun NavGraph.get(@IdRes id: Int): NavDestination =
    findNode(id) ?: throw IllegalArgumentException("No destination for $id was found in $this")

/** Returns `true` if a destination with `id` is found in this navigation graph. */
public operator fun NavGraph.contains(@IdRes id: Int): Boolean = findNode(id) != null
