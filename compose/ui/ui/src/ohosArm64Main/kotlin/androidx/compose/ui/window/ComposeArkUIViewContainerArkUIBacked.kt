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

package androidx.compose.ui.window

import androidx.annotation.CallSuper
import androidx.annotation.MainThread
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeTabService
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.LocalSystemTheme
import androidx.compose.ui.SystemTheme
import androidx.compose.ui.arkui.ArkUIView
import androidx.compose.ui.arkui.BasicArkUIViewControllerArkUIBacked
import androidx.compose.ui.extention.DelicateComposeApi
import androidx.compose.ui.extention.GlobalContentScope
import androidx.compose.ui.graphics.kLog
import androidx.compose.ui.interop.ArkUIInteropContext
import androidx.compose.ui.interop.LocalArkUIInteropContext
import androidx.compose.ui.interop.arkc.ArkUINativeView
import androidx.compose.ui.interop.arkc.clone
import androidx.compose.ui.napi.JsEnv
import androidx.compose.ui.napi.call
import androidx.compose.ui.napi.nApiValue
import androidx.compose.ui.platform.ChoreographerManager
import androidx.compose.ui.platform.InterfaceOrientation
import androidx.compose.ui.platform.LocalArkUIViewController
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalEnableCApi
import androidx.compose.ui.platform.LocalFrameManager
import androidx.compose.ui.platform.LocalInterfaceOrientation
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalNapiEnv
import androidx.compose.ui.platform.LocalPlatformInsetsHolder
import androidx.compose.ui.platform.LocalRootNodeId
import androidx.compose.ui.platform.LocalUIContext
import androidx.compose.ui.platform.MainDispatcherFactory
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformInsetsHolder
import androidx.compose.ui.platform.PlatformWindowContext
import androidx.compose.ui.render.RenderStrategy
import androidx.compose.ui.render.RenderStrategyFactory
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.scene.ComposeSceneContext
import androidx.compose.ui.scene.ComposeSceneLayer
import androidx.compose.ui.scene.ComposeSceneMediatorArkUIBacked
import androidx.compose.ui.scene.MultiLayerComposeScene
import androidx.compose.ui.scene.SingleLayerComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.trace
import kotlin.coroutines.CoroutineContext
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.skia.Canvas
import org.jetbrains.skiko.ArkTsContext
import org.jetbrains.skiko.JsRenderNode
import org.jetbrains.skiko.CRenderNode
import org.jetbrains.skiko.RenderNode
import platform.arkui.OH_ArkUI_GetNodeContentFromNapiValue
import platform.arkui.OH_ArkUI_NodeContent_AddNode
import platform.ohos.ArkUI_NodeContentHandleVar
import platform.ohos.napi_env
import platform.ohos.napi_ref
import platform.ohos.napi_value

private val coroutineDispatcher = Dispatchers.Main
private const val XC_ASYNC_CREATE = "xc_async_create"

private val VIEW_MAP = HashMap<Int, ComposeArkUIViewContainerArkUIBacked>()

private const val TAG = "RenderNodeUIView"

fun RenderNodeDrawCallBack(id: Int, canvas: COpaquePointer) {
    val renderNodeUIView: ComposeArkUIViewContainerArkUIBacked = VIEW_MAP[id] ?: run {
        kLog("RenderNodeDrawCallBack: no renderNode with id:$id")
        return
    }
    renderNodeUIView.onDraw(Canvas(canvas))
}

fun initJsRenderNodeContext(env: napi_env, nodeConstructor: napi_value, statusModifyConstructor: napi_value, ratio: Double, fixed: Boolean) {
    ArkTsContext.initEnv(env)
    JsRenderNode.initJsRenderNodeContext(nodeConstructor, statusModifyConstructor, staticCFunction(::RenderNodeDrawCallBack))
    JsRenderNode.setPixelRatio(ratio)
    CRenderNode.setCAPIFixed(fixed)
}

