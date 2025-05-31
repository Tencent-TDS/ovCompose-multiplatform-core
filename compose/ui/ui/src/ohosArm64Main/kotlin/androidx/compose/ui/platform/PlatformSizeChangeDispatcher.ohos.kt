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

internal class PlatformSizeChangeDispatcher(private val messenger: Messenger) {


    fun onComposeSizeChange(id: String, width: Int, height: Int) {
        messenger.send(
            type = SEND_COMPOSE_SIZE_CHANGE, message = makeMessage(id, width, height)
        )
    }

    private fun makeMessage(id: String, width: Int, height: Int): String = buildString {
        append(width).append("#").append(height)
    }

    companion object {
        const val SEND_COMPOSE_SIZE_CHANGE = "compose.ui.ComposeSizeChange"
    }
}