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
import androidx.compose.ui.arkui.backhandler.PlatformOnBackPressedDispatcher
import androidx.compose.ui.arkui.density.DensityManager
import androidx.compose.ui.arkui.extra.DefaultExtraStorage
import androidx.compose.ui.arkui.extra.ExtraStorage
import androidx.compose.ui.arkui.frame.FrameController
import androidx.compose.ui.arkui.messenger.MessengerImpl
import androidx.compose.ui.arkui.messenger.MessengerOwner
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_wrapped
import androidx.compose.ui.arkui.window.WindowStageEvent
import androidx.compose.ui.arkui.window.WindowStageManager
import androidx.compose.ui.arkui.window.isForeground
import androidx.compose.ui.interop.arkc.ArkUINativeInteropContainer
import androidx.compose.ui.napi.DisposableRef
import androidx.compose.ui.platform.Context
import androidx.compose.ui.platform.ContextImpl
import androidx.compose.ui.platform.IVsyncProxy
import androidx.compose.ui.platform.UIContext
import androidx.compose.ui.platform.UIContextImpl
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.ComposeArkUIViewControllerConfiguration
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.asStableRef
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.skia.Canvas
import platform.arkui.ARKUI_ALIGNMENT_TOP_START
import platform.ohos.napi_env
import platform.ohos.napi_value
import kotlin.coroutines.CoroutineContext

/**
 * InternalArkUIViewController，负责桥接 Compose 与鸿蒙 ArkUI 间的交互逻辑，这些 API 被设计为内部的，不可被开发者调用
 */
internal sealed interface InternalArkUIViewControllerArkUIBacked : InternalArkUIViewController,
    MessengerOwner, IVsyncProxy {

    fun initRenderContext(
        enableCApi: Boolean,
        importFrameMgr: napi_value,
        rootContent: napi_value?
    )

    fun onIdle(timeLeft: Long) {}
    fun draw(canvas: napi_value) {}
    fun getJsNode(): napi_value?
    fun onDraw(canvas: Canvas?, timestamp: Long, targetTimestamp: Long) {}
}

