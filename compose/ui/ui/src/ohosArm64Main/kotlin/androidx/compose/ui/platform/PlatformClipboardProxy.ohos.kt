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

import androidx.compose.ui.arkui.messenger.Messenger
import androidx.compose.ui.arkui.messenger.send
import org.jetbrains.skiko.IClipboardProxy

/**
 * Platform Clipboard Proxy
 *
 * @author gavinbaoliu
 * @since 2025/3/18
 */
internal class PlatformClipboardProxy(private val messenger: Messenger) : IClipboardProxy {

    override fun setText(text: String) {
        messenger.send(SEND_SET_TEXT, text)
    }

    override fun getText(): String? =
        messenger.send(SEND_GET_TEXT)

    override fun hasText(): Boolean =
        messenger.send(SEND_HAS_TEXT) == TRUE

    companion object {
        const val TRUE = "true"
        const val SEND_SET_TEXT = "compose.ui.Clipboard:setText"
        const val SEND_GET_TEXT = "compose.ui.Clipboard:getText"
        const val SEND_HAS_TEXT = "compose.ui.Clipboard:hasText"
    }
}