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

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.interop.ArkUIView
import androidx.compose.ui.graphics.isDebugLogEnabled
import androidx.compose.ui.graphics.kLog
import androidx.compose.ui.interop.OhosTrace
import androidx.compose.ui.napi.JsEnv
import androidx.compose.ui.napi.JsFunction
import androidx.compose.ui.napi.JsObject
import androidx.compose.ui.napi.asBoolean
import androidx.compose.ui.napi.asFloat
import androidx.compose.ui.napi.asInt
import androidx.compose.ui.napi.call
import androidx.compose.ui.napi.get
import androidx.compose.ui.napi.js
import androidx.compose.ui.napi.jsFunction
import androidx.compose.ui.napi.nApiValue
import androidx.compose.ui.napi.set
import androidx.compose.ui.node.InteropContainer
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.TrackInteropModifierElement
import androidx.compose.ui.node.TrackInteropModifierNode
import androidx.compose.ui.node.countInteropComponentsBefore
import androidx.compose.ui.unit.Constraints
import platform.ohos.napi_ref
import platform.ohos.napi_value
import kotlin.math.abs

private inline fun Float.precisionNotEqual(rhs: Float): Boolean = abs(this - rhs) > 0.0000001

/**
 * Bridge of an ArkUIView in ArkTs part which contains a RenderNode.
 */
