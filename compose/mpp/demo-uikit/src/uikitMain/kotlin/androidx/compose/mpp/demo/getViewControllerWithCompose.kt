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

package androidx.compose.mpp.demo

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.window.Application

fun getViewControllerWithCompose() = Application("Compose/Native sample") {
    val textState1 = remember { mutableStateOf("sync text state") }
    val counter = remember { mutableStateOf(0) }
    LazyColumn {
        items(3) {
            Stub()
        }
        item {
            Box(Modifier.size(200.dp, 100.dp)) {

                // Usage:
                ComposeUITextField(Modifier.fillMaxSize(), textState1.value, onValueChange = { textState1.value = it })

                // Compose Button displays over UIKit view
                Button(onClick = { counter.value++ }, Modifier.align(Alignment.BottomCenter)) {
                    Text("Click ${counter.value}")
                }
            }
        }
        item {
            TextField(value = textState1.value, onValueChange = { textState1.value = it })
        }
        items(10) {
            Stub()
        }
    }
}

@Composable
internal fun Stub() {
    Box(Modifier.size(100.dp).background(Color.Gray).padding(10.dp))
}
