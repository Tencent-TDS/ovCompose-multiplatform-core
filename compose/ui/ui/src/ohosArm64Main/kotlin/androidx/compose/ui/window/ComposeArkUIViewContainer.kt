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
import androidx.compose.ui.arkui.BasicArkUIViewController
import androidx.compose.ui.arkui.extra.get
import androidx.compose.ui.extention.DelicateComposeApi
import androidx.compose.ui.extention.GlobalContentScope
import androidx.compose.ui.interop.ArkUIInteropContext
import androidx.compose.ui.interop.LocalArkUIInteropContext
import androidx.compose.ui.interop.LocalBackArkUINativeInteropContainer
import androidx.compose.ui.interop.LocalBackInteropContainer
import androidx.compose.ui.interop.LocalForeArkUINativeInteropContainer
import androidx.compose.ui.interop.LocalForeInteropContainer
import androidx.compose.ui.interop.LocalTouchableArkUINativeInteropContainer
import androidx.compose.ui.interop.LocalTouchableInteropContainer
import androidx.compose.ui.interop.OhosTrace
import androidx.compose.ui.node.TrackInteropContainer
import androidx.compose.ui.platform.InterfaceOrientation
import androidx.compose.ui.platform.LocalArkUIViewController
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInterfaceOrientation
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalNapiEnv
import androidx.compose.ui.platform.LocalPlatformInsetsHolder
import androidx.compose.ui.platform.LocalUIContext
import androidx.compose.ui.platform.MainDispatcherFactory
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformInsetsHolder
import androidx.compose.ui.platform.PlatformWindowContext
import androidx.compose.ui.platform.accessibility.OHNativeXComponent
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.scene.ComposeSceneContext
import androidx.compose.ui.scene.ComposeSceneLayer
import androidx.compose.ui.scene.ComposeSceneMediator
import androidx.compose.ui.scene.MultiLayerComposeScene
import androidx.compose.ui.render.RenderStrategy
import androidx.compose.ui.scene.SingleLayerComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import platform.ohos.napi_value

private val coroutineDispatcher = Dispatchers.Main
private const val XC_ASYNC_CREATE = "xc_async_create"

