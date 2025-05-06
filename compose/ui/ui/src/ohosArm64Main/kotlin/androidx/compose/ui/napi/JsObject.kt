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

import kotlin.native.ref.createCleaner
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import platform.ohos.napi_ref
import platform.ohos.napi_value


internal val JsCleanerCoroutineScope =
    CoroutineScope(CoroutineName("JsCleanerCoroutineScope") + Dispatchers.Main)

open class JsObject(value: napi_value?) {

    private val jsValueRef: napi_ref? = JsEnv.createReference(value)

    private val cleaner = createCleaner(jsValueRef) {
        JsCleanerCoroutineScope.launch {
            JsEnv.deleteReference(it)
        }
    }

    val jsValue: napi_value? get() = JsEnv.getReferenceValue(jsValueRef)

    operator fun get(key: String): napi_value? =
        JsEnv.getProperty(jsValue, key.nApiValue())

    operator fun set(key: String, value: napi_value?) {
        JsEnv.setProperty(jsValue, key.nApiValue(), value)
    }

    fun call(name: String): napi_value? {
        val func = get(name)
        return JsEnv.callFunction(jsValue, func)
    }

    fun call(name: String, vararg args: napi_value?): napi_value? {
        val func = get(name)
        return JsEnv.callFunction(jsValue, func, *args)
    }

    fun call(name: String, vararg args: JsObject?): napi_value? =
        call(name, *args.map { it?.jsValue }.toTypedArray())

    operator fun String.invoke(value: napi_value?) {
        set(this, value)
    }

    operator fun String.invoke(value: Any?) {
        set(this, value.nApiValue())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is JsObject) return false
        val targetJsValue = jsValue
        val srcJsValue = other.jsValue
        if (targetJsValue === srcJsValue) return true
        if (targetJsValue == null || srcJsValue == null) return false

        val srcNames = JsEnv.getAllPropertyNames(srcJsValue)
        val srcLength = JsEnv.getArrayLength(srcNames)

        val targetNames = JsEnv.getAllPropertyNames(targetJsValue)
        val targetLength = JsEnv.getArrayLength(targetNames)
        if (targetLength != srcLength) {
            return false
        }
        for (i in 0 until srcLength) {
            val srcKey = JsEnv.getElement(srcNames, i)
            val targetKey = JsEnv.getElement(targetNames, i)
            val srcValue = JsEnv.getProperty(srcJsValue, srcKey)
            val targetValue = JsEnv.getProperty(targetJsValue, targetKey)
            val ret = JsEnv.strictEquals(srcValue, targetValue)
            if (!ret) {
                return false
            }
        }
        return true
    }
}

fun js(block: (JsObject.() -> Unit)? = null): JsObject {
    val jsObject = JsObject(JsEnv.createObject())
    if (block != null) {
        jsObject.apply(block)
    }
    return jsObject
}