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

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.w3c.dom.PopStateEvent
import org.w3c.dom.Window
import org.w3c.dom.events.Event

actual typealias BrowserWindow = Window

actual fun configureBrowserNavigation(window: Window, navController: NavController) {
    var initState = true
    var updateState = true

    window.addEventListener("popstate", { event: Event ->
        if (event is PopStateEvent) { //back or forward in the browser
            val state = event.state?.unsafeCast<JsString>().toString()

            val restoredRoutes = state.lines()
            val currentBackStack = navController.currentBackStack.value

            //don't handle next navigation calls
            updateState = false

            //clear current stack
            currentBackStack.firstOrNull { it.destination !is NavGraph }?.let { root ->
                root.destination.route?.let { navController.popBackStack(it, true) }
            }
            //restore stack
            restoredRoutes.forEach { route -> navController.navigate(route) }
        }
    })

    //global listener is fine here
    GlobalScope.launch {
        navController.currentBackStack.collect { stack ->
            val routes = stack.filter { it.destination !is NavGraph }
                .map { it.getRouteWithArgs() ?: return@collect }

            val newUri = window.location.run { "$protocol//$host/${routes.last()}" }
            val state = routes.joinToString("\n")

            if (updateState) {
                if (initState) {
                    window.history.replaceState(state.toJsString(), "", newUri)
                    initState = false
                } else {
                    window.history.pushState(state.toJsString(), "", newUri)
                }
            }
            updateState = true
        }
    }
}