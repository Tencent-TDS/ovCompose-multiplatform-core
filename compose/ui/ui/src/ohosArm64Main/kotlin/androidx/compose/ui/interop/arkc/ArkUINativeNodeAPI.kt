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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import cnames.structs.ArkUI_LayoutConstraint
import cnames.structs.ArkUI_NodeCustomEvent
import cnames.structs.ArkUI_NodeEvent
import kotlin.reflect.KProperty1
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.cstr
import kotlinx.cinterop.get
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKString
import kotlinx.cinterop.useContents
import platform.arkui.ARKUI_ALIGNMENT_CENTER
import platform.arkui.ARKUI_CLIP_TYPE_RECTANGLE
import platform.arkui.ARKUI_NODE_CUSTOM
import platform.arkui.ARKUI_VISIBILITY_HIDDEN
import platform.arkui.ARKUI_VISIBILITY_NONE
import platform.arkui.ARKUI_VISIBILITY_VISIBLE
import platform.arkui.ArkUI_Alignment
import platform.arkui.ArkUI_AttributeItem
import platform.arkui.ArkUI_GestureMask
import platform.arkui.ArkUI_GesturePriority
import platform.arkui.ArkUI_NativeAPIVariantKind.ARKUI_NATIVE_NODE
import platform.arkui.ArkUI_NativeNodeAPI_1
import platform.arkui.ArkUI_NodeAttributeType
import platform.arkui.ArkUI_NodeDirtyFlag
import platform.arkui.ArkUI_NodeHandle
import platform.arkui.ArkUI_NodeType
import platform.arkui.ArkUI_NumberValue
import platform.arkui.ArkUI_Visibility
import platform.arkui.NODE_ALIGNMENT
import platform.arkui.NODE_BACKGROUND_COLOR
import platform.arkui.NODE_BORDER_COLOR
import platform.arkui.NODE_BORDER_WIDTH
import platform.arkui.NODE_CLIP
import platform.arkui.NODE_CLIP_SHAPE
import platform.arkui.NODE_ENABLED
import platform.arkui.NODE_HEIGHT
import platform.arkui.NODE_HEIGHT_PERCENT
import platform.arkui.NODE_ID
import platform.arkui.NODE_LAYOUT_RECT
import platform.arkui.NODE_NEED_LAYOUT
import platform.arkui.NODE_NEED_MEASURE
import platform.arkui.NODE_NEED_RENDER
import platform.arkui.NODE_OFFSET
import platform.arkui.NODE_UNIQUE_ID
import platform.arkui.NODE_VISIBILITY
import platform.arkui.NODE_WIDTH
import platform.arkui.NODE_WIDTH_PERCENT
import platform.arkui.NORMAL
import platform.arkui.NORMAL_GESTURE_MASK
import platform.arkui.OH_ArkUI_LayoutConstraint_Create
import platform.arkui.OH_ArkUI_LayoutConstraint_Dispose
import platform.arkui.OH_ArkUI_LayoutConstraint_GetMaxHeight
import platform.arkui.OH_ArkUI_LayoutConstraint_GetMaxWidth
import platform.arkui.OH_ArkUI_LayoutConstraint_GetMinHeight
import platform.arkui.OH_ArkUI_LayoutConstraint_GetMinWidth
import platform.arkui.OH_ArkUI_LayoutConstraint_GetPercentReferenceHeight
import platform.arkui.OH_ArkUI_LayoutConstraint_GetPercentReferenceWidth
import platform.arkui.OH_ArkUI_LayoutConstraint_SetMaxHeight
import platform.arkui.OH_ArkUI_LayoutConstraint_SetMaxWidth
import platform.arkui.OH_ArkUI_LayoutConstraint_SetMinHeight
import platform.arkui.OH_ArkUI_LayoutConstraint_SetMinWidth
import platform.arkui.OH_ArkUI_LayoutConstraint_SetPercentReferenceHeight
import platform.arkui.OH_ArkUI_LayoutConstraint_SetPercentReferenceWidth
import platform.arkui.OH_ArkUI_QueryModuleInterfaceByName

internal val STUB_LAMBDA_WITH_RECEIVER: Any.() -> Unit = {}

