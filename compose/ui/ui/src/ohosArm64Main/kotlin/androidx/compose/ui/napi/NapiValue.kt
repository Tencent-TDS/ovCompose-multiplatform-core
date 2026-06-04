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
import kotlinx.coroutines.launch
import platform.ohos.napi_ref
import platform.ohos.napi_value


/**
 * Kotlin 管理的 [napi_value]；由 Kotlin 持有对 [napi_value] 的引用，并且在当前 Kotlin 对象销毁时自动释放对 [napi_value] 的引用
 *
 * @author gavinbaoliu
 * @since 2024/11/29
 */
interface NapiValue {
    /**
     * 获取原始 [napi_value]，由于 JavaScript 运行时会发生内存整理，所获取的 [napi_value] 不应当被长期保存，仅应在当前调用栈上使用
     */
    val rawValue: napi_value
}

internal class NapiValueImpl(napiValue: napi_value) : NapiValue {

    private val reference: napi_ref? = JsEnv.createReference(napiValue)

    // holds a reference to cleaner
    private val cleaner = createCleaner(reference) { reference ->
        JsCleanerCoroutineScope.launch {
            JsEnv.deleteReference(reference)
        }
    }

    override val rawValue: napi_value
        get() = JsEnv.getReferenceValue(reference)
            ?: throw IllegalStateException("NapiValue($this) get raw value from $reference failed.")
}