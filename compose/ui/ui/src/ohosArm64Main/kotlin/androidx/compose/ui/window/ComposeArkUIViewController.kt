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

@file:Suppress("FunctionName")

package androidx.compose.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.ui.arkui.ArkUIViewController
import androidx.compose.ui.arkui.createControllerNapiValue
import platform.ohos.napi_env
import platform.ohos.napi_value

/**
 * 创建一个包含 Compose UI 组件的 ComposeArkUIViewController，用于接入鸿蒙 ArkUI 视图体系
 *
 * @return [ArkUIViewController] 的 [napi_value]
 * @author gavinbaoliu
 * @since 2024/5/21
 */
fun ComposeArkUIViewController(
    env: napi_env,
    content: @Composable () -> Unit
): napi_value = ComposeArkUIViewController(env, configure = {}, content = content)

/**
 * 创建一个包含 Compose UI 组件的 ComposeArkUIViewController，用于接入鸿蒙 ArkUI 视图体系
 *
 * @return [ArkUIViewController] 的 [napi_value]
 */
fun ComposeArkUIViewController(
    env: napi_env,
    configure: ComposeArkUIViewControllerConfiguration.() -> Unit = {},
    content: @Composable () -> Unit
): napi_value =
    ComposeArkUIViewContainer(
        configuration = ComposeArkUIViewControllerConfiguration().apply(configure),
        content = content
    ).createControllerNapiValue(env)