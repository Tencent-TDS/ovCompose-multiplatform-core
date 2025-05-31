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
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlinx.cinterop.CValue
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.invoke
import kotlinx.cinterop.staticCFunction
import platform.arkui.ARKUI_NODE_CUSTOM
import platform.arkui.ARKUI_NODE_CUSTOM_EVENT_ON_DRAW
import platform.arkui.ARKUI_NODE_CUSTOM_EVENT_ON_LAYOUT
import platform.arkui.ARKUI_NODE_CUSTOM_EVENT_ON_MEASURE
import platform.arkui.ArkUI_Alignment
import platform.arkui.ArkUI_GestureMask
import platform.arkui.ArkUI_GesturePriority
import platform.arkui.ArkUI_IntOffset
import platform.arkui.ArkUI_NodeCustomEventType
import platform.arkui.ArkUI_NodeDirtyFlag
import platform.arkui.ArkUI_NodeEventType
import platform.arkui.ArkUI_NodeHandle
import platform.arkui.ArkUI_NodeType
import platform.arkui.ArkUI_Visibility
import platform.arkui.NODE_EVENT_ON_APPEAR
import platform.arkui.NODE_EVENT_ON_ATTACH
import platform.arkui.NODE_EVENT_ON_DETACH
import platform.arkui.NODE_EVENT_ON_DISAPPEAR
import platform.arkui.NODE_NEED_LAYOUT
import platform.arkui.NODE_NEED_MEASURE
import platform.arkui.NODE_NEED_RENDER
import platform.arkui.NODE_ON_CLICK
import platform.arkui.NODE_ON_TOUCH_INTERCEPT
import platform.arkui.NORMAL
import platform.arkui.NORMAL_GESTURE_MASK
import platform.arkui.OH_ArkUI_NodeCustomEvent_GetDrawContextInDraw
import platform.arkui.OH_ArkUI_NodeCustomEvent_GetEventTargetId
import platform.arkui.OH_ArkUI_NodeCustomEvent_GetEventType
import platform.arkui.OH_ArkUI_NodeCustomEvent_GetLayoutConstraintInMeasure
import platform.arkui.OH_ArkUI_NodeCustomEvent_GetPositionInLayout
import platform.arkui.OH_ArkUI_NodeCustomEvent_GetUserData
import platform.arkui.OH_ArkUI_NodeEvent_GetEventType
import platform.arkui.OH_ArkUI_NodeEvent_GetInputEvent
import platform.arkui.OH_ArkUI_NodeEvent_GetTargetId
import platform.arkui.OH_ArkUI_NodeEvent_GetUserData

open class ArkUINativeView private constructor(
    val handle: ArkUI_NodeHandle,
    private val eventManager: ArkUIViewEventManager
) : ArkUIViewComponent, ArkUIViewEvent by eventManager {

    internal constructor(
        type: ArkUI_NodeType = ARKUI_NODE_CUSTOM,
        block: ArkUI_NodeHandle.() -> Unit = STUB_LAMBDA_WITH_RECEIVER
    ) : this(ArkUINodeHandle(type, block))

    internal constructor(handle: ArkUI_NodeHandle) : this(handle, ArkUIViewEventManager(handle))

    private val children = mutableListOf<ArkUINativeView>()

    override var enabled: Boolean by handle::enabled

    override var alignment: ArkUI_Alignment by handle::alignment

    override var width: Float by handle::width

    override var height: Float by handle::height

    override var widthPercent: Float by handle::widthPercent

    override var heightPercent: Float by handle::heightPercent

    override var backgroundColor: Color by handle::backgroundColor

    override var borderWidth: Float by handle::borderWidth

    override var borderColor: Color by handle::borderColor

    override var clip: Boolean by handle::clip

    override var offset: Offset by handle::offset

    override var layoutRect: IntRect by handle::layoutRect

    override var measuredSize: IntSize by handle::measuredSize

    override var layoutPosition: IntOffset by handle::layoutPosition

    override var isVisible: Boolean by handle::isVisible

    override var isHidden: Boolean by handle::isHidden

    override var isNone: Boolean by handle::isNone

    override var visibility: ArkUI_Visibility by handle::visibility

    operator fun invoke() = handle

    inline operator fun <T> invoke(block: ArkUI_NodeHandle.() -> T): T = handle(block)

    inline fun <T> handle(block: ArkUI_NodeHandle.() -> T): T = handle.run(block)

    override fun clipSize(size: Size) = handle.clipSize(size)

    override fun measureNode(constraints: Constraints) = handle.measureNode(constraints)

    override fun layoutNode(position: IntOffset) = handle.layoutNode(position)

    override fun needMeasure() = markDirty(NODE_NEED_MEASURE)

    override fun needLayout() = markDirty(NODE_NEED_LAYOUT)

    override fun needRender() = markDirty(NODE_NEED_RENDER)

    override fun markDirty(flag: ArkUI_NodeDirtyFlag) = handle.markDirty(flag)

    override fun addChild(node: ArkUINativeView) {
        children.add(node)
        handle.addChild(node.handle)
    }

    override fun insertChildAt(view: ArkUINativeView, index: Int) {
        children.add(index, view)
        handle.insertChildAt(view.handle, index)
    }

    override fun removeChild(view: ArkUINativeView) {
        children.remove(view)
        handle.removeChild(view.handle)
    }

    override fun getChildAt(index: Int): ArkUINativeView? = children.getOrNull(index)

    override fun getTotalChildCount(): Int = children.size

    override fun addGesture(
        gesture: CArkUIGestureRecognizer,
        priority: ArkUI_GesturePriority,
        mask: ArkUI_GestureMask
    ) {
        handle.addGesture(gesture, priority, mask)
    }

    override fun removeGesture(gesture: CArkUIGestureRecognizer) {
        handle.removeGesture(gesture)
    }

    open fun dispose() {
        eventManager.dispose()
        handle.dispose()
    }

    fun disposeTree() {
        dispose()
        for (child in children) child.disposeTree()
    }
}

