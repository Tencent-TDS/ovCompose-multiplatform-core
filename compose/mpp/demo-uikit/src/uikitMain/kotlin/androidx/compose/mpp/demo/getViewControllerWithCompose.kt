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
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Application

fun getViewControllerWithCompose() = Application("Compose/Native sample") {
    var textState1 by remember { mutableStateOf("qwe") }
    var textState2 by remember { mutableStateOf("text field 2") }
    var textFromClipboard by remember { mutableStateOf("click for clipboard") }
    val clipboard = LocalClipboardManager.current
    Column {
        Text(".")
        Text(".")
        Text(".")
        Text(".")
        Text(".")
        Text(".")
        Text(".")
        Text(".")
        Text("Hello, UIKit")
        TextField(value = textState1, onValueChange = {
//            Exception().printStackTrace()
            textState1 = it
//            textState1 = it + "-"
        })
        LaunchedEffect(Unit) {
            while(false) {
                repeat(11) {
                    kotlinx.coroutines.delay(1_000)
                    print("$it ")
                }
                println()
                textState1 = "0"
            }
        }
        TextField(value = textState2, onValueChange = {
            textState2 = it
        })
        Image(
            modifier = Modifier.pointerInput(Unit) {
                this.awaitPointerEventScope {  }
            },
            painter = object : Painter() {
                override val intrinsicSize: Size = Size(16f, 16f)
                override fun DrawScope.onDraw() {
                    drawRect(color = Color.Blue)
                }
            },
            contentDescription = "image sample"
        )
        Button(
            modifier = Modifier.padding(16.dp),
            onClick = {
                textFromClipboard = clipboard.getText()?.text ?: "clipboard is empty"
            },
            colors = ButtonDefaults.buttonColors(backgroundColor = Color.Gray)
        ) {
            Text(textFromClipboard)
        }
    }
}