class ArkUIView internal constructor(
    val name: String,
    var parameter: JsObject,
    val onMeasured: ((width: Int, height: Int) -> Unit)? = null,
    val composeParameterUpdater: ((JsObject) -> Unit)? = null
) {
    internal var onRequestDisallowInterceptTouchEvent: ((Boolean) -> Unit)? = null

    private var jsOnMeasured: JsFunction<ArkUIView, Unit>? = null
    private var jsComposeParameterUpdater: JsFunction<ArkUIView, Unit>? = null
    private var jsOnRequestDisallowInterceptTouchEvent: JsFunction<ArkUIView, Unit>? = null

    // Use `UNDEFINED` instead of 0 as initial value.
    private var translationX: Float = UNDEFINED
    private var translationY: Float = UNDEFINED
    private var positionX: Float = UNDEFINED
    private var positionY: Float = UNDEFINED
    private var sizeWidth: Float = UNDEFINED
    private var sizeHeight: Float = UNDEFINED
    private var visible = false

    // init this when View is created.
    internal var jsArkUIViewRef: napi_ref? = null

    var backgroundColor: Color
        set(value) {
            // Background color of ArkUIView is forced to be opaque.
            val newColor = if (value == Color.Unspecified) Color.White else value
            jsArkUIViewRef.call("setBackgroundColor", newColor.toArgb().nApiValue())
        }
        get() {
            val argb = jsArkUIViewRef.call("getBackgroundColor").asInt()
            return if (argb != null) Color(argb) else Color.Unspecified
        }

    val measuredHeight: Float?
        get() = jsArkUIViewRef["measuredHeight"].asFloat()

    val measuredWidth: Float?
        get() = jsArkUIViewRef["measuredWidth"].asFloat()

    val id: Float?
        get() = jsArkUIViewRef["id"].asFloat()

    fun bindJs(jsArkUIView: napi_value?) {
        jsArkUIView ?: return

        this.jsArkUIViewRef = JsEnv.createReference(jsArkUIView)

        val jsOnMeasured = jsFunction(this) { width: Int?, height: Int? ->
            if (width != null && height != null) {
                onMeasured?.invoke(width, height)
            }
        }

        jsArkUIViewRef["onMeasured"] = jsOnMeasured.jsValue
        this.jsOnMeasured = jsOnMeasured

        if (isDebugLogEnabled) {
            kLog("ArkUIView bindJs value=${jsOnMeasured.jsValue?.rawValue}, id=${id}, hash=${hashCode()}")
        }

        val jsComposeParameterUpdater = jsFunction(this) { newParameter: JsObject? ->
            if (newParameter != null) {
                this@ArkUIView.parameter = newParameter
                composeParameterUpdater?.invoke(newParameter)
            }
        }

        jsArkUIViewRef["composeParameterUpdater"] = jsComposeParameterUpdater.jsValue
        this.jsComposeParameterUpdater = jsComposeParameterUpdater

        val jsOnRequestDisallowInterceptTouchEvent =
            jsFunction(this) { disallowIntercept: Boolean? ->
                if (disallowIntercept != null) {
                    onRequestDisallowInterceptTouchEvent?.invoke(disallowIntercept)
                }
            }

        jsArkUIViewRef["onRequestDisallowInterceptTouchEvent"] = jsOnRequestDisallowInterceptTouchEvent.jsValue
        this.jsOnRequestDisallowInterceptTouchEvent = jsOnRequestDisallowInterceptTouchEvent
    }

    /**
     * Set translation of the ArkUIView.
     *
     * @param x in pixels.
     * @param y in pixels.
     */
    fun setTranslation(x: Float, y: Float) {
        if (translationX.precisionNotEqual(x) || translationY.precisionNotEqual(y)) {
            translationX = x
            translationY = y
            jsArkUIViewRef.call("setTranslation", x.nApiValue(), y.nApiValue())
        }
    }

    fun setPosition(x: Float, y: Float) {
        if (positionX.precisionNotEqual(x) || positionY.precisionNotEqual(y)) {
            positionX = x
            positionY = y
            jsArkUIViewRef.call("setPosition", x.nApiValue(), y.nApiValue())
        }
    }

    fun setSize(width: Float, height: Float) {
        if (sizeWidth.precisionNotEqual(width) || sizeHeight.precisionNotEqual(height)) {
            sizeWidth = width
            sizeHeight = height
            jsArkUIViewRef.call("setSize", width.nApiValue(), height.nApiValue())
        }
    }

    fun setClipBounds(left: Float, top: Float, right: Float, bottom: Float) {
        jsArkUIViewRef.call("setClipBounds", left.nApiValue(), top.nApiValue(), right.nApiValue(), bottom.nApiValue())
    }

    fun setVisible(visible: Boolean) {
        if (this.visible != visible) {
            this.visible = visible
            jsArkUIViewRef.call("setVisible", visible.nApiValue())
        }
    }

    fun measure(constraints: Constraints, density: Float) {
        fun size(width: Int, height: Int) = js {
            "width"(width)
            "height"(height)
        }.jsValue
        jsArkUIViewRef.call("measure", js {
            "minSize"(size(constraints.minWidth, constraints.minHeight))
            "maxSize"(size(constraints.maxWidth, constraints.maxHeight))
            "percentReference"(size(constraints.maxWidth, constraints.maxHeight))
        })
    }

    fun dispatchTouchEvent(touchEvent: TouchEvent): Boolean {
        if (jsArkUIViewRef != null) {
            return jsArkUIViewRef.call("onTouchEvent", touchEvent.nativeEvent).asBoolean() ?: false
        }
        return false
    }

    fun update(parameter: JsObject) {
        if (jsArkUIViewRef != null) {
            this.parameter = parameter
            jsArkUIViewRef.call("updateArkUIParameter", parameter.jsValue, false.nApiValue())
        }
    }

    fun customProperty(key: String, value: napi_value?) {
        jsArkUIViewRef.call("customProperty", key.nApiValue(), value)
    }

    fun getCustomProperty(key: String): napi_value? =
        jsArkUIViewRef.call("getCustomProperty", key.nApiValue())

    fun dispose() {
        if (isDebugLogEnabled) {
            kLog("ArkUIView dispose value=${jsOnMeasured?.jsValue?.rawValue}, id=${id}, hash=${hashCode()}")
        }
        jsArkUIViewRef["onMeasured"] = null
        jsOnMeasured?.dispose()
        jsOnMeasured = null

        jsArkUIViewRef["composeParameterUpdater"] = null
        jsComposeParameterUpdater?.dispose()
        jsComposeParameterUpdater = null

        jsArkUIViewRef["onRequestDisallowInterceptTouchEvent"] = null
        jsOnRequestDisallowInterceptTouchEvent?.dispose()
        jsOnRequestDisallowInterceptTouchEvent = null

        JsEnv.deleteReference(jsArkUIViewRef)
        jsArkUIViewRef = null
    }

    companion object {
        // Used as the layout initial value, take a negative value instead of 0.
        private const val UNDEFINED = -1f
    }
}

