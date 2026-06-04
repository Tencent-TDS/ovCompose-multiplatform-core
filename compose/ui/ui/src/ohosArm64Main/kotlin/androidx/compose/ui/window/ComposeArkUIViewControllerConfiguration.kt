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

import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.ui.render.RenderStrategyFactory
import androidx.compose.ui.scene.MultiLayerComposeScene
import androidx.compose.ui.scene.SingleLayerComposeScene

/**
 * ComposeArkViewController 的配置类，负责配置 ComposeArkViewController 以及从其中接受一些信息
 *
 * @author gavinbaoliu
 * @since 2024/5/9
 */
class ComposeArkUIViewControllerConfiguration {

    /**
     * ComposeArkUIViewController 代理；用来接收 ComposeArkUIViewController 特定生命事件的回调
     */
    var delegate: ComposeArkUIViewControllerDelegate = object : ComposeArkUIViewControllerDelegate {}

    /**
     * 是否使用平台化的 Layer
     *
     * 如果为 true 则使用 [SingleLayerComposeScene] 构建 Scene，调用平台化能力构建新的 Layer
     * 如果为 false 则使用 [MultiLayerComposeScene] 构建 Scene，其内部实现了多 Layer 的能力
     */
    @ExperimentalComposeApi
    internal var platformLayers: Boolean = false

    /**
     * 渲染模式
     */
    var renderType: RenderStrategyFactory.RenderType =
        RenderStrategyFactory.RenderType.RenderTypeDirect

    /**
     * 抑制内存回收
     */
    @ExperimentalComposeApi
    var internalStartGCSuppressor: () -> Unit = {}

    /**
     * 取消抑制内存回收
     */
    @ExperimentalComposeApi
    var internalStopGCSuppressor: () -> Unit = {}
}

/**
 * ComposeArkUIViewController 代理，用来接收 ComposeArkUIViewController 特定生命事件的回调
 */
interface ComposeArkUIViewControllerDelegate {
    /**
     * ArkUI Compose 组件 aboutToAppear() 回调时调用；ArkUI Compose 组件构造完后调用
     *
     * 该函数在 ComposeArkUIViewController aboutToAppear() 回调*末尾*调用，意味着 ComposeArkUIViewController 已经做好初始化可以被正常访问
     *
     * ```typescript
     * @Component
     * export struct Compose {
     *   aboutToAppear(): void {}
     * }
     * ```
     */
    fun aboutToAppear() = Unit

    /**
     * ArkUI Compose 组件 aboutToDisappear() 回调时调用；ArkUI Compose 组件即将销毁前调用
     *
     * 该函数在 ComposeArkUIViewController aboutToDisappear() 回调*开头*调用，意味着 ComposeArkUIViewController 可被访问并在之后进行清理
     *
     * ```typescript
     * @Component
     * export struct Compose {
     *   aboutToDisappear(): void {}
     * }
     * ```
     */
    fun aboutToDisappear() = Unit

    /**
     * ArkUI Compose 组件 onPageShow() 回调时调用；每次页面显示都会调用，包括路由过程和页面进入前台
     *
     * 该函数在 ComposeArkUIViewController onPageShow() 回调*末尾*调用
     *
     * ```typescript
     * @Component
     * export struct Compose {
     *   onPageShow(): void {}
     * }
     * ```
     */
    fun onPageShow() = Unit

    /**
     * ArkUI Compose 组件 onPageHide() 回调时调用；每次页面隐藏都会调用，包括路由过程和页面进入后台
     *
     * 该函数在 ComposeArkUIViewController onPageHide() 回调*开头*调用
     *
     * ```typescript
     * @Component
     * export struct Compose {
     *   onPageHide(): void {}
     * }
     * ```
     */
    fun onPageHide() = Unit

    /**
     * ArkUI Compose 组件中 XComponent Surface 创建时调用；由于 XComponent 存在动态插拔，所以可能多次回调
     *
     * 该函数在 ComposeArkUIViewController onSurfaceCreated() 回调*末尾*调用，意味着 Surface 相关内容已经做好初始化可以被正常访问
     *
     * ```typescript
     * @Component
     * export struct Compose {
     *   build(){
     *     Stack() {
     *       if(this.isSurfaceActive){
     *         XComponent()
     *       }
     *     }
     *   }
     * }
     * ```
     */
    fun onSurfaceCreated(width: Int, height: Int) = Unit

    /**
     * ArkUI Compose 组件中 XComponent Surface 变化时调用
     *
     * 该函数在 ComposeArkUIViewController onSurfaceChanged() 回调*末尾*调用
     */
    fun onSurfaceChanged(width: Int, height: Int) = Unit

    /**
     * ArkUI Compose 组件中 XComponent Surface 每帧回调，仅在 XComponent 存在时候回调
     *
     * 该函数在 ComposeArkUIViewController onSurfaceFrame() 回调*末尾*调用
     */
    fun onSurfaceFrame(timestamp: Long, targetTimestamp: Long) = Unit

    /**
     * ArkUI Compose 组件中 XComponent Surface 销毁时调用；由于 XComponent 存在动态插拔，所以可能多次回调
     *
     * 该函数在 ComposeArkUIViewController onSurfaceDestroyed() 回调*开头*调用，意味着 Surface 相关内容可被访问并在之后进行清理
     */
    fun onSurfaceDestroyed() = Unit
}