internal typealias ArkUILayoutConstraint = CPointer<ArkUI_LayoutConstraint>?
internal typealias ArkUIDrawContext = CPointer<cnames.structs.ArkUI_DrawContext>?
internal typealias ArkUINodeEvent = CPointer<ArkUI_NodeEvent>?
internal typealias ArkUINodeCustomEvent = CPointer<ArkUI_NodeCustomEvent>?
internal typealias ArkUIInputEvent = CPointer<cnames.structs.ArkUI_UIInputEvent>?

internal fun ArkUINodeHandle(
    type: ArkUI_NodeType = ARKUI_NODE_CUSTOM,
    block: ArkUI_NodeHandle.() -> Unit = STUB_LAMBDA_WITH_RECEIVER
): ArkUI_NodeHandle =
    requireNotNull(KArkUINativeNodeAPI.createNode(type)) {
        "ArkUI_NodeType($type) was not found on ArkUI_NativeNodeAPI_1#createNode."
    }.apply(block)

internal val CArkUINativeNodeAPI: ArkUI_NativeNodeAPI_1 by lazy {
    val nodeApi = OH_ArkUI_QueryModuleInterfaceByName(
        ARKUI_NATIVE_NODE,
        "ArkUI_NativeNodeAPI_1"
    )

    requireNotNull(nodeApi) { "ArkUI_NativeNodeAPI_1 was not found." }

    nodeApi.reinterpret<ArkUI_NativeNodeAPI_1>().pointed
}

internal val KArkUINativeNodeAPI by lazy { ArkUINativeNodeAPI(CArkUINativeNodeAPI) }

