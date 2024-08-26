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

package androidx.compose.mpp.demo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import platform.MapKit.MKMapView

@OptIn(ExperimentalComposeUiApi::class)
val UpdatableInteropPropertiesExample = Screen.Example("Updatable interop properties") {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(100) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                var interactionMode by remember { mutableStateOf(0) }

                UIKitView(
                    factory = { MKMapView() },
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    properties = UIKitInteropProperties(
                        interactionMode = when (interactionMode) {
                            1 -> UIKitInteropInteractionMode.Cooperative()
                            2 -> UIKitInteropInteractionMode.NonCooperative
                            else -> null
                        }
                    ),
                    onReset = { }
                )

                var isDropdownExpanded by remember { mutableStateOf(false) }

                Button(
                    onClick = { isDropdownExpanded = true }
                ) {
                    Text("Change interaction mode")
                }

                DropdownMenu(
                    expanded = isDropdownExpanded,
                    onDismissRequest = { isDropdownExpanded = false }
                ) {
                    DropdownMenuItem(onClick = { interactionMode = 0 }) {
                        Text("Non-interactive")
                    }
                    DropdownMenuItem(onClick = { interactionMode = 1 }) {
                        Text("Cooperative")
                    }
                    DropdownMenuItem(onClick = { interactionMode = 2 }) {
                        Text("Non-cooperative")
                    }
                }
            }
        }
    }
}