private class ArkUIViewEventManager(private val handle: ArkUI_NodeHandle) : ArkUIViewEvent {

    private val lazyRef = lazy { StableRef.create(this) }

    private val stableRef by lazyRef

    init {
        // remove before adding.
        KArkUINativeNodeAPI.removeNodeEventReceiver(handle, EventReceiver)
        KArkUINativeNodeAPI.removeNodeCustomEventReceiver(handle, CustomEventReceiver)
        KArkUINativeNodeAPI.addNodeEventReceiver(handle, EventReceiver)
        KArkUINativeNodeAPI.addNodeCustomEventReceiver(handle, CustomEventReceiver)
    }

    // events
    override var onClick: Function0<Unit>? by Event(NODE_ON_CLICK, null)

    override var onAppear: Function0<Unit>? by Event(NODE_EVENT_ON_APPEAR, null)

    override var onDisAppear: Function0<Unit>? by Event(NODE_EVENT_ON_DISAPPEAR, null)

    override var onAttach: Function0<Unit>? by Event(NODE_EVENT_ON_ATTACH, null)

    override var onDetach: Function0<Unit>? by Event(NODE_EVENT_ON_DETACH, null)

    override var onTouchIntercept: Function1<ArkUIInputEvent, Unit>?
        by Event(NODE_ON_TOUCH_INTERCEPT, null)

    // custom events
    override var onMeasure: Function1<ArkUILayoutConstraint, Unit>?
        by CustomEvent(ARKUI_NODE_CUSTOM_EVENT_ON_MEASURE, null)

    override var onLayout: Function1<CValue<ArkUI_IntOffset>, Unit>?
        by CustomEvent(ARKUI_NODE_CUSTOM_EVENT_ON_LAYOUT, null)

    override var onDraw: Function1<ArkUIDrawContext, Unit>?
        by CustomEvent(ARKUI_NODE_CUSTOM_EVENT_ON_DRAW, null)

    fun dispose() {
        onClick = null
        onAppear = null
        onDisAppear = null
        onAttach = null
        onDetach = null

        onMeasure = null
        onLayout = null
        onDraw = null
        KArkUINativeNodeAPI.removeNodeEventReceiver(handle, EventReceiver)
        KArkUINativeNodeAPI.removeNodeCustomEventReceiver(handle, CustomEventReceiver)
        if (lazyRef.isInitialized()) {
            lazyRef.value.dispose()
        }
    }

    private fun processEvent(event: ArkUINodeEvent) {
        val type = OH_ArkUI_NodeEvent_GetEventType(event)
        when (type) {
            NODE_ON_CLICK -> onClick?.invoke()
            NODE_EVENT_ON_APPEAR -> onAppear?.invoke()
            NODE_EVENT_ON_DISAPPEAR -> onDisAppear?.invoke()
            NODE_EVENT_ON_ATTACH -> onAttach?.invoke()
            NODE_EVENT_ON_DETACH -> onDetach?.invoke()
            NODE_ON_TOUCH_INTERCEPT -> onTouchIntercept?.invoke(
                OH_ArkUI_NodeEvent_GetInputEvent(event)
            )
        }
    }

