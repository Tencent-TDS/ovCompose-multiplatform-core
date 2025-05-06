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

package androidx.compose.ui.platform

import androidx.compose.ui.text.input.PlatformTextInputService
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

/**
 * PlatformContextImpl
 *
 * @author gavinbaoliu
 * @since 2024/5/14
 */
internal class PlatformContextImpl(
    override val windowInfo: WindowInfo,
    override val textInputService: PlatformTextInputService,
    override val textToolbar: TextToolbar,
    override val semanticsOwnerListener: PlatformContext.SemanticsOwnerListener?,
    densityProvider: () -> Density,
) : PlatformContext by PlatformContext.Empty {

    override val viewConfiguration = object : ViewConfiguration by super.viewConfiguration {

        override val maximumFlingVelocity: Float
            get() = with(densityProvider()) { MAXIMUM_FLING_VELOCITY.toPx() }
    }

    companion object {
        // android.view.ViewConfiguration#MAXIMUM_FLING_VELOCITY = 8000dp.
        // 参考iOS fling体验， 低初速度，低摩擦力
        private val MAXIMUM_FLING_VELOCITY = 5000.dp
    }
}