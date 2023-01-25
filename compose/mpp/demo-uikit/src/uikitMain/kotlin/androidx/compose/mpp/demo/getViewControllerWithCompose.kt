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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Slider
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.useContents
import kotlinx.coroutines.delay
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSNotification
import platform.Foundation.NSSelectorFromString
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.UIKit.UIControlEventEditingChanged
import platform.Foundation.NSValue
import platform.MapKit.MKMapView
import platform.Metal.MTLTextureProtocol
import platform.UIKit.CGRectValue
import platform.UIKit.UIButton
import platform.UIKit.UIColor
import platform.UIKit.UIScreen
import platform.UIKit.UISwitch
import platform.UIKit.UITextField
import platform.UIKit.UIView
import platform.UIKit.addSubview
import platform.UIKit.backgroundColor
import platform.UIKit.setClipsToBounds
import platform.UIKit.setNeedsUpdateConstraints
import platform.WebKit.WKWebView
import platform.darwin.NSObject

val BACKGROUND_COLOR = Color.LightGray

fun getViewControllerWithCompose() = Application("Compose/Native sample") {
    val textState1 = remember { mutableStateOf("sync text state") }
    val counter = remember { mutableStateOf(0) }
    if(false) Popup(object : PopupPositionProvider {
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
        items(6) {
            Stub()
        }
        Example("UISwitch") {
            UIKitInteropView(modifier = Modifier.size(70.dp, 50.dp), factory = { UISwitch() })
        }
        Example("UITextField with shared state") {
            ComposeUITextField(Modifier.fillMaxWidth().height(50.dp), textState1.value, onValueChange = { textState1.value = it })
            TextField(value = textState1.value, onValueChange = { textState1.value = it })
        }
        Example("WebView") {
            UIKitInteropView(modifier = Modifier.size(300.dp, 400.dp), factory = {
                val wkWebView = WKWebView(frame = CGRectMake(0.0, 0.0, 300.0, 400.0))
                wkWebView.loadRequest(NSURLRequest.requestWithURL(NSURL.URLWithString("https://kotlinlang.org")!!))
                wkWebView
            })
        }
        Example("MapView") {
            UIKitInteropView(modifier = Modifier.size(300.dp, 300.dp), factory = {
                val mapView = MKMapView(frame = CGRectMake(0.0, 0.0, 300.0, 300.0))
                mapView
            })
        }
        Example("Modifiers") {
            var alpha by remember { mutableStateOf(1f) }
            var corner by remember { mutableStateOf(0f) }
            var rotate by remember { mutableStateOf(0f) }
            UIKitInteropView(
                modifier = Modifier.size(300.dp, 300.dp)
                    .alpha(alpha)
                    .clip(RoundedCornerShape(size = corner.dp))
                    .rotate(rotate),
                factory = {
                    val mapView = MKMapView(frame = CGRectMake(0.0, 0.0, 300.0, 300.0))
                    mapView
            })
            Row {
                Text("Alpha")
                Slider(alpha, onValueChange = {alpha = it}, Modifier.fillMaxWidth())
            }
            Row {
                Text("Corner")
                Slider(corner, onValueChange = {corner = it}, Modifier.fillMaxWidth(), valueRange = 0f..150f)
            }
            Row {
                Text("Rotate")
                Slider(rotate, onValueChange = {rotate = it}, Modifier.fillMaxWidth(), valueRange = 0f..360f)
            }
        }
        Example("Todo") {
            Box(Modifier.size(200.dp, 200.dp)) {
                UIKitInteropView(modifier = Modifier.fillMaxSize(), factory = {
                    UISwitch(CGRectMake(0.0, 0.0, 100.0, 100.0))
                })
//                Button(onClick = { counter.value++ }, Modifier.align(Alignment.BottomCenter)) {
//                    Text("Click ${counter.value}")
//                }
            }
        }
        items(10) {
            Stub()
        }
    }
}

internal fun LazyListScope.Example(title: String, content: @Composable () -> Unit) {
    item {
        Column(Modifier.fillMaxWidth().border(width = 1.dp, color = Color.Black).padding(10.dp)) {
            Text(title)
            Spacer(Modifier.size(10.dp))
            content()
        }
    }
    item {
        Stub()
    }
}

@Composable
internal fun Stub() {
    Box(Modifier.fillMaxWidth().height(50.dp).padding(10.dp))
}
