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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.round
import cnames.structs.CGContext
import kotlin.coroutines.CoroutineContext
import kotlin.native.concurrent.AtomicNativePtr
import kotlin.native.internal.NativePtr
import kotlin.random.Random
import kotlin.system.getTimeNanos
import kotlinx.atomicfu.atomic
import kotlinx.cinterop.COpaque
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.COpaquePointerVar
import kotlinx.cinterop.CPointed
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVarOf
import kotlinx.cinterop.CValue
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.UByteVarOf
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.cValue
import kotlinx.cinterop.free
import kotlinx.cinterop.interpretCPointer
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.objcPtr
import kotlinx.cinterop.pin
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toCPointer
import kotlinx.cinterop.useContents
import kotlinx.cinterop.value
import kotlinx.coroutines.CloseableCoroutineDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.jetbrains.skia.GrBackendTexture
import org.jetbrains.skia.Image
import org.jetbrains.skiko.SkikoTouchEvent
import org.jetbrains.skiko.SkikoTouchEventKind
import org.jetbrains.skiko.createFromMetalTexture
import platform.CoreGraphics.CGAffineTransformConcat
import platform.CoreGraphics.CGAffineTransformIdentity
import platform.CoreGraphics.CGAffineTransformMakeTranslation
import platform.CoreGraphics.CGAffineTransformScale
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGBitmapContextGetAlphaInfo
import platform.CoreGraphics.CGBitmapContextGetBytesPerRow
import platform.CoreGraphics.CGBitmapContextGetData
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGContextClearRect
import platform.CoreGraphics.CGContextScaleCTM
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGLayerGetContext
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSRunLoop
import platform.Foundation.NSSelectorFromString
import platform.Metal.MTLBufferProtocol
import platform.Metal.MTLCreateSystemDefaultDevice
import platform.Metal.MTLDeviceProtocol
import platform.Metal.MTLPixelFormatRGBA8Unorm
import platform.Metal.MTLRegion
import platform.Metal.MTLRegionMake2D
import platform.Metal.MTLResourceStorageModePrivate
import platform.Metal.MTLResourceStorageModeShared
import platform.Metal.MTLTextureDescriptor
import platform.Metal.MTLTextureProtocol
import platform.Metal.MTLTextureUsageShaderRead
import platform.QuartzCore.CADisplayLink
import platform.QuartzCore.CATransaction
import platform.QuartzCore.CATransform3DConcat
import platform.QuartzCore.CATransform3DMakeScale
import platform.QuartzCore.CATransform3DMakeTranslation
import platform.UIKit.UIColor
import platform.UIKit.UIEvent
import platform.UIKit.UIGraphicsBeginImageContext
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIGraphicsPopContext
import platform.UIKit.UIGraphicsPushContext
import platform.UIKit.UITouch
import platform.UIKit.UIView
import platform.UIKit.addSubview
import platform.UIKit.backgroundColor
import platform.UIKit.drawViewHierarchyInRect
import platform.UIKit.insertSubview
import platform.UIKit.layoutIfNeeded
import platform.UIKit.performWithoutAnimation
import platform.UIKit.removeFromSuperview
import platform.UIKit.setAnimationsEnabled
import platform.UIKit.setContentScaleFactor
import platform.UIKit.setFrame
import platform.UIKit.setNeedsDisplay
import platform.UIKit.setNeedsUpdateConstraints
import platform.UIKit.snapshotViewAfterScreenUpdates
import platform.darwin.Byte
import platform.darwin.ByteVar
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.getpagesize
import platform.posix.posix_memalign

val NoOpUpdate: UIView.() -> Unit = {}

@Composable
public fun <T : UIView> UIKitInteropView(
    background: Color = Color.White,
    factory: () -> T,
    modifier: Modifier = Modifier,
    update: (T) -> Unit = NoOpUpdate,
    dispose: (T) -> Unit = {},
) {
    val componentInfo = remember { ComponentInfo<T>() }
    val root = LocalLayerContainer.current
    val density = LocalDensity.current.density
    val focusManager = LocalFocusManager.current//todo redundant
    val focusSwitcher = remember { FocusSwitcher(componentInfo, focusManager) }
    var rectInPixels by remember { mutableStateOf(IntRect(0, 0, 0, 0)) }
    var uiViewSize by remember { mutableStateOf(IntSize(0, 0)) }
    var localToWindowOffset: IntOffset by remember { mutableStateOf(IntOffset.Zero) }
    Box(
        modifier = modifier.onGloballyPositioned { childCoordinates ->
            val coordinates = childCoordinates.parentCoordinates!!
            val newRectInPixels = IntRect(localToWindowOffset, coordinates.size)
            if (rectInPixels != newRectInPixels) {
                val rect = newRectInPixels / density
                componentInfo.container.setFrame(rect.toCGRect())
                if (rectInPixels.width != newRectInPixels.width || rectInPixels.height != newRectInPixels.height) {
                    componentInfo.component.setFrame(CGRectMake(0.0, 0.0, rect.width.toDouble(), rect.height.toDouble()))
                }
                rectInPixels = newRectInPixels
            }
        }.drawBehind {
            drawRect(Color.Transparent, blendMode = BlendMode.DstAtop)//draw transparent hole
        }
    ) {
        focusSwitcher.Content()
    }

    DisposableEffect(factory) {
        componentInfo.component = factory()
        componentInfo.container = UIView().apply {
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

//<editor-fold desc="FocusSwitcher">
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
//</editor-fold>

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
