package androidx.compose.ui.interop.arkc

import androidx.compose.ui.geometry.Offset
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.arkui.ArkUI_NodeHandle
import platform.arkui.OH_ArkUI_PointerEvent_GetX
import platform.arkui.OH_ArkUI_PointerEvent_GetY
import platform.ohos.OH_ArkUI_PointerEvent_SetClonedEventActionType
import platform.ohos.OH_ArkUI_PointerEvent_SetClonedEventLocalPosition
import platform.arkui.OH_ArkUI_UIInputEvent_GetAction
import platform.ohos.OH_ArkUI_PointerEvent_CreateClonedEvent
import platform.ohos.OH_ArkUI_PointerEvent_DestroyClonedEvent
import platform.ohos.OH_ArkUI_PointerEvent_PostClonedEvent

internal typealias ArkUIInputEvent = CPointer<cnames.structs.ArkUI_UIInputEvent>?

internal val ArkUIInputEvent.x: Float get() = OH_ArkUI_PointerEvent_GetX(this)
internal val ArkUIInputEvent.y: Float get() = OH_ArkUI_PointerEvent_GetY(this)

internal var ArkUIInputEvent.position: Offset
    get() = Offset(x, y)
    set(value) {
        OH_ArkUI_PointerEvent_SetClonedEventLocalPosition(this, value.x, value.y)
    }

internal var ArkUIInputEvent.action: Int
    get() = OH_ArkUI_UIInputEvent_GetAction(this)
    set(value) {
        OH_ArkUI_PointerEvent_SetClonedEventActionType(this, value)
    }

internal fun ArkUIInputEvent.clone(): ArkUIInputEvent = memScoped {
    val event = alloc<CPointerVar<cnames.structs.ArkUI_UIInputEvent>>()
    OH_ArkUI_PointerEvent_CreateClonedEvent(this@clone, event.ptr)
    event.value
}

internal fun ArkUIInputEvent.destroy() = OH_ArkUI_PointerEvent_DestroyClonedEvent(this)

internal fun ArkUI_NodeHandle.postClonedEvent(event: ArkUIInputEvent): Int =
    OH_ArkUI_PointerEvent_PostClonedEvent(this, event)
