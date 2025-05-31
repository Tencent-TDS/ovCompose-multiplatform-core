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
     * 是否使用平台化的 Layer
     *
     * 如果为 true 则使用 [SingleLayerComposeScene] 构建 Scene，调用平台化能力构建新的 Layer
     * 如果为 false 则使用 [MultiLayerComposeScene] 构建 Scene，其内部实现了多 Layer 的能力
     */
    @ExperimentalComposeApi
    internal var platformLayers: Boolean = false

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