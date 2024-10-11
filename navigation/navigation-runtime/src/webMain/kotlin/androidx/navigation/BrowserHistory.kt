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
 * @param getBackStackEntryPath A function that returns the path to show for a given [NavBackStackEntry].
 */
internal suspend fun BrowserWindow.bindToNavigation(
    navController: NavController,
    getBackStackEntryPath: (entry: NavBackStackEntry) -> String
) {
    coroutineScope {
        val localWindow = this@bindToNavigation
        val appAddress = with(localWindow.location) { origin + pathname }.removeSuffix("/")
        var initState = true
        var updateState = true

        launch {
            localWindow.popStateEvents().collect { event ->
                val state = event.state.toString()

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

                //don't handle next navigation calls
                updateState = false

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

                val newUri = appAddress + getBackStackEntryPath(entries.last())
                val state = routes.joinToString("\n")


                if (updateState) {
                    if (initState) {
                        localWindow.history.replaceState(state, "", newUri)
                        initState = false
                    } else {
                        localWindow.history.pushState(state, "", newUri)
                    }
                }
                updateState = true
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

private val argPlaceholder = Regex("""\{*.\}""")
internal fun NavBackStackEntry.getRouteWithArgs(): String? {
    val entry = this
    val route = entry.destination.route ?: return null
    if (!route.contains(argPlaceholder)) return route
    val args = entry.arguments ?: Bundle()
    val nameToValue = entry.destination.arguments.map { (name, arg) ->
        val serializedTypeValue = arg.type.serializeAsValue(arg.type[args, name])
        name to serializedTypeValue
    }

    val routeWithFilledArgs =
        nameToValue.fold(initial = route) { acc, (argumentName: String, value: String) ->
            acc.replace("{$argumentName}", value)
        }
    return routeWithFilledArgs.takeIf { !it.contains(argPlaceholder) }
}

internal external interface BrowserLocation {
    val origin: String
    val pathname: String
}

internal external interface BrowserHistory {
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