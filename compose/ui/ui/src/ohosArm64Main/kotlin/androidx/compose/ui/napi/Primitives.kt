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

package androidx.compose.ui.napi

import kotlinx.cinterop.COpaquePointer
import platform.ohos.napi_value

inline fun String.nApiValue(): napi_value? =
    JsEnv.createStringUtf8(this)

inline fun Boolean.nApiValue(): napi_value? =
    JsEnv.getBoolean(this)

inline fun Int.nApiValue(): napi_value? =
    JsEnv.createInt32(this)

inline fun Long.nApiValue(): napi_value? =
    JsEnv.createInt64(this)

inline fun Float.nApiValue(): napi_value? =
    JsEnv.createFloat(this)

inline fun Double.nApiValue(): napi_value? =
    JsEnv.createDouble(this)

@Suppress("UNCHECKED_CAST")
fun Any?.nApiValue(): napi_value? {
    return when (this) {
        null -> null
        is COpaquePointer -> this as? napi_value
        is Int -> this.nApiValue()
        is Float -> this.nApiValue()
        is Double -> this.nApiValue()
        is String -> this.nApiValue()
        is Boolean -> this.nApiValue()
        is Unit -> JsEnv.getUndefined()
        else -> throw IllegalArgumentException("Unsupported type: ${this::class}")
    }
}