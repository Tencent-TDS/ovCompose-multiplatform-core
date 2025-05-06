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

import platform.ohos.napi_value

class JsArray(jsValue: napi_value?) : JsObject(jsValue), Iterable<napi_value?> {

    val size: Int
        get() = JsEnv.getArrayLength(jsValue)

    operator fun get(index: Int): napi_value? = JsEnv.getElement(jsValue, index)

    operator fun set(index: Int, value: napi_value?) {
        JsEnv.setElement(jsValue, index, value)
    }

    override fun iterator(): Iterator<napi_value?> {
        return object : Iterator<napi_value?> {
            val size = this@JsArray.size
            var position = 0

            override fun hasNext(): Boolean = position < size - 1

            override fun next(): napi_value? = get(position++)
        }
    }
}