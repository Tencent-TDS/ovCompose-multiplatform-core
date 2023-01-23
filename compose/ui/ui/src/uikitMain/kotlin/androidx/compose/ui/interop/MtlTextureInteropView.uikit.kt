/*
 * Copyright 2023 The Android Open Source Project
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
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SomeTexture
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toBackendTexture
import androidx.compose.ui.input.pointer.UIKitInteropModifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.round
import kotlinx.atomicfu.atomic
import kotlinx.cinterop.useContents
import org.jetbrains.skia.GrBackendTexture
import org.jetbrains.skiko.createFromMetalTexture
import platform.CoreGraphics.CGRectMake
import platform.Metal.MTLTextureProtocol
import platform.QuartzCore.CATransaction
import platform.UIKit.UIColor
import platform.UIKit.UIEvent
import platform.UIKit.UITouch
import platform.UIKit.UIView
import platform.UIKit.addSubview
import platform.UIKit.backgroundColor
import platform.UIKit.insertSubview
import platform.UIKit.layoutIfNeeded
import platform.UIKit.removeFromSuperview
import platform.UIKit.setFrame
import platform.UIKit.setNeedsDisplay
import platform.UIKit.setNeedsUpdateConstraints
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@Composable
public fun MtlTextureInteropView(
    background: Color = Color.White,
    factory: () -> MTLTextureProtocol,
    modifier: Modifier = Modifier,
) {
    val componentInfo = remember { ComponentInfo2() }

    val mtlTexture = remember { factory() }
    val root = LocalLayerContainer.current
    val skikoTouchEventHandler = SkikoTouchEventHandler.current
    val backendTextureToImage = SkikoBackendTextureToImage.current
    val density = LocalDensity.current.density
    val focusManager = LocalFocusManager.current
    val focusSwitcher = remember { FocusSwitcher2(componentInfo, focusManager) }
    var rectInPixels by remember { mutableStateOf(IntRect(0, 0, 0, 0)) }

    Box(
        modifier = modifier.onGloballyPositioned { childCoordinates ->
            val coordinates = childCoordinates.parentCoordinates!!
            val newRectInPixels = IntRect(coordinates.localToWindow(Offset.Zero).round(), coordinates.size)
            if (rectInPixels != newRectInPixels) {
                rectInPixels = newRectInPixels
                dispatch_async(dispatch_get_main_queue()) {
                    val rect = rectInPixels / density
                    val cgRect = rect.toCGRect()
                    componentInfo.texture
                }
            }
        }.drawBehind {
            if (false) drawRect(Color.Transparent, blendMode = BlendMode.DstAtop)
            drawIntoCanvas {
                val image = backendTextureToImage(SomeTexture(mtlTexture).toBackendTexture())
                if (image != null) {
                    it.nativeCanvas.drawImage(image, 0f, 0f)
                }
            }
        }.then(UIKitInteropModifier(rectInPixels.width, rectInPixels.height))
    ) {
        focusSwitcher.Content()
    }

    DisposableEffect(factory) {
//        val focusListener = object : FocusListener {
//            override fun focusGained(e: FocusEvent) {
//                if (componentInfo.container.isParentOf(e.oppositeComponent)) {
//                    when (e.cause) {
//                        FocusEvent.Cause.TRAVERSAL_FORWARD -> focusSwitcher.moveForward()
//                        FocusEvent.Cause.TRAVERSAL_BACKWARD -> focusSwitcher.moveBackward()
//                        else -> Unit
//                    }
//                }
//            }
//
//            override fun focusLost(e: FocusEvent) = Unit
//        }
//        root.addFocusListener(focusListener)
        componentInfo.texture = factory()
        onDispose {

        }
    }
    SideEffect {

    }
}

private class FocusSwitcher2(
    private val info: ComponentInfo2,
    private val focusManager: FocusManager
) {
    private val backwardRequester = FocusRequester()
    private val forwardRequester = FocusRequester()
    private var isRequesting = false

    fun moveBackward() {
        try {
            isRequesting = true
            backwardRequester.requestFocus()
        } finally {
            isRequesting = false
        }
        focusManager.moveFocus(FocusDirection.Previous)
    }

    fun moveForward() {
        try {
            isRequesting = true
            forwardRequester.requestFocus()
        } finally {
            isRequesting = false
        }
        focusManager.moveFocus(FocusDirection.Next)
    }

    @Composable
    fun Content() {
        Box(
            Modifier
                .focusRequester(backwardRequester)
                .onFocusChanged {
                    if (it.isFocused && !isRequesting) {
                        focusManager.clearFocus(force = true)

//                        val component = info.container.focusTraversalPolicy.getFirstComponent(info.container)
//                        if (component != null) {
//                            component.requestFocus(FocusEvent.Cause.TRAVERSAL_FORWARD)
//                        } else {
//                            moveForward()
//                        }
                    }
                }
                .focusTarget()
        )
        Box(
            Modifier
                .focusRequester(forwardRequester)
                .onFocusChanged {
                    if (it.isFocused && !isRequesting) {
                        focusManager.clearFocus(force = true)

//                        val component = info.container.focusTraversalPolicy.getLastComponent(info.container)
//                        if (component != null) {
//                            component.requestFocus(FocusEvent.Cause.TRAVERSAL_BACKWARD)
//                        } else {
//                            moveBackward()
//                        }
                    }
                }
                .focusTarget()
        )
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

private fun parseColor(color: Color): UIColor {
    return UIColor(
        red = color.red.toDouble(),
        green = color.green.toDouble(),
        blue = color.blue.toDouble(),
        alpha = color.alpha.toDouble()
    )
}

private class ComponentInfo2 {
    lateinit var texture: MTLTextureProtocol
}