internal value class ArkUINativeNodeAPI(private val nodeApi: ArkUI_NativeNodeAPI_1) {

    val setAttribute get() = requireApi(ArkUI_NativeNodeAPI_1::setAttribute)

    val getAttribute get() = requireApi(ArkUI_NativeNodeAPI_1::getAttribute)

    val createNode get() = requireApi(ArkUI_NativeNodeAPI_1::createNode)

    val disposeNode get() = requireApi(ArkUI_NativeNodeAPI_1::disposeNode)

    val markDirty get() = requireApi(ArkUI_NativeNodeAPI_1::markDirty)

    val measureNode get() = requireApi(ArkUI_NativeNodeAPI_1::measureNode)

    val layoutNode get() = requireApi(ArkUI_NativeNodeAPI_1::layoutNode)

    val setLayoutPosition get() = requireApi(ArkUI_NativeNodeAPI_1::setLayoutPosition)

    val getLayoutPosition get() = requireApi(ArkUI_NativeNodeAPI_1::getLayoutPosition)

    val setMeasuredSize get() = requireApi(ArkUI_NativeNodeAPI_1::setMeasuredSize)

    val getMeasuredSize get() = requireApi(ArkUI_NativeNodeAPI_1::getMeasuredSize)

    val getChildAt get() = requireApi(ArkUI_NativeNodeAPI_1::getChildAt)

    val insertChildAt get() = requireApi(ArkUI_NativeNodeAPI_1::insertChildAt)

    val insertChildAfter get() = requireApi(ArkUI_NativeNodeAPI_1::insertChildAfter)

    val insertChildBefore get() = requireApi(ArkUI_NativeNodeAPI_1::insertChildBefore)

    val getTotalChildCount get() = requireApi(ArkUI_NativeNodeAPI_1::getTotalChildCount)

    val addChild get() = requireApi(ArkUI_NativeNodeAPI_1::addChild)

    val removeChild get() = requireApi(ArkUI_NativeNodeAPI_1::removeChild)

    val removeAllChildren get() = requireApi(ArkUI_NativeNodeAPI_1::removeAllChildren)

    val setUserData get() = requireApi(ArkUI_NativeNodeAPI_1::setUserData)

    val getUserData get() = requireApi(ArkUI_NativeNodeAPI_1::getUserData)

    val addNodeCustomEventReceiver get() = requireApi(ArkUI_NativeNodeAPI_1::addNodeCustomEventReceiver)

    val addNodeEventReceiver get() = requireApi(ArkUI_NativeNodeAPI_1::addNodeEventReceiver)

    val registerNodeEvent get() = requireApi(ArkUI_NativeNodeAPI_1::registerNodeEvent)

    val registerNodeCustomEvent get() = requireApi(ArkUI_NativeNodeAPI_1::registerNodeCustomEvent)

    val unregisterNodeEvent get() = requireApi(ArkUI_NativeNodeAPI_1::unregisterNodeEvent)

    val unregisterNodeCustomEvent get() = requireApi(ArkUI_NativeNodeAPI_1::unregisterNodeCustomEvent)

    val registerNodeEventReceiver get() = requireApi(ArkUI_NativeNodeAPI_1::registerNodeEventReceiver)

    val registerNodeCustomEventReceiver get() = requireApi(ArkUI_NativeNodeAPI_1::registerNodeCustomEventReceiver)

    val unregisterNodeEventReceiver get() = requireApi(ArkUI_NativeNodeAPI_1::unregisterNodeEventReceiver)

    val unregisterNodeCustomEventReceiver get() = requireApi(ArkUI_NativeNodeAPI_1::unregisterNodeCustomEventReceiver)

    val removeNodeEventReceiver get() = requireApi(ArkUI_NativeNodeAPI_1::removeNodeEventReceiver)

    val removeNodeCustomEventReceiver get() = requireApi(ArkUI_NativeNodeAPI_1::removeNodeCustomEventReceiver)

    val getParent get() = requireApi(ArkUI_NativeNodeAPI_1::getParent)

    inline fun setAttributeValue(
        handle: ArkUI_NodeHandle,
        type: ArkUI_NodeAttributeType,
        length: Int,
        block: (values: List<ArkUI_NumberValue>) -> Unit,
    ) {
        setAttribute(handle, type, length = length, block = block)
    }

    fun <R> getAttributeValue(
        handle: ArkUI_NodeHandle,
        type: ArkUI_NodeAttributeType,
        default: () -> R,
        block: (values: CPointer<ArkUI_NumberValue>) -> R
    ): R = getAttribute(handle, type)?.pointed?.value?.let(block) ?: default()

    fun <R> getAttributeValue(
        handle: ArkUI_NodeHandle,
        type: ArkUI_NodeAttributeType,
        default: R,
        block: (values: CPointer<ArkUI_NumberValue>) -> R
    ): R = getAttribute(handle, type)?.pointed?.value?.let(block) ?: default

    fun setAttributeString(
        handle: ArkUI_NodeHandle,
        type: ArkUI_NodeAttributeType,
        value: String
    ) {
        memScoped {
            val attr = alloc<ArkUI_AttributeItem> {
                string = value.cstr.ptr
            }
            KArkUINativeNodeAPI.setAttribute(handle, type, attr.ptr)
        }
    }

    fun getAttributeString(handle: ArkUI_NodeHandle, type: ArkUI_NodeAttributeType, default: () -> String): String =
        KArkUINativeNodeAPI.getAttribute(handle, type)?.pointed?.string?.toKString() ?: default()

    fun getAttributeString(handle: ArkUI_NodeHandle, type: ArkUI_NodeAttributeType, default: String): String =
        KArkUINativeNodeAPI.getAttribute(handle, type)?.pointed?.string?.toKString() ?: default

    fun setAttributeObject(handle: ArkUI_NodeHandle, type: ArkUI_NodeAttributeType, value: COpaquePointer) {
        memScoped {
            val attr = alloc<ArkUI_AttributeItem> {
                `object` = value
            }
            KArkUINativeNodeAPI.setAttribute(handle, type, attr.ptr)
        }
    }

    fun getAttributeObject(handle: ArkUI_NodeHandle, type: ArkUI_NodeAttributeType): COpaquePointer? =
        KArkUINativeNodeAPI.getAttribute(handle, type)?.pointed?.`object`

    inline fun setAttribute(
        handle: ArkUI_NodeHandle,
        type: ArkUI_NodeAttributeType,
        str: String? = null,
        obj: COpaquePointer? = null,
        length: Int = 0,
        block: (values: List<ArkUI_NumberValue>) -> Unit,
    ) {
        memScoped {
            val values = if (length > 0) {
                val list = List(length) { alloc<ArkUI_NumberValue>() }.also(block)
                allocArray<ArkUI_NumberValue>(length) { index ->
                    i32 = list[index].i32
                    f32 = list[index].f32
                    u32 = list[index].u32
                }
            } else {
                null
            }
            val attr = alloc<ArkUI_AttributeItem> {
                this.value = values
                this.size = length
                this.string = str?.cstr?.ptr
                this.`object` = obj
            }
            KArkUINativeNodeAPI.setAttribute(handle, type, attr.ptr)
        }
    }

    private fun <V> requireApi(api: KProperty1<ArkUI_NativeNodeAPI_1, V?>): V =
        requireNotNull(api.get(nodeApi)) {
            "API ${api.name}@$api in ArkUI_NativeNodeAPI_1@$nodeApi was null."
        }
}

