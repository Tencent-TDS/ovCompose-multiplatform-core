/*
 * Copyright 2025 The Android Open Source Project
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

@file:JvmName("NavHostControllerKt")
@file:JvmMultifileClass

package androidx.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.Navigator
import androidx.savedstate.SavedState
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

@Composable
public actual fun rememberNavController(
    vararg navigators: Navigator<out NavDestination>
): NavHostController {
    return rememberSaveable(inputs = navigators, saver = NavControllerSaver()) {
            createNavController()
        }
        .apply {
            for (navigator in navigators) {
                navigatorProvider.addNavigator(navigator)
            }
        }
}

private fun createNavController() =
    NavHostController().apply {
        navigatorProvider.addNavigator(ComposeNavGraphNavigator(navigatorProvider))
        navigatorProvider.addNavigator(ComposeNavigator())
        navigatorProvider.addNavigator(DialogNavigator())
    }

/** Saver to save and restore the NavController across config change and process death. */
private fun NavControllerSaver(): Saver<NavHostController, *> =
    Saver<NavHostController, SavedState>(
        save = { it.saveState() },
        restore = { createNavController().apply { restoreState(it) } }
    )
