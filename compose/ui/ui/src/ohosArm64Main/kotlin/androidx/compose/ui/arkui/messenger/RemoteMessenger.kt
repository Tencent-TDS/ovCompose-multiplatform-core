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

import androidx.compose.ui.napi.JsEnv
import androidx.compose.ui.napi.NapiValue
import androidx.compose.ui.napi.NapiValueImpl
import androidx.compose.ui.napi.asString
import androidx.compose.ui.napi.nApiValue
import platform.ohos.napi_value

/**
 * A proxy for a remote messenger
 *
 * @author gavinbaoliu
 * @since 2025/3/14
 */
internal interface RemoteMessenger {
    fun handle(type: String, message: String): String?
}

internal class RemoteMessengerImpl(value: napi_value) : RemoteMessenger, NapiValue by NapiValueImpl(value) {
    override fun handle(type: String, message: String): String? {
        val handleFunction = JsEnv.getNamedProperty(rawValue, "handle")
        return JsEnv.callFunction(rawValue, handleFunction, type.nApiValue(), message.nApiValue()).asString()
    }
}