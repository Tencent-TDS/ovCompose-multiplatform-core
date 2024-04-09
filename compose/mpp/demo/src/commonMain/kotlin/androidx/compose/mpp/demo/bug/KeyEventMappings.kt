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

package androidx.compose.mpp.demo.bug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.mpp.demo.Screen
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp


// https://github.com/JetBrains/compose-multiplatform/issues/3644
val KeyEventMappingsBugReproducer = Screen.Example("KeyEventMappingsBugReproducer (web)") {
    val focusRequester = remember { FocusRequester() }
    Box(Modifier.size(1000.dp).background(Color.Red).focusRequester(focusRequester).focusTarget().onKeyEvent {
        println("" + it.key + " " + it.type)
        false
    }) {
        Text("Try to press different keys and look at the console...")
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}