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

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import org.w3c.dom.PopStateEvent
import org.w3c.dom.Window
import org.w3c.dom.events.Event

/**
 * Bind the browser navigation state to the given navigation controller.
 *
 * @param navController an instance of NavController handling the navigation logic
 */
suspend fun Window.bindToNavigation(navController: NavController) {
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

                val routes = stack.filter { it.destination !is NavGraph }
                    .map { it.getRouteWithArgs() ?: return@collect }

                val newUri = "$appAddress/${routes.last()}"
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

private fun Window.popStateEvents(): Flow<PopStateEvent> = callbackFlow {
    val localWindow = this@popStateEvents
    val callback: (Event) -> Unit = { event: Event ->
        if (!isClosedForSend) {
            if (event is PopStateEvent) {
                trySend(event)
            }
        }
    }

    localWindow.addEventListener("popstate", callback)
    awaitClose {
        localWindow.removeEventListener("popstate", callback)
    }
}
