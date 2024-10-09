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

import org.w3c.dom.Window

/**
 * Binds the browser window state to the given navigation controller.
 *
 * @param navController The [NavController] instance to bind to browser window navigation.
 * @param getBackStackEntryPath An optional function that returns the path to show for a given [NavBackStackEntry].
 */
internal suspend fun Window.bindToNavigation(
    navController: NavController,
    getBackStackEntryPath: (entry: NavBackStackEntry) -> String = {
        "/${it.getRouteWithArgs().orEmpty()}"
    }
) {
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    (this as BrowserWindow).bindToNavigation(navController, getBackStackEntryPath)
}
