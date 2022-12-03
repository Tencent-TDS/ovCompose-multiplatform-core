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

package androidx.compose.ui.interop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.round
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIColor
import platform.UIKit.UIView
import platform.UIKit.addSubview
import platform.UIKit.backgroundColor
import platform.UIKit.removeFromSuperview
import platform.UIKit.setBounds
import platform.UIKit.setFrame
import platform.UIKit.setNeedsDisplay
import platform.UIKit.setNeedsUpdateConstraints

@Composable
public fun <T : UIView> UIKitInteropView(
    modifier: Modifier = Modifier,
    factory: () -> T,
//    background: Color = Color.White,
) {
    val componentInfo = remember { ComponentInfo() }

    val root = LocalLayerContainer.current
    val density = LocalDensity.current.density
//    val focusSwitcher = remember { FocusSwitcher(componentInfo, focusManager) }

    Box(
        modifier = modifier.onGloballyPositioned { childCoordinates ->
            val coordinates = childCoordinates.parentCoordinates!!
            val location: IntOffset = coordinates.localToWindow(Offset.Zero).round()
            val size = coordinates.size
            val rect = IntRect(location, size) / density
            componentInfo.container.setFrame(rect.toCGRect())
            componentInfo.container.setNeedsDisplay()
            componentInfo.container.setNeedsUpdateConstraints()
//            componentInfo.container.validate()
//            componentInfo.container.repaint()
        }
    ) {
//        focusSwitcher.Content()
    }

    DisposableEffect(factory) {
        componentInfo.container = UIView().apply {
//            layout = BorderLayout(0, 0)
        }
        root.addSubview(componentInfo.container)
        onDispose {
            componentInfo.container.removeFromSuperview()
        }
    }

    SideEffect {
        componentInfo.container.backgroundColor = UIColor.yellowColor()
    }
}

@Composable
private fun Box(modifier: Modifier, content: @Composable () -> Unit = {}) {
    Layout(
        content = content,
        modifier = modifier,
        measurePolicy = { measurables, constraints ->
            val placeables = measurables.map { it.measure(constraints) }
            layout(
                placeables.maxOfOrNull { it.width } ?: 0,
                placeables.maxOfOrNull { it.height } ?: 0
            ) {
                placeables.forEach {
                    it.place(0, 0)
                }
            }
        }
    )
}

private class ComponentInfo {
    lateinit var container: UIView
}


