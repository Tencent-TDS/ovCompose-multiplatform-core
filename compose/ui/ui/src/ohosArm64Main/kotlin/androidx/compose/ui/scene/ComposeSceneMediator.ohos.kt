/*
 * Tencent is pleased to support the open source community by making ovCompose available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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

package androidx.compose.ui.scene

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.arkui.AxisEvent
import androidx.compose.ui.arkui.InternalArkUIViewController
import androidx.compose.ui.arkui.MouseEvent
import androidx.compose.ui.arkui.TouchEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.pointer.HistoricalChange
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.areAnyPressed
import androidx.compose.ui.input.pointer.isAltPressed
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.interop.ArkUIInteropAction
import androidx.compose.ui.interop.ArkUIInteropContext
import androidx.compose.ui.interop.OhosTrace
import androidx.compose.ui.napi.JsEnv
import androidx.compose.ui.napi.asBoolean
import androidx.compose.ui.napi.asFloat
import androidx.compose.ui.napi.asInt
import androidx.compose.ui.napi.asJsArray
import androidx.compose.ui.napi.nApiValue
import androidx.compose.ui.platform.LocalKeyboardAvoidFocusOffset
import androidx.compose.ui.platform.LocalKeyboardOverlapHeight
import androidx.compose.ui.platform.PlatformClipboardProxy
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformContextImpl
import androidx.compose.ui.platform.PlatformSizeChangeDispatcher
import androidx.compose.ui.platform.PlatformTextToolbar
import androidx.compose.ui.platform.PlatformWindowContext
import androidx.compose.ui.platform.accessibility.OHNativeXComponent
import androidx.compose.ui.platform.accessibility.SemanticsOwnerListenerImpl
import androidx.compose.ui.platform.input.key.PlatformKeyEventImpl
import androidx.compose.ui.platform.textinput.KeyboardVisibilityListenerImpl
import androidx.compose.ui.platform.textinput.TextInputService
import androidx.compose.ui.render.RenderStrategy
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.window.ComposeArkUIViewControllerConfiguration
import org.jetbrains.skiko.SkikoInputModifiers
import org.jetbrains.skiko.SkikoKey
import org.jetbrains.skiko.SkikoKeyboardEvent
import org.jetbrains.skiko.SkikoKeyboardEventKind
import org.jetbrains.skiko.currentNanoTime
import platform.devices.KEYCODE_UNKNOWN
import platform.ohos.napi_env
import platform.ohos.napi_value
import kotlin.coroutines.CoroutineContext

@InternalComposeUiApi
internal class ComposeSceneMediator(
    private val controller: InternalArkUIViewController,
    private val configuration: ComposeArkUIViewControllerConfiguration,
    private val windowContext: PlatformWindowContext,
    private val interopContext: ArkUIInteropContext,
    val coroutineContext: CoroutineContext,
    component: OHNativeXComponent,
    composeSceneFactory: (
        invalidate: () -> Unit,
        platformContext: PlatformContext,
        coroutineContext: CoroutineContext
    ) -> ComposeScene
) {
    private val keyboardOverlapHeightState: MutableState<Dp> = mutableStateOf(0.dp)
    private val keyboardAvoidFocusOffsetState: MutableState<Dp> = mutableStateOf(0.dp)
    private val semanticsOwnerListener = SemanticsOwnerListenerImpl(component)

    private var dispose = false
    private var sizeChange = false

    private val platformContext: PlatformContext by lazy {
        PlatformContextImpl(
            windowContext.windowInfo,
            TextInputService(),
            PlatformTextToolbar(controller.messenger, PlatformClipboardProxy(controller.messenger)),
            semanticsOwnerListener,
            densityProvider = { scene.density }
        )
    }

    private val scene: ComposeScene by lazy {
        composeSceneFactory(
            controller::invalidate,
            platformContext,
            coroutineContext
        )
    }

    private val keyboardVisibilityListener by lazy {
        KeyboardVisibilityListenerImpl(
            density = { scene.density },
            keyboardOverlapHeightState = keyboardOverlapHeightState,
            keyboardAvoidFocusOffsetState = keyboardAvoidFocusOffsetState,
            getSceneViewHeight = ::getViewHeight,
            focusManagerProvider = { scene.focusManager },
        )
    }

    private val sizeChangeDispatcher by lazy {
        PlatformSizeChangeDispatcher(controller.messenger)
    }

    private val activeChangedPointers = mutableMapOf<PointerId, ComposeScenePointer>()

    init {
        controller.bindContext(interopContext)
    }

    fun setSize(width: Int, height: Int) {
        scene.density = Density(controller.density)
        val bounds = scene.boundsInWindow
        if (bounds?.width != width || bounds.height != height) {
            scene.boundsInWindow = IntRect(0, 0, width, height)
            sizeChange = true
        }
    }

    fun setContent(content: @Composable () -> Unit) {
        scene.setContent { ProvideComposeSceneMediatorCompositionLocals(content) }
    }

    fun onDraw(id: String, timestamp: Long, targetTimestamp: Long, renderStrategy: RenderStrategy) {
        notifySizeChange(id, renderStrategy.width, renderStrategy.height)
        renderStrategy.render(targetTimestamp, scene::render)
    }

    fun keyboardWillShow(keyboardHeight: Float) {
        keyboardVisibilityListener.keyboardWillShow(keyboardHeight)
    }

    fun keyboardWillHide() {
        keyboardVisibilityListener.keyboardWillHide()
    }

    fun disposeSurface() {
        controller.renderStrategy.close()
    }

    fun dispose() {
        // scene close 后会释放里面的资源，不用再 scene = null
        dispose = true
        scene.close()
        controller.renderStrategy.close()
        semanticsOwnerListener.dispose()
        // After scene is disposed all ArkUI interop actions can't be deferred to be synchronized with rendering
        // Thus they need to be executed now.
        interopContext.getInteropActions().process()
    }

    private fun notifySizeChange(id: String, width: Int, height: Int) {
        if (sizeChange) {
            sizeChange = false
            sizeChangeDispatcher.onComposeSizeChange(id, width, height)
        }
    }

    // The set of the changed and active pointers.

    @OptIn(InternalComposeApi::class, ExperimentalComposeApi::class)
    fun sendPointerEvent(env: napi_env, event: napi_value): Boolean {
        val eventType = event.pointerEventType
        suppressGCIfNeed(eventType)

        val changedPointers = event.getChangedPointers(scene.density.density)
        if (changedPointers.isEmpty()) return false

        // remove the inactive touches.
        activeChangedPointers.removeIf { (_, pointer) -> !pointer.pressed }
        // put the changed touches.
        activeChangedPointers.putAll(changedPointers.associateBy { it.id })
        val pointers = activeChangedPointers.values.toList()

        OhosTrace.traceSync("sendPointerEvent") {
            scene.sendPointerEvent(
                eventType = eventType,
                pointers = pointers,
                timeMillis = event.timestamp,
                nativeEvent = TouchEvent(event)
            )
        }
        return true
    }

    fun onAxisEvent(event: napi_value) {
        val action = JsEnv.getNamedProperty(event, "action").asInt()
        val eventType = when (action) {
            1 -> PointerEventType.ScrollStart
            2 -> PointerEventType.Scroll
            3 -> PointerEventType.ScrollEnd
            else -> return
        }
        val density = scene.density
        scene.sendPointerEvent(
            eventType = eventType,
            type = PointerType.Mouse,
            position = event.position.toOffset(density),
            scrollDelta = event.axisScrollDelta.toOffset(density) * SCROLL_DELTA_MULTIPLIER,
            timeMillis = event.timestamp,
            keyboardModifiers = event.keyboardModifiers,
            nativeEvent = AxisEvent(event),
        )
    }

    fun onPinchEvent(event: napi_value) {
        val density = scene.density
        val scale = event.pinchScale
        scene.sendPointerEvent(
            eventType = PointerEventType.Pinch,
            type = PointerType.Mouse,
            position = event.pinchCenter.toOffset(density),
            pinchScale = scale,
            timeMillis = event.timestamp,
            keyboardModifiers = event.keyboardModifiers
        )
    }

    @OptIn(androidx.compose.ui.annotation.InternalComposeApi::class)
    fun onKeyEvent(event: napi_value): Boolean {
        val keyEventType = when (JsEnv.getNamedProperty(event, "type").asInt()) {
            0 -> SkikoKeyboardEventKind.DOWN
            1 -> SkikoKeyboardEventKind.UP
            else -> SkikoKeyboardEventKind.UNKNOWN
        }
        val keyCode = JsEnv.getNamedProperty(event, "keyCode").asInt() ?: KEYCODE_UNKNOWN
        return scene.sendKeyEvent(
            KeyEvent(
                nativeKeyEvent = SkikoKeyboardEvent(
                    key = SkikoKey.valueOf(keyCode),
                    kind = keyEventType,
                    modifiers = event.skikoInputModifiers,
                    timestamp = event.timestamp,
                    platform = PlatformKeyEventImpl(event)
                )
            )
        )
    }

    fun onMouseEvent(event: napi_value) {
        val action = JsEnv.getNamedProperty(event, "action").asInt()
        if (action == 13) {
            scene.processCancelledPointerInputEvent()
            return
        }
        val eventType = when (action) {
            1 -> PointerEventType.Press
            2 -> PointerEventType.Release
            3 -> PointerEventType.Move
            4 -> PointerEventType.Enter
            5 -> PointerEventType.Exit
            else -> PointerEventType.Unknown
        }
        val buttonValue = JsEnv.getNamedProperty(event, "button").asInt()
        val button = when (buttonValue) {
            1 -> PointerButton.Primary
            2 -> PointerButton.Secondary
            3 -> PointerButton.Tertiary
            4 -> PointerButton.Back
            5 -> PointerButton.Forward
            else -> null
        }
        val buttons = event.pressedButtons

        // remove the inactive touches.
        activeChangedPointers.removeIf { (_, pointer) -> !pointer.pressed }
        val pointer = ComposeScenePointer(
            id = PointerId(MOUSE_POINTER_ID),
            type = PointerType.Mouse,
            position = event.position.toOffset(scene.density),
            pressed = buttons.areAnyPressed
        )
        // put the changed touches.
        activeChangedPointers[pointer.id] = pointer
        val pointers = activeChangedPointers.values.toList()

        scene.sendPointerEvent(
            eventType = eventType,
            button = button,
            buttons = buttons,
            pointers = pointers,
            keyboardModifiers = event.keyboardModifiers,
            timeMillis = event.timestamp,
            nativeEvent = MouseEvent(event)
        )
    }

    @OptIn(InternalComposeApi::class, ExperimentalComposeApi::class)
    private fun suppressGCIfNeed(eventType: PointerEventType) {
        when (eventType) {
            PointerEventType.Move -> {
                // Do nothing when move.
            }

            PointerEventType.Press -> configuration.internalStartGCSuppressor()

            else -> configuration.internalStopGCSuppressor()
        }
    }

    @Composable
    private fun ProvideComposeSceneMediatorCompositionLocals(content: @Composable () -> Unit) {
        CompositionLocalProvider(
            LocalKeyboardOverlapHeight provides keyboardOverlapHeightState.value,
            LocalKeyboardAvoidFocusOffset provides keyboardAvoidFocusOffsetState.value,
            content = content
        )
    }

    fun getViewHeight() = scene.boundsInWindow?.height ?: 0

    companion object {
        private const val TYPE = "type"
        private const val TIMESTAMP = "timestamp"

        private val napi_value.pointerEventType: PointerEventType
            get() = JsEnv.getValueInt32(JsEnv.getNamedProperty(this, TYPE), -1).asPointerEventType()

        private val napi_value.timestamp: Long
            get() {
                val nanoTime =
                    JsEnv.getValueInt64(JsEnv.getNamedProperty(this, TIMESTAMP), currentNanoTime())
                return (nanoTime / 1E6).toLong()
            }

        private val napi_value.position: DpOffset
            get() {
                val positionX = JsEnv.getNamedProperty(this, "x").asFloat() ?: 0f
                val positionY = JsEnv.getNamedProperty(this, "y").asFloat() ?: 0f
                return DpOffset(positionX.dp, positionY.dp)
            }


        private val napi_value.axisScrollDelta: DpOffset
            get() {
                // get axis delta
                val getHorizontalAxisValue = JsEnv.getNamedProperty(this, "getHorizontalAxisValue")
                val horizontalAxisValue =
                    JsEnv.callFunction(this, getHorizontalAxisValue).asFloat() ?: 0f
                val getVerticalAxisValue = JsEnv.getNamedProperty(this, "getVerticalAxisValue")
                val verticalAxisValue =
                    JsEnv.callFunction(this, getVerticalAxisValue).asFloat() ?: 0f
                return DpOffset(horizontalAxisValue.dp, verticalAxisValue.dp)
            }

        private val napi_value.pinchScale: Float
            get() {
                return JsEnv.getNamedProperty(this, "scale").asFloat() ?: 1f
            }

        private val napi_value.pinchCenter: DpOffset
            get() {
                val pinchCenterX = JsEnv.getNamedProperty(this, "pinchCenterX").asFloat() ?: 0f
                val pinchCenterY = JsEnv.getNamedProperty(this, "pinchCenterY").asFloat() ?: 0f
                return DpOffset(pinchCenterX.dp, pinchCenterY.dp)
            }

        private val napi_value.pressedButtons: PointerButtons
            get() {
                val buttonsValue = JsEnv.getNamedProperty(this, "pressedButtons").asJsArray()
                var isPrimaryPressed = false
                var isSecondaryPressed = false
                var isTertiaryPressed = false
                var isBackPressed = false
                var isForwardPressed = false
                for (button in buttonsValue) {
                    when (button.asInt()) {
                        1 -> isPrimaryPressed = true
                        2 -> isSecondaryPressed = true
                        3 -> isTertiaryPressed = true
                        4 -> isBackPressed = true
                        5 -> isForwardPressed = true
                    }
                }
                return PointerButtons(
                    isPrimaryPressed,
                    isSecondaryPressed,
                    isTertiaryPressed,
                    isBackPressed,
                    isForwardPressed
                )
            }

        private val napi_value.keyboardModifiers: PointerKeyboardModifiers
            get() {
                val event = this
                val func = JsEnv.getNamedProperty(event, "getModifierKeyState")
                val ctrlFlag = JsEnv.arrayOf("Ctrl".nApiValue())
                val isCtrlPressed = JsEnv.callFunction(event, func, ctrlFlag).asBoolean() ?: false
                val altFlag = JsEnv.arrayOf("Alt".nApiValue())
                val isAltPressed = JsEnv.callFunction(event, func, altFlag).asBoolean() ?: false
                val shiftFlag = JsEnv.arrayOf("Shift".nApiValue())
                val isShiftPressed = JsEnv.callFunction(event, func, shiftFlag).asBoolean() ?: false
                return PointerKeyboardModifiers(
                    isCtrlPressed = isCtrlPressed,
                    isAltPressed = isAltPressed,
                    isShiftPressed = isShiftPressed
                )
            }

        private val napi_value.skikoInputModifiers: SkikoInputModifiers
            get() {
                val keyboardModifiers = keyboardModifiers
                var result = 0
                if (keyboardModifiers.isAltPressed) {
                    result = result.or(SkikoInputModifiers.ALT.value)
                }
                if (keyboardModifiers.isShiftPressed) {
                    result = result.or(SkikoInputModifiers.SHIFT.value)
                }
                if (keyboardModifiers.isCtrlPressed) {
                    result = result.or(SkikoInputModifiers.CONTROL.value)
                }
                return SkikoInputModifiers(result)
            }

        private fun Int.asPointerEventType(): PointerEventType = when (this) {
            0 -> PointerEventType.Press // TouchType.Down
            1 -> PointerEventType.Release // Up
            2 -> PointerEventType.Move // Move
            3 -> PointerEventType.Release  // Cancel
            else -> PointerEventType.Unknown
        }

        private fun PointerEventType.isPressed(): Boolean =
            this == PointerEventType.Press || this == PointerEventType.Move

        private fun <K, V> MutableMap<K, V>.removeIf(predicate: (Map.Entry<K, V>) -> Boolean) {
            val iterator = iterator()
            while (iterator.hasNext()) {
                if (predicate(iterator.next())) {
                    iterator.remove()
                }
            }
        }

        private fun napi_value.getChangedPointers(density: Float): List<ComposeScenePointer> {

            val historicalPoints = mutableListOf<HistoricalChange>()
            val historicalPointsFun = JsEnv.getNamedProperty(this, "getHistoricalPoints")
            JsEnv.callFunction(this, historicalPointsFun)?.forEachArray { historicalPoint ->
                if (historicalPoint != null) {
                    val timestamp = historicalPoint.timestamp
                    val touchObject = JsEnv.getNamedProperty(historicalPoint, "touchObject")
                    // TODO: Gavin 2024/12/24 avoid boxing
                    val x = JsEnv.getValueDouble(JsEnv.getNamedProperty(touchObject, "x"))
                        ?.toFloat()
                    val y = JsEnv.getValueDouble(JsEnv.getNamedProperty(touchObject, "y"))
                        ?.toFloat()
                    if (x != null && y != null) {
                        historicalPoints.add(
                            HistoricalChange(
                                uptimeMillis = timestamp,
                                position = Offset(x * density, y * density)
                            )
                        )
                    }
                }
            }

            val changedPointers = mutableListOf<ComposeScenePointer>()
            JsEnv.getNamedProperty(this, "changedTouches")?.forEachArray { touchEvent ->
                if (touchEvent != null) {
                    // TODO: Gavin 2024/12/24 avoid boxing
                    val x = JsEnv.getValueDouble(JsEnv.getNamedProperty(touchEvent, "x"))?.toFloat()
                    val y = JsEnv.getValueDouble(JsEnv.getNamedProperty(touchEvent, "y"))?.toFloat()
                    val type = JsEnv.getValueInt32(JsEnv.getNamedProperty(touchEvent, "type"))
                    val id = JsEnv.getValueInt64(JsEnv.getNamedProperty(touchEvent, "id"))
                    if (x != null && y != null && type != null && id != null) {
                        changedPointers.add(
                            ComposeScenePointer(
                                id = PointerId(id),
                                position = Offset(x * density, y * density),
                                pressed = type.asPointerEventType().isPressed(),
                                type = PointerType.Touch,
                                pressure = 1f,
                                historical = historicalPoints
                            )
                        )
                    }
                }
            }
            return changedPointers
        }

        private inline fun napi_value.forEachArray(
            crossinline block: (napi_value?) -> Unit
        ) {
            val count = JsEnv.getArrayLength(this)
            repeat(count) {
                block(JsEnv.getElement(this, it))
            }
        }
    }
}

// Scale factor for scroll delta and velocity from ohos axis event.
private const val SCROLL_DELTA_MULTIPLIER = 0.002f
private const val MOUSE_POINTER_ID = 100L

fun ArkUIInteropContext.getInteropActions(): List<ArkUIInteropAction> {
    val interopTransaction = retrieve()
    return interopTransaction.actions
}

fun List<ArkUIInteropAction>.process() {
    fastForEach { it() }
}