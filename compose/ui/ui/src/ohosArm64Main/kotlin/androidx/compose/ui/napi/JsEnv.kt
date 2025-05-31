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

import androidx.compose.ui.graphics.kLog
import androidx.compose.ui.graphics.isDebugLogEnabled
import kotlinx.cinterop.BooleanVar
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.COpaquePointerVar
import kotlinx.cinterop.DoubleVar
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.cValuesOf
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toKStringFromUtf8
import kotlinx.cinterop.value
import platform.ohos.NAPI_AUTO_LENGTH
import platform.ohos.napi_acquire_threadsafe_function
import platform.ohos.napi_call_function
import platform.ohos.napi_call_threadsafe_function
import platform.ohos.napi_callback
import platform.ohos.napi_callback_info
import platform.ohos.napi_create_double
import platform.ohos.napi_create_function
import platform.ohos.napi_create_int32
import platform.ohos.napi_create_int64
import platform.ohos.napi_create_object
import platform.ohos.napi_create_reference
import platform.ohos.napi_create_string_utf8
import platform.ohos.napi_create_threadsafe_function
import platform.ohos.napi_delete_reference
import platform.ohos.napi_env
import platform.ohos.napi_get_array_length
import platform.ohos.napi_get_boolean
import platform.ohos.napi_get_cb_info
import platform.ohos.napi_get_element
import platform.ohos.napi_get_global
import platform.ohos.napi_get_named_property
import platform.ohos.napi_get_null
import platform.ohos.napi_get_property
import platform.ohos.napi_get_property_names
import platform.ohos.napi_get_reference_value
import platform.ohos.napi_get_undefined
import platform.ohos.napi_get_value_bool
import platform.ohos.napi_get_value_double
import platform.ohos.napi_get_value_int32
import platform.ohos.napi_get_value_int64
import platform.ohos.napi_get_value_string_utf8
import platform.ohos.napi_ok
import platform.ohos.napi_ref
import platform.ohos.napi_refVar
import platform.ohos.napi_set_element
import platform.ohos.napi_set_property
import platform.ohos.napi_status
import platform.ohos.napi_threadsafe_function
import platform.ohos.napi_threadsafe_functionVar
import platform.ohos.napi_threadsafe_function_call_js
import platform.ohos.napi_threadsafe_function_call_mode
import platform.ohos.napi_strict_equals
import platform.ohos.napi_typeof
import platform.ohos.napi_value
import platform.ohos.napi_valueVar
import platform.ohos.napi_valuetype
import platform.ohos.napi_wrap
import platform.posix.int32_tVar
import platform.posix.int64_tVar
import platform.posix.size_tVar
import platform.posix.u_int32_tVar

object JsEnv {

    private var globalNApiEnv: napi_env? = null

    fun init(env: napi_env) {
        globalNApiEnv = env
    }

    fun env(): napi_env = globalNApiEnv ?: error("napi_env is not initialized. Call JsEnv.init(env) first.")

    private fun checkStatus(status: napi_status): Boolean {
        if (status != napi_ok) {
            if (isDebugLogEnabled) {
                kLog("napi failed: $status")
            }
            return false
        }
        return true
    }

    fun getElement(`object`: napi_value?, index: Int): napi_value? {
        `object` ?: return null

        return memScoped {
            val result = alloc<napi_valueVar>()
            checkStatus(napi_get_element(env(), `object`, index.toUInt(), result.ptr))
            result.value
        }
    }

    fun setElement(`object`: napi_value?, index: Int, value: napi_value?) {
        `object` ?: return

        checkStatus(napi_set_element(env(), `object`, index.toUInt(), value))
    }

    fun getArrayLength(array: napi_value?): Int {
        array ?: return 0

        return memScoped {
            val result = alloc<u_int32_tVar>()
            checkStatus(napi_get_array_length(env(), array, result.ptr))
            result.value.toInt()
        }
    }

    fun getAllPropertyNames(`object`: napi_value?): napi_value? {
        return memScoped {
            val result = alloc<napi_valueVar>()
            checkStatus(napi_get_property_names(env(), `object`, result.ptr))
            result.value
        }
    }

    fun strictEquals(trgObject: napi_value?, srcObject: napi_value?): Boolean {
        return memScoped {
            val result = alloc<BooleanVar>()
            checkStatus(napi_strict_equals(env(), trgObject, srcObject, result.ptr))
            result.value
        }
    }

    fun getProperty(`object`: napi_value?, key: napi_value?): napi_value? {
        `object` ?: return null
        key ?: return null

        return memScoped {
            val result = alloc<napi_valueVar>()
            checkStatus(napi_get_property(env(), `object`, key, result.ptr))
            result.value
        }
    }

    fun setProperty(`object`: napi_value?, key: napi_value?, value: napi_value?) {
        `object` ?: return
        key ?: return
        checkStatus(napi_set_property(env(), `object`, key, value ?: getNull()))
    }

    fun getNamedProperty(`object`: napi_value?, str: String): napi_value? {
        `object` ?: return null
        return memScoped {
            val result = alloc<napi_valueVar>()
            checkStatus(napi_get_named_property(env(), `object`, str, result.ptr))
            result.value
        }
    }