    private fun processCustomEvent(event: ArkUINodeCustomEvent) {
        val type = OH_ArkUI_NodeCustomEvent_GetEventType(event)
        when (type) {
            ARKUI_NODE_CUSTOM_EVENT_ON_MEASURE ->
                onMeasure?.invoke(OH_ArkUI_NodeCustomEvent_GetLayoutConstraintInMeasure(event))

            ARKUI_NODE_CUSTOM_EVENT_ON_LAYOUT ->
                onLayout?.invoke(OH_ArkUI_NodeCustomEvent_GetPositionInLayout(event))

            ARKUI_NODE_CUSTOM_EVENT_ON_DRAW ->
                onDraw?.invoke(OH_ArkUI_NodeCustomEvent_GetDrawContextInDraw(event))
        }
    }

    companion object {
        private const val VIEW_TARGET_ID = 1111 shl 8

        private val EventReceiver = staticCFunction { event: ArkUINodeEvent ->
            val target = OH_ArkUI_NodeEvent_GetTargetId(event)
            if (target != VIEW_TARGET_ID) return@staticCFunction

            val userData = requireNotNull(OH_ArkUI_NodeEvent_GetUserData(event))
            val node = userData.asStableRef<ArkUIViewEventManager>().get()
            node.processEvent(event)
        }

        private val CustomEventReceiver = staticCFunction { event: ArkUINodeCustomEvent ->
            val target = OH_ArkUI_NodeCustomEvent_GetEventTargetId(event)
            if (target != VIEW_TARGET_ID) return@staticCFunction

            val userData = requireNotNull(OH_ArkUI_NodeCustomEvent_GetUserData(event))
            val node = userData.asStableRef<ArkUIViewEventManager>().get()
            node.processCustomEvent(event)
        }
    }

    private open class Event<V>(
        private val type: ArkUI_NodeEventType,
        private var value: V,
    ) : ReadWriteProperty<ArkUIViewEventManager, V> {
        open val register get() = KArkUINativeNodeAPI.registerNodeEvent
        open val unregister get() = KArkUINativeNodeAPI.unregisterNodeEvent

        override fun getValue(thisRef: ArkUIViewEventManager, property: KProperty<*>): V =
            value

        override fun setValue(thisRef: ArkUIViewEventManager, property: KProperty<*>, value: V) {
            val handle = thisRef.handle
            if (value == null && this.value != null) {
                unregister(handle, type)
            }
            if (value != null && this.value == null) {
                register(handle, type, VIEW_TARGET_ID, thisRef.stableRef.asCPointer())
            }
            this.value = value
        }
    }

    private class CustomEvent<V>(
        type: ArkUI_NodeCustomEventType,
        value: V
    ) : Event<V>(type, value) {
        override val register get() = KArkUINativeNodeAPI.registerNodeCustomEvent
        override val unregister get() = KArkUINativeNodeAPI.unregisterNodeCustomEvent
    }
}

internal interface ArkUIViewEvent {
    // events
    var onClick: Function0<Unit>?
    var onAppear: Function0<Unit>?
    var onDisAppear: Function0<Unit>?
    var onAttach: Function0<Unit>?
    var onDetach: Function0<Unit>?
    var onTouchIntercept: Function1<ArkUIInputEvent, Unit>?

    // custom events
    var onMeasure: Function1<ArkUILayoutConstraint, Unit>?
    var onLayout: Function1<CValue<ArkUI_IntOffset>, Unit>?
    var onDraw: Function1<ArkUIDrawContext, Unit>?
}

internal interface ArkUIViewComponent {
    var enabled: Boolean

    var alignment: ArkUI_Alignment

    var width: Float

    var height: Float

    var widthPercent: Float

    var heightPercent: Float

    var backgroundColor: Color

    var borderWidth: Float

    var borderColor: Color

    var clip: Boolean

    var offset: Offset

    var layoutRect: IntRect

    var measuredSize: IntSize

    var layoutPosition: IntOffset

    var isVisible: Boolean

    var isHidden: Boolean

    var isNone: Boolean

    var visibility: ArkUI_Visibility

    fun clipSize(size: Size)

    fun measureNode(constraints: Constraints)

    fun layoutNode(position: IntOffset)

    fun needMeasure()

    fun needLayout()

    fun needRender()

    fun markDirty(flag: ArkUI_NodeDirtyFlag)

    fun addChild(node: ArkUINativeView)

    fun insertChildAt(view: ArkUINativeView, index: Int)

    fun removeChild(view: ArkUINativeView)

    fun getChildAt(index: Int): ArkUINativeView?

    fun getTotalChildCount(): Int

    fun addGesture(
        gesture: CArkUIGestureRecognizer,
        priority: ArkUI_GesturePriority = NORMAL,
        mask: ArkUI_GestureMask = NORMAL_GESTURE_MASK
    )

    fun removeGesture(gesture: CArkUIGestureRecognizer)
}