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

@file:Suppress("FunctionName")

package androidx.compose.ui.arkui

import androidx.annotation.CallSuper
import androidx.annotation.MainThread
import androidx.compose.ui.annotation.InternalComposeApi
import androidx.compose.ui.arkui.backhandler.OnBackPressedDispatcher
import androidx.compose.ui.arkui.backhandler.OnBackPressedDispatcherOwner
import androidx.compose.ui.arkui.backhandler.PlatformOnBackPressedDispatcher
import androidx.compose.ui.arkui.density.DensityManager
import androidx.compose.ui.arkui.extra.DefaultExtraStorage
import androidx.compose.ui.arkui.extra.ExtraStorage
import androidx.compose.ui.arkui.extra.ExtraStorageOwner
import androidx.compose.ui.arkui.frame.FrameController
import androidx.compose.ui.arkui.messenger.MessengerImpl
import androidx.compose.ui.arkui.messenger.MessengerOwner
import androidx.compose.ui.arkui.messenger.RemoteMessengerImpl
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_wrapped
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_xcomponent_finishDraw
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_xcomponent_prepareDraw
import androidx.compose.ui.arkui.window.WindowStageEvent
import androidx.compose.ui.arkui.window.WindowStageManager
import androidx.compose.ui.arkui.window.isForeground
import androidx.compose.ui.interop.OhosTrace
import androidx.compose.ui.napi.DisposableRef
import androidx.compose.ui.napi.JsEnv
import androidx.compose.ui.platform.ChoreographerManager
import androidx.compose.ui.platform.Context
import androidx.compose.ui.platform.ContextImpl
import androidx.compose.ui.platform.IVsyncProxy
import androidx.compose.ui.platform.UIContext
import androidx.compose.ui.platform.UIContextImpl
import androidx.compose.ui.platform.accessibility.OHNativeXComponent
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import kotlin.coroutines.CoroutineContext
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.reinterpret
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import platform.ohos.napi_env
import platform.ohos.napi_value


/**
 * ArkUIViewController，提供 Compose 与鸿蒙 ArkUI 间的交互能力
 *
 * @author gavinbaoliu
 * @since 2024/5/9
 */
sealed interface ArkUIViewController : LifecycleOwner, OnBackPressedDispatcherOwner, ExtraStorageOwner {

    /**
     * Return harmony ArkUI Context
     */
    val context: Context

    /**
     * Return harmony arkUI UIContext
     */
    val uiContext: UIContext

    /**
     * Return the nano time at which the drawing of the view hierarchy started.
     */
    val drawingTime: Long

    /**
     * Return the layout direction.
     */
    val layoutDirection: LayoutDirection
}

/**
 * InternalArkUIViewController，负责桥接 Compose 与鸿蒙 ArkUI 间的交互逻辑，这些 API 被设计为内部的，不可被开发者调用
 */
internal sealed interface InternalArkUIViewController : ArkUIViewController, MessengerOwner, IVsyncProxy {
    val density: Float
    fun aboutToAppear() {}
    fun aboutToDisappear() {}
    fun onPageShow()
    fun onPageHide()
    fun onBackPress(): Boolean = false
    fun onSurfaceCreated(component: OHNativeXComponent, width: Int, height: Int) {}
    fun onSurfaceChanged(width: Int, height: Int) {}
    fun onSurfaceShow() {}
    fun onSurfaceHide() {}
    fun onSurfaceDestroyed() {}
    fun onFrame(timestamp: Long, targetTimestamp: Long) {}
    fun onFocusEvent() {}
    fun onKeyEvent() {}
    fun dispatchTouchEvent(nativeTouchEvent: napi_value, ignoreInteropView: Boolean) = false
    fun dispatchMouseEvent() {}
    fun dispatchHoverEvent() {}
    fun keyboardWillShow(keyboardHeight: Float) {}
    fun keyboardWillHide() {}
    fun invalidate() {}
    fun requestSyncRefresh(): Int
    fun cancelSyncRefresh(refreshId: Int)
    fun onDraw(timestamp: Long, targetTimestamp: Long) {}
    fun onFinalize() {}
}

