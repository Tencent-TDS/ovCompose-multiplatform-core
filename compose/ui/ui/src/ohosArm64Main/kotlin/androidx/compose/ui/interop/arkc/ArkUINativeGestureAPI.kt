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

package androidx.compose.ui.interop.arkc

import androidx.compose.ui.platform.EmptyViewConfiguration
import kotlin.reflect.KProperty1
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.invoke
import kotlinx.cinterop.pointed
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.staticCFunction
import platform.arkui.ArkUI_GestureDirectionMask
import platform.arkui.ArkUI_GroupGestureMode
import platform.arkui.ArkUI_NativeAPIVariantKind
import platform.arkui.ArkUI_NativeGestureAPI_1
import platform.arkui.GESTURE_DIRECTION_ALL
import platform.arkui.GESTURE_EVENT_ACTION_ACCEPT
import platform.arkui.GESTURE_EVENT_ACTION_CANCEL
import platform.arkui.GESTURE_EVENT_ACTION_END
import platform.arkui.GESTURE_EVENT_ACTION_UPDATE
import platform.arkui.OH_ArkUI_GestureEvent_GetActionType
import platform.arkui.OH_ArkUI_GestureEvent_GetNode
import platform.arkui.OH_ArkUI_QueryModuleInterfaceByName

internal typealias CArkUIGestureRecognizer = CPointer<cnames.structs.ArkUI_GestureRecognizer>?

internal typealias ArkUIGestureEvent = CPointer<cnames.structs.ArkUI_GestureEvent>?

private val GESTURE_TOUCH_SLOP = EmptyViewConfiguration.touchSlop.toDouble()

internal val CArkUINativeGestureAPI: ArkUI_NativeGestureAPI_1 by lazy {
    val gestureApi = OH_ArkUI_QueryModuleInterfaceByName(
        ArkUI_NativeAPIVariantKind.ARKUI_NATIVE_GESTURE,
        "ArkUI_NativeGestureAPI_1"
    )

    requireNotNull(gestureApi) { "ArkUI_NativeGestureAPI_1 was not found." }

    gestureApi.reinterpret<ArkUI_NativeGestureAPI_1>().pointed
}

internal val KArkUINativeGestureAPI by lazy {
    ArkUINativeGestureAPI(CArkUINativeGestureAPI)
}

internal class PanGesture(
    fingers: Int = 1,
    directions: ArkUI_GestureDirectionMask = GESTURE_DIRECTION_ALL,
    distance: Double = GESTURE_TOUCH_SLOP
) : ArkUIGestureRecognizer(
    KArkUINativeGestureAPI.createPanGesture(fingers, directions, distance)
)

internal class TapGesture(count: Int = 1, fingers: Int = 1) :
    ArkUIGestureRecognizer(KArkUINativeGestureAPI.createTapGesture(count, fingers))

internal class GroupGesture(mode: ArkUI_GroupGestureMode) :
    ArkUIGestureRecognizer(KArkUINativeGestureAPI.createGroupGesture(mode))

internal class RotationGesture(fingers: Int, angle: Double) :
    ArkUIGestureRecognizer(KArkUINativeGestureAPI.createRotationGesture(fingers, angle))

internal class PinchGesture(fingers: Int, distance: Double) :
    ArkUIGestureRecognizer(KArkUINativeGestureAPI.createPinchGesture(fingers, distance))

internal class SwipeGesture(fingers: Int, directions: ArkUI_GestureDirectionMask, speed: Double) :
    ArkUIGestureRecognizer(KArkUINativeGestureAPI.createSwipeGesture(fingers, directions, speed))

internal class LongPressGesture(fingers: Int, repeatResult: Boolean, duration: Int) :
    ArkUIGestureRecognizer(
        KArkUINativeGestureAPI.createLongPressGesture(fingers, repeatResult, duration)
    )

internal typealias GestureEventCallBack = (event: ArkUIGestureEvent) -> Unit

internal sealed class ArkUIGestureRecognizer(internal val recognizer: CArkUIGestureRecognizer) {

    var onAccept: GestureEventCallBack? = null
    var onUpdate: GestureEventCallBack? = null
    var onEnd: GestureEventCallBack? = null
    var onCancel: GestureEventCallBack? = null

