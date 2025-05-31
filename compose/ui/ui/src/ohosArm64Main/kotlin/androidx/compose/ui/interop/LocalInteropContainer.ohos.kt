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

package androidx.compose.ui.interop

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.arkui.ArkUIRootView
import androidx.compose.ui.interop.arkc.ArkUINativeInteropContainer

val LocalArkUINativeInteropContainer = staticCompositionLocalOf<ArkUINativeInteropContainer> {
    error("CompositionLocal ArkUI Native InteropContainer not provided")
}

val LocalBackArkUINativeInteropContainer = staticCompositionLocalOf<ArkUINativeInteropContainer> {
    error("CompositionLocal Back ArkUI Native InteropContainer not provided")
}

val LocalForeArkUINativeInteropContainer = staticCompositionLocalOf<ArkUINativeInteropContainer> {
    error("CompositionLocal Fore ArkUI Native InteropContainer not provided")
}

val LocalTouchableArkUINativeInteropContainer = staticCompositionLocalOf<ArkUINativeInteropContainer> {
    error("CompositionLocal Touchable ArkUI Native InteropContainer not provided")
}

val LocalInteropContainer = staticCompositionLocalOf<ArkUIRootView> {
    error("CompositionLocal LayerContainer not provided")
}

val LocalBackInteropContainer = staticCompositionLocalOf<ArkUIRootView> {
    error("CompositionLocal back LayerContainer not provided")
}

val LocalForeInteropContainer = staticCompositionLocalOf<ArkUIRootView> {
    error("CompositionLocal fore LayerContainer not provided")
}

val LocalTouchableInteropContainer = staticCompositionLocalOf<ArkUIRootView> {
    error("CompositionLocal touchable LayerContainer not provided")
}
