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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.KeyboardType
import platform.ohos.napi_value

internal enum class InputType(val value: Int) {
    NONE(-1),   // NONE。
    TEXT(0),   // 文本类型。
    MULTILINE(1),   // 多行类型。
    NUMBER(2),   // 数字类型。
    PHONE(3),   // 电话号码类型。
    DATETIME(4),   // 日期类型。
    EMAIL_ADDRESS(5),   // 邮箱地址类型。
    URL(6),   // 链接类型。
    VISIBLE_PASSWORD(7),   // 密码类型。
    NUMBER_PASSWORD11(8),   // 数字密码类型。
}

/**
 * Enter键的功能类型
 */
internal enum class EnterKeyType(val value: Int) {
    UNSPECIFIED(0),   // 未指定。
    NONE(1),   // NONE。
    GO(2),   // 前往。
    SEARCH(3),   // 查找。
    SEND(4),   // 发送。
    NEXT(5),   // 下一步。
    DONE(6),   // 完成。
    PREVIOUS(7),   // 上一步。。
}

internal class OhosTextConfig(private val imeOptions: ImeOptions) {

    val inputType: InputType
    val enterKeyType: EnterKeyType

    init {
        when (imeOptions.keyboardType) {
            KeyboardType.Text -> this.inputType = InputType.TEXT
            KeyboardType.Ascii -> this.inputType = InputType.TEXT
            KeyboardType.Number -> this.inputType = InputType.NUMBER
            KeyboardType.Phone -> this.inputType = InputType.PHONE
            KeyboardType.Uri -> this.inputType = InputType.URL
            KeyboardType.Email -> this.inputType = InputType.EMAIL_ADDRESS
            KeyboardType.Password -> this.inputType = InputType.VISIBLE_PASSWORD
            KeyboardType.NumberPassword -> this.inputType = InputType.NUMBER_PASSWORD11
            KeyboardType.Decimal -> this.inputType = InputType.NUMBER
            else -> this.inputType = InputType.NONE
        }

        this.enterKeyType = when (imeOptions.imeAction) {
            ImeAction.Default -> EnterKeyType.UNSPECIFIED
            ImeAction.Go -> EnterKeyType.GO
            ImeAction.Search -> EnterKeyType.SEARCH
            ImeAction.Send -> EnterKeyType.SEND
            ImeAction.Previous -> EnterKeyType.PREVIOUS
            ImeAction.Next -> EnterKeyType.NEXT
            ImeAction.Done -> EnterKeyType.DONE
            else -> EnterKeyType.NONE
        }
    }
}

internal fun OhosTextConfig.asJSValue(): napi_value? {
    val textConfig = JsEnv.createObject()

    val inputAttribute = JsEnv.createObject()
    JsEnv.setProperty(inputAttribute,
        JsEnv.createStringUtf8("textInputType"),
        JsEnv.createInt32(inputType.value)
    )
    JsEnv.setProperty(inputAttribute,
        JsEnv.createStringUtf8("enterKeyType"),
        JsEnv.createInt32(enterKeyType.value)
    )

    JsEnv.setProperty(textConfig, JsEnv.createStringUtf8("inputAttribute"), inputAttribute)

    return textConfig
}