class ArkUIViewContainer {
    // the arkUIView is null until added to hierarchy.
    private var _arkUIView: ArkUIView? = null
    var arkUIView: ArkUIView
        set(value) {
            value.onRequestDisallowInterceptTouchEvent = onRequestDisallowInterceptTouchEvent
            _arkUIView = value
        }
        get() = requireNotNull(_arkUIView) { "ArkUIView has not been added to container!" }

    internal var onRequestDisallowInterceptTouchEvent: ((Boolean) -> Unit)? = null
        set(value) {
            field = value
            _arkUIView?.onRequestDisallowInterceptTouchEvent = field
        }

    fun dispatchTouchEvent(touchEvent: TouchEvent): Boolean {
        return _arkUIView?.dispatchTouchEvent(touchEvent) ?: return false
    }
}

/**
 * Bridge of an ArkUIView in ArkTs part which contains a RenderNode.
 */
class ArkUIRootView(jsArkUIRootView: napi_value) : InteropContainer<ArkUIViewContainer> {

    private var rootViewRef = JsEnv.createReference(jsArkUIRootView)

    override var rootModifier: TrackInteropModifierNode<ArkUIViewContainer>? = null
    override var interopViews = mutableSetOf<ArkUIViewContainer>()
        private set

    fun buildView(view: ArkUIView) {
        val rootView = JsEnv.getReferenceValue(rootViewRef)
        val addSubViewFunc = JsEnv.getProperty(rootView, "buildView".nApiValue())
        val jsArkUIView = JsEnv.callFunction(
            rootView,
            addSubViewFunc,
            view.name.nApiValue(),
            view.parameter.jsValue,
        )
        OhosTrace.traceSync("bindJs") {
            view.bindJs(jsArkUIView)
        }
    }

    override fun addInteropView(nativeView: ArkUIViewContainer) {
        val index = countInteropComponentsBefore(nativeView)
        interopViews.add(nativeView)
        insertSubView(nativeView.arkUIView, index)
    }

    override fun removeInteropView(nativeView: ArkUIViewContainer) {
        removeSubView(nativeView.arkUIView)
        interopViews.remove(nativeView)
    }

    private fun addSubView(view: ArkUIView) {
        val rootView = JsEnv.getReferenceValue(rootViewRef)
        val addSubViewFunc = JsEnv.getProperty(rootView, "addSubView".nApiValue())
        val jsArkUIView = JsEnv.callFunction(
            rootView,
            addSubViewFunc,
            view.name.nApiValue(),
            view.parameter.jsValue
        )
        view.bindJs(jsArkUIView)
    }

    private fun insertSubView(view: ArkUIView, index: Int) {
        val rootView = JsEnv.getReferenceValue(rootViewRef)
        val arkUIView = JsEnv.getReferenceValue(view.jsArkUIViewRef)
        val addSubViewFunc = JsEnv.getProperty(rootView, "insertSubView".nApiValue())
        JsEnv.callFunction(
            rootView,
            addSubViewFunc,
            arkUIView,
            index.nApiValue()
        )
    }

    private fun removeSubView(view: ArkUIView) {
        val rootView = JsEnv.getReferenceValue(rootViewRef)
        val removeSubViewFunc = JsEnv.getProperty(rootView, "removeSubView".nApiValue())

        val jsArkUIView = JsEnv.getReferenceValue(view.jsArkUIViewRef)
        JsEnv.callFunction(rootView, removeSubViewFunc, jsArkUIView)
        view.dispose()
    }

    fun dispose() {
        JsEnv.deleteReference(rootViewRef)
        rootViewRef = null
    }

}

/**
 * Modifier to track interop view inside [LayoutNode] hierarchy.
 *
 * @param view The [ArkUIView] that matches the current node.
 */
internal fun Modifier.trackUIKitInterop(
    view: ArkUIViewContainer
): Modifier = this then TrackInteropModifierElement(
    nativeView = view
)