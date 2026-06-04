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

package androidx.compose.ui.arkui.messenger

import androidx.annotation.MainThread

/**
 * Interface providing messenger capability for message passing between Kotlin and ArkTS runtime.
 *
 * @author gavinbaoliu
 * @since 2025/3/13
 */
internal interface MessengerOwner {
    val messenger: Messenger
}

/**
 * Messenger interface handling cross-environment communication between Kotlin and ArkUI runtimes.
 *
 * This serialized communication mechanism should not be used in performance-critical scenarios.
 */
internal interface Messenger {

    /**
     * Sends a message to the ArkTS runtime and receives its response.
     *
     * @param type The message type identifier for routing
     * @param message The payload data to send
     * @return Response from the ArkTS runtime
     */
    @MainThread
    fun send(type: String, message: String): String?

    /**
     * Registers a handler for incoming messages from ArkTS runtime.
     *
     * @param type The message type identifier to handle
     * @param receiver Callback function that processes the message and returns a response
     */
    @MainThread
    fun onReceive(type: String, receiver: (message: String) -> String)
}

internal inline fun Messenger.send(type: String): String? = send(type, "")

internal inline fun Messenger.onReceive(type: String, noinline receiver: () -> Unit) =
    onReceive(type) { receiver();"" }

internal inline fun Messenger.onReceive(type: String, noinline receiver: (message: String) -> Unit) =
    onReceive(type) { receiver(it);"" }

internal class MessengerImpl : Messenger, RemoteMessenger {

    private var remote: RemoteMessenger? = null
    private val receivers: MutableMap<String, (String) -> String> = hashMapOf()

    internal fun bind(remote: RemoteMessenger) {
        this.remote = remote
    }

    override fun send(type: String, message: String): String? =
        remote?.handle(type, message)

    override fun handle(type: String, message: String): String? =
        receivers[type]?.invoke(message)

    override fun onReceive(type: String, receiver: (message: String) -> String) {
        receivers[type] = receiver
    }
}
