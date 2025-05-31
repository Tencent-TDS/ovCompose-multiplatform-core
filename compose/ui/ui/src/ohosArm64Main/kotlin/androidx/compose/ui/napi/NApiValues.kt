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

import kotlinx.cinterop.CPointer
import platform.ohos.napi_ref
import platform.ohos.napi_value

fun napi_value?.asJsArray() = JsArray(this)

fun napi_value?.asJsObject() = JsObject(this)

fun napi_value?.asInt() = JsEnv.getValueInt32(this)

fun napi_value?.asLong() = JsEnv.getValueInt64(this)

fun napi_value?.asFloat() = JsEnv.getValueFloat(this)

fun napi_value?.asDouble() = JsEnv.getValueDouble(this)

fun napi_value?.asString() = JsEnv.getValueStringUtf8(this)

fun napi_value?.asBoolean() = JsEnv.getValueBool(this)

inline operator fun napi_ref?.get(key: String): napi_value? {
    val referenceValue = JsEnv.getReferenceValue(this)
    return JsEnv.getProperty(referenceValue, key.nApiValue())
}

inline operator fun napi_ref?.set(key: String, value: napi_value?) {
    val referenceValue = JsEnv.getReferenceValue(this)
    JsEnv.setProperty(referenceValue, key.nApiValue(), value)
}

inline fun napi_ref?.call(name: String, vararg args: napi_value?): napi_value? {
    val referenceValue = JsEnv.getReferenceValue(this)
    val func = JsEnv.getProperty(referenceValue, name.nApiValue())
    return JsEnv.callFunction(referenceValue, func, *args)
}

inline fun napi_ref?.call(name: String, vararg args: JsObject?): napi_value? {
    val referenceValue = JsEnv.getReferenceValue(this)
    val func = JsEnv.getProperty(referenceValue, name.nApiValue())
    return JsEnv.callFunction(referenceValue, func, *args.map { it?.jsValue }.toTypedArray())
}

inline fun napi_ref?.call(name: String): napi_value? {
    val referenceValue = JsEnv.getReferenceValue(this)
    val func = JsEnv.getProperty(referenceValue, name.nApiValue())
    return JsEnv.callFunction(referenceValue, func)
}

@Suppress("IMPLICIT_CAST_TO_ANY")
inline fun <reified R> napi_value?.asTypeOf(): R {
    return when (R::class) {
        CPointer::class ->
            (this as? R)
                ?: throw IllegalArgumentException(
                    "Unsupported type: ${R::class}. " +
                        "Can not convert napi_value to any other CPointer types."
                )

        Unit::class -> Unit
        Int::class -> asInt()
        Float::class -> asFloat()
        Double::class -> asDouble()
        String::class -> asString()
        Boolean::class -> asBoolean()
        JsObject::class -> asJsObject()
        JsArray::class -> asJsArray()
        else -> throw IllegalArgumentException("Unsupported type: ${R::class}")
    } as R
}