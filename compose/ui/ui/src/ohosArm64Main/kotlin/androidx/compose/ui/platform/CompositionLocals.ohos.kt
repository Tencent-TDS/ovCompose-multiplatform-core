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

package androidx.compose.ui.platform

import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.arkui.ArkUIViewController
import androidx.lifecycle.LifecycleOwner
import platform.ohos.napi_env

/**
 * The CompositionLocal containing the current [napi_env].
 */
val LocalNapiEnv = staticCompositionLocalOf<napi_env> {
    noLocalProvidedFor("LocalNapiEnv")
}

/**
 * The CompositionLocal containing the current [Context].
 */
val LocalContext = staticCompositionLocalOf<Context> {
    noLocalProvidedFor("LocalContext")
}

/**
 * The CompositionLocal containing the current [UIContext].
 */
val LocalUIContext = staticCompositionLocalOf<UIContext> {
    noLocalProvidedFor("LocalUIContext")
}

/**
 * The CompositionLocal containing the current [ArkUIViewController].
 */
val LocalArkUIViewController = staticCompositionLocalOf<ArkUIViewController> {
    noLocalProvidedFor("LocalArkUIViewController")
}

/**
 * The CompositionLocal containing the current [LifecycleOwner].
 */
val LocalLifecycleOwner = staticCompositionLocalOf<LifecycleOwner> {
    noLocalProvidedFor("LocalLifecycleOwner")
}

/**
 * The CompositionLocal containing the current [LocalInterfaceOrientation].
 */
@InternalComposeApi
val LocalInterfaceOrientation = staticCompositionLocalOf { InterfaceOrientation.Portrait }

private fun noLocalProvidedFor(name: String): Nothing {
    error("CompositionLocal $name not present")
}