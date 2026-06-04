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

package androidx.compose.ui.arkui.density

import androidx.compose.ui.arkui.messenger.MessengerOwner
import androidx.compose.ui.arkui.messenger.send

/**
 * Density Manager
 *
 * @author gavinbaoliu
 * @since 2025/3/27
 */
internal class DensityManager(
    messengerOwner: MessengerOwner
) : MessengerOwner by messengerOwner {

    fun getDensity(): Float {
        val result = messenger.send(SEND_GET_DENSITY)
        if (result == null) {
            androidx.compose.ui.graphics.kLog("DensityManager:getDensity failed to invoke getDensity().")
        }
        val density = result?.toFloatOrNull()
        if (density == null) {
            androidx.compose.ui.graphics.kLog("DensityManager:getDensity failed to parse density: $result")
        }
        return density ?: 1f // Use 1f as default
    }

    companion object {
        private const val SEND_GET_DENSITY = "compose.ui:Density.getDensity"
    }
}