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

package androidx.compose.ui.arkui.window

import androidx.compose.ui.arkui.messenger.MessengerOwner
import androidx.compose.ui.arkui.messenger.onReceive

/**
 * Window Stage Manager
 *
 * @author gavinbaoliu
 * @since 2025/4/7
 */
internal class WindowStageManager(
    messengerOwner: MessengerOwner
) : MessengerOwner by messengerOwner {

    private val onWindowStageEventBlocks: MutableList<(WindowStageEvent) -> Unit> = mutableListOf()

    init {
        messenger.onReceive(RECEIVE_ON_WINDOW_STAGE_EVENT, ::onReceiveWindowStageEvent)
    }

    fun onWindowStageEvent(block: (WindowStageEvent) -> Unit): () -> Unit {
        onWindowStageEventBlocks.add(block)
        return { onWindowStageEventBlocks.remove(block) }
    }

    private fun onReceiveWindowStageEvent(message: String) {
        WindowStageEvent.valueOfOrNull(message)?.let { event ->
            onWindowStageEventBlocks.forEach { it(event) }
        }
    }

    companion object {
        private const val RECEIVE_ON_WINDOW_STAGE_EVENT =
            "compose.ui:WindowStage.onWindowStageEvent"
    }
}

internal enum class WindowStageEvent(val value: String) {
    SHOWN("SHOWN"), HIDDEN("HIDDEN"), RESUMED("RESUMED"), PAUSED("PAUSED");

    companion object {
        fun valueOfOrNull(value: String): WindowStageEvent? =
            WindowStageEvent.entries.find { it.value == value }
    }
}

internal val WindowStageEvent.isForeground
    get() = this == WindowStageEvent.SHOWN || this == WindowStageEvent.RESUMED