@OptIn(InternalComposeApi::class, InternalComposeUiApi::class, ExperimentalComposeApi::class)
internal class ComposeArkUIViewContainer(
    private val configuration: ComposeArkUIViewControllerConfiguration,
    private val content: @Composable () -> Unit,
) : BasicArkUIViewController(configuration, coroutineDispatcher) {

    private val windowContext = PlatformWindowContext().apply {
        setWindowFocused(true)
    }
    internal val interopContext: ArkUIInteropContext by lazy {
        ArkUIInteropContext(requestRedraw = { invalidate() })
    }

    private var mediator: ComposeSceneMediator? = null
    internal val interfaceOrientationState: MutableState<InterfaceOrientation> = mutableStateOf(
        InterfaceOrientation.Portrait
    )
    internal val systemThemeState: MutableState<SystemTheme> = mutableStateOf(SystemTheme.Unknown)

    internal val insetsHolder by lazy {
        PlatformInsetsHolder(context, lifecycle)
    }

    override fun onSurfaceCreated(component: OHNativeXComponent, width: Int, height: Int) {
        super.onSurfaceCreated(component, width, height)
        val asyncCreate = extras.get(XC_ASYNC_CREATE) == "true"
        if (asyncCreate) {
            coroutineScope.launch { onSurfaceCreatedInner(component, width, height) }
        } else {
            onSurfaceCreatedInner(component, width, height)
        }
    }

    private fun onSurfaceCreatedInner(component: OHNativeXComponent, width: Int, height: Int) {
        if (isSurfaceActive) {
            OhosTrace.traceSync("onSurfaceCreatedInner") {
                createMediatorIfNeeded(component)
                setMediaSize(width, height)
                windowContext.setContainerSize(IntSize(width, height))
            }
        }
    }

    override fun onSurfaceChanged(width: Int, height: Int) {
        super.onSurfaceChanged(width, height)
        androidx.compose.ui.graphics.kLog("onSurfaceChanged width:$width height:$height")
        OhosTrace.traceSync("KmmOnSurfaceChanged $width*$height") {
            if (isSurfaceActive) {
                setMediaSize(width, height)
                windowContext.setContainerSize(IntSize(width, height))
                invalidate()
            }
        }
    }

    override fun onSurfaceDestroyed() {
        super.onSurfaceDestroyed()
        mediator?.disposeSurface()
    }

    override fun aboutToDisappear() {
        // mediator?.dispose() 要先于 aboutToDisappear 执行，目的是为了执行 processInteropActions
        mediator?.dispose()
        mediator = null
        super.aboutToDisappear()
    }

    override fun onDraw(timestamp: Long, targetTimestamp: Long, renderStrategy: RenderStrategy) {
        if (isSurfaceActive) {
            mediator?.onDraw(id, timestamp, targetTimestamp, renderStrategy)
        }
    }

    override fun dispatchTouchEvent(
        nativeTouchEvent: napi_value,
        ignoreInteropView: Boolean
    ): Boolean {
        if (isSurfaceActive) {
            return mediator?.sendPointerEvent(requiredEnv, nativeTouchEvent) ?: false
        }
        return true
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

    private fun createMediatorIfNeeded(component: OHNativeXComponent) {
        if (mediator == null && isSurfaceActive) {
            mediator = createMediator(component)
        }
    }

    private fun setMediaSize(width: Int, height: Int) {
        mediator?.setSize(width, height)
        renderStrategy.setSize(width, height)
    }

    private fun createMediator(component: OHNativeXComponent): ComposeSceneMediator {
        val mediator = ComposeSceneMediator(
            controller = this,
            configuration = configuration,
            interopContext = interopContext,
            windowContext = windowContext,
            coroutineContext = MainDispatcherFactory.getDispatcher(),
            component = component,
            composeSceneFactory = ::createComposeScene
        )
        mediator.setContent {
            ProvideContainerCompositionLocals(this) {
                requiredBackRootView.TrackInteropContainer {
                    requiredForeRootView.TrackInteropContainer {
                        requiredTouchableRootView.TrackInteropContainer {
                            backNativeInteropContainer.TrackInteropContainer {
                                foreNativeInteropContainer.TrackInteropContainer {
                                    touchableNativeInteropContainer.TrackInteropContainer {
                                        content()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return mediator
    }

    @OptIn(InternalComposeUiApi::class)
    private fun createComposeScene(
        invalidate: () -> Unit,
        platformContext: PlatformContext,
        coroutineContext: CoroutineContext,
    ): ComposeScene = if (configuration.platformLayers) {
        SingleLayerComposeScene(
            density = Density(density),
            layoutDirection = layoutDirection,
            coroutineContext = coroutineContext,
            composeSceneContext = ComposeSceneContextImpl(platformContext),
            invalidate = invalidate
        )
    } else {
        MultiLayerComposeScene(
            density = Density(density),
            layoutDirection = layoutDirection,
            coroutineContext = coroutineContext,
            composeSceneContext = ComposeSceneContextImpl(platformContext),
            invalidate = invalidate
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
    composeContainer: ComposeArkUIViewContainer,
    content: @Composable () -> Unit,
) = with(composeContainer) {
    CompositionLocalProvider(
        LocalArkUIViewController provides this,
        LocalLifecycleOwner provides this,
        LocalNapiEnv provides requiredEnv,
        LocalContext provides context,
        LocalUIContext provides uiContext,
        LocalInterfaceOrientation provides interfaceOrientationState.value,
        LocalBackInteropContainer provides requiredBackRootView,
        LocalForeInteropContainer provides requiredForeRootView,
        LocalTouchableInteropContainer provides requiredTouchableRootView,
        LocalBackArkUINativeInteropContainer provides backNativeInteropContainer,
        LocalForeArkUINativeInteropContainer provides foreNativeInteropContainer,
        LocalTouchableArkUINativeInteropContainer provides touchableNativeInteropContainer,
        LocalArkUIInteropContext provides interopContext,
        LocalSystemTheme provides systemThemeState.value,
        LocalPlatformInsetsHolder provides insetsHolder,
        content = { GlobalContentScope.content(content) }
    )
}