internal abstract class BasicArkUIViewController(
    coroutineContext: CoroutineContext
) : InternalArkUIViewController {

    private val coroutineScope = CoroutineScope(coroutineContext)
    private val lifecycleRegistry by lazy(LazyThreadSafetyMode.NONE) { LifecycleRegistry(this) }

    // 页面是否处于活跃态，活跃时会触发重组，非活跃时不触发重组
    private var active = true
    private var invalid = false
    private var syncInvalid = false
    private var syncRefreshId = 0
    private val syncRefreshList = ArrayList<Int>()
    private val densityManager by lazy { DensityManager(this) }

    private var isPageShown = false
    private var lastWindowStageEvent: WindowStageEvent? = null
    private var onWindowStageEventDisposable: (() -> Unit)? = null
    private val windowStageManager by lazy { WindowStageManager(this) }

    // Set frameController to null to disable dynamic frame rate, as current implementations will cause performance cracking
    private var frameController: FrameController? = null

    internal var id: String = ""
    internal var env: napi_env? = null
    internal var internalContext: ContextImpl? = null
    internal var internalUiContext: UIContextImpl? = null
    internal var render: COpaquePointer? = null
    internal var backRootView: ArkUIRootView? = null
    internal var foreRootView: ArkUIRootView? = null
    internal var touchableRootView: ArkUIRootView? = null

    internal val requiredEnv: napi_env
        get() = env ?: throw IllegalStateException("napi env is not initialized.")

    internal val requiredBackRootView: ArkUIRootView
        get() = backRootView ?: throw IllegalStateException("back root view is not initialized.")

    internal val requiredForeRootView: ArkUIRootView
        get() = foreRootView ?: throw IllegalStateException("fore root view is not initialized.")

    internal val requiredTouchableRootView: ArkUIRootView
        get() = touchableRootView ?: throw IllegalStateException("touchable root view is not initialized.")

    override val context: Context
        get() = internalContext ?: throw IllegalStateException("context is not initialized.")

    override val uiContext: UIContext
        get() = internalUiContext ?: throw IllegalStateException("ui context is not initialized.")

    override var drawingTime: Long = 0

    override val layoutDirection: LayoutDirection = LayoutDirection.Ltr

    override val lifecycle: Lifecycle get() = lifecycleRegistry

    override val onBackPressedDispatcher: OnBackPressedDispatcher by lazy { PlatformOnBackPressedDispatcher(this) }

    override val messenger: MessengerImpl = MessengerImpl()

    override val extras: ExtraStorage by lazy { DefaultExtraStorage() }

    override val density: Float
        get() = densityManager.getDensity()

    override fun invalidate() {
        invalid = true
        frameController?.requireFrameCallback()
    }

    override fun requestSyncRefresh(): Int {
        val id = ++syncRefreshId
        invalid = true
        syncInvalid = true
        syncRefreshList.add(id)
        frameController?.requireFrameCallback()
        androidx.compose.ui.graphics.kLog("requestSyncRefresh id:" + id + " requestCount:" + syncRefreshList.size)
        return id
    }

    override fun cancelSyncRefresh(refreshId: Int) {
        syncRefreshList.remove(refreshId)
        if (syncRefreshList.isEmpty()) {
            syncInvalid = false
        }
        androidx.compose.ui.graphics.kLog("cancelSyncRefresh id:" + refreshId + " requestCount:" + syncRefreshList.size)
    }

    @CallSuper
    @MainThread
    override fun aboutToAppear() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        onWindowStageEventDisposable = windowStageManager.onWindowStageEvent {
            lastWindowStageEvent = it
            notifyLifecycleEvent()
        }
    }

    @CallSuper
    @MainThread
    override fun aboutToDisappear() {
        onWindowStageEventDisposable?.invoke()
        onWindowStageEventDisposable = null
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    @CallSuper
    @MainThread
    override fun onPageShow() {
        active = true
        androidx.compose.ui.graphics.kLog("ArkUIViewController($id) onPageShow active=$active")
        isPageShown = true
        notifyLifecycleEvent()
    }

    @CallSuper
    @MainThread
    override fun onPageHide() {
        isPageShown = false
        notifyLifecycleEvent()
        active = false
        androidx.compose.ui.graphics.kLog("ArkUIViewController($id) onPageHide active=$active")
    }

    override fun onBackPress(): Boolean =
        onBackPressedDispatcher.onBackPressed()

    @CallSuper
    @MainThread
    override fun onFrame(timestamp: Long, targetTimestamp: Long) {
        frameController?.releaseFrameCallback()
        if (active) {
            if (syncInvalid) {
                onComposeVsync(timestamp)
                onFrameInner(timestamp, targetTimestamp)
            } else {
                coroutineScope.launch {
                    onComposeVsync(timestamp)
                    onFrameInner(timestamp, targetTimestamp)
                }
            }
        }
    }

    private fun onComposeVsync(timestamp: Long) {
        OhosTrace.traceSync("ComposeVsync") {
            ChoreographerManager.onVsync(timestamp)
        }
    }

    private fun onFrameInner(timestamp: Long, targetTimestamp: Long) {
        if (!invalid) {
            return
        }
        invalid = false
        OhosTrace.traceSync("onDraw") {
            prepareDraw()
            drawingTime = timestamp
            onDraw(timestamp, targetTimestamp)
        }

        OhosTrace.traceSync("finishDraw") {
            finishDraw()
        }
    }

    override fun onSurfaceCreated(component: OHNativeXComponent, width: Int, height: Int) {
        super.onSurfaceCreated(component, width, height)
    }

    override fun onSurfaceChanged(width: Int, height: Int) {
        super.onSurfaceChanged(width, height)
    }

    override fun onSurfaceDestroyed() {
        super.onSurfaceDestroyed()
        disposeAnything()
    }

    override fun requireFrameCallback() {
        frameController?.requireFrameCallback()
    }

    override fun isActive(): Boolean = active

    private fun disposeAnything() {
        internalContext = null
        internalUiContext = null
        backRootView?.dispose()
        backRootView = null
        foreRootView?.dispose()
        foreRootView = null
        touchableRootView?.dispose()
        touchableRootView = null
        frameController?.dispose()
        frameController = null
        env = null
    }


    private fun prepareDraw(): Boolean =
        if (render == null) false else androidx_compose_ui_arkui_utils_xcomponent_prepareDraw(render)

    private fun finishDraw(): Boolean =
        if (render == null) false else androidx_compose_ui_arkui_utils_xcomponent_finishDraw(render)

    private fun notifyLifecycleEvent() {
        val windowStageEvent = lastWindowStageEvent
        if (isPageShown && (windowStageEvent == null || windowStageEvent.isForeground)) {
            // 如果页面是显示状态，并且 Window 在前台(或者 Window 信息不可知，默认在前台)，则回调 ON_RESUME 生命周期
            if (lifecycle.currentState != Lifecycle.Event.ON_RESUME.targetState) {
                androidx.compose.ui.graphics.kLog("ArkUIViewController($id) notifyLifecycleEvent ON_RESUME")
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            }
        } else {
            if (lifecycle.currentState != Lifecycle.Event.ON_STOP.targetState) {
                androidx.compose.ui.graphics.kLog("ArkUIViewController($id) notifyLifecycleEvent ON_STOP")
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            }
        }
    }
}