@OptIn(InternalComposeApi::class, InternalComposeUiApi::class, ExperimentalComposeApi::class)
internal class ComposeArkUIViewContainerArkUIBacked(
    private val configuration: ComposeArkUIViewControllerConfiguration,
    private val content: @Composable () -> Unit,
) : BasicArkUIViewControllerArkUIBacked(configuration, coroutineDispatcher), RenderNodeView {

    internal var enableCApi: Boolean = false
    internal var frameMgr: FrameManager? = null
    internal val requireFrameMgr: FrameManager get() = requireNotNull(frameMgr){
        "Required frame manager in ComposeArkUIViewContainerArkUIBacked"
    }
    internal var renderNode: RenderNode? = null
    internal val requireRenderNode: RenderNode get() = requireNotNull(renderNode) {
        "Required render node in ComposeArkUIViewContainerArkUIBacked"
    }

    private var active = true
    private var invalid = false
    private var width: Int = 0
    private var height: Int = 0
    private var pendingOnIdle = false
    private var vsyncTimeStamp: Long = 0

    override val renderStrategy: RenderStrategy =
        RenderStrategyFactory.getRender(configuration.renderType)

    private val windowContext = PlatformWindowContext().apply {
        setWindowFocused(true)
    }
    internal val interopContext: ArkUIInteropContext by lazy {
        ArkUIInteropContext(requestRedraw = { invalidate() })
    }

    private var mediator: ComposeSceneMediatorArkUIBacked? = createMediator()
    internal val interfaceOrientationState: MutableState<InterfaceOrientation> = mutableStateOf(
        InterfaceOrientation.Portrait
    )
    internal val systemThemeState: MutableState<SystemTheme> = mutableStateOf(SystemTheme.Unknown)

    internal val insetsHolder by lazy {
        PlatformInsetsHolder(context, lifecycle)
    }

    override fun initRenderContext(
        enableCApi: Boolean,
        importFrameMgr: napi_value,
        rootContent: napi_value?
    ) {
        this.enableCApi = enableCApi
        renderNode = if (enableCApi && rootContent != null) CRenderNode(rootContent) else JsRenderNode()
        frameMgr = importFrameMgr.getFrameManager()
        VIEW_MAP[requireRenderNode.id] = this
        memScoped {
            val nodeContent = alloc<ArkUI_NodeContentHandleVar>()
            OH_ArkUI_GetNodeContentFromNapiValue(requiredEnv, rootContent, nodeContent.ptr)
            OH_ArkUI_NodeContent_AddNode(nodeContent.value, ArkUINativeView().apply {
                widthPercent = 1f
                heightPercent = 1f
                onTouch = {
                   mediator?.nativeEvent = it.clone()
                }
            }.handle)
        }
    }

    override fun onSurfaceChanged(width: Int, height: Int) {
        super.onSurfaceChanged(width, height)
        androidx.compose.ui.graphics.kLog("onSurfaceChanged width:$width height:$height")
        trace("KmmOnSurfaceChanged $width*$height") {
            if (!active) return
            if (this.width == 0 && this.height == 0) {
                mediator?.setContent {
                    ProvideContainerCompositionLocals(this) {
                        content()
                    }
                }
            }
            this.width = width
            this.height = height
            mediator?.setSize(width, height)
            if (enableCApi) {
                (renderNode as CRenderNode).reSize(width, height)
            }
            windowContext.setContainerSize(IntSize(width, height))
            invalidate()
        }
    }

    override fun updateDensity(density: Float) {
        if (!active) return
        val density = if (density > 0) density else this.density
        mediator?.setDensity(density)
        JsRenderNode.setPixelRatio(density.toDouble())
        if (enableCApi) {
            (renderNode as CRenderNode).reSize(width, height)
        }
    }

    override fun isActive(): Boolean = active

    override fun aboutToDisappear() {
        // mediator?.dispose() 要先于 aboutToDisappear 执行，目的是为了执行 processInteropActions
        mediator?.dispose()
        mediator = null
        super.aboutToDisappear()
    }

    override fun dispatchTouchEvent(
        nativeTouchEvent: napi_value,
        ignoreInteropView: Boolean
    ): Boolean {
        if (!active) return true
        return mediator?.sendPointerEvent(requiredEnv, nativeTouchEvent) ?: false
    }

    override fun onAxisEvent(axisEvent: napi_value) {
        if (isSurfaceActive) {
            mediator?.onAxisEvent(axisEvent)
        }
    }

    override fun onKeyEvent(event: napi_value): Boolean {
        return if (ComposeTabService.harmonyKeyEventEnabled && isSurfaceActive) {
            mediator?.onKeyEvent(event) ?: false
        } else {
            false
        }
    }

    override fun onMouseEvent(event: napi_value) {
        if (isSurfaceActive) {
            mediator?.onMouseEvent(event)
        }
    }

    override fun onPinchEvent(event: napi_value) {
        if (isSurfaceActive) {
            mediator?.onPinchEvent(event)
        }
    }

    override fun keyboardWillShow(keyboardHeight: Float) {
        mediator?.keyboardWillShow(keyboardHeight)
    }

    override fun keyboardWillHide() {
        mediator?.keyboardWillHide()
    }

    override fun invalidate() {
        // 统一切回 JS 主线程：SnapshotStateObserver/Recomposer 等可能在 worker 线程触发 invalidate，
        // 而 frameMgr.postFrameCallback 内部走 napi_call_function，必须在创建 napi_env 的主线程执行，
        // 否则 ecma_vm 会触发多线程检测并 abort (SIGABRT)。
        coroutineScope.launch(Dispatchers.Main.immediate) {
            if (!active || !isPageActive || invalid) return@launch
            invalid = true
            frameMgr?.postFrameCallback()
        }
    }

    override fun onDraw(canvas: Canvas) {
        trace("onDraw ${renderNode?.id}") {
            if (!active) return
            ChoreographerManager.onVsync(vsyncTimeStamp)
            mediator?.onDraw(canvas, vsyncTimeStamp)
        }
    }

    override fun draw(canvas: napi_value) {
        (renderNode as? JsRenderNode)?.draw(canvas)
    }

    override fun getJsNode(): napi_value? {
        return (renderNode as? JsRenderNode)?.getJsNode()
    }

    override fun onSurfaceDestroyed() {
        super.onSurfaceDestroyed()
        active = false
        frameMgr?.onDispose()
        frameMgr = null
        mediator?.dispose()
        mediator = null
        VIEW_MAP.remove(renderNode?.id)
    }

    override fun onBackPress(): Boolean =
        onBackPressedDispatcher.onBackPressed()

    @CallSuper
    @MainThread
    override fun onFrame(timeStamp: Long, targetTimestamp: Long) {
        trace("onFrame ${renderNode?.id}") {
            if (!active) return
            pendingOnIdle = true
            if (!invalid) {
                kLog("RenderNodeUIView onFrame: node ${renderNode?.id} is not invalid")
                return
            }
            invalid = false
            vsyncTimeStamp = timeStamp
            renderNode?.notifyRedraw()
        }
    }

    override fun onIdle(timeLeft: Long) {
        if (!active) return
        if (pendingOnIdle) {
            pendingOnIdle = false
        }
    }

    private fun createMediator(): ComposeSceneMediatorArkUIBacked {
        val mediator = ComposeSceneMediatorArkUIBacked(
            controller = this,
            configuration = configuration,
            interopContext = interopContext,
            windowContext = windowContext,
            coroutineContext = MainDispatcherFactory.getDispatcher(),
            composeSceneFactory = ::createComposeScene
        )
        return mediator
    }

    @OptIn(InternalComposeUiApi::class)
    private fun createComposeScene(
        platformContext: PlatformContext,
        coroutineContext: CoroutineContext,
    ): ComposeScene = if (configuration.platformLayers) {
        SingleLayerComposeScene(
            density = Density(density),
            layoutDirection = layoutDirection,
            coroutineContext = coroutineContext,
            composeSceneContext = ComposeSceneContextImpl(platformContext),
            invalidate = this::invalidate
        )
    } else {
        MultiLayerComposeScene(
            density = Density(density),
            layoutDirection = layoutDirection,
            coroutineContext = coroutineContext,
            composeSceneContext = ComposeSceneContextImpl(platformContext),
            invalidate = this::invalidate
        )
    }

    private inner class ComposeSceneContextImpl(
        override val platformContext: PlatformContext
    ) : ComposeSceneContext {

        override fun createPlatformLayer(
            density: Density,
            layoutDirection: LayoutDirection,
            focusable: Boolean,
            compositionContext: CompositionContext
        ): ComposeSceneLayer {
            // 鸿蒙暂不支持创建 PlatformLayer
            throw UnsupportedOperationException("Unsupported create platform layer.")
        }
    }
}

