/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.desktop.examples.bug

import androidx.compose.material.Button
import androidx.compose.material.ClassInMaterialModule
import androidx.compose.material.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.*
import androidx.compose.ui.window.ClassInModuleComposeUiUi
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() {
    application {
        Window(onCloseRequest = ::exitApplication) {
            var reproduceBug by remember { mutableStateOf(false) } // Set to true if you want to reproduce the bug on application startup
            if (reproduceBug) {
                ClassInAppModule().composableFun() // works good
                ClassInMaterialModule().composableFun() // works good
                ClassInModuleComposeUiUi().composableFun() // reproduce bug
            } else {
                Button(onClick = { reproduceBug = true }) {
                    Text("Reproduce bug")
                }
            }
        }
    }
}