@InternalComposeApi
fun _ArkUIViewController_setId(controllerRef: COpaquePointer, id: String) {
    controllerRef.getController()?.id = id
}

@InternalComposeApi
fun _ArkUIViewController_getId(controllerRef: COpaquePointer): String? =
    controllerRef.getController()?.id

@InternalComposeApi
fun _ArkUIViewController_setEnv(controllerRef: COpaquePointer, env: napi_env) {
    JsEnv.init(env)
    controllerRef.getController()?.env = env
}

@InternalComposeApi
fun _ArkUIViewController_setContext(controllerRef: COpaquePointer, context: napi_value) {
    controllerRef.getController()?.internalContext = ContextImpl(context)
}

@InternalComposeApi
fun _ArkUIViewController_setUIContext(controllerRef: COpaquePointer, uiContext: napi_value) {
    controllerRef.getController()?.internalUiContext = UIContextImpl(uiContext)
}

@InternalComposeApi
fun _ArkUIViewController_setMessenger(controllerRef: COpaquePointer, messenger: napi_value) {
    controllerRef.getController()?.messenger?.bind(RemoteMessengerImpl(messenger))
}

@InternalComposeApi
fun _ArkUIViewController_sendMessage(controllerRef: COpaquePointer, type: String, message: String): String? =
    controllerRef.getController()?.messenger?.handle(type, message)

@InternalComposeApi
fun _ArkUIViewController_setXComponentRender(controllerRef: COpaquePointer, render: COpaquePointer) {
    controllerRef.getController()?.render = render
}

@InternalComposeApi
fun _ArkUIViewController_getXComponentRender(controllerRef: COpaquePointer): COpaquePointer? =
    controllerRef.getController()?.render

@InternalComposeApi
fun _ArkUIViewController_setRootView(
    controllerRef: COpaquePointer,
    backRootView: napi_value,
    foreRootView: napi_value,
    touchableRootView: napi_value
) {
    controllerRef.getController()?.backRootView = ArkUIRootView(backRootView)
    controllerRef.getController()?.foreRootView = ArkUIRootView(foreRootView)
    controllerRef.getController()?.touchableRootView = ArkUIRootView(touchableRootView)
}

@InternalComposeApi
fun _ArkUIViewController_aboutToAppear(controllerRef: COpaquePointer) {
    controllerRef.getController()?.aboutToAppear()
}

@InternalComposeApi
fun _ArkUIViewController_aboutToDisappear(controllerRef: COpaquePointer) {
    controllerRef.getController()?.aboutToDisappear()
}

@InternalComposeApi
fun _ArkUIViewController_onPageShow(controllerRef: COpaquePointer) {
    controllerRef.getController()?.onPageShow()
}

@InternalComposeApi
fun _ArkUIViewController_onPageHide(controllerRef: COpaquePointer) {
    controllerRef.getController()?.onPageHide()
}

