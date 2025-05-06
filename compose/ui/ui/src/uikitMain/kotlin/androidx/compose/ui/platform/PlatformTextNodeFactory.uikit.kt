package androidx.compose.ui.platform

import androidx.compose.runtime.EnableIosRenderLayerV2

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

actual class PlatformTextNodeFactory {

    /**
     * 返回[IOSTextNode]代理文本测量、绘制、更新的操作
     */
    actual fun createPlatformDelegateTextNode(): PlatformTextNode? = if (EnableIosRenderLayerV2) {
        IOSTextNode()
    } else {
        null
    }

    actual companion object {
        actual val instance: PlatformTextNodeFactory = PlatformTextNodeFactory()
    }
}