internal abstract class BasicArkUIViewControllerArkUIBacked(
    private val configuration: ComposeArkUIViewControllerConfiguration,
    coroutineContext: CoroutineContext
) : InternalArkUIViewControllerArkUIBacked {

    protected val coroutineScope = CoroutineScope(coroutineContext)
    private val lifecycleRegistry by lazy(LazyThreadSafetyMode.NONE) { LifecycleRegistry(this) }

    private var invalid = false

    // forceInvalid 即使在 active 为 false 的情况下，也会执行 draw，用于 Surface 重新创建时，立即渲染历史画面
    private var forceInvalid = false
    private var syncInvalid = false
    private var syncRefreshId = 0
    private val syncRefreshList = ArrayList<Int>()
    private val densityManager by lazy { DensityManager(this) }

    private var isPageShown = false

    protected var isPageActive = true
        private set(value) {
            if (field != value) {
                field = value
                if (field) invalidate()
            }
        }

    private var lastWindowStageEvent: WindowStageEvent? = null
    private var onWindowStageEventDisposable: (() -> Unit)? = null
    private val windowStageManager by lazy { WindowStageManager(this) }

    // Set frameController to null to disable dynamic frame rate, as current implementations will cause performance cracking
    private var frameController: FrameController? = null

    override var id: String = ""
    override var env: napi_env? = null
    override var internalContext: ContextImpl? = null
    override var internalUiContext: UIContextImpl? = null

    override var backRootView: ArkUIRootView? = null
    override var foreRootView: ArkUIRootView? = null
    override var touchableRootView: ArkUIRootView? = null
    override val backNativeInteropContainer: ArkUINativeInteropContainer? = null
    override val foreNativeInteropContainer: ArkUINativeInteropContainer? = null
    override val touchableNativeInteropContainer: ArkUINativeInteropContainer? = null

    internal val requiredEnv: napi_env
        get() = env ?: throw IllegalStateException("napi env is not initialized.")

    override val context: Context
        get() = internalContext ?: throw IllegalStateException("context is not initialized.")

    override val uiContext: UIContext
        get() = internalUiContext ?: throw IllegalStateException("ui context is not initialized.")

    override var drawingTime: Long = 0

    override val layoutDirection: LayoutDirection = LayoutDirection.Ltr

    override val lifecycle: Lifecycle get() = lifecycleRegistry

    override val onBackPressedDispatcher: OnBackPressedDispatcher by lazy {
        PlatformOnBackPressedDispatcher(
            this
        )
    }

    override val messenger: MessengerImpl = MessengerImpl()

    override val extras: ExtraStorage by lazy { DefaultExtraStorage() }

    override val density: Float
        get() = densityManager.getDensity()

    override var isSurfaceActive: Boolean = false

    private var isComponentActive = false

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
        configuration.delegate.aboutToAppear()
        isComponentActive = true
    }

    @CallSuper
    @MainThread
    override fun aboutToDisappear() {
        isComponentActive = false
        configuration.delegate.aboutToDisappear()
        disposeAnything()
        onWindowStageEventDisposable?.invoke()
        onWindowStageEventDisposable = null
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    @CallSuper
    @MainThread
    override fun onPageShow() {
        androidx.compose.ui.graphics.kLog("ArkUIViewController($id) onPageShow")
        isPageActive = true
        isPageShown = true
        notifyLifecycleEvent()
        configuration.delegate.onPageShow()
    }

    @CallSuper
    @MainThread
    override fun onPageHide() {
        configuration.delegate.onPageHide()
        isPageShown = false
        notifyLifecycleEvent()
        isPageActive = false
        androidx.compose.ui.graphics.kLog("ArkUIViewController($id) onPageHide")
    }

    override fun onBackPress(): Boolean =
        onBackPressedDispatcher.onBackPressed()

    override fun onSurfaceDestroyed() {
        isSurfaceActive = false
        configuration.delegate.onSurfaceDestroyed()
        super.onSurfaceDestroyed()
    }

    override fun requireFrameCallback() {
        frameController?.requireFrameCallback()
    }

    private fun disposeAnything() {
        internalContext = null
        internalUiContext = null
        frameController?.dispose()
        frameController = null
        env = null
    }

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

    private fun nativeInteropContainer() = ArkUINativeInteropContainer().apply {
        widthPercent = 1f
        heightPercent = 1f
        alignment = ARKUI_ALIGNMENT_TOP_START
    }
}

@InternalComposeApi
fun _ArkUIViewController_initRenderContext(
    controllerRef: COpaquePointer,
    enableCApi: Boolean,
    importFrameMgr: napi_value,
    rootContent: napi_value?
) {
    controllerRef.getController()?.initRenderContext(enableCApi, importFrameMgr, rootContent)
}

@InternalComposeApi
fun _ArkUIViewController_onIdle(controllerRef: COpaquePointer, timeLeft: Long) {
    controllerRef.getController()?.onIdle(timeLeft)
}

@InternalComposeApi
fun _ArkUIViewController_draw(controllerRef: COpaquePointer, canvas: napi_value) {
    controllerRef.getController()?.draw(canvas)
}

@InternalComposeApi
fun _ArkUIViewController_getJsNode(controllerRef: COpaquePointer): napi_value? {
    return controllerRef.getController()?.getJsNode()
}

// 创建一个 Stable<DisposableRef<BasicArkUIViewController>> 的不透明指针
internal inline fun BasicArkUIViewControllerArkUIBacked.createControllerRef(): COpaquePointer =
    StableRef.create(DisposableRef(this)).asCPointer()

internal inline fun BasicArkUIViewControllerArkUIBacked.createControllerNapiValue(env: napi_env): napi_value =
    androidx_compose_ui_arkui_utils_wrapped(env, createControllerRef()) as napi_value

// 从 Stable<DisposableRef<BasicArkUIViewController>> 取出 Controller
private inline fun COpaquePointer.getController(): BasicArkUIViewControllerArkUIBacked? =
    asStableRef<DisposableRef<BasicArkUIViewControllerArkUIBacked>>().get().get()