    private val ref = StableRef.create(this)

    init {
        KArkUINativeGestureAPI.setGestureEventTarget(
            recognizer, GESTURE_EVENT_ACTION_ALL, ref.asCPointer(), GestureEventReceiver
        )
    }

    fun onGestureEvent(event: ArkUIGestureEvent) {
        val action = event.action
        when (action) {
            GESTURE_EVENT_ACTION_ACCEPT -> onAccept?.invoke(event)
            GESTURE_EVENT_ACTION_UPDATE -> onUpdate?.invoke(event)
            GESTURE_EVENT_ACTION_END -> onEnd?.invoke(event)
            GESTURE_EVENT_ACTION_CANCEL -> onCancel?.invoke(event)
        }
    }

    fun dispose() {
        ref.dispose()
        recognizer.dispose()
    }

    companion object {
        private val GESTURE_EVENT_ACTION_ALL =
            GESTURE_EVENT_ACTION_ACCEPT or GESTURE_EVENT_ACTION_UPDATE or GESTURE_EVENT_ACTION_END or GESTURE_EVENT_ACTION_CANCEL

        private val GestureEventReceiver =
            staticCFunction { event: ArkUIGestureEvent, extraParams: COpaquePointer? ->
                val data = requireNotNull(extraParams)
                val recognizer = data.asStableRef<ArkUIGestureRecognizer>().get()
                recognizer.onGestureEvent(event)
            }
    }
}

internal fun CArkUIGestureRecognizer.dispose() {
    KArkUINativeGestureAPI.dispose(this)
}

internal val ArkUIGestureEvent.action get() = OH_ArkUI_GestureEvent_GetActionType(this)
internal val ArkUIGestureEvent.node get() = OH_ArkUI_GestureEvent_GetNode(this)

internal value class ArkUINativeGestureAPI(private val gestureApi: ArkUI_NativeGestureAPI_1) {

    val addChildGesture get() = requireApi(ArkUI_NativeGestureAPI_1::addChildGesture)
    val addGestureToNode get() = requireApi(ArkUI_NativeGestureAPI_1::addGestureToNode)
    val createPanGesture get() = requireApi(ArkUI_NativeGestureAPI_1::createPanGesture)
    val createTapGesture get() = requireApi(ArkUI_NativeGestureAPI_1::createTapGesture)
    val createGroupGesture get() = requireApi(ArkUI_NativeGestureAPI_1::createGroupGesture)
    val createPinchGesture get() = requireApi(ArkUI_NativeGestureAPI_1::createPinchGesture)
    val createSwipeGesture get() = requireApi(ArkUI_NativeGestureAPI_1::createSwipeGesture)
    val createLongPressGesture get() = requireApi(ArkUI_NativeGestureAPI_1::createLongPressGesture)
    val createRotationGesture get() = requireApi(ArkUI_NativeGestureAPI_1::createRotationGesture)
    val createTapGestureWithDistanceThreshold get() = requireApi(ArkUI_NativeGestureAPI_1::createTapGestureWithDistanceThreshold)
    val dispose get() = requireApi(ArkUI_NativeGestureAPI_1::dispose)
    val getGestureType get() = requireApi(ArkUI_NativeGestureAPI_1::getGestureType)
    val removeChildGesture get() = requireApi(ArkUI_NativeGestureAPI_1::removeChildGesture)
    val removeGestureFromNode get() = requireApi(ArkUI_NativeGestureAPI_1::removeGestureFromNode)
    val setGestureEventTarget get() = requireApi(ArkUI_NativeGestureAPI_1::setGestureEventTarget)
    val setInnerGestureParallelTo get() = requireApi(ArkUI_NativeGestureAPI_1::setInnerGestureParallelTo)
    val setGestureInterrupterToNode get() = requireApi(ArkUI_NativeGestureAPI_1::setGestureInterrupterToNode)

    private fun <V> requireApi(api: KProperty1<ArkUI_NativeGestureAPI_1, V?>): V =
        requireNotNull(api.get(gestureApi)) {
            "API ${api.name}@$api in ArkUI_NativeGestureAPI_1@$gestureApi was null."
        }
}