    fun createObject(): napi_value? {
        return memScoped {
            val result = alloc<napi_valueVar>()
            checkStatus(napi_create_object(env(), result.ptr))
            result.value
        }
    }

    fun getReferenceValue(ref: napi_ref?): napi_value? {
        ref ?: return null

        return memScoped {
            val result = alloc<napi_valueVar>()
            checkStatus(napi_get_reference_value(env(), ref, result.ptr))
            result.value
        }
    }

    fun createReference(value: napi_value?): napi_ref? {
        return memScoped {
            val ref = alloc<napi_refVar>()
            checkStatus(napi_create_reference(env(), value, 1U, ref.ptr))
            ref.value
        }
    }

    fun deleteReference(ref: napi_ref?) {
        ref ?: return

        checkStatus(napi_delete_reference(env(), ref))
    }

    fun createFunction(name: String?, cb: napi_callback?, data: COpaquePointer?): napi_value? {
        memScoped {
            val func = alloc<napi_valueVar>()
            checkStatus(napi_create_function(env(), name, NAPI_AUTO_LENGTH, cb, data, func.ptr))
            return func.value
        }
    }

    fun callFunction(
        receiver: napi_value?,
        func: napi_value?,
        vararg args: napi_value?
    ): napi_value? {
        func ?: return null

        return memScoped {
            val result = alloc<napi_valueVar>()
            checkStatus(
                napi_call_function(
                    env(),
                    receiver,
                    func,
                    args.size.toULong(),
                    cValuesOf(*args),
                    result.ptr
                )
            )
            result.value
        }
    }

    fun createStringUtf8(str: String): napi_value? {
        return memScoped {
            val result = alloc<napi_valueVar>()
            checkStatus(napi_create_string_utf8(env(), str, NAPI_AUTO_LENGTH, result.ptr))
            result.value
        }
    }

    fun createDouble(value: Double): napi_value? {
        return memScoped {
            val result = alloc<napi_valueVar>()
            checkStatus(napi_create_double(env(), value, result.ptr))
            result.value
        }
    }

    fun createFloat(value: Float): napi_value? = createDouble(value.toDouble())

    fun createObjectWithWrap(value: Any): napi_value? {
        return memScoped {
            val result = alloc<napi_valueVar>()
            checkStatus(napi_create_object(env(), result.ptr))
            val valueRef = StableRef.create(value)
            checkStatus(
                napi_wrap(
                    env(), result.value, valueRef.asCPointer(),
                    staticCFunction { _: napi_env?, data: COpaquePointer?, _: COpaquePointer? ->
                        if (data == null) return@staticCFunction
                        data.asStableRef<Any>().dispose()
                    },
                    null, null
                )
            )
            result.value
        }
    }

    fun getBoolean(value: Boolean): napi_value? {
        return memScoped {
            val result = alloc<napi_valueVar>()
            checkStatus(napi_get_boolean(env(), value, result.ptr))
            result.value
        }
    }

    fun createInt32(value: Int): napi_value? {
        return memScoped {
            val result = alloc<napi_valueVar>()
            checkStatus(napi_create_int32(env(), value, result.ptr))
            result.value
        }
    }

    fun createInt64(value: Long): napi_value? {
        return memScoped {
            val result = alloc<napi_valueVar>()
            checkStatus(napi_create_int64(env(), value, result.ptr))
            result.value
        }
    }

    fun getValueInt32(value: napi_value?): Int? {
        value ?: return null

        return memScoped {
            val result = alloc<int32_tVar>()
            if (checkStatus(napi_get_value_int32(env(), value, result.ptr))) {
                result.value
            } else {
                null
            }
        }
    }

    fun getValueInt32(value: napi_value?, default: Int): Int {
        value ?: return default
        return memScoped {
            val result = alloc<int32_tVar>()
            if (checkStatus(napi_get_value_int32(env(), value, result.ptr))) {
                result.value
            } else {
                default
            }
        }
    }

    fun getValueInt64(value: napi_value?): Long? {
        value ?: return null

        return memScoped {
            val result = alloc<int64_tVar>()
            if (checkStatus(napi_get_value_int64(env(), value, result.ptr))) {
                result.value
            } else {
                null
            }
        }
    }

    fun getValueInt64(value: napi_value?, default: Long): Long {
        value ?: return default
        return memScoped {
            val result = alloc<int64_tVar>()
            if (checkStatus(napi_get_value_int64(env(), value, result.ptr))) {
                result.value
            } else {
                default
            }
        }
    }

    fun getValueDouble(value: napi_value?): Double? {
        value ?: return null

        return memScoped {
            val result = alloc<DoubleVar>()
            if (checkStatus(napi_get_value_double(env(), value, result.ptr))) {
                result.value
            } else {
                null
            }
        }
    }

    fun getValueFloat(value: napi_value?): Float? = getValueDouble(value)?.toFloat()

