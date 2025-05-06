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

package androidx.compose.ui.platform.textinput

import androidx.compose.ui.napi.JsEnv
import androidx.compose.ui.napi.JsFunction
import androidx.compose.ui.napi.JsObject
import androidx.compose.ui.napi.jsFunction
import platform.ohos.napi_value

internal object InputMethodManager {

    private const val TAG = "InputMethodManager"

    private const val EVENT_INSERT_TEXT = "insertText"
    private const val EVENT_DELETE_LEFT = "deleteLeft"
    private const val EVENT_SEND_FUNCTION_KEY = "sendFunctionKey"

    private var isListened = false
    private var inputConnection: InputConnection? = null

    private var attachCallback = jsFunction(this) { error: JsObject? ->
        if (error != null && !JsEnv.isUndefined(error.jsValue)) {
            log(TAG, "Failed to show softKeyboard: ${JsEnv.stringify(error.jsValue)}")
            return@jsFunction
        }
        log(TAG, "Success to show softKeyboard")
        registerListeners()
    }
    private var detachCallback = jsFunction(this) { error: JsObject? ->
        if (error != null && !JsEnv.isUndefined(error.jsValue)) {
            log(TAG, "Failed to hide softKeyboard: ${JsEnv.stringify(error.jsValue)}")
            return@jsFunction
        }
        log(TAG, "Success to hide softKeyboard")
        unregisterListeners()
    }
    private var onInsertTextCallback: JsFunction<InputMethodManager, Unit?>? = null
    private var onDeleteLeftCallback: JsFunction<InputMethodManager, Unit?>? = null
    private var onSendFunctionKeyCallback: JsFunction<InputMethodManager, Boolean?>? = null

    fun showSoftKeyboard(textConfig: OhosTextConfig?, inputConnection: InputConnection) {
        log(TAG, "showSoftKeyboard")

        // controller.attach()
        val controller = getController()
        val attachFunc = JsEnv.getProperty(controller, JsEnv.createStringUtf8("attach"))
        JsEnv.callFunction(
            controller, attachFunc, JsEnv.getBoolean(true),
            textConfig?.asJSValue(), attachCallback.jsValue
        )

        InputMethodManager.inputConnection = inputConnection
    }

    fun hideSoftKeyboard() {
        log(TAG, "hideSoftKeyboard")

        // controller.detach()
        val controller = getController()
        val detachFunc = JsEnv.getProperty(controller, JsEnv.createStringUtf8("detach"))
        JsEnv.callFunction(controller, detachFunc, detachCallback.jsValue)

        inputConnection = null
    }

    private fun getController(): napi_value? {
        val global = JsEnv.getGlobal()

        // const inputMethod = global.requireNapi('inputMethod')
        val requireNapiFunc = JsEnv.getProperty(global, JsEnv.createStringUtf8("requireNapi"))
        val inputMethod = JsEnv.callFunction(global, requireNapiFunc, JsEnv.createStringUtf8("inputMethod"))

        // const controller = inputMethod.getController
        val getControllerFunc = JsEnv.getProperty(inputMethod, JsEnv.createStringUtf8("getController"))
        val controller = JsEnv.callFunction(inputMethod, getControllerFunc)

        return controller
    }

    private fun bindListener(controller: napi_value?, event: String, callback: napi_value?) {
        // controller.on(event, callback)
        val onFunc = JsEnv.getProperty(controller, JsEnv.createStringUtf8("on"))
        JsEnv.callFunction(controller, onFunc, JsEnv.createStringUtf8(event), callback)
    }

    private fun unbindListener(controller: napi_value?, event: String, callback: napi_value?) {
        // controller.off(event, callback)
        val offFunc = JsEnv.getProperty(controller, JsEnv.createStringUtf8("off"))
        // TODO nathanwwang 传入callback的函数有引用问题导致反注册失败，先调用全部移除的函数，controller.off(event)
//        JsEnv.callFunction(controller, offFunc, JsEnv.createStringUtf8(event), callback)
        JsEnv.callFunction(controller, offFunc, JsEnv.createStringUtf8(event))
    }

    private fun registerListeners() {
        if (isListened) {
            return
        }
        isListened = true
        log(TAG, "registerListeners")
        val controller = getController()

        // controller.on('insertText', callback)
        onInsertTextCallback = jsFunction(this) { text: String? ->
            log(TAG, "insertText, text: $text")
            text?.let { inputConnection?.insertText(it) }
        }.apply {
            bindListener(controller, EVENT_INSERT_TEXT, jsValue)
        }

        // controller.on('deleteLeft', callback)
        onDeleteLeftCallback = jsFunction(this) { _: Int? ->
            log(TAG, "deleteLeft")
            inputConnection?.deleteBackward()
        }.apply {
            bindListener(controller, EVENT_DELETE_LEFT, jsValue)
        }

        // controller.on('sendFunctionKey', callback)
        onSendFunctionKeyCallback = jsFunction(this) { functionKey: JsObject? ->
            val enterKeyType = JsEnv.getValueInt32(functionKey?.get("enterKeyType"))
                ?: return@jsFunction false
            log(TAG, "sendFunctionKey, enterKeyType: $enterKeyType")
            inputConnection?.performEditorAction(enterKeyType)
        }.apply {
            bindListener(controller, EVENT_SEND_FUNCTION_KEY, jsValue)
        }
    }

    private fun unregisterListeners() {
        if (!isListened) {
            return
        }
        isListened = false
        log(TAG, "unregisterListeners")
        val controller = getController()
        onInsertTextCallback?.apply {
            unbindListener(controller, EVENT_INSERT_TEXT, jsValue)
            dispose()
            onInsertTextCallback = null
        }
        onDeleteLeftCallback?.apply {
            unbindListener(controller, EVENT_DELETE_LEFT, jsValue)
            dispose()
            onDeleteLeftCallback = null
        }
        onSendFunctionKeyCallback?.apply {
            unbindListener(controller, EVENT_SEND_FUNCTION_KEY, jsValue)
            dispose()
            onSendFunctionKeyCallback = null
        }
    }
}