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

import kotlinx.cinterop.StableRef
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.staticCFunction
import platform.ohos.napi_callback_info
import platform.ohos.napi_env

private class JsFunctionCall<Receiver : Any, R>(
    val receiver: Receiver? = null,
    val block: Receiver?.(JsCallContext) -> R
) {
    operator fun invoke(context: JsCallContext) = receiver?.block(context)
}

class JsFunction<Receiver : Any, R>(
    receiver: Receiver? = null,
    name: String? = null,
    block: Receiver?.(JsCallContext) -> R
) {
    private val callRef = StableRef.create(JsFunctionCall(receiver, block))

    private val jsValueRef = JsEnv.createFunction(
        name, staticCFunction { _: napi_env?, callbackInfo: napi_callback_info? ->
            val context = JsCallContext(callbackInfo)
            val call = context.data?.asStableRef<JsFunctionCall<*, *>>()
                ?.get() as JsFunctionCall<Receiver, R>
            call(context).nApiValue()
        }, callRef.asCPointer()
    ).let(JsEnv::createReference)

    val jsValue get() = JsEnv.getReferenceValue(jsValueRef)

    fun dispose() {
        JsEnv.deleteReference(jsValueRef)
        callRef.dispose()
    }
}

inline fun <Receiver : Any, R> jsFunction(
    receiver: Receiver? = null,
    name: String? = null,
    crossinline block: Receiver?.() -> R
): JsFunction<Receiver, R> {
    return JsFunction(receiver, name) {
        block()
    }
}

inline fun <Receiver : Any, reified P, R> jsFunction(
    receiver: Receiver? = null,
    name: String? = null,
    crossinline block: Receiver?.(P?) -> R
): JsFunction<Receiver, R> {
    return JsFunction(receiver, name) {
        block(it.arguments[0].asTypeOf<P>())
    }
}

inline fun <Receiver : Any, reified P0, reified P1, R> jsFunction(
    receiver: Receiver? = null,
    name: String? = null,
    crossinline block: Receiver?.(P0?, P1?) -> R
): JsFunction<Receiver, R> {
    return JsFunction(receiver, name) {
        block(
            it.arguments[0].asTypeOf<P0>(),
            it.arguments[1].asTypeOf<P1>()
        )
    }
}

inline fun <Receiver : Any, reified P0, reified P1, reified P2, R> jsFunction(
    receiver: Receiver? = null,
    name: String? = null,
    crossinline block: Receiver?.(P0?, P1?, P2?) -> R
): JsFunction<Receiver, R> {
    return JsFunction(receiver, name) {
        block(
            it.arguments[0].asTypeOf<P0>(),
            it.arguments[1].asTypeOf<P1>(),
            it.arguments[2].asTypeOf<P2>()
        )
    }
}

inline fun <Receiver : Any, reified P0, reified P1, reified P2, reified P3, R> jsFunction(
    receiver: Receiver? = null,
    name: String? = null,
    crossinline block: Receiver?.(P0?, P1?, P2?, P3?) -> R
): JsFunction<Receiver, R> {
    return JsFunction(receiver, name) {
        block(
            it.arguments[0].asTypeOf<P0>(),
            it.arguments[1].asTypeOf<P1>(),
            it.arguments[2].asTypeOf<P2>(),
            it.arguments[3].asTypeOf<P3>()
        )
    }
}