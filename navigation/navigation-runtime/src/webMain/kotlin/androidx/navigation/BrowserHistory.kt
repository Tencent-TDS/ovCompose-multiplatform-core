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
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraph

expect abstract class BrowserWindow

/**
 * Configures the browser navigation for the given window and navigation controller.
 *
 * @param window an instance of browser's window to be configured
 * @param navController an instance of NavController handling the navigation logic
 */
expect fun configureBrowserNavigation(window: BrowserWindow, navController: NavController)

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