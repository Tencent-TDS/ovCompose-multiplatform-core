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
import androidx.navigation.NavDestination.Companion.createRoute
import androidx.navigation.serialization.generateHashCode
import kotlin.jvm.JvmOverloads
import kotlin.reflect.KClass
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer

public actual class NavOptions
internal constructor(
    private val singleTop: Boolean,
    private val restoreState: Boolean,
    /**
     * The destination to pop up to before navigating. When set, all non-matching destinations
     * should be popped from the back stack.
     *
     * @return the destinationId to pop up to, clearing all intervening destinations
     * @see Builder.setPopUpTo
     * @see isPopUpToInclusive
     * @see shouldPopUpToSaveState
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val popUpToId: Int,
    private val popUpToInclusive: Boolean,
    private val popUpToSaveState: Boolean,
) {
    public actual var popUpToRoute: String? = null
        private set

    public actual var popUpToRouteClass: KClass<*>? = null
        private set

    public actual var popUpToRouteObject: Any? = null
        private set

    /** NavOptions stores special options for navigate actions */
    internal constructor(
        singleTop: Boolean,
        restoreState: Boolean,
        popUpToRoute: String?,
        popUpToInclusive: Boolean,
        popUpToSaveState: Boolean,
    ) : this(
        singleTop,
        restoreState,
        createRoute(popUpToRoute).hashCode(),
        popUpToInclusive,
        popUpToSaveState
    ) {
        this.popUpToRoute = popUpToRoute
    }

    /** NavOptions stores special options for navigate actions */
    @OptIn(InternalSerializationApi::class)
    internal constructor(
        singleTop: Boolean,
        restoreState: Boolean,
        popUpToRouteClass: KClass<*>?,
        popUpToInclusive: Boolean,
        popUpToSaveState: Boolean,
    ) : this(
        singleTop,
        restoreState,
        popUpToRouteClass!!.serializer().generateHashCode(),
        popUpToInclusive,
        popUpToSaveState
    ) {
        this.popUpToRouteClass = popUpToRouteClass
    }

    /** NavOptions stores special options for navigate actions */
    @OptIn(InternalSerializationApi::class)
    internal constructor(
        singleTop: Boolean,
        restoreState: Boolean,
        popUpToRouteObject: Any,
        popUpToInclusive: Boolean,
        popUpToSaveState: Boolean,
    ) : this(
        singleTop,
        restoreState,
        popUpToRouteObject::class.serializer().generateHashCode(),
        popUpToInclusive,
        popUpToSaveState
    ) {
        this.popUpToRouteObject = popUpToRouteObject
    }

    public actual fun shouldLaunchSingleTop(): Boolean {
        return singleTop
    }

    public actual fun shouldRestoreState(): Boolean {
        return restoreState
    }

    public actual fun isPopUpToInclusive(): Boolean {
        return popUpToInclusive
    }

    public actual fun shouldPopUpToSaveState(): Boolean {
        return popUpToSaveState
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is NavOptions) return false
        return singleTop == other.singleTop &&
            restoreState == other.restoreState &&
            popUpToId == other.popUpToId &&
            popUpToRoute == other.popUpToRoute &&
            popUpToRouteClass == other.popUpToRouteClass &&
            popUpToRouteObject == other.popUpToRouteObject &&
            popUpToInclusive == other.popUpToInclusive &&
            popUpToSaveState == other.popUpToSaveState
    }

    override fun hashCode(): Int {
        var result = if (shouldLaunchSingleTop()) 1 else 0
        result = 31 * result + if (shouldRestoreState()) 1 else 0
        result = 31 * result + popUpToId
        result = 31 * result + popUpToRoute.hashCode()
        result = 31 * result + popUpToRouteClass.hashCode()
        result = 31 * result + popUpToRouteObject.hashCode()
        result = 31 * result + if (isPopUpToInclusive()) 1 else 0
        result = 31 * result + if (shouldPopUpToSaveState()) 1 else 0
        return result
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString(): String {
        val sb = StringBuilder()
        sb.append(this::class.simpleName)
        sb.append("(")
        if (singleTop) {
            sb.append("launchSingleTop ")
        }
        if (restoreState) {
            sb.append("restoreState ")
        }
        if (popUpToRoute != null || popUpToId != -1)
            if (popUpToRoute != null) {
                sb.append("popUpTo(")
                if (popUpToRoute != null) {
                    sb.append(popUpToRoute)
                } else if (popUpToRouteClass != null) {
                    sb.append(popUpToRouteClass)
                } else if (popUpToRouteObject != null) {
                    sb.append(popUpToRouteObject)
                } else {
                    sb.append("0x")
                    sb.append(popUpToId.toHexString())
                }
                if (popUpToInclusive) {
                    sb.append(" inclusive")
                }
                if (popUpToSaveState) {
                    sb.append(" saveState")
                }
                sb.append(")")
            }
        return sb.toString()
    }

    public actual class Builder {
        private var singleTop = false
        private var restoreState = false

        private var popUpToId = -1
        private var popUpToRoute: String? = null
        private var popUpToRouteClass: KClass<*>? = null
        private var popUpToRouteObject: Any? = null
        private var popUpToInclusive = false
        private var popUpToSaveState = false

        public actual fun setLaunchSingleTop(singleTop: Boolean): Builder {
            this.singleTop = singleTop
            return this
        }

        public actual fun setRestoreState(restoreState: Boolean): Builder {
            this.restoreState = restoreState
            return this
        }

        /**
         * Pop up to a given destination before navigating. This pops all non-matching destinations
         * from the back stack until this destination is found.
         *
         * @param destinationId The destination to pop up to, clearing all intervening destinations.
         * @param inclusive true to also pop the given destination from the back stack.
         * @param saveState true if the back stack and the state of all destinations between the
         *   current destination and [destinationId] should be saved for later restoration via
         *   [setRestoreState] or the `restoreState` attribute using the same ID as [popUpToId]
         *   (note: this matching ID is true whether [inclusive] is true or false).
         * @return this Builder
         * @see NavOptions.popUpToId
         * @see NavOptions.isPopUpToInclusive
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @JvmOverloads
        public fun setPopUpTo(
            destinationId: Int,
            inclusive: Boolean,
            saveState: Boolean = false
        ): Builder {
            popUpToId = destinationId
            popUpToRoute = null
            popUpToInclusive = inclusive
            popUpToSaveState = saveState
            return this
        }

        @JvmOverloads
        public actual fun setPopUpTo(
            route: String?,
            inclusive: Boolean,
            saveState: Boolean
        ): Builder {
            popUpToRoute = route
            popUpToId = -1
            popUpToInclusive = inclusive
            popUpToSaveState = saveState
            return this
        }

        @JvmOverloads
        @Suppress("MissingGetterMatchingBuilder") // no need for getter
        public actual inline fun <reified T : Any> setPopUpTo(
            inclusive: Boolean,
            saveState: Boolean
        ): Builder {
            setPopUpTo(T::class, inclusive, saveState)
            return this
        }

        @JvmOverloads
        public actual fun <T : Any> setPopUpTo(
            route: KClass<T>,
            inclusive: Boolean,
            saveState: Boolean
        ): Builder {
            popUpToRouteClass = route
            popUpToId = -1
            popUpToInclusive = inclusive
            popUpToSaveState = saveState
            return this
        }

        @JvmOverloads
        @Suppress("MissingGetterMatchingBuilder")
        @OptIn(InternalSerializationApi::class)
        public actual fun <T : Any> setPopUpTo(
            route: T,
            inclusive: Boolean,
            saveState: Boolean
        ): Builder {
            popUpToRouteObject = route
            setPopUpTo(route::class.serializer().generateHashCode(), inclusive, saveState)
            return this
        }

        public actual fun build(): NavOptions {
            return if (popUpToRoute != null) {
                NavOptions(
                    singleTop,
                    restoreState,
                    popUpToRoute,
                    popUpToInclusive,
                    popUpToSaveState,
                )
            } else if (popUpToRouteClass != null) {
                NavOptions(
                    singleTop,
                    restoreState,
                    popUpToRouteClass,
                    popUpToInclusive,
                    popUpToSaveState,
                )
            } else if (popUpToRouteObject != null) {
                NavOptions(
                    singleTop,
                    restoreState,
                    popUpToRouteObject!!,
                    popUpToInclusive,
                    popUpToSaveState,
                )
            } else {
                NavOptions(
                    singleTop,
                    restoreState,
                    popUpToId,
                    popUpToInclusive,
                    popUpToSaveState,
                )
            }
        }
    }
}