@OptIn(InternalComposeApi::class, DelicateComposeApi::class)
@Composable
internal fun ProvideContainerCompositionLocals(
    composeContainer: ComposeArkUIViewContainerArkUIBacked,
    content: @Composable () -> Unit,
) = with(composeContainer) {
    CompositionLocalProvider(
        LocalArkUIViewController provides this,
        LocalLifecycleOwner provides this,
        LocalNapiEnv provides requiredEnv,
        LocalContext provides context,
        LocalUIContext provides uiContext,
        LocalInterfaceOrientation provides interfaceOrientationState.value,
        LocalFrameManager provides requireFrameMgr,
        LocalEnableCApi provides enableCApi,
        LocalRootNodeId provides requireRenderNode.id,
        LocalArkUIInteropContext provides interopContext,
        LocalSystemTheme provides systemThemeState.value,
        LocalPlatformInsetsHolder provides insetsHolder,
        content = { GlobalContentScope.content(content) }
    )
}

fun napi_value.getFrameManager(): FrameManager {
    return JsImportFrameManager(JsEnv.createReference(this))
}

class JsImportFrameManager(private var instance: napi_ref?): FrameManager {

    override fun postFrameCallback() {
        this.instance.call("postFrameCallback")
    }

    override fun enableFrameCallback(id: Int) {
        this.instance.call("enableFrameCallback", id.nApiValue())
    }

    override fun disableFrameCallback(id: Int) {
        this.instance.call("disableFrameCallback", id.nApiValue())

    }

    override fun buildArkUIView(view: ArkUIView) {
        val arkUIView = this.instance.call("buildView", view.name.nApiValue(), view.parameter.jsValue)
        trace("bindJs") {
            view.bindJs(arkUIView)
        }
    }

    override fun buildNativeView(): napi_value? {
        val nativeView = this.instance.call("buildNativeView")
        return nativeView
    }

    override fun onDispose() {
        this.instance.call("onDispose")
        JsEnv.deleteReference(this.instance)
        this.instance = null
    }

}

interface RenderNodeView {
    fun onSurfaceDestroyed()
    fun onBackPress(): Boolean = false
    fun invalidate()
    fun onDraw(canvas: Canvas)
    fun draw(canvas: napi_value)
    fun getJsNode(): napi_value?
    fun onSurfaceChanged(width: Int, height: Int)
}

interface FrameManager {
    fun postFrameCallback()
    fun enableFrameCallback(id: Int)
    fun disableFrameCallback(id: Int)
    fun buildArkUIView(view: ArkUIView)
    fun buildNativeView(): napi_value?
    fun onDispose()
}