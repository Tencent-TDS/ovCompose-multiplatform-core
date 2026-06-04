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

package androidx.compose.ui.platform.v2

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.node.OwnedLayer
import androidx.compose.ui.node.OwnedLayerFactory
import androidx.compose.ui.uikit.ExperimentalConfig
import androidx.compose.ui.unit.Density
import platform.UIKit.UIView

class UIViewLayerFactory(
    private val rootView: UIView,
    private val experimentalConfig: ExperimentalConfig?,
    private val clipChildren: Boolean = true
) : OwnedLayerFactory() {

    override fun createLayer(
        density: Density,
        drawBlock: (Canvas) -> Unit,
        invalidateParentLayer: () -> Unit,
        onDestroy: () -> Unit
    ): OwnedLayer {
        return UIViewLayer(
            density,
            invalidateParentLayer,
            drawBlock,
            onDestroy,
            rootView,
            experimentalConfig,
            clipChildren
        )
    }
}