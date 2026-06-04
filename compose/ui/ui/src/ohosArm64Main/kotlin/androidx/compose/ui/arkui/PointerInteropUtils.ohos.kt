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

package androidx.compose.ui.arkui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.interop.arkc.ArkUIInputEvent
import androidx.compose.ui.interop.arkc.action
import androidx.compose.ui.interop.arkc.x
import androidx.compose.ui.interop.arkc.y
import androidx.compose.ui.napi.JsObject
import androidx.compose.ui.napi.asInt
import androidx.compose.ui.napi.asLong
import androidx.compose.ui.napi.nApiValue
import org.jetbrains.skiko.currentNanoTime
import platform.ohos.OH_ArkUI_PointerEvent_SetClonedEventLocalPosition
import platform.arkui.UI_TOUCH_EVENT_ACTION_CANCEL
import platform.ohos.napi_value

class ArkUITouchEvent(val nativeEvent: ArkUIInputEvent, val jsEvent: TouchEvent)
class MouseEvent(nativeEvent: napi_value?) : JsObject(nativeEvent)
class AxisEvent(nativeEvent: napi_value?) : JsObject(nativeEvent)

class TouchEvent(nativeEvent: napi_value?) : JsObject(nativeEvent) {
    var type: Int
        set(value) {
            this["type"] = value.nApiValue()
        }
        get() = requireNotNull(get("type").asInt()) { "The type of TouchEvent(ohos) was null!" }

    var timestamp: Long
        set(value) {
            this["timestamp"] = value.nApiValue()
        }
        get() = requireNotNull(get("timestamp").asLong()) { "The timestamp of TouchEvent(ohos) was null!" }

    val nativeEvent: napi_value? get() = jsValue

    companion object {
        const val ACTION_DOWN = 0
        const val ACTION_UP = 1
        const val ACTION_MOVE = 2
        const val ACTION_CANCEL = 3
    }
}

/**
 * Converts to a [TouchEvent] and runs [block] with it.
 *
 * @param block The block to be executed with the resulting [TouchEvent].
 */
internal fun PointerEvent.toTouchEventScope(
    block: (TouchEvent) -> Unit
) {
    toTouchEventScope(block, false)
}

/**
 * Converts to an [TouchEvent.ACTION_CANCEL] [TouchEvent] and runs [block] with it.
 *
 * @param block The block to be executed with the resulting [TouchEvent].
 */
internal fun PointerEvent.toCancelTouchEventScope(
    block: (TouchEvent) -> Unit
) {
    toTouchEventScope(block, true)
}

private fun PointerEvent.toTouchEventScope(
    block: (TouchEvent) -> Unit,
    cancel: Boolean
) {
    // TODO(nativeEvent): how to handle null nativeEvent.
    val touchEvent = when (nativeEvent) {
        is TouchEvent -> nativeEvent
        is ArkUITouchEvent -> nativeEvent.jsEvent
        else -> return
    }

    if (cancel) {
        val oldType = touchEvent.type
        val oldTimestamp = touchEvent.timestamp

        touchEvent.type = TouchEvent.ACTION_CANCEL
        // The timestamp also need to be updated here to pass the validity check,
        // such as `PostEventManager::CheckPointValidity` in ohos.
        touchEvent.timestamp = currentNanoTime()

        block(touchEvent)

        touchEvent.timestamp = oldTimestamp
        touchEvent.type = oldType
    } else {
        block(touchEvent)
    }
}

/**
 * Converts to a [ArkUIInputEvent] and runs [block] with it.
 *
 * @param block The block to be executed with the resulting [ArkUIInputEvent].
 */
internal fun PointerEvent.toNativeTouchEventScope(
    position: Offset,
    block: (ArkUIInputEvent) -> Unit
) {
    toNativeTouchEventScope(position, block, false)
}

/**
 * Converts to an [UI_TOUCH_EVENT_ACTION_CANCEL] [ArkUIInputEvent] and runs [block] with it.
 *
 * @param block The block to be executed with the resulting [ArkUIInputEvent].
 */
internal fun PointerEvent.toCancelNativeTouchEventScope(
    offset: Offset,
    block: (ArkUIInputEvent) -> Unit
) {
    toNativeTouchEventScope(offset, block, true)
}

private fun PointerEvent.toNativeTouchEventScope(
    offset: Offset,
    block: (ArkUIInputEvent) -> Unit,
    cancel: Boolean
) {
    val event = nativeEvent as? ArkUITouchEvent ?: return
    val nativeEvent = event.nativeEvent
    val x = nativeEvent.x
    val y = nativeEvent.y
    OH_ArkUI_PointerEvent_SetClonedEventLocalPosition(nativeEvent, x - offset.x, y - offset.y)
    if (cancel) {
        val oldAction = nativeEvent.action
        nativeEvent.action = UI_TOUCH_EVENT_ACTION_CANCEL.toInt()
        block(nativeEvent)
        nativeEvent.action = oldAction
    } else {
        block(nativeEvent)
    }
    OH_ArkUI_PointerEvent_SetClonedEventLocalPosition(nativeEvent, x, y)
}
