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

package androidx.compose.ui.render

import androidx.compose.ui.interop.ArkUIInteropContext

object RenderStrategyFactory {

    enum class RenderType {
        RenderTypeDirect,
        RenderTypePicture,
        RenderTypePictureAsync,
    }

    data class StrategyConfig(
        var interopContext: ArkUIInteropContext? = null
    )

    /**
     * 临时性的变量，异步渲染在部分情况下需要关闭，为了快速在业务中验证，可以在业务中限制起作用的场景。
     * 只针对异步策略生效，可以暂时关闭。
     */
    var forceMainRender = false

    private val DEFAULT_RENDER_TYPE = RenderType.RenderTypeDirect
    private val strategyRegistry = mapOf(
        RenderType.RenderTypeDirect to { RenderStrategyDirect() },
        RenderType.RenderTypePicture to { RenderStrategyPicture() },
        RenderType.RenderTypePictureAsync to { RenderStrategyPictureAsync() }
    )

    /**
     * 先设置为 RenderTypeDirect，后续待其他模式优化完性能，再调整为可配置。
     */
    internal fun getRender(
        renderType: RenderType = DEFAULT_RENDER_TYPE
    ): RenderStrategy {
        return strategyRegistry[renderType]?.invoke()
            ?: throw IllegalArgumentException("Unknown render type: $renderType")
    }
}