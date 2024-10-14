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

import androidx.core.bundle.Bundle
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

/**
 * Binds the browser window state to the given navigation controller.
 *
 * @param navController The [NavController] instance to bind to browser window navigation.
 * @param getBackStackEntryRoute A function that returns the route to show for a given [NavBackStackEntry].
 */
internal suspend fun BrowserWindow.bindToNavigation(
    navController: NavController,
    getBackStackEntryRoute: (entry: NavBackStackEntry) -> String
) {
    coroutineScope {
        val localWindow = this@bindToNavigation
        val appAddress = with(localWindow.location) { origin + pathname }

        launch {
            localWindow.popStateEvents().collect { event ->
                val state = event.state

                if (state == null) {
                    //if user manually put a new address or open a new page, then there is no state
                    return@collect
                }

                val restoredRoutes = state.lines()
                val currentBackStack = navController.currentBackStack.value
                val currentRoutes = currentBackStack.filter { it.destination !is NavGraph }
                    .mapNotNull { it.getRouteWithArgs() }

                var commonTail = -1
                restoredRoutes.forEachIndexed { index, restoredRoute ->
                    if (index >= currentRoutes.size) {
                        return@forEachIndexed
                    }
                    if (restoredRoute == currentRoutes[index]) {
                        commonTail = index
                    }
                }

                if (commonTail == -1) {
                    //clear full stack
                    currentRoutes.firstOrNull()?.let { root ->
                        navController.popBackStack(root, true)
                    }
                } else {
                    currentRoutes[commonTail].let { lastCommon ->
                        navController.popBackStack(lastCommon, false)
                    }
                }

                //restore stack
                if (commonTail < restoredRoutes.size - 1) {
                    val newRoutes = restoredRoutes.subList(commonTail + 1, restoredRoutes.size)
                    newRoutes.forEach { route -> navController.navigate(route) }
                }
            }
        }

        launch {
            navController.currentBackStack.collect { stack ->
                if (stack.isEmpty()) return@collect

                val entries = stack.filter { it.destination !is NavGraph }
                if (entries.isEmpty()) return@collect
                val routes = entries.map { it.getRouteWithArgs() ?: return@collect }

                val newUri = appAddress + getBackStackEntryRoute(entries.last())
                val state = routes.joinToString("\n")

                val currentState = localWindow.history.state
                when (currentState) {
                    null -> {
                        //user manually put a new address or open a new page,
                        // we need to save the current state in the browser history
                        localWindow.history.replaceState(state, "", newUri)
                    }

                    state -> {
                        //this was a restoration of the state (back/forward browser navigation)
                        //the callback came from the popStateEvents
                        //the browser state is equal the app state, but we need to update shown uri
                        localWindow.history.replaceState(state, "", newUri)
                    }

                    else -> {
                        //the navigation happened in the compose app,
                        // we need to push the new state to the browser history
                        localWindow.history.pushState(state, "", newUri)
                    }
                }
            }
        }
    }
}

@OptIn(DelicateCoroutinesApi::class)
@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
private fun BrowserWindow.popStateEvents(): Flow<BrowserPopStateEvent> = callbackFlow {
    val localWindow = this@popStateEvents
    val callback: (BrowserEvent) -> Unit = { event: BrowserEvent ->
        if (!isClosedForSend) {
            (event as? BrowserPopStateEvent)?.let { trySend(it) }
        }
    }

    localWindow.addEventListener("popstate", callback)
    awaitClose {
        localWindow.removeEventListener("popstate", callback)
    }
}

private val argPlaceholder = Regex("""\{.*?\}""")
internal fun NavBackStackEntry.getRouteWithArgs(): String? {
    val entry = this
    val route = entry.destination.route ?: return null
    if (!route.contains(argPlaceholder)) return route
    val args = entry.arguments ?: Bundle()
    val nameToTypedValue = entry.destination.arguments.mapValues { (name, arg) ->
        arg.type.serializeAsValue(arg.type[args, name])
    }

    val routeWithFilledArgs = route.replace(argPlaceholder) { match ->
        val key = match.value.trim('{', '}')
        nameToTypedValue[key] ?: if (args.containsKey(key)) {
            //untyped args stored as strings
            //see: androidx.navigation.NavDeepLink.parseArgument
            args.getString(key)!!
        } else ""
    }

    return routeWithFilledArgs
}

internal external interface BrowserLocation {
    val origin: String
    val pathname: String
}

internal external interface BrowserHistory {
    val state: String?
    fun pushState(data: String?, title: String, url: String?)
    fun replaceState(data: String?, title: String, url: String?)
}

internal external interface BrowserEvent
internal external interface BrowserPopStateEvent : BrowserEvent {
    val state: String?
}

internal external interface BrowserEventTarget {
    fun addEventListener(type: String, callback: ((BrowserEvent) -> Unit)?)
    fun removeEventListener(type: String, callback: ((BrowserEvent) -> Unit)?)
}

internal external interface BrowserWindow : BrowserEventTarget {
    val location: BrowserLocation
    val history: BrowserHistory
}