/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui

import androidx.compose.runtime.*
import platform.UIKit.UIDeviceOrientation

val _stateKeyboardHeight = mutableStateOf(0f)

val LocalKeyboardOverlapHeightState = staticCompositionLocalOf<State<Float>> {
    error("CompositionLocal LocalKeyboardOverlapHeight not provided")
}

val LocalSafeAreaTopState = staticCompositionLocalOf<State<Float>> {
    error("CompositionLocal LocalSafeAreaTopState not provided")
}

val LocalSafeAreaBottomState = staticCompositionLocalOf<State<Float>> {
    error("CompositionLocal LocalSafeAreaBottomState not provided")
}

val LocalSafeAreaLeftState = staticCompositionLocalOf<State<Float>> {
    error("CompositionLocal LocalSafeAreaLeftState not provided")
}

val LocalSafeAreaRightState = staticCompositionLocalOf<State<Float>> {
    error("CompositionLocal LocalSafeAreaRightState not provided")
}

val LocalUIDeviceOrientationState = staticCompositionLocalOf<State<UIDeviceOrientation>> {
    error("CompositionLocal LocalUIDeviceOrientationState not provided")
}
