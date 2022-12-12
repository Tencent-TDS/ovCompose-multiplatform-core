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
import androidx.compose.ui.input.pointer.UIKitInteropModifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.round
import kotlinx.atomicfu.atomic
import platform.CoreGraphics.CGRectMake
import platform.QuartzCore.CATransaction
import platform.UIKit.UIColor
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

val NoOpUpdate: UIView.() -> Unit = {}

/**
 * TODO doc
 */
@Composable
public fun <T : UIView> UIKitInteropView(
    background: Color = Color.White,
    factory: () -> T,
    modifier: Modifier = Modifier,
    update: (T) -> Unit = NoOpUpdate,
    dispose: (T) -> Unit = {}
) {
    val componentInfo = remember { ComponentInfo<T>() }

    val root = LocalLayerContainer.current
    val density = LocalDensity.current.density
    val focusManager = LocalFocusManager.current
    val focusSwitcher = remember { FocusSwitcher(componentInfo, focusManager) }
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
                    CATransaction.begin()
                    componentInfo.container.setFrame(cgRect)
                    componentInfo.component.setFrame(CGRectMake(0.0, 0.0, rect.width.toDouble(), rect.height.toDouble()))
                    CATransaction.commit()
                    componentInfo.component.layoutIfNeeded()
                    componentInfo.component.setNeedsDisplay()
                    componentInfo.component.setNeedsUpdateConstraints()
                    componentInfo.component.resignFirstResponder()
                }
            }
        }.drawBehind {
            drawRect(Color.Transparent, blendMode = BlendMode.DstAtop)
        }.then(UIKitInteropModifier(rectInPixels.width, rectInPixels.height))
    ) {
        focusSwitcher.Content()
    }

    DisposableEffect(factory) {
        componentInfo.component = factory()
        componentInfo.container = UIView(CGRectMake(0.0, 0.0, 0.0, 0.0)).apply {
            addSubview(componentInfo.component)
        }
        componentInfo.updater = Updater(componentInfo.component, update)
        root.insertSubview(componentInfo.container, 0)
        onDispose {
            componentInfo.container.removeFromSuperview()
            componentInfo.updater.dispose()
            dispose(componentInfo.component)
        }
    }
    SideEffect {
        componentInfo.container.backgroundColor = parseColor(background)
        componentInfo.updater.update = update
    }
}

private class FocusSwitcher<T : UIView>(
    private val info: ComponentInfo<T>,
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
                          // TODO Copied from Desktop:
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
                          // TODO Copied from Desktop:
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

private class ComponentInfo<T : UIView> {
    lateinit var container: UIView
    lateinit var component: T
    lateinit var updater: Updater<T>
}

private class Updater<T : UIView>(
    private val component: T,
    update: (T) -> Unit
) {
    private var isDisposed = false
    private val isUpdateScheduled = atomic(false)
    private val snapshotObserver = SnapshotStateObserver { command ->
        command()
    }

    private val scheduleUpdate = { _: T ->
        if (!isUpdateScheduled.getAndSet(true)) {
            dispatch_async(dispatch_get_main_queue()) {
                isUpdateScheduled.value = false
                if (!isDisposed) {
                    performUpdate()
                }
            }
        }
    }

    var update: (T) -> Unit = update
        set(value) {
            if (field != value) {
                field = value
                performUpdate()
            }
        }

    private fun performUpdate() {
        // don't replace scheduleUpdate by lambda reference,
        // scheduleUpdate should always be the same instance
        snapshotObserver.observeReads(component, scheduleUpdate) {
            update(component)
        }
    }

    init {
        snapshotObserver.start()
        performUpdate()
    }

    fun dispose() {
        snapshotObserver.stop()
        snapshotObserver.clear()
        isDisposed = true
    }
}
