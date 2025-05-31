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
import androidx.compose.ui.arkui.InternalArkUIViewController
import androidx.compose.ui.arkui.TouchEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.HistoricalChange
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.interop.ArkUIInteropContext
import androidx.compose.ui.interop.OhosTrace
import androidx.compose.ui.napi.JsEnv
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
import androidx.compose.ui.platform.textinput.KeyboardVisibilityListenerImpl
import androidx.compose.ui.platform.textinput.TextInputService
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.window.ComposeArkUIViewControllerConfiguration
import kotlin.coroutines.CoroutineContext
import org.jetbrains.skiko.currentNanoTime
import platform.ohos.napi_env
import platform.ohos.napi_value

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

    private val render: ComposeSceneRender by lazy {
        ComposeSceneRender(onDraw = scene::render)
    }

    private val keyboardVisibilityListener by lazy {
        KeyboardVisibilityListenerImpl(
            density = { scene.density },
            keyboardOverlapHeightState = keyboardOverlapHeightState,
            keyboardAvoidFocusOffsetState = keyboardAvoidFocusOffsetState,
            sceneMediatorProvider = { this },
            focusManagerProvider = { scene.focusManager },
        )
    }

    private val sizeChangeDispatcher by lazy {
        PlatformSizeChangeDispatcher(controller.messenger)
    }

    private val activeChangedPointers = mutableMapOf<PointerId, ComposeScenePointer>()

    fun setSize(width: Int, height: Int) {
        scene.density = Density(controller.density)
        val bounds = scene.boundsInWindow
        if (bounds?.width != width || bounds.height != height) {
            scene.boundsInWindow = IntRect(0, 0, width, height)
            render.setSize(width, height)
            sizeChange = true
        }
    }

    fun setContent(content: @Composable () -> Unit) {
        scene.setContent { ProvideComposeSceneMediatorCompositionLocals(content) }
    }

    fun onDraw(id: String, timestamp: Long, targetTimestamp: Long) {
        notifySizeChange(id)
        render.draw(targetTimestamp)
        // processInteropActions needs to be placed behind render.draw() to avoid drawing out of sync problems
        processInteropActions()
    }

    fun keyboardWillShow(keyboardHeight: Float) {
        keyboardVisibilityListener.keyboardWillShow(keyboardHeight)
    }

    fun keyboardWillHide() {
        keyboardVisibilityListener.keyboardWillHide()
    }

    fun dispose() {
        // scene close 后会释放里面的资源，不用再 scene = null
        dispose = true
        scene.close()
        render.close()
        semanticsOwnerListener.dispose()
        // After scene is disposed all ArkUI interop actions can't be deferred to be synchronized with rendering
        // Thus they need to be executed now.
        processInteropActions()
    }

    private fun processInteropActions() {
        val interopTransaction = interopContext.retrieve()
        interopTransaction.actions.fastForEach { it() }
    }

    private fun notifySizeChange(id: String) {
        if (sizeChange) {
            sizeChange = false
            sizeChangeDispatcher.onComposeSizeChange(id, render.width, render.height)
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
                    // TODO: Gavin 2024/12/24 avoid boxing
                    val x = JsEnv.getValueDouble(JsEnv.getNamedProperty(historicalPoint, "x"))
                        ?.toFloat()
                    val y = JsEnv.getValueDouble(JsEnv.getNamedProperty(historicalPoint, "y"))
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