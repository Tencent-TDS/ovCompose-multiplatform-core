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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.interop.UIKitInteropView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Application
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import kotlinx.coroutines.delay
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIButton
import platform.UIKit.UIColor
import platform.UIKit.UITextField
import platform.UIKit.UIView
import platform.UIKit.addSubview
import platform.UIKit.backgroundColor

fun getViewControllerWithCompose() = Application("Compose/Native sample") {
    val textState1 = remember { mutableStateOf("text field 1") }
    val textState2 = remember { mutableStateOf("text field 2") }
    Popup(object : PopupPositionProvider {
        override fun calculatePosition(
            anchorBounds: IntRect,
            windowSize: IntSize,
            layoutDirection: LayoutDirection,
            popupContentSize: IntSize
        ): IntOffset = IntOffset(50, 50)
    }) {
        val shape = RoundedCornerShape(10.dp)
        Box(
            Modifier.size(150.dp).clip(shape).background(Color.LightGray).border(2.dp, color = Color.Black, shape),
            contentAlignment = Alignment.Center,
        ) {
            Text("Popup")
        }
    }
    LazyColumn {
        items(2) {
            Stub()
        }
        item {
            Box(Modifier.size(200.dp, 200.dp)) {
                UIKitInteropView(
                    background = Color.Green,
                    modifier = Modifier.size(200.dp, 200.dp),
                    factory = {
                        UITextField(CGRectMake(0.0, 0.0, 300.0, 100.0)).apply {
                            text = "UITextField"
                        }
                    }
                )
//                Box(Modifier.size(100.dp, 100.dp).background(Color.Black))
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