@InternalComposeApi
fun _ArkUIViewController_onBackPress(controllerRef: COpaquePointer): Boolean =
    controllerRef.getController()?.onBackPress() ?: false

@InternalComposeApi
fun _ArkUIViewController_onSurfaceCreated(
    controllerRef: COpaquePointer,
    xcomponentPtr: COpaquePointer,
    width: Int,
    height: Int
) {
    controllerRef.getController()?.onSurfaceCreated(xcomponentPtr.reinterpret(), width, height)
}

@InternalComposeApi
fun _ArkUIViewController_onSurfaceChanged(controllerRef: COpaquePointer, width: Int, height: Int) {
    controllerRef.getController()?.onSurfaceChanged(width, height)
}

@InternalComposeApi
fun _ArkUIViewController_onSurfaceShow(controllerRef: COpaquePointer) {
    controllerRef.getController()?.onSurfaceShow()
}

@InternalComposeApi
fun _ArkUIViewController_onSurfaceHide(controllerRef: COpaquePointer) {
    controllerRef.getController()?.onSurfaceHide()
}

@InternalComposeApi
fun _ArkUIViewController_onSurfaceDestroyed(controllerRef: COpaquePointer) {
    controllerRef.getController()?.onSurfaceDestroyed()
    // 取消 DisposableRef 对 BasicArkUIViewController 的引用
    controllerRef.asStableRef<DisposableRef<BasicArkUIViewController>>().get().dispose()
}

@InternalComposeApi
fun _ArkUIViewController_onFrame(controllerRef: COpaquePointer, timestamp: Long, targetTimestamp: Long) {
    OhosTrace.increaseVsyncId()
    OhosTrace.traceSync("VsyncFrame") {
        controllerRef.getController()?.onFrame(timestamp, targetTimestamp)
    }
}

@InternalComposeApi
fun _ArkUIViewController_onFocusEvent(controllerRef: COpaquePointer) {
    controllerRef.getController()?.onFocusEvent()
}

@InternalComposeApi
fun _ArkUIViewController_onKeyEvent(controllerRef: COpaquePointer) {
    controllerRef.getController()?.onKeyEvent()
}

@InternalComposeApi
fun _ArkUIViewController_dispatchTouchEvent(
    controllerRef: COpaquePointer,
    nativeTouchEvent: napi_value,
    ignoreInteropView: Boolean
): Boolean =
    controllerRef.getController()?.dispatchTouchEvent(nativeTouchEvent, ignoreInteropView) ?: false

@InternalComposeApi
fun _ArkUIViewController_dispatchMouseEvent(controllerRef: COpaquePointer) {
    controllerRef.getController()?.dispatchMouseEvent()
}

@InternalComposeApi
fun _ArkUIViewController_dispatchHoverEvent(controllerRef: COpaquePointer) {
    controllerRef.getController()?.dispatchHoverEvent()
}

@InternalComposeApi
fun _ArkUIViewController_keyboardWillShow(controllerRef: COpaquePointer, keyboardHeight: Float) {
    controllerRef.getController()?.keyboardWillShow(keyboardHeight)
}

@InternalComposeApi
fun _ArkUIViewController_keyboardWillHide(controllerRef: COpaquePointer) {
    controllerRef.getController()?.keyboardWillHide()
}

@InternalComposeApi
fun _ArkUIViewController_requestSyncRefresh(controllerRef: COpaquePointer): Int {
    var refreshId = -1
    OhosTrace.traceSync("requestSyncRefresh") {
        refreshId = controllerRef.getController()?.requestSyncRefresh() ?: -1
    }
    return refreshId
}

@InternalComposeApi
fun _ArkUIViewController_cancelSyncRefresh(controllerRef: COpaquePointer, refreshId: Int) {
    OhosTrace.traceSync("cancelSyncRefresh") {
        controllerRef.getController()?.cancelSyncRefresh(refreshId)
    }
}

@InternalComposeApi
fun _ArkUIViewController_onFinalize(controllerRef: COpaquePointer) {
    controllerRef.getController()?.onFinalize()
    // 取消 StableRef 对 DisposableRef 的引用
    controllerRef.asStableRef<DisposableRef<BasicArkUIViewController>>().dispose()
}

// 创建一个 Stable<DisposableRef<BasicArkUIViewController>> 的不透明指针
internal inline fun BasicArkUIViewController.createControllerRef(): COpaquePointer =
    StableRef.create(DisposableRef(this)).asCPointer()

internal inline fun BasicArkUIViewController.createControllerNapiValue(env: napi_env): napi_value =
    androidx_compose_ui_arkui_utils_wrapped(env, createControllerRef()) as napi_value

// 从 Stable<DisposableRef<BasicArkUIViewController>> 取出 Controller
private inline fun COpaquePointer.getController(): BasicArkUIViewController? =
    asStableRef<DisposableRef<BasicArkUIViewController>>().get().get()