internal var ArkUI_NodeHandle.id: String
    set(value) {
        KArkUINativeNodeAPI.setAttributeString(this, NODE_ID, value)
    }
    get() = KArkUINativeNodeAPI.getAttributeString(this, NODE_ID, "")

internal val ArkUI_NodeHandle.uniqueId: Int
    get() = KArkUINativeNodeAPI.getAttributeValue(this, NODE_UNIQUE_ID, -1) { value ->
        value[0].i32
    }

internal var ArkUI_NodeHandle.enabled: Boolean
    set(value) {
        KArkUINativeNodeAPI.setAttributeValue(this, NODE_ENABLED, 1) { values ->
            values[0].i32 = if (value) 1 else 0
        }
    }
    get() = KArkUINativeNodeAPI.getAttributeValue(this, NODE_ENABLED, true) { value ->
        value[0].i32 == 1
    }

internal var ArkUI_NodeHandle.alignment: ArkUI_Alignment
    set(value) {
        KArkUINativeNodeAPI.setAttributeValue(this, NODE_ALIGNMENT, 1) { values ->
            values[0].i32 = value.toInt()
        }
    }
    get() = KArkUINativeNodeAPI.getAttributeValue(
        this,
        NODE_ALIGNMENT,
        ARKUI_ALIGNMENT_CENTER
    ) { value ->
        value[0].i32.toUInt()
    }

internal var ArkUI_NodeHandle.width: Float
    set(value) {
        KArkUINativeNodeAPI.setAttributeValue(this, NODE_WIDTH, 1) { values ->
            values[0].f32 = value
        }
    }
    get() = KArkUINativeNodeAPI.getAttributeValue(this, NODE_WIDTH, 0f) { value ->
        value[0].f32
    }

internal var ArkUI_NodeHandle.height: Float
    set(value) {
        KArkUINativeNodeAPI.setAttributeValue(this, NODE_HEIGHT, 1) { values ->
            values[0].f32 = value
        }
    }
    get() = KArkUINativeNodeAPI.getAttributeValue(this, NODE_HEIGHT, 0f) { value ->
        value[0].f32
    }

internal var ArkUI_NodeHandle.widthPercent: Float
    set(value) {
        KArkUINativeNodeAPI.setAttributeValue(this, NODE_WIDTH_PERCENT, 1) { values ->
            values[0].f32 = value
        }
    }
    get() = KArkUINativeNodeAPI.getAttributeValue(this, NODE_WIDTH_PERCENT, 0f) { value ->
        value[0].f32
    }

internal var ArkUI_NodeHandle.heightPercent: Float
    set(value) {
        KArkUINativeNodeAPI.setAttributeValue(this, NODE_HEIGHT_PERCENT, 1) { values ->
            values[0].f32 = value
        }
    }
    get() = KArkUINativeNodeAPI.getAttributeValue(this, NODE_HEIGHT_PERCENT, 0f) { value ->
        value[0].f32
    }

internal var ArkUI_NodeHandle.backgroundColor: Color
    set(value) {
        KArkUINativeNodeAPI.setAttributeValue(this, NODE_BACKGROUND_COLOR, 1) { values ->
            values[0].u32 = value.toArgb().toUInt()
        }
    }
    get() = KArkUINativeNodeAPI.getAttributeValue(this, NODE_CLIP, Color.Unspecified) { value ->
        Color(value[0].u32.toInt())
    }

internal var ArkUI_NodeHandle.borderWidth: Float
    set(value) {
        KArkUINativeNodeAPI.setAttributeValue(this, NODE_BORDER_WIDTH, 1) { values ->
            values[0].f32 = value
        }
    }
    get() = KArkUINativeNodeAPI.getAttributeValue(this, NODE_BORDER_WIDTH, 0f) { value ->
        value[0].f32
    }

internal var ArkUI_NodeHandle.borderColor: Color
    set(value) {
        KArkUINativeNodeAPI.setAttributeValue(this, NODE_BORDER_COLOR, 1) { values ->
            values[0].u32 = value.toArgb().toUInt()
        }
    }
    get() = KArkUINativeNodeAPI.getAttributeValue(
        this,
        NODE_BORDER_COLOR,
        Color.Unspecified
    ) { value ->
        Color(value[0].f32.toInt())
    }

internal var ArkUI_NodeHandle.clip: Boolean
    set(value) {
        KArkUINativeNodeAPI.setAttributeValue(this, NODE_CLIP, 1) { values ->
            values[0].i32 = if (value) 1 else 0
        }
    }
    get() = KArkUINativeNodeAPI.getAttributeValue(this, NODE_CLIP, false) { values ->
        values[0].i32 == 1
    }

internal var ArkUI_NodeHandle.offset: Offset
    set(value) {
        KArkUINativeNodeAPI.setAttributeValue(this, NODE_OFFSET, 2) { values ->
            values[0].f32 = value.x
            values[1].f32 = value.y
        }
    }
    get() = KArkUINativeNodeAPI.getAttributeValue(this, NODE_OFFSET, Offset.Unspecified) { value ->
        Offset(value[0].f32, value[1].f32)
    }

internal var ArkUI_NodeHandle.layoutRect: IntRect
    set(value) {
        KArkUINativeNodeAPI.setAttributeValue(this, NODE_LAYOUT_RECT, 4) { values ->
            values[0].i32 = value.left
            values[1].i32 = value.top
            values[0].i32 = value.width
            values[1].i32 = value.height
        }
    }
    get() = KArkUINativeNodeAPI.getAttributeValue(this, NODE_OFFSET, IntRect.Zero) { value ->
        IntRect(IntOffset(value[0].i32, value[1].i32), IntSize(value[2].i32, value[3].i32))
    }

internal var ArkUI_NodeHandle.measuredSize: IntSize
    set(value) {
        KArkUINativeNodeAPI.setMeasuredSize(this, value.width, value.height)
    }
    get() = KArkUINativeNodeAPI.getMeasuredSize(this).useContents { IntSize(width, height) }

internal var ArkUI_NodeHandle.layoutPosition: IntOffset
    set(value) {
        KArkUINativeNodeAPI.setLayoutPosition(this, value.x, value.y)
    }
    get() = KArkUINativeNodeAPI.getLayoutPosition(this).useContents { IntOffset(x, y) }

internal var ArkUI_NodeHandle.isVisible: Boolean
    set(value) {
        visibility = if (value) ARKUI_VISIBILITY_VISIBLE else ARKUI_VISIBILITY_NONE
    }
    get() = visibility == ARKUI_VISIBILITY_VISIBLE

internal var ArkUI_NodeHandle.isHidden: Boolean
    set(value) {
        visibility = if (value) ARKUI_VISIBILITY_HIDDEN else ARKUI_VISIBILITY_VISIBLE
    }
    get() = visibility == ARKUI_VISIBILITY_HIDDEN

internal var ArkUI_NodeHandle.isNone: Boolean
    set(value) {
        visibility = if (value) ARKUI_VISIBILITY_NONE else ARKUI_VISIBILITY_VISIBLE
    }
    get() = visibility == ARKUI_VISIBILITY_NONE

internal var ArkUI_NodeHandle.visibility: ArkUI_Visibility
    set(value) {
        KArkUINativeNodeAPI.setAttributeValue(this, NODE_VISIBILITY, 1) { values ->
            values[0].i32 = value.toInt()
        }
    }
    get() = KArkUINativeNodeAPI.getAttributeValue(
        this,
        NODE_VISIBILITY,
        ARKUI_VISIBILITY_VISIBLE
    ) { value ->
        value[0].i32.toUInt()
    }

internal val ArkUI_NodeHandle.parent: ArkUI_NodeHandle? get() = KArkUINativeNodeAPI.getParent(this)

internal fun ArkUI_NodeHandle.removeFromParent() {
    parent?.removeChild(this)
}

internal fun ArkUI_NodeHandle.clipSize(size: Size) =
    KArkUINativeNodeAPI.setAttributeValue(this, NODE_CLIP_SHAPE, length = 5) { values ->
        values[0].i32 = ARKUI_CLIP_TYPE_RECTANGLE.toInt()
        values[1].f32 = size.width
        values[2].f32 = size.height
        values[3].f32 = 0f
        values[4].f32 = 0f
    }