    fun getValueStringUtf8(value: napi_value?): String? {
        value ?: return null

        return memScoped {
            val size = alloc<size_tVar>()
            if (!checkStatus(napi_get_value_string_utf8(env(), value, null, 0U, size.ptr))) {
                return null
            }

            val buf = allocArray<ByteVar>(size.value.toInt() + 1)
            if (checkStatus(
                    napi_get_value_string_utf8(
                        env(),
                        value,
                        buf.getPointer(this),
                        size.value + 1u,
                        size.ptr
                    )
                )
            ) {
                buf.toKStringFromUtf8()
            } else {
                null
            }
        }
    }

    fun getValueBool(value: napi_value?): Boolean? {
        value ?: return null

        return memScoped {
            val result = alloc<BooleanVar>()
            if (checkStatus(napi_get_value_bool(env(), value, result.ptr))) {
                result.value
            } else {
                null
            }
        }
    }

    fun getUndefined(): napi_value? {
        return memScoped {
            val result = alloc<napi_valueVar>()
            checkStatus(napi_get_undefined(env(), result.ptr))
            result.value
        }
    }

    fun isUndefined(value: napi_value?): Boolean =
        getType(value) == napi_valuetype.napi_undefined

    private fun getNull(): napi_value? {
        return memScoped {
            val result = alloc<napi_valueVar>()
            checkStatus(napi_get_undefined(env(), result.ptr))
            napi_get_null(env(), result?.ptr) // Create a null value
            result.value
        }
    }

    fun getType(value: napi_value?): napi_valuetype? {
        return memScoped {
            val result = alloc<napi_valuetype.Var>()
            if (checkStatus(napi_typeof(env(), value, result.ptr))) {
                result.value
            } else {
                null
            }
        }
    }

    fun getCbInfo(callbackInfo: napi_callback_info?): JsCallContext {
        return memScoped {
            val argc = alloc<size_tVar>()
            checkStatus(napi_get_cb_info(env(), callbackInfo, argc.ptr, null, null, null))

            val length = argc.value.toInt()
            val args = allocArray<napi_valueVar>(length)
            val jsThis = alloc<napi_valueVar>()
            val data = alloc<COpaquePointerVar>()

            checkStatus(
                napi_get_cb_info(
                    env(),
                    callbackInfo,
                    argc.ptr,
                    args.getPointer(this),
                    jsThis.ptr,
                    data.ptr
                )
            )

            val arguments = ArrayList<napi_value?>()
            for (i in 0..<length) {
                arguments.add(args[i])
            }
            JsCallContext(jsThis.value, arguments, data.value)
        }
    }

    fun getGlobal(): napi_value? {
        return memScoped {
            val result = alloc<napi_valueVar>()
            napi_get_global(env(), result.ptr)
            result.value
        }
    }

    fun createThreadsafeFunction(
        workName: String,
        callback: napi_threadsafe_function_call_js?
    ): napi_threadsafe_function? {
        return memScoped {
            val jsWorkName = createStringUtf8(workName)
            val result = alloc<napi_threadsafe_functionVar>()
            napi_create_threadsafe_function(
                env(), 0.nApiValue(), null,
                jsWorkName?.getPointer(this),
                0UL, 1UL, null, null, null, callback, result.ptr
            )
            result.value
        }
    }

    fun callThreadsafeFunction(function: napi_threadsafe_function?, data: COpaquePointer?) {
        napi_acquire_threadsafe_function(function)
        napi_call_threadsafe_function(
            function, data, napi_threadsafe_function_call_mode.napi_tsfn_nonblocking
        )
    }

    fun stringify(value: napi_value?): String? {
        val global = JsEnv.getGlobal()
        val json = JsEnv.getProperty(global, JsEnv.createStringUtf8("JSON"))
        val stringifyFunc = JsEnv.getProperty(json, JsEnv.createStringUtf8("stringify"))
        val result = JsEnv.callFunction(json, stringifyFunc, value)
        return JsEnv.getValueStringUtf8(result)
    }

    fun printValue(value: napi_value?, key: String? = "") {
        val type = getType(value)
        when (type) {
            napi_valuetype.napi_undefined -> kLog("JsEnv::printValue: $key is undefined")
            napi_valuetype.napi_null -> kLog("JsEnv::printValue: $key is null")
            napi_valuetype.napi_symbol -> kLog("JsEnv::printValue: $key is symbol")
            napi_valuetype.napi_object -> kLog("JsEnv::printValue: $key is object")
            napi_valuetype.napi_function -> kLog("JsEnv::printValue: $key is function")
            napi_valuetype.napi_number -> kLog("JsEnv::printValue: $key is number")
            napi_valuetype.napi_string -> kLog("JsEnv::printValue: $key is string")
            napi_valuetype.napi_boolean -> kLog("JsEnv::printValue: $key is boolean")
            napi_valuetype.napi_bigint -> kLog("JsEnv::printValue: $key is bigint")
            napi_valuetype.napi_external -> kLog("JsEnv::printValue: $key is external")
            null -> kLog("JsEnv::printValue: $key is null")
            else -> {}
        }
    }
}