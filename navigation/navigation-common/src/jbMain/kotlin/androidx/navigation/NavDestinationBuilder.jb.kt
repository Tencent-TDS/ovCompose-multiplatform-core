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
import androidx.navigation.serialization.generateHashCode
import androidx.navigation.serialization.generateNavArguments
import androidx.navigation.serialization.generateRoutePattern
import kotlin.jvm.JvmName
import kotlin.jvm.JvmSuppressWildcards
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer

@NavDestinationDsl
public actual open class NavDestinationBuilder<out D : NavDestination>
internal constructor(
    protected actual val navigator: Navigator<out D>,
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val id: Int,
    public actual val route: String?
) {
    public actual constructor(navigator: Navigator<out D>, route: String?) :
        this(navigator, -1, route)

    @OptIn(InternalSerializationApi::class)
    public actual constructor(
        navigator: Navigator<out D>,
        @Suppress("OptionalBuilderConstructorArgument") route: KClass<*>?,
        typeMap: Map<KType, @JvmSuppressWildcards NavType<*>>,
    ) : this(
        navigator,
        route?.serializer()?.generateHashCode() ?: -1,
        route?.serializer()?.generateRoutePattern(typeMap)
    ) {
        route?.apply {
            serializer().generateNavArguments(typeMap).forEach { arguments[it.name] = it.argument }
        }
        this.typeMap = typeMap
    }

    private lateinit var typeMap: Map<KType, NavType<*>>

    public actual var label: CharSequence? = null

    private var arguments = mutableMapOf<String, NavArgument>()

    public actual fun argument(name: String, argumentBuilder: NavArgumentBuilder.() -> Unit) {
        arguments[name] = NavArgumentBuilder().apply(argumentBuilder).build()
    }

    @Suppress("BuilderSetStyle")
    public actual fun argument(name: String, argument: NavArgument) {
        arguments[name] = argument
    }

    private var deepLinks = mutableListOf<NavDeepLink>()

    public actual fun deepLink(uriPattern: String) {
        deepLinks.add(NavDeepLink(uriPattern))
    }

    @JvmName("deepLinkSafeArgs")
    public actual inline fun <reified T : Any> deepLink(
        basePath: String,
    ) {
        deepLink(T::class, basePath) {}
    }

    public actual fun deepLink(navDeepLink: NavDeepLinkDslBuilder.() -> Unit) {
        deepLinks.add(NavDeepLinkDslBuilder().apply(navDeepLink).build())
    }

    @Suppress("BuilderSetStyle")
    public actual inline fun <reified T : Any> deepLink(
        basePath: String,
        noinline navDeepLink: NavDeepLinkDslBuilder.() -> Unit
    ) {
        deepLink(T::class, basePath, navDeepLink)
    }

    @OptIn(InternalSerializationApi::class)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual fun <T : Any> deepLink(
        route: KClass<T>,
        basePath: String,
        navDeepLink: NavDeepLinkDslBuilder.() -> Unit
    ) {
        // make sure they used the safe args constructors which automatically adds
        // argument to the destination
        check(this::typeMap.isInitialized) {
            "Cannot add deeplink from KClass [$route]. Use the NavDestinationBuilder " +
                "constructor that takes a KClass with the same arguments."
        }
        val deepLinkArgs = route.serializer().generateNavArguments(typeMap)
        deepLinkArgs.forEach {
            val arg = arguments[it.name]
            // make sure deep link doesn't contain extra arguments not present in the route KClass
            // and that it doesn't contain different arg type
            require(arg != null && arg.type == it.argument.type) {
                "Cannot add deeplink from KClass [$route]. DeepLink contains unknown argument " +
                    "[${it.name}]. Ensure deeplink arguments matches the destination's " +
                    "route from KClass"
            }
        }
        deepLink(navDeepLink(route, basePath, typeMap, navDeepLink))
    }

    @Suppress("BuilderSetStyle")
    public actual fun deepLink(navDeepLink: NavDeepLink) {
        deepLinks.add(navDeepLink)
    }

    @Suppress("BuilderSetStyle")
    protected actual open fun instantiateDestination(): D = navigator.createDestination()

    public actual open fun build(): D {
        return instantiateDestination().also { destination ->
            destination.label = label
            arguments.forEach { (name, argument) -> destination.addArgument(name, argument) }
            deepLinks.forEach { deepLink -> destination.addDeepLink(deepLink) }
            if (route != null) {
                destination.route = route
            }
            if (id != -1) {
                destination.id = id
            }
        }
    }
}