internal fun ArkUI_NodeHandle.measureNode(constraints: Constraints) {
    val arkConstraints = OH_ArkUI_LayoutConstraint_Create().apply {
        minWidth = constraints.minWidth
        maxWidth = constraints.maxWidth
        minHeight = constraints.minHeight
        maxHeight = constraints.maxHeight
        percentReferenceWidth = constraints.maxWidth
        percentReferenceHeight = constraints.maxHeight
    }
    KArkUINativeNodeAPI.measureNode(this, arkConstraints)
    arkConstraints.dispose()
}

internal fun ArkUI_NodeHandle.layoutNode(position: IntOffset) {
    KArkUINativeNodeAPI.layoutNode(this, position.x, position.y)
}

internal fun ArkUI_NodeHandle.needMeasure() = markDirty(NODE_NEED_MEASURE)

internal fun ArkUI_NodeHandle.needLayout() = markDirty(NODE_NEED_LAYOUT)

internal fun ArkUI_NodeHandle.needRender() = markDirty(NODE_NEED_RENDER)

internal fun ArkUI_NodeHandle.markDirty(flag: ArkUI_NodeDirtyFlag) =
    KArkUINativeNodeAPI.markDirty(this, flag)

internal fun ArkUI_NodeHandle.addChild(node: ArkUI_NodeHandle) =
    KArkUINativeNodeAPI.addChild(this, node)

internal fun ArkUI_NodeHandle.insertChildAt(node: ArkUI_NodeHandle, index: Int) =
    KArkUINativeNodeAPI.insertChildAt(this, node, index)

internal fun ArkUI_NodeHandle.removeChild(node: ArkUI_NodeHandle) =
    KArkUINativeNodeAPI.removeChild(this, node)

internal fun ArkUI_NodeHandle.getChildAt(index: Int): ArkUI_NodeHandle? =
    KArkUINativeNodeAPI.getChildAt(this, index)

internal fun ArkUI_NodeHandle.getTotalChildCount(): UInt =
    KArkUINativeNodeAPI.getTotalChildCount(this)

internal fun ArkUI_NodeHandle.addGesture(
    gesture: CArkUIGestureRecognizer,
    priority: ArkUI_GesturePriority = NORMAL,
    mask: ArkUI_GestureMask = NORMAL_GESTURE_MASK
) = KArkUINativeGestureAPI.addGestureToNode(this, gesture, priority, mask)

internal fun ArkUI_NodeHandle.removeGesture(gesture: CArkUIGestureRecognizer) =
    KArkUINativeGestureAPI.removeGestureFromNode(this, gesture)

internal fun ArkUI_NodeHandle.dispose() {
    KArkUINativeNodeAPI.disposeNode(this)
}

internal var ArkUILayoutConstraint.minWidth: Int
    set(value) {
        OH_ArkUI_LayoutConstraint_SetMinWidth(this, value)
    }
    get() = OH_ArkUI_LayoutConstraint_GetMinWidth(this)

internal var ArkUILayoutConstraint.maxWidth: Int
    set(value) {
        OH_ArkUI_LayoutConstraint_SetMaxWidth(this, value)
    }
    get() = OH_ArkUI_LayoutConstraint_GetMaxWidth(this)

internal var ArkUILayoutConstraint.minHeight: Int
    set(value) {
        OH_ArkUI_LayoutConstraint_SetMinHeight(this, value)
    }
    get() = OH_ArkUI_LayoutConstraint_GetMinHeight(this)

internal var ArkUILayoutConstraint.maxHeight: Int
    set(value) {
        OH_ArkUI_LayoutConstraint_SetMaxHeight(this, value)
    }
    get() = OH_ArkUI_LayoutConstraint_GetMaxHeight(this)

internal var ArkUILayoutConstraint.percentReferenceWidth: Int
    set(value) {
        OH_ArkUI_LayoutConstraint_SetPercentReferenceWidth(this, value)
    }
    get() = OH_ArkUI_LayoutConstraint_GetPercentReferenceWidth(this)

internal var ArkUILayoutConstraint.percentReferenceHeight: Int
    set(value) {
        OH_ArkUI_LayoutConstraint_SetPercentReferenceHeight(this, value)
    }
    get() = OH_ArkUI_LayoutConstraint_GetPercentReferenceHeight(this)

internal fun ArkUILayoutConstraint.dispose() =
    OH_ArkUI_LayoutConstraint_